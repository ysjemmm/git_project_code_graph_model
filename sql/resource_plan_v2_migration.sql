-- ============================================================
-- 资源规划 V2 改造：新增 product_demand_owner_time 表
-- 支持按人拆分人天，实现资源视角 + 人员视角
-- ============================================================

-- 1. 建表
CREATE TABLE IF NOT EXISTS `product_demand_owner_time` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_demand_id` bigint(20) NOT NULL COMMENT '产品需求ID',
  `owner_id` varchar(64) NOT NULL COMMENT '负责人账号ID',
  `owner` varchar(128) NOT NULL COMMENT '负责人姓名',
  `resource_type` varchar(32) NOT NULL COMMENT '资源类型: frontend/backend/qa/ued/product/ops/security',
  `resource_time` decimal(10,1) NOT NULL DEFAULT 0 COMMENT '分配人天',
  `product_demand_group_id` bigint(20) DEFAULT NULL COMMENT '所属规划分组ID（冗余）',
  `biz_domain_group_id` bigint(20) DEFAULT NULL COMMENT '业务域集ID（冗余）',
  `create_man_id` varchar(64) DEFAULT NULL,
  `create_man` varchar(128) DEFAULT NULL,
  `create_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `modify_man_id` varchar(64) DEFAULT NULL,
  `modify_man` varchar(128) DEFAULT NULL,
  `modify_date` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_demand_owner_type` (`product_demand_id`, `owner_id`, `resource_type`, `is_deleted`),
  KEY `idx_owner_id` (`owner_id`, `is_deleted`),
  KEY `idx_group_id` (`product_demand_group_id`, `is_deleted`),
  KEY `idx_biz_domain_group` (`biz_domain_group_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品需求资源人天分配表（按人拆分）';


-- ============================================================
-- 2. 数据迁移：从老数据生成 product_demand_owner_time 记录
-- 逻辑：每个需求的某个资源类型的总人天 / 该类型的人数 = 每人人天
-- ============================================================

-- 2a. 有 owner 记录的需求：按人均分
INSERT INTO product_demand_owner_time (
  product_demand_id, owner_id, owner, resource_type, resource_time,
  product_demand_group_id, biz_domain_group_id,
  create_man_id, create_man, create_date,
  modify_man_id, modify_man, modify_date, is_deleted
)
SELECT
  pdo.product_demand_id,
  pdo.owner_id,
  pdo.owner,
  pdo.resource_type,
  -- 总人天 / 该类型人数，保留1位小数
  ROUND(
    CASE pdo.resource_type
      WHEN 'frontend' THEN IFNULL(pd.front_time, 0)
      WHEN 'backend'  THEN IFNULL(pd.back_time, 0)
      WHEN 'qa'       THEN IFNULL(pd.qa_time, 0)
      WHEN 'ued'      THEN IFNULL(pd.ued_time, 0)
      WHEN 'product'  THEN IFNULL(pd.product_time, 0)
      WHEN 'ops'      THEN IFNULL(pd.ops_time, 0)
      WHEN 'security' THEN IFNULL(pd.security_time, 0)
      ELSE 0
    END / owner_count.cnt,
    1
  ) AS resource_time,
  pdgi.product_demand_group_id,
  pdg.biz_domain_group_id,
  pdo.create_man_id,
  pdo.create_man,
  pdo.create_date,
  pdo.modify_man_id,
  pdo.modify_man,
  pdo.modify_date,
  0
FROM product_demand_owner pdo
INNER JOIN product_demand pd ON pd.id = pdo.product_demand_id AND pd.is_deleted = 0
-- 计算每个需求每个资源类型有多少人
INNER JOIN (
  SELECT product_demand_id, resource_type, COUNT(*) AS cnt
  FROM product_demand_owner
  WHERE is_deleted = 0
  GROUP BY product_demand_id, resource_type
) owner_count ON owner_count.product_demand_id = pdo.product_demand_id
  AND owner_count.resource_type = pdo.resource_type
-- 关联分组信息（可能为空）
LEFT JOIN product_demand_group_item pdgi ON pdgi.product_demand_id = pdo.product_demand_id
  AND pdgi.is_deleted = 0
LEFT JOIN product_demand_group pdg ON pdg.id = pdgi.product_demand_group_id
  AND pdg.is_deleted = 0
WHERE pdo.is_deleted = 0
  -- 只迁移有人天数据的
  AND (
    CASE pdo.resource_type
      WHEN 'frontend' THEN IFNULL(pd.front_time, 0)
      WHEN 'backend'  THEN IFNULL(pd.back_time, 0)
      WHEN 'qa'       THEN IFNULL(pd.qa_time, 0)
      WHEN 'ued'      THEN IFNULL(pd.ued_time, 0)
      WHEN 'product'  THEN IFNULL(pd.product_time, 0)
      WHEN 'ops'      THEN IFNULL(pd.ops_time, 0)
      WHEN 'security' THEN IFNULL(pd.security_time, 0)
      ELSE 0
    END
  ) > 0;

-- 2b. 有人天但没有 owner 记录的需求：标记为「未分配」
INSERT INTO product_demand_owner_time (
  product_demand_id, owner_id, owner, resource_type, resource_time,
  product_demand_group_id, biz_domain_group_id,
  create_date, modify_date, is_deleted
)
SELECT
  pd.id,
  'UNASSIGNED',
  '未分配',
  rt.resource_type,
  CASE rt.resource_type
    WHEN 'frontend' THEN pd.front_time
    WHEN 'backend'  THEN pd.back_time
    WHEN 'qa'       THEN pd.qa_time
    WHEN 'ued'      THEN pd.ued_time
    WHEN 'product'  THEN pd.product_time
    WHEN 'ops'      THEN pd.ops_time
    WHEN 'security' THEN pd.security_time
  END AS resource_time,
  pdgi.product_demand_group_id,
  pdg.biz_domain_group_id,
  NOW(), NOW(), 0
FROM product_demand pd
CROSS JOIN (
  SELECT 'frontend' AS resource_type UNION ALL
  SELECT 'backend' UNION ALL
  SELECT 'qa' UNION ALL
  SELECT 'ued' UNION ALL
  SELECT 'product' UNION ALL
  SELECT 'ops' UNION ALL
  SELECT 'security'
) rt
LEFT JOIN product_demand_group_item pdgi ON pdgi.product_demand_id = pd.id AND pdgi.is_deleted = 0
LEFT JOIN product_demand_group pdg ON pdg.id = pdgi.product_demand_group_id AND pdg.is_deleted = 0
WHERE pd.is_deleted = 0
  AND (
    CASE rt.resource_type
      WHEN 'frontend' THEN IFNULL(pd.front_time, 0)
      WHEN 'backend'  THEN IFNULL(pd.back_time, 0)
      WHEN 'qa'       THEN IFNULL(pd.qa_time, 0)
      WHEN 'ued'      THEN IFNULL(pd.ued_time, 0)
      WHEN 'product'  THEN IFNULL(pd.product_time, 0)
      WHEN 'ops'      THEN IFNULL(pd.ops_time, 0)
      WHEN 'security' THEN IFNULL(pd.security_time, 0)
    END
  ) > 0
  -- 排除已有 owner 记录的
  AND NOT EXISTS (
    SELECT 1 FROM product_demand_owner pdo
    WHERE pdo.product_demand_id = pd.id
      AND pdo.resource_type = rt.resource_type
      AND pdo.is_deleted = 0
  );

-- ============================================================
-- 3. 验证脚本：检查迁移后数据一致性
-- 每个需求每个类型的人天总和应等于主表的 xxxTime（允许 0.1 误差因为四舍五入）
-- ============================================================

SELECT
  pd.id,
  'frontend' AS type,
  IFNULL(pd.front_time, 0) AS old_time,
  IFNULL(s.total_time, 0) AS new_time,
  IFNULL(pd.front_time, 0) - IFNULL(s.total_time, 0) AS diff
FROM product_demand pd
LEFT JOIN (
  SELECT product_demand_id, SUM(resource_time) AS total_time
  FROM product_demand_owner_time
  WHERE is_deleted = 0 AND resource_type = 'frontend'
  GROUP BY product_demand_id
) s ON s.product_demand_id = pd.id
WHERE pd.is_deleted = 0 AND IFNULL(pd.front_time, 0) > 0
HAVING ABS(diff) > 0.2

UNION ALL

SELECT
  pd.id, 'backend',
  IFNULL(pd.back_time, 0),
  IFNULL(s.total_time, 0),
  IFNULL(pd.back_time, 0) - IFNULL(s.total_time, 0)
FROM product_demand pd
LEFT JOIN (
  SELECT product_demand_id, SUM(resource_time) AS total_time
  FROM product_demand_owner_time
  WHERE is_deleted = 0 AND resource_type = 'backend'
  GROUP BY product_demand_id
) s ON s.product_demand_id = pd.id
WHERE pd.is_deleted = 0 AND IFNULL(pd.back_time, 0) > 0
HAVING ABS(diff) > 0.2

UNION ALL

SELECT
  pd.id, 'qa',
  IFNULL(pd.qa_time, 0),
  IFNULL(s.total_time, 0),
  IFNULL(pd.qa_time, 0) - IFNULL(s.total_time, 0)
FROM product_demand pd
LEFT JOIN (
  SELECT product_demand_id, SUM(resource_time) AS total_time
  FROM product_demand_owner_time
  WHERE is_deleted = 0 AND resource_type = 'qa'
  GROUP BY product_demand_id
) s ON s.product_demand_id = pd.id
WHERE pd.is_deleted = 0 AND IFNULL(pd.qa_time, 0) > 0
HAVING ABS(diff) > 0.2;

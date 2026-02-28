-- ============================================================
-- 产品需求上线时间字段新增及数据迁移
-- 功能：记录产品需求实际上线时间
-- 迁移逻辑：
--   1. 关联了项目且项目有 actual_end_date → 用项目的 actual_end_date（发布正式节点实际时间）
--   2. 未关联项目或项目无 actual_end_date → 用需求自身的 modify_date（状态变更时间）
-- ============================================================

-- 1. 给 product_demand 表新增上线时间字段
ALTER TABLE product_demand
ADD COLUMN online_time DATETIME DEFAULT NULL
COMMENT '上线时间（产品需求完成上线时记录）';

-- 2. 数据迁移：为状态为"完成上线"(30)的产品需求填充上线时间
UPDATE product_demand pd
LEFT JOIN (
  SELECT 
    ppd.product_demand_id,
    MAX(p.actual_end_date) AS latest_actual_end_date
  FROM project_product_demand ppd
  INNER JOIN project p ON p.id = ppd.project_id 
    AND p.is_deleted = 0
    AND p.actual_end_date IS NOT NULL
  WHERE ppd.is_deleted = 0
  GROUP BY ppd.product_demand_id
) proj ON proj.product_demand_id = pd.id
SET pd.online_time = COALESCE(proj.latest_actual_end_date, pd.modify_date)
WHERE pd.status = 30
  AND pd.is_deleted = 0
  AND pd.online_time IS NULL;

-- 3. 验证数据迁移结果
SELECT 
  COUNT(*) AS total_online_demands,
  COUNT(online_time) AS has_online_time,
  COUNT(*) - COUNT(online_time) AS missing_online_time
FROM product_demand
WHERE status = 30 AND is_deleted = 0;

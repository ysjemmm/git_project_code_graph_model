-- ============================================================
-- 产品需求上线时间字段新增及数据迁移
-- 功能：记录产品需求实际上线时间
-- 迁移逻辑：
--   1. 已结项的项目：使用项目结项时间
--   2. 未结项的项目：使用当前时间
-- ============================================================

-- 1. 给 product_demand 表新增上线时间字段
ALTER TABLE product_demand
ADD COLUMN online_time DATETIME DEFAULT NULL
COMMENT '上线时间（产品需求完成上线时记录）';

-- 2. 数据迁移：为状态为"完成上线"(30)的产品需求填充上线时间
-- 逻辑：
--   - 如果产品需求关联的项目已结项，使用项目结项时间
--   - 如果产品需求关联的项目未结项，使用当前时间
--   - 如果产品需求未关联项目，使用当前时间

UPDATE product_demand pd
LEFT JOIN (
  -- 查找每个产品需求关联的项目中最新的结项时间
  SELECT 
    ppd.product_demand_id,
    MAX(p.conclusion_date) AS latest_conclusion_date
  FROM project_product_demand ppd
  INNER JOIN project p ON p.id = ppd.project_id 
    AND p.is_deleted = 0
    AND p.conclusion_date IS NOT NULL  -- 只取已结项的项目
  WHERE ppd.is_deleted = 0
  GROUP BY ppd.product_demand_id
) proj ON proj.product_demand_id = pd.id
SET pd.online_time = COALESCE(proj.latest_conclusion_date, NOW())
WHERE pd.status = 30  -- 完成上线状态
  AND pd.is_deleted = 0
  AND pd.online_time IS NULL;  -- 只更新未设置上线时间的记录

-- 3. 验证数据迁移结果
SELECT 
  COUNT(*) AS total_online_demands,
  COUNT(online_time) AS has_online_time,
  COUNT(*) - COUNT(online_time) AS missing_online_time
FROM product_demand
WHERE status = 30 AND is_deleted = 0;

-- 4. 查看迁移详情（可选，用于验证）
-- SELECT 
--   pd.id,
--   pd.name,
--   pd.status,
--   pd.online_time,
--   GROUP_CONCAT(DISTINCT p.name) AS related_projects,
--   GROUP_CONCAT(DISTINCT p.conclusion_date) AS conclusion_dates
-- FROM product_demand pd
-- LEFT JOIN project_product_demand ppd ON ppd.product_demand_id = pd.id AND ppd.is_deleted = 0
-- LEFT JOIN project p ON p.id = ppd.project_id AND p.is_deleted = 0
-- WHERE pd.status = 30 AND pd.is_deleted = 0
-- GROUP BY pd.id
-- ORDER BY pd.id DESC
-- LIMIT 20;

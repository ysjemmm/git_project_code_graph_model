-- 修复产品线 id=124 下已关联项目的产品需求 online_time = 项目 actual_end_date
-- 范围：online_time 在 2026-02-15 ~ 2026-02-28 之间的数据

-- 诊断
SELECT pd.id, pd.name, pd.online_time AS old_online_time, p.actual_end_date AS new_online_time, p.name AS project_name
FROM product_demand pd
INNER JOIN project_product_demand ppd ON pd.id = ppd.product_demand_id AND ppd.is_deleted = 0
INNER JOIN project p ON ppd.project_id = p.id AND p.is_deleted = 0
WHERE pd.product_line_id = 124
  AND pd.is_deleted = 0
  AND pd.status = 30
  AND p.actual_end_date IS NOT NULL
  AND pd.online_time BETWEEN '2026-02-25 00:00:00' AND '2026-02-28 23:59:59';

-- 执行
UPDATE product_demand pd
INNER JOIN project_product_demand ppd ON pd.id = ppd.product_demand_id AND ppd.is_deleted = 0
INNER JOIN project p ON ppd.project_id = p.id AND p.is_deleted = 0
SET pd.online_time = p.actual_end_date,
    pd.modify_date = pd.modify_date
WHERE pd.product_line_id = 124
  AND pd.is_deleted = 0
  AND pd.status = 30
  AND p.actual_end_date IS NOT NULL
  AND pd.online_time BETWEEN '2026-02-25 00:00:00' AND '2026-02-28 23:59:59';

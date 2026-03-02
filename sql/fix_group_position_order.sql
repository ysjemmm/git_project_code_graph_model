-- ============================================================
-- 修复产品需求规划分组排序：让新分组排前面、旧分组排后面
--
-- 逻辑：
--   ORDER BY ASC → 旧分组行号小 → position 小 → 排后面
--                → 新分组行号大 → position 大 → 排前面
--   查询 ORDER BY position DESC → 新的在前，旧的在后 ✓
--
-- 注意：表上有唯一索引 (biz_domain_group_id, position, is_deleted)
--       需要先清空 position 到临时值，避免冲突
--
-- 需要手动在 MySQL 客户端分别执行三段
-- ============================================================


-- ==================== 业务域集 26 ====================

-- Step 1: 先把 position 设成基于 id 的临时值（避免唯一索引冲突）
UPDATE product_demand_group
SET position = id * -1
WHERE biz_domain_group_id = 26
  AND is_deleted = 0;

-- Step 2: 重新分配 position
SET @row_num = 0;

UPDATE product_demand_group pdg
INNER JOIN (
    SELECT
        id,
        (@row_num := @row_num + 1) * 1000 AS new_position
    FROM product_demand_group,
         (SELECT @row_num := 0) r
    WHERE biz_domain_group_id = 26
      AND is_deleted = 0
    ORDER BY SUBSTRING_INDEX(name, '-', 1) ASC, name ASC
) ranked ON pdg.id = ranked.id
SET pdg.position = ranked.new_position;


-- ==================== 业务域集 47 ====================

UPDATE product_demand_group
SET position = id * -1
WHERE biz_domain_group_id = 47
  AND is_deleted = 0;

SET @row_num = 0;

UPDATE product_demand_group pdg
INNER JOIN (
    SELECT
        id,
        (@row_num := @row_num + 1) * 1000 AS new_position
    FROM product_demand_group,
         (SELECT @row_num := 0) r
    WHERE biz_domain_group_id = 47
      AND is_deleted = 0
    ORDER BY SUBSTRING_INDEX(name, '-', 1) ASC, name ASC
) ranked ON pdg.id = ranked.id
SET pdg.position = ranked.new_position;


-- ==================== 业务域集 46 ====================

UPDATE product_demand_group
SET position = id * -1
WHERE biz_domain_group_id = 46
  AND is_deleted = 0;

SET @row_num = 0;

UPDATE product_demand_group pdg
INNER JOIN (
    SELECT
        id,
        (@row_num := @row_num + 1) * 1000 AS new_position
    FROM product_demand_group,
         (SELECT @row_num := 0) r
    WHERE biz_domain_group_id = 46
      AND is_deleted = 0
    ORDER BY SUBSTRING_INDEX(name, '-', 1) ASC, name ASC
) ranked ON pdg.id = ranked.id
SET pdg.position = ranked.new_position;


-- ==================== 验证 ====================

SELECT
    pdg.biz_domain_group_id,
    pdg.id AS group_id,
    pdg.name AS group_name,
    pdg.position,
    SUBSTRING_INDEX(pdg.name, '-', 1) AS date_prefix
FROM product_demand_group pdg
WHERE pdg.biz_domain_group_id IN (26, 47, 46)
  AND pdg.is_deleted = 0
ORDER BY pdg.biz_domain_group_id, pdg.position DESC;

-- ============================================================
-- 故事点估算模式：DDL 变更
-- biz_domain_group 新增 estimation_mode 字段
-- product_demand 新增 story_point 字段
-- ============================================================

-- 1. biz_domain_group 表新增估算模式字段
ALTER TABLE biz_domain_group
ADD COLUMN estimation_mode VARCHAR(20) NOT NULL DEFAULT 'resource'
COMMENT '估算模式：resource=传统资源规划, storyPoint=故事点';

-- 2. product_demand 表新增故事点字段
ALTER TABLE product_demand
ADD COLUMN story_point INT DEFAULT NULL
COMMENT '故事点（SP），正整数，仅故事点模式下使用';

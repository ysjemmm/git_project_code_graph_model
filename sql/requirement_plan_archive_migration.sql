-- 产品需求分组归档功能 - 数据库迁移脚本
-- 为 product_demand_group 表新增归档相关字段和索引

ALTER TABLE product_demand_group
ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已归档：0-未归档，1-已归档',
ADD COLUMN archived_time DATETIME DEFAULT NULL COMMENT '归档时间',
ADD INDEX idx_archived_biz_domain_group (archived, biz_domain_group_id);

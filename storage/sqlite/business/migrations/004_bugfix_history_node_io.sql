-- 为 bugfix_history 表增加链路快照字段
-- node_io_json: 完整的 nodeIO 数据（JSON），用于历史记录复刻
-- outcome: 链路走向 success | error_end | failed
ALTER TABLE bugfix_history ADD COLUMN node_io_json TEXT;
ALTER TABLE bugfix_history ADD COLUMN outcome TEXT NOT NULL DEFAULT '';

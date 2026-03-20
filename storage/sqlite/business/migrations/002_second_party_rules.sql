-- 二方包规则（第二方依赖识别/归属）

CREATE TABLE IF NOT EXISTS second_party_rules (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  enabled INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0,

  -- 匹配条件：groupId 与 artifactId 同时命中（AND）
  group_id_regex TEXT NOT NULL,
  artifact_id_regex TEXT NOT NULL,

  -- 目标端：映射到某个 project_type=Application 的 project_name（project_name 唯一）
  target_project_name TEXT NOT NULL,

  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_second_party_rules_enabled ON second_party_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_second_party_rules_sort_order ON second_party_rules(sort_order);


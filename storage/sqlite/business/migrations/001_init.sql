-- ============================================================
-- Business SQLite 全量初始化（幂等，IF NOT EXISTS）
-- 新环境只需执行此文件即可建好所有表
-- 已有数据库：schema_migrations 版本记录保证此文件不会重跑
-- ============================================================

-- ── 导入任务 ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS git_import_tasks (
  task_id TEXT PRIMARY KEY,
  status TEXT NOT NULL,
  priority TEXT NOT NULL,
  created_at TEXT NOT NULL,
  started_at TEXT,
  completed_at TEXT,
  retry_count INTEGER NOT NULL DEFAULT 0,
  max_retries INTEGER NOT NULL DEFAULT 3,
  max_workers INTEGER,
  task_json TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_git_import_tasks_status ON git_import_tasks(status);
CREATE INDEX IF NOT EXISTS idx_git_import_tasks_priority ON git_import_tasks(priority);

-- ── 二方包规则 ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS second_party_rules (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL DEFAULT '',
  enabled INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0,
  group_id_regex TEXT NOT NULL,
  artifact_id_regex TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_second_party_rules_enabled ON second_party_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_second_party_rules_sort_order ON second_party_rules(sort_order);

-- ── 应用项目缓存 ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS application_projects_cache (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  repo_name TEXT NOT NULL UNIQUE,
  project_name TEXT,
  project_key TEXT,
  project_type TEXT,
  repo_url TEXT,
  branch TEXT,
  commit_hash TEXT,
  last_update_time TEXT,
  cache_dir TEXT,
  repo_exists INTEGER NOT NULL DEFAULT 0,
  remote_url TEXT,
  head_branch TEXT,
  head_commit TEXT,
  dirty INTEGER,
  cache_size_mb REAL,
  merkle_branches TEXT,
  maven_scan_enabled INTEGER NOT NULL DEFAULT 1,
  force_maven INTEGER NOT NULL DEFAULT 0,
  clear_database INTEGER NOT NULL DEFAULT 0,
  auto_link_external INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_application_projects_last_update
  ON application_projects_cache(last_update_time);

-- ── pom.xml 解析出的应用依赖 ────────────────────────────────────
CREATE TABLE IF NOT EXISTS application_dependencies (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  app_id INTEGER NOT NULL,
  group_id TEXT NOT NULL DEFAULT '',
  artifact_id TEXT NOT NULL DEFAULT '',
  version TEXT NOT NULL DEFAULT '',
  scope TEXT NOT NULL DEFAULT '',
  parent_group_id TEXT NOT NULL DEFAULT '',
  parent_artifact_id TEXT NOT NULL DEFAULT '',
  parent_version TEXT NOT NULL DEFAULT '',
  is_second_party INTEGER NOT NULL DEFAULT 0,
  matched_rule_id INTEGER,
  scanned_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (app_id) REFERENCES application_projects_cache(id) ON DELETE CASCADE,
  FOREIGN KEY (matched_rule_id) REFERENCES second_party_rules(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_app_deps_app_id ON application_dependencies(app_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_app_deps_unique
  ON application_dependencies(app_id, group_id, artifact_id, version, scope);

-- ── 手动关联：pom 依赖 -> 已导入项目 ───────────────────────────
CREATE TABLE IF NOT EXISTS app_dependency_links (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  app_id INTEGER NOT NULL,
  group_id TEXT NOT NULL DEFAULT '',
  artifact_id TEXT NOT NULL DEFAULT '',
  linked_app_id INTEGER NOT NULL,
  dep_version TEXT NOT NULL DEFAULT '',
  note TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (app_id) REFERENCES application_projects_cache(id) ON DELETE CASCADE,
  FOREIGN KEY (linked_app_id) REFERENCES application_projects_cache(id) ON DELETE CASCADE,
  UNIQUE (app_id, group_id, artifact_id)
);

CREATE INDEX IF NOT EXISTS idx_dep_links_app_id ON app_dependency_links(app_id);

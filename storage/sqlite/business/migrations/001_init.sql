-- 业务基础表初始化
-- 由 storage/sqlite/business/business_db.py 的 migrate() 读取并执行

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


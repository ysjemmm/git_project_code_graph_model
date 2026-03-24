-- 任务验收 diff 明细持久化
CREATE TABLE IF NOT EXISTS import_task_delta_details (
  task_id TEXT PRIMARY KEY,
  detail_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (task_id) REFERENCES git_import_tasks(task_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_import_task_delta_updated_at
  ON import_task_delta_details(updated_at);

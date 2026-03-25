-- Bugfix 执行历史记录表
CREATE TABLE IF NOT EXISTS bugfix_history (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime')),
  project     TEXT    NOT NULL DEFAULT '',
  bug_id      TEXT    NOT NULL DEFAULT '',
  bug_title   TEXT    NOT NULL DEFAULT '',
  model       TEXT    NOT NULL DEFAULT '',
  depth       TEXT    NOT NULL DEFAULT '',
  status      TEXT    NOT NULL DEFAULT 'running',  -- running | success | failed
  duration_ms INTEGER,                              -- 耗时（毫秒），NULL 表示未完成
  session_id  TEXT,                                 -- 关联会话 ID
  prompt      TEXT,                                 -- 用户输入的问题描述
  git_url     TEXT,
  git_branch  TEXT,
  git_commit  TEXT
);

CREATE INDEX IF NOT EXISTS idx_bugfix_history_created_at ON bugfix_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bugfix_history_bug_id     ON bugfix_history(bug_id);

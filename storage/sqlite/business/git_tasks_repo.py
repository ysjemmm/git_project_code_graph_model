"""
GitImportTasksRepo

将 core/task_queue.py 的任务状态从 JSON/内存迁移到 SQLite。
这里采用“task_json + 部分字段冗余索引”的方式，便于未来扩字段而不频繁改表。
"""

from __future__ import annotations

import json
from dataclasses import asdict, is_dataclass
from typing import Any, Dict, Iterable, List, Optional

from storage.sqlite.business.business_db import BusinessSqliteDB, get_business_db, BusinessDBConfig


class GitImportTasksRepo:
    def __init__(self, db: BusinessSqliteDB):
        self.db = db

    @staticmethod
    def _normalize_task_json(task: Any) -> str:
        if task is None:
            return json.dumps({}, ensure_ascii=False)
        if is_dataclass(task):
            return json.dumps(asdict(task), ensure_ascii=False)
        if isinstance(task, dict):
            return json.dumps(task, ensure_ascii=False)
        # 兜底：尽量序列化，失败则转字符串
        try:
            return json.dumps(task, ensure_ascii=False)
        except Exception:
            return json.dumps({"_repr": repr(task)}, ensure_ascii=False)

    def upsert_task(
        self,
        *,
        task_id: str,
        status: str,
        priority: str,
        created_at: str,
        started_at: Optional[str],
        completed_at: Optional[str],
        retry_count: int,
        max_retries: int,
        task_obj: Any,
        updated_at: str,
    ) -> None:
        payload = self._normalize_task_json(task_obj)
        with self.db.transaction() as conn:
            conn.execute(
                """
                INSERT INTO git_import_tasks (
                  task_id, status, priority,
                  created_at, started_at, completed_at,
                  retry_count, max_retries,
                  task_json, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(task_id) DO UPDATE SET
                  status=excluded.status,
                  priority=excluded.priority,
                  started_at=excluded.started_at,
                  completed_at=excluded.completed_at,
                  retry_count=excluded.retry_count,
                  max_retries=excluded.max_retries,
                  task_json=excluded.task_json,
                  updated_at=excluded.updated_at
                """,
                (
                    task_id,
                    status,
                    priority,
                    created_at,
                    started_at,
                    completed_at,
                    retry_count,
                    max_retries,
                    payload,
                    updated_at,
                ),
            )

    def upsert_many(self, rows: Iterable[Dict[str, Any]]) -> None:
        sql = """
        INSERT INTO git_import_tasks (
          task_id, status, priority,
          created_at, started_at, completed_at,
          retry_count, max_retries,
          task_json, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(task_id) DO UPDATE SET
          status=excluded.status,
          priority=excluded.priority,
          started_at=excluded.started_at,
          completed_at=excluded.completed_at,
          retry_count=excluded.retry_count,
          max_retries=excluded.max_retries,
          task_json=excluded.task_json,
          updated_at=excluded.updated_at
        """
        with self.db.transaction() as conn:
            for r in rows:
                payload = self._normalize_task_json(r.get("task_obj") or r)
                conn.execute(
                    sql,
                    (
                        r["task_id"],
                        r["status"],
                        r["priority"],
                        r["created_at"],
                        r.get("started_at"),
                        r.get("completed_at"),
                        int(r.get("retry_count", 0)),
                        int(r.get("max_retries", 3)),
                        payload,
                        r["updated_at"],
                    ),
                )

    def load_tasks(self, *, statuses: List[str]) -> List[Dict[str, Any]]:
        """
        加载指定状态的任务，并解析 task_json。
        """
        conn = self.db.conn
        assert conn is not None
        cur = conn.cursor()
        placeholders = ",".join(["?"] * len(statuses)) if statuses else "''"
        q = f"SELECT task_json FROM git_import_tasks WHERE status IN ({placeholders})"
        rows = cur.execute(q, statuses).fetchall()
        out: List[Dict[str, Any]] = []
        for r in rows:
            try:
                out.append(json.loads(r["task_json"]))
            except Exception:
                out.append({"_task_json": r["task_json"]})
        return out

    def list_all_tasks(self) -> List[Dict[str, Any]]:
        conn = self.db.conn
        assert conn is not None
        cur = conn.cursor()
        rows = cur.execute("SELECT task_json FROM git_import_tasks").fetchall()
        out: List[Dict[str, Any]] = []
        for r in rows:
            try:
                out.append(json.loads(r["task_json"]))
            except Exception:
                out.append({"_task_json": r["task_json"]})
        return out

    def get_task(self, task_id: str) -> Optional[Dict[str, Any]]:
        conn = self.db.conn
        assert conn is not None
        row = conn.execute("SELECT task_json FROM git_import_tasks WHERE task_id = ?", (task_id,)).fetchone()
        if not row:
            return None
        try:
            return json.loads(row["task_json"])
        except Exception:
            return {"_task_json": row["task_json"]}

    def delete_task(self, task_id: str) -> None:
        conn = self.db.conn
        assert conn is not None
        with self.db.transaction():
            conn.execute("DELETE FROM git_import_tasks WHERE task_id = ?", (task_id,))


def get_git_import_tasks_repo(db_path: str | None = None) -> GitImportTasksRepo:
    cfg = BusinessDBConfig(db_path=db_path) if db_path else None
    db = get_business_db(cfg)
    return GitImportTasksRepo(db=db)


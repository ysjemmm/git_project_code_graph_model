"""
BugfixHistoryRepo — bugfix_history 表的增删查操作。
"""
from __future__ import annotations

import sqlite3
from dataclasses import dataclass
from typing import List, Optional

from .business_db import BusinessSqliteDB


@dataclass
class BugfixHistoryRecord:
    id: int
    created_at: str
    project: str
    bug_id: str
    bug_title: str
    model: str
    depth: str
    status: str             # running | success | failed
    duration_ms: Optional[int]
    session_id: Optional[str]
    prompt: Optional[str]
    git_url: Optional[str]
    git_branch: Optional[str]
    git_commit: Optional[str]
    node_io_json: Optional[str] = None   # 完整链路快照 JSON
    outcome: str = ''                    # success | error_end | failed

    def to_dict(self) -> dict:
        import json as _json
        duration_str: Optional[str] = None
        if self.duration_ms is not None:
            total_s = self.duration_ms // 1000
            duration_str = f"{total_s // 60}m {total_s % 60:02d}s"
        node_io = None
        if self.node_io_json:
            try:
                node_io = _json.loads(self.node_io_json)
            except Exception:
                pass
        return {
            "id":          str(self.id),
            "time":        self.created_at[:16],
            "project":     self.project,
            "bugId":       self.bug_id,
            "bugTitle":    self.bug_title,
            "model":       self.model,
            "depth":       self.depth,
            "status":      self.status,
            "outcome":     self.outcome,
            "duration":    duration_str,
            "sessionId":   self.session_id,
            "prompt":      self.prompt,
            "gitUrl":      self.git_url,
            "gitBranch":   self.git_branch,
            "gitCommit":   self.git_commit,
            "nodeIo":      node_io,
        }

    @classmethod
    def from_row(cls, row: sqlite3.Row) -> "BugfixHistoryRecord":
        keys = row.keys()
        return cls(
            id=row["id"],
            created_at=row["created_at"],
            project=row["project"] or "",
            bug_id=row["bug_id"] or "",
            bug_title=row["bug_title"] or "",
            model=row["model"] or "",
            depth=row["depth"] or "",
            status=row["status"] or "running",
            duration_ms=row["duration_ms"],
            session_id=row["session_id"],
            prompt=row["prompt"],
            git_url=row["git_url"],
            git_branch=row["git_branch"],
            git_commit=row["git_commit"],
            node_io_json=row["node_io_json"] if "node_io_json" in keys else None,
            outcome=row["outcome"] if "outcome" in keys else "",
        )


class BugfixHistoryRepo:
    def __init__(self, db: BusinessSqliteDB):
        self._db = db

    def create(
        self,
        *,
        project: str,
        bug_id: str,
        bug_title: str,
        model: str,
        depth: str,
        status: str = "running",
        duration_ms: Optional[int] = None,
        session_id: Optional[str] = None,
        prompt: Optional[str] = None,
        git_url: Optional[str] = None,
        git_branch: Optional[str] = None,
        git_commit: Optional[str] = None,
        node_io_json: Optional[str] = None,
        outcome: str = "",
    ) -> int:
        """插入一条历史记录，返回新记录的 id。"""
        assert self._db.conn is not None
        cur = self._db.conn.execute(
            """
            INSERT INTO bugfix_history
              (project, bug_id, bug_title, model, depth, status, duration_ms,
               session_id, prompt, git_url, git_branch, git_commit,
               node_io_json, outcome)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (project, bug_id, bug_title, model, depth, status, duration_ms,
             session_id, prompt, git_url, git_branch, git_commit,
             node_io_json, outcome),
        )
        self._db.conn.commit()
        return cur.lastrowid  # type: ignore[return-value]

    def update_status(
        self,
        record_id: int,
        status: str,
        duration_ms: Optional[int] = None,
        node_io_json: Optional[str] = None,
        outcome: Optional[str] = None,
    ) -> None:
        """更新记录状态（执行完成后调用）。"""
        assert self._db.conn is not None
        sets = ["status=?", "duration_ms=?"]
        params: list = [status, duration_ms]
        if node_io_json is not None:
            sets.append("node_io_json=?")
            params.append(node_io_json)
        if outcome is not None:
            sets.append("outcome=?")
            params.append(outcome)
        params.append(record_id)
        self._db.conn.execute(
            f"UPDATE bugfix_history SET {', '.join(sets)} WHERE id=?", params
        )
        self._db.conn.commit()

    def list_records(
        self,
        *,
        page: int = 1,
        page_size: int = 20,
        project: Optional[str] = None,
        bug_id: Optional[str] = None,
    ) -> tuple[int, List[BugfixHistoryRecord]]:
        """分页查询，返回 (total, items)。"""
        assert self._db.conn is not None
        conditions: list[str] = []
        params: list = []
        if project:
            conditions.append("project = ?")
            params.append(project)
        if bug_id:
            conditions.append("bug_id = ?")
            params.append(bug_id)

        where = ("WHERE " + " AND ".join(conditions)) if conditions else ""

        total: int = self._db.conn.execute(
            f"SELECT COUNT(*) AS cnt FROM bugfix_history {where}", params
        ).fetchone()["cnt"]

        offset = (page - 1) * page_size
        rows = self._db.conn.execute(
            f"""
            SELECT * FROM bugfix_history {where}
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """,
            params + [page_size, offset],
        ).fetchall()

        return total, [BugfixHistoryRecord.from_row(r) for r in rows]

    def get_by_id(self, record_id: int) -> Optional[BugfixHistoryRecord]:
        assert self._db.conn is not None
        row = self._db.conn.execute(
            "SELECT * FROM bugfix_history WHERE id=? LIMIT 1", (record_id,)
        ).fetchone()
        return BugfixHistoryRecord.from_row(row) if row else None

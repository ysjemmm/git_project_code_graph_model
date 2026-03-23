from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from storage.sqlite.business.business_db import BusinessSqliteDB


def _now() -> str:
    return datetime.now().isoformat()


class AppDependencyLinksRepo:
    def __init__(self, db: BusinessSqliteDB):
        self.db = db
        self._ensure_dep_version_column()

    def _ensure_dep_version_column(self) -> None:
        """向前兼容：若 dep_version 列不存在则添加。"""
        try:
            cur = self.db.conn.cursor()
            cols = [row[1] for row in cur.execute("PRAGMA table_info(app_dependency_links)").fetchall()]
            if "dep_version" not in cols:
                cur.execute("ALTER TABLE app_dependency_links ADD COLUMN dep_version TEXT NOT NULL DEFAULT ''")
                self.db.conn.commit()
        except Exception:
            pass

    def list_by_app(self, app_id: int) -> List[Dict[str, Any]]:
        """返回项目 A 的所有手动关联记录，含关联项目的基本信息。"""
        rows = self.db.conn.cursor().execute(
            """
            SELECT l.id, l.app_id, l.group_id, l.artifact_id, l.linked_app_id,
                   l.dep_version, l.note, l.created_at,
                   p.project_name AS linked_project_name,
                   p.repo_name    AS linked_repo_name
            FROM app_dependency_links l
            JOIN application_projects_cache p ON p.id = l.linked_app_id
            WHERE l.app_id = ?
            ORDER BY l.group_id, l.artifact_id
            """,
            (app_id,),
        ).fetchall()
        return [dict(r) for r in rows]

    def upsert(
        self,
        app_id: int,
        group_id: str,
        artifact_id: str,
        linked_app_id: int,
        dep_version: str = "",
        note: str = "",
    ) -> int:
        """新增或更新一条关联（同一 app_id+group_id+artifact_id 唯一）。返回记录 id。"""
        with self.db.transaction() as conn:
            conn.execute(
                """
                INSERT INTO app_dependency_links (app_id, group_id, artifact_id, linked_app_id, dep_version, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(app_id, group_id, artifact_id)
                DO UPDATE SET linked_app_id = excluded.linked_app_id,
                              dep_version = excluded.dep_version,
                              note = excluded.note
                """,
                (app_id, group_id, artifact_id, linked_app_id, dep_version or "", note or "", _now()),
            )
            row = conn.execute(
                "SELECT id FROM app_dependency_links WHERE app_id=? AND group_id=? AND artifact_id=?",
                (app_id, group_id, artifact_id),
            ).fetchone()
            return row["id"] if row else -1

    def delete(self, link_id: int) -> bool:
        """删除一条关联记录。"""
        with self.db.transaction() as conn:
            conn.execute("DELETE FROM app_dependency_links WHERE id = ?", (link_id,))
        return True

    def get_all_for_neo4j_sync(self) -> List[Dict[str, Any]]:
        """
        返回所有关联记录，含双方项目名，用于同步 DEPENDS_ON 边到 Neo4j。
        """
        rows = self.db.conn.cursor().execute(
            """
            SELECT l.group_id, l.artifact_id, l.dep_version, l.note,
                   a.project_name AS from_project,
                   b.project_name AS to_project
            FROM app_dependency_links l
            JOIN application_projects_cache a ON a.id = l.app_id
            JOIN application_projects_cache b ON b.id = l.linked_app_id
            """,
        ).fetchall()
        return [dict(r) for r in rows]

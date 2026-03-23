from __future__ import annotations

import json
from datetime import datetime
from typing import Any, Dict, List, Optional

from api.schemas import CacheProjectItem
from storage.sqlite.business.business_db import BusinessSqliteDB


def _now() -> str:
    return datetime.now().isoformat()


class ApplicationProjectsRepo:
    def __init__(self, db: BusinessSqliteDB):
        self.db = db
        self._ensure_auto_link_column()

    def _ensure_auto_link_column(self) -> None:
        """向前兼容：若 auto_link_external 列不存在则添加。"""
        try:
            cur = self.db.conn.cursor()
            cols = [row[1] for row in cur.execute("PRAGMA table_info(application_projects_cache)").fetchall()]
            if "auto_link_external" not in cols:
                cur.execute("ALTER TABLE application_projects_cache ADD COLUMN auto_link_external INTEGER NOT NULL DEFAULT 1")
                self.db.conn.commit()
        except Exception:
            pass

    def _to_int_bool(self, v: Any) -> Optional[int]:
        if v is None:
            return None
        return 1 if bool(v) else 0

    def _to_merkle_json(self, branches: Any) -> str:
        if branches is None:
            return "[]"
        if isinstance(branches, list):
            return json.dumps(branches, ensure_ascii=False)
        return json.dumps(list(branches), ensure_ascii=False)

    def upsert_many(self, items: List[CacheProjectItem]) -> None:
        if not items:
            return
        rows: List[Dict[str, Any]] = []
        for it in items:
            rows.append(
                {
                    "repo_name": it.repo_name,
                    "project_name": it.project_name,
                    "project_key": it.project_key,
                    "project_type": it.project_type,
                    "repo_url": it.repo_url,
                    "branch": it.branch,
                    "commit_hash": it.commit_hash,
                    "last_update_time": it.last_update_time,
                    "cache_dir": it.cache_dir,
                    "repo_exists": 1 if bool(it.repo_exists) else 0,
                    "remote_url": it.remote_url,
                    "head_branch": it.head_branch,
                    "head_commit": it.head_commit,
                    "dirty": self._to_int_bool(it.dirty),
                    "cache_size_mb": it.cache_size_mb,
                    "merkle_branches": self._to_merkle_json(getattr(it, "merkle_branches", None)),
                }
            )

        sql = """
        INSERT INTO application_projects_cache (
          repo_name, project_name, project_key, project_type, repo_url,
          branch, commit_hash, last_update_time, cache_dir,
          repo_exists, remote_url, head_branch, head_commit, dirty,
          cache_size_mb, merkle_branches
        ) VALUES (
          :repo_name, :project_name, :project_key, :project_type, :repo_url,
          :branch, :commit_hash, :last_update_time, :cache_dir,
          :repo_exists, :remote_url, :head_branch, :head_commit, :dirty,
          :cache_size_mb, :merkle_branches
        )
        ON CONFLICT(repo_name) DO UPDATE SET
          project_name=excluded.project_name,
          project_key=excluded.project_key,
          project_type=excluded.project_type,
          repo_url=excluded.repo_url,
          branch=excluded.branch,
          commit_hash=excluded.commit_hash,
          last_update_time=excluded.last_update_time,
          cache_dir=excluded.cache_dir,
          repo_exists=excluded.repo_exists,
          remote_url=excluded.remote_url,
          head_branch=excluded.head_branch,
          head_commit=excluded.head_commit,
          dirty=excluded.dirty,
          cache_size_mb=excluded.cache_size_mb,
          merkle_branches=excluded.merkle_branches,
          updated_at=:updated_at
        """
        with self.db.transaction() as conn:
            conn.executemany(sql, [{**r, "updated_at": _now()} for r in rows])

    def list_all(self, *, include_libs: bool = False) -> List[CacheProjectItem]:
        where = ""
        if not include_libs:
            where = "WHERE lower(coalesce(project_type,'')) = 'application'"

        sql = f"""
        SELECT
          id, repo_name, project_name, project_key, project_type, repo_url,
          branch, commit_hash, last_update_time, cache_dir,
          repo_exists, remote_url, head_branch, head_commit, dirty,
          cache_size_mb, merkle_branches
        FROM application_projects_cache
        {where}
        ORDER BY last_update_time DESC, repo_name ASC
        """
        cur = self.db.conn.cursor()
        rows = cur.execute(sql).fetchall() or []

        out: List[CacheProjectItem] = []
        for r in rows:
            merkle_raw = r["merkle_branches"] if "merkle_branches" in r.keys() else "[]"
            try:
                merkle_branches = json.loads(merkle_raw) if merkle_raw else []
            except Exception:
                merkle_branches = []

            out.append(
                CacheProjectItem(
                    id=r["id"],
                    repo_name=r["repo_name"],
                    project_name=r["project_name"],
                    project_key=r["project_key"],
                    project_type=r["project_type"],
                    repo_url=r["repo_url"],
                    branch=r["branch"],
                    commit_hash=r["commit_hash"],
                    last_update_time=r["last_update_time"],
                    cache_dir=r["cache_dir"],
                    repo_exists=bool(r["repo_exists"]),
                    remote_url=r["remote_url"],
                    head_branch=r["head_branch"],
                    head_commit=r["head_commit"],
                    dirty=(
                        None
                        if r["dirty"] is None
                        else (bool(r["dirty"]) if isinstance(r["dirty"], (int, bool)) else bool(str(r["dirty"])))
                    ),
                    cache_size_mb=r["cache_size_mb"],
                    merkle_branches=merkle_branches if isinstance(merkle_branches, list) else [],
                )
            )
        return out

    def count(self) -> int:
        cur = self.db.conn.cursor()
        row = cur.execute("SELECT count(*) AS c FROM application_projects_cache").fetchone()
        return int(row["c"] if row else 0)

    def update_import_settings(
        self,
        app_id: int,
        *,
        maven_scan_enabled: Optional[bool] = None,
        force_maven: Optional[bool] = None,
        clear_database: Optional[bool] = None,
        auto_link_external: Optional[bool] = None,
    ) -> bool:
        """仅更新导入相关的开关字段，返回是否找到并更新了记录。"""
        sets = []
        params: List[Any] = []
        if maven_scan_enabled is not None:
            sets.append("maven_scan_enabled = ?")
            params.append(1 if maven_scan_enabled else 0)
        if force_maven is not None:
            sets.append("force_maven = ?")
            params.append(1 if force_maven else 0)
        if clear_database is not None:
            sets.append("clear_database = ?")
            params.append(1 if clear_database else 0)
        if auto_link_external is not None:
            sets.append("auto_link_external = ?")
            params.append(1 if auto_link_external else 0)
        if not sets:
            return False
        sets.append("updated_at = ?")
        params.append(_now())
        params.append(app_id)
        sql = f"UPDATE application_projects_cache SET {', '.join(sets)} WHERE id = ?"
        with self.db.transaction() as conn:
            cur = conn.execute(sql, params)
            return (cur.rowcount or 0) > 0

    def get_import_settings(self, app_id: int) -> Optional[Dict[str, Any]]:
        cur = self.db.conn.cursor()
        # 向前兼容：auto_link_external 列可能不存在于旧库
        try:
            row = cur.execute(
                "SELECT maven_scan_enabled, force_maven, clear_database, auto_link_external FROM application_projects_cache WHERE id = ?",
                (app_id,),
            ).fetchone()
        except Exception:
            row = cur.execute(
                "SELECT maven_scan_enabled, force_maven, clear_database FROM application_projects_cache WHERE id = ?",
                (app_id,),
            ).fetchone()
            if row is not None:
                row = (*row, None)  # 补齐第4列
        if row is None:
            return None
        def _b(v: Any) -> bool:
            if v is None:
                return False
            return bool(v)
        return {
            "maven_scan_enabled": _b(row[0]) if row[0] is not None else True,
            "force_maven": _b(row[1]),
            "clear_database": _b(row[2]),
            "auto_link_external": _b(row[3]) if row[3] is not None else True,
        }

    def delete_by_id(self, app_id: int) -> None:
        with self.db.transaction() as conn:
            conn.execute("DELETE FROM application_projects_cache WHERE id = ?", (app_id,))

    def delete_by_repo_name(self, repo_name: str) -> None:
        with self.db.transaction() as conn:
            conn.execute("DELETE FROM application_projects_cache WHERE repo_name = ?", (repo_name,))


from __future__ import annotations

from datetime import datetime
import re
from typing import Any, Dict, List, Optional

from storage.sqlite.business.business_db import BusinessSqliteDB


def _now() -> str:
    return datetime.now().isoformat()


class AppDependenciesRepo:
    def __init__(self, db: BusinessSqliteDB):
        self.db = db
        self._ensure_matched_rule_id_column()
        self._ensure_parent_columns()

    def _ensure_matched_rule_id_column(self) -> None:
        """对已有数据库做向前兼容：若 matched_rule_id 列不存在则添加。"""
        try:
            cur = self.db.conn.cursor()
            cols = [row[1] for row in cur.execute("PRAGMA table_info(application_dependencies)").fetchall()]
            if "matched_rule_id" not in cols:
                cur.execute("ALTER TABLE application_dependencies ADD COLUMN matched_rule_id INTEGER")
                self.db.conn.commit()
        except Exception:
            pass

    def _ensure_parent_columns(self) -> None:
        """向前兼容：若 parent 相关列不存在则添加。"""
        try:
            cur = self.db.conn.cursor()
            cols = [row[1] for row in cur.execute("PRAGMA table_info(application_dependencies)").fetchall()]
            changed = False
            if "parent_group_id" not in cols:
                cur.execute("ALTER TABLE application_dependencies ADD COLUMN parent_group_id TEXT NOT NULL DEFAULT ''")
                changed = True
            if "parent_artifact_id" not in cols:
                cur.execute("ALTER TABLE application_dependencies ADD COLUMN parent_artifact_id TEXT NOT NULL DEFAULT ''")
                changed = True
            if "parent_version" not in cols:
                cur.execute("ALTER TABLE application_dependencies ADD COLUMN parent_version TEXT NOT NULL DEFAULT ''")
                changed = True
            if changed:
                self.db.conn.commit()
        except Exception:
            pass

    def replace_all(self, app_id: int, deps: List[Dict[str, Any]]) -> None:
        """删除旧记录，批量插入新记录。"""
        now = _now()
        with self.db.transaction() as conn:
            conn.execute("DELETE FROM application_dependencies WHERE app_id = ?", (app_id,))
            if deps:
                conn.executemany(
                    """
                    INSERT OR IGNORE INTO application_dependencies
                      (app_id, group_id, artifact_id, version, scope,
                       parent_group_id, parent_artifact_id, parent_version,
                       is_second_party, matched_rule_id, scanned_at)
                    VALUES (:app_id, :group_id, :artifact_id, :version, :scope,
                            :parent_group_id, :parent_artifact_id, :parent_version,
                            :is_second_party, :matched_rule_id, :scanned_at)
                    """,
                    [
                        {
                            "app_id": app_id,
                            "group_id": d.get("group_id", ""),
                            "artifact_id": d.get("artifact_id", ""),
                            "version": d.get("version", ""),
                            "scope": d.get("scope", "compile"),
                            "parent_group_id": d.get("parent_group_id", ""),
                            "parent_artifact_id": d.get("parent_artifact_id", ""),
                            "parent_version": d.get("parent_version", ""),
                            "is_second_party": 1 if d.get("is_second_party") else 0,
                            "matched_rule_id": d.get("matched_rule_id"),
                            "scanned_at": now,
                        }
                        for d in deps
                    ],
                )

    def list_by_app(self, app_id: int) -> List[Dict[str, Any]]:
        cur = self.db.conn.cursor()
        rows = cur.execute(
            """
            SELECT d.group_id, d.artifact_id, d.version, d.scope,
                   d.parent_group_id, d.parent_artifact_id, d.parent_version,
                   d.is_second_party, d.scanned_at, d.matched_rule_id,
                   r.name AS rule_name,
                   r.group_id_regex AS rule_group_id_regex,
                   r.artifact_id_regex AS rule_artifact_id_regex,
                   r.sort_order AS rule_sort_order
            FROM application_dependencies d
            LEFT JOIN second_party_rules r ON r.id = d.matched_rule_id
            WHERE d.app_id = ?
            ORDER BY d.is_second_party DESC, d.group_id, d.artifact_id
            """,
            (app_id,),
        ).fetchall()
        items = [
            {
                "group_id": r["group_id"],
                "artifact_id": r["artifact_id"],
                "version": r["version"],
                "scope": r["scope"],
                "parent_group_id": r["parent_group_id"],
                "parent_artifact_id": r["parent_artifact_id"],
                "parent_version": r["parent_version"],
                "is_second_party": bool(r["is_second_party"]),
                "scanned_at": r["scanned_at"],
                "matched_rule_id": r["matched_rule_id"],
                "matched_rule": {
                    "name": r["rule_name"] or "",
                    "group_id_regex": r["rule_group_id_regex"],
                    "artifact_id_regex": r["rule_artifact_id_regex"],
                    "sort_order": r["rule_sort_order"],
                } if r["matched_rule_id"] is not None else None,
            }
            for r in rows
        ]

        def _is_parent_like(artifact_id: str, scope: str) -> bool:
            a = str(artifact_id or "").lower()
            s = str(scope or "").lower()
            if s == "import":
                return True
            return a.endswith("-parent") or a.endswith("-bom") or a.endswith("-dependencies")

        # 先构建“父级/BOM 二方包”候选，供详情页展示“识别依据”
        parent_candidates: List[Dict[str, Any]] = [
            x for x in items
            if x["is_second_party"] and _is_parent_like(x.get("artifact_id", ""), x.get("scope", ""))
        ]

        for x in items:
            if not x.get("is_second_party"):
                x["matched_via"] = "none"
                x["parent_dependency"] = None
                continue

            mr = x.get("matched_rule") or {}
            gid_rule = str(mr.get("group_id_regex") or "")
            aid_rule = str(mr.get("artifact_id_regex") or "")
            gid = str(x.get("group_id") or "")
            aid = str(x.get("artifact_id") or "")
            pgid = str(x.get("parent_group_id") or "")
            paid = str(x.get("parent_artifact_id") or "")
            direct = False
            parent_rule = False
            try:
                direct = bool(gid_rule and aid_rule and re.search(gid_rule, gid) and re.search(aid_rule, aid))
                parent_rule = bool(
                    (not direct)
                    and gid_rule
                    and aid_rule
                    and pgid
                    and paid
                    and re.search(gid_rule, pgid)
                    and re.search(aid_rule, paid)
                )
            except Exception:
                direct = False
                parent_rule = False

            if direct:
                x["matched_via"] = "direct"
                x["parent_dependency"] = None
                continue

            if parent_rule:
                x["matched_via"] = "parent"
                x["parent_dependency"] = {
                    "group_id": pgid,
                    "artifact_id": paid,
                    "version": str(x.get("parent_version") or ""),
                    "scope": "",
                }
                continue

            parent = None
            for p in parent_candidates:
                if str(p.get("group_id") or "") != gid:
                    continue
                pv = str(p.get("version") or "")
                xv = str(x.get("version") or "")
                if pv and xv and pv != xv:
                    continue
                parent = p
                break

            if parent is not None:
                x["matched_via"] = "parent"
                x["parent_dependency"] = {
                    "group_id": parent.get("group_id", ""),
                    "artifact_id": parent.get("artifact_id", ""),
                    "version": parent.get("version", ""),
                    "scope": parent.get("scope", ""),
                }
            else:
                x["matched_via"] = "direct"
                x["parent_dependency"] = None

        return items

    def scanned_at(self, app_id: int) -> Optional[str]:
        """返回最近一次扫描时间。"""
        cur = self.db.conn.cursor()
        row = cur.execute(
            "SELECT MAX(scanned_at) AS t FROM application_dependencies WHERE app_id = ?",
            (app_id,),
        ).fetchone()
        return row["t"] if row else None

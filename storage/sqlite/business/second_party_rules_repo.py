"""
二方包规则仓储
"""

from __future__ import annotations

import sqlite3
from datetime import datetime
from typing import Any, Dict, List, Optional

from .business_db import BusinessSqliteDB


class SecondPartyRulesRepo:
    def __init__(self, db: BusinessSqliteDB):
        self.db = db
        self._ensure_name_column()

    def _ensure_name_column(self) -> None:
        """对已有数据库向前兼容：若 name 列不存在则添加。"""
        try:
            cur = self.db.conn.cursor()
            cols = [row[1] for row in cur.execute("PRAGMA table_info(second_party_rules)").fetchall()]
            if "name" not in cols:
                cur.execute("ALTER TABLE second_party_rules ADD COLUMN name TEXT NOT NULL DEFAULT ''")
                self.db.conn.commit()
        except Exception:
            pass

    @staticmethod
    def _row_to_rule(row: sqlite3.Row) -> Dict[str, Any]:
        return {
            "id": int(row["id"]),
            "name": row["name"] if "name" in row.keys() else "",
            "enabled": bool(int(row["enabled"])),
            "sort_order": int(row["sort_order"]),
            "group_id_regex": row["group_id_regex"],
            "artifact_id_regex": row["artifact_id_regex"],
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
        }

    def list_rules(self) -> List[Dict[str, Any]]:
        cur = self.db.conn.cursor()
        rows = cur.execute(
            """
            SELECT id, name, enabled, sort_order, group_id_regex, artifact_id_regex, created_at, updated_at
            FROM second_party_rules
            ORDER BY sort_order ASC, id DESC
            """
        ).fetchall()
        return [self._row_to_rule(r) for r in rows]

    def create_rule(
        self,
        *,
        name: str,
        enabled: bool,
        sort_order: int,
        group_id_regex: str,
        artifact_id_regex: str,
    ) -> Dict[str, Any]:
        now = datetime.now().isoformat()
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute(
                """
                INSERT INTO second_party_rules (
                  name, enabled, sort_order,
                  group_id_regex, artifact_id_regex,
                  created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (name, 1 if enabled else 0, int(sort_order), group_id_regex, artifact_id_regex, now, now),
            )
            rule_id = int(cur.lastrowid)
            row = cur.execute(
                "SELECT id, name, enabled, sort_order, group_id_regex, artifact_id_regex, created_at, updated_at "
                "FROM second_party_rules WHERE id = ?",
                (rule_id,),
            ).fetchone()
            if row is None:
                raise RuntimeError("failed to fetch created rule")
            return self._row_to_rule(row)

    def update_rule(
        self,
        rule_id: int,
        *,
        name: str,
        enabled: bool,
        sort_order: int,
        group_id_regex: str,
        artifact_id_regex: str,
    ) -> Optional[Dict[str, Any]]:
        now = datetime.now().isoformat()
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute(
                """
                UPDATE second_party_rules
                SET name = ?, enabled = ?, sort_order = ?,
                    group_id_regex = ?, artifact_id_regex = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                (name, 1 if enabled else 0, int(sort_order), group_id_regex, artifact_id_regex, now, int(rule_id)),
            )
            if cur.rowcount <= 0:
                return None
            row = cur.execute(
                "SELECT id, name, enabled, sort_order, group_id_regex, artifact_id_regex, created_at, updated_at "
                "FROM second_party_rules WHERE id = ?",
                (int(rule_id),),
            ).fetchone()
            return self._row_to_rule(row) if row else None

    def delete_rule(self, rule_id: int) -> bool:
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute("DELETE FROM second_party_rules WHERE id = ?", (int(rule_id),))
            return cur.rowcount > 0


def get_second_party_rules_repo(db: BusinessSqliteDB) -> SecondPartyRulesRepo:
    return SecondPartyRulesRepo(db=db)

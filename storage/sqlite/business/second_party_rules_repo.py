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

    @staticmethod
    def _row_to_rule(row: sqlite3.Row) -> Dict[str, Any]:
        return {
            "id": int(row["id"]),
            "enabled": bool(int(row["enabled"])),
            "sort_order": int(row["sort_order"]),
            "group_id_regex": row["group_id_regex"],
            "artifact_id_regex": row["artifact_id_regex"],
            "target_project_name": row["target_project_name"],
            "created_at": row["created_at"],
            "updated_at": row["updated_at"],
        }

    def list_rules(self) -> List[Dict[str, Any]]:
        conn = self.db.conn
        assert conn is not None
        cur = conn.cursor()
        rows = cur.execute(
            """
            SELECT id, enabled, sort_order, group_id_regex, artifact_id_regex, target_project_name, created_at, updated_at
            FROM second_party_rules
            ORDER BY sort_order ASC, id DESC
            """
        ).fetchall()
        return [self._row_to_rule(r) for r in rows]

    def create_rule(
        self,
        *,
        enabled: bool,
        sort_order: int,
        group_id_regex: str,
        artifact_id_regex: str,
        target_project_name: str,
    ) -> Dict[str, Any]:
        now = datetime.now().isoformat()
        conn = self.db.conn
        assert conn is not None
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute(
                """
                INSERT INTO second_party_rules (
                  enabled, sort_order,
                  group_id_regex, artifact_id_regex,
                  target_project_name,
                  created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    1 if enabled else 0,
                    int(sort_order),
                    group_id_regex,
                    artifact_id_regex,
                    target_project_name,
                    now,
                    now,
                ),
            )
            rule_id = int(cur.lastrowid)
            row = cur.execute(
                """
                SELECT id, enabled, sort_order, group_id_regex, artifact_id_regex, target_project_name, created_at, updated_at
                FROM second_party_rules
                WHERE id = ?
                """,
                (rule_id,),
            ).fetchone()
            if row is None:
                raise RuntimeError("failed to fetch created rule")
            return self._row_to_rule(row)

    def update_rule(
        self,
        rule_id: int,
        *,
        enabled: bool,
        sort_order: int,
        group_id_regex: str,
        artifact_id_regex: str,
        target_project_name: str,
    ) -> Optional[Dict[str, Any]]:
        now = datetime.now().isoformat()
        conn = self.db.conn
        assert conn is not None
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute(
                """
                UPDATE second_party_rules
                SET enabled = ?, sort_order = ?,
                    group_id_regex = ?, artifact_id_regex = ?,
                    target_project_name = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                (
                    1 if enabled else 0,
                    int(sort_order),
                    group_id_regex,
                    artifact_id_regex,
                    target_project_name,
                    now,
                    int(rule_id),
                ),
            )
            if cur.rowcount <= 0:
                return None
            row = cur.execute(
                """
                SELECT id, enabled, sort_order, group_id_regex, artifact_id_regex, target_project_name, created_at, updated_at
                FROM second_party_rules
                WHERE id = ?
                """,
                (int(rule_id),),
            ).fetchone()
            if row is None:
                return None
            return self._row_to_rule(row)

    def delete_rule(self, rule_id: int) -> bool:
        conn = self.db.conn
        assert conn is not None
        with self.db.transaction() as tx:
            cur = tx.cursor()
            cur.execute("DELETE FROM second_party_rules WHERE id = ?", (int(rule_id),))
            return cur.rowcount > 0


def get_second_party_rules_repo(db: BusinessSqliteDB) -> SecondPartyRulesRepo:
    return SecondPartyRulesRepo(db=db)


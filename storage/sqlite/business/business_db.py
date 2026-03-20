"""
Business SQLite 基础层

提供：
- 统一的连接创建（WAL、row_factory、pragma）
- migrate：建表（幂等）
- transaction：事务上下文管理
"""

from __future__ import annotations

import sqlite3
import threading
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Iterator, Optional

from tools.constants import PROJECT_ROOT_PATH


@dataclass(frozen=True)
class BusinessDBConfig:
    db_path: str = str(PROJECT_ROOT_PATH / ".cache" / "business.db")


_singleton_lock = threading.Lock()
_singleton_instances: dict[str, "BusinessSqliteDB"] = {}


class BusinessSqliteDB:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.conn: Optional[sqlite3.Connection] = None
        self._connect()
        self.migrate()

    def _connect(self) -> None:
        db_file = Path(self.db_path)
        db_file.parent.mkdir(parents=True, exist_ok=True)
        self.conn = sqlite3.connect(self.db_path, check_same_thread=False)
        self.conn.row_factory = sqlite3.Row
        # 并发写读优化：WAL + 合理同步
        self.conn.execute("PRAGMA journal_mode=WAL")
        self.conn.execute("PRAGMA synchronous=NORMAL")
        self.conn.execute("PRAGMA foreign_keys=ON")

    def migrate(self) -> None:
        """
        建表（幂等）
        使用迁移文件驱动：按版本顺序读取 `migrations/*.sql` 并执行。
        """
        assert self.conn is not None
        cur = self.conn.cursor()

        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS schema_migrations (
              version INTEGER PRIMARY KEY,
              applied_at TEXT NOT NULL
            )
            """
        )

        # 读取已应用的迁移版本
        applied_versions = set()
        rows = cur.execute("SELECT version FROM schema_migrations").fetchall()
        for r in rows:
            try:
                applied_versions.add(int(r["version"]))
            except Exception:
                continue

        # 应用未应用的迁移文件
        migrations_dir = Path(__file__).resolve().parent / "migrations"
        if migrations_dir.exists():
            sql_files = sorted(
                list(migrations_dir.glob("*.sql")),
                key=lambda p: int(p.stem.split("_", 1)[0]) if p.stem and p.stem.split("_", 1)[0].isdigit() else 0,
            )
            for sql_path in sql_files:
                version_str = sql_path.stem.split("_", 1)[0]
                if not version_str.isdigit():
                    continue
                version = int(version_str)
                if version in applied_versions:
                    continue
                sql_text = sql_path.read_text(encoding="utf-8")
                cur.executescript(sql_text)
                cur.execute(
                    "INSERT INTO schema_migrations(version, applied_at) VALUES (?, ?)",
                    (version, datetime.now().isoformat()),
                )

        self.conn.commit()

    @contextmanager
    def transaction(self) -> Iterator[sqlite3.Connection]:
        """
        事务上下文管理：
        - COMMIT：成功提交
        - ROLLBACK：异常回滚
        """
        assert self.conn is not None
        try:
            self.conn.execute("BEGIN")
            yield self.conn
            self.conn.commit()
        except Exception:
            self.conn.rollback()
            raise

    def close(self) -> None:
        if self.conn is not None:
            self.conn.close()
            self.conn = None


def get_business_db(config: BusinessDBConfig | None = None) -> BusinessSqliteDB:
    cfg = config or BusinessDBConfig()
    with _singleton_lock:
        inst = _singleton_instances.get(cfg.db_path)
        if inst is not None:
            # 即使单例已存在，也要重新跑 migrate（基于 schema_migrations 幂等），
            # 这样新加迁移文件不需要重启服务即可生效。
            inst.migrate()
            return inst
        inst = BusinessSqliteDB(cfg.db_path)
        _singleton_instances[cfg.db_path] = inst
        return inst


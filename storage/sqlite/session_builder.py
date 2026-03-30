from contextlib import contextmanager
from pathlib import Path

from fastapi import Depends
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker, Session

from storage.sqlite.dao.project_dao import ProjectDAO
from tools.constants import CACHE_BUSINESS_DB_PATH


class SQLiteDatabases:
    def __init__(self, db_path: str):
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        self.db_path = db_path
        self.engine = self._create_engine()
        self._configure_pragmas()
        self.SessionLocal = sessionmaker(
            bind=self.engine,
            autocommit=False,
            autoflush=False,
        )

    def _create_engine(self):
        db_url = f"sqlite:///{self.db_path}"

        return create_engine(
            db_url,
            echo=False,

            # 连接池配置
            pool_size=10,
            max_overflow=20,
            pool_timeout=30,
            pool_recycle=3600,
            pool_pre_ping=True,

            connect_args={
                'check_same_thread': False,
                'timeout': 30,
                'uri': True,
            }
        )

    def _configure_pragmas(self):
        """通过事件配置 pragma"""

        @event.listens_for(self.engine, "connect")
        def set_pragma(dbapi_connection, connection_record):
            cursor = dbapi_connection.cursor()

            # 1. 启用 WAL 模式（关键）
            cursor.execute("PRAGMA journal_mode=WAL")

            # 2. 同步模式：NORMAL（性能与安全平衡）
            cursor.execute("PRAGMA synchronous=NORMAL")

            # 3. 缓存大小：10MB
            cursor.execute("PRAGMA cache_size=-10000")

            # 4. 临时文件存内存（可选，根据内存大小决定）
            cursor.execute("PRAGMA temp_store=MEMORY")

            # 5. 忙等待超时：30秒
            cursor.execute("PRAGMA busy_timeout=30000")

            cursor.close()

        # 触发事件注册
        _ = self.engine  # 确保事件已注册

    @contextmanager
    def get_session(self):
        """获取数据库会话（读写都用同一个）"""
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

_db = SQLiteDatabases(CACHE_BUSINESS_DB_PATH)

def get_sqlite_db():
    with _db.get_session() as session:
        yield session

def get_project_dao(session: Session = Depends(get_sqlite_db)):
    return ProjectDAO(session)
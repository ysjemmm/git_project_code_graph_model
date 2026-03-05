"""
项目类数据库模块
用于管理 SQLite 数据库中的项目 Java 类信息
"""
import sqlite3
import os
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional
from datetime import datetime

from tools.constants import PROJECT_ROOT_PATH


@dataclass
class ProjectClassInfo:
    """项目类信息"""
    fqn: str                    # 完全限定名
    simple_name: str            # 简单名称
    package_name: str           # 包名
    project_name: str           # 项目名称
    file_path: str              # 源文件路径
    relative_path: str          # 相对路径
    is_anonymous: bool          # 是否匿名类
    is_nested: bool             # 是否嵌套类
    parent_class: Optional[str] # 父类 FQN
    symbol_id: str              # 符号 ID
    parent_symbol_id: Optional[str]  # 父符号 ID
    insert_time: str            # 插入时间
    last_modified: str          # 文件修改时间


class ProjectClassDB:
    """项目类数据库（单例模式）"""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls, db_path: Optional[str] = None):
        """单例模式：确保只有一个实例"""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(ProjectClassDB, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self, db_path: Optional[str] = None):
        """
        初始化数据库连接（只初始化一次）
        
        参数:
            db_path: 数据库文件路径（默认使用 PROJECT_ROOT_PATH/.cache/project_classes.db）
        """
        # 避免重复初始化
        if self._initialized:
            return
        
        # 使用项目根路径
        if db_path is None:
            db_path = str(PROJECT_ROOT_PATH / ".cache" / "project_classes.db")
        
        self.db_path = db_path
        self.conn: Optional[sqlite3.Connection] = None
        
        # 确保数据库目录存在
        db_dir = Path(db_path).parent
        db_dir.mkdir(parents=True, exist_ok=True)
        
        # 连接数据库
        self._connect()
        self._initialized = True
    
    def _connect(self):
        """建立数据库连接"""
        if self.conn is None:
            self.conn = sqlite3.connect(self.db_path, check_same_thread=False)
            self.conn.row_factory = sqlite3.Row  # 使用字典式访问
            # 启用 WAL 模式以支持并发读写
            self.conn.execute("PRAGMA journal_mode=WAL")
            self.conn.execute("PRAGMA synchronous=NORMAL")
    
    def initialize_schema(self) -> None:
        """
        创建数据库表和索引
        
        创建:
            - project_classes 表
            - 索引：fqn、simple_name、project_name、file_path、parent_class
        """
        cursor = self.conn.cursor()
        
        # 创建 project_classes 表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS project_classes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fqn TEXT NOT NULL,
                simple_name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                project_name TEXT NOT NULL,
                file_path TEXT NOT NULL,
                relative_path TEXT NOT NULL,
                is_anonymous BOOLEAN NOT NULL,
                is_nested BOOLEAN NOT NULL,
                parent_class TEXT,
                symbol_id TEXT NOT NULL,
                parent_symbol_id TEXT,
                insert_time TEXT NOT NULL,
                last_modified TEXT NOT NULL,
                UNIQUE(project_name, fqn)
            )
        """)
        
        # 创建索引
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_fqn 
            ON project_classes(fqn)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_simple_name 
            ON project_classes(simple_name)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_project_name 
            ON project_classes(project_name)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_file_path 
            ON project_classes(file_path)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_parent_class 
            ON project_classes(parent_class)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_symbol_id 
            ON project_classes(symbol_id)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_parent_symbol_id 
            ON project_classes(parent_symbol_id)
        """)
        
        self.conn.commit()
    
    def update_from_file(
        self,
        project_name: str,
        file_path: str,
        classes: List[ProjectClassInfo]
    ) -> bool:
        """
        增量更新单个文件的类
        
        参数:
            project_name: 项目名称
            file_path: 文件路径
            classes: 类信息列表
        
        返回:
            True: 已更新
            False: 未修改，跳过
        """
        if not classes:
            return False
        
        # 获取文件修改时间
        try:
            file_mtime = datetime.fromtimestamp(os.path.getmtime(file_path))
            file_mtime_str = file_mtime.strftime('%Y-%m-%d %H:%M:%S')
        except OSError:
            return False
        
        cursor = self.conn.cursor()
        
        # 检查是否需要更新
        cursor.execute("""
            SELECT last_modified FROM project_classes 
            WHERE project_name = ? AND file_path = ? 
            LIMIT 1
        """, (project_name, file_path))
        
        row = cursor.fetchone()
        if row and row['last_modified'] >= file_mtime_str:
            return False  # 未修改，跳过
        
        # 删除该文件的所有类
        cursor.execute("""
            DELETE FROM project_classes 
            WHERE project_name = ? AND file_path = ?
        """, (project_name, file_path))
        
        # 插入新类
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        cursor.executemany("""
            INSERT OR REPLACE INTO project_classes 
            (fqn, simple_name, package_name, project_name, file_path, 
             relative_path, is_anonymous, is_nested, parent_class, 
             symbol_id, parent_symbol_id, insert_time, last_modified)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, [
            (c.fqn, c.simple_name, c.package_name, project_name, file_path,
             c.relative_path, c.is_anonymous, c.is_nested, c.parent_class,
             c.symbol_id, c.parent_symbol_id, current_time, file_mtime_str)
            for c in classes
        ])
        
        self.conn.commit()
        return True
    
    def query_by_fqn(
        self,
        fqn: str,
        project_name: Optional[str] = None
    ) -> Optional[ProjectClassInfo]:
        """
        按 FQN 查询
        
        参数:
            fqn: 完全限定名
            project_name: 项目名称（可选，如果不指定则查询所有项目）
        
        返回:
            匹配的 ProjectClassInfo 对象，如果不存在则返回 None
        """
        cursor = self.conn.cursor()
        
        if project_name:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE project_name = ? AND fqn = ?
            """, (project_name, fqn))
        else:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE fqn = ?
                ORDER BY insert_time DESC
                LIMIT 1
            """, (fqn,))
        
        row = cursor.fetchone()
        if row:
            return ProjectClassInfo(
                fqn=row['fqn'],
                simple_name=row['simple_name'],
                package_name=row['package_name'],
                project_name=row['project_name'],
                file_path=row['file_path'],
                relative_path=row['relative_path'],
                is_anonymous=bool(row['is_anonymous']),
                is_nested=bool(row['is_nested']),
                parent_class=row['parent_class'],
                symbol_id=row['symbol_id'],
                parent_symbol_id=row['parent_symbol_id'],
                insert_time=row['insert_time'],
                last_modified=row['last_modified']
            )
        return None
    
    def query_by_simple_name(
        self,
        simple_name: str,
        project_name: str,
        include_anonymous: bool = False
    ) -> List[ProjectClassInfo]:
        """
        按简单名称查询
        
        参数:
            simple_name: 简单类名
            project_name: 项目名称
            include_anonymous: 是否包含匿名类（默认False）
        
        返回:
            匹配的 ProjectClassInfo 对象列表
        """
        cursor = self.conn.cursor()
        
        if include_anonymous:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE project_name = ? AND simple_name = ?
                ORDER BY package_name
            """, (project_name, simple_name))
        else:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE project_name = ? AND simple_name = ? AND is_anonymous = 0
                ORDER BY package_name
            """, (project_name, simple_name))
        
        rows = cursor.fetchall()
        return [
            ProjectClassInfo(
                fqn=row['fqn'],
                simple_name=row['simple_name'],
                package_name=row['package_name'],
                project_name=row['project_name'],
                file_path=row['file_path'],
                relative_path=row['relative_path'],
                is_anonymous=bool(row['is_anonymous']),
                is_nested=bool(row['is_nested']),
                parent_class=row['parent_class'],
                symbol_id=row['symbol_id'],
                parent_symbol_id=row['parent_symbol_id'],
                insert_time=row['insert_time'],
                last_modified=row['last_modified']
            )
            for row in rows
        ]
    
    def query_by_project(
        self,
        project_name: str,
        include_anonymous: bool = False
    ) -> List[ProjectClassInfo]:
        """
        查询项目的所有类
        
        参数:
            project_name: 项目名称
            include_anonymous: 是否包含匿名类（默认False）
        
        返回:
            匹配的 ProjectClassInfo 对象列表
        """
        cursor = self.conn.cursor()
        
        if include_anonymous:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE project_name = ?
                ORDER BY fqn
            """, (project_name,))
        else:
            cursor.execute("""
                SELECT * FROM project_classes 
                WHERE project_name = ? AND is_anonymous = 0
                ORDER BY fqn
            """, (project_name,))
        
        rows = cursor.fetchall()
        return [
            ProjectClassInfo(
                fqn=row['fqn'],
                simple_name=row['simple_name'],
                package_name=row['package_name'],
                project_name=row['project_name'],
                file_path=row['file_path'],
                relative_path=row['relative_path'],
                is_anonymous=bool(row['is_anonymous']),
                is_nested=bool(row['is_nested']),
                parent_class=row['parent_class'],
                symbol_id=row['symbol_id'],
                parent_symbol_id=row['parent_symbol_id'],
                insert_time=row['insert_time'],
                last_modified=row['last_modified']
            )
            for row in rows
        ]
    
    def query_by_file(
        self,
        project_name: str,
        file_path: str
    ) -> List[ProjectClassInfo]:
        """
        查询文件的所有类
        
        参数:
            project_name: 项目名称
            file_path: 文件路径
        
        返回:
            匹配的 ProjectClassInfo 对象列表
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT * FROM project_classes 
            WHERE project_name = ? AND file_path = ?
            ORDER BY fqn
        """, (project_name, file_path))
        
        rows = cursor.fetchall()
        return [
            ProjectClassInfo(
                fqn=row['fqn'],
                simple_name=row['simple_name'],
                package_name=row['package_name'],
                project_name=row['project_name'],
                file_path=row['file_path'],
                relative_path=row['relative_path'],
                is_anonymous=bool(row['is_anonymous']),
                is_nested=bool(row['is_nested']),
                parent_class=row['parent_class'],
                symbol_id=row['symbol_id'],
                parent_symbol_id=row['parent_symbol_id'],
                insert_time=row['insert_time'],
                last_modified=row['last_modified']
            )
            for row in rows
        ]
    
    def batch_insert_classes(self, classes: List[ProjectClassInfo]) -> int:
        """
        批量插入类信息
        
        参数:
            classes: ProjectClassInfo 对象列表
        
        返回:
            插入的记录数
        """
        if not classes:
            return 0
        
        cursor = self.conn.cursor()
        cursor.executemany("""
            INSERT OR REPLACE INTO project_classes 
            (fqn, simple_name, package_name, project_name, file_path, 
             relative_path, is_anonymous, is_nested, parent_class, 
             symbol_id, parent_symbol_id, insert_time, last_modified)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, [
            (c.fqn, c.simple_name, c.package_name, c.project_name, c.file_path,
             c.relative_path, c.is_anonymous, c.is_nested, c.parent_class,
             c.symbol_id, c.parent_symbol_id, c.insert_time, c.last_modified)
            for c in classes
        ])
        
        inserted_count = cursor.rowcount
        self.conn.commit()
        return inserted_count
    
    def delete_by_file(self, project_name: str, file_path: str) -> int:
        """
        删除文件的所有类
        
        参数:
            project_name: 项目名称
            file_path: 文件路径
        
        返回:
            删除的记录数
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            DELETE FROM project_classes 
            WHERE project_name = ? AND file_path = ?
        """, (project_name, file_path))
        
        deleted_count = cursor.rowcount
        self.conn.commit()
        return deleted_count
    
    def delete_by_project(self, project_name: str) -> int:
        """
        删除项目的所有类（别名方法）
        
        参数:
            project_name: 项目名称
        
        返回:
            删除的记录数
        """
        return self.delete_project(project_name)
    
    def query_nested_classes(
        self,
        parent_fqn: str,
        project_name: str
    ) -> List[ProjectClassInfo]:
        """
        查询嵌套类
        
        参数:
            parent_fqn: 父类的完全限定名
            project_name: 项目名称
        
        返回:
            嵌套类列表
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT * FROM project_classes 
            WHERE project_name = ? AND parent_class = ?
            ORDER BY simple_name
        """, (project_name, parent_fqn))
        
        rows = cursor.fetchall()
        return [
            ProjectClassInfo(
                fqn=row['fqn'],
                simple_name=row['simple_name'],
                package_name=row['package_name'],
                project_name=row['project_name'],
                file_path=row['file_path'],
                relative_path=row['relative_path'],
                is_anonymous=bool(row['is_anonymous']),
                is_nested=bool(row['is_nested']),
                parent_class=row['parent_class'],
                symbol_id=row['symbol_id'],
                parent_symbol_id=row['parent_symbol_id'],
                insert_time=row['insert_time'],
                last_modified=row['last_modified']
            )
            for row in rows
        ]
    
    def delete_project(self, project_name: str) -> int:
        """
        删除项目的所有类
        
        参数:
            project_name: 项目名称
        
        返回:
            删除的记录数
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            DELETE FROM project_classes 
            WHERE project_name = ?
        """, (project_name,))
        
        deleted_count = cursor.rowcount
        self.conn.commit()
        return deleted_count
    
    def close(self):
        """
        关闭数据库连接
        
        注意：由于使用单例模式，通常不需要手动关闭连接
        """
        if self.conn:
            self.conn.close()
            self.conn = None
    
    def __enter__(self):
        """上下文管理器入口"""
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """上下文管理器出口"""
        # 单例模式下不关闭连接，保持连接池活跃
        pass


# 全局单例实例
_global_project_db_instance: Optional[ProjectClassDB] = None
_global_project_db_lock = threading.Lock()


def get_project_class_db(db_path: Optional[str] = None) -> ProjectClassDB:
    """
    获取全局 ProjectClassDB 实例（推荐使用）
    
    参数:
        db_path: 数据库文件路径（默认使用 PROJECT_ROOT_PATH/.cache/project_classes.db）
    
    返回:
        ProjectClassDB 单例实例
    
    示例:
        # 方式 1: 使用全局实例（推荐）
        db = get_project_class_db()
        classes = db.query_by_simple_name("User", "my-project")
        # 不需要 close()
        
        # 方式 2: 直接创建实例（也是单例）
        db = ProjectClassDB()
        classes = db.query_by_simple_name("User", "my-project")
        # 不需要 close()
    """
    global _global_project_db_instance
    
    if _global_project_db_instance is None:
        with _global_project_db_lock:
            if _global_project_db_instance is None:
                _global_project_db_instance = ProjectClassDB(db_path)
    
    return _global_project_db_instance

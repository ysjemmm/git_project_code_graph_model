"""
JAR 类数据库模块
用于管理 SQLite 数据库中的 Java 类信息
"""
import sqlite3
import time
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Dict

from tools.constants import PROJECT_ROOT_PATH


@dataclass
class ClassInfo:
    """表示一个Java类的信息"""
    fqn: str                    # 完全限定名，例如 "com.example.User"
    simple_name: str            # 简单名称，例如 "User" 或 "Outer$Inner"
    package_name: str           # 包名，例如 "com.example"（可能为空）
    jar_name: str               # JAR文件名，例如 "spring-core-5.3.0.jar"
    jar_path: str               # JAR文件完整路径（用于兼容性）
    is_anonymous: bool          # 是否为匿名类
    insert_time: str            # 插入时间，格式: "YYYY-MM-DD HH:MM:SS"
    file_path: Optional[str] = None             # 类在 JAR 中的文件路径，例如 "com/example/User.class"
    parent_artifact_id: Optional[str] = None    # 父项目 artifact ID
    parent_group_id: Optional[str] = None       # 父项目 group ID
    parent_version: Optional[str] = None        # 父项目版本
    artifact_id: Optional[str] = None           # 当前项目 artifact ID
    artifact_group_id: Optional[str] = None     # 当前项目 group ID
    artifact_version: Optional[str] = None      # 当前项目版本


class JARClassDB:
    """SQLite数据库操作类（单例模式）"""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls, db_path: Optional[str] = None):
        """
        单例模式：确保每个数据库路径只有一个实例
        """
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super(JARClassDB, cls).__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self, db_path: Optional[str] = None):
        """
        初始化数据库连接（只初始化一次）
        
        参数:
            db_path: 数据库文件路径（默认使用 PROJECT_ROOT_PATH/.cache/jar_classes.db）
        """
        # 避免重复初始化
        if self._initialized:
            return
        
        # 使用项目根路径
        if db_path is None:
            db_path = str(PROJECT_ROOT_PATH / ".cache" / "jar_classes.db")
        
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
            - jar_classes 表（id, fqn, simple_name, package_name, jar_name, jar_path, is_anonymous, insert_time）
            - jar_metadata 表（id, jar_path, scan_timestamp, class_count）
            - 索引：fqn + jar_name（联合唯一）、simple_name、package_name、jar_name
        """
        cursor = self.conn.cursor()
        
        # 创建 jar_classes 表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS jar_classes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fqn TEXT NOT NULL,
                simple_name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                jar_name TEXT NOT NULL,
                jar_path TEXT NOT NULL,
                is_anonymous BOOLEAN NOT NULL,
                insert_time TEXT NOT NULL,
                file_path TEXT,
                parent_artifact_id TEXT,
                parent_group_id TEXT,
                parent_version TEXT,
                artifact_id TEXT,
                artifact_group_id TEXT,
                artifact_version TEXT,
                UNIQUE(fqn, jar_name)
            )
        """)
        
        # 创建索引
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_fqn 
            ON jar_classes(fqn)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_simple_name 
            ON jar_classes(simple_name)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_package_name 
            ON jar_classes(package_name)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_jar_name 
            ON jar_classes(jar_name)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_jar_path 
            ON jar_classes(jar_path)
        """)
        
        cursor.execute("""
            CREATE INDEX IF NOT EXISTS idx_insert_time 
            ON jar_classes(insert_time)
        """)
        
        # 创建 jar_metadata 表
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS jar_metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                jar_path TEXT NOT NULL UNIQUE,
                scan_timestamp REAL NOT NULL,
                class_count INTEGER NOT NULL
            )
        """)
        
        cursor.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_jar_metadata_path 
            ON jar_metadata(jar_path)
        """)
        
        self.conn.commit()
    
    def batch_insert_classes(self, classes: List[ClassInfo]) -> int:
        """
        批量插入类信息
        
        参数:
            classes: ClassInfo对象列表
        
        返回:
            插入的记录数
        
        行为:
            - 使用事务确保原子性
            - 如果 (fqn, jar_name) 组合已存在，更新为新的信息（upsert）
            - 批量操作以提高性能
            - 自动记录插入时间
        """
        if not classes:
            return 0
        
        cursor = self.conn.cursor()
        inserted_count = 0
        
        # 获取当前时间字符串
        from datetime import datetime
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        try:
            # 使用 INSERT OR REPLACE 来处理重复的 FQN
            cursor.executemany("""
                INSERT OR REPLACE INTO jar_classes 
                (fqn, simple_name, package_name, jar_name, jar_path, is_anonymous, insert_time, file_path,
                 parent_artifact_id, parent_group_id, parent_version,
                 artifact_id, artifact_group_id, artifact_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, [
                (c.fqn, c.simple_name, c.package_name, c.jar_name, c.jar_path, c.is_anonymous, current_time, c.file_path,
                 c.parent_artifact_id, c.parent_group_id, c.parent_version,
                 c.artifact_id, c.artifact_group_id, c.artifact_version)
                for c in classes
            ])
            
            inserted_count = cursor.rowcount
            self.conn.commit()
            
        except Exception as e:
            self.conn.rollback()
            raise Exception(f"批量插入失败: {e}")
        
        return inserted_count
    
    def _row_to_classinfo(self, row) -> ClassInfo:
        """将数据库行转换为 ClassInfo 对象"""
        # 获取所有可用的列名
        keys = row.keys() if hasattr(row, 'keys') else []
        
        return ClassInfo(
            fqn=row['fqn'],
            simple_name=row['simple_name'],
            package_name=row['package_name'],
            jar_name=row['jar_name'],
            jar_path=row['jar_path'],
            is_anonymous=bool(row['is_anonymous']),
            insert_time=row['insert_time'],
            file_path=row['file_path'] if 'file_path' in keys else None,
            parent_artifact_id=row['parent_artifact_id'] if 'parent_artifact_id' in keys else None,
            parent_group_id=row['parent_group_id'] if 'parent_group_id' in keys else None,
            parent_version=row['parent_version'] if 'parent_version' in keys else None,
            artifact_id=row['artifact_id'] if 'artifact_id' in keys else None,
            artifact_group_id=row['artifact_group_id'] if 'artifact_group_id' in keys else None,
            artifact_version=row['artifact_version'] if 'artifact_version' in keys else None
        )
    
    def query_by_fqn(self, fqn: str) -> Optional[ClassInfo]:
        """
        按完全限定名精确查询
        
        参数:
            fqn: 完全限定名
        
        返回:
            匹配的ClassInfo对象，如果不存在则返回None
        """
        cursor = self.conn.cursor()
        
        # 检查表中是否有 POM 相关字段
        select_fields = self._get_select_fields()
        
        cursor.execute(f"""
            SELECT {select_fields}
            FROM jar_classes
            WHERE fqn = ?
        """, (fqn,))
        
        row = cursor.fetchone()
        if row:
            return self._row_to_classinfo(row)
        return None
    
    def _get_select_fields(self) -> str:
        """获取 SELECT 语句的字段列表"""
        # 检查表结构，确定哪些字段存在
        cursor = self.conn.cursor()
        cursor.execute("PRAGMA table_info(jar_classes)")
        columns = [row[1] for row in cursor.fetchall()]
        
        # 基础字段
        base_fields = ['fqn', 'simple_name', 'package_name', 'jar_name', 'jar_path', 'is_anonymous', 'insert_time']
        
        # 可选字段
        optional_fields = ['file_path', 'parent_artifact_id', 'parent_group_id', 'parent_version', 
                          'artifact_id', 'artifact_group_id', 'artifact_version']
        
        # 只选择存在的字段
        select_fields = base_fields.copy()
        for field in optional_fields:
            if field in columns:
                select_fields.append(field)
        
        return ', '.join(select_fields)
    
    def query_by_simple_name(
        self, 
        simple_name: str, 
        include_anonymous: bool = False
    ) -> List[ClassInfo]:
        """
        按简单名称查询
        
        参数:
            simple_name: 简单类名
            include_anonymous: 是否包含匿名类（默认False，不包含匿名类）
        
        返回:
            匹配的ClassInfo对象列表，按package_name排序
        """
        cursor = self.conn.cursor()
        select_fields = self._get_select_fields()
        
        if include_anonymous:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE simple_name = ?
                ORDER BY package_name
            """, (simple_name,))
        else:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE simple_name = ? AND is_anonymous = 0
                ORDER BY package_name
            """, (simple_name,))
        
        rows = cursor.fetchall()
        return [self._row_to_classinfo(row) for row in rows]
    
    def query_by_package(
        self, 
        package_name: str, 
        include_anonymous: bool = False
    ) -> List[ClassInfo]:
        """
        按包名查询
        
        参数:
            package_name: 包名
            include_anonymous: 是否包含匿名类（默认False，不包含匿名类）
        
        返回:
            该包中所有类的ClassInfo对象列表，按simple_name排序
        """
        cursor = self.conn.cursor()
        select_fields = self._get_select_fields()
        
        if include_anonymous:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE package_name = ?
                ORDER BY simple_name
            """, (package_name,))
        else:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE package_name = ? AND is_anonymous = 0
                ORDER BY simple_name
            """, (package_name,))
        
        rows = cursor.fetchall()
        return [self._row_to_classinfo(row) for row in rows]
    
    def query_by_jar(
        self, 
        jar_path: str, 
        include_anonymous: bool = False
    ) -> List[ClassInfo]:
        """
        按JAR文件路径查询
        
        参数:
            jar_path: JAR文件路径
            include_anonymous: 是否包含匿名类（默认False，不包含匿名类）
        
        返回:
            该JAR中所有类的ClassInfo对象列表，按fqn排序
        """
        cursor = self.conn.cursor()
        select_fields = self._get_select_fields()
        
        if include_anonymous:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE jar_path = ?
                ORDER BY fqn
            """, (jar_path,))
        else:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE jar_path = ? AND is_anonymous = 0
                ORDER BY fqn
            """, (jar_path,))
        
        rows = cursor.fetchall()
        return [self._row_to_classinfo(row) for row in rows]
    
    def query_by_jar_name(
        self, 
        jar_name: str, 
        include_anonymous: bool = False
    ) -> List[ClassInfo]:
        """
        按JAR文件名查询
        
        参数:
            jar_name: JAR文件名，例如 "spring-core-5.3.0.jar"
            include_anonymous: 是否包含匿名类（默认False，不包含匿名类）
        
        返回:
            该JAR中所有类的ClassInfo对象列表，按fqn排序
        """
        cursor = self.conn.cursor()
        select_fields = self._get_select_fields()
        
        if include_anonymous:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE jar_name = ?
                ORDER BY fqn
            """, (jar_name,))
        else:
            cursor.execute(f"""
                SELECT {select_fields}
                FROM jar_classes
                WHERE jar_name = ? AND is_anonymous = 0
                ORDER BY fqn
            """, (jar_name,))
        
        rows = cursor.fetchall()
        return [self._row_to_classinfo(row) for row in rows]
    
    def update_jar_metadata(
        self, 
        jar_path: str, 
        class_count: int
    ) -> None:
        """
        更新或插入JAR元数据
        
        参数:
            jar_path: JAR文件路径
            class_count: 类数量
        
        行为:
            - 更新scan_timestamp为当前时间
            - 如果记录不存在则插入，存在则更新
        """
        cursor = self.conn.cursor()
        current_time = time.time()
        
        cursor.execute("""
            INSERT OR REPLACE INTO jar_metadata 
            (jar_path, scan_timestamp, class_count)
            VALUES (?, ?, ?)
        """, (jar_path, current_time, class_count))
        
        self.conn.commit()
    
    def get_jar_metadata(self, jar_path: str) -> Optional[Dict]:
        """
        获取JAR元数据
        
        参数:
            jar_path: JAR文件路径
        
        返回:
            包含jar_path、scan_timestamp、class_count的字典
            如果不存在则返回None
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT jar_path, scan_timestamp, class_count
            FROM jar_metadata
            WHERE jar_path = ?
        """, (jar_path,))
        
        row = cursor.fetchone()
        if row:
            return {
                'jar_path': row['jar_path'],
                'scan_timestamp': row['scan_timestamp'],
                'class_count': row['class_count']
            }
        return None
    
    def delete_jar_classes(self, jar_path: str) -> int:
        """
        删除指定JAR的所有类记录
        
        参数:
            jar_path: JAR文件路径
        
        返回:
            删除的记录数
        """
        cursor = self.conn.cursor()
        cursor.execute("""
            DELETE FROM jar_classes
            WHERE jar_path = ?
        """, (jar_path,))
        
        deleted_count = cursor.rowcount
        self.conn.commit()
        
        return deleted_count
    
    def close(self):
        """
        关闭数据库连接
        
        注意：由于使用单例模式，通常不需要手动关闭连接
        连接会在程序结束时自动关闭
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
_global_db_instance: Optional[JARClassDB] = None
_global_db_lock = threading.Lock()


def get_jar_class_db(db_path: Optional[str] = None) -> JARClassDB:
    """
    获取全局 JARClassDB 实例（推荐使用）
    
    参数:
        db_path: 数据库文件路径（默认使用 PROJECT_ROOT_PATH/.cache/jar_classes.db）
    
    返回:
        JARClassDB 单例实例
    
    示例:
        # 方式 1: 使用全局实例（推荐）
        db = get_jar_class_db()
        classes = db.query_by_simple_name("User")
        # 不需要 close()
        
        # 方式 2: 直接创建实例（也是单例）
        db = JARClassDB()
        classes = db.query_by_simple_name("User")
        # 不需要 close()
    """
    global _global_db_instance
    
    if _global_db_instance is None:
        with _global_db_lock:
            if _global_db_instance is None:
                _global_db_instance = JARClassDB(db_path)
    
    return _global_db_instance

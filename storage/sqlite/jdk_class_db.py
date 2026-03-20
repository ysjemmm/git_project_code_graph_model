"""
JDK 类数据库模块
用于管理 SQLite 数据库中的 JDK 标准库类信息
"""
import sqlite3
import threading
from pathlib import Path
from typing import Optional

from tools.constants import PROJECT_ROOT_PATH
from .jar_class_db import ClassInfo, JARClassDB


class JDKClassDB(JARClassDB):
    """
    JDK 类数据库（继承自 JARClassDB，使用相同的表结构）
    
    单例模式，支持多个 JDK 版本的数据库
    """
    
    _instances = {}  # {db_path: instance}
    _lock = threading.Lock()
    
    def __new__(cls, db_path: Optional[str] = None, jdk_version: Optional[int] = None):
        """单例模式：每个数据库路径只有一个实例"""
        # 确定数据库路径
        if db_path is None:
            if jdk_version:
                db_path = str(PROJECT_ROOT_PATH / ".cache" / f"jdk{jdk_version}_classes.db")
            else:
                # 默认尝试查找已存在的 JDK 数据库
                db_path = cls._find_existing_jdk_db()
                if not db_path:
                    # 如果没有找到，使用默认路径
                    db_path = str(PROJECT_ROOT_PATH / ".cache" / "jdk_classes.db")
        
        with cls._lock:
            if db_path not in cls._instances:
                instance = super(JARClassDB, cls).__new__(cls)
                instance._initialized = False
                cls._instances[db_path] = instance
            
            return cls._instances[db_path]
    
    def __init__(self, db_path: Optional[str] = None, jdk_version: Optional[int] = None):
        """
        初始化 JDK 类数据库（只初始化一次）
        
        参数:
            db_path: 数据库文件路径
            jdk_version: JDK 版本号（用于自动确定数据库路径）
        """
        # 避免重复初始化
        if self._initialized:
            return
        
        # 确定数据库路径
        if db_path is None:
            if jdk_version:
                db_path = str(PROJECT_ROOT_PATH / ".cache" / f"jdk{jdk_version}_classes.db")
            else:
                db_path = self._find_existing_jdk_db()
                if not db_path:
                    db_path = str(PROJECT_ROOT_PATH / ".cache" / "jdk_classes.db")
        
        # 调用父类初始化
        super().__init__(db_path)
    
    @staticmethod
    def _find_existing_jdk_db() -> Optional[str]:
        """查找已存在的 JDK 数据库"""
        cache_dir = PROJECT_ROOT_PATH / ".cache"
        if not cache_dir.exists():
            return None
        
        # 按版本号从高到低查找（包括最新的 JDK 版本）
        for version in [24, 23, 22, 21, 17, 11, 8]:
            db_path = cache_dir / f"jdk{version}_classes.db"
            if db_path.exists():
                return str(db_path)
        
        # 查找通用的 jdk_classes.db
        db_path = cache_dir / "jdk_classes.db"
        if db_path.exists():
            return str(db_path)
        
        return None
    
    @classmethod
    def get_available_versions(cls) -> list:
        """获取所有可用的 JDK 版本"""
        cache_dir = PROJECT_ROOT_PATH / ".cache"
        if not cache_dir.exists():
            return []
        
        versions = []
        for db_file in cache_dir.glob("jdk*_classes.db"):
            # 提取版本号
            name = db_file.stem  # jdk17_classes
            if name.startswith("jdk") and name.endswith("_classes"):
                version_str = name[3:-8]  # 17
                try:
                    version = int(version_str)
                    versions.append(version)
                except ValueError:
                    pass
        
        return sorted(versions, reverse=True)


# 全局单例实例
_global_jdk_db_instance: Optional[JDKClassDB] = None
_global_jdk_db_lock = threading.Lock()


def get_jdk_class_db(jdk_version: Optional[int] = None) -> Optional[JDKClassDB]:
    """
    获取全局 JDKClassDB 实例（推荐使用）
    
    参数:
        jdk_version: JDK 版本号（可选，如果不指定则自动查找）
    
    返回:
        JDKClassDB 单例实例，如果没有找到 JDK 数据库则返回 None
    
    示例:
        # 方式 1: 自动查找（推荐）
        db = get_jdk_class_db()
        if db:
            cls = db.query_by_fqn("java.lang.String")
        
        # 方式 2: 指定版本
        db = get_jdk_class_db(jdk_version=17)
        if db:
            cls = db.query_by_fqn("java.lang.String")
    """
    global _global_jdk_db_instance
    
    # 如果已经有实例且版本匹配，直接返回
    if _global_jdk_db_instance is not None:
        return _global_jdk_db_instance
    
    with _global_jdk_db_lock:
        if _global_jdk_db_instance is None:
            # 检查是否存在 JDK 数据库
            db_path = JDKClassDB._find_existing_jdk_db()
            if db_path:
                _global_jdk_db_instance = JDKClassDB(db_path, jdk_version)
            else:
                # 没有找到 JDK 数据库，返回 None
                return None
    
    return _global_jdk_db_instance


def has_jdk_index() -> bool:
    """检查是否存在 JDK 索引"""
    return JDKClassDB._find_existing_jdk_db() is not None

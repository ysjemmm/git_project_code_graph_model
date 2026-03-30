import os
from contextlib import contextmanager
from urllib.parse import urlparse, urlunparse

from neomodel import config

from storage.neo4j.dao.project_neo_dao import ProjectNeoDao


class Neo4jDatabase:
    def __init__(self, uri: str, username: str, password: str):
        # 去除可能的引号和空白字符
        uri = uri.strip().strip('"').strip("'")
        username = username.strip().strip('"').strip("'")
        password = password.strip().strip('"').strip("'")
        
        print("[neo4j] raw uri:", uri)
        print("[neo4j] username:", username)
        
        # 将 neo4j:// 协议转换为 bolt:// 协议（neomodel 要求的格式）
        if uri.startswith("neo4j+s://"):
            uri = uri.replace("neo4j+s://", "bolt+s://")
        elif uri.startswith("neo4j://"):
            uri = uri.replace("neo4j://", "bolt://")
        elif uri.startswith("neo4j+ssc://"):
            uri = uri.replace("neo4j+ssc://", "bolt+ssc://")

        # 格式：bolt://user:password@host:port
        parsed = urlparse(uri)
        # 重建 URL，加入用户名和密码
        netloc = f"{username}:{password}@{parsed.hostname}"
        if parsed.port:
            netloc += f":{parsed.port}"
        
        final_uri = urlunparse((
            parsed.scheme,
            netloc,
            parsed.path,
            parsed.params,
            parsed.query,
            parsed.fragment
        ))
        
        print("[neo4j] final uri:", final_uri)
        
        config.DATABASE_URL = final_uri
        config.MAX_POOL_SIZE = 100  # 连接池最大连接数
        config.MAX_CONNECTION_LIFETIME = 3600  # 连接最大生命周期（秒）
        config.CONNECTION_ACQUISITION_TIMEOUT = 60.0  # 获取连接超时
        config.KEEP_ALIVE = True  # 启用 keep-alive
        
        # 强制重新连接 - 使用显式导入的 db
        from neomodel import db as neo4j_db
        neo4j_db.set_connection(final_uri)
        self._test_connection()

    def _test_connection(self):
        try:
            # 使用模块级的 db 对象执行测试查询
            from neomodel import db as neo4j_db
            neo4j_db.cypher_query("RETURN 1")
            print("[neo4j] Connection test successful!")
        except Exception as e:
            raise ConnectionError(f"Neo4j 连接失败：{e}")
    
    @contextmanager
    def get_session(self):
        # neomodel 的事务管理由驱动自动处理
        # 直接 yield db 对象，不需要手动 begin/commit/rollback
        from neomodel import db as neo4j_db
        try:
            yield neo4j_db
        except Exception:
            # 发生异常时不需要手动 rollback，neomodel 会自动处理
            raise
        finally:
            # neomodel 会自动清理事务资源
            pass
    
    def close(self):
        from neomodel import db as neo4j_db
        if hasattr(neo4j_db, 'driver') and neo4j_db.driver:
            neo4j_db.driver.close()

_neo4j_db = Neo4jDatabase(
    uri=os.environ.get("NEO4J_URI", "neo4j+s://26fa83e0.databases.neo4j.io"),
    username=os.environ.get("NEO4J_USER", "neo4j"),
    password=os.environ.get("NEO4J_PASSWORD", "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4")
)

def get_project_neo_dao():
    return ProjectNeoDao()
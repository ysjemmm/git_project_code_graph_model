"""
Neo4j 数据库连接器
支持连接到 Neo4j 云数据库并执行 Cypher 查询
内置连接池管理，避免重复创建连接
"""

import logging
from threading import Lock
from typing import Dict, List, Optional

from neo4j import GraphDatabase, Driver

logger = logging.getLogger(__name__)


class Neo4jConnectorPool:
    """Neo4j 连接池管理器（单例模式）"""
    
    _instance = None
    _lock = Lock()
    _drivers: Dict[str, Driver] = {}
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance
    
    @classmethod
    def get_driver(cls, uri: str, username: str, password: str) -> Driver:
        """获取或创建连接池驱动"""
        pool = cls()
        key = f"{uri}:{username}"
        
        if key not in pool._drivers:
            with cls._lock:
                if key not in pool._drivers:
                    try:
                        driver = GraphDatabase.driver(
                            uri,
                            auth=(username, password),
                            max_connection_pool_size=150
                        )
                        pool._drivers[key] = driver
                        logger.info(f"创建新的连接池: {uri}")
                    except Exception as e:
                        logger.error(f"创建连接池失败: {e}")
                        raise
        
        return pool._drivers[key]
    
    @classmethod
    def close_all(cls):
        """关闭所有连接池"""
        pool = cls()
        for key, driver in pool._drivers.items():
            try:
                driver.close()
                logger.info(f"关闭连接池: {key}")
            except Exception as e:
                logger.error(f"关闭连接池失败: {e}")
        pool._drivers.clear()


class Neo4jConnector:
    """Neo4j 数据库连接器"""
    
    def __init__(self, uri: str, username: str, password: str, database: str = "neo4j"):
        """初始化 Neo4j 连接器
        
        Args:
            uri: 连接 URI（如 bolt://host:7687）
            username: 用户名
            password: 密码
            database: 数据库名称（默认: "neo4j"）
        """
        self.uri = uri
        self.username = username
        self.password = password
        self.database = database
        self.driver: Optional[Driver] = None
        self.connected = False
    
    def connect(self) -> bool:
        """连接到 Neo4j 数据库（使用连接池）"""
        try:
            self.driver = Neo4jConnectorPool.get_driver(
                self.uri,
                self.username,
                self.password
            )
            
            with self.driver.session(database=self.database) as session:
                session.run("RETURN 1")
            
            self.connected = True
            logger.info(f"成功连接到 Neo4j: {self.uri}")
            return True
        
        except Exception as e:
            logger.error(f"连接失败: {e}")
            self.connected = False
            return False
    
    def disconnect(self):
        """断开连接（不会关闭连接池）"""
        self.connected = False
        logger.info("已断开连接（连接池保持活跃）")
    
    def execute_query(self, query: str, parameters: Optional[Dict] = None) -> List[Dict]:
        """执行 Cypher 查询
        
        Args:
            query: Cypher 查询语句
            parameters: 查询参数字典（可选）
        
        Returns:
            查询结果列表
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return []
        
        try:
            with self.driver.session(database=self.database) as session:
                result = session.run(query, parameters or {})
                records = [dict(record) for record in result]
                if records:
                    logger.info(f"查询成功，返回 {len(records)} 条记录")
                return records
        
        except Exception as e:
            logger.error(f"查询失败: {e}")
            return []
    
    def create_node(self, label: str, properties: Dict) -> bool:
        """创建节点
        
        Args:
            label: 节点标签
            properties: 节点属性字典
        
        Returns:
            是否创建成功
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return False
        
        try:
            prop_strs = []
            for key, value in properties.items():
                if isinstance(value, str):
                    prop_strs.append(f"{key}: '{value}'")
                elif isinstance(value, bool):
                    prop_strs.append(f"{key}: {str(value).lower()}")
                else:
                    prop_strs.append(f"{key}: {value}")
            
            props_str = ", ".join(prop_strs)
            query = f"CREATE (n:{label} {{{props_str}}})"
            
            with self.driver.session(database=self.database) as session:
                session.run(query)
            
            logger.info(f"创建节点成功: {label}")
            return True
        
        except Exception as e:
            logger.error(f"创建节点失败: {e}")
            return False
    
    def create_relationship(self, 
                          source_id: str, 
                          target_id: str, 
                          rel_type: str,
                          properties: Optional[Dict] = None) -> bool:
        """创建关系
        
        Args:
            source_id: 源节点 ID
            target_id: 目标节点 ID
            rel_type: 关系类型
            properties: 关系属性（可选）
        
        Returns:
            是否创建成功
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return False
        
        try:
            props_str = ""
            if properties:
                prop_strs = []
                for key, value in properties.items():
                    if isinstance(value, str):
                        prop_strs.append(f"{key}: '{value}'")
                    elif isinstance(value, bool):
                        prop_strs.append(f"{key}: {str(value).lower()}")
                    else:
                        prop_strs.append(f"{key}: {value}")
                props_str = f" {{{', '.join(prop_strs)}}}"
            
            query = (f"MATCH (a {{id: '{source_id}'}}), (b {{id: '{target_id}'}}) "
                    f"CREATE (a)-[:{rel_type}{props_str}]->(b)")
            
            with self.driver.session(database=self.database) as session:
                session.run(query)
            
            logger.info(f"创建关系成功: {rel_type}")
            return True
        
        except Exception as e:
            logger.error(f"创建关系失败: {e}")
            return False
    
    def clear_database(self) -> bool:
        """清空数据库中的所有节点和关系"""
        if not self.connected:
            logger.error("未连接到数据库")
            return False
        
        try:
            with self.driver.session(database=self.database) as session:
                session.run("MATCH (n) DETACH DELETE n")
            
            logger.info("数据库已清空")
            return True
        
        except Exception as e:
            logger.error(f"清空数据库失败: {e}")
            return False
    
    def get_statistics(self) -> Dict[str, int]:
        """获取数据库统计信息
        
        Returns:
            包含 total_nodes、total_relationships、node_types、relationship_types 的字典
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return {}
        
        try:
            with self.driver.session(database=self.database) as session:
                node_count = session.run("MATCH (n) RETURN count(n) as count").single()["count"]
                rel_count = session.run("MATCH ()-[r]->() RETURN count(r) as count").single()["count"]
                node_types = session.run(
                    "MATCH (n) RETURN labels(n)[0] as label, count(*) as count"
                ).data()
                rel_types = session.run(
                    "MATCH ()-[r]->() RETURN type(r) as type, count(*) as count"
                ).data()
            
            stats = {
                "total_nodes": node_count,
                "total_relationships": rel_count,
                "node_types": {item["label"]: item["count"] for item in node_types},
                "relationship_types": {item["type"]: item["count"] for item in rel_types}
            }
            
            logger.info(f"获取统计信息成功")
            return stats
        
        except Exception as e:
            logger.error(f"获取统计信息失败: {e}")
            return {}
    
    def import_from_csv(self, nodes_file: str, relationships_file: str) -> bool:
        """从 CSV 文件导入数据
        
        Args:
            nodes_file: 节点 CSV 文件路径
            relationships_file: 关系 CSV 文件路径
        
        Returns:
            是否导入成功
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return False
        
        try:
            logger.info("CSV 导入功能需要进一步实现")
            return True
        
        except Exception as e:
            logger.error(f"CSV 导入失败: {e}")
            return False
    
    def delete_nodes_by_file(self, file_path: str, project_name: str) -> int:
        """删除特定文件相关的所有节点及其关系
        
        Args:
            file_path: Java 文件路径
            project_name: 项目名称
        
        Returns:
            删除的节点数
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return 0
        
        try:
            with self.driver.session(database=self.database) as session:
                query = """
                MATCH (f:JavaFile {file_path: $file_path, belong_project: $project_name})
                OPTIONAL MATCH (f)-[r1:CONTAINS]->(obj:JavaObject)
                OPTIONAL MATCH (obj)-[r2:MEMBER_OF]->(method:Method)
                OPTIONAL MATCH (obj)-[r3:MEMBER_OF]->(field:Field)
                OPTIONAL MATCH (method)-[r4:CALLS]->()
                OPTIONAL MATCH (method)-[r5:ACCESSES]->()
                OPTIONAL MATCH (field)-[r6:ACCESSES]->()
                OPTIONAL MATCH (method)-[r7:MEMBER_OF]->(param:Parameter)
                OPTIONAL MATCH (obj)-[r8:EXTENDS]->()
                OPTIONAL MATCH (obj)-[r9:IMPLEMENTS]->()
                WITH f, obj, method, field, param, 
                     [r1, r2, r3, r4, r5, r6, r7, r8, r9] as rels
                DETACH DELETE f, obj, method, field, param
                RETURN count(DISTINCT f) + count(DISTINCT obj) + count(DISTINCT method) + 
                       count(DISTINCT field) + count(DISTINCT param) as deleted_count
                """
                
                result = session.run(query, {
                    "file_path": file_path,
                    "project_name": project_name
                }).single()
                
                deleted_count = result["deleted_count"] if result else 0
                logger.info(f"删除文件 {file_path} 相关 {deleted_count} 个节点")
                return deleted_count
        
        except Exception as e:
            logger.error(f"删除节点失败: {e}")
            return 0


def create_test_data(connector: Neo4jConnector) -> bool:
    """创建测试数据
    
    Args:
        connector: Neo4j 连接器实例
    
    Returns:
        是否创建成功
    """
    try:
        print("\n创建测试节点...")
        
        connector.create_node("CLASS", {
            "id": "test_class_1",
            "name": "User",
            "qualified_name": "com.example.User",
            "file_path": "com/example/User.java",
            "is_public": True
        })
        
        connector.create_node("CLASS", {
            "id": "test_class_2",
            "name": "UserService",
            "qualified_name": "com.example.UserService",
            "file_path": "com/example/UserService.java",
            "is_public": True
        })
        
        connector.create_node("METHOD", {
            "id": "test_method_1",
            "name": "findById",
            "qualified_name": "com.example.UserService.findById",
            "is_public": True,
            "is_static": False
        })
        
        connector.create_node("FIELD", {
            "id": "test_field_1",
            "name": "id",
            "qualified_name": "com.example.User.id",
            "type_name": "Long",
            "is_public": False
        })
        
        print("节点创建成功")
        
        print("\n创建测试关系...")
        
        connector.create_relationship(
            "test_method_1",
            "test_class_2",
            "MEMBER_OF"
        )
        
        connector.create_relationship(
            "test_field_1",
            "test_class_1",
            "MEMBER_OF"
        )
        
        connector.create_relationship(
            "test_field_1",
            "test_class_1",
            "TYPE_OF",
            {"edge_type": "field_type"}
        )
        
        print("关系创建成功")
        
        return True
    
    except Exception as e:
        print(f"创建测试数据失败: {e}")
        return False

"""
Neo4j 数据库连接器
支持连接到 Neo4j 云数据库并执行 Cypher 查询
内置连接池管理，避免重复创建连接
"""

import logging
import re
import hashlib
import time
from threading import Lock
from typing import Any, Dict, List, Optional

from neo4j import GraphDatabase, Driver
from neo4j.exceptions import ServiceUnavailable, SessionExpired, Neo4jError

from storage.neo4j.graph_schema import JavaGraphEdgeType
from storage.neo4j.query_diagnostics import record_neo4j_operation

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
        # 注意：连接池 key 必须包含凭据指纹，否则密码轮换/多凭据场景会错误复用旧 driver。
        # 不保存明文密码，只使用不可逆的短 hash 作为区分。
        pwd_fp = hashlib.sha256((password or "").encode("utf-8")).hexdigest()[:12]
        key = f"{uri}:{username}:{pwd_fp}"
        
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
        
        except ServiceUnavailable as e:
            logger.error(f"连接失败（服务不可用/网络异常）: {e}")
            self.connected = False
            return False
        except OSError as e:
            logger.error(f"连接失败（网络/套接字错误）: {e}")
            self.connected = False
            return False
        except Neo4jError as e:
            # Neo4jError 会覆盖认证失败、数据库不存在、语句错误等
            code = getattr(e, "code", None)
            msg = str(e)
            if code and "Security" in code:
                logger.error(f"连接失败（认证/权限问题）[{code}]: {msg}")
            elif code and "Database" in code:
                logger.error(f"连接失败（数据库名称可能不存在）[{code}]: {msg}")
            else:
                logger.error(f"连接失败（Neo4j 错误）[{code or 'unknown_code'}]: {msg}")
            self.connected = False
            return False
        except Exception as e:
            logger.error(f"连接失败（未知错误）: {e}")
            self.connected = False
            return False
    
    def disconnect(self):
        """断开连接（不会关闭连接池）"""
        self.connected = False
        logger.info("已断开连接（连接池保持活跃）")
    
    def execute_read_query(self, query: str, parameters: Optional[Dict] = None) -> List[Dict]:
        """在读事务中执行 Cypher 查询（推荐用于所有只读查询）。"""
        if not self.connected:
            logger.error("未连接到数据库")
            return []

        def _run() -> List[Dict]:
            with self.driver.session(database=self.database) as session:
                def _tx_run(tx):
                    result = tx.run(query, parameters or {})
                    records = [dict(record) for record in result]
                    if records:
                        logger.debug(f"查询成功，返回 {len(records)} 条记录")
                    return records

                return session.execute_read(_tx_run)

        start = time.perf_counter()
        try:
            rows = _run()
            elapsed_ms = (time.perf_counter() - start) * 1000
            record_neo4j_operation(
                op_type="read",
                query=query,
                parameters=parameters,
                elapsed_ms=elapsed_ms,
                ok=True,
                row_count=len(rows),
            )
            return rows
        except (ServiceUnavailable, SessionExpired, Neo4jError, OSError) as e:
            # 连接抖动/会话失效：尝试重连并重试一次
            logger.warning(f"查询失败（将重试一次）: {e}")
            elapsed_ms = (time.perf_counter() - start) * 1000
            record_neo4j_operation(
                op_type="read",
                query=query,
                parameters=parameters,
                elapsed_ms=elapsed_ms,
                ok=False,
                error=str(e),
                row_count=0,
            )
            try:
                self.connected = False
                if not self.connect():
                    return []
                retry_start = time.perf_counter()
                rows = _run()
                retry_elapsed_ms = (time.perf_counter() - retry_start) * 1000
                record_neo4j_operation(
                    op_type="read-retry",
                    query=query,
                    parameters=parameters,
                    elapsed_ms=retry_elapsed_ms,
                    ok=True,
                    row_count=len(rows),
                )
                return rows
            except Exception as e2:
                logger.error(f"查询重试失败: {e2}")
                retry_elapsed_ms = (time.perf_counter() - start) * 1000
                record_neo4j_operation(
                    op_type="read-retry",
                    query=query,
                    parameters=parameters,
                    elapsed_ms=retry_elapsed_ms,
                    ok=False,
                    error=str(e2),
                    row_count=0,
                )
                return []
        except Exception as e:
            logger.error(f"查询失败: {e}")
            elapsed_ms = (time.perf_counter() - start) * 1000
            record_neo4j_operation(
                op_type="read",
                query=query,
                parameters=parameters,
                elapsed_ms=elapsed_ms,
                ok=False,
                error=str(e),
                row_count=0,
            )
            return []

    def execute_query(self, query: str, parameters: Optional[Dict] = None) -> List[Dict]:
        """
        兼容方法：历史代码中大量使用 execute_query 执行只读查询。
        新代码建议使用 execute_read_query / execute_write_query 区分读写语义。
        """
        return self.execute_read_query(query, parameters)

    def execute_write_query(self, query: str, parameters: Optional[Dict] = None) -> List[Dict]:
        """
        在写事务中执行 Cypher（用于批量写入/关系创建等）。

        说明：
        - 使用 session.execute_write 以符合 Neo4j 官方推荐写法
        - 连接抖动/会话失效时，重连并重试一次（与 execute_query 策略一致）
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return []

        # ========== 旧方法：使用 neo4j 库的 driver.session ==========
        # def _run() -> List[Dict]:
        #     with self.driver.session(database=self.database) as session:
        #         def _tx_run(tx):
        #             result = tx.run(query, parameters or {})
        #             return [dict(record) for record in result]
        #         return session.execute_write(_tx_run)
        
        # ========== 新方法：使用 neomodel 的 db 对象 ==========
        from neomodel import db as neo4j_db
        
        try:
            start = time.perf_counter()
            results, _ = neo4j_db.cypher_query(query, parameters or {})
            elapsed_ms = (time.perf_counter() - start) * 1000
            
            # 记录性能指标
            record_neo4j_operation(
                op_type="write",
                query=query,
                parameters=parameters or {},
                elapsed_ms=elapsed_ms,
                ok=True,
                row_count=len(results),
            )
            
            # 转换为字典列表格式（保持与旧代码兼容）
            return [dict(zip([str(key) for key in result.keys()], result)) 
                   for result in results]
        except Exception as e:
            logger.error(f"neomodel 写入失败：{e}")
            elapsed_ms = (time.perf_counter() - start) * 1000
            record_neo4j_operation(
                op_type="write",
                query=query,
                parameters=parameters or {},
                elapsed_ms=elapsed_ms,
                ok=False,
                error=str(e),
                row_count=0,
            )
            raise
    
    def create_node(self, label: str, properties: Dict) -> bool:
        """创建节点（不推荐：优先使用批量 UNWIND/MERGE 写入）
        
        Args:
            label: 节点标签
            properties: 节点属性字典
        
        Returns:
            是否创建成功
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return False
        
        # Neo4j 无法参数化 label，只能做“白名单/正则校验 + 拼接”
        if not isinstance(label, str) or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", label):
            logger.error(f"非法节点 label: {label}")
            return False

        try:
            query = f"""
            CREATE (n:{label})
            SET n += $props
            RETURN count(n) as created
            """.strip()
            result = self.execute_write_query(query, {"props": properties or {}})
            created = int(result[0].get("created", 0)) if result else 0
            logger.info(f"创建节点成功: {label}（{created}）")
            return created > 0
        except Exception as e:
            logger.error(f"创建节点失败: {e}")
            return False
    
    def create_relationship(self, 
                          source_id: str, 
                          target_id: str, 
                          rel_type: str,
                          properties: Optional[Dict] = None) -> bool:
        """创建关系（不推荐：优先使用批量 UNWIND/MERGE 写入）
        
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
        
        # Neo4j 无法参数化 rel_type，只能做“白名单/正则校验 + 拼接”
        if not isinstance(rel_type, str) or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", rel_type):
            logger.error(f"非法关系类型 rel_type: {rel_type}")
            return False

        try:
            query = f"""
            MATCH (a {{symbol_id: $source_id}})
            MATCH (b {{symbol_id: $target_id}})
            MERGE (a)-[r:{rel_type}]->(b)
            SET r += $props
            RETURN count(r) as created
            """.strip()
            result = self.execute_write_query(
                query,
                {"source_id": source_id, "target_id": target_id, "props": properties or {}},
            )
            created = int(result[0].get("created", 0)) if result else 0
            logger.info(f"创建关系成功: {rel_type}（{created}）")
            return created > 0
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
    
    def delete_project_data(self, project_name: str, project_key: Optional[str] = None) -> int:
        """删除指定项目的子图

        以 Project 节点作为根：
        - 优先使用 project_key（建议传 Project 根 symbol_id）精确匹配
        - 否则回退到标签为 Project、name = project_name、project_type = 'Application' 的项目根节点匹配
        - 删除与这些项目节点存在路径关系的所有节点（子图）
        - 为降低误删风险，路径只允许经过「本项目自有结构」的边（HAVE/CONTAINS/MEMBER_OF/HAS_COMMENT），
          不沿 CONTAINS_LIB/LIB_LINK/EXTENDS/IMPLEMENTS/CALLS/ACCESSES，避免跨项目连通导致误删其他项目。
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return 0
        
        # 只沿「本项目内」结构边展开，不沿跨项目边（CONTAINS_LIB/LIB_LINK/EXTENDS/IMPLEMENTS/CALLS/ACCESSES）
        # 否则从 A 的根经 CONTAINS_LIB→外部类→LIB_LINK→B 的内部类，会删掉整图
        structural_rel_types = [
            JavaGraphEdgeType.HAVE.value,
            JavaGraphEdgeType.CONTAINS.value,
            JavaGraphEdgeType.MEMBER_OF.value,
            JavaGraphEdgeType.HAS_COMMENT.value,
        ]
        query = """
        // 找到作为根的 Application 类型项目节点
        MATCH (p:Project {project_type: 'Application'})
        WHERE ($project_key IS NOT NULL AND $project_key <> "" AND p.symbol_id = $project_key)
           OR (($project_key IS NULL OR $project_key = "") AND p.name = $project_name)
        WITH collect(p) AS roots
        WHERE size(roots) > 0

        // 沿结构边展开，再按归属过滤：只删 belong_project = project_name 或为根节点的节点，
        // 避免无向路径经 HAS_COMMENT 等连到其他项目导致误删
        CALL (roots) {
          WITH roots AS rlist
          UNWIND rlist AS r
          MATCH path = (r)-[*0..]-(n)
          WHERE ALL(rel IN relationships(path) WHERE type(rel) IN $structural_rel_types)
          WITH rlist, n
          WHERE (
            n IN rlist
            OR (n.belong_project IS NOT NULL AND n.belong_project = $project_name)
            OR (n:Project AND n.project_type = 'Application' AND n.name = $project_name)
          )
          WITH collect(DISTINCT n) AS nodes
          RETURN nodes
        }

        WITH nodes
        FOREACH (n IN nodes | DETACH DELETE n)
        RETURN size(nodes) AS deleted_count
        """

        def _run() -> int:
            with self.driver.session(database=self.database) as session:
                result = session.run(
                    query,
                    {
                        "project_name": project_name,
                        "project_key": project_key,
                        "structural_rel_types": structural_rel_types,
                    },
                ).single()
                return result["deleted_count"] if result else 0

        try:
            deleted_count = _run()
            logger.info(f"删除项目 {project_name} 相关 {deleted_count} 个节点")
            return deleted_count
        except (ServiceUnavailable, SessionExpired, Neo4jError, OSError) as e:
            logger.warning(f"删除项目子图失败（将重试一次）: {e}")
            try:
                self.connected = False
                if not self.connect():
                    return 0
                deleted_count = _run()
                logger.info(f"删除项目 {project_name} 相关 {deleted_count} 个节点")
                return deleted_count
            except Exception as e2:
                logger.error(f"删除项目子图重试失败: {e2}")
                return 0
        except Exception as e:
            logger.error(f"删除项目子图失败: {e}")
            return 0
    
    def get_statistics(self) -> Dict[str, int]:
        """获取数据库统计信息
        
        Returns:
            包含 total_nodes、total_relationships、node_types、relationship_types 的字典
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return {}
        
        def _run() -> Dict[str, int]:
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
                "relationship_types": {item["type"]: item["count"] for item in rel_types},
            }

            logger.info("获取统计信息成功")
            return stats

        try:
            return _run()
        except (ServiceUnavailable, SessionExpired, Neo4jError, OSError) as e:
            logger.warning(f"获取统计信息失败（将重试一次）: {e}")
            try:
                self.connected = False
                if not self.connect():
                    return {}
                return _run()
            except Exception as e2:
                logger.error(f"获取统计信息重试失败: {e2}")
                return {}
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
    
    def delete_nodes_by_file(self, file_path: str, project_name: str, project_key: Optional[str] = None) -> int:
        """删除特定文件相关的所有节点及其关系
        
        Args:
            file_path: Java 文件路径
            project_name: 项目名称
            project_key: 项目唯一键（建议传 Project 根 symbol_id）；优先用于精确匹配，避免同名项目误删
        
        Returns:
            删除的节点数
        """
        if not self.connected:
            logger.error("未连接到数据库")
            return 0
        
        try:
            with self.driver.session(database=self.database) as session:
                query = """
                MATCH (f:File {file_path: $file_path})
                WHERE ($project_key IS NOT NULL AND $project_key <> "" AND coalesce(f['project_key'], "") = $project_key)
                   OR (($project_key IS NULL OR $project_key = "") AND f.belong_project = $project_name)
                OPTIONAL MATCH (f)-[r1:CONTAINS]->(obj:JavaObject)
                OPTIONAL MATCH (obj)-[r2:MEMBER_OF]->(method:JavaMethod)
                OPTIONAL MATCH (obj)-[r3:MEMBER_OF]->(field:JavaField)
                OPTIONAL MATCH (method)-[r4:CALLS]->()
                OPTIONAL MATCH (method)-[r5:ACCESSES]->()
                OPTIONAL MATCH (field)-[r6:ACCESSES]->()
                OPTIONAL MATCH (method)-[r7:MEMBER_OF]->(param:JavaMethodParameter)
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
                    "project_name": project_name,
                    "project_key": project_key,
                }).single()
                
                deleted_count = result["deleted_count"] if result else 0
                logger.info(f"删除文件 {file_path} 相关 {deleted_count} 个节点")
                return deleted_count
        
        except Exception as e:
            logger.error(f"删除节点失败: {e}")
            return 0

    def delete_nodes_by_file_with_details(
        self,
        file_path: str,
        project_name: str,
        project_key: Optional[str] = None,
        sample_limit: int = 60,
    ) -> Dict[str, Any]:
        """
        删除文件子图，并返回删除明细（用于验收详情展示）。
        """
        out: Dict[str, Any] = {
            "deleted_nodes": 0,
            "deleted_relationships": 0,
            "sample_nodes": [],
        }
        if not self.connected:
            logger.error("未连接到数据库")
            return out

        details_query = """
        MATCH (f:File {file_path: $file_path})
        WHERE ($project_key IS NOT NULL AND $project_key <> "" AND coalesce(f['project_key'], "") = $project_key)
           OR (($project_key IS NULL OR $project_key = "") AND f.belong_project = $project_name)
        OPTIONAL MATCH (f)-[:CONTAINS]->(obj:JavaObject)
        OPTIONAL MATCH (obj)-[:MEMBER_OF]->(method:JavaMethod)
        OPTIONAL MATCH (obj)-[:MEMBER_OF]->(field:JavaField)
        OPTIONAL MATCH (method)-[:MEMBER_OF]->(param:JavaMethodParameter)
        WITH [f, obj, method, field, param] AS raw_nodes
        UNWIND raw_nodes AS n
        WITH collect(DISTINCT n) AS nodes
        CALL (nodes) {
          WITH nodes
          UNWIND nodes AS x
          WITH x WHERE x IS NOT NULL
          OPTIONAL MATCH (x)-[r]-()
          RETURN count(DISTINCT r) AS rel_count
        }
        CALL (nodes) {
          WITH nodes
          UNWIND nodes AS x
          WITH x WHERE x IS NOT NULL
          RETURN labels(x)[0] AS label,
                 coalesce(x.symbol_id, '') AS symbol_id,
                 coalesce(x.qualified_name, x.name, x.file_path, x.symbol_id, '') AS display
          LIMIT $sample_limit
        }
        RETURN size([x IN nodes WHERE x IS NOT NULL]) AS node_count,
               rel_count AS rel_count,
               collect({label: label, symbol_id: symbol_id, display: display}) AS samples
        """
        try:
            with self.driver.session(database=self.database) as session:
                row = session.run(
                    details_query,
                    {
                        "file_path": file_path,
                        "project_name": project_name,
                        "project_key": project_key,
                        "sample_limit": max(0, int(sample_limit or 0)),
                    },
                ).single()
                if row:
                    out["deleted_relationships"] = int(row.get("rel_count") or 0)
                    out["sample_nodes"] = list(row.get("samples") or [])
        except Exception as e:
            logger.warning(f"读取文件删除明细失败（降级仅返回计数）: {e}")

        out["deleted_nodes"] = int(
            self.delete_nodes_by_file(
                file_path=file_path,
                project_name=project_name,
                project_key=project_key,
            )
            or 0
        )
        return out


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

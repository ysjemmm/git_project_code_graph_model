"""
外部类链接器
用于将外部定义的类（EXTERNAL_DEFINITION）链接到实际项目中的类定义（INNER_DEFINITION）

使用场景：
- 类 A 作为 JAR 被项目 B 引用时，创建 EXTERNAL_DEFINITION 节点
- 类 A 作为项目源码导入时，创建 INNER_DEFINITION 节点
- 链接器将这两个节点通过 LIB_LINK 关系连接起来
"""
from typing import List, Dict, Optional
from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.java_modules import JavaGraphEdgeType
from storage.neo4j.queries import Neo4jQueries
from parser.utils.logger import get_logger

logger = get_logger("external_linker")


class ExternalClassLinker:
    """外部类链接器 - 链接同一个类的外部定义和内部定义"""
    
    def __init__(self, connector: Neo4jConnector):
        """
        初始化链接器
        
        参数:
            connector: Neo4j 连接器
        """
        self.connector = connector
    
    def link_all(self, dry_run: bool = False, sample_limit: int = 10) -> Dict:
        """
        链接所有外部定义到对应的内部定义
        
        匹配规则：
        - fqn（完全限定名）必须相同
        - belong_project（项目名）必须相同
        
        参数:
            dry_run: 如果为 True，只统计不实际创建关系
        
        返回:
            统计信息字典
        """
        logger.info("[INFO] 开始链接外部类到内部实现...")
        
        # 使用 Cypher 查询直接匹配并创建关系
        if dry_run:
            # 预览模式：只统计匹配数量
            query = Neo4jQueries.count_all_matches()
            result = self.connector.execute_query(query)
            record = next(iter(result), None)
            match_count = record['match_count'] if record else 0
            
            logger.info(f"[INFO] 找到 {match_count} 个可以链接的类")
            
            return {
                'success': True,
                'matches_found': match_count,
                'relationships_created': 0,
                'dry_run': True,
                'sample_limit': int(sample_limit),
            }
        else:
            # 实际创建关系
            query = Neo4jQueries.link_all_external_to_internal()
            result = self.connector.execute_write_query(query)
            record = next(iter(result), None)
            matched_count = int(record.get('matched_count', 0)) if record else 0
            created_count = int(record.get('created_count', 0)) if record else 0
            
            logger.info(f"[INFO] 匹配到 {matched_count} 对外部/内部定义，实际新建 {created_count} 条 LIB_LINK")
            
            return {
                'success': True,
                'matches_found': matched_count,
                'relationships_created': created_count,
                'dry_run': False,
                'matched_count': matched_count,
                'created_count': created_count,
            }
    
    def link_by_project(
        self,
        project_name: str,
        project_key: str = "",
        dry_run: bool = False,
        sample_limit: int = 10,
        include_sample: bool = True,
    ) -> Dict:
        """
        链接指定项目的外部定义到内部定义
        
        适用场景：刚导入一个新项目后，只链接该项目相关的类
        
        参数:
            project_name: 项目名称（兜底）
            project_key: 项目唯一键（建议 Project 根 symbol_id）；优先用于精确过滤
            dry_run: 如果为 True，只统计不实际创建关系
        
        返回:
            统计信息字典
        """
        logger.info(f"[INFO] 链接项目 '{project_name}' 的外部类...")
        
        sample: Optional[list[dict]] = None
        if include_sample and sample_limit and int(sample_limit) > 0:
            try:
                sample_query = Neo4jQueries.sample_matches_by_project()
                sample_rows = self.connector.execute_query(
                    sample_query,
                    {"project_name": project_name, "project_key": project_key, "limit": int(sample_limit)},
                )
                sample = [dict(r) for r in (sample_rows or [])]
            except Exception:
                sample = None

        if dry_run:
            query = Neo4jQueries.count_matches_by_project()
            result = self.connector.execute_query(query, {'project_name': project_name, 'project_key': project_key})
            record = next(iter(result), None)
            match_count = record['match_count'] if record else 0
            
            return {
                'success': True,
                'project_name': project_name,
                'matches_found': match_count,
                'relationships_created': 0,
                'dry_run': True,
                'sample_limit': int(sample_limit),
                'sample': sample,
            }
        else:
            query = Neo4jQueries.link_external_to_internal_by_project()
            result = self.connector.execute_write_query(query, {'project_name': project_name, 'project_key': project_key})
            record = next(iter(result), None)
            matched_count = int(record.get('matched_count', 0)) if record else 0
            created_count = int(record.get('created_count', 0)) if record else 0
            
            return {
                'success': True,
                'project_name': project_name,
                'matches_found': matched_count,
                'relationships_created': created_count,
                'dry_run': False,
                'matched_count': matched_count,
                'created_count': created_count,
                'sample_limit': int(sample_limit),
                'sample': sample,
            }
    
    def get_statistics(self) -> Dict:
        """
        获取链接统计信息
        
        返回:
            统计信息字典
        """
        query = Neo4jQueries.count_lib_links()
        result = self.connector.execute_query(query)
        record = next(iter(result), None)
        linked_count = record['linked_count'] if record else 0
        
        # 统计未链接的外部类
        unlinked_query = Neo4jQueries.count_unlinked_externals()
        result = self.connector.execute_query(unlinked_query)
        record = next(iter(result), None)
        unlinked_count = record['unlinked_count'] if record else 0
        
        return {
            'linked_count': linked_count,
            'unlinked_count': unlinked_count
        }

    def link_lib_to_application(self) -> Dict:
        """
        为同名 Project(Lib) 与 Project(Application) 建立 SAME_ARTIFACT（Lib -> Application），
        便于从 A 依赖的 Lib B 跳到 B 的 Application 源码。
        返回: { success, created_count }
        """
        try:
            query = Neo4jQueries.merge_same_artifact_links()
            result = self.connector.execute_write_query(query)
            record = next(iter(result), None)
            created_count = int(record.get('created_count', 0)) if record else 0
            logger.info(f"[INFO] 同名 Lib/Application 建立 SAME_ARTIFACT: {created_count} 条")
            return {'success': True, 'created_count': created_count}
        except Exception as e:
            logger.warning(f"[WARN] 建立 SAME_ARTIFACT 失败: {e}")
            return {'success': False, 'created_count': 0}
    
    def find_unlinked_external_classes(self, limit: int = 100) -> List[Dict]:
        """
        查找未链接到内部实现的外部类
        
        参数:
            limit: 返回结果数量限制
        
        返回:
            未链接的外部类列表
        """
        query = Neo4jQueries.find_unlinked_externals()
        result = self.connector.execute_query(query, {'limit': limit})
        return [dict(record) for record in result]
    
    def find_duplicate_definitions(self, limit: int = 100) -> List[Dict]:
        """
        查找同时存在外部定义和内部定义的类
        
        这些类应该被链接，如果还没有链接的话
        
        参数:
            limit: 返回结果数量限制
        
        返回:
            重复定义的类列表
        """
        query = Neo4jQueries.find_duplicate_definitions()
        result = self.connector.execute_query(query, {'limit': limit})
        return [dict(record) for record in result]

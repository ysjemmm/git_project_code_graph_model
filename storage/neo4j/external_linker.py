"""
外部类链接器
用于将外部定义的类（EXTERNAL_DEFINITION）链接到实际项目中的类定义（INNER_DEFINITION）

使用场景：
- 类 A 作为 JAR 被项目 B 引用时，创建 EXTERNAL_DEFINITION 节点
- 类 A 作为项目源码导入时，创建 INNER_DEFINITION 节点
- 链接器将这两个节点通过 LIB_LINK 关系连接起来
"""
from typing import List, Dict
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
    
    def link_all(self, dry_run: bool = False) -> Dict:
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
                'dry_run': True
            }
        else:
            # 实际创建关系
            query = Neo4jQueries.link_all_external_to_internal()
            result = self.connector.execute_query(query)
            record = next(iter(result), None)
            created_count = record['created_count'] if record else 0
            
            logger.info(f"[INFO] 创建了 {created_count} 个链接关系")
            
            return {
                'success': True,
                'matches_found': created_count,
                'relationships_created': created_count,
                'dry_run': False
            }
    
    def link_by_project(self, project_name: str, dry_run: bool = False) -> Dict:
        """
        链接指定项目的外部定义到内部定义
        
        适用场景：刚导入一个新项目后，只链接该项目相关的类
        
        参数:
            project_name: 项目名称
            dry_run: 如果为 True，只统计不实际创建关系
        
        返回:
            统计信息字典
        """
        logger.info(f"[INFO] 链接项目 '{project_name}' 的外部类...")
        
        if dry_run:
            query = Neo4jQueries.count_matches_by_project()
            result = self.connector.execute_query(query, {'project_name': project_name})
            record = next(iter(result), None)
            match_count = record['match_count'] if record else 0
            
            return {
                'success': True,
                'project_name': project_name,
                'matches_found': match_count,
                'relationships_created': 0,
                'dry_run': True
            }
        else:
            query = Neo4jQueries.link_external_to_internal_by_project()
            result = self.connector.execute_query(query, {'project_name': project_name})
            record = next(iter(result), None)
            created_count = record['created_count'] if record else 0
            
            return {
                'success': True,
                'project_name': project_name,
                'matches_found': created_count,
                'relationships_created': created_count,
                'dry_run': False
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

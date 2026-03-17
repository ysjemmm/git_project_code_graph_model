"""
Neo4j Cypher 查询管理模块

集中管理所有 Cypher 查询语句，提供参数化查询方法
"""
from typing import Dict, List

from storage.neo4j.java_modules import (
    REL_ENDPOINT_LABELS,
    MEMBER_OF_SOURCE_LABELS,
    MEMBER_OF_TARGET_LABELS,
)


class Neo4jQueries:
    """Neo4j 查询语句集合"""
    
    # ==================== 外部类链接查询 ====================
    
    @staticmethod
    def batch_find_internals() -> str:
        """
        批量查找内部定义节点
        
        参数:
            conditions: List[Dict] - 查询条件列表
              - fqn: 完全限定名
              - project_key: 可选，项目唯一键（建议 Project 根 symbol_id），用于精确过滤
              - project: 兜底项目名（旧字段）
        
        返回字段:
            fqn, project, symbol_id
        """
        return """
        UNWIND $conditions AS cond
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = cond.fqn
          AND internal.belong_project = cond.project
          AND (
            cond.project_key IS NULL OR cond.project_key = "" OR internal.project_key = cond.project_key OR internal.project_key IS NULL
          )
        RETURN internal.qualified_name as fqn,
               internal.belong_project as project,
               internal.symbol_id as symbol_id
        """
    
    @staticmethod
    def batch_find_externals() -> str:
        """
        批量查找外部定义节点
        
        参数:
            conditions: List[Dict] - 查询条件列表
              - fqn: 完全限定名
              - project_key: 可选，项目唯一键（建议 Project 根 symbol_id），用于精确过滤
              - project: 兜底项目名（旧字段）
        
        返回字段:
            fqn, project, symbol_id
        """
        return """
        UNWIND $conditions AS cond
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND external.qualified_name = cond.fqn
          AND external.belong_project = cond.project
          AND (
            cond.project_key IS NULL OR cond.project_key = "" OR external.project_key = cond.project_key OR external.project_key IS NULL
          )
        RETURN external.qualified_name as fqn,
               external.belong_project as project,
               external.symbol_id as symbol_id
        """
    
    @staticmethod
    def find_internal_by_fqn() -> str:
        """
        根据 FQN 和项目名查找单个内部定义
        
        参数:
            fqn: str - 完全限定名
            project_name: str - 项目名称（兜底）
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id），优先用于精确过滤
        
        返回字段:
            symbol_id
        """
        return """
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = $fqn
          AND internal.belong_project = $project_name
          AND (
            $project_key IS NULL OR $project_key = "" OR internal.project_key = $project_key OR internal.project_key IS NULL
          )
        RETURN internal.symbol_id as symbol_id
        LIMIT 1
        """
    
    @staticmethod
    def find_externals_by_fqn() -> str:
        """
        根据 FQN 和项目名查找外部定义（可能有多个）
        
        参数:
            fqn: str - 完全限定名
            project_name: str - 项目名称（兜底）
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id），优先用于精确过滤
        
        返回字段:
            symbol_id
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND external.qualified_name = $fqn
          AND external.belong_project = $project_name
          AND (
            $project_key IS NULL OR $project_key = "" OR external.project_key = $project_key OR external.project_key IS NULL
          )
        RETURN external.symbol_id as symbol_id
        """
    
    @staticmethod
    def link_all_external_to_internal() -> str:
        """
        链接所有外部定义到内部定义（全局匹配）
        
        返回字段:
            matched_count, created_count
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        MERGE (external)-[r:LIB_LINK]->(internal)
        ON CREATE SET r.__tmp_created = 1
        WITH r, coalesce(r.__tmp_created, 0) AS is_new
        SET r.__tmp_created = null
        WITH count(r) AS matched_count, sum(is_new) AS created_count
        RETURN matched_count, created_count
        """
    
    @staticmethod
    def link_external_to_internal_by_project() -> str:
        """
        链接指定项目的外部定义到内部定义
        
        参数:
            project_name: str - 项目名称（兜底）
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id），优先用于精确过滤
        
        返回字段:
            matched_count, created_count
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND external.belong_project = $project_name
          AND (
            $project_key IS NULL OR $project_key = "" OR external.project_key = $project_key OR external.project_key IS NULL
          )
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        MERGE (external)-[r:LIB_LINK]->(internal)
        ON CREATE SET r.__tmp_created = 1
        WITH r, coalesce(r.__tmp_created, 0) AS is_new
        SET r.__tmp_created = null
        WITH count(r) AS matched_count, sum(is_new) AS created_count
        RETURN matched_count, created_count
        """

    @staticmethod
    def sample_matches_by_project() -> str:
        """
        采样：返回可链接的 external/internal 对，以及是否已存在 LIB_LINK。

        参数:
            project_name: str - 项目名称（兜底）
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id），优先用于精确过滤
            limit: int - 返回数量限制

        返回字段:
            fqn, project, external_symbol_id, internal_symbol_id, is_linked
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND external.belong_project = $project_name
          AND (
            $project_key IS NULL OR $project_key = "" OR external.project_key = $project_key OR external.project_key IS NULL
          )
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        OPTIONAL MATCH (external)-[r:LIB_LINK]->(internal)
        RETURN external.qualified_name AS fqn,
               external.belong_project AS project,
               external.symbol_id AS external_symbol_id,
               internal.symbol_id AS internal_symbol_id,
               r IS NOT NULL AS is_linked
        ORDER BY project, fqn
        LIMIT $limit
        """
    
    @staticmethod
    def count_all_matches() -> str:
        """
        统计所有可以链接的外部-内部定义对（不实际创建链接）
        
        返回字段:
            match_count
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        RETURN count(*) as match_count
        """
    
    @staticmethod
    def count_matches_by_project() -> str:
        """
        统计指定项目可以链接的外部-内部定义对
        
        参数:
            project_name: str - 项目名称（兜底）
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id），优先用于精确过滤
        
        返回字段:
            match_count
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND external.belong_project = $project_name
          AND (
            $project_key IS NULL OR $project_key = "" OR external.project_key = $project_key
          )
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        RETURN count(*) as match_count
        """
    
    # ==================== 统计查询 ====================
    
    @staticmethod
    def count_lib_links() -> str:
        """
        统计已创建的 LIB_LINK 关系数量
        
        返回字段:
            linked_count
        """
        return """
        MATCH (external:JavaObject)-[r:LIB_LINK]->(internal:JavaObject)
        RETURN count(r) as linked_count
        """
    
    @staticmethod
    def count_unlinked_externals() -> str:
        """
        统计未链接的外部定义数量
        
        返回字段:
            unlinked_count
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND NOT (external)-[:LIB_LINK]->()
        RETURN count(external) as unlinked_count
        """
    
    @staticmethod
    def find_unlinked_externals() -> str:
        """
        查找未链接的外部定义
        
        参数:
            limit: int - 返回结果数量限制
        
        返回字段:
            symbol_id, fqn, belong_project
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
          AND NOT (external)-[:LIB_LINK]->()
        RETURN external.symbol_id as symbol_id,
               external.qualified_name as fqn,
               external.belong_project as belong_project
        ORDER BY external.belong_project, external.qualified_name
        LIMIT $limit
        """
    
    @staticmethod
    def find_duplicate_definitions() -> str:
        """
        查找同时存在外部定义和内部定义的类
        
        参数:
            limit: int - 返回结果数量限制
        
        返回字段:
            fqn, project, external_symbol_id, internal_symbol_id, is_linked
        """
        return """
        MATCH (external:JavaObject)
        WHERE external.from_type = 'ExternalDefinition'
        MATCH (internal:JavaObject)
        WHERE internal.from_type = 'InnerDefinition'
          AND internal.qualified_name = external.qualified_name
          AND internal.belong_project = external.belong_project
          AND (
            external.project_key IS NULL OR external.project_key = "" OR internal.project_key = external.project_key OR internal.project_key IS NULL
          )
        OPTIONAL MATCH (external)-[r:LIB_LINK]->(internal)
        RETURN external.qualified_name as fqn,
               external.belong_project as project,
               external.symbol_id as external_symbol_id,
               internal.symbol_id as internal_symbol_id,
               r IS NOT NULL as is_linked
        ORDER BY project, fqn
        LIMIT $limit
        """
    
    # ==================== 批量操作查询 ====================
    
    @staticmethod
    def batch_create_nodes() -> str:
        """
        批量创建节点（使用 UNWIND）
        
        参数:
            nodes: List[Dict] - 节点数据列表
            node_type: str - 节点类型标签
            unique_keys: str - 唯一键字段（逗号分隔）
        
        返回字段:
            created
        
        注意：此方法返回模板，需要动态构建完整查询
        """
        # 这个方法需要动态构建，因为 unique_keys 是变化的
        # 实际使用时在 exporter.py 中构建
        return """
        UNWIND $nodes AS node
        MERGE (n:{node_type} {{{unique_keys}}})
        SET n += node
        RETURN count(n) as created
        """
    
    @staticmethod
    def batch_create_relationships() -> str:
        """
        批量创建关系（使用 UNWIND）
        
        参数:
            relationships: List[Dict] - 关系数据列表，每个包含 source_id, target_id
            rel_type: str - 关系类型
        
        返回字段:
            created
        
        注意：此方法返回模板，需要动态构建完整查询
        """
        # 这个方法需要动态构建，因为 rel_type 是变化的
        # 实际使用时在 exporter.py 中构建
        return """
        UNWIND $relationships AS rel
        MATCH (source {{symbol_id: rel.source_id}})
        MATCH (target {{symbol_id: rel.target_id}})
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN count(r) as created
        """
    
    # ==================== 删除操作查询 ====================
    
    @staticmethod
    def delete_nodes_by_file() -> str:
        """
        删除指定文件相关的所有节点及其关系
        
        参数:
            file_path: str - 文件路径
            project_name: str - 项目名称
            project_key: str|None - 项目唯一键（建议 Project 根 symbol_id）；优先用于精确匹配
        
        返回字段:
            deleted_count
        """
        return """
        MATCH (f:JavaFile {file_path: $file_path})
        WHERE ($project_key IS NOT NULL AND $project_key <> "" AND f.project_key = $project_key)
           OR (($project_key IS NULL OR $project_key = "") AND f.belong_project = $project_name)
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


class QueryBuilder:
    """查询构建器 - 用于动态构建复杂查询"""
    
    @staticmethod
    def build_batch_create_nodes_query(node_type: str, unique_key_names: List[str]) -> str:
        """
        构建批量创建节点的查询
        
        参数:
            node_type: 节点类型标签
            unique_key_names: 唯一键字段列表
        
        返回:
            完整的 Cypher 查询语句
        """
        unique_keys = ", ".join([f"{k}: node.{k}" for k in unique_key_names])
        
        query = f"""
        UNWIND $nodes AS node
        MERGE (n:{node_type} {{{unique_keys}}})
        SET n += node
        RETURN count(n) as created
        """
        return query.strip()
    
    @staticmethod
    def build_batch_create_relationships_query(rel_type: str) -> str:
        """
        构建批量创建关系的查询（带 label 的 MATCH，走 symbol_id 索引，加速写入）。

        若 REL_ENDPOINT_LABELS 中指定了该 rel_type 的 (source_label, target_label)，
        则使用 MATCH (source:SourceLabel) / MATCH (target:TargetLabel)，避免全图按 symbol_id 扫描。
        """
        # 带 label 的 MATCH 走各 label 的 symbol_id 索引，避免全图扫描；两条 MATCH 避免笛卡尔积
        labels = REL_ENDPOINT_LABELS.get(rel_type)
        if labels:
            source_label, target_label = labels
            query = f"""
        UNWIND $relationships AS rel
        MATCH (source:{source_label}) WHERE source.symbol_id = rel.source_id
        MATCH (target:{target_label}) WHERE target.symbol_id = rel.target_id
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN count(r) AS created
            """
        elif rel_type == "MEMBER_OF":
            query = f"""
        UNWIND $relationships AS rel
        MATCH (source) WHERE source.symbol_id = rel.source_id AND ({MEMBER_OF_SOURCE_LABELS})
        MATCH (target) WHERE target.symbol_id = rel.target_id AND ({MEMBER_OF_TARGET_LABELS})
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN count(r) AS created
            """
        else:
            query = f"""
        UNWIND $relationships AS rel
        MATCH (source) WHERE source.symbol_id = rel.source_id
        MATCH (target) WHERE target.symbol_id = rel.target_id
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN count(r) AS created
            """
        return query.strip()
    
    @staticmethod
    def build_find_nodes_query(
        node_type: str,
        conditions: Dict[str, str],
        return_fields: List[str],
        limit: int = None
    ) -> str:
        """
        构建查找节点的查询
        
        参数:
            node_type: 节点类型标签
            conditions: 查询条件字典 {字段名: 参数名}
            return_fields: 返回字段列表
            limit: 结果数量限制（可选）
        
        返回:
            完整的 Cypher 查询语句
        """
        where_clauses = [f"n.{field} = ${param}" for field, param in conditions.items()]
        where_str = " AND ".join(where_clauses)
        return_str = ", ".join([f"n.{field} as {field}" for field in return_fields])
        
        query = f"""
        MATCH (n:{node_type})
        WHERE {where_str}
        RETURN {return_str}
        """
        
        if limit:
            query += f"\nLIMIT {limit}"
        
        return query.strip()

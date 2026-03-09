#!/usr/bin/env python3

from typing import Dict, List, Any


class MergeQueryBuilder:
    """MERGE 查询构建"""

    @staticmethod
    def build_merge_node_query(node_type: str, node_dict: Dict[str, Any]) -> str:
        """构建节点 MERGE 查询，使用唯一键防止重复插入"""
        # 获取该节点类型的唯一键
        unique_key_names = get_unique_key_for_node_type(node_type)
        unique_key_dict = {k: node_dict.get(k) for k in unique_key_names if k in node_dict}
        
        # 构建唯一键条件 (用于 MERGE)
        unique_key_str = ", ".join([f"{k}: ${k}" for k in unique_key_dict.keys()])
        
        # 构建所有属性设置
        all_properties = ", ".join([f"n.{k} = ${k}" for k in node_dict.keys()])
        
        query = f"""
        MERGE (n:{node_type} {{{unique_key_str}}})
        ON CREATE SET {all_properties}
        ON MATCH SET {all_properties}
        RETURN n
        """
        
        return query.strip()

    @staticmethod
    def build_merge_query(node_type: str, unique_key: Dict[str, Any],
                          properties: Dict[str, Any]) -> str:
        """构建 MERGE 查询，用于更新或创建节点"""
        # 构建唯一键条件
        unique_key_str = ", ".join([f"{k}: ${k}" for k in unique_key.keys()])

        # 构建 ON CREATE SET ON MATCH SET 的属性设置
        property_keys = [k for k in properties.keys() if k not in unique_key.keys()]
        property_sets = ", ".join([f"n.{k} = ${k}" for k in property_keys])

        # 如果没有额外属性,只设置唯一键
        if not property_sets:
            property_sets = "n.symbol_id = $symbol_id"

        query = f"""
        MERGE (n:{node_type} {{{unique_key_str}}})
        ON CREATE SET {property_sets}
        ON MATCH SET {property_sets}
        RETURN n
        """

        return query.strip()

    @staticmethod
    def build_merge_relationship_query(source_id: str, target_id: str, rel_type: str) -> str:
        """构建关系 MERGE 查询，使用唯一键防止重复创建"""
        query = f"""
        MATCH (source {{symbol_id: $source_id}})
        MATCH (target {{symbol_id: $target_id}})
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN r
        """
        
        return query.strip()

    @staticmethod
    def build_merge_relationship_query_with_properties(rel_type: str,
                                                       properties: List[str]) -> str:
        """构建带属性的关系 MERGE 查询"""
        if not properties:
            return MergeQueryBuilder.build_merge_relationship_query("", "", rel_type)

        # 构建 ON CREATE SET ON MATCH SET
        property_sets = ", ".join([f"r.{p} = $rel_{p}" for p in properties])

        query = f"""
        MATCH (source {{symbol_id: $source_id}})
        MATCH (target {{symbol_id: $target_id}})
        MERGE (source)-[r:{rel_type}]->(target)
        ON CREATE SET {property_sets}
        ON MATCH SET {property_sets}
        RETURN r
        """

        return query.strip()

    @staticmethod
    def get_unique_key_from_node(node: Any) -> Dict[str, Any]:

        if not hasattr(node, 'get_unique_key'):
            raise ValueError(f"节点对象必须实现 get_unique_key 方法")

        return node.get_unique_key()

    @staticmethod
    def extract_unique_key_from_dict(node_dict: Dict[str, Any],
                                     unique_key_names: List[str]) -> Dict[str, Any]:

        return {k: node_dict.get(k) for k in unique_key_names if k in node_dict}


# 节点类型的唯一键定义
NODE_UNIQUE_KEYS = {
    'Project': ['symbol_id'],  # symbol_id 已包含 project_type 和 version 信息
    'File': ['symbol_id'],  # 添加 File 节点类型
    'JavaObject': ['symbol_id'],  # 修改为只用 symbol_id
    'JavaMethod': ['symbol_id'],  # 添加 JavaMethod
    'JavaField': ['symbol_id'],  # 添加 JavaField
    'JavaMethodParameter': ['symbol_id'],  # 添加 JavaMethodParameter
    'JavaEnumConstant': ['symbol_id'],  # 添加 JavaEnumConstant
    'JavaRecordComponent': ['symbol_id'],  # 添加 JavaRecordComponent
    'JavaCodeBlock': ['symbol_id'],
    'Comment': ['symbol_id'],  # 添加 Comment 节点类型
}

# 关系类型的唯一键定义
# 所有关系的唯一键都是 source + target + relationship_type
RELATIONSHIP_UNIQUE_KEYS = {
    'HAVE': ['source', 'target'],
    'CONTAINS': ['source', 'target'],
    'MEMBER_OF': ['source', 'target'],
    'EXTENDS': ['source', 'target'],
    'IMPLEMENTS': ['source', 'target'],
    'CALLS': ['source', 'target'],
    'ACCESSES': ['source', 'target'],
    'HAS_COMMENT': ['source', 'target'],  # 添加 HAS_COMMENT 关系
}

# 关系类型的可更新属性定义
RELATIONSHIP_PROPERTIES = {
    'HAVE': [],  # 无属性
    'CONTAINS': [],  # 无属性
    'MEMBER_OF': [],  # 无属性
    'EXTENDS': [],  # 无属性
    'IMPLEMENTS': [],  # 无属性
    'CALLS': ['call_count', 'last_call_line'],  # 调用次数、最后调用行
    'ACCESSES': ['access_count', 'access_type', 'last_access_line'],  # 访问次数、访问类型、最后访问行
    'HAS_COMMENT': [],  # 无属性
}


def get_unique_key_for_node_type(node_type: str) -> List[str]:
    return NODE_UNIQUE_KEYS.get(node_type, ['symbol_id'])


def get_unique_key_for_relationship_type(rel_type: str) -> List[str]:
    return RELATIONSHIP_UNIQUE_KEYS.get(rel_type, ['source', 'target'])


def get_properties_for_relationship_type(rel_type: str) -> List[str]:
    return RELATIONSHIP_PROPERTIES.get(rel_type, [])

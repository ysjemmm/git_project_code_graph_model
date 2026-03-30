from __future__ import annotations

from typing import Dict, List

from parser.languages.java.java_constants import ObjectFromType
from storage.neo4j.graph_schema import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
)
from storage.neo4j.queries import Neo4jQueries


class JavaExternalLinkPreparationMixin:
    """
    exporter 迁移段：在内存中准备外部类链接（LIB_LINK）。

    说明：
    - 这是旧逻辑的兼容实现，依赖 self.connector 执行 Neo4j 读查询
    - 新主流程推荐在落库后使用 ExternalClassLinker.link_by_project()
    """

    def _prepare_external_links(self):
        try:
            if not getattr(self, "connector", None):
                return

            current_external = []
            current_internal = []

            for node in self.nodes_to_create.get(JavaNeo4jNodeType.JavaObject, []):
                node_from_type = node.get("from_type") if isinstance(node, dict) else getattr(node, "from_type", None)
                if node_from_type == ObjectFromType.EXTERNAL_DEFINITION.value:
                    current_external.append(node)
                elif node_from_type == ObjectFromType.INNER_DEFINITION.value:
                    current_internal.append(node)

            if not current_external and not current_internal:
                return

            # 场景 1: 新的外部定义 → 批量查找已有的内部定义
            if current_external:
                external_conditions = [
                    {
                        "fqn": node.get("qualified_name") if isinstance(node, dict) else node.qualified_name,
                        "project_key": node.get("project_key") if isinstance(node, dict) else getattr(node, "project_key", self.project_id),
                        "project": node.get("belong_project") if isinstance(node, dict) else node.belong_project,
                    }
                    for node in current_external
                ]
                internal_nodes_map = self._batch_find_internals_in_db(external_conditions)
                for external_node in current_external:
                    qn = external_node.get("qualified_name") if isinstance(external_node, dict) else external_node.qualified_name
                    bp = external_node.get("belong_project") if isinstance(external_node, dict) else external_node.belong_project
                    sid = external_node.get("symbol_id") if isinstance(external_node, dict) else external_node.symbol_id
                    key = (qn, bp)
                    if key in internal_nodes_map:
                        self.relationships_to_create.append(
                            (sid, internal_nodes_map[key], JavaGraphEdgeType.LIB_LINK.value)
                        )

            # 场景 2: 新的内部定义 → 批量查找已有的外部定义
            if current_internal:
                internal_conditions = [
                    {
                        "fqn": node.get("qualified_name") if isinstance(node, dict) else node.qualified_name,
                        "project_key": node.get("project_key") if isinstance(node, dict) else getattr(node, "project_key", self.project_id),
                        "project": node.get("belong_project") if isinstance(node, dict) else node.belong_project,
                    }
                    for node in current_internal
                ]
                external_nodes_map = self._batch_find_externals_in_db(internal_conditions)
                for internal_node in current_internal:
                    qn = internal_node.get("qualified_name") if isinstance(internal_node, dict) else internal_node.qualified_name
                    bp = internal_node.get("belong_project") if isinstance(internal_node, dict) else internal_node.belong_project
                    sid = internal_node.get("symbol_id") if isinstance(internal_node, dict) else internal_node.symbol_id
                    key = (qn, bp)
                    if key in external_nodes_map:
                        for external_symbol_id in external_nodes_map[key]:
                            self.relationships_to_create.append(
                                (external_symbol_id, sid, JavaGraphEdgeType.LIB_LINK.value)
                            )

        except Exception:
            # 兼容逻辑：准备失败不影响导入主流程
            return

    def _batch_find_internals_in_db(self, conditions: List[Dict]) -> Dict:
        if not conditions:
            return {}

        query = Neo4jQueries.batch_find_internals()
        result = self.connector.execute_query(query, {"conditions": conditions})

        result_map = {}
        for record in result:
            key = (record["fqn"], record["project"])
            result_map[key] = record["symbol_id"]
        return result_map

    def _batch_find_externals_in_db(self, conditions: List[Dict]) -> Dict:
        if not conditions:
            return {}

        query = Neo4jQueries.batch_find_externals()
        result = self.connector.execute_query(query, {"conditions": conditions})

        result_map = {}
        for record in result:
            key = (record["fqn"], record["project"])
            result_map.setdefault(key, []).append(record["symbol_id"])
        return result_map

    def _find_internal_in_db(self, fqn: str, project_name: str) -> dict:
        query = Neo4jQueries.find_internal_by_fqn()
        result = self.connector.execute_query(
            query,
            {"fqn": fqn, "project_name": project_name, "project_key": self.project_id},
        )
        records = list(result)
        return dict(records[0]) if records else None

    def _find_externals_in_db(self, fqn: str, project_name: str) -> list:
        query = Neo4jQueries.find_externals_by_fqn()
        result = self.connector.execute_query(
            query,
            {"fqn": fqn, "project_name": project_name, "project_key": self.project_id},
        )
        return [dict(record) for record in result]

    def _count_external_links(self) -> int:
        return sum(
            1
            for rel in self.relationships_to_create
            if rel[2] == JavaGraphEdgeType.LIB_LINK.value
        )


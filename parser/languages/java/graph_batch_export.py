from __future__ import annotations

from dataclasses import asdict, is_dataclass
from typing import Any

from core.graph_ir import GraphBatch


class JavaGraphBatchExportMixin:
    """
    Java 构建阶段：prepare_from_ast_data + build_graph_batch（从旧 Java exporter 实现迁移出来）。

    约束：
    - 宿主类需提供：
      - self.project_id
      - self.nodes_to_create / self.relationships_to_create
      - self._prepare_project_node / _collect_ast_file_nodes / _parse_extend_impl_relationships / _prepare_external_links
    """

    def prepare_from_ast_data(self, ast_data_list: list, auto_link_external: bool = True) -> GraphBatch:
        project_node = self._prepare_project_node()

        for ast_data in ast_data_list:
            if ast_data:
                self._collect_ast_file_nodes(ast_data, project_node)

        self._parse_extend_impl_relationships(ast_data_list)

        if auto_link_external:
            self._prepare_external_links()

        return self.build_graph_batch()

    def build_graph_batch(self) -> GraphBatch:
        nodes: dict[str, list[dict]] = {}
        for node_type, items in (self.nodes_to_create or {}).items():
            if not items:
                continue
            label = node_type.value
            bucket: list[dict] = []
            for node in items:
                if isinstance(node, dict):
                    node_dict = node
                elif is_dataclass(node):
                    node_dict = asdict(node)
                elif hasattr(node, "__dict__"):
                    node_dict = node.__dict__
                else:
                    continue

                if isinstance(node_dict, dict) and "project_key" not in node_dict:
                    node_dict["project_key"] = getattr(self, "project_id", None)

                bucket.append(node_dict)
            if bucket:
                nodes[label] = bucket

        rels: dict[str, list[dict]] = {}
        for s, t, rel_type in (self.relationships_to_create or []):
            rels.setdefault(rel_type, []).append({"source_id": s, "target_id": t})

        return GraphBatch(nodes=nodes, relationships=rels)


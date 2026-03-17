from __future__ import annotations

from typing import Any, Dict

from storage.neo4j.merge_builder import get_unique_key_for_node_type


class JavaNodeDedupeMixin:
    """
    Java exporter 构建阶段去重小工具（基于唯一键判断节点是否已在待创建列表中）。

    约束：
    - 宿主类需提供 self.nodes_to_create（dict-like）
    """

    def _node_exists_in_list(self, node_type: Any, node_data: Dict[str, Any]) -> bool:
        unique_keys = get_unique_key_for_node_type(node_type.value)
        unique_values = {key: node_data.get(key) for key in unique_keys}

        nodes_list = self.nodes_to_create.get(node_type, [])
        for node in nodes_list:
            if isinstance(node, dict):
                node_values = {key: node.get(key) for key in unique_keys}
            else:
                node_values = {key: getattr(node, key, None) for key in unique_keys}
            if node_values == unique_values:
                return True
        return False


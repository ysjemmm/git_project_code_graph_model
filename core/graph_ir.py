from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Dict, Iterable, List, Set, Tuple

logger = logging.getLogger(__name__)

# 关键 label 的最小必填字段（软约束：缺则 warning 并丢弃，不抛错）
_KEY_LABEL_REQUIRED: Dict[str, Set[str]] = {
    "Project": {"symbol_id", "name"},
    "File": {"symbol_id"},
    "JavaObject": {"symbol_id", "qualified_name"},
    "JavaMethod": {"symbol_id", "name"},
}


@dataclass(frozen=True, slots=True)
class GraphBatch:
    """
    语言无关的图写入中间表示（IR）。

    约定：
    - nodes: label -> 节点属性 dict 列表（每个 dict 至少应包含 symbol_id）
    - relationships: rel_type -> 关系列表（每项包含 source_id/target_id）
    """

    nodes: Dict[str, List[dict]]
    relationships: Dict[str, List[dict]]

    def validate(self) -> None:
        """
        轻量校验（用于线上尽早失败，而不是落库时才报 Cypher 错）。
        """
        for label, items in (self.nodes or {}).items():
            if not isinstance(label, str) or not label:
                raise ValueError("GraphBatch.nodes 的 key 必须是非空字符串 label")
            for idx, node in enumerate(items or []):
                if not isinstance(node, dict):
                    raise ValueError(f"GraphBatch.nodes[{label}][{idx}] 必须是 dict")
                sid = node.get("symbol_id")
                if not sid:
                    raise ValueError(f"GraphBatch.nodes[{label}][{idx}] 缺少 symbol_id")

        for rel_type, items in (self.relationships or {}).items():
            if not isinstance(rel_type, str) or not rel_type:
                raise ValueError("GraphBatch.relationships 的 key 必须是非空字符串 rel_type")
            for idx, rel in enumerate(items or []):
                if not isinstance(rel, dict):
                    raise ValueError(f"GraphBatch.relationships[{rel_type}][{idx}] 必须是 dict")
                if not rel.get("source_id") or not rel.get("target_id"):
                    raise ValueError(f"GraphBatch.relationships[{rel_type}][{idx}] 缺少 source_id/target_id")

    def filter_invalid_nodes(self) -> "GraphBatch":
        """
        软约束：对关键 label 做最小字段校验，缺则 warning 并丢弃该节点。
        同时丢弃引用已丢弃节点的关系，避免 MERGE 时 MATCH 不到端点。
        返回过滤后的新 GraphBatch，不修改原对象。
        """
        required = _KEY_LABEL_REQUIRED
        new_nodes: Dict[str, List[dict]] = {}
        kept_ids: Set[str] = set()
        for label, items in (self.nodes or {}).items():
            if not items:
                continue
            req_fields = required.get(label)
            if not req_fields:
                for n in items:
                    if isinstance(n, dict) and n.get("symbol_id"):
                        kept_ids.add(str(n["symbol_id"]))
                new_nodes[label] = list(items)
                continue
            kept: List[dict] = []
            for idx, node in enumerate(items):
                if not isinstance(node, dict):
                    continue
                missing = [f for f in req_fields if not node.get(f)]
                if missing:
                    logger.warning(
                        f"GraphBatch.nodes[{label}][{idx}] 缺少关键字段 {missing}，已丢弃 (symbol_id={node.get('symbol_id', '?')})"
                    )
                    continue
                kept.append(node)
                sid = node.get("symbol_id")
                if sid:
                    kept_ids.add(str(sid))
            if kept:
                new_nodes[label] = kept

        # 丢弃引用已丢弃节点的关系
        new_rels: Dict[str, List[dict]] = {}
        for rel_type, items in (self.relationships or {}).items():
            if not items:
                continue
            filtered = [
                r for r in items
                if isinstance(r, dict)
                and r.get("source_id") in kept_ids
                and r.get("target_id") in kept_ids
            ]
            if filtered:
                new_rels[rel_type] = filtered
        return GraphBatch(nodes=new_nodes, relationships=new_rels)

    def dedupe_relationships(self) -> "GraphBatch":
        """
        关系去重：减少 MERGE 压力（保持语义不变）。
        """
        new_rels: Dict[str, List[dict]] = {}
        for rel_type, items in (self.relationships or {}).items():
            seen: set[Tuple[str, str]] = set()
            out: list[dict] = []
            for rel in items or []:
                if not isinstance(rel, dict):
                    continue
                s = rel.get("source_id")
                t = rel.get("target_id")
                if not s or not t:
                    continue
                key = (s, t)
                if key in seen:
                    continue
                seen.add(key)
                out.append({"source_id": s, "target_id": t})
            if out:
                new_rels[rel_type] = out
        return GraphBatch(nodes=self.nodes or {}, relationships=new_rels)

    def dedupe_nodes(self) -> "GraphBatch":
        """
        节点去重与合并：同一 label 下按 symbol_id 合并属性（后写覆盖前写）。

        说明：
        - 这能显著减少同一批次中重复 upsert 的压力（性能更稳）
        - 合并策略选择“last-write-wins”，对当前导出逻辑最安全（不依赖字段可交换性）
        """
        new_nodes: Dict[str, List[dict]] = {}
        for label, items in (self.nodes or {}).items():
            merged: Dict[str, dict] = {}
            for node in items or []:
                if not isinstance(node, dict):
                    continue
                sid = node.get("symbol_id")
                if not sid:
                    continue
                base = merged.get(sid)
                if base is None:
                    merged[sid] = dict(node)
                else:
                    base.update(node)
            if merged:
                new_nodes[label] = list(merged.values())
        return GraphBatch(nodes=new_nodes, relationships=self.relationships or {})


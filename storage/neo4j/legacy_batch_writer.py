from __future__ import annotations

from typing import Any

from parser.utils.logger import get_logger
from storage.neo4j.merge_builder import MergeQueryBuilder, get_unique_key_for_node_type
from storage.neo4j.queries import QueryBuilder

logger = get_logger("neo4j_legacy_batch_writer")


class Neo4jLegacyBatchWriterMixin:
    """
    兼容：旧 exporter 直写 Neo4j 的批量落库实现（UNWIND + MERGE + 回退逐条）。

    约束：
    - 宿主类需提供：
      - self.connector
      - self.project_id
      - self.nodes_to_create / self.relationships_to_create
    """

    @staticmethod
    def _build_unwind_merge_nodes_query(node_type: str, unique_key_names: list[str]) -> str:
        return QueryBuilder.build_batch_create_nodes_query(node_type, unique_key_names)

    @staticmethod
    def _build_unwind_merge_relationships_query(rel_type: str) -> str:
        return QueryBuilder.build_batch_create_relationships_query(rel_type)

    @staticmethod
    def _get_unique_keys_for_node_type(node_type: str) -> list[str]:
        return get_unique_key_for_node_type(node_type)

    def _create_nodes_batch(self, batch_size: int = 5000) -> int:
        from dataclasses import asdict, is_dataclass

        if not getattr(self, "connector", None):
            raise ValueError("connector 为空，无法执行 legacy batch writer")

        total_created = 0

        for node_type, nodes in (self.nodes_to_create or {}).items():
            if not nodes:
                continue

            try:
                unique_key_names = self._get_unique_keys_for_node_type(node_type.value)

                nodes_dicts: list[dict] = []
                for node in nodes:
                    if isinstance(node, dict):
                        node_dict = node
                    elif is_dataclass(node):
                        node_dict = asdict(node)
                    elif hasattr(node, "__dict__"):
                        node_dict = node.__dict__
                    else:
                        logger.warning(f"Cannot serialize node of type {type(node)}, skipping")
                        continue

                    if isinstance(node_dict, dict) and "project_key" not in node_dict:
                        node_dict["project_key"] = self.project_id

                    nodes_dicts.append(node_dict)

                if not nodes_dicts:
                    continue

                total_nodes = len(nodes_dicts)
                created_count = 0

                for i in range(0, total_nodes, batch_size):
                    batch = nodes_dicts[i : i + batch_size]
                    total_batches = (total_nodes + batch_size - 1) // batch_size
                    batch_num = i // batch_size + 1

                    query = self._build_unwind_merge_nodes_query(node_type.value, unique_key_names)
                    self.connector.execute_write_query(query, {"nodes": batch})
                    created_count += len(batch)

                    if total_batches > 1:
                        logger.info(
                            f"Created {len(batch)} {node_type} nodes (batch {batch_num}/{total_batches})"
                        )

                total_created += created_count
                logger.info(f"Created {created_count} {node_type} nodes in total")

            except Exception as e:
                logger.error(f"Failed to create {node_type} nodes with UNWIND: {e}")
                try:
                    for node in nodes:
                        if isinstance(node, dict):
                            node_dict = node
                        elif is_dataclass(node):
                            node_dict = asdict(node)
                        else:
                            node_dict = node.__dict__ if hasattr(node, "__dict__") else node

                        query = MergeQueryBuilder.build_merge_node_query(node_type.value, node_dict)
                        self.connector.execute_write_query(query, node_dict)
                        total_created += 1
                except Exception as e2:
                    logger.error(f"Fallback: Failed to create {node_type.value} nodes: {e2}")

        logger.info(f"Created {total_created} nodes in total")
        return total_created

    def _create_relationships_batch(self, batch_size: int = 5000) -> int:
        if not getattr(self, "connector", None):
            raise ValueError("connector 为空，无法执行 legacy batch writer")

        total_created = 0
        failed_relationships: list[dict[str, Any]] = []

        try:
            if not self.relationships_to_create:
                return 0

            relationships_by_type: dict[str, list[dict[str, str]]] = {}
            for source_id, target_id, rel_type in self.relationships_to_create:
                relationships_by_type.setdefault(rel_type, []).append(
                    {"source_id": source_id, "target_id": target_id}
                )

            for rel_type, relationships in relationships_by_type.items():
                try:
                    total_rels = len(relationships)
                    created_count = 0

                    for i in range(0, total_rels, batch_size):
                        batch = relationships[i : i + batch_size]
                        total_batches = (total_rels + batch_size - 1) // batch_size
                        batch_num = i // batch_size + 1

                        query = self._build_unwind_merge_relationships_query(rel_type)
                        self.connector.execute_write_query(query, {"relationships": batch})
                        created_count += len(batch)

                        if total_batches > 1:
                            logger.info(
                                f"Created {len(batch)} {rel_type} relationships (batch {batch_num}/{total_batches})"
                            )

                    total_created += created_count
                    logger.info(f"Created {created_count} {rel_type} relationships in total")

                except Exception as e:
                    logger.error(f"Failed to create {rel_type} relationships with UNWIND: {e}")
                    try:
                        for rel in relationships:
                            try:
                                query = MergeQueryBuilder.build_merge_relationship_query(
                                    rel["source_id"], rel["target_id"], rel_type
                                )
                                self.connector.execute_write_query(
                                    query,
                                    {"source_id": rel["source_id"], "target_id": rel["target_id"]},
                                )
                                total_created += 1
                            except Exception as rel_error:
                                failed_relationships.append(
                                    {
                                        "source_id": rel["source_id"],
                                        "target_id": rel["target_id"],
                                        "rel_type": rel_type,
                                        "error": str(rel_error),
                                    }
                                )
                    except Exception as e2:
                        logger.error(f"Fallback: Failed to create {rel_type} relationships: {e2}")

        except Exception as e:
            logger.error(f"Failed to create relationships: {e}")

        if failed_relationships:
            logger.warning(f"Total failed relationships: {len(failed_relationships)}")

        logger.info(f"Created {total_created} relationships in total")
        return total_created


class Neo4jLegacyBatchWriter(Neo4jLegacyBatchWriterMixin):
    """
    组合用：把 legacy mixin 封装成可实例化 writer。

    说明：
    - 旧 mixin 要求宿主类提供 connector/project_id/nodes_to_create/relationships_to_create
    - 这里把这些依赖变成构造参数，便于 exporter 组合调用而不是继承
    """

    def __init__(
        self,
        *,
        connector,
        project_id: str,
        nodes_to_create,
        relationships_to_create,
    ):
        self.connector = connector
        self.project_id = project_id
        self.nodes_to_create = nodes_to_create
        self.relationships_to_create = relationships_to_create


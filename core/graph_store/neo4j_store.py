from __future__ import annotations

import re
from typing import Any, Dict, List

from neomodel import db as neo4j_db

from core.import_context import ProjectImportContext
from parser.utils.logger import get_logger

logger = get_logger("neo4j_store")
from core.graph_ir import GraphBatch
from core.graph_store.protocols import GraphStore
from core.import_result import ImportResult
from storage.neo4j.merge_builder import get_unique_key_for_node_type
from storage.neo4j.queries import QueryBuilder
from storage.neo4j.external_linker import ExternalClassLinker
from parser.languages.java.java_graph_batch_builder import build_java_graph_batch


class Neo4jGraphStore(GraphStore):
    """
    Neo4j GraphStore 实现：把 Neo4jConnector 与“历史兼容导出器”的细节收口到这一层。
    """

    def __init__(self, connector):
        self.connector = connector

    def delete_project_subgraph(self, ctx: ProjectImportContext) -> int:
        return int(
            self.connector.delete_project_data(ctx.project_name, project_key=ctx.project_key)
        )

    def delete_file_subgraph(self, ctx: ProjectImportContext, file_path: str) -> int:
        return int(
            self.connector.delete_nodes_by_file(file_path, ctx.project_name, project_key=ctx.project_key)
        )

    def delete_file_subgraph_with_details(self, ctx: ProjectImportContext, file_path: str) -> Dict[str, Any]:
        fn = getattr(self.connector, "delete_nodes_by_file_with_details", None)
        if callable(fn):
            try:
                return fn(file_path, ctx.project_name, project_key=ctx.project_key, sample_limit=60)
            except Exception:
                pass
        return {
            "deleted_nodes": self.delete_file_subgraph(ctx, file_path),
            "deleted_relationships": 0,
            "sample_nodes": [],
        }

    def export_ast(
        self,
        ctx: ProjectImportContext,
        ast_data_list: list,
        symbol_table: Any,
        clear_database: bool,
    ) -> Dict:
        # 兼容：旧流程仍可通过 store.export_ast 走 Java AST -> GraphBatch -> Neo4j
        if clear_database:
            self.delete_project_subgraph(ctx)

        batch = build_java_graph_batch(
            project_name=ctx.project_name,
            project_key=ctx.project_key,
            project_path=ctx.repo_cache_dir,
            ast_data_list=ast_data_list,
            include_comment_nodes=ctx.include_comment_nodes,
            include_lib_nodes=getattr(ctx, 'auto_link_external', True),
        )
        result = self.write_batch(ctx, batch)

        # 外部类链接统一在落库后执行；用 link_all 全局连一次，避免依赖导入顺序（先 A 后 B 或先 B 后 A 都能连上）
        if getattr(ctx, 'auto_link_external', True):
            try:
                linker = ExternalClassLinker(self.connector)
                _ = linker.link_all(dry_run=False)
                _ = linker.link_lib_to_application()
            except Exception:
                # 链接失败不影响主导入流程
                pass

        # 若本次导入的是 Application 项目，将同名 Lib 节点合并进来（去重）
        try:
            linker = ExternalClassLinker(self.connector)
            linker.absorb_lib_node(ctx.project_name)
        except Exception:
            pass

        return result

    def write_batch(self, ctx: ProjectImportContext, batch: GraphBatch) -> Dict:
        """
        将 GraphBatch 落库到 Neo4j（批量 UNWIND + MERGE），并使用写事务执行。
        """
        batch.validate()
        batch = batch.filter_invalid_nodes()
        batch = batch.dedupe_nodes().dedupe_relationships()
        # 单次 UNWIND 条数：过大会导致单次事务慢/超时，过小则网络往返多；建议 500～2000，远程 Neo4j 可适当减小
        batch_size = 500
        label_re = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
        rel_re = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")

        total_nodes = sum(len(n) for n in (batch.nodes or {}).values())
        total_rels = sum(len(r) for r in (batch.relationships or {}).values())
        logger.info(f"[写入] 开始落库，共 {total_nodes} 个节点、{total_rels} 条关系（batch_size={batch_size}）")

        attempted_nodes = 0
        created_nodes = 0
        attempted_nodes_by_label: Dict[str, int] = {}
        created_nodes_by_label: Dict[str, int] = {}
        node_samples: List[Dict[str, Any]] = []
        node_labels = list((batch.nodes or {}).keys())
        for label_idx, (label, nodes) in enumerate((batch.nodes or {}).items(), 1):
            if not nodes:
                continue

            if not isinstance(label, str) or not label_re.fullmatch(label):
                raise ValueError(f"非法节点 label: {label}")

            unique_keys = get_unique_key_for_node_type(label)
            query = QueryBuilder.build_batch_create_nodes_query(label, unique_keys)
            num_batches = (len(nodes) + batch_size - 1) // batch_size
            logger.info(f"[写入] 节点 {label}: {len(nodes)} 个（{label_idx}/{len(node_labels)}，{num_batches} 批）")

            attempted_nodes += len(nodes)
            attempted_nodes_by_label[label] = attempted_nodes_by_label.get(label, 0) + len(nodes)
            if len(node_samples) < 120:
                for n in nodes:
                    if len(node_samples) >= 120:
                        break
                    if isinstance(n, dict):
                        node_samples.append(
                            {
                                "label": label,
                                "symbol_id": n.get("symbol_id"),
                                "display": n.get("qualified_name") or n.get("name") or n.get("file_path") or n.get("symbol_id") or "",
                                "file_path": n.get("file_path") or n.get("belong_file") or "",
                            }
                        )
            for i in range(0, len(nodes), batch_size):
                chunk = nodes[i:i + batch_size]
                # 统一注入 project_key（写入层兜底，避免上游漏传）
                for n in chunk:
                    if isinstance(n, dict) and "project_key" not in n:
                        n["project_key"] = ctx.project_key
                result_raw, _ = neo4j_db.cypher_query(query, {"nodes": chunk})
                created_in_batch = int(result_raw[0][0]) if result_raw else 0
                created_nodes += created_in_batch
                created_nodes_by_label[label] = created_nodes_by_label.get(label, 0) + created_in_batch
                # 每 3 批或最后一批输出进度
                batch_num = i // batch_size + 1
                if batch_num % 3 == 0 or batch_num == num_batches:
                    logger.info(f"  - {label}: 已写入 {min(i + batch_size, len(nodes))}/{len(nodes)}")

        attempted_relationships = 0
        created_relationships = 0
        attempted_relationships_by_type: Dict[str, int] = {}
        created_relationships_by_type: Dict[str, int] = {}
        relationship_samples: List[Dict[str, Any]] = []
        rel_types = list((batch.relationships or {}).keys())
        for rel_idx, (rel_type, relationships) in enumerate((batch.relationships or {}).items(), 1):
            if not relationships:
                continue

            if not isinstance(rel_type, str) or not rel_re.fullmatch(rel_type):
                raise ValueError(f"非法关系类型 rel_type: {rel_type}")

            query = QueryBuilder.build_batch_create_relationships_query(rel_type)
            num_batches = (len(relationships) + batch_size - 1) // batch_size
            logger.info(f"[写入] 关系 {rel_type}: {len(relationships)} 条（{rel_idx}/{len(rel_types)}，{num_batches} 批）")

            attempted_relationships += len(relationships)
            attempted_relationships_by_type[rel_type] = attempted_relationships_by_type.get(rel_type, 0) + len(relationships)
            if len(relationship_samples) < 120:
                for r in relationships:
                    if len(relationship_samples) >= 120:
                        break
                    if isinstance(r, dict):
                        relationship_samples.append(
                            {
                                "type": rel_type,
                                "source_id": r.get("source_id"),
                                "target_id": r.get("target_id"),
                            }
                        )
            for i in range(0, len(relationships), batch_size):
                chunk = [
                    {"source_id": r.get("source_id"), "target_id": r.get("target_id")}
                    for r in relationships[i:i + batch_size]
                    if isinstance(r, dict)
                ]
                batch_num = i // batch_size + 1
                # 每批执行前打一条，便于确认确实按批执行（避免误以为“一整块一次执行”）
                if num_batches > 1:
                    logger.info(f"  - {rel_type}: 执行第 {batch_num}/{num_batches} 批（本批 {len(chunk)} 条）")
                result_raw, _ = neo4j_db.cypher_query(query, {"relationships": chunk})
                created_in_batch = int(result_raw[0][0]) if result_raw else 0
                created_relationships += created_in_batch
                created_relationships_by_type[rel_type] = created_relationships_by_type.get(rel_type, 0) + created_in_batch
                if num_batches > 1 and (batch_num % 3 == 0 or batch_num == num_batches):
                    logger.info(f"  - {rel_type}: 已写入 {min(i + batch_size, len(relationships))}/{len(relationships)}")

        logger.info(f"[写入] 完成，节点 attempted={attempted_nodes} created={created_nodes}，关系 attempted={attempted_relationships} created={created_relationships}")
        result = ImportResult(
            success=True,
            status="imported",
            message=f"Successfully wrote {attempted_nodes} nodes (created={created_nodes})",
            project_name=ctx.project_name,
            project_key=ctx.project_key,
            attempted_nodes=attempted_nodes,
            attempted_relationships=attempted_relationships,
            created_nodes=created_nodes,
            created_relationships=created_relationships,
        )
        # 对外接口仍按 dict 返回（兼容 GraphStore 协议签名）
        out = result.to_dict()
        out["detail"] = {
            "attempted_nodes_by_label": attempted_nodes_by_label,
            "created_nodes_by_label": created_nodes_by_label,
            "attempted_relationships_by_type": attempted_relationships_by_type,
            "created_relationships_by_type": created_relationships_by_type,
            "node_samples": node_samples,
            "relationship_samples": relationship_samples,
        }
        return out


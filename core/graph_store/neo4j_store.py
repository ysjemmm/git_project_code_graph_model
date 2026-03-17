from __future__ import annotations

import re
from typing import Any, Dict

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
        )
        result = self.write_batch(ctx, batch)

        # 外部类链接统一在落库后执行，避免构建阶段耦合存储层读查询
        try:
            linker = ExternalClassLinker(self.connector)
            _ = linker.link_by_project(ctx.project_name, project_key=ctx.project_key, dry_run=False)
        except Exception:
            # 链接失败不影响主导入流程
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
            for i in range(0, len(nodes), batch_size):
                chunk = nodes[i:i + batch_size]
                # 统一注入 project_key（写入层兜底，避免上游漏传）
                for n in chunk:
                    if isinstance(n, dict) and "project_key" not in n:
                        n["project_key"] = ctx.project_key
                result = self.connector.execute_write_query(query, {"nodes": chunk})
                created_nodes += int(result[0].get("created", 0)) if result else 0
                # 每 3 批或最后一批输出进度
                batch_num = i // batch_size + 1
                if batch_num % 3 == 0 or batch_num == num_batches:
                    logger.info(f"  - {label}: 已写入 {min(i + batch_size, len(nodes))}/{len(nodes)}")

        attempted_relationships = 0
        created_relationships = 0
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
                result = self.connector.execute_write_query(query, {"relationships": chunk})
                created_relationships += int(result[0].get("created", 0)) if result else 0
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
        return result.to_dict()


"""
Java 专用：Neo4j AST 导出兼容实现。

说明：
- 该类本质上是 Java 导出逻辑的兼容外观（facade）
- 构建阶段完全委托给 `JavaAstGraphBuilderEngine`（不再通过继承 mixin）
- 写入阶段优先走通用 `Neo4jGraphStore.write_batch()`，失败时再回退到 legacy writer
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional

from parser.languages.java.core.ast_node_types import JavaFileStructure
from parser.languages.java.java_ast_graph_builder import JavaAstGraphBuilderEngine
from parser.utils.logger import get_logger
from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.legacy_batch_writer import Neo4jLegacyBatchWriter

logger = get_logger("neo4j_exporter")


class JavaNeo4jAstExporter:
    """Java AST -> Neo4j 导出器（Java 兼容外观类）。"""

    def __init__(
        self,
        connector: Optional[Neo4jConnector],
        project_name: str = "",
        project_id: str = "",
        project_path: str = "",
    ):
        self.connector = connector
        self.project_id = project_id
        self.project_name = project_name
        self.project_path = project_path

        self._builder = JavaAstGraphBuilderEngine(
            connector=connector,
            project_name=project_name,
            project_id=project_id,
            project_path=project_path,
        )

    # ==================== 兼容：把原来 mixin 方法转发到 builder ====================
    def prepare_from_ast_data(self, *args, **kwargs):
        return self._builder.prepare_from_ast_data(*args, **kwargs)

    def build_graph_batch(self, *args, **kwargs):
        return self._builder.build_graph_batch(*args, **kwargs)

    def _prepare_external_links(self, *args, **kwargs):
        return self._builder._prepare_external_links(*args, **kwargs)

    def _count_external_links(self, *args, **kwargs):
        return self._builder._count_external_links(*args, **kwargs)

    def export_from_ast_data(
        self,
        ast_data_list: List[JavaFileStructure],
        clear_database: bool = True,
        symbol_tables: List[Any] = None,
        auto_link_external: bool = True,
    ) -> Dict:
        """导出 AST 数据到 Neo4j（兼容旧调用方）。"""
        try:
            if not self.connector:
                raise ValueError(
                    "connector 为空：请使用 GraphBatch + GraphStore.write_batch() 的通用写入路径，或传入有效 Neo4jConnector"
                )

            if clear_database:
                # 仅清理当前项目的子图，避免误删其他项目或共享库数据
                if self.project_name:
                    deleted_count = self.connector.delete_project_data(
                        self.project_name,
                        project_key=self.project_id,
                    )
                    logger.info(
                        f"已清理项目 {self.project_name} 旧数据，共删除 {deleted_count} 个节点"
                    )
                else:
                    logger.warning(
                        "未提供 project_name，跳过项目子图清理（不再执行全库清空）"
                    )

            batch = self._builder.prepare_from_ast_data(
                ast_data_list, auto_link_external=False
            )

            linked_count = 0
            try:
                from core.import_context import ProjectImportContext
                from core.graph_store.neo4j_store import Neo4jGraphStore
                from storage.neo4j.external_linker import ExternalClassLinker

                ctx = ProjectImportContext(
                    project_name=self.project_name,
                    project_key=self.project_id,
                    repo_cache_dir=self.project_path,
                    language="java",
                    languages=["java"],
                    clear_database=False,
                )
                store = Neo4jGraphStore(self.connector)
                write_result = store.write_batch(ctx, batch)

                if auto_link_external:
                    try:
                        linker = ExternalClassLinker(self.connector)
                        link_result = linker.link_by_project(
                            self.project_name,
                            project_key=self.project_id,
                            dry_run=False,
                        )
                        linked_count = int(
                            link_result.get("relationships_created", 0) or 0
                        )
                    except Exception:
                        linked_count = 0

                return {
                    "success": True,
                    "message": write_result.get(
                        "message",
                        f"Successfully wrote {write_result.get('attempted_nodes', 0)} nodes",
                    ),
                    # 兼容：exporter 对外仍返回 created_*（本次新建数量）
                    "created_nodes": int(write_result.get("created_nodes", 0) or 0),
                    "created_relationships": int(
                        write_result.get("created_relationships", 0) or 0
                    ),
                    # 新增：attempted_*（本次提交写入条目数）
                    "attempted_nodes": int(write_result.get("attempted_nodes", 0) or 0),
                    "attempted_relationships": int(
                        write_result.get("attempted_relationships", 0) or 0
                    ),
                    "linked_external_classes": linked_count,
                }
            except Exception:
                if auto_link_external:
                    self._builder._prepare_external_links()

                writer = Neo4jLegacyBatchWriter(
                    connector=self.connector,
                    project_id=self.project_id,
                    nodes_to_create=self._builder.nodes_to_create,
                    relationships_to_create=self._builder.relationships_to_create,
                )
                writer._create_nodes_batch()
                writer._create_relationships_batch()
                linked_count = (
                    self._builder._count_external_links() if auto_link_external else 0
                )

            return {
                "success": True,
                "message": f"Successfully exported {len(self._builder.created_nodes)} nodes",
                "created_nodes": len(self._builder.created_nodes),
                "created_relationships": len(self._builder.relationships_to_create),
                "linked_external_classes": linked_count,
            }
        except Exception as e:
            logger.error(f"Export failed: {e}")
            return {
                "success": False,
                "message": str(e),
                "created_nodes": 0,
                "created_relationships": 0,
                "linked_external_classes": 0,
            }


# 兼容：历史类名（大量旧代码/脚本依赖）
Neo4jExporterAST = JavaNeo4jAstExporter

__all__ = ["JavaNeo4jAstExporter", "Neo4jExporterAST"]

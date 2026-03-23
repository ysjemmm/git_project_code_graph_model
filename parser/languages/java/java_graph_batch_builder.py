from __future__ import annotations

from core.graph_ir import GraphBatch
from parser.languages.java.java_ast_graph_builder import JavaAstGraphBuilderEngine
from parser.utils.logger import get_logger

logger = get_logger("graph_batch")


def build_java_graph_batch(
    *,
    project_name: str,
    project_key: str,
    project_path: str,
    ast_data_list: list,
    include_comment_nodes: bool = False,
    include_lib_nodes: bool = True,
) -> GraphBatch:
    """
    Java 专用：将 Java AST 列表构建为语言无关 GraphBatch。

    说明：
    - 该构建过程不进行任何数据库读写（不依赖 Neo4jConnector）
    - 外部类链接（LIB_LINK）建议由落库后统一执行（ExternalClassLinker），避免构建阶段耦合存储层
    - include_comment_nodes=False 时仅将注释存于父节点 simple_comment，不创建 Comment 节点
    - include_lib_nodes=False 时跳过创建 Project(Lib) 节点和 CONTAINS_LIB 关系
    """
    logger.info(f"[构建] 开始构建 GraphBatch，共 {len(ast_data_list)} 个文件")
    builder = JavaAstGraphBuilderEngine(
        connector=None,
        project_name=project_name,
        project_id=project_key,
        project_path=project_path,
        include_comment_nodes=include_comment_nodes,
        include_lib_nodes=include_lib_nodes,
    )
    batch = builder.prepare_from_ast_data(ast_data_list, auto_link_external=False)
    total_nodes = sum(len(n) for n in (batch.nodes or {}).values())
    total_rels = sum(len(r) for r in (batch.relationships or {}).values())
    logger.info(f"[构建] 完成，{total_nodes} 个节点、{total_rels} 条关系")
    return batch


from __future__ import annotations

from typing import Dict, List, Optional, Set, Tuple

from parser.languages.java.comment_processing import JavaCommentProcessingMixin
from parser.languages.java.external_link_preparation import JavaExternalLinkPreparationMixin
from parser.languages.java.external_type_resolution import JavaExternalTypeResolutionMixin
from parser.languages.java.graph_batch_export import JavaGraphBatchExportMixin
from parser.languages.java.member_collection import JavaMemberCollectionMixin
from parser.languages.java.nested_type_collection import JavaNestedTypeCollectionMixin
from parser.languages.java.node_dedupe_utils import JavaNodeDedupeMixin
from parser.languages.java.project_file_collection import JavaProjectAndFileCollectionMixin
from parser.languages.java.type_collection import JavaTopLevelTypeCollectionMixin
from parser.languages.java.type_relationships import JavaTypeRelationshipCollectionMixin
from parser.languages.java.symbol.symbol_manager import SymbolManager
from storage.neo4j.java_modules import JavaNeo4jNodeType


class JavaAstGraphBuilder(
    JavaCommentProcessingMixin,
    JavaExternalTypeResolutionMixin,
    JavaExternalLinkPreparationMixin,
    JavaProjectAndFileCollectionMixin,
    JavaTopLevelTypeCollectionMixin,
    JavaMemberCollectionMixin,
    JavaNestedTypeCollectionMixin,
    JavaTypeRelationshipCollectionMixin,
    JavaNodeDedupeMixin,
    JavaGraphBatchExportMixin,
):
    """
    Java AST -> GraphBatch 构建器（把一堆 mixin 聚合到一个基类里）。

    说明：
    - 该类不负责写入 Neo4j，只负责在内存中构建 nodes_to_create / relationships_to_create 并导出 GraphBatch
    - exporter 只需要继承它，就不必显式写一长串 mixin
    """

    pass


class JavaAstGraphBuilderEngine(JavaAstGraphBuilder):
    """
    可实例化的 Java AST -> GraphBatch 构建引擎（组合用）。

    说明：
    - exporter/adapter 可通过持有该实例完成构建，而无需继承一堆 mixin
    """

    def __init__(
        self,
        *,
        connector=None,
        project_name: str = "",
        project_id: str = "",
        project_path: str = "",
        include_comment_nodes: bool = False,
    ):
        self.connector = connector
        self.project_id = project_id
        self.project_name = project_name
        self.project_path = project_path
        self.include_comment_nodes = include_comment_nodes

        self.created_nodes: Set[str] = set()
        self.relationships_to_create: List[Tuple[str, str, str]] = []
        self.nodes_to_create: Dict[JavaNeo4jNodeType, List] = {
            node_type: [] for node_type in JavaNeo4jNodeType
        }

        self.symbol_manager = SymbolManager(project_name=project_name)


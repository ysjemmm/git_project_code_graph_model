"""Annotation analyzer for Java AST"""

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import AnnotationTypeInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


class AnnotationAnalyzer(BaseAnalyzer):

    def __init__(self, is_nested: bool = False):
        super().__init__()
        self._init()
        self._is_nested = is_nested

    def _init(self):
        self.anno_info = AnnotationTypeInfo()
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_annotation_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> AnnotationTypeInfo | None:
        """Handle annotation type declaration node"""
        self._init()
        self._ast_must_nodes(node)

        self.anno_info.set_pos_from_node(node)
        self.anno_info.annotations = AnalyzerHelper.extract_java_marked_annotation(node)

        # Extract annotation base
        self._extract_annotation_base(node)

        AnalyzerCache.get_annotation_body_analyzer(context.project_name).handle_annotation_body(
            self.type2node.get(JavaAstNodeType.ANNOTATION_TYPE_BODY, [None])[0],
            self.anno_info,
            context
        )

        return self.anno_info

    def _extract_annotation_base(self, node: ExtractedNode) -> str:
        """Extract annotation base"""
        self.anno_info.annotation_name = node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
        self.anno_info.raw_metadata = node.extractions.get(JavaAstNodeType.EX_ANNOTATION_BODY.value, "")


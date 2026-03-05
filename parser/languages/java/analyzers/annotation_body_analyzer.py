"""Annotation Body analyzer for Java AST"""
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import FieldInfo, MethodInfo, AnnotationTypeInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


class AnnotationBodyAnalyzer(BaseAnalyzer):
    """Analyzes Java annotations body"""

    def __init__(self):
        super().__init__()
        self._anno_info = None

    def _init(self):
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_annotation_body(self, node: ExtractedNode, anno_info: AnnotationTypeInfo, context: AnalyzerContext):
        """Handle annotation body"""
        if not node or self.get_node_type(node) != JavaAstNodeType.ANNOTATION_TYPE_BODY.value:
            return
        self._anno_info = anno_info
        self._init()
        self._ast_must_nodes(node)

        AnalyzerHelper.extract_java_nested_object(self._anno_info, self.type2node, context, anno_info.symbol_id)

        self._anno_info.comments = self._extract_comments(context)
        self._anno_info.elements = self._extract_fields(context)

    def _extract_comments(self, context) -> list:
        """Extract comments"""
        analyzer = AnalyzerCache.get_comment_analyzer(context.project_name)

        # 获取注释节点
        line_comments = self.type2node.get(JavaAstNodeType.LINE_COMMENT, [])
        block_comments = self.type2node.get(JavaAstNodeType.BLOCK_COMMENT, [])
        nodes = line_comments + block_comments

        comments = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_comment(n)
                if result is not None:
                    comments.append(result)
        return comments

    def _extract_fields(self, context) -> list[FieldInfo]:
        """Extract fields"""
        analyzer = AnalyzerCache.get_field_analyzer(context.project_name)

        fields = self.type2node.get(JavaAstNodeType.ANNOTATION_TYPE_ELEMENT_DECLARATION, [])

        fs = []
        for n in fields:
            if n is not None:
                result = analyzer.handle_field_declaration(n, self._anno_info.symbol_id)
                if result is not None:
                    fs.append(result)
        return fs

    def _extract_methods(self, context) -> List[MethodInfo]:
        analyzer = AnalyzerCache.get_method_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.METHOD_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_method_declaration(n, context)
                if result is not None:
                    mtds.append(result)
        return mtds





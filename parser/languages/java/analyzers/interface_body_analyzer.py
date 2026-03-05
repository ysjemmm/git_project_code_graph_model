"""Interface Body analyzer for Java AST"""
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import FieldInfo, MethodInfo, InterfaceInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


class InterfaceBodyAnalyzer(BaseAnalyzer):
    """Analyzes Java annotations"""

    def __init__(self):
        super().__init__()
        self._interface_info = None

    def _init(self):
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_interface_body(self, node: ExtractedNode, interface_info: InterfaceInfo, context: AnalyzerContext):
        """Handle interface body"""
        if not node or self.get_node_type(node) != JavaAstNodeType.INTERFACE_BODY.value:
            return
        self._interface_info = interface_info
        self._init()
        self._ast_must_nodes(node)

        AnalyzerHelper.extract_java_nested_object(self._interface_info, self.type2node, context, interface_info.symbol_id)

        self._interface_info.comments = self._extract_comments(context)
        self._interface_info.fields = self._extract_fields(context)
        self._interface_info.methods = self._extract_methods(context)

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

        fields = self.type2node.get(JavaAstNodeType.CONSTANT_DECLARATION, [])

        fs = []
        for n in fields:
            if n is not None:
                result = analyzer.handle_field_declaration(n, self._interface_info.symbol_id)
                if result is not None:
                    fs.append(result)
        return fs

    def _extract_methods(self, context) -> List[MethodInfo]:
        analyzer = AnalyzerCache.get_method_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.METHOD_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_method_declaration(n, context, self._interface_info.symbol_id)
                if result is not None:
                    mtds.append(result)
        return mtds





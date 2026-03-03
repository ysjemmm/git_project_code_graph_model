"""Class Body analyzer for Java AST"""
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import ClassInfo, FieldInfo, MethodInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


class ClassBodyAnalyzer(BaseAnalyzer):
    """Analyzes Java annotations"""

    def __init__(self):
        super().__init__()
        self._class_info = None

    def _init(self):
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_class_body(self, node: ExtractedNode, class_info: ClassInfo, context: AnalyzerContext):
        """Handle annotation node"""
        if not node or self.get_node_type(node) != JavaAstNodeType.CLASS_BODY.value:
            return
        self._class_info = class_info
        self._init()
        self._ast_must_nodes(node)

        AnalyzerHelper.extract_java_nested_object(self._class_info, self.type2node, context)

        self._class_info.comments = self._extract_comments(context)
        self._class_info.fields = self._extract_fields(context)
        self._class_info.methods = self._extract_methods(context)
        self._class_info.constructors = self._extract_constructors(context)
        self._class_info.code_blocks = self._extract_code_blocks(context)

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

        fields = self.type2node.get(JavaAstNodeType.FIELD_DECLARATION, [])

        fs = []
        for n in fields:
            if n is not None:
                result = analyzer.handle_field_declaration(n, self._class_info.symbol_id)
                if result is not None:
                    fs.append(result)
        return fs

    def _extract_methods(self, context) -> List[MethodInfo]:
        analyzer = AnalyzerCache.get_method_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.METHOD_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_method_declaration(n, context, self._class_info.symbol_id)
                if result is not None:
                    mtds.append(result)
        return mtds

    def _extract_constructors(self, context):
        analyzer = AnalyzerCache.get_constructor_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.CONSTRUCTOR_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_constructor_declaration(n, context, self._class_info.symbol_id)
                if result is not None:
                    mtds.append(result)
        return mtds

    def _extract_code_blocks(self, context):
        analyzer = AnalyzerCache.get_code_block_analyzer(context.project_name)

        code_blocks = (self.type2node.get(JavaAstNodeType.BLOCK, []) +
                       self.type2node.get(JavaAstNodeType.STATIC_INITIALIZER, []))
        cbs = []
        for n in code_blocks:
            if n is not None:
                result = analyzer.handle_code_block_declaration(n, context)
                if result is not None:
                    result.is_static = n.node_type == JavaAstNodeType.STATIC_INITIALIZER.value
                    cbs.append(result)
        return cbs





"""Enum Body analyzer for Java AST"""
from collections import deque
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import FieldInfo, MethodInfo, EnumInfo, EnumConstantInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


class EnumBodyAnalyzer(BaseAnalyzer):
    """Analyzes Java annotations"""

    def __init__(self):
        super().__init__()
        self._enum_info = None

    def _init(self):
        self.type2node = {}

    def _ast_must_nodes(self, node):
        node_queue = deque()
        if node is not None:
            node_queue.append(node)
        while len(node_queue) > 0:
            cur_node = node_queue.popleft()
            if cur_node.node_type in [JavaAstNodeType.ENUM_BODY.value, JavaAstNodeType.ENUM_BODY_DECLARATIONS.value]:
                for n in cur_node.children:
                    node_queue.append(n)
            else:
                self.type2node.setdefault(JavaAstNodeType.from_value(cur_node.node_type), []).append(cur_node)

    def handle_enum_body(self, node: ExtractedNode, enum_info: EnumInfo, context: AnalyzerContext):
        """Handle interface body"""
        if not node or self.get_node_type(node) != JavaAstNodeType.ENUM_BODY.value:
            return
        self._enum_info = enum_info
        self._init()
        self._ast_must_nodes(node)

        AnalyzerHelper.extract_java_nested_object(self._enum_info, self.type2node, context, enum_info.symbol_id)

        self._enum_info.comments = self._extract_comments(context)
        self._enum_info.enum_constants = self._extract_constants(context)
        self._enum_info.fields = self._extract_fields(context)
        self._enum_info.methods = self._extract_methods(context)
        self._enum_info.constructors = self._extract_constructors(context)
        self._enum_info.code_blocks = self._extract_code_blocks(context)

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
                result = analyzer.handle_field_declaration(n, self._enum_info.symbol_id)
                if result is not None:
                    fs.append(result)
        return fs

    def _extract_methods(self, context) -> List[MethodInfo]:
        analyzer = AnalyzerCache.get_method_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.METHOD_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_method_declaration(n, context, self._enum_info.symbol_id)
                if result is not None:
                    mtds.append(result)
        return mtds

    def _extract_constructors(self, context):
        analyzer = AnalyzerCache.get_constructor_analyzer(context.project_name)

        methods = self.type2node.get(JavaAstNodeType.CONSTRUCTOR_DECLARATION, [])
        mtds = []
        for n in methods:
            if n is not None:
                result = analyzer.handle_constructor_declaration(n, context, self._enum_info.symbol_id)
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

    def _extract_constants(self, context) -> List[EnumConstantInfo]:
        cns_nodes = self.type2node.get(JavaAstNodeType.ENUM_CONSTANT, [])
        constants = []
        for n in cns_nodes:
            cns = EnumConstantInfo()
            cns.set_pos_from_node(n)
            cns.constant_name = n.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
            cns.raw_constant = AstTool.node_text(n)
            cns.annotations = AnalyzerHelper.extract_java_marked_annotation(
                AstTool.find_child_by_type(n, JavaAstNodeType.MODIFIERS.value, True)
            )

            cns.arguments = []

            param = AstTool.find_child_by_type(n, JavaAstNodeType.ARGUMENT_LIST.value, True)
            if isinstance(param, ExtractedNode):
                for p in param.children:
                    if p.node_type == JavaAstNodeType.STRING_LITERAL.value:
                        cns.arguments.append(AstTool.node_text(p).strip('"'))
                    elif p.node_type == JavaAstNodeType.CHARACTER_LITERAL.value:
                        cns.arguments.append(AstTool.node_text(p).strip("'"))
                    else:
                        cns.arguments.append(AstTool.node_text(p))
                pass
            
            # Generate symbol_id for enum constant
            if cns.constant_name:
                cns.symbol_id = AnalyzerHelper.generate_symbol_id_for_enum_constant(
                    self._enum_info.symbol_id, cns.constant_name
                )
                cns.parent_symbol_id = self._enum_info.symbol_id
            
            constants.append(cns)
        return constants





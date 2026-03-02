"""Enum analyzer for Java AST"""

from dataclasses import dataclass

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import EnumInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


@dataclass
class EnumAnalyzer(BaseAnalyzer):
    """Analyzes Java enum declarations"""

    def __init__(self, is_nested: bool = False):
        super().__init__()
        self._init()
        self._is_nested = is_nested

    def _init(self):
        self.enum_info = EnumInfo()
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_enum_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> EnumInfo | None:
        """Handle enum declaration node"""
        from parser.languages.java.utils.analyzer_cache import AnalyzerCache

        self._init()
        self._ast_must_nodes(node)

        self.enum_info.set_pos_from_node(node)
        self.enum_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
            AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True))
        
        # Extract enum base
        self._extract_enum_base(node)

        AnalyzerCache.get_enum_body_analyzer(context.project_name).handle_enum_body(
            self.type2node.get(JavaAstNodeType.ENUM_BODY, [None])[0],
            self.enum_info,
            context
        )
        
        return self.enum_info

    def _extract_enum_base(self, node: ExtractedNode):
        """Extract enum"""
        self.enum_info.enum_name = node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
        self.enum_info.raw_metadata = node.extractions.get(JavaAstNodeType.EX_ENUM_BODY.value, "")
        self.enum_info.super_interfaces = AnalyzerHelper.extract_java_super_class(node, JavaAstNodeType.SUPER_INTERFACES)

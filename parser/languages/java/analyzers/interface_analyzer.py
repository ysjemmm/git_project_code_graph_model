"""Interface analyzer for Java AST"""

from dataclasses import dataclass

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import InterfaceInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


@dataclass
class InterfaceAnalyzer(BaseAnalyzer):
    """Analyzes Java interface declarations"""

    def __init__(self, is_nested: bool = False):
        super().__init__()
        self._init()
        self._is_nested = is_nested

    def _init(self):
        self.interface_info = InterfaceInfo()
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_interface_declaration(self, node: ExtractedNode, context: AnalyzerContext, parent_symbol_id: str) -> InterfaceInfo | None:
        """Handle interface declaration node"""
        from parser.languages.java.utils.analyzer_cache import AnalyzerCache

        self._init()
        self._ast_must_nodes(node)

        self.interface_info.set_pos_from_node(node)
        self.interface_info.annotations = AnalyzerHelper.extract_java_marked_annotation(node)

        # Extract interface base
        self._extract_interface_base(node)

        # Generate symbol_id before processing body
        self.interface_info.symbol_id = AnalyzerHelper.generate_symbol_id_for_class(
            parent_symbol_id, self.interface_info.interface_name
        )
        self.interface_info.parent_symbol_id = parent_symbol_id

        AnalyzerCache.get_interface_body_analyzer(context.project_name).handle_interface_body(
            self.type2node.get(JavaAstNodeType.INTERFACE_BODY, [None])[0],
            self.interface_info,
            context
        )

        return self.interface_info

    def _extract_interface_base(self, node: ExtractedNode):
        """Extract interface"""
        self.interface_info.interface_name = node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
        self.interface_info.raw_metadata = node.extractions.get(JavaAstNodeType.EX_INTERFACE_BODY.value, "")
        self.interface_info.extends_interfaces = AnalyzerHelper.extract_java_super_class(node, JavaAstNodeType.EXTENDS_INTERFACES)
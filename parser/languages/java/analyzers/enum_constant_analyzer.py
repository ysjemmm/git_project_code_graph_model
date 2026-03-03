"""Enum constant analyzer for Java AST"""

from dataclasses import dataclass

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import EnumConstantInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


@dataclass
class EnumConstantAnalyzer(BaseAnalyzer):
    """Analyzes Java enum constants"""

    def __init__(self):
        super().__init__()

    def handle_enum_constant(self, node: ExtractedNode, parent_symbol_id: str = "") -> EnumConstantInfo:
        """Handle enum constant node"""
        constant_info = EnumConstantInfo()
        
        if not node:
            return constant_info
        
        # Set location from node
        constant_info.set_pos_from_node(node)
        
        # Extract constant name
        constant_info.constant_name = self._extract_constant_name(node)
        
        # Extract constant declaration
        constant_info.raw_constant = self._extract_constant_declaration(node)
        
        # Generate symbol_id
        if parent_symbol_id and constant_info.constant_name:
            constant_info.symbol_id = AnalyzerHelper.generate_symbol_id_for_enum_constant(
                parent_symbol_id, constant_info.constant_name
            )
            constant_info.parent_symbol_id = parent_symbol_id
        
        return constant_info

    def _extract_constant_declaration(self, node: ExtractedNode) -> str:
        """Extract constant declaration (entire enum constant)"""
        # Get the full text of the constant node
        return AstTool.node_text(node)

    def _extract_constant_name(self, node: ExtractedNode) -> str:
        """Extract constant name"""
        extractions = self.get_extractions(node)
        if JavaAstNodeType.IDENTIFIER.value in extractions:
            return extractions[JavaAstNodeType.IDENTIFIER.value]
        
        identifier = AstTool.find_child_by_type(node, JavaAstNodeType.IDENTIFIER.value)
        if identifier and not isinstance(identifier, list):
            return AstTool.node_text(identifier)
        
        return ""

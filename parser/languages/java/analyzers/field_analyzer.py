"""Field analyzer for Java AST"""

from dataclasses import dataclass
from typing import Tuple

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import FieldInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


@dataclass
class FieldAnalyzer(BaseAnalyzer):
    """Analyzes Java field declarations"""

    def __init__(self):
        super().__init__()

    def handle_field_declaration(self, node: ExtractedNode, parent_symbol_id: str = "") -> FieldInfo | None:
        """
            Handle field declaration node
            if node is interface, then node_type is CONSTANT_DECLARATION
            else if node is annotation, then node_type is ANNOTATION_TYPE_ELEMENT_DECLARATION
            else node_type is FIELD_DECLARATION
        """
        if not node or (self.get_node_type(node) != JavaAstNodeType.FIELD_DECLARATION.value and
                        self.get_node_type(node) != JavaAstNodeType.CONSTANT_DECLARATION.value and
                        self.get_node_type(node) != JavaAstNodeType.ANNOTATION_TYPE_ELEMENT_DECLARATION.value):
            return None

        field_info = FieldInfo()

        # Set location from node
        field_info.set_pos_from_node(node)

        field_info.raw_field = AstTool.node_text(node)
        
        # Extract field type
        field_info.field_type = self._extract_field_type(node)
        
        # Extract field name、value
        f_name, f_value = self._extract_annotation_element_name_value(node) if node.node_type == JavaAstNodeType.ANNOTATION_TYPE_ELEMENT_DECLARATION.value else\
                          self._extract_field_name_value(node)
        field_info.has_initial_value = f_value is not None
        field_info.field_name = f_name
        field_info.initial_value = "" if f_value is None else f_value

        # Extract marked annotation
        field_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
            AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True)
        )

        # Extract modifiers
        field_info.is_final, field_info.is_static = self.extract_modifiers(node)
        
        # Generate symbol_id
        if parent_symbol_id and field_info.field_name:
            field_info.symbol_id = AnalyzerHelper.generate_symbol_id_for_field(
                parent_symbol_id, field_info.field_name
            )
            field_info.parent_symbol_id = parent_symbol_id
        
        return field_info

    def _extract_field_type(self, node: ExtractedNode) -> str:
        """Extract field type"""
        return node.extractions.get(JavaAstNodeType.EX_FIELD_TYPE.value, "")

    def _extract_field_name_value(self, node: ExtractedNode) -> Tuple[str, str | None]:
        """Extract field name"""
        # Find variable declarator
        var_name = ""
        var_value = None
        if node.node_type != JavaAstNodeType.ANNOTATION_TYPE_ELEMENT_DECLARATION.value:
            var_declarator = AstTool.find_child_by_type(node, JavaAstNodeType.VARIABLE_DECLARATOR.value, True)
            if var_declarator:
                var_name = var_declarator.extractions.get(JavaAstNodeType.EX_FIELD_NAME.value, "")
                var_value = var_declarator.extractions.get(JavaAstNodeType.EX_FIELD_VALUE.value)
        return var_name, var_value

    def _extract_annotation_element_name_value(self, node: ExtractedNode) -> Tuple[str, str | None]:
        """Extract annotation element name"""
        var_name = ""
        var_value = None
        if node.node_type == JavaAstNodeType.ANNOTATION_TYPE_ELEMENT_DECLARATION.value:
            var_name  = node.extractions.get(JavaAstNodeType.EX_FIELD_NAME.value, "")
            var_value = node.extractions.get(JavaAstNodeType.EX_FIELD_VALUE.value)
        return var_name, var_value

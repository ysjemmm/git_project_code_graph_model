"""Common extractors for Java AST analysis"""

from loraxmod import ExtractedNode

from parser.languages.java.java_ast_enums import JavaAstNodeType
from tools.ast_tool import AstTool


class CommonExtractors:
    """Common extraction utilities for Java AST"""

    @staticmethod
    def unwrap_list_result(result):
        """Unwrap list result from AST tool"""
        if isinstance(result, list) and len(result) > 0:
            return result[0]
        return result

    @staticmethod
    def extract_type_name(type_node: ExtractedNode) -> str:
        """Extract type name from type node"""
        if not type_node:
            return ""
        
        return AstTool.node_text(type_node)

    @staticmethod
    def extract_identifier(node: ExtractedNode) -> str:
        """Extract identifier from node"""
        if not node:
            return ""
        
        # Try extractions first
        from parser.languages.java.core.base_analyzer import BaseAnalyzer
        extractions = BaseAnalyzer.get_extractions(node)
        if JavaAstNodeType.IDENTIFIER.value in extractions:
            return extractions[JavaAstNodeType.IDENTIFIER.value]
        
        # Find identifier child
        identifier = AstTool.find_child_by_type(node, JavaAstNodeType.IDENTIFIER.value)
        if identifier and not isinstance(identifier, list):
            return AstTool.node_text(identifier)
        
        return ""

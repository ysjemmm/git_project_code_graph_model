"""Package analyzer for Java AST"""

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import PackageInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from tools.ast_tool import AstTool


class PackageAnalyzer(BaseAnalyzer):
    """Analyzes Java package declarations"""

    def __init__(self):
        super().__init__()

    def handle_package_declaration(self, node: ExtractedNode) -> PackageInfo:
        """Handle package declaration node"""
        package_info = PackageInfo()
        
        if not node or self.get_node_type(node) != JavaAstNodeType.PACKAGE_DECLARATION.value:
            return package_info
        
        # Set location from node
        package_info.set_pos_from_node(node)
        
        # Extract package name
        package_info.name = self._extract_package_name(node)
        
        return package_info

    def _extract_package_name(self, node: ExtractedNode) -> str:
        """Extract package name"""
        # Get the full text and remove 'package' keyword
        text = AstTool.node_text(node)
        if text.startswith('package '):
            text = text[8:]  # Remove 'package '
        if text.endswith(';'):
            text = text[:-1]  # Remove ';'
        return text.strip()

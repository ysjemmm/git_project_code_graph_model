"""Import analyzer for Java AST"""

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import ImportInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from tools.ast_tool import AstTool


class ImportAnalyzer(BaseAnalyzer):
    """Analyzes Java import declarations"""

    def __init__(self):
        super().__init__()

    def handle_import_declaration(self, node: ExtractedNode) -> ImportInfo | None:
        """Handle import declaration node"""

        if not node or self.get_node_type(node) != JavaAstNodeType.IMPORT_DECLARATION.value:
            return None

        import_info = ImportInfo()
        # Set location from node
        import_info.set_pos_from_node(node)
        
        # Extract import path
        import_info.import_path = self._extract_import_path(node)
        
        # Check if static import
        import_info.is_static = self._is_static_import(node)
        
        # Check if wildcard import
        import_info.is_wildcard = import_info.import_path.rstrip().endswith('.*')

        return import_info

    def _extract_import_path(self, node: ExtractedNode) -> str:
        """Extract import path"""
        text = AstTool.node_text(node)
        # Remove 'import' keyword and ';'
        if text.startswith('import '):
            text = text[7:]
        if text.startswith('static '):
            text = text[7:]
        if text.endswith(';'):
            text = text[:-1]
        return text.strip()

    def _is_static_import(self, node: ExtractedNode) -> bool:
        """Check if static import"""
        text = AstTool.node_text(node)
        return 'static' in text

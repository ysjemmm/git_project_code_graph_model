"""Code block analyzer for Java AST"""

from dataclasses import dataclass

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import CodeBlockInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from tools.ast_tool import AstTool


@dataclass
class CodeBlockAnalyzer(BaseAnalyzer):
    """Analyzes Java Code block declarations"""

    def __init__(self):
        super().__init__()

    def handle_code_block_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> CodeBlockInfo | None:
        """Handle code block declaration node"""
        code_block_info = CodeBlockInfo()
        
        if not node or (self.get_node_type(node) != JavaAstNodeType.BLOCK.value and
                        self.get_node_type(node) != JavaAstNodeType.STATIC_INITIALIZER.value):
            return None
        
        # Set location from node
        code_block_info.set_pos_from_node(node)
        
        # Extract code block base info
        self._extract_block_base_info(node, code_block_info)
        
        return code_block_info

    def _extract_block_base_info(self, node: ExtractedNode, method: CodeBlockInfo):
        """Extract code block"""
        method.raw_method = AstTool.node_text(node)
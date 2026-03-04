"""Base analyzer class for Java AST analysis"""
from typing import Dict, List

from jedi.inference.gradual.typing import Tuple
from loraxmod import ExtractedNode
from sqlalchemy import false

from parser.languages.java import JavaAstNodeType
from parser.languages.java.core.ast_node_types import LocationRange
from tools.ast_tool import AstTool


class BaseAnalyzer:
    """Base class for all Java AST analyzers"""

    def __init__(self):
        """Initialize base analyzer"""
        self.type2node: Dict[JavaAstNodeType, List[ExtractedNode]] = {}
        pass

    @staticmethod
    def get_node_type(node) -> str:
        """Get node type from tree-sitter or ExtractedNode
        
        Tree-sitter nodes use 'type' attribute, while ExtractedNode uses 'node_type'.
        This method handles both cases.
        """
        if node is None:
            return ""
        
        # Try node_type first (ExtractedNode)
        if hasattr(node, 'node_type'):
            return node.node_type
        
        # Fall back to type (tree-sitter)
        if hasattr(node, 'type'):
            return node.type
        
        return ""

    @staticmethod
    def get_extractions(node) -> dict:
        """Get extractions from ExtractedNode or empty dict for tree-sitter nodes"""
        if node is None:
            return {}
        
        if hasattr(node, 'extractions'):
            return node.extractions
        
        return {}

    """
    is_final: ?
    is_static: ?
    """
    @staticmethod
    def extract_modifiers(node: ExtractedNode) -> Tuple[bool, bool]:
        """Extract modifiers from node"""
        modifier = node
        if node.node_type != JavaAstNodeType.MODIFIERS.value:
            modifier = AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True)
        if modifier is not None:
            return (
              " final" in AstTool.node_text(modifier),
              " static" in AstTool.node_text(modifier)
            )
        return False, False
"""Base analyzer class for Java AST analysis"""
from typing import Dict, List

from jedi.inference.gradual.typing import Tuple
from loraxmod import ExtractedNode

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

    @staticmethod
    def create_location_range(node) -> LocationRange:
        """Create location range from node
        
        Extracts location information from tree-sitter nodes.
        Tree-sitter uses 0-based line numbers, so we add 1 to convert to 1-based.
        """
        start_line = 0
        start_column = 0
        end_line = 0
        end_column = 0
        
        if node:
            # Tree-sitter nodes have start_point and end_point attributes
            # These are Point objects with row and column attributes
            if hasattr(node, 'start_point'):
                start_point = node.start_point
                if hasattr(start_point, 'row') and hasattr(start_point, 'column'):
                    start_line = start_point.row + 1  # Convert from 0-based to 1-based
                    start_column = start_point.column
                elif isinstance(start_point, (tuple, list)) and len(start_point) >= 2:
                    start_line = start_point[0] + 1
                    start_column = start_point[1]
            
            if hasattr(node, 'end_point'):
                end_point = node.end_point
                if hasattr(end_point, 'row') and hasattr(end_point, 'column'):
                    end_line = end_point.row + 1  # Convert from 0-based to 1-based
                    end_column = end_point.column
                elif isinstance(end_point, (tuple, list)) and len(end_point) >= 2:
                    end_line = end_point[0] + 1
                    end_column = end_point[1]
        
        return LocationRange(
            start_line=start_line,
            start_column=start_column,
            end_line=end_line,
            end_column=end_column
        )

    """
    is_final: ?
    is_static: ?
    """
    @staticmethod
    def extract_modifiers(node) -> Tuple[bool, bool]:
        """Extract modifiers from node"""
        text = AstTool.node_text(node)
        modifiers = [m.strip() for m in text.split() if m.strip() == 'final' or m.strip() == 'static']
        return modifiers.__contains__("final"), modifiers.__contains__("static")
"""Comment analyzer for Java AST"""

from loraxmod import ExtractedNode

from parser.languages.java import JavaAstNodeType
from parser.languages.java.core.ast_node_types import CommentInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from tools.ast_tool import AstTool


class CommentAnalyzer(BaseAnalyzer):
    """Analyzes Java comments"""
    
    def __init__(self):
        super().__init__()
    
    def handle_comment(self, node: ExtractedNode) -> CommentInfo | None:
        """Analyze comments in node"""

        node_type = self.get_node_type(node)
        if not node or (node_type != JavaAstNodeType.LINE_COMMENT.value and node_type != JavaAstNodeType.BLOCK_COMMENT.value):
            return None
        comment_info = CommentInfo()
        comment_info.set_pos_from_node(node)
        comment_info.raw_comment = AstTool.node_text(node)
        return comment_info


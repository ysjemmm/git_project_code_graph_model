"""Record analyzer for Java AST"""

from dataclasses import dataclass

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import RecordInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper


@dataclass
class RecordAnalyzer(BaseAnalyzer):
    """Analyzes Java record declarations"""

    def __init__(self, is_nested: bool = False):
        super().__init__()
        self._init()
        self._is_nested = is_nested

    def _init(self):
        self._record_info = RecordInfo()
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)

    def handle_record_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> RecordInfo | None:
        """Handle record declaration node"""
        from parser.languages.java.utils.analyzer_cache import AnalyzerCache

        self._init()
        self._ast_must_nodes(node)

        self._record_info.set_pos_from_node(node)
        self._record_info.annotations = AnalyzerHelper.extract_java_marked_annotation(node)

        # Extract record base
        self._extract_record_base(node, context)

        AnalyzerCache.get_record_body_analyzer(context.project_name).handle_record_body(
            self.type2node.get(JavaAstNodeType.CLASS_BODY, [None])[0],
            self._record_info,
            context
        )

        return self._record_info

    def _extract_record_base(self, node: ExtractedNode, context: AnalyzerContext) -> None:
        self._record_info.record_name = node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
        self._record_info.type_parameters = AnalyzerHelper.extract_java_type_parameters(node)
        self._record_info.raw_metadata = node.extractions.get(JavaAstNodeType.EX_RECORD_BODY.value, "")
        self._record_info.super_interfaces = AnalyzerHelper.extract_java_super_class(node, JavaAstNodeType.SUPER_INTERFACES)
        # parse components
        from parser.languages.java.utils.analyzer_cache import AnalyzerCache
        self._record_info.components, comments = AnalyzerHelper.extract_java_parameters_comments(node, AnalyzerCache.get_comment_analyzer(context.project_name))
        self._record_info.comments.extend(comments)

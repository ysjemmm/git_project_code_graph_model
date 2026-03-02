from dataclasses import dataclass
from typing import Tuple

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import ClassInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


@dataclass
class ClassAnalyzer(BaseAnalyzer):

    def __init__(self, is_nested: bool = False):
        super().__init__()
        self._init()
        self._is_nested = is_nested

    def _init(self):
        self.class_info = ClassInfo()
        self.type2node = {}

    def _ast_must_nodes(self, node):
        if node is not None:
            for n in node.children:
                self.type2node.setdefault(JavaAstNodeType.from_value(n.node_type), []).append(n)
    
    def handle_class_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> ClassInfo:
        self._init()
        self._ast_must_nodes(node)

        self.class_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
            AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True))
        self.class_info.mapping_uri, self.class_info.has_uri = self._extract_class_http_uri()
        context.before_uri_path = self.class_info.mapping_uri

        self.class_info.set_pos_from_node(node)
        self._extract_class_base(node)

        AnalyzerCache.get_class_body_analyzer(context.project_name).handle_class_body(
            self.type2node.get(JavaAstNodeType.CLASS_BODY, [None])[0],
            self.class_info,
            context
        )

        return self.class_info

    def _extract_class_base(self, node: ExtractedNode):
        """Extract class"""
        self.class_info.class_name = node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
        # self.class_info.raw_metadata = node.extractions.get(JavaAstNodeType.EX_INTERFACE_BODY.value, "")
        self.class_info.type_parameters = AnalyzerHelper.extract_java_type_parameters(node)
        self.class_info.super_interfaces = AnalyzerHelper.extract_java_super_class(node, JavaAstNodeType.SUPER_INTERFACES)

        super_classes = AnalyzerHelper.extract_java_super_class(node, JavaAstNodeType.SUPER_CLASS)
        self.class_info.super_class = super_classes[0] if super_classes else ""

    def _extract_class_http_uri(self) -> Tuple[str, bool]:
        req_anno = [anno for anno in self.class_info.annotations if anno.name.endswith('RequestMapping')]
        if not req_anno:  # 更简洁的检查方式
            return '', False

        annotation_values = req_anno[0].values
        uri_value = annotation_values.get('value') or annotation_values.get('path')
        if uri_value is None:
            return '', False
        if not uri_value.startswith('/'):
            uri_value = '/' + uri_value
        if not uri_value.endswith('/'):
            uri_value = uri_value + '/'
        return uri_value, True

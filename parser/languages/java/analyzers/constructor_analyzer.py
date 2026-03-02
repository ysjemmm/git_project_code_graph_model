"""Constructor analyzer for Java AST"""

from dataclasses import dataclass
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import ConstructorInfo, ParameterInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool


@dataclass
class ConstructorAnalyzer(BaseAnalyzer):
    """Analyzes Java constructor declarations"""
    
    current_class_name: str = ""

    def __init__(self):
        super().__init__()

    def handle_constructor_declaration(self, node: ExtractedNode, context: AnalyzerContext) -> ConstructorInfo | None:
        """Handle constructor declaration node"""
        constructor_info = ConstructorInfo()

        if not node or self.get_node_type(node) != JavaAstNodeType.CONSTRUCTOR_DECLARATION.value:
            return None

        # Set location from node
        constructor_info.set_pos_from_node(node)

        # Extract constructor base info
        self._extract_constructor_base_info(node, constructor_info)

        #Extract constructor marked annotation
        constructor_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
            AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True)
        )

        # Extract parameters
        constructor_info.parameters = self._extract_parameters(node, constructor_info, context)

        # Extract exceptions
        constructor_info.exceptions = self._extract_exceptions(node)

        return constructor_info

    def _extract_constructor_base_info(self, node: ExtractedNode, constructor: ConstructorInfo):
        """Extract constructor name"""
        constructor.constructor_name = node.extractions.get(JavaAstNodeType.IDENTIFIER.value, "")
        constructor.raw_method = node.extractions.get(JavaAstNodeType.EX_METHOD_BODY.value, "")

    def _extract_parameters(self, node: ExtractedNode, constructor: ConstructorInfo, context: AnalyzerContext) -> List[ParameterInfo]:
        """Extract constructor parameters"""
        parameters = []

        # Find formal parameters node
        params_node = AstTool.find_child_by_type(node, JavaAstNodeType.FORMAL_PARAMETERS.value, True)
        if not params_node:
            return parameters

        param_nodes = params_node.children
        for pn in param_nodes:
            if pn.node_type == JavaAstNodeType.BLOCK_COMMENT.value:
                comment_analyzer = AnalyzerCache.get_comment_analyzer(context.project_name)
                constructor.comments.append(comment_analyzer.handle_comment(pn))
                continue

            now_param_info = ParameterInfo()
            now_param_info.set_pos_from_node(pn)
            now_param_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
                AstTool.find_child_by_type(pn, JavaAstNodeType.MODIFIERS.value, True)
            )
            now_param_info.raw_parameter = AstTool.node_text(pn)
            if pn.node_type == JavaAstNodeType.FORMAL_PARAMETER.value:
                now_param_info.parameter_name = pn.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
                now_param_info.parameter_type = pn.extractions.get(JavaAstNodeType.EX_METHOD_PARAM_TYPE.value, "")

            elif pn.node_type == JavaAstNodeType.SPREAD_PARAMETERS.value:
                splits = AstTool.node_text(pn).split("...")
                now_param_info.parameter_name = splits[1] if len(splits) > 1 else ""
                now_param_info.parameter_type = splits[0] if len(splits) > 0 else ""
                now_param_info.is_varargs = True

            parameters.append(now_param_info)
        return parameters

    def _parse_parameter(self, node: ExtractedNode) -> ParameterInfo:
        """Parse a single parameter"""
        param_info = ParameterInfo()

        # Extract parameter type
        type_node = AstTool.find_child_by_type(node, JavaAstNodeType.TYPE.value)
        if type_node and not isinstance(type_node, list):
            param_info.parameter_type = AstTool.node_text(type_node)

        # Extract parameter name
        identifier = AstTool.find_child_by_type(node, JavaAstNodeType.IDENTIFIER.value)
        if identifier and not isinstance(identifier, list):
            param_info.parameter_name = AstTool.node_text(identifier)

        # Set location
        param_info.location = self.create_location_range(node)

        return param_info

    def _extract_exceptions(self, node: ExtractedNode) -> List[str]:
        """Extract thrown exceptions"""
        exceptions = []

        throws_node = AstTool.find_child_by_type(node, JavaAstNodeType.THROWS.value, True)
        if throws_node:
            for tn in AstTool.find_child_by_type(throws_node, JavaAstNodeType.TYPE_IDENTIFIER.value):
                exceptions.append(AstTool.node_text(tn))
        return exceptions
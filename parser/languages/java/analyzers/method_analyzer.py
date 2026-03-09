"""Method analyzer for Java AST"""

from dataclasses import dataclass
from typing import List

from loraxmod import ExtractedNode

from parser.languages.java.core.ast_node_types import MethodInfo, ParameterInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.java_ast_enums import JavaAstNodeType
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from tools.ast_tool import AstTool

SPRING_HTTP_ANNOTATION = ['RequestMapping', 'GetMapping', 'PostMapping', 'PutMapping', 'DeleteMapping', 'PatchMapping']

@dataclass
class MethodAnalyzer(BaseAnalyzer):
    """Analyzes Java method declarations"""
    
    current_class_name: str = ""

    def __init__(self, current_class_name: str = ""):
        super().__init__()

    def handle_method_declaration(self, node: ExtractedNode, context: AnalyzerContext, parent_symbol_id: str = "") -> MethodInfo | None:
        """Handle method declaration node"""
        method_info = MethodInfo()
        
        if not node or self.get_node_type(node) != JavaAstNodeType.METHOD_DECLARATION.value:
            return None
        
        # Set location from node
        method_info.set_pos_from_node(node)
        
        # Extract method base info
        self._extract_method_base_info(node, method_info)

        #Extract method marked annotation
        method_info.annotations = AnalyzerHelper.extract_java_marked_annotation(
            AstTool.find_child_by_type(node, JavaAstNodeType.MODIFIERS.value, True)
        )

        # 根据注解判断 URI 信息
        self._extract_http_info(method_info, context)
        
        # Extract modifiers
        method_info.is_final, method_info.is_static = self.extract_modifiers(node)

        # Extract type parameters
        method_info.type_parameters = AnalyzerHelper.extract_java_type_parameters(node)
        
        # Extract parameters (需要先提取参数以获取参数类型)
        method_info.parameters = self._extract_parameters(node, method_info, context)
        
        # Generate symbol_id (在提取参数后生成，因为需要参数类型)
        if parent_symbol_id and method_info.method_name:
            param_types = [p.parameter_type for p in method_info.parameters]
            method_info.symbol_id = AnalyzerHelper.generate_symbol_id_for_method(
                parent_symbol_id, method_info.method_name, param_types, method_info.is_static
            )
            method_info.parent_symbol_id = parent_symbol_id
            
            # 为每个参数生成 symbol_id
            for param in method_info.parameters:
                if param.parameter_name:
                    param.symbol_id = AnalyzerHelper.generate_symbol_id_for_parameter(
                        method_info.symbol_id, param.parameter_name
                    )
                    param.parent_symbol_id = method_info.symbol_id
        
        # Extract exceptions
        method_info.exceptions = self._extract_exceptions(node)
        
        return method_info

    def _extract_method_base_info(self, node: ExtractedNode, method: MethodInfo):
        """Extract method name"""
        method.method_name = node.extractions.get(JavaAstNodeType.IDENTIFIER.value, "")
        method.return_type = node.extractions.get(JavaAstNodeType.EX_METHOD_RETURN_TYPE.value, "")
        method.raw_method = node.extractions.get(JavaAstNodeType.EX_METHOD_BODY.value, "")

    def _extract_parameters(self, node: ExtractedNode, method: MethodInfo, context: AnalyzerContext) -> List[ParameterInfo]:
        """Extract method parameters"""
        parameters = []
        
        # Find formal parameters node
        params_node = AstTool.find_child_by_type(node, JavaAstNodeType.FORMAL_PARAMETERS.value, True)
        if not params_node:
            return parameters

        param_nodes = params_node.children
        for pn in param_nodes:
            if pn.node_type == JavaAstNodeType.BLOCK_COMMENT.value:
                comment_analyzer = AnalyzerCache.get_comment_analyzer(context.project_name)
                method.comments.append(comment_analyzer.handle_comment(pn))
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

    def _extract_exceptions(self, node: ExtractedNode) -> List[str]:
        """Extract thrown exceptions"""
        exceptions = []

        throws_node = AstTool.find_child_by_type(node, JavaAstNodeType.THROWS.value, True)
        if throws_node:
            for tn in AstTool.find_child_by_type(throws_node, JavaAstNodeType.TYPE_IDENTIFIER.value):
                exceptions.append(AstTool.node_text(tn))
        return exceptions

    def _extract_http_info(self, method_info, context: AnalyzerContext):
        annotations = method_info.annotations
        if annotations is None:
            return

        for annotation in annotations:
            if annotation.name == 'RequestMapping':
                support_method_str = annotation.values.get('method', 'GET')
                if support_method_str.startswith('{') and support_method_str.endswith('}'):
                    method_info.mapping_method_types = [signal_method.split(".")[1].strip().upper() for signal_method in support_method_str[1:-1].split(',')]
                else:
                    splits = support_method_str.split(".")
                    method_info.mapping_method_types = splits[1].strip() if len(splits) > 1 else splits[0].strip()
                method_info.base_mapping_uri = annotation.values.get("value", '')
            elif annotation.name == 'GetMapping':
                method_info.mapping_method_types = ['GET']
                method_info.base_mapping_uri = annotation.values.get("value", '')
            elif annotation.name == 'PostMapping':
                method_info.mapping_method_types = ['POST']
                method_info.base_mapping_uri = annotation.values.get("value", '')
            elif annotation.name == 'PutMapping':
                method_info.mapping_method_types = ['PUT']
                method_info.base_mapping_uri = annotation.values.get("value", '')
            elif annotation.name == 'DeleteMapping':
                method_info.mapping_method_types = ['DELETE']
                method_info.base_mapping_uri = annotation.values.get("value", '')
            elif annotation.name == 'PatchMapping':
                method_info.mapping_method_types = ['PATCH']
                method_info.base_mapping_uri = annotation.values.get("value", '')

        if method_info.base_mapping_uri is not None and len(method_info.mapping_method_types) > 0:
            if not method_info.base_mapping_uri.startswith('/'):
                method_info.base_mapping_uri = "/" + method_info.base_mapping_uri
            if not method_info.base_mapping_uri.endswith('/'):
                method_info.base_mapping_uri = method_info.base_mapping_uri[:-1]
            method_info.full_mapping_uri = AstTool.join_http_paths(context.before_uri_path, method_info.base_mapping_uri)
        elif context.before_uri_path is not None:
            method_info.full_mapping_uri = context.before_uri_path

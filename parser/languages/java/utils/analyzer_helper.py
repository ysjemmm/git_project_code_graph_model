import re
from typing import List, Tuple, Dict

from loraxmod import ExtractedNode

from parser.languages.java import JavaAstNodeType
from parser.languages.java.analyzers.comment_analyzer import CommentAnalyzer
from parser.languages.java.core.ast_node_types import MarkedAnnotationInfo, CommentInfo, ParameterInfo
from tools.ast_tool import AstTool

annotation_param_comments_pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''


class AnalyzerHelper:

    @staticmethod
    def extract_java_marked_annotation(raw_node: ExtractedNode | None) -> List[MarkedAnnotationInfo]:
        if not raw_node:
            return []

        # 判断当前节点是否是 identifier
        node = raw_node if raw_node.node_type == JavaAstNodeType.MODIFIERS.value else (
            AstTool.find_child_by_type(raw_node, JavaAstNodeType.MODIFIERS.value, True))

        child_nodes = [child for child in node.children if child.node_type in
                       {JavaAstNodeType.MARKER_ANNOTATION.value, JavaAstNodeType.ANNOTATION.value}]
        annotations = []
        for anno_node in child_nodes:
            anno = MarkedAnnotationInfo()
            anno.set_pos_from_node(anno_node)
            anno.name = anno_node.extractions.get(JavaAstNodeType.EX_IDENTIFIER.value, "")
            # 安全地获取参数，如果不存在则为 None
            params, comments = AnalyzerHelper._parse_annotation_param_comments(
                anno_node.extractions.get(JavaAstNodeType.EX_PARAMETERS.value))
            anno.values = params
            anno.comments = comments[0] if len(comments) > 0 else ""
            annotations.append(anno)
        return annotations

    @staticmethod
    def extract_java_super_class(node: ExtractedNode, super_node_type: JavaAstNodeType) -> list:
        super_node = AstTool.find_child_by_type(node, super_node_type.value, True)
        if isinstance(super_node, ExtractedNode):
            if super_node_type == JavaAstNodeType.SUPER_CLASS:
                type_idf_node = AstTool.find_child_by_type(super_node, JavaAstNodeType.TYPE_IDENTIFIER.value, True)
                if isinstance(type_idf_node, ExtractedNode):
                    return [AstTool.node_text(type_idf_node).strip()]
            else:
                type_list_node = AstTool.find_child_by_type(super_node, JavaAstNodeType.TYPE_LIST.value, True)
                if isinstance(type_list_node, ExtractedNode):
                    type_idf_nodes = AstTool.find_child_by_types(type_list_node,
                                                                 [JavaAstNodeType.TYPE_IDENTIFIER.value,
                                                                  JavaAstNodeType.GENERIC_TYPE.value])
                    return [AstTool.node_text(r).strip() for r in type_idf_nodes]
        return []

    @staticmethod
    def extract_java_type_parameters(node: ExtractedNode | None) -> list:
        if node is None:
            return []

        tp_node = node if node.node_type == JavaAstNodeType.TYPE_PARAMETERS else (
                    AstTool.find_child_by_type(node, JavaAstNodeType.TYPE_PARAMETERS.value, True))
        if tp_node:
            raw_text = AstTool.node_text(tp_node)
            return [item.strip() for item in raw_text.lstrip('<').rstrip('>').split(",")]
        return []

    @staticmethod
    def extract_java_parameters_comments(node: ExtractedNode, comment_analyzer: CommentAnalyzer) -> Tuple[List[ParameterInfo], List[CommentInfo]]:
        """Extract method parameters"""
        parameters = []
        comments = []

        # Find formal parameters node
        params_node = AstTool.find_child_by_type(node, JavaAstNodeType.FORMAL_PARAMETERS.value, True)
        if not params_node:
            return parameters, comments

        param_nodes = params_node.children
        for pn in param_nodes:
            if pn.node_type == JavaAstNodeType.BLOCK_COMMENT.value:
                comments.append(comment_analyzer.handle_comment(pn))
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
        return parameters, comments

    @staticmethod
    def extract_java_nested_object(filler, type2node: Dict, context, parent_symbol_id: str):
        from parser.languages.java.analyzers.class_analyzer import ClassAnalyzer
        from parser.languages.java.analyzers.enum_analyzer import EnumAnalyzer
        from parser.languages.java.analyzers.interface_analyzer import InterfaceAnalyzer
        from parser.languages.java.analyzers.annotation_analyzer import AnnotationAnalyzer
        from parser.languages.java.analyzers.record_analyzer import RecordAnalyzer

        class_analyzer = None
        enum_analyzer = None
        interface_analyzer = None
        annotation_analyzer = None
        record_analyzer = None

        for nested_obj in type2node.get(JavaAstNodeType.CLASS_DECLARATION, []):
            if class_analyzer is None:
                class_analyzer = ClassAnalyzer(True)
            if filler.nested_classes is None:
                filler.nested_classes = []
            filler.nested_classes.append(
                class_analyzer.handle_class_declaration(nested_obj, context, parent_symbol_id))

        for nested_obj in type2node.get(JavaAstNodeType.ENUM_DECLARATION, []):
            if enum_analyzer is None:
                enum_analyzer = EnumAnalyzer(True)
            if filler.nested_enums is None:
                filler.nested_enums = []
            filler.nested_enums.append(
                enum_analyzer.handle_enum_declaration(nested_obj, context, parent_symbol_id))

        for nested_obj in type2node.get(JavaAstNodeType.INTERFACE_DECLARATION, []):
            if interface_analyzer is None:
                interface_analyzer = InterfaceAnalyzer(True)
            if filler.nested_interfaces is None:
                filler.nested_interfaces = []
            filler.nested_interfaces.append(
                interface_analyzer.handle_interface_declaration(nested_obj, context, parent_symbol_id))

        for nested_obj in type2node.get(JavaAstNodeType.ANNOTATION_TYPE_DECLARATION, []):
            if annotation_analyzer is None:
                annotation_analyzer = AnnotationAnalyzer(True)
            if filler.nested_annotations is None:
                filler.nested_annotations = []
            filler.nested_annotations.append(
                annotation_analyzer.handle_annotation_declaration(nested_obj, context, parent_symbol_id))

        for nested_obj in type2node.get(JavaAstNodeType.RECORD_DECLARATION, []):
            if record_analyzer is None:
                record_analyzer = RecordAnalyzer(True)
            if filler.nested_records is None:
                filler.nested_records = []
            filler.nested_records.append(
                record_analyzer.handle_record_declaration(nested_obj, context, parent_symbol_id))

    @staticmethod
    def _parse_annotation_param_comments(parameters: str | None) -> Tuple[Dict[str, str], List[str]]:
        """解析注解参数并提取注释

        支持三种格式：
        1. None - 没有参数
        2. '("value")' - 单个值参数
        3. '(key = "value", key2 = "value2")' - 键值对参数
        4. '(method = {RequestMethod.GET, RequestMethod.POST})' - 数组参数

        还支持参数中包含注释，如 '("all"/**comment*/)'

        Args:
            parameters: 参数字符串，如 '("all")' 或 '(value = "/api/user", name = "haha")'

        Returns:
            (参数字典, 注释列表)
            例如: ({'value': 'all'}, ['comment'])
                  ({'value': '/api/user', 'name': 'haha'}, ['comment1', 'comment2'])
                  ({'method': '{RequestMethod.GET, RequestMethod.POST}'}, [])
        """
        result = {}
        comments = []

        if parameters is None or not parameters:
            return result, comments

        # 提取注释
        comments = AnalyzerHelper._extract_annotation_parameters_comments(parameters)

        # 移除注释
        params_str = AnalyzerHelper._remove_comments(parameters)

        # 移除外层括号
        params_str = params_str.strip()
        if params_str.startswith('(') and params_str.endswith(')'):
            params_str = params_str[1:-1].strip()

        if not params_str:
            return result, comments

        # 检查是否是键值对格式（包含 '='）
        if '=' in params_str:
            # 键值对格式：key = "value", key2 = "value2", method = {GET, POST}
            pairs = AnalyzerHelper._split_annotation_pairs(params_str)
            for pair in pairs:
                pair = pair.strip()
                if '=' in pair:
                    key, value = pair.split('=', 1)
                    key = key.strip()
                    value = value.strip()
                    # 处理不同类型的值：字符串、数组、枚举等
                    value = AnalyzerHelper._extract_annotation_value(value)
                    result[key] = value
        else:
            # 单个值格式：直接是值
            value = params_str.strip()
            value = AnalyzerHelper._extract_annotation_value(value)
            result['value'] = value

        return result, comments

    @staticmethod
    def _extract_annotation_value(value: str) -> str:
        """提取注解参数值，处理字符串、数组、枚举等类型
        
        Args:
            value: 原始值字符串
            
        Returns:
            处理后的值
        """
        value = value.strip()
        
        # 处理字符串（带引号）
        if (value.startswith('"') and value.endswith('"')) or \
           (value.startswith("'") and value.endswith("'")):
            return value[1:-1]
        
        # 处理数组 {value1, value2, ...}
        if value.startswith('{') and value.endswith('}'):
            return value
        
        # 处理其他类型（枚举、数字、布尔值等）保持原样
        return value

    @staticmethod
    def _extract_annotation_parameters_comments(text: str) -> List[str]:
        """从字符串中提取所有注释（块注释和行注释）

        包括注释的边界符号，如 /* ... */ 和 // ...

        Args:
            text: 包含注释的字符串

        Returns:
            注释内容列表（包括边界符号）
        """
        comments = []

        # 移除字符串中的内容，避免误识别字符串中的注释符号
        # 先用占位符替换所有字符串
        temp_text = text
        temp_text = re.sub(annotation_param_comments_pattern, '""', temp_text)

        # 提取块注释 /* ... */
        block_comments = re.findall(r'/\*.*?\*/', temp_text, re.DOTALL)
        comments.extend(block_comments)

        return comments

    @staticmethod
    def _remove_comments(text: str) -> str:
        """移除字符串中的注释（块注释和行注释）

        Args:
            text: 包含注释的字符串

        Returns:
            移除注释后的字符串
        """
        result = []
        i = 0
        in_string = False
        string_char = None

        while i < len(text):
            # 处理字符串
            if text[i] in ('"', "'") and (i == 0 or text[i - 1] != '\\'):
                if not in_string:
                    in_string = True
                    string_char = text[i]
                    result.append(text[i])
                elif text[i] == string_char:
                    in_string = False
                    string_char = None
                    result.append(text[i])
                else:
                    result.append(text[i])
                i += 1
            # 如果在字符串中，直接添加字符
            elif in_string:
                result.append(text[i])
                i += 1
            # 处理块注释 /* ... */
            elif i < len(text) - 1 and text[i:i + 2] == '/*':
                # 找到注释的结束
                end = text.find('*/', i + 2)
                if end != -1:
                    i = end + 2
                else:
                    i = len(text)
            # 处理行注释 // ...
            elif i < len(text) - 1 and text[i:i + 2] == '//':
                # 找到行尾
                end = text.find('\n', i)
                if end != -1:
                    i = end
                else:
                    i = len(text)
            else:
                result.append(text[i])
                i += 1

        return ''.join(result)

    @staticmethod
    def _split_annotation_pairs(params_str: str) -> List[str]:
        """分割注解参数对，处理嵌套的括号、花括号和引号

        Args:
            params_str: 参数字符串，如 'key = "value", key2 = "value2", method = {GET, POST}'

        Returns:
            参数对列表
        """
        pairs = []
        current_pair = ""
        in_quotes = False
        quote_char = None
        paren_depth = 0
        brace_depth = 0  # 追踪花括号深度

        for char in params_str:
            if char in ('"', "'") and (not in_quotes or quote_char == char):
                in_quotes = not in_quotes
                quote_char = char if in_quotes else None
                current_pair += char
            elif char == '(' and not in_quotes:
                paren_depth += 1
                current_pair += char
            elif char == ')' and not in_quotes:
                paren_depth -= 1
                current_pair += char
            elif char == '{' and not in_quotes:
                brace_depth += 1
                current_pair += char
            elif char == '}' and not in_quotes:
                brace_depth -= 1
                current_pair += char
            elif char == ',' and not in_quotes and paren_depth == 0 and brace_depth == 0:
                # 只在所有括号都关闭时才分割
                if current_pair.strip():
                    pairs.append(current_pair)
                current_pair = ""
            else:
                current_pair += char

        if current_pair.strip():
            pairs.append(current_pair)

        return pairs

    @staticmethod
    def generate_symbol_id_for_project(project_name: str) -> str:
        """为 Project 项目生成 symbol_id
        格式: project#{project_name}
        """
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_project(project_name)

    @staticmethod
    def generate_symbol_id_for_file(project_id: str, file_path: str) -> str:
        """为 Java 文件生成 symbol_id
        格式: {project_id}<file>{file_path}
        """
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_file(project_id, file_path)


    # ========== Symbol ID 生成方法 ==========
    
    @staticmethod
    def generate_symbol_id_for_class(parent_symbol_id: str, class_name: str) -> str:
        """为类/接口/枚举/注解/记录生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_class(parent_symbol_id, class_name)
    
    @staticmethod
    def generate_symbol_id_for_method(parent_symbol_id: str, method_name: str, 
                                     param_types: List[str], is_static: bool = False) -> str:
        """为方法生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        
        if is_static:
            return SymbolIdGenerator.for_static_method(parent_symbol_id, method_name, param_types)
        else:
            return SymbolIdGenerator.for_method(parent_symbol_id, method_name, param_types)
    
    @staticmethod
    def generate_symbol_id_for_constructor(parent_symbol_id: str, param_types: List[str]) -> str:
        """为构造器生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_constructor(parent_symbol_id, param_types)
    
    @staticmethod
    def generate_symbol_id_for_field(parent_symbol_id: str, field_name: str) -> str:
        """为字段生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_field(parent_symbol_id, field_name)
    
    @staticmethod
    def generate_symbol_id_for_enum_constant(parent_symbol_id: str, constant_name: str) -> str:
        """为枚举常量生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_enum_constant(parent_symbol_id, constant_name)
    
    @staticmethod
    def generate_symbol_id_for_parameter(method_symbol_id: str, param_name: str) -> str:
        """为方法参数生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_parameter(method_symbol_id, param_name)
    
    @staticmethod
    def generate_symbol_id_for_record_component(record_symbol_id: str, component_name: str) -> str:
        """为 record 组件生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_record_component(record_symbol_id, component_name)

    @staticmethod
    def generate_symbol_id_for_javadoc_comment(parent_symbol_id: str, index: int) -> str:
        """为 Javadoc 注释生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_javadoc_comment(parent_symbol_id, index)
    
    @staticmethod
    def generate_symbol_id_for_long_comment(parent_symbol_id: str) -> str:
        """为长注释生成 symbol_id"""
        from parser.common.symbol_table import SymbolIdGenerator
        return SymbolIdGenerator.for_long_comment(parent_symbol_id)

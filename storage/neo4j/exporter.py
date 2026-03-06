"""
Neo4j 导出器- 基于 AST 数据直接构建
按照 java_neo4j_modules 标准，从 AST 节点处理器的输出直接构建图数据库
"""
from collections import defaultdict
from pathlib import Path
from typing import Dict, Set, List, Any, Tuple

from parser.languages.java.core.ast_node_types import JavaFileStructure, ClassInfo, InterfaceInfo, \
    EnumInfo, AnnotationTypeInfo, RecordInfo, MethodInfo, ConstructorInfo, ParameterInfo, FieldInfo, EnumConstantInfo, \
    CodeBlockInfo
from parser.languages.java.symbol.symbol_commons import ClassLocation, ClassLocationType
from parser.languages.java.symbol.symbol_manager import SymbolManager
from parser.utils.logger import get_logger
from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.field_names import (
    NODE_SYMBOL_ID, NODE_NAME, NODE_QUALIFIED_NAME, NODE_BELONG_PROJECT,
    EDGE_SOURCE_SYMBOL, EDGE_TARGET_SYMBOL,
    INHERITANCE_IS_EXTENSION,
)
from storage.neo4j.java_modules import JavaGraphEdgeType, ObjectType, ObjectFromType, JavaNeo4jNodeType, \
    JavaFileNodeGraphNode, JavaObjectNodeGraphNode, JavaMethodNodeGraphNode, JavaParameterNodeGraphNode, \
    JavaFieldNodeGraphNode, JavaEnumConstantNodeGraphNode, CommentNodeGraphNode, CommentType, \
    CommentStorageDecision, JavadocParseResult, JavaCodeBlockNodeGraphNode
from storage.neo4j.merge_builder import MergeQueryBuilder, get_unique_key_for_node_type

logger = get_logger("neo4j_exporter")


# 注释存储策略配置
class CommentStorageConfig:
    """注释存储策略配置"""
    SHORT_COMMENT_THRESHOLD = 200  # 短注释阈值（字符数）
    MULTIPLE_COMMENT_THRESHOLD = 3  # 多条注释阈值
    MULTIPLE_COMMENT_MIN_LENGTH = 100  # 多条注释最小总长度

class JavadocParser:
    """Javadoc解析器"""
    
    @staticmethod
    def parse(content: str) -> JavadocParseResult:
        """解析Javadoc内容"""
        result = JavadocParseResult()
        
        lines = content.split('\n')
        summary_lines = []
        in_summary = True
        
        for line in lines:
            # 移除行首行* 和空行
            line = line.strip()
            if line.startswith('*'):
                line = line[1:].strip()

            if not line or line in ['/**', '*/']:
                continue
            
            # 解析标签
            if line.startswith('@'):
                in_summary = False
                
                if line.startswith('@param'):
                    param_text = line[6:].strip()
                    result.params.append(param_text)
                    
                elif line.startswith('@return'):
                    result.return_desc = line[7:].strip()
                    
                elif line.startswith('@throws') or line.startswith('@exception'):
                    tag_len = 7 if line.startswith('@throws') else 10
                    throws_text = line[tag_len:].strip()
                    result.throws.append(throws_text)
                    
                elif line.startswith('@author'):
                    result.author = line[7:].strip()
                    
                elif line.startswith('@version'):
                    result.version = line[8:].strip()
                    
                elif line.startswith('@since'):
                    result.since = line[6:].strip()
                    
                elif line.startswith('@deprecated'):
                    result.deprecated = line[11:].strip()
                    
                elif line.startswith('@see'):
                    result.see.append(line[4:].strip())
            else:
                # 摘要内容
                if in_summary:
                    summary_lines.append(line)
        
        result.summary = ' '.join(summary_lines)
        return result

class Neo4jExporterAST:
    """基于 AST 数据的 Neo4j 导出器"""
    
    def __init__(self, connector: Neo4jConnector, project_name: str = "",
                            project_id: str = "",
                            project_path: str = "",):
        self.connector = connector
        self.created_nodes: Set[str] = set()
        self.project_id = project_id
        self.project_name = project_name
        self.project_path = project_path
        self.relationships_to_create: List[Tuple[str, str, str]] = []
        self.nodes_to_create: Dict[JavaNeo4jNodeType, List] = {node_type: [] for node_type in JavaNeo4jNodeType}

        self.symbol_manager = SymbolManager(project_name = project_name)
    
    def export_from_ast_data(self, ast_data_list: List[JavaFileStructure],
                            clear_database: bool = True,
                            symbol_tables: List[Any] = None) -> Dict:
        """导出 AST 数据到 Neo4j"""
        try:
            if clear_database:
                self.connector.clear_database()
            
            self._prepare_project_node()
            
            for ast_data in ast_data_list:
                if ast_data:
                    self._collect_ast_file_nodes(ast_data)
            
            # 处理符号表中的关系（继承、实现、调用）
            self._parse_extend_impl_relationships(ast_data_list)
            # if symbol_tables:
            #     for symbol_table in symbol_tables:
            #         if symbol_table:
            #             self._collect_relationships_from_symbol_table(symbol_table)
            
            self._create_nodes_batch()
            self._create_relationships_batch()
            
            return {
                'success': True,
                'message': f'Successfully exported {len(self.created_nodes)} nodes',
                'created_nodes': len(self.created_nodes),
                'created_relationships': len(self.relationships_to_create)
            }
        except Exception as e:
            logger.error(f"Export failed: {e}")
            return {
                'success': False,
                'message': str(e),
                'created_nodes': 0,
                'created_relationships': 0
            }
    
    def _prepare_project_node(self):
        """准备项目节点"""
        properties = {
            NODE_SYMBOL_ID: self.project_id,
            NODE_NAME: self.project_name,
            NODE_QUALIFIED_NAME: self.project_id,
            NODE_BELONG_PROJECT: self.project_name,
        }
        self.nodes_to_create[JavaNeo4jNodeType.Project].append(properties)
        self.created_nodes.add(self.project_id)
    
    def _collect_ast_file_nodes(self, ast_data: JavaFileStructure | None):
        """收集单个 Java 文件的所有节点"""
        if ast_data is None:
            return

        java_file_node = JavaFileNodeGraphNode()
        
        # 计算相对路径
        try:
            file_path = Path(ast_data.file_path)
            project_path = Path(self.project_path)
            relative_path = file_path.relative_to(project_path)
            relative_path_str = str(relative_path).replace('\\', '/')  # 统一使用 / 分隔符
        except ValueError:
            # 如果无法计算相对路径，使用文件名
            relative_path_str = ast_data.file_name
        
        java_file_node.file_path = relative_path_str
        java_file_node.name = ast_data.file_name
        java_file_node.package_name = ast_data.package_info.name
        java_file_node.start_line = ast_data.location.start_line
        java_file_node.end_line = ast_data.location.end_line
        java_file_node.start_column = ast_data.location.start_column
        java_file_node.end_column = ast_data.location.end_column
        java_file_node.imports = [imp.import_path for imp in ast_data.import_details]
        java_file_node.belong_project = self.project_name
        java_file_node.file_type = "Java"
        java_file_node.full_path = ast_data.file_path
        java_file_node.symbol_id = ast_data.symbol_id  

        self.nodes_to_create[JavaNeo4jNodeType.File].append(java_file_node)
        self.created_nodes.add(java_file_node.symbol_id)

        self.relationships_to_create.append((self.project_id, java_file_node.symbol_id, JavaGraphEdgeType.HAVE.value))
        
        for class_data in ast_data.classes:
            self._collect_class_nodes(class_data, java_file_node)
        
        for interface_data in ast_data.interfaces:
            self._collect_interface_nodes(interface_data, java_file_node)
        
        for enum_data in ast_data.enums:
            self._collect_enum_nodes(enum_data, java_file_node)
        
        for annotation_data in ast_data.annotations:
            self._collect_annotation_nodes(annotation_data, java_file_node)
        
        for record_data in ast_data.records:
            self._collect_record_nodes(record_data, java_file_node)

        self.symbol_manager.collect_from_java_file(
            project_name=self.project_name,
            java_file_structure=ast_data)
    
    def _collect_class_nodes(self, class_data: ClassInfo | None, java_file_node: JavaFileNodeGraphNode):
        """收集类定义的所有节点"""
        if class_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = class_data.class_name
        java_object_node.qualified_name = java_file_node.package_name + "." + class_data.class_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.symbol_id = class_data.symbol_id  # 使用 analyzer 生成 symbol_id
        java_object_node.parent_symbol_id = class_data.parent_symbol_id
        java_object_node.type_parameters = class_data.type_parameters
        java_object_node.start_line = class_data.location.start_line
        java_object_node.end_line = class_data.location.end_line
        java_object_node.start_column = class_data.location.start_column
        java_object_node.end_column = class_data.location.end_column
        java_object_node.object_type = ObjectType.CLASS_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.request_uri = class_data.mapping_uri
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_class = class_data.super_class
        java_object_node.super_interfaces = class_data.super_interfaces

        java_object_node.annotations = [ann.name for ann in class_data.annotations]
        
        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append((java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value))
        
        # 收集注释节点
        self._collect_comment_nodes(class_data.comments, java_object_node.symbol_id, 
                                    java_object_node, java_object_node.belong_project)

        for code_blocks in class_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in class_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        
        for field_data in class_data.fields:
            self._collect_field_nodes(field_data, java_object_node)
        
        for constructor_data in class_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)
        
        # 处理嵌套类型
        for nested_class in class_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)
        
        for nested_interface in class_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)
        
        for nested_enum in class_data.nested_enums:
            self._collect_nested_enum_nodes(nested_enum, java_object_node)
        
        for nested_annotation in class_data.nested_annotations:
            self._collect_nested_annotation_nodes(nested_annotation, java_object_node)
        
        for nested_record in class_data.nested_records:
            self._collect_nested_record_nodes(nested_record, java_object_node)
    
    def _collect_interface_nodes(self, interface_data: InterfaceInfo | None, java_file_node: JavaFileNodeGraphNode) -> None:
        if interface_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = interface_data.interface_name
        java_object_node.qualified_name = java_file_node.package_name + "." + interface_data.interface_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.symbol_id = interface_data.symbol_id 
        java_object_node.parent_symbol_id = interface_data.parent_symbol_id
        java_object_node.start_line = interface_data.location.start_line
        java_object_node.end_line = interface_data.location.end_line
        java_object_node.start_column = interface_data.location.start_column
        java_object_node.end_column = interface_data.location.end_column
        java_object_node.object_type = ObjectType.INTERFACE_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = interface_data.extends_interfaces

        java_object_node.annotations = [ann.name for ann in interface_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value))

        # 收集注释节点
        self._collect_comment_nodes(interface_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        for method_data in interface_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        
        # 处理嵌套类型
        for nested_class in interface_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)
        
        for nested_interface in interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)
    
    def _collect_enum_nodes(self, enum_data: EnumInfo| None, java_file_node: JavaFileNodeGraphNode):
        if enum_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = enum_data.enum_name
        java_object_node.qualified_name = java_file_node.package_name + "." + enum_data.enum_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.symbol_id = enum_data.symbol_id 
        java_object_node.parent_symbol_id = enum_data.parent_symbol_id
        java_object_node.start_line = enum_data.location.start_line
        java_object_node.end_line = enum_data.location.end_line
        java_object_node.start_column = enum_data.location.start_column
        java_object_node.end_column = enum_data.location.end_column
        java_object_node.object_type = ObjectType.ENUM_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = enum_data.super_interfaces

        java_object_node.annotations = [ann.name for ann in enum_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value))

        # 收集注释节点
        self._collect_comment_nodes(enum_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        for code_blocks in enum_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in enum_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for field_data in enum_data.fields:
            self._collect_field_nodes(field_data, java_object_node)

        for constant_data in enum_data.enum_constants:
            self._collect_enum_constant_nodes(constant_data, java_object_node)
        
        # 处理嵌套类型
        for nested_class in enum_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)
        
        for nested_interface in enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)
    
    def _collect_annotation_nodes(self, annotation_data: AnnotationTypeInfo | None, java_file_node: JavaFileNodeGraphNode):
        if annotation_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = annotation_data.annotation_name
        java_object_node.qualified_name = java_file_node.package_name + "." + annotation_data.annotation_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.symbol_id = annotation_data.symbol_id
        java_object_node.parent_symbol_id = annotation_data.parent_symbol_id
        java_object_node.start_line = annotation_data.location.start_line
        java_object_node.end_line = annotation_data.location.end_line
        java_object_node.start_column = annotation_data.location.start_column
        java_object_node.end_column = annotation_data.location.end_column
        java_object_node.object_type = ObjectType.ANNOTATION_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.annotations = [ann.name for ann in annotation_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value))

        # 收集注释节点
        self._collect_comment_nodes(annotation_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        for field_data in annotation_data.elements:
            self._collect_field_nodes(field_data, java_object_node)
    
    def _collect_record_nodes(self, record_data: RecordInfo| None, java_file_node: JavaFileNodeGraphNode):
        if record_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = record_data.record_name
        java_object_node.qualified_name = java_file_node.package_name + "." + record_data.record_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.symbol_id = record_data.symbol_id  
        java_object_node.parent_symbol_id = record_data.parent_symbol_id
        java_object_node.type_parameters = record_data.type_parameters
        java_object_node.start_line = record_data.location.start_line
        java_object_node.end_line = record_data.location.end_line
        java_object_node.start_column = record_data.location.start_column
        java_object_node.end_column = record_data.location.end_column
        java_object_node.object_type = ObjectType.RECORD_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = record_data.super_interfaces
        java_object_node.annotations = [ann.name for ann in record_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value))

        # 收集注释节点
        self._collect_comment_nodes(record_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        for code_blocks in record_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in record_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for constructor_data in record_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)

        for param_data in record_data.components:
            self._collect_record_component_nodes(param_data, java_object_node)
    
    def _collect_method_nodes(self, method_data: MethodInfo, java_object_node: JavaObjectNodeGraphNode):
        if method_data is None:
            return

        java_method_node = JavaMethodNodeGraphNode()
        java_method_node.is_constructor = False
        java_method_node.name = method_data.method_name
        java_method_node.belong_project = java_object_node.belong_project

        param_types = [p.parameter_type for p in method_data.parameters]
        params_str = ",".join(param_types) if param_types else ""
        if method_data.is_static:
            java_method_node.is_static = True
            java_method_node.symbol_id = method_data.symbol_id  
        else:
            java_method_node.symbol_id = method_data.symbol_id  

        java_method_node.parent_symbol_id = method_data.parent_symbol_id
        java_method_node.start_line = method_data.location.start_line
        java_method_node.end_line = method_data.location.end_line
        java_method_node.start_column = method_data.location.start_column
        java_method_node.end_column = method_data.location.end_column
        java_method_node.raw_metadata = method_data.raw_method
        java_method_node.mapping_method_type = ""
        java_method_node.mapping_uri = method_data.base_mapping_uri
        java_method_node.throws_exceptions = method_data.exceptions
        java_method_node.type_parameters = method_data.type_parameters
        java_method_node.return_type = method_data.return_type
        java_method_node.mapping_method_type = ",".join(method_data.mapping_method_types) if method_data.mapping_method_types else ""
        java_method_node.base_uri = method_data.base_mapping_uri
        java_method_node.full_uri = method_data.full_mapping_uri
        java_object_node.annotations = [ann.name for ann in method_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node.symbol_id)
        self.relationships_to_create.append((java_object_node.symbol_id, java_method_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))
        
        # 收集注释节点
        self._collect_comment_nodes(method_data.comments, java_method_node.symbol_id,
                                    java_method_node, java_method_node.belong_project)
        
        for param_data in method_data.parameters:
            self._collect_parameter_nodes(param_data, java_method_node)

    def _collect_code_block_nodes(self, code_block_data: CodeBlockInfo | None, java_object_node: JavaObjectNodeGraphNode):
        if code_block_data is None:
            return

        java_cb_node = JavaCodeBlockNodeGraphNode()
        java_cb_node.name = "__CodeBlock__"
        java_cb_node.is_static = code_block_data.is_static
        java_cb_node.belong_project = java_object_node.belong_project

        java_cb_node.symbol_id = code_block_data.symbol_id
        java_cb_node.parent_symbol_id = code_block_data.parent_symbol_id
        java_cb_node.start_line = code_block_data.location.start_line
        java_cb_node.end_line = code_block_data.location.end_line
        java_cb_node.start_column = code_block_data.location.start_column
        java_cb_node.end_column = code_block_data.location.end_column
        java_cb_node.raw_metadata = code_block_data.raw_method

        self.nodes_to_create[JavaNeo4jNodeType.JavaCodeBlock].append(java_cb_node)
        self.created_nodes.add(java_cb_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_cb_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))

    def _collect_constructor_nodes(self, constructor_data: ConstructorInfo | None, java_object_node: JavaObjectNodeGraphNode):
        if constructor_data is None:
            return

        java_method_node = JavaMethodNodeGraphNode()
        java_method_node.name = constructor_data.constructor_name
        java_method_node.is_constructor = True
        java_method_node.belong_project = java_object_node.belong_project

        param_types = [p.parameter_type for p in constructor_data.parameters]
        params_str = ",".join(param_types) if param_types else ""
        java_method_node.symbol_id = constructor_data.symbol_id  

        java_method_node.parent_symbol_id = constructor_data.parent_symbol_id
        java_method_node.start_line = constructor_data.location.start_line
        java_method_node.end_line = constructor_data.location.end_line
        java_method_node.start_column = constructor_data.location.start_column
        java_method_node.end_column = constructor_data.location.end_column
        java_method_node.raw_metadata = constructor_data.raw_method
        java_method_node.throws_exceptions = constructor_data.exceptions
        java_method_node.annotations = [ann.name for ann in constructor_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_method_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))

        for param_data in constructor_data.parameters:
            self._collect_parameter_nodes(param_data, java_method_node)
    
    def _collect_field_nodes(self, field_data: FieldInfo | None, java_object_node: JavaObjectNodeGraphNode):
        if field_data is None:
            return

        java_field_node = JavaFieldNodeGraphNode()
        java_field_node.name = field_data.field_name
        java_field_node.is_static = field_data.is_static
        java_field_node.is_final = field_data.is_final
        java_field_node.has_default_value = field_data.has_initial_value
        java_field_node.default_value = field_data.initial_value
        java_field_node.belong_project = java_object_node.belong_project

        java_field_node.parent_symbol_id = field_data.parent_symbol_id
        java_field_node.symbol_id = field_data.symbol_id  

        java_field_node.start_line = field_data.location.start_line
        java_field_node.end_line = field_data.location.end_line
        java_field_node.start_column = field_data.location.start_column
        java_field_node.end_column = field_data.location.end_column
        java_field_node.raw_metadata = field_data.raw_field
        java_field_node.annotations = [ann.name for ann in field_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaField].append(java_field_node)
        self.created_nodes.add(java_field_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_field_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))
        
        # 收集注释节点
        self._collect_comment_nodes(field_data.comments, java_field_node.symbol_id,
                                    java_field_node, java_field_node.belong_project)
    
    def _collect_parameter_nodes(self, param_data: ParameterInfo | None, java_method_node: JavaMethodNodeGraphNode):
        if param_data is None:
            return

        java_param_node = JavaParameterNodeGraphNode()
        java_param_node.is_constructor = False
        java_param_node.name = param_data.parameter_name
        java_param_node.type_name = param_data.parameter_type
        java_param_node.belong_project = java_method_node.belong_project
        java_param_node.symbol_id = param_data.symbol_id  
        java_param_node.parent_symbol_id = java_method_node.symbol_id
        java_param_node.start_line = param_data.location.start_line
        java_param_node.end_line = param_data.location.end_line
        java_param_node.start_column = param_data.location.start_column
        java_param_node.end_column = param_data.location.end_column
        java_param_node.raw_metadata = param_data.raw_parameter
        java_param_node.annotations = [ann.name for ann in param_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethodParameter].append(java_param_node)
        self.created_nodes.add(java_param_node.symbol_id)
        self.relationships_to_create.append((java_method_node.symbol_id, java_param_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))

    def _collect_enum_constant_nodes(self, constant_data: EnumConstantInfo | None, java_object_node: JavaObjectNodeGraphNode):
        if constant_data is None:
            return

        java_enum_constant_node = JavaEnumConstantNodeGraphNode()
        java_enum_constant_node.name = constant_data.constant_name
        java_enum_constant_node.belong_project = java_object_node.belong_project
        java_enum_constant_node.parent_symbol_id = constant_data.parent_symbol_id
        java_enum_constant_node.symbol_id = constant_data.symbol_id  

        java_enum_constant_node.start_line = constant_data.location.start_line
        java_enum_constant_node.end_line = constant_data.location.end_line
        java_enum_constant_node.start_column = constant_data.location.start_column
        java_enum_constant_node.end_column = constant_data.location.end_column
        java_enum_constant_node.raw_metadata = constant_data.raw_constant
        java_enum_constant_node.arguments = constant_data.arguments
        java_enum_constant_node.annotations = [ann.name for ann in constant_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaEnumConstant].append(java_enum_constant_node)
        self.created_nodes.add(java_enum_constant_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_enum_constant_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))

    def _collect_record_component_nodes(self, param_data: ParameterInfo | None, record_node: JavaObjectNodeGraphNode):
        if param_data is None:
            return

        java_param_node = JavaParameterNodeGraphNode()
        java_param_node.is_constructor = False
        java_param_node.name = param_data.parameter_name
        java_param_node.type_name = param_data.parameter_type
        java_param_node.belong_project = record_node.belong_project
        java_param_node.symbol_id = param_data.symbol_id  
        java_param_node.parent_symbol_id = param_data.symbol_id
        java_param_node.start_line = param_data.location.start_line
        java_param_node.end_line = param_data.location.end_line
        java_param_node.start_column = param_data.location.start_column
        java_param_node.end_column = param_data.location.end_column
        java_param_node.raw_metadata = param_data.raw_parameter
        java_param_node.annotations = [ann.name for ann in param_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaRecordComponent].append(java_param_node)
        self.created_nodes.add(java_param_node.symbol_id)
        self.relationships_to_create.append((record_node.symbol_id, java_param_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value))
    
    def _get_unique_keys_for_node_type(self, node_type: str) -> List[str]:
        return get_unique_key_for_node_type(node_type)
    
    def _build_unwind_merge_nodes_query(self, node_type: str, unique_key_names: List[str]) -> str:
        unique_keys = ", ".join([f"{k}: node.{k}" for k in unique_key_names])
        
        # 构建 UNWIND 查询
        query = f"""
        UNWIND $nodes AS node
        MERGE (n:{node_type} {{{unique_keys}}})
        SET n += node
        RETURN count(n) as created
        """
        return query.strip()
    
    def _build_unwind_merge_relationships_query(self, rel_type: str) -> str:
        query = f"""
        UNWIND $relationships AS rel
        MATCH (source {{symbol_id: rel.source_id}})
        MATCH (target {{symbol_id: rel.target_id}})
        MERGE (source)-[r:{rel_type}]->(target)
        RETURN count(r) as created
        """
        return query.strip()
    
    def _create_nodes_batch(self) -> int:
        from dataclasses import asdict, is_dataclass
        
        total_created = 0
        
        for node_type, nodes in self.nodes_to_create.items():
            if not nodes:
                continue
            
            try:
                # 使用 UNWIND 批量创建节点
                unique_key_names = self._get_unique_keys_for_node_type(node_type.value)
                
                nodes_dicts = []
                for node in nodes:
                    if isinstance(node, dict):
                        nodes_dicts.append(node)
                    elif is_dataclass(node):
                        nodes_dicts.append(asdict(node))
                    elif hasattr(node, '__dict__'):
                        nodes_dicts.append(node.__dict__)
                    else:
                        logger.warning(f"Cannot serialize node of type {type(node)}, skipping")
                        continue
                
                if not nodes_dicts:
                    continue
                
                # 构建 UNWIND 查询
                query = self._build_unwind_merge_nodes_query(node_type.value, unique_key_names)
                parameters = {"nodes": nodes_dicts}
                
                result = self.connector.execute_query(query, parameters)
                created_count = len(nodes_dicts)
                total_created += created_count
                logger.info(f"Created {created_count} {node_type} nodes using UNWIND")
            except Exception as e:
                logger.error(f"Failed to create {node_type} nodes with UNWIND: {e}")
                # 回退到逐个创建
                try:
                    for node in nodes:
                        if isinstance(node, dict):
                            node_dict = node
                        elif is_dataclass(node):
                            node_dict = asdict(node)
                        else:
                            node_dict = node.__dict__ if hasattr(node, '__dict__') else node
                        
                        query = MergeQueryBuilder.build_merge_node_query(node_type.value, node_dict)
                        self.connector.execute_query(query, node_dict)
                        total_created += 1
                except Exception as e2:
                    logger.error(f"Fallback: Failed to create {node_type.value} nodes: {e2}")
        
        logger.info(f"Created {total_created} nodes in total")
        return total_created
    
    def _create_relationships_batch(self) -> int:
        total_created = 0
        failed_relationships = []
        
        try:
            if not self.relationships_to_create:
                return 0
            
            relationships_by_type = {}
            for source_id, target_id, rel_type in self.relationships_to_create:
                if rel_type not in relationships_by_type:
                    relationships_by_type[rel_type] = []
                relationships_by_type[rel_type].append({
                    "source_id": source_id,
                    "target_id": target_id
                })
            
            # 使用 UNWIND 批量创建关系
            for rel_type, relationships in relationships_by_type.items():
                try:
                    query = self._build_unwind_merge_relationships_query(rel_type)
                    parameters = {"relationships": relationships}
                    
                    result = self.connector.execute_query(query, parameters)
                    created_count = len(relationships)
                    total_created += created_count
                    logger.info(f"Created {created_count} {rel_type} relationships using UNWIND")
                except Exception as e:
                    logger.error(f"Failed to create {rel_type} relationships with UNWIND: {e}")
                    try:
                        for rel in relationships:
                            try:
                                query = MergeQueryBuilder.build_merge_relationship_query(
                                    rel["source_id"], rel["target_id"], rel_type
                                )
                                parameters = {
                                    "source_id": rel["source_id"],
                                    "target_id": rel["target_id"]
                                }
                                self.connector.execute_query(query, parameters)
                                total_created += 1
                            except Exception as rel_error:
                                failed_relationships.append({
                                    "source_id": rel["source_id"],
                                    "target_id": rel["target_id"],
                                    "rel_type": rel_type,
                                    "error": str(rel_error)
                                })
                                logger.warning(f"Failed to create relationship: {rel['source_id']} -[{rel_type}]-> {rel['target_id']}: {rel_error}")
                    except Exception as e2:
                        logger.error(f"Fallback: Failed to create {rel_type} relationships: {e2}")
        except Exception as e:
            logger.error(f"Failed to create relationships: {e}")
        
        if failed_relationships:
            logger.warning(f"Total failed relationships: {len(failed_relationships)}")
            for failed_rel in failed_relationships[:10]:
                logger.warning(f"  {failed_rel['source_id']} -[{failed_rel['rel_type']}]-> {failed_rel['target_id']}: {failed_rel['error']}")
        
        logger.info(f"Created {total_created} relationships in total")
        return total_created
    
    def _collect_relationships_from_symbol_table(self, symbol_table: Any) -> None:
        """从符号表中收集关系（继承、实现、调用、访问）"""
        try:
            if hasattr(symbol_table, 'inheritance_edges'):
                inheritance_count = 0
                for edge in symbol_table.inheritance_edges:
                    source_id = self._get_attr(edge, EDGE_SOURCE_SYMBOL, None)
                    target_id = self._get_attr(edge, EDGE_TARGET_SYMBOL, None)
                    is_extension = self._get_attr(edge, INHERITANCE_IS_EXTENSION, False)
                    
                    if source_id and target_id:
                        # 只添加已创建的节点之间的关系
                        if source_id in self.created_nodes and target_id in self.created_nodes:
                            rel_type = JavaGraphEdgeType.EXTENDS.value if is_extension else JavaGraphEdgeType.IMPLEMENTS.value
                            self.relationships_to_create.append((source_id, target_id, rel_type))
                            inheritance_count += 1
                            logger.debug(f"Added {rel_type} relationship: {source_id} -> {target_id}")
                logger.info(f"Collected {inheritance_count} inheritance relationships from symbol table")
            
            # 处理方法调用关系
            if hasattr(symbol_table, 'call_edges'):
                call_count = 0
                for edge in symbol_table.call_edges:
                    source_id = self._get_attr(edge, EDGE_SOURCE_SYMBOL, None)
                    target_id = self._get_attr(edge, EDGE_TARGET_SYMBOL, None)
                    
                    if source_id and target_id:
                        # 只添加已创建的节点之间的关系
                        if source_id in self.created_nodes and target_id in self.created_nodes:
                            self.relationships_to_create.append((source_id, target_id, JavaGraphEdgeType.CALLS.value))
                            call_count += 1
                            logger.debug(f"Added CALLS relationship: {source_id} -> {target_id}")
                logger.info(f"Collected {call_count} call relationships from symbol table")
            
            # 处理字段访问关系
            if hasattr(symbol_table, 'access_edges'):
                access_count = 0
                for edge in symbol_table.access_edges:
                    source_id = self._get_attr(edge, EDGE_SOURCE_SYMBOL, None)
                    target_id = self._get_attr(edge, EDGE_TARGET_SYMBOL, None)
                    
                    if source_id and target_id:
                        # 只添加已创建的节点之间的关系
                        if source_id in self.created_nodes and target_id in self.created_nodes:
                            self.relationships_to_create.append((source_id, target_id, JavaGraphEdgeType.ACCESSES.value))
                            access_count += 1
                            logger.debug(f"Added ACCESSES relationship: {source_id} -> {target_id}")
                logger.info(f"Collected {access_count} access relationships from symbol table")
        
        except Exception as e:
            logger.error(f"Failed to collect relationships from symbol table: {e}")
            import traceback
            traceback.print_exc()

    def _parse_extend_impl_relationships(self, ast_data_list: List[JavaFileStructure]):
        for ast_data in ast_data_list:
            for c in ast_data.classes:
                if c.super_class is not None and c.super_class.strip() != '':
                    location = self.symbol_manager.parse_java_object_where(c.super_class.split("<")[0], ast_data, project_name=self.project_name)
                    self._parse_class_location_to_node(location, c.symbol_id, JavaGraphEdgeType.EXTENDS.value, ObjectType.CLASS_TYPE)
                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(interface.split("<")[0], ast_data, project_name=self.project_name)
                    self._parse_class_location_to_node(location, c.symbol_id, JavaGraphEdgeType.IMPLEMENTS.value, ObjectType.INTERFACE_TYPE)
            for c in ast_data.interfaces:
                for interface in c.extends_interfaces:
                    location = self.symbol_manager.parse_java_object_where(interface.split("<")[0], ast_data, project_name=self.project_name)
                    self._parse_class_location_to_node(location, c.symbol_id, JavaGraphEdgeType.EXTENDS.value, ObjectType.INTERFACE_TYPE)
            for c in ast_data.enums:
                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(interface.split("<")[0], ast_data, project_name=self.project_name)
                    self._parse_class_location_to_node(location, c.symbol_id, JavaGraphEdgeType.IMPLEMENTS.value, ObjectType.INTERFACE_TYPE)
            for c in ast_data.records:
                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(interface.split("<")[0], ast_data, project_name=self.project_name)
                    self._parse_class_location_to_node(location, c.symbol_id,JavaGraphEdgeType.IMPLEMENTS.value, ObjectType.INTERFACE_TYPE)

    def _parse_class_location_to_node(self, location: ClassLocation, class_symbol_id: str, extend_type: str, object_type: ObjectType):
        if location is None:
            return
        if location.type == ClassLocationType.EXTERNAL:
            java_object = JavaObjectNodeGraphNode()
            java_object.symbol_id = location.jar_path + '<path>' + location.fqn
            java_object.qualified_name = location.fqn
            java_object.name = location.fqn.rsplit(".", 1)[-1]
            java_object.object_type = object_type.value
            java_object.belong_project = Path(location.jar_path).stem if location.jar_path else f"UNKNOWN[{ClassLocationType.EXTERNAL.value}]"
            java_object.from_type = ObjectFromType.EXTERNAL_DEFINITION.value
            self.created_nodes.add(java_object.symbol_id)
            self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object)
            self.relationships_to_create.append((class_symbol_id, java_object.symbol_id, extend_type))
        elif location.type == ClassLocationType.JDK:
            # JDK 标准库类
            java_object = JavaObjectNodeGraphNode()
            java_object.symbol_id = location.jar_path + '<path>' + location.fqn if location.jar_path else f"JDK<path>{location.fqn}"
            java_object.qualified_name = location.fqn
            java_object.name = location.fqn.rsplit(".", 1)[-1]
            java_object.object_type = object_type.value
            # 从 jar_path 提取 JDK 模块名（例如 java.base.jmod -> java.base）
            if location.jar_path:
                jar_name = Path(location.jar_path).stem  # 例如 "java.base"
                java_object.belong_project = f"JDK:{jar_name}"
            else:
                java_object.belong_project = "JDK"
            java_object.from_type = ObjectFromType.JDK_DEFINITION.value
            self.created_nodes.add(java_object.symbol_id)
            self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object)
            self.relationships_to_create.append((class_symbol_id, java_object.symbol_id, extend_type))
        elif location.type == ClassLocationType.INTERNAL:
            self.relationships_to_create.append((class_symbol_id, location.symbol_id, extend_type))
        elif location.type == ClassLocationType.UNKNOWN:
            java_object = JavaObjectNodeGraphNode()
            # 修复：处理 None 值
            jar_path = location.jar_path or "UNKNOWN"
            fqn = location.fqn or "UNKNOWN"
            java_object.symbol_id = f"{jar_path}<path>{fqn}"
            java_object.belong_project = "UNKNOWN"
            java_object.qualified_name = fqn
            java_object.name = fqn.rsplit(".", 1)[-1] if fqn != "UNKNOWN" else "UNKNOWN"
            java_object.object_type = object_type.value
            java_object.from_type = ObjectFromType.UNKNOWN_DEFINITION.value
            self.created_nodes.add(java_object.symbol_id)
            self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object)
            self.relationships_to_create.append((class_symbol_id, java_object.symbol_id, extend_type))

    # ==================== 嵌套类型处理方法 ====================
    
    def _collect_nested_class_nodes(self, nested_class_data: ClassInfo, parent_object_node: JavaObjectNodeGraphNode, depth: int = 0):
        if depth > 50:
            logger.warning(f"嵌套深度超过限制(50层): {nested_class_data.class_name}")
            return
        
        if nested_class_data is None:
            return
        
        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_class_data.class_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_class_data.class_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_class_data.symbol_id  
        java_object_node.parent_symbol_id = nested_class_data.parent_symbol_id
        java_object_node.type_parameters = nested_class_data.type_parameters
        java_object_node.start_line = nested_class_data.location.start_line
        java_object_node.end_line = nested_class_data.location.end_line
        java_object_node.start_column = nested_class_data.location.start_column
        java_object_node.end_column = nested_class_data.location.end_column
        java_object_node.object_type = ObjectType.CLASS_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.request_uri = nested_class_data.mapping_uri
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_class_data.annotations]
        
        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )
        
        # 收集注释节点
        self._collect_comment_nodes(nested_class_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)
        
        # 处理成员
        for method_data in nested_class_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        
        for field_data in nested_class_data.fields:
            self._collect_field_nodes(field_data, java_object_node)
        
        for constructor_data in nested_class_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)
        
        for nested_class in nested_class_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)
        
        for nested_interface in nested_class_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)
        
        for nested_enum in nested_class_data.nested_enums:
            self._collect_nested_enum_nodes(nested_enum, java_object_node, depth + 1)
        
        for nested_annotation in nested_class_data.nested_annotations:
            self._collect_nested_annotation_nodes(nested_annotation, java_object_node, depth + 1)
        
        for nested_record in nested_class_data.nested_records:
            self._collect_nested_record_nodes(nested_record, java_object_node, depth + 1)
    
    def _collect_nested_interface_nodes(self, nested_interface_data: InterfaceInfo, parent_object_node: JavaObjectNodeGraphNode, depth: int = 0):
        if depth > 50:
            logger.warning(f"嵌套深度超过限制(50层): {nested_interface_data.interface_name}")
            return
        
        if nested_interface_data is None:
            return
        
        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_interface_data.interface_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_interface_data.interface_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_interface_data.symbol_id  
        java_object_node.parent_symbol_id = nested_interface_data.parent_symbol_id
        java_object_node.start_line = nested_interface_data.location.start_line
        java_object_node.end_line = nested_interface_data.location.end_line
        java_object_node.start_column = nested_interface_data.location.start_column
        java_object_node.end_column = nested_interface_data.location.end_column
        java_object_node.object_type = ObjectType.INTERFACE_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_interface_data.annotations]
        
        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )
        
        # 收集注释节点
        self._collect_comment_nodes(nested_interface_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)
        
        # 处理成员
        for method_data in nested_interface_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        
        for field_data in nested_interface_data.fields:
            self._collect_field_nodes(field_data, java_object_node)
        
        # 递归处理嵌套类型
        for nested_class in nested_interface_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)
        
        for nested_interface in nested_interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)
    
    def _collect_nested_enum_nodes(self, nested_enum_data: EnumInfo, parent_object_node: JavaObjectNodeGraphNode, depth: int = 0):
        if depth > 50:
            logger.warning(f"嵌套深度超过限制(50层): {nested_enum_data.enum_name}")
            return

        if nested_enum_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_enum_data.enum_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_enum_data.enum_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_enum_data.symbol_id
        java_object_node.parent_symbol_id = nested_enum_data.parent_symbol_id
        java_object_node.start_line = nested_enum_data.location.start_line
        java_object_node.end_line = nested_enum_data.location.end_line
        java_object_node.start_column = nested_enum_data.location.start_column
        java_object_node.end_column = nested_enum_data.location.end_column
        java_object_node.object_type = ObjectType.ENUM_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_enum_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        # 收集注释节点
        self._collect_comment_nodes(nested_enum_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        # 处理成员
        for method_data in nested_enum_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for field_data in nested_enum_data.fields:
            self._collect_field_nodes(field_data, java_object_node)

        for constructor_data in nested_enum_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)

        for constant_data in nested_enum_data.enum_constants:
            self._collect_enum_constant_nodes(constant_data, java_object_node)

        # 递归处理嵌套类型
        for nested_class in nested_enum_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)

        for nested_interface in nested_enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)

    def _collect_nested_annotation_nodes(self, nested_annotation_data: AnnotationTypeInfo, parent_object_node: JavaObjectNodeGraphNode, depth: int = 0):
        if depth > 50:
            logger.warning(f"嵌套深度超过限制(50层): {nested_annotation_data.annotation_name}")
            return

        if nested_annotation_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_annotation_data.annotation_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_annotation_data.annotation_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_annotation_data.symbol_id
        java_object_node.parent_symbol_id = nested_annotation_data.parent_symbol_id
        java_object_node.start_line = nested_annotation_data.location.start_line
        java_object_node.end_line = nested_annotation_data.location.end_line
        java_object_node.start_column = nested_annotation_data.location.start_column
        java_object_node.end_column = nested_annotation_data.location.end_column
        java_object_node.object_type = ObjectType.ANNOTATION_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_annotation_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        # 收集注释节点
        self._collect_comment_nodes(nested_annotation_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)

        # 处理成员
        for element_data in nested_annotation_data.elements:
            self._collect_field_nodes(element_data, java_object_node)

    def _collect_nested_record_nodes(self, nested_record_data: RecordInfo, parent_object_node: JavaObjectNodeGraphNode, depth: int = 0):
        if depth > 50:
            logger.warning(f"嵌套深度超过限制(50层): {nested_record_data.record_name}")
            return
        
        if nested_record_data is None:
            return
        
        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_record_data.record_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_record_data.record_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_record_data.symbol_id  
        java_object_node.parent_symbol_id = nested_record_data.parent_symbol_id
        java_object_node.type_parameters = nested_record_data.type_parameters
        java_object_node.start_line = nested_record_data.location.start_line
        java_object_node.end_line = nested_record_data.location.end_line
        java_object_node.start_column = nested_record_data.location.start_column
        java_object_node.end_column = nested_record_data.location.end_column
        java_object_node.object_type = ObjectType.RECORD_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_record_data.annotations]
        
        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )
        
        # 收集注释节点
        self._collect_comment_nodes(nested_record_data.comments, java_object_node.symbol_id,
                                    java_object_node, java_object_node.belong_project)
        
        # 处理成员
        for method_data in nested_record_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        
        for constructor_data in nested_record_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)
        
        for component_data in nested_record_data.components:
            self._collect_record_component_nodes(component_data, java_object_node)

    def _get_attr(self, obj: Any, attr: str, default: Any = None) -> Any:
        """获取对象属性，支持对象和字段"""
        if obj is None:
            return default
        if isinstance(obj, dict):
            return obj.get(attr, default)
        return getattr(obj, attr, default)

    def _decide_comment_storage_strategy(self, comments: List[Any]) -> CommentStorageDecision:
        """决定注释存储策略

        Args:
            comments: 注释列表

        Returns:
            CommentStorageDecision: 存储决策
        """
        if not comments:
            return CommentStorageDecision.STORE_AS_ATTRIBUTE

        # 检查是否包含Javadoc
        has_javadoc = any(
            c.raw_comment.strip().startswith('/**')
            for c in comments
            if hasattr(c, 'raw_comment')
        )

        if has_javadoc:
            return CommentStorageDecision.CREATE_JAVADOC_NODE

        # 计算总长度
        total_text = "\n".join([c.raw_comment for c in comments if hasattr(c, 'raw_comment')])
        total_length = len(total_text)

        # 规则1: 总长度超过阈-> 创建长注释节点
        if total_length > CommentStorageConfig.SHORT_COMMENT_THRESHOLD:
            return CommentStorageDecision.CREATE_LONG_COMMENT_NODE

        # 规则2: 多条注释且总长点 > 100 -> 创建长注释节点
        if (len(comments) > CommentStorageConfig.MULTIPLE_COMMENT_THRESHOLD and
                total_length > CommentStorageConfig.MULTIPLE_COMMENT_MIN_LENGTH):
            return CommentStorageDecision.CREATE_LONG_COMMENT_NODE

        # 默认: 存为属性
        return CommentStorageDecision.STORE_AS_ATTRIBUTE

    def _collect_comment_nodes(self, comments: List[Any], parent_symbol_id: str,
                               parent_node: Any, parent_belong_project: str) -> None:
        """收集注释节点（混合策略）

        Args:
            comments: 注释列表
            parent_symbol_id: 父节点symbol_id
            parent_node: 父节点对象（用于设置simple_comment属性）
            parent_belong_project: 所属项目
        """
        if not comments:
            parent_node.simple_comment = ""
            parent_node.has_detailed_comment = False
            return

        # 决定存储策略
        decision = self._decide_comment_storage_strategy(comments)

        if decision == CommentStorageDecision.STORE_AS_ATTRIBUTE:
            # 策略A: 存为属性
            all_text = "\n".join([c.raw_comment for c in comments if hasattr(c, 'raw_comment')])
            parent_node.simple_comment = all_text
            parent_node.has_detailed_comment = False

        elif decision == CommentStorageDecision.CREATE_JAVADOC_NODE:
            # 策略B: 创建Javadoc节点
            parent_node.has_detailed_comment = True
            self._create_javadoc_nodes(comments, parent_symbol_id, parent_node, parent_belong_project)

        elif decision == CommentStorageDecision.CREATE_LONG_COMMENT_NODE:
            # 策略C: 创建长注释节点
            parent_node.has_detailed_comment = True
            self._create_long_comment_node(comments, parent_symbol_id, parent_belong_project)

    def _create_javadoc_nodes(self, comments: List[Any], parent_symbol_id: str,
                              parent_node: Any, parent_belong_project: str) -> None:
        """创建Javadoc节点

        Args:
            comments: 注释列表
            parent_symbol_id: 父节点symbol_id
            parent_node: 父节点对象
            parent_belong_project: 所属项目
        """
        javadoc_comments = []
        other_comments = []

        for comment in comments:
            if not hasattr(comment, 'raw_comment'):
                continue

            if comment.raw_comment.strip().startswith('/**'):
                javadoc_comments.append(comment)
            else:
                other_comments.append(comment)

        # 为每个Javadoc创建节点
        for idx, comment in enumerate(javadoc_comments):
            comment_node = CommentNodeGraphNode()
            comment_node.content = comment.raw_comment
            comment_node.comment_type = CommentType.JAVADOC.value
            comment_node.belong_project = parent_belong_project
            comment_node.char_count = len(comment.raw_comment)
            comment_node.line_count = comment.raw_comment.count('\n') + 1

            # 解析Javadoc
            javadoc_result = JavadocParser.parse(comment.raw_comment)
            comment_node.javadoc_summary = javadoc_result.summary
            comment_node.javadoc_params = javadoc_result.params
            comment_node.javadoc_return = javadoc_result.return_desc
            comment_node.javadoc_throws = javadoc_result.throws
            comment_node.javadoc_author = javadoc_result.author
            comment_node.javadoc_version = javadoc_result.version
            comment_node.javadoc_since = javadoc_result.since
            comment_node.javadoc_deprecated = javadoc_result.deprecated
            comment_node.javadoc_see = javadoc_result.see

            # 位置信息
            comment_node.start_line = comment.location.start_line
            comment_node.end_line = comment.location.end_line
            comment_node.start_column = comment.location.start_column
            comment_node.end_column = comment.location.end_column

            # 生成symbol_id
            comment_node.symbol_id = f"{parent_symbol_id}@javadoc#{idx}"
            comment_node.parent_symbol_id = parent_symbol_id
            comment_node.name = f"javadoc_{idx}"

            # 添加到待创建列表
            self.nodes_to_create[JavaNeo4jNodeType.Comment].append(comment_node)
            self.created_nodes.add(comment_node.symbol_id)

            # 创建关系
            self.relationships_to_create.append(
                (parent_symbol_id, comment_node.symbol_id, JavaGraphEdgeType.HAS_COMMENT.value)
            )

        # 其他简短注释存为属性
        if other_comments:
            other_text = "\n".join([c.raw_comment for c in other_comments])
            parent_node.simple_comment = other_text
        else:
            parent_node.simple_comment = ""

    def _create_long_comment_node(self, comments: List[Any], parent_symbol_id: str,
                                  parent_belong_project: str) -> None:
        """创建长注释节点（聚合所有注释）

        Args:
            comments: 注释列表
            parent_symbol_id: 父节点symbol_id
            parent_belong_project: 所属项目
        """
        comment_node = CommentNodeGraphNode()

        # 合并所有注释
        all_text = "\n---\n".join([c.raw_comment for c in comments if hasattr(c, 'raw_comment')])
        comment_node.content = all_text
        comment_node.comment_type = CommentType.LONG_COMMENT.value
        comment_node.belong_project = parent_belong_project
        comment_node.char_count = len(all_text)
        comment_node.line_count = all_text.count('\n') + 1

        # 使用第一个和最后一个注释的位置
        first_comment = comments[0]
        last_comment = comments[-1]
        comment_node.start_line = first_comment.location.start_line
        comment_node.end_line = last_comment.location.end_line
        comment_node.start_column = first_comment.location.start_column
        comment_node.end_column = last_comment.location.end_column

        # 生成symbol_id
        comment_node.symbol_id = f"{parent_symbol_id}@longcomment"
        comment_node.parent_symbol_id = parent_symbol_id
        comment_node.name = "long_comment"

        # 添加到待创建列表
        self.nodes_to_create[JavaNeo4jNodeType.Comment].append(comment_node)
        self.created_nodes.add(comment_node.symbol_id)

        # 创建关系
        self.relationships_to_create.append(
            (parent_symbol_id, comment_node.symbol_id, JavaGraphEdgeType.HAS_COMMENT.value)
        )
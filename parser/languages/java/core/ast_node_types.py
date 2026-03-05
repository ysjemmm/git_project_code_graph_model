"""AST Node Types - Data classes for Java AST structures"""

from dataclasses import dataclass, field
from typing import List, Optional, Dict, Any

from loraxmod import ExtractedNode


@dataclass
class LocationRange:
    """Location range in source code"""
    start_line: int = 0
    start_column: int = 0
    end_line: int = 0
    end_column: int = 0

    @property
    def start_pos(self) -> tuple:
        """Return start position as tuple (line, column)"""
        return self.start_line, self.start_column

    @property
    def end_pos(self) -> tuple:
        """Return end position as tuple (line, column)"""
        return self.end_line, self.end_column

@dataclass
class BaseLocationAstNode:
    """Base class for all AST nodes with common location property"""
    location: LocationRange = field(default_factory=lambda: LocationRange())

    def set_pos_from_node(self, node: ExtractedNode):
        """Set location from tree-sitter node or ExtractedNode

        Extracts location information from tree-sitter nodes or ExtractedNode.
        Tree-sitter uses 0-based column numbers, so we add 1 to convert to 1-based.
        """
        self.location = LocationRange(
            start_line=node.start_line,
            start_column=node.start_column + 1,
            end_line=node.end_line,
            end_column=node.end_column + 1,
        )

@dataclass
class BaseAstNode(BaseLocationAstNode):
    comments: List[CommentInfo] = field(default_factory=list)
    symbol_id: str = ""
    parent_symbol_id: str = ""

@dataclass
class CommentInfo(BaseLocationAstNode):
    raw_comment: str = ""

@dataclass
class MarkedAnnotationInfo(BaseLocationAstNode):
    """Marked Annotation information"""
    name: str = ""
    values: Dict[str, Any] = field(default_factory=dict)
    comments: str = ""

@dataclass
class ImportInfo(BaseAstNode):
    """Import statement information"""
    import_path: str = ""
    is_static: bool = False
    # For exporter: "import java.util.ArrayList;"
    is_wildcard: bool = False


@dataclass
class PackageInfo(BaseAstNode):
    """Package declaration information"""
    name: str = ""


@dataclass
class InterfaceInfo(BaseAstNode):
    """Interface information"""
    interface_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    methods: List['MethodInfo'] = field(default_factory=list)
    fields: List['FieldInfo'] = field(default_factory=list)
    nested_classes: List['ClassInfo'] = field(default_factory=list)
    nested_interfaces: List['InterfaceInfo'] = field(default_factory=list)
    extends_interfaces: List[str] = field(default_factory=list)
    symbol_id: str = ""
    raw_metadata: str = ""

    # 嵌套 Interface 默认是 static
    is_static: bool = False
    # Interface 隐式为 final
    is_final: bool = False
    
    @property
    def type_name(self) -> str:
        """Alias for interface_name for consistency"""
        return self.interface_name
    
    @type_name.setter
    def type_name(self, value: str):
        self.interface_name = value

@dataclass
class EnumInfo(BaseAstNode):
    """Enum information"""
    enum_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    super_interfaces: List[str] = field(default_factory=list)
    enum_constants: List[EnumConstantInfo] = field(default_factory=list)
    methods: List[MethodInfo] = field(default_factory=list)
    fields: List[FieldInfo] = field(default_factory=list)
    constructors: List[ConstructorInfo] = field(default_factory=list)
    code_blocks: List[CodeBlockInfo] = field(default_factory=list)
    nested_classes: List[ClassInfo] = field(default_factory=list)
    nested_interfaces: List[InterfaceInfo] = field(default_factory=list)
    symbol_id: str = ""
    raw_metadata: str = ""

    # 嵌套 Enum 默认是 static
    is_static: bool = False
    # Enum 隐式为 final
    is_final: bool = True
    
    @property
    def type_name(self) -> str:
        """Alias for enum_name for consistency"""
        return self.enum_name
    
    @type_name.setter
    def type_name(self, value: str):
        self.enum_name = value


@dataclass
class RecordInfo(BaseAstNode):
    """Record information"""
    record_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    type_parameters: List[str] = field(default_factory=list)
    super_interfaces: List[str] = field(default_factory=list)
    components: List[ParameterInfo] = field(default_factory=list)
    methods: List[MethodInfo] = field(default_factory=list)
    constructors: List[ConstructorInfo] = field(default_factory=list)
    code_blocks: List[CodeBlockInfo] = field(default_factory=list)
    symbol_id: str = ""
    raw_metadata: str = ""

    # 顶层 Record 不能为 static，但嵌套 Record 默认是 static
    is_static: bool = False
    # Record 隐式为 final
    is_final: bool = True

    @property
    def type_name(self) -> str:
        """Alias for record_name for consistency"""
        return self.record_name
    
    @type_name.setter
    def type_name(self, value: str):
        self.record_name = value


@dataclass
class AnnotationTypeInfo(BaseAstNode):
    """Annotation type information"""
    annotation_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    elements: List[FieldInfo] = field(default_factory=list)
    symbol_id: str = ""
    raw_metadata: str = ""

    # 嵌套 Annotation 默认是 static
    is_static: bool = False
    # Annotation 一定不能设置为 final
    is_final: bool = False

    @property
    def type_name(self) -> str:
        """Alias for annotation_name for consistency"""
        return self.annotation_name
    
    @type_name.setter
    def type_name(self, value: str):
        self.annotation_name = value


@dataclass
class MethodInfo(BaseAstNode):
    """Method information"""
    method_name: str = ""
    return_type: str = ""
    type_parameters: List[str] = field(default_factory=list)
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    parameters: List[ParameterInfo] = field(default_factory=list)
    exceptions: List[str] = field(default_factory=list)
    method_calls: List[Dict[str, Any]] = field(default_factory=list)
    field_accesses: List[Dict[str, Any]] = field(default_factory=list)
    symbol_id: str = ""
    raw_method: str = ""

    mapping_method_types: List[str] = field(default_factory=list)
    base_mapping_uri: str = ""
    full_mapping_uri: str = ""

    is_final: bool = False
    is_static: bool = False


@dataclass
class ConstructorInfo(BaseAstNode):
    """Constructor information"""
    constructor_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    parameters: List[ParameterInfo] = field(default_factory=list)
    exceptions: List[str] = field(default_factory=list)
    method_calls: List[Dict[str, Any]] = field(default_factory=list)
    field_accesses: List[Dict[str, Any]] = field(default_factory=list)
    symbol_id: str = ""
    raw_method: str = ""


@dataclass
class FieldInfo(BaseAstNode):
    """Field information"""
    field_name: str = ""
    field_type: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    has_initial_value: bool = False
    initial_value: Optional[str] = None
    symbol_id: str = ""
    raw_field: str = ""
    is_static: bool = False
    is_final: bool = False


@dataclass
class ParameterInfo(BaseAstNode):
    """Parameter information"""
    parameter_name: str = ""
    parameter_type: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    is_varargs: bool = False
    symbol_id: str = ""
    raw_parameter: str = ""
    
    @property
    def type_name(self) -> str:
        """Alias for parameter_type for consistency"""
        return self.parameter_type
    
    @type_name.setter
    def type_name(self, value: str):
        self.parameter_type = value


@dataclass
class EnumConstantInfo(BaseAstNode):
    """Enum constant information"""
    constant_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    arguments: List[str] = field(default_factory=list)
    raw_constant: str = ""

@dataclass
class CodeBlockInfo(BaseAstNode):
    """block information"""
    raw_method: str = ""
    is_static: bool = False

@dataclass
class ClassInfo(BaseAstNode):
    """Class information"""
    class_name: str = ""
    annotations: List[MarkedAnnotationInfo] = field(default_factory=list)
    methods: List[MethodInfo] = field(default_factory=list)
    fields: List[FieldInfo] = field(default_factory=list)
    constructors: List[ConstructorInfo] = field(default_factory=list)
    code_blocks: List[CodeBlockInfo] = field(default_factory=list)
    nested_classes: List['ClassInfo'] = field(default_factory=list)
    nested_interfaces: List[InterfaceInfo] = field(default_factory=list)
    nested_enums: List[EnumInfo] = field(default_factory=list)
    nested_annotations: List[AnnotationTypeInfo] = field(default_factory=list)
    nested_records: List[RecordInfo] = field(default_factory=list)
    super_class: str = ""
    super_interfaces: List[str] = field(default_factory=list)
    type_parameters: List[str] = field(default_factory=list)
    symbol_id: str = ""
    raw_metadata: str = ""

    is_static: bool = False
    is_final: bool = False

    # extended properties
    has_uri: bool = False
    mapping_uri: str = ""
    
    @property
    def type_name(self) -> str:
        """Alias for class_name for consistency"""
        return self.class_name
    
    @type_name.setter
    def type_name(self, value: str):
        self.class_name = value


@dataclass
class JavaFileStructure(BaseAstNode):
    """Java file structure"""
    file_path: str = ""
    relative_path: str = ""
    package_info: PackageInfo = None
    import_details: List[ImportInfo] = field(default_factory=list)
    classes: List[ClassInfo] = field(default_factory=list)
    interfaces: List[InterfaceInfo] = field(default_factory=list)
    enums: List[EnumInfo] = field(default_factory=list)
    annotations: List[AnnotationTypeInfo] = field(default_factory=list)
    records: List[RecordInfo] = field(default_factory=list)
    file_name: str = ""

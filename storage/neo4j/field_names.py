"""
Neo4j 导出器字段名常量定义
集中管理所有数据类的属性名，避免魔法值散布在代码中
"""

# ============================================================================
# ParameterInfo 字段名
# ============================================================================
PARAM_NAME = "name"
PARAM_TYPE_NAME = "type_name"
PARAM_SYMBOL_ID = "symbol_id"
PARAM_RAW_PARAMETER = "raw_parameter"
PARAM_POSITION = "position"
PARAM_ANNOTATIONS = "annotations"

# ============================================================================
# FieldInfo 字段名
# ============================================================================
FIELD_NAME = "field_name"
FIELD_TYPE_NAME = "field_type"
FIELD_SYMBOL_ID = "symbol_id"
FIELD_RAW_FIELD = "raw_field"
FIELD_POSITION = "position"
FIELD_ANNOTATIONS = "annotations"
FIELD_HAS_DEFAULT_VALUE = "has_default_value"
FIELD_DEFAULT_VALUE = "default_value"

# ============================================================================
# MethodInfo 字段名
# ============================================================================
METHOD_NAME = "method_name"
METHOD_RETURN_TYPE = "return_type"
METHOD_SYMBOL_ID = "symbol_id"
METHOD_RAW_SIGNATURE = "raw_signature"
METHOD_RAW_METHOD = "raw_method"
METHOD_POSITION = "position"
METHOD_PARAMETERS = "parameters"
METHOD_ANNOTATIONS = "annotations"
METHOD_THROWS_EXCEPTIONS = "throws_exceptions"
METHOD_MAPPING_URI = "mapping_uri"
METHOD_MAPPING_METHOD_TYPE = "mapping_method_type"

# ============================================================================
# ConstructorInfo 字段名
# ============================================================================
CONSTRUCTOR_NAME = "constructor_name"
CONSTRUCTOR_SYMBOL_ID = "symbol_id"
CONSTRUCTOR_RAW_SIGNATURE = "raw_signature"
CONSTRUCTOR_RAW_METHOD = "raw_method"
CONSTRUCTOR_POSITION = "position"
CONSTRUCTOR_PARAMETERS = "parameters"
CONSTRUCTOR_ANNOTATIONS = "annotations"
CONSTRUCTOR_THROWS_EXCEPTIONS = "throws_exceptions"

# ============================================================================
# TypeDeclarationInfo 字段名（ClassInfo, InterfaceInfo, EnumInfo 等）
# ============================================================================
TYPE_NAME = "type_name"
TYPE_SYMBOL_ID = "symbol_id"
TYPE_RAW_METADATA = "raw_metadata"
TYPE_POSITION = "position"
TYPE_ANNOTATIONS = "annotations"

# ============================================================================
# ClassInfo 特有字段名
# ============================================================================
CLASS_FIELDS = "fields"
CLASS_METHODS = "methods"
CLASS_STATIC_METHODS = "static_methods"
CLASS_CONSTRUCTORS = "constructors"
CLASS_STATIC_BLOCKS = "static_blocks"
CLASS_INSTANCE_BLOCKS = "instance_blocks"
CLASS_MAPPING_URI = "mapping_uri"
CLASS_NESTED_CLASSES = "nested_classes"
CLASS_NESTED_INTERFACES = "nested_interfaces"
CLASS_NESTED_ENUMS = "nested_enums"
CLASS_NESTED_RECORDS = "nested_records"
CLASS_NESTED_ANNOTATIONS = "nested_annotations"

# ============================================================================
# InterfaceInfo 特有字段名
# ============================================================================
INTERFACE_CONSTANTS = "constants"
INTERFACE_METHODS = "methods"
INTERFACE_NESTED_CLASSES = "nested_classes"
INTERFACE_NESTED_INTERFACES = "nested_interfaces"
INTERFACE_NESTED_ENUMS = "nested_enums"
INTERFACE_NESTED_RECORDS = "nested_records"
INTERFACE_NESTED_ANNOTATIONS = "nested_annotations"

# ============================================================================
# EnumInfo 特有字段名
# ============================================================================
ENUM_CONSTANTS = "enum_constants"
ENUM_FIELDS = "fields"
ENUM_METHODS = "methods"
ENUM_CONSTRUCTORS = "constructors"
ENUM_STATIC_BLOCKS = "static_blocks"
ENUM_INSTANCE_BLOCKS = "instance_blocks"
ENUM_NESTED_CLASSES = "nested_classes"
ENUM_NESTED_INTERFACES = "nested_interfaces"
ENUM_NESTED_ENUMS = "nested_enums"

# ============================================================================
# RecordInfo 特有字段名
# ============================================================================
RECORD_COMPONENTS = "components"
RECORD_METHODS = "methods"
RECORD_CONSTRUCTORS = "constructors"
RECORD_STATIC_BLOCKS = "static_blocks"
RECORD_NESTED_CLASSES = "nested_classes"
RECORD_NESTED_INTERFACES = "nested_interfaces"
RECORD_NESTED_ENUMS = "nested_enums"
RECORD_NESTED_RECORDS = "nested_records"

# ============================================================================
# AnnotationTypeInfo 特有字段名
# ============================================================================
ANNOTATION_ELEMENTS = "elements"
ANNOTATION_CONSTANTS = "constants"
ANNOTATION_NESTED_ENUMS = "nested_enums"
ANNOTATION_NESTED_ANNOTATIONS = "nested_annotations"

# ============================================================================
# JavaFileStructure 字段名
# ============================================================================
FILE_NAME = "file_name"
FILE_PATH = "file_path"
FILE_PACKAGE_INFO = "package_info"
FILE_IMPORT_DETAILS = "import_details"
FILE_CLASS_DETAILS = "class_details"
FILE_INTERFACE_DETAILS = "interface_details"
FILE_ENUM_DETAILS = "enum_details"
FILE_RECORD_DETAILS = "record_details"
FILE_ANNOTATION_DETAILS = "annotation_details"
FILE_COMMENTS = "comments"
FILE_POSITION = "position"

# ============================================================================
# PackageInfo 字段名
# ============================================================================
PACKAGE_NAME = "package_name"
PACKAGE_POSITION = "position"

# ============================================================================
# ImportInfo 字段名
# ============================================================================
IMPORT_RAW_IMPORT = "raw_import"
IMPORT_TYPE_NAME = "type_name"
IMPORT_QUALIFIED_NAME = "qualified_name"
IMPORT_IS_STATIC = "is_static"
IMPORT_IS_WILDCARD = "is_wildcard"
IMPORT_POSITION = "position"

# ============================================================================
# LocationRange 字段名
# ============================================================================
LOCATION_START_POS = "start_pos"
LOCATION_END_POS = "end_pos"

# ============================================================================
# Neo4j 节点属性名
# ============================================================================
NODE_SYMBOL_ID = "symbol_id"
NODE_NAME = "name"
NODE_QUALIFIED_NAME = "qualified_name"
NODE_TYPE_NAME = "type_name"
NODE_PARENT_SYMBOL_ID = "parent_symbol_id"
NODE_BELONG_PROJECT = "belong_project"
NODE_START_LINE = "start_line"
NODE_START_COLUMN = "start_column"
NODE_END_LINE = "end_line"
NODE_END_COLUMN = "end_column"
NODE_FILE_PATH = "file_path"
NODE_PACKAGE_NAME = "package_name"
NODE_IMPORTS = "imports"
NODE_RAW_METADATA = "raw_metadata"
NODE_OBJECT_TYPE = "object_type"
NODE_RETURN_TYPE = "return_type"
NODE_MAPPING_URI = "mapping_uri"
NODE_MAPPING_METHOD_TYPE = "mapping_method_type"
NODE_RAW_SIGNATURE = "raw_signature"
NODE_RAW_METHOD_BODY = "raw_method_body"
NODE_IS_STATIC = "is_static"
NODE_FROM_TYPE = "from_type"
NODE_ANNOTATION_FROM_OBJECT = "annotation_from_object"
NODE_FILE_TYPE = "file_type"
NODE_ANNOTATIONS = "annotations"
NODE_THROWS_EXCEPTIONS = "throws_exceptions"
NODE_HAS_DEFAULT_VALUE = "has_default_value"
NODE_DEFAULT_VALUE = "default_value"

# ============================================================================
# SymbolEdge 字段名（符号表中的边）
# ============================================================================
EDGE_SOURCE_SYMBOL = "source_symbol"
EDGE_TARGET_SYMBOL = "target_symbol"
EDGE_TYPE = "edge_type"
EDGE_LOCATION = "location"

# ============================================================================
# InheritanceEdge 字段名
# ============================================================================
INHERITANCE_IS_IMPLEMENTATION = "is_implementation"
INHERITANCE_IS_EXTENSION = "is_extension"

# ============================================================================
# CallEdge 字段名
# ============================================================================
CALL_SITE = "call_site"
CALL_IS_VIRTUAL = "is_virtual_call"
CALL_IS_STATIC = "is_static_call"
CALL_ACTUAL_ARG_TYPES = "actual_arg_types"
CALL_RESOLVED_TARGET = "resolved_target"

# ============================================================================
# AccessEdge 字段名
# ============================================================================
ACCESS_IS_WRITE = "is_write"
ACCESS_IS_READ = "is_read"


from dataclasses import dataclass, field
from enum import Enum
from typing import List, Dict, Any


class FileType(Enum):
    
    JAVA_FILE = "javaFile"

class CommentType(Enum):
    """注释类型枚举"""
    JAVADOC = "javadoc"
    LONG_COMMENT = "long_comment"
    BLOCK_COMMENT = "block_comment"
    LINE_COMMENT = "line_comment"

class CommentStorageDecision(Enum):
    """注释存储决策"""
    STORE_AS_ATTRIBUTE = "store_as_attribute"  # 存为节点属性
    CREATE_JAVADOC_NODE = "create_javadoc_node"  # 创建Javadoc节点
    CREATE_LONG_COMMENT_NODE = "create_long_comment_node"  # 创建长注释节点

class ObjectType(Enum):
    
    CLASS_TYPE = "classType"
    INTERFACE_TYPE = "interfaceType"
    ENUM_TYPE = "enumType"
    ANNOTATION_TYPE = "annotationType"
    RECORD_TYPE = "recordType"

class ObjectFromType(Enum):
    
    INNER_DEFINITION = "InnerDefinition"          # 文件内顶层定义
    NESTED_DEFINITION = "NestedDefinition"        # 嵌套定义（类内部的类/接口/枚举等）
    EXTERNAL_DEFINITION = "ExternalDefinition"    # 外部引用
    UNKNOWN_DEFINITION = "UnknownDefinition"      # 未知引用

class JavaGraphEdgeType(Enum):
    
    # 一个项目节点有多少个文件节
    HAVE = "HAVE"
    # 一个文件节点有多少个对象定义节
    CONTAINS = "CONTAINS"
    # 一个对象节点有多少个属性(静非静态)、方法(静态与非静态)、代码块(实静态)、枚举常量、记录组件
    MEMBER_OF = "MEMBER_OF"
    # 类继承关
    EXTENDS = "EXTENDS"
    # 接口实现关系
    IMPLEMENTS = "IMPLEMENTS"
    # 方法调用关系
    CALLS = "CALLS"
    # 字段访问关系
    ACCESSES = "ACCESSES"
    # 注释关系
    HAS_COMMENT = "HAS_COMMENT"
    
    def get_unique_key(self) -> List[str]:
        
        # 所有关系的唯一键都 source + target + relationship_type
        return ["source", "target"]
    
    def get_properties(self) -> List[str]:
        
        # 默认关系没有额外属性,可以在子类中覆盖
        return []

class JavaNeo4jNodeType(Enum):
    Project = "Project"
    File = "File"
    JavaObject = "JavaObject"
    JavaMethod = "JavaMethod"
    JavaCodeBlock = "JavaCodeBlock"
    JavaField = "JavaField"
    JavaMethodParameter = "JavaMethodParameter"
    JavaEnumConstant = "JavaEnumConstant"
    JavaRecordComponent = "JavaRecordComponent"
    Comment = "Comment"

class RelationshipProperty:
    """关系属性基类"""
    
    def __init__(self):
        self.created_at: str = None  # 创建时间
        self.updated_at: str = None  # 更新时间
        self.count: int = 1  # 调用次数或访问次数
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "created_at": self.created_at,
            "updated_at": self.updated_at,
            "count": self.count
        }

class CallRelationshipProperty(RelationshipProperty):
    """方法调用关系属性"""
    
    def __init__(self):
        super().__init__()
        self.call_count: int = 1  # 调用次数
        self.last_call_line: int = None  # 最后一次调用的行号
    
    def to_dict(self) -> Dict[str, Any]:
        result = super().to_dict()
        result.update({
            "call_count": self.call_count,
            "last_call_line": self.last_call_line
        })
        return result

class AccessRelationshipProperty(RelationshipProperty):
    """字段访问关系属性"""
    
    def __init__(self):
        super().__init__()
        self.access_count: int = 1  # 访问次数
        self.access_type: str = "READ"  # READ, WRITE, READ_WRITE
        self.last_access_line: int = None  # 最后一次访问的行号
    
    def to_dict(self) -> Dict[str, Any]:
        result = super().to_dict()
        result.update({
            "access_count": self.access_count,
            "access_type": self.access_type,
            "last_access_line": self.last_access_line
        })
        return result

class BaseNode:
    name: str = ""
    qualified_name: str = ""
    symbol_id: str = ""
    parent_symbol_id: str
    belong_project: str  # 节点所属的项目名称
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        raise NotImplementedError("子类必须实现 get_unique_key 方法")

class BaseLocationNode(BaseNode):
    start_line: int
    start_column: int
    end_line: int
    end_column: int

class ProjectGraphNode(BaseNode):
    pass

class FileNodeGraphNode(BaseLocationNode):
    full_path: str
    file_path: str
    file_type: FileType

class ObjectNodeGraphNode(BaseLocationNode):
    object_type: str
    raw_metadata: str

class MethodNodeGraphNode(BaseLocationNode):
    raw_signature: str
    raw_metadata: str

class CodeBlockNodeGraphNode(BaseLocationNode):
    raw_metadata: str

class FieldNodeGraphNode(BaseLocationNode):
    raw_metadata: str

class JavaFileNodeGraphNode(FileNodeGraphNode):
    imports: List[str]
    package_name: str
    file_type: str = FileType.JAVA_FILE  # 文件类型
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class JavaObjectNodeGraphNode(ObjectNodeGraphNode):
    from_type: str
    request_uri: str
    annotation_from_object: List[str]
    annotations: List[str] = None  # 注解列表
    type_parameters: List[str] = None
    simple_comment: str = ""  # 简短注释（直接存储）
    has_detailed_comment: bool = False  # 是否有详细注释节点

    super_class: str
    super_interfaces: list[str] = field(default_factory=list)
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class JavaMethodNodeGraphNode(MethodNodeGraphNode):
    is_static: bool
    is_constructor: bool = False
    return_type: str = "void"  # 返回类型
    base_uri: str = ""  # REST 映射路径
    full_uri: str = ""  # REST 全映射路径
    mapping_method_type: str = ""  # HTTP 方法类型
    type_parameters: List[str] = None  # 泛型
    annotations: List[str] = None  # 注解列表
    throws_exceptions: List[str] = None  # 异常列表
    simple_comment: str = ""  # 简短注释（直接存储）
    has_detailed_comment: bool = False  # 是否有详细注释节点
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }


class JavaCodeBlockNodeGraphNode(CodeBlockNodeGraphNode):
    is_static: bool

    def get_unique_key(self) -> Dict[str, Any]:
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class JavaFieldNodeGraphNode(FieldNodeGraphNode):
    is_static: bool
    is_final: bool
    type_name: str = "Object"  # 字段类型
    annotations: List[str] = None  # 注解列表
    has_default_value: bool = False  # 是否有默认值
    default_value: str = ""  # 默认值
    simple_comment: str = ""  # 简短注释（直接存储）
    has_detailed_comment: bool = False  # 是否有详细注释节点
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }


class JavaEnumConstantNodeGraphNode(FieldNodeGraphNode):
    annotations: List[str] = None  # 注解列表
    arguments: List[str] = None

    def get_unique_key(self) -> Dict[str, Any]:
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class JavaParameterNodeGraphNode(BaseLocationNode):
    """参数节点"""
    type_name: str
    annotations: List[str] = None  # 注解列表
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class ProjectNode(ProjectGraphNode):
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id
        }

class ParameterNode(BaseLocationNode):
    
    type_name: str
    annotations: List[str] = None  # 注解列表
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class ConstructorNode(BaseLocationNode):
    """构造函数节点"""
    raw_signature: str
    annotations: List[str] = None  # 注解列表
    throws_exceptions: List[str] = None  # 异常列表
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }

class CommentNodeGraphNode(BaseLocationNode):
    """注释节点"""
    content: str = ""  # 注释内容
    comment_type: str = ""  # 注释类型: CommentType枚举值
    char_count: int = 0  # 字符数
    line_count: int = 0  # 行数
    
    # Javadoc特有字段
    javadoc_summary: str = ""  # Javadoc摘要
    javadoc_params: List[str] = None  # @param列表
    javadoc_return: str = ""  # @return
    javadoc_throws: List[str] = None  # @throws列表
    javadoc_author: str = ""  # @author
    javadoc_version: str = ""  # @version
    javadoc_since: str = ""  # @since
    javadoc_deprecated: str = ""  # @deprecated
    javadoc_see: List[str] = None  # @see列表
    
    def get_unique_key(self) -> Dict[str, Any]:
        
        return {
            "symbol_id": self.symbol_id,
            "belong_project": self.belong_project
        }


@dataclass
class JavadocParseResult:
    """Javadoc解析结果"""
    summary: str = ""
    params: List[str] = field(default_factory=list)
    return_desc: str = ""
    throws: List[str] = field(default_factory=list)
    author: str = ""
    version: str = ""
    since: str = ""
    deprecated: str = ""
    see: List[str] = field(default_factory=list)
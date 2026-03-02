
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional

from parser.languages.java.core.ast_node_types import LocationRange


class SymbolType(Enum):
    
    PROJECT = "project"           # 项目(所有JAVAFILE的根节点
    JAVAFILE = "javafile"         # Java 文件
    PACKAGE = "package"           # 
    IMPORT = "import"             # 导入
    CLASS = "class"               # 
    INTERFACE = "interface"       # 接口
    ENUM = "enum"                 # 枚举
    RECORD = "record"             # 记录
    ANNOTATION = "annotation"     # 注解定义
    METHOD = "method"             # 方法
    CONSTRUCTOR = "constructor"   # 构造器
    FIELD = "field"               # 字段
    PARAMETER = "parameter"       # 参数
    LOCAL_VARIABLE = "local_var"  # 局部变
    TYPE_PARAMETER = "type_param" # 类型参数(泛型)

@dataclass
class Symbol:
    
    symbol_id: str                           # 唯一标识
    symbol_type: SymbolType                  # 符号类型
    name: str                                # 短名称("findById"
    qualified_name: str                      # 全限定名(如 "com.example.UserService.findById"
    declaring_symbol: Optional[str] = None   # 所属父符号ID(如方法所属的类)
    location: LocationRange = field(default_factory=LocationRange)
    file_path: str = ""                      # 定义文件路径
    
    # 类型信息(对于变量、字段、方法返回等
    type_name: str = ""                      # 类型名称(如 "String", "List<User>"
    type_symbol: Optional[str] = None        # 类型对应的符号ID(用于链接到类定义)
    
    # 修饰符信
    is_public: bool = False
    is_private: bool = False
    is_protected: bool = False
    is_static: bool = False
    is_final: bool = False
    is_abstract: bool = False
    
    # 额外元数
    metadata: Dict = field(default_factory=dict)
    
    # 初始化相关属性(仅对 FIELD 节点有效
    initialization_status: str = "uninitialized"  # "uninitialized" | "initialized_in_constructor" | "lazy_initialized" | "externally_initialized" | "initialized_in_declaration"
    initialization_methods: list = field(default_factory=list)  # 初始化该字段的方法列
    is_required_before_use: bool = False  # 使用前必须初始化
    default_value: str = ""  # 默认

@dataclass
class TypeInfo:
    
    raw_type: str                            # 原始类型名("List"
    full_type: str                           # 完整类型(如 "List<User>"
    type_arguments: List[str] = field(default_factory=list)  # 泛型参数类型
    type_argument_symbols: List[str] = field(default_factory=list)  # 泛型参数符号ID
    is_primitive: bool = False
    is_array: bool = False
    array_dimensions: int = 0
    is_generic: bool = False                 # 是否是泛型类
    
    # 类型边界(用于泛型参数)
    lower_bounds: List[str] = field(default_factory=list)
    upper_bounds: List[str] = field(default_factory=list)

@dataclass
class SymbolEdge:
    """符号关系边基"""
    source_symbol: str                       # 源符号ID
    target_symbol: str                       # 目标符号ID
    edge_type: str                           # 关系类型
    location: LocationRange = field(default_factory=LocationRange)

@dataclass
class InheritanceEdge(SymbolEdge):
    """继承/实现关系"""
    is_implementation: bool = False          # 是否是实现(接口
    is_extension: bool = False               # 是否是扩展(类继承)

@dataclass
class CallEdge(SymbolEdge):
    """方法调用"""
    call_site: LocationRange = field(default_factory=LocationRange)
    is_virtual_call: bool = True             # 是否是虚方法调用(多态)
    is_static_call: bool = False             # 是否是静态方法调
    actual_arg_types: List[str] = field(default_factory=list)  # 实际参数类型
    resolved_target: Optional[str] = None    # 实际解析到的目标(考虑多态)

@dataclass
class AccessEdge(SymbolEdge):
    """字段访问"""
    is_write: bool = False                   # 是否写入
    is_read: bool = True                     # 是否读取

@dataclass
class MembershipEdge(SymbolEdge):
    
    pass

@dataclass
class TypeEdge(SymbolEdge):
    """类型关系边(字段/参数/返回值的类型指向类型定义"""
    pass

@dataclass
class InitializationEdge(SymbolEdge):
    """初始化关系边(方法初始化字段"""
    initialization_type: str = "direct"  # "direct" | "indirect" | "conditional"
    is_guaranteed: bool = False  # 是否保证初始
    line_number: int = 0  # 初始化的行号
    condition: str = ""  # 初始化条

@dataclass
class DependencyEdge(SymbolEdge):
    """初始化依赖关系边(方法依赖字段初始化"""
    required_before_call: bool = True  # 使用前必须初始化
    initialization_methods: list = None  # 必须先调用的初始化方
    
    def __post_init__(self):
        if self.initialization_methods is None:
            self.initialization_methods = []

@dataclass
class SymbolTable:
    
    # 符号存储: symbol_id -> Symbol
    symbols: Dict[str, Symbol] = field(default_factory=dict)
    
    # 名称索引: qualified_name -> symbol_id(可能有多个重载
    name_index: Dict[str, List[str]] = field(default_factory=dict)
    
    # 文件索引: file_path -> List[symbol_id]
    file_index: Dict[str, List[str]] = field(default_factory=dict)
    
    # 类型索引: type_name -> List[symbol_id](该类型的所有符号)
    type_index: Dict[str, List[str]] = field(default_factory=dict)
    
    # 关系
    inheritance_edges: List[InheritanceEdge] = field(default_factory=list)
    call_edges: List[CallEdge] = field(default_factory=list)
    access_edges: List[AccessEdge] = field(default_factory=list)
    membership_edges: List[MembershipEdge] = field(default_factory=list)
    type_edges: List[TypeEdge] = field(default_factory=list)
    initialization_edges: List[InitializationEdge] = field(default_factory=list)
    dependency_edges: List[DependencyEdge] = field(default_factory=list)
    
    # 父子关系: parent_symbol_id -> List[child_symbol_id]
    parent_child_map: Dict[str, List[str]] = field(default_factory=dict)
    
    # 字段类型映射: class_id -> {field_name -> type_name}
    field_types: Dict[str, Dict[str, str]] = field(default_factory=dict)
    
    def register_symbol(self, symbol: Symbol) -> str:
        
        # 检查重
        if symbol.symbol_id in self.symbols:
            # 可以处理重载或报
            pass
        
        # 存储符号
        self.symbols[symbol.symbol_id] = symbol
        
        # 更新名称索引
        if symbol.qualified_name not in self.name_index:
            self.name_index[symbol.qualified_name] = []
        self.name_index[symbol.qualified_name].append(symbol.symbol_id)
        
        # 更新文件索引
        if symbol.file_path not in self.file_index:
            self.file_index[symbol.file_path] = []
        self.file_index[symbol.file_path].append(symbol.symbol_id)
        
        # 更新类型索引
        if symbol.type_name:
            if symbol.type_name not in self.type_index:
                self.type_index[symbol.type_name] = []
            self.type_index[symbol.type_name].append(symbol.symbol_id)
        
        # 更新父子关系
        if symbol.declaring_symbol:
            if symbol.declaring_symbol not in self.parent_child_map:
                self.parent_child_map[symbol.declaring_symbol] = []
            self.parent_child_map[symbol.declaring_symbol].append(symbol.symbol_id)
        
        return symbol.symbol_id
    
    def lookup_by_id(self, symbol_id: str) -> Optional[Symbol]:
        
        return self.symbols.get(symbol_id)
    
    def lookup_by_name(self, qualified_name: str) -> List[Symbol]:
        
        symbol_ids = self.name_index.get(qualified_name, [])
        return [self.symbols[sid] for sid in symbol_ids if sid in self.symbols]
    
    def lookup_by_file(self, file_path: str) -> List[Symbol]:
        """查找文件中的所有符"""
        symbol_ids = self.file_index.get(file_path, [])
        return [self.symbols[sid] for sid in symbol_ids if sid in self.symbols]
    
    def lookup_children(self, parent_symbol_id: str) -> List[Symbol]:
        
        child_ids = self.parent_child_map.get(parent_symbol_id, [])
        return [self.symbols[sid] for sid in child_ids if sid in self.symbols]
    
    def add_inheritance_edge(self, edge: InheritanceEdge):
        """添加继承关系"""
        self.inheritance_edges.append(edge)
    
    def add_call_edge(self, edge: CallEdge):
        """添加调用关系"""
        self.call_edges.append(edge)
    
    def add_access_edge(self, edge: AccessEdge):
        """添加访问关系"""
        self.access_edges.append(edge)
    
    def add_membership_edge(self, edge: MembershipEdge):
        """添加成员属于关系"""
        self.membership_edges.append(edge)
    
    def add_type_edge(self, edge: TypeEdge):
        """添加类型关系"""
        self.type_edges.append(edge)
    
    def add_initialization_edge(self, edge: InitializationEdge):
        
        self.initialization_edges.append(edge)
    
    def add_dependency_edge(self, edge: DependencyEdge):
        
        self.dependency_edges.append(edge)
    
    def get_inheritance_graph(self) -> Dict[str, List[str]]:
        
        graph = {}
        for edge in self.inheritance_edges:
            if edge.source_symbol not in graph:
                graph[edge.source_symbol] = []
            graph[edge.source_symbol].append(edge.target_symbol)
        return graph
    
    def get_class_hierarchy(self, symbol_id: str) -> Dict:
        
        symbol = self.lookup_by_id(symbol_id)
        if not symbol:
            return {}
        
        return {
            "symbol": symbol,
            "extends": symbol.metadata.get('extends'),
            "implements": symbol.metadata.get('implements', []),
            "parents": self.get_inheritance_graph().get(symbol_id, [])
        }
    
    def get_call_graph(self) -> Dict[str, List[CallEdge]]:
        
        graph = {}
        for edge in self.call_edges:
            if edge.source_symbol not in graph:
                graph[edge.source_symbol] = []
            graph[edge.source_symbol].append(edge)
        return graph
    
    def get_statistics(self) -> Dict:
        """获取符号表统计信"""
        return {
            "total_symbols": len(self.symbols),
            "by_type": {
                symbol_type.value: len([s for s in self.symbols.values() if s.symbol_type == symbol_type])
                for symbol_type in SymbolType
            },
            "inheritance_edges": len(self.inheritance_edges),
            "call_edges": len(self.call_edges),
            "access_edges": len(self.access_edges),
            "membership_edges": len(self.membership_edges),
            "type_edges": len(self.type_edges),
            "initialization_edges": len(self.initialization_edges),
            "dependency_edges": len(self.dependency_edges),
            "files": len(self.file_index)
        }

class SymbolIdGenerator:
    
    
    @staticmethod
    def for_class(file_path: str, class_name: str) -> str:
        
        return f"{file_path}#{class_name}"
    
    @staticmethod
    def for_method(class_symbol_id: str, method_name: str, param_types: List[str] = None) -> str:
        
        base = f"{class_symbol_id}#{method_name}"
        
        if param_types is not None:
            params = ",".join(param_types)
            return f"{base}({params})"
        return f"{base}()"
    
    @staticmethod
    def for_field(class_symbol_id: str, field_name: str) -> str:
        
        return f"{class_symbol_id}#{field_name}"
    
    @staticmethod
    def for_constructor(class_symbol_id: str, param_types: List[str] = None) -> str:
        
        base = f"{class_symbol_id}#<init>"
        
        if param_types is not None:
            params = ",".join(param_types)
            return f"{base}({params})"
        return f"{base}()"
    
    @staticmethod
    def for_parameter(method_symbol_id: str, param_name: str, index: int) -> str:
        
        return f"{method_symbol_id}#${param_name}#{index}"
    
    @staticmethod
    def for_import(file_path: str, import_name: str) -> str:
        
        return f"{file_path}#import:{import_name}"
    
    @staticmethod
    def for_local_var(method_symbol_id: str, var_name: str, line: int) -> str:
        
        return f"{method_symbol_id}#{var_name}@{line}"
    
    @staticmethod
    def for_nested_class(parent_class_symbol_id: str, nested_class_name: str) -> str:
        
        return f"{parent_class_symbol_id}#{nested_class_name}"
    
    @staticmethod
    def for_static_field(class_symbol_id: str, field_name: str) -> str:
        
        return f"{class_symbol_id}#${field_name}"
    
    @staticmethod
    def for_static_method(class_symbol_id: str, method_name: str, param_types: List[str] = None) -> str:
        base = f"{class_symbol_id}#${method_name}"
        
        if param_types is not None:
            params = ",".join(param_types)
            return f"{base}({params})"
        return f"{base}()"
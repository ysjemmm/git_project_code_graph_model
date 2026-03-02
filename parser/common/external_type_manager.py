
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional, Set

class ExternalTypeSource(Enum):
    
    JAVA_STDLIB = "java_stdlib"      # Java 标准
    JAVA_EXT = "java_ext"            # Java 扩展
    THIRD_PARTY = "third_party"      # 三方
    UNKNOWN = "unknown"              # 未知来源

@dataclass
class ExternalType:
    
    type_name: str                   # 类型名("Serializable"
    qualified_name: str              # 全限定名(如 "java.io.Serializable"
    source: ExternalTypeSource       # 来源
    package_name: str                # 包名
    
    # 类型信息
    is_interface: bool = False       # 是否是接
    is_class: bool = True            # 是否是类
    is_enum: bool = False            # 是否是枚
    is_annotation: bool = False      # 是否是注
    
    # 继承信息
    parent_types: List[str] = field(default_factory=list)  # 父类型全限定
    implemented_interfaces: List[str] = field(default_factory=list)  # 实现的接
    
    # 泛型信息
    type_parameters: List[str] = field(default_factory=list)  # 类型参数
    
    # 元数
    description: str = ""            # 描述
    metadata: Dict = field(default_factory=dict)

@dataclass
class ExternalTypeReference:
    
    source_symbol_id: str            # 源符号ID(本地代码中的类
    target_type_name: str            # 目标类型
    target_qualified_name: str       # 目标全限定名
    reference_type: str              # 引用类型(extends, implements, uses
    location_line: int = 0           # 引用位置(行号)

class ExternalTypeManager:
    """外部类型管理"""
    
    def __init__(self):
        # 外部类型 qualified_name -> ExternalType
        self.external_types: Dict[str, ExternalType] = {}
        
        # 外部类型引用: source_symbol_id -> List[ExternalTypeReference]
        self.external_references: Dict[str, List[ExternalTypeReference]] = {}
        
        # 类型名到全限定名的映射(用于快速查找)
        self.type_name_to_qualified: Dict[str, Set[str]] = {}
        
        # 初始化标准库类型
        self._init_java_stdlib()
    
    def _init_java_stdlib(self):
        """初始Java 标准库类"""
        stdlib_types = [
            # java.lang
            ("Object", "java.lang.Object", True, False),
            ("String", "java.lang.String", True, False),
            ("Exception", "java.lang.Exception", True, False),
            ("RuntimeException", "java.lang.RuntimeException", True, False),
            ("Throwable", "java.lang.Throwable", True, False),
            ("Class", "java.lang.Class", True, False),
            ("Enum", "java.lang.Enum", True, False),
            ("Record", "java.lang.Record", True, False),
            
            # java.io
            ("Serializable", "java.io.Serializable", False, True),
            ("IOException", "java.io.IOException", True, False),
            ("File", "java.io.File", True, False),
            
            # java.util
            ("List", "java.util.List", False, True),
            ("Map", "java.util.Map", False, True),
            ("Set", "java.util.Set", False, True),
            ("Collection", "java.util.Collection", False, True),
            ("ArrayList", "java.util.ArrayList", True, False),
            ("HashMap", "java.util.HashMap", True, False),
            ("HashSet", "java.util.HashSet", True, False),
            ("Iterator", "java.util.Iterator", False, True),
            
            # java.lang.annotation
            ("Annotation", "java.lang.annotation.Annotation", False, True),
            ("Retention", "java.lang.annotation.Retention", False, True),
            ("Target", "java.lang.annotation.Target", False, True),
            
            # java.util.function
            ("Function", "java.util.function.Function", False, True),
            ("Consumer", "java.util.function.Consumer", False, True),
            ("Supplier", "java.util.function.Supplier", False, True),
            ("Predicate", "java.util.function.Predicate", False, True),
            
            # java.lang (Comparable)
            ("Comparable", "java.lang.Comparable", False, True),
            ("Comparator", "java.util.Comparator", False, True),
            
            # java.util.stream
            ("Stream", "java.util.stream.Stream", False, True),
            ("Collector", "java.util.stream.Collector", False, True),
        ]
        
        for type_name, qualified_name, is_class, is_interface in stdlib_types:
            external_type = ExternalType(
                type_name=type_name,
                qualified_name=qualified_name,
                source=ExternalTypeSource.JAVA_STDLIB,
                package_name=qualified_name.rsplit(".", 1)[0],
                is_class=is_class,
                is_interface=is_interface
            )
            self.register_external_type(external_type)
    
    def register_external_type(self, external_type: ExternalType):
        
        self.external_types[external_type.qualified_name] = external_type
        
        # 更新类型名映
        if external_type.type_name not in self.type_name_to_qualified:
            self.type_name_to_qualified[external_type.type_name] = set()
        self.type_name_to_qualified[external_type.type_name].add(external_type.qualified_name)
    
    def add_external_reference(self, reference: ExternalTypeReference):
        
        if reference.source_symbol_id not in self.external_references:
            self.external_references[reference.source_symbol_id] = []
        self.external_references[reference.source_symbol_id].append(reference)
    
    def get_external_type(self, qualified_name: str) -> Optional[ExternalType]:
        
        return self.external_types.get(qualified_name)
    
    def find_external_type_by_name(self, type_name: str) -> List[ExternalType]:
        """通过类型名查找外部类型(可能返回多个)"""
        qualified_names = self.type_name_to_qualified.get(type_name, set())
        return [self.external_types[qn] for qn in qualified_names if qn in self.external_types]
    
    def get_external_references(self, source_symbol_id: str) -> List[ExternalTypeReference]:
        """获取符号的所有外部类型引用"""
        return self.external_references.get(source_symbol_id, [])
    
    def resolve_type_to_external(self, type_name: str, package_context: str = "") -> Optional[ExternalType]:
        """
        解析类型名到外部类型
        
        Args:
            type_name: 类型名("Serializable"
            package_context: 包上下文(用于导入解析)
        
        Returns:
            外部类型信息,如果不是外部类型则返回 None
        """
        # 1. 如果已经是全限定名,直接查找
        if "." in type_name:
            return self.get_external_type(type_name)
        
        # 2. 通过类型名查
        candidates = self.find_external_type_by_name(type_name)
        if len(candidates) == 1:
            return candidates[0]
        elif len(candidates) > 1:
            # 多个候选,返回第一个(通常是标准库
            return candidates[0]
        
        return None
    
    def get_statistics(self) -> Dict:
        
        return {
            "total_external_types": len(self.external_types),
            "java_stdlib_types": sum(1 for t in self.external_types.values() 
                                     if t.source == ExternalTypeSource.JAVA_STDLIB),
            "third_party_types": sum(1 for t in self.external_types.values() 
                                     if t.source == ExternalTypeSource.THIRD_PARTY),
            "total_references": sum(len(refs) for refs in self.external_references.values()),
        }

# 全局外部类型管理器实例
_global_external_type_manager: Optional[ExternalTypeManager] = None

def get_external_type_manager() -> ExternalTypeManager:
    """获取全局外部类型管理"""
    global _global_external_type_manager
    if _global_external_type_manager is None:
        _global_external_type_manager = ExternalTypeManager()
    return _global_external_type_manager

def reset_external_type_manager():
    global _global_external_type_manager
    _global_external_type_manager = None
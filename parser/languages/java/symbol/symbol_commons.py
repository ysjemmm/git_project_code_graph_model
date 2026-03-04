""" 符号表通用类 """
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Set

from parser.languages.java.utils.package_trie import LazyPackageTrie


@dataclass
class ProjectSymbolTable:
    project_name: str
    # com.ysj.Car -> Symbol
    fqn_class_index: Dict[str, JavaSymbol] = field(default_factory=dict)
    # Car -> [com.ysj.Car, com.yyy.Car]
    simple_class_name_index: Dict[str, Set[str]] = field(
        default_factory=lambda: defaultdict(set)
    )
    # com.ysj -> [Car]
    package_class_index: Dict[str, Set[str]] = field(
        default_factory=lambda: defaultdict(set)
    )
    # 前缀树，用于处理通配符情况【import com.ysj.*], 但它是按需构建，懒加载
    package_trie: LazyPackageTrie = field(default=None)

    def __post_init__(self):
        """初始化后自动创建前缀树"""
        if self.package_trie is None:
            # 提供回调函数，从 fqn_class_index 动态加载
            self.package_trie = LazyPackageTrie(
                symbol_loader=lambda fqn: self.fqn_class_index.get(fqn)
            )

    def to_dict(self):
        """转换为可序列化的字典"""
        result = {
            'project_name': self.project_name,
            'fqn_class_index': {
                fqn: symbol.to_dict() for fqn, symbol in self.fqn_class_index.items()
            },
            'simple_class_name_index': {
                name: list(fqns) for name, fqns in self.simple_class_name_index.items()
            },
            'package_class_index': {
                package: list(classes) for package, classes in self.package_class_index.items()
            }
        }
        
        # 如果前缀树已构建，添加其序列化数据
        if self.package_trie and self.package_trie.is_built():
            result['package_trie'] = self.package_trie.trie.to_dict()
        
        return result

@dataclass
class JavaSymbol:
    symbol_id: str
    name: str
    package_path: str
    methods: List[str] = field(default_factory=list)
    static_methods: List[str] = field(default_factory=list)
    fields: Set[str] = field(default_factory=set)
    static_fields: Set[str] = field(default_factory=set)
    constructors: List[str] = field(default_factory=list)

    inner_classes: Set[str] = field(default_factory=set)
    static_inner_classes: Set[str] = field(default_factory=set)

    def to_dict(self):
        """转换为字典（用于 JSON 序列化）"""
        return {
            'symbol_id': self.symbol_id,
            'name': self.name,
            'methods': self.methods,
            'static_methods': self.static_methods,
            'fields': self.fields,
            'static_fields': self.static_fields,
            'constructors': self.constructors
        }

import os
from pathlib import Path

from parser.languages.java.core.ast_node_types import JavaFileStructure, PackageInfo
from parser.languages.java.symbol.symbol_commons import ProjectSymbolTable, JavaSymbol

java_class_name_mapping = {
        'ClassInfo': 'class_name',
        'InterfaceInfo': 'interface_name',
        'AnnotationTypeInfo': 'annotation_name',
        'EnumInfo': 'enum_name',
        'RecordInfo': 'record_name',
}
java_class_nested_mapping = ['nested_classes', 'nested_interfaces', 'nested_enums', 'nested_annotations', 'nested_records']

class SymbolManager:

    def __init__(self):
        self.project_symbol_table_cache = {}

    def get_project_symbol_table_cache(self, project_name) -> ProjectSymbolTable:
        if self.project_symbol_table_cache.get(project_name) is None:
            self.project_symbol_table_cache[project_name] = ProjectSymbolTable(project_name)
        return self.project_symbol_table_cache[project_name]

    def collect_from_java_file(self, project_name: str, java_file_structure: JavaFileStructure):
        fqn_path = SymbolManager._extract_package_path(java_file_structure.package_info,
                                                       java_file_structure.relative_path)
        for c in java_file_structure.classes:
            self._extract_java_class_symbol(project_name, fqn_path, c)
        for c in java_file_structure.interfaces:
            self._extract_java_class_symbol(project_name, fqn_path, c)
        for c in java_file_structure.enums:
            self._extract_java_class_symbol(project_name, fqn_path, c)
        for c in java_file_structure.annotations:
            self._extract_java_class_symbol(project_name, fqn_path, c)
        for c in java_file_structure.records:
            self._extract_java_class_symbol(project_name, fqn_path, c)

    def _extract_java_class_symbol(self, project_name, parent_fqn_path, class_info, parent_java_symbol: JavaSymbol | None = None) -> JavaSymbol:
        project_symbol_table_cache = self.get_project_symbol_table_cache(project_name)

        class_name = getattr(class_info, java_class_name_mapping.get( class_info.__class__.__name__))
        if class_name is None:
            raise RuntimeError(f"[SymbolManager]: Class {class_name} not found in class info {class_info}")

        java_symbol = JavaSymbol(class_info.symbol_id, class_name, parent_fqn_path)
        fqn_path_cls = parent_fqn_path + "." + class_name

        project_symbol_table_cache.fqn_class_index[fqn_path_cls] = java_symbol
        project_symbol_table_cache.simple_class_name_index[class_name].add(fqn_path_cls)
        project_symbol_table_cache.package_class_index[parent_fqn_path].add(class_name)

        if parent_java_symbol is not None:
            if class_info.is_static:
                parent_java_symbol.static_inner_classes.add(class_info.class_name)
            else:
                parent_java_symbol.inner_classes.add(class_info.class_name)

        # 处理嵌套类
        for mapping in java_class_nested_mapping:
            for nested_class in getattr(class_info, mapping, []):
                self._extract_java_class_symbol(project_name, fqn_path_cls, nested_class, java_symbol)

        return java_symbol

    @staticmethod
    def _extract_package_path(package_info: PackageInfo, file_path: str) -> str:
        if package_info.name is not None:
            return package_info.name

        # Path.resolve() 会规范化路径，统一使用系统分隔符
        dir_path = Path(file_path).resolve().parent

        # 转换为字符串后，直接使用系统分隔符处理
        dir_str = str(dir_path)

        # 查找 "src/main/java"（使用系统分隔符）
        java_src = os.path.join("src", "main", "java")

        if java_src in dir_str:
            package_path = dir_str.split(java_src)[-1]
            # 去除开头的分隔符
            package_path = package_path.lstrip(os.sep)
            # 将系统分隔符替换为点号
            return package_path.replace(os.sep, '.')
        return ""
import os
from pathlib import Path

from parser.languages.java.core.ast_node_types import JavaFileStructure, PackageInfo
from parser.languages.java.symbol.symbol_commons import ProjectSymbolTable, JavaSymbol


class SymbolManager:

    def __init__(self):
        self.project_symbol_table_cache = {}

    def get_project_symbol_table_cache(self, project_name) -> ProjectSymbolTable:
        if self.project_symbol_table_cache.get(project_name) is None:
            self.project_symbol_table_cache[project_name] = ProjectSymbolTable(project_name)
        return self.project_symbol_table_cache[project_name]

    def collect_from_java_file(self, project_name: str, java_file_structure: JavaFileStructure):
        project_symbol_table_cache = self.get_project_symbol_table_cache(project_name)

        fqn_path = SymbolManager._extract_package_path(java_file_structure.package_info,
                                                       java_file_structure.relative_path)

        element_mappings = [
            (java_file_structure.classes, 'class_name'),
            (java_file_structure.interfaces, 'interface_name'),
            (java_file_structure.annotations, 'annotation_name'),
            (java_file_structure.enums, 'enum_name'),
            (java_file_structure.records, 'record_name'),
        ]

        for elements, name_attr in element_mappings:
            for element in elements:
                element_name = getattr(element, name_attr)
                java_symbol = JavaSymbol(element.symbol_id, element_name, fqn_path)
                fqn_path_cls = fqn_path + "." + element_name

                project_symbol_table_cache.fqn_class_index[fqn_path_cls] = java_symbol
                project_symbol_table_cache.simple_class_name_index[element_name].add(fqn_path_cls)
                project_symbol_table_cache.package_class_index[fqn_path].add(element_name)

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
from parser.common.symbol_table import SymbolIdGenerator


def generate_project_symbol_id(project_name: str, project_type: str = "Application", version: str = "") -> str:
    """
    为 Project 项目生成 symbol_id（语言无关）。

    格式由 SymbolIdGenerator 统一管理：
    - project#{project_name}@{project_type}@{version}
    """
    return SymbolIdGenerator.for_project(project_name, project_type, version)


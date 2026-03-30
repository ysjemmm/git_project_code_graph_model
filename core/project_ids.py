from parser.common.symbol_table import SymbolIdGenerator
from typing import Optional


def generate_project_symbol_id(project_name: str, project_type: str = "Application", version: str = "") -> str:
    """
    为 Project 项目生成 symbol_id（语言无关）。

    格式由 SymbolIdGenerator 统一管理：
    - project#{project_name}@{project_type}@{version}
    """
    return SymbolIdGenerator.for_project(project_name, project_type, version)


def parse_version_from_project_key(project_key: Optional[str]) -> Optional[str]:
    """
    从 project_key 中解析版本号。

    格式：project#{name}@{type}@{version}
    版本号本身可能含 '@'，取第三段起全部拼回。
    """
    if not project_key:
        return None
    parts = project_key.split("@")
    if len(parts) < 3:
        return None
    return "@".join(parts[2:]) or None

from dataclasses import dataclass
from typing import Any, Optional

@dataclass
class AnalyzerContext:
    """分析器上下文 - 统一的参数传递载体"""
    project_name: str
    project_path: str = ""
    file_path: str = ""  # 当前文件路径（相对于项目根目录）
    root_project_symbol_id: str = ""  # 根项目的 symbol_id
    before_uri_path: str = ""
    parser: Optional[Any] = None  # Parser对象，用于解析文件
    lang_type: str = "java"  # 语言类型

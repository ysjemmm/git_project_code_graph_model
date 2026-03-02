from dataclasses import dataclass
from typing import Any, Optional

@dataclass
class AnalyzerContext:
    """分析器上下文 - 统一的参数传递载体"""
    project_name: str
    project_path: str = ""
    before_uri_path: str = ""
    parser: Optional[Any] = None  # Parser对象，用于解析文件
    lang_type: str = "java"  # 语言类型

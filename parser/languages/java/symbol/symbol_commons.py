""" 符号表通用类 """
from dataclasses import dataclass
from enum import Enum
from typing import Optional


class ClassLocationType(Enum):
    """类位置类型"""
    INTERNAL = "INTERNAL"      # 项目内部定义的类
    EXTERNAL = "EXTERNAL"      # 外部 JAR 包的类
    UNKNOWN = "UNKNOWN"       # 无法解析的类


@dataclass
class ClassLocation:
    """类位置信息"""
    type: ClassLocationType     # 类型：内部/外部/未知
    fqn: Optional[str]          # 完全限定名
    jar_path: Optional[str]     # JAR 路径（仅外部类）
    file_path: Optional[str]    # 文件路径（仅内部类）
    resolution_method: str      # 解析方法

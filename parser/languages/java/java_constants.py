"""
Java 解析侧常量定义：节点类型、注释类型等枚举。
原 java_modules.py 中的 Java 语言相关常量，迁移至此统一维护。
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import List


class FileType(Enum):
    JAVA_FILE = "javaFile"


class ObjectType(Enum):
    CLASS_TYPE = "classType"
    INTERFACE_TYPE = "interfaceType"
    ENUM_TYPE = "enumType"
    ANNOTATION_TYPE = "annotationType"
    RECORD_TYPE = "recordType"


class ObjectFromType(Enum):
    INNER_DEFINITION = "InnerDefinition"
    NESTED_DEFINITION = "NestedDefinition"
    EXTERNAL_DEFINITION = "ExternalDefinition"
    JDK_DEFINITION = "JdkDefinition"
    UNKNOWN_DEFINITION = "UnknownDefinition"


class CommentType(Enum):
    JAVADOC = "javadoc"
    LONG_COMMENT = "long_comment"
    BLOCK_COMMENT = "block_comment"
    LINE_COMMENT = "line_comment"


class CommentStorageDecision(Enum):
    STORE_AS_ATTRIBUTE = "store_as_attribute"
    CREATE_JAVADOC_NODE = "create_javadoc_node"
    CREATE_LONG_COMMENT_NODE = "create_long_comment_node"


@dataclass
class JavadocParseResult:
    """Javadoc 解析结果"""
    summary: str = ""
    params: List[str] = field(default_factory=list)
    return_desc: str = ""
    throws: List[str] = field(default_factory=list)
    author: str = ""
    version: str = ""
    since: str = ""
    deprecated: str = ""
    see: List[str] = field(default_factory=list)

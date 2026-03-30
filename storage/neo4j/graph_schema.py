"""
Neo4j 图结构定义：节点类型、关系类型、端点标签映射。
原 java_modules.py 中的图结构相关常量，迁移至此统一维护。
"""
from __future__ import annotations

from enum import Enum
from typing import Dict, Optional, Tuple


class JavaGraphEdgeType(Enum):
    HAVE = "HAVE"
    CONTAINS = "CONTAINS"
    MEMBER_OF = "MEMBER_OF"
    CONTAINS_LIB = "CONTAINS_LIB"
    EXTENDS = "EXTENDS"
    IMPLEMENTS = "IMPLEMENTS"
    CALLS = "CALLS"
    ACCESSES = "ACCESSES"
    HAS_COMMENT = "HAS_COMMENT"
    LIB_LINK = "LIB_LINK"
    SAME_ARTIFACT = "SAME_ARTIFACT"
    DEPENDS_ON = "DEPENDS_ON"

    def get_unique_key(self):
        return ["source", "target"]

    def get_properties(self):
        return []


class JavaNeo4jNodeType(Enum):
    Project = "Project"
    File = "File"
    JavaObject = "JavaObject"
    JavaMethod = "JavaMethod"
    JavaCodeBlock = "JavaCodeBlock"
    JavaField = "JavaField"
    JavaMethodParameter = "JavaMethodParameter"
    JavaEnumConstant = "JavaEnumConstant"
    JavaRecordComponent = "JavaRecordComponent"
    Comment = "Comment"


# 关系类型 -> (source_label, target_label)，用于批量 MERGE 时带 label 走索引
REL_ENDPOINT_LABELS: Dict[str, Optional[Tuple[str, str]]] = {
    JavaGraphEdgeType.HAVE.value: (JavaNeo4jNodeType.Project.value, JavaNeo4jNodeType.File.value),
    JavaGraphEdgeType.CONTAINS.value: (JavaNeo4jNodeType.File.value, JavaNeo4jNodeType.JavaObject.value),
    JavaGraphEdgeType.EXTENDS.value: (JavaNeo4jNodeType.JavaObject.value, JavaNeo4jNodeType.JavaObject.value),
    JavaGraphEdgeType.IMPLEMENTS.value: (JavaNeo4jNodeType.JavaObject.value, JavaNeo4jNodeType.JavaObject.value),
    JavaGraphEdgeType.CALLS.value: (JavaNeo4jNodeType.JavaMethod.value, JavaNeo4jNodeType.JavaMethod.value),
    JavaGraphEdgeType.ACCESSES.value: (JavaNeo4jNodeType.JavaMethod.value, JavaNeo4jNodeType.JavaField.value),
    JavaGraphEdgeType.HAS_COMMENT.value: (JavaNeo4jNodeType.JavaObject.value, JavaNeo4jNodeType.Comment.value),
    JavaGraphEdgeType.CONTAINS_LIB.value: (JavaNeo4jNodeType.Project.value, JavaNeo4jNodeType.JavaObject.value),
    JavaGraphEdgeType.LIB_LINK.value: (JavaNeo4jNodeType.JavaObject.value, JavaNeo4jNodeType.JavaObject.value),
    JavaGraphEdgeType.SAME_ARTIFACT.value: (JavaNeo4jNodeType.Project.value, JavaNeo4jNodeType.Project.value),
    JavaGraphEdgeType.DEPENDS_ON.value: (JavaNeo4jNodeType.Project.value, JavaNeo4jNodeType.Project.value),
}

MEMBER_OF_SOURCE_LABELS = "source:JavaObject OR source:JavaMethod"
MEMBER_OF_TARGET_LABELS = (
    "target:JavaMethod OR target:JavaField OR target:JavaMethodParameter OR "
    "target:JavaEnumConstant OR target:JavaCodeBlock"
)

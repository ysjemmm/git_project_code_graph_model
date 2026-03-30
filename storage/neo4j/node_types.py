"""
Neo4j 节点写入侧数据结构定义（TypedDict）。

设计原则：
- 字段名与 neo_orm.py 的 StructuredNode 定义严格对齐，两者同步修改
- TypedDict 直接就是 dict，build_graph_batch 无需序列化
"""
from __future__ import annotations

from typing import List, Optional
from typing_extensions import TypedDict, NotRequired


class ProjectNode(TypedDict):
    symbol_id: str
    name: str
    project_key: NotRequired[Optional[str]]
    project_type: NotRequired[Optional[str]]
    belong_project: NotRequired[Optional[str]]
    repo_url: NotRequired[Optional[str]]
    repo_branch: NotRequired[Optional[str]]
    repo_commit: NotRequired[Optional[str]]
    version: NotRequired[Optional[str]]


class FileNode(TypedDict):
    symbol_id: str
    file_path: str
    name: NotRequired[Optional[str]]
    full_path: NotRequired[Optional[str]]
    package_name: NotRequired[Optional[str]]
    file_type: NotRequired[Optional[str]]
    imports: NotRequired[List[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaObjectNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    qualified_name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    object_type: NotRequired[Optional[str]]
    from_type: NotRequired[Optional[str]]
    raw_metadata: NotRequired[Optional[str]]
    super_class: NotRequired[Optional[str]]
    super_interfaces: NotRequired[List[str]]
    type_parameters: NotRequired[List[str]]
    annotations: NotRequired[List[str]]
    simple_comment: NotRequired[Optional[str]]
    has_detailed_comment: NotRequired[bool]
    request_uri: NotRequired[Optional[str]]
    belong_file: NotRequired[Optional[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaMethodNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    raw_signature: NotRequired[Optional[str]]
    return_type: NotRequired[Optional[str]]
    is_static: NotRequired[bool]
    is_constructor: NotRequired[bool]
    base_uri: NotRequired[Optional[str]]
    full_uri: NotRequired[Optional[str]]
    mapping_method_type: NotRequired[Optional[str]]
    type_parameters: NotRequired[List[str]]
    annotations: NotRequired[List[str]]
    throws_exceptions: NotRequired[List[str]]
    simple_comment: NotRequired[Optional[str]]
    has_detailed_comment: NotRequired[bool]
    raw_metadata: NotRequired[Optional[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaFieldNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    type_name: NotRequired[Optional[str]]
    is_static: NotRequired[bool]
    is_final: NotRequired[bool]
    has_default_value: NotRequired[bool]
    default_value: NotRequired[Optional[str]]
    annotations: NotRequired[List[str]]
    simple_comment: NotRequired[Optional[str]]
    has_detailed_comment: NotRequired[bool]
    raw_metadata: NotRequired[Optional[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaMethodParameterNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    type_name: NotRequired[Optional[str]]
    annotations: NotRequired[List[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaCodeBlockNode(TypedDict):
    symbol_id: str
    parent_symbol_id: NotRequired[Optional[str]]
    is_static: NotRequired[bool]
    raw_metadata: NotRequired[Optional[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaEnumConstantNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    annotations: NotRequired[List[str]]
    arguments: NotRequired[List[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class JavaRecordComponentNode(TypedDict):
    symbol_id: str
    name: NotRequired[Optional[str]]
    parent_symbol_id: NotRequired[Optional[str]]
    type_name: NotRequired[Optional[str]]
    annotations: NotRequired[List[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]


class CommentNode(TypedDict):
    symbol_id: str
    parent_symbol_id: NotRequired[Optional[str]]
    content: NotRequired[Optional[str]]
    comment_type: NotRequired[Optional[str]]
    char_count: NotRequired[Optional[int]]
    line_count: NotRequired[Optional[int]]
    javadoc_summary: NotRequired[Optional[str]]
    javadoc_params: NotRequired[List[str]]
    javadoc_return: NotRequired[Optional[str]]
    javadoc_throws: NotRequired[List[str]]
    javadoc_author: NotRequired[Optional[str]]
    javadoc_version: NotRequired[Optional[str]]
    javadoc_since: NotRequired[Optional[str]]
    javadoc_deprecated: NotRequired[Optional[str]]
    javadoc_see: NotRequired[List[str]]
    start_line: NotRequired[Optional[int]]
    end_line: NotRequired[Optional[int]]
    start_column: NotRequired[Optional[int]]
    end_column: NotRequired[Optional[int]]
    belong_project: NotRequired[Optional[str]]
    project_key: NotRequired[Optional[str]]

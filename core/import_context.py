from __future__ import annotations

from dataclasses import dataclass
from typing import Optional, Sequence


@dataclass(frozen=True, slots=True)
class ProjectImportContext:
    """
    单次导入任务的上下文载体（不包含运行时状态）。

    目标：
    - 收口项目相关的关键参数，减少在 importer/adapter/exporter 之间散落传参
    - 不改变既有对外 API 与行为
    """

    project_name: str
    project_key: str  # 推荐：Application 项目根 Project 节点的 symbol_id
    repo_cache_dir: str

    source_dir: Optional[str] = None
    language: Optional[str] = None
    languages: Optional[Sequence[str]] = None

    clear_database: bool = False
    include_comment_nodes: bool = False  # 是否创建 Comment 节点（默认不创建，注释仅存于父节点 simple_comment）


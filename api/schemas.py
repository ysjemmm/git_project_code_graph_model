"""
API 请求/响应模型定义，统一放在此处便于维护与复用。
"""
from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel


class HistoryMessage(BaseModel):
    role: str  # "user" | "assistant"
    content: str


class BugfixRequest(BaseModel):
    apply: bool = False
    git_url: str
    git_branch: Optional[str] = None
    git_commit: Optional[str] = None
    message: str
    provider: Optional[str] = "deepseek"
    model: Optional[str] = None
    uploaded_file_names: Optional[List[str]] = None
    session_id: Optional[str] = None
    history: Optional[List[HistoryMessage]] = None


class BugfixResponse(BaseModel):
    ok: bool
    message: str
    target_file: str
    applied: bool
    unified_diff: str


class GitRepoItem(BaseModel):
    name: str
    url: str


class GitRepoListResponse(BaseModel):
    gitList: List[GitRepoItem]


class GitRefListResponse(BaseModel):
    items: List[str]


class UploadSavedItem(BaseModel):
    filename: str
    path: str


class UploadResponse(BaseModel):
    ok: bool
    saved: List[UploadSavedItem] = []


class GraphProjectItem(BaseModel):
    project_name: str
    project_key: Optional[str] = None
    project_type: Optional[str] = None  # Application | Lib | ...
    branch: Optional[str] = None
    commit_hash: Optional[str] = None
    repo_url: Optional[str] = None
    last_update_time: Optional[str] = None
    node_count: Optional[int] = None
    relationship_count: Optional[int] = None


class GraphProjectListResponse(BaseModel):
    items: List[GraphProjectItem] = []


class CacheProjectItem(BaseModel):
    id: Optional[int] = None
    repo_name: str
    project_name: Optional[str] = None
    project_key: Optional[str] = None
    project_type: Optional[str] = None

    # git cache metadata
    repo_url: Optional[str] = None
    branch: Optional[str] = None
    commit_hash: Optional[str] = None
    last_update_time: Optional[str] = None
    cache_dir: Optional[str] = None

    # local repo status
    repo_exists: bool = False
    remote_url: Optional[str] = None
    head_branch: Optional[str] = None
    head_commit: Optional[str] = None
    dirty: Optional[bool] = None

    # cache metrics
    cache_size_mb: Optional[float] = None
    merkle_branches: List[str] = []

    # 依赖关联概览
    linked_by_count: Optional[int] = 0   # 多少项目“关联到我”（入向）
    linked_to_count: Optional[int] = 0   # 我“关联到”多少项目（出向）
    linked_by_preview: List[str] = []    # 入向关联预览（来源项目(次数)）

    # 应用类型与语言
    app_type: Optional[str] = 'backend'   # 'frontend' | 'backend'
    language: Optional[str] = 'java'      # 'java'|'python'|'go'|'other'|'vue'|'react'


class CacheProjectListResponse(BaseModel):
    items: List[CacheProjectItem] = []


class MerkleTreeSummary(BaseModel):
    repo_name: str
    branch: str
    node_count: int
    file_count: int
    dir_count: int
    max_depth: int


class MerkleTreeResponse(BaseModel):
    summary: MerkleTreeSummary
    tree: dict


class DeleteApplicationRequest(BaseModel):
    project_name: str
    project_key: Optional[str] = None
    repo_name: Optional[str] = None


class DeleteApplicationResponse(BaseModel):
    ok: bool
    message: str
    deleted_graph_nodes: int = 0
    cache_deleted: bool = False


class CanDeleteApplicationResponse(BaseModel):
    """删除前检查：是否存在进行中的导入任务或 AI 会话。"""
    ok: bool  # True 表示可删除
    block_reason: Optional[str] = None  # 不可删除时的原因（进行中导入任务 / AI 对话）


class SecondPartyRuleCreateRequest(BaseModel):
    name: str
    enabled: bool = True
    sort_order: int = 0
    group_id_regex: str
    artifact_id_regex: str


class SecondPartyRuleUpdateRequest(BaseModel):
    name: str
    enabled: bool = True
    sort_order: int = 0
    group_id_regex: str
    artifact_id_regex: str


class SecondPartyRuleResponseItem(BaseModel):
    id: int
    name: str
    enabled: bool
    sort_order: int
    group_id_regex: str
    artifact_id_regex: str
    created_at: str
    updated_at: str


class SecondPartyRuleResponse(BaseModel):
    ok: bool
    message: Optional[str] = None
    item: Optional[SecondPartyRuleResponseItem] = None


class SecondPartyRuleListResponse(BaseModel):
    ok: bool
    items: List[SecondPartyRuleResponseItem] = []

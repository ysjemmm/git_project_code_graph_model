from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Any, Dict, Literal, Optional


ImportStatus = Literal["cached", "cleared", "imported", "failed"]


@dataclass
class ImportResult:
    """
    统一的导入结果结构（内部使用，外部仍可接收 dict）。

    说明：
    - created_* 表示本次真正新建的数量（MERGE 创建）
    - attempted_* 表示本次提交给数据库执行写入的条目数（经过去重）
    - deleted_nodes 多用于 clear_subgraph 场景
    """

    success: bool
    status: ImportStatus
    message: str = ""

    # Git / 项目信息
    repo_name: Optional[str] = None
    branch: Optional[str] = None
    project_name: Optional[str] = None
    project_key: Optional[str] = None
    commit_hash: Optional[str] = None

    # 变更统计
    attempted_nodes: int = 0
    attempted_relationships: int = 0
    created_nodes: int = 0
    created_relationships: int = 0
    deleted_nodes: int = 0

    # 耗时（毫秒）
    duration_ms: Optional[int] = None

    # 附加信息（例如统计结果）
    statistics: Optional[Dict[str, Any]] = None
    extra: Optional[Dict[str, Any]] = None

    def to_dict(self) -> Dict[str, Any]:
        """
        对外兼容的 dict 形式（保留历史字段命名）。
        """
        base = asdict(self)

        # 兼容：老字段语义为 created（本次新建）
        base["nodes_count"] = self.created_nodes
        base["relationships_count"] = self.created_relationships
        base["added_nodes"] = self.created_nodes
        base["added_relationships"] = self.created_relationships

        # statistics / extra 原样挂载（如果有）
        if self.statistics is not None:
            base["statistics"] = self.statistics
        if self.extra is not None:
            base["extra"] = self.extra

        return base


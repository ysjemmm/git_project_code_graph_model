"""
Bugfix 历史记录 API：增/查。
"""
from __future__ import annotations

from typing import Optional

from fastapi import APIRouter
from pydantic import BaseModel

from storage.sqlite.business import get_business_db, BugfixHistoryRepo

router = APIRouter(prefix="/api/bugfix-history", tags=["bugfix-history"])


def _get_repo() -> BugfixHistoryRepo:
    db = get_business_db()
    return BugfixHistoryRepo(db)


# ─── 请求/响应 Schema ─────────────────────────────────────────────────────────

class CreateHistoryRequest(BaseModel):
    project:    str
    bug_id:     str
    bug_title:  str
    model:      str
    depth:      str
    status:     str = "running"       # running | success | failed
    duration_ms: Optional[int] = None
    session_id:  Optional[str] = None
    prompt:      Optional[str] = None
    git_url:     Optional[str] = None
    git_branch:  Optional[str] = None
    git_commit:  Optional[str] = None


class UpdateStatusRequest(BaseModel):
    status:       str
    duration_ms:  Optional[int] = None
    node_io_json: Optional[str] = None   # 完整链路快照 JSON
    outcome:      Optional[str] = None   # success | error_end | failed


# ─── 路由 ────────────────────────────────────────────────────────────────────

@router.post("")
def create_history(body: CreateHistoryRequest):
    """新增一条 Bugfix 执行历史记录。"""
    repo = _get_repo()
    record_id = repo.create(
        project=body.project,
        bug_id=body.bug_id,
        bug_title=body.bug_title,
        model=body.model,
        depth=body.depth,
        status=body.status,
        duration_ms=body.duration_ms,
        session_id=body.session_id,
        prompt=body.prompt,
        git_url=body.git_url,
        git_branch=body.git_branch,
        git_commit=body.git_commit,
    )
    record = repo.get_by_id(record_id)
    return {"ok": True, "id": record_id, "item": record.to_dict() if record else None}


@router.patch("/{record_id}/status")
def update_history_status(record_id: int, body: UpdateStatusRequest):
    """更新记录状态（执行完成后调用）。"""
    repo = _get_repo()
    repo.update_status(
        record_id,
        status=body.status,
        duration_ms=body.duration_ms,
        node_io_json=body.node_io_json,
        outcome=body.outcome,
    )
    record = repo.get_by_id(record_id)
    return {"ok": True, "item": record.to_dict() if record else None}


@router.get("")
def list_history(
    page:      int = 1,
    page_size: int = 20,
    project:   Optional[str] = None,
    bug_id:    Optional[str] = None,
):
    """分页查询 Bugfix 执行历史。"""
    repo = _get_repo()
    total, items = repo.list_records(
        page=page,
        page_size=page_size,
        project=project or None,
        bug_id=bug_id or None,
    )
    return {
        "ok":        True,
        "total":     total,
        "page":      page,
        "page_size": page_size,
        "items":     [r.to_dict() for r in items],
    }


@router.get("/{record_id}")
def get_history(record_id: int):
    """获取单条历史记录。"""
    repo = _get_repo()
    record = repo.get_by_id(record_id)
    if not record:
        return {"ok": False, "message": f"记录 {record_id} 不存在"}
    return {"ok": True, "item": record.to_dict()}

"""
图谱导入任务管理 API：
- 严格单并发（max_workers=1），其余排队
- 提交任务：git_url + branch/commit_id + maven_scan_enabled
- 查询任务：列表/单个
- 读取任务日志：增量拉取（offset）
"""
from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Query

from api.config import PROJECT_ROOT

router = APIRouter(prefix="/api", tags=["import"])


def _log_dir() -> Path:
    d = PROJECT_ROOT / ".cache" / "task_logs"
    d.mkdir(parents=True, exist_ok=True)
    return d


def _task_queue():
    # 强制单并发：max_workers=1
    from core.task_queue import get_task_queue

    q = get_task_queue(max_workers=1, cache_base_dir=str(PROJECT_ROOT / ".cache" / "git_repos"))
    if not q.running:
        q.start()
    return q


def _task_log_path(task_id: str) -> Path:
    return _log_dir() / f"{task_id}.log"


class _TaskLogHandler(logging.Handler):
    def __init__(self, path: Path):
        super().__init__(level=logging.INFO)
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)

    def emit(self, record: logging.LogRecord) -> None:
        try:
            msg = self.format(record)
            self.path.open("a", encoding="utf-8", errors="ignore").write(msg + "\n")
        except Exception:
            pass


def _install_task_logger(task_id: str) -> _TaskLogHandler:
    """
    在单并发约束下，将 root logger 绑定到该 task 的日志文件。
    """
    handler = _TaskLogHandler(_task_log_path(task_id))
    handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s - %(message)s"))
    root = logging.getLogger()
    root.addHandler(handler)
    return handler


def _uninstall_task_logger(handler: _TaskLogHandler) -> None:
    try:
        logging.getLogger().removeHandler(handler)
        handler.close()
    except Exception:
        pass


# --- 轻量 monkeypatch：给 TaskQueue._execute_task 包一层日志落盘 ---
_PATCHED = False


def _ensure_patched() -> None:
    global _PATCHED
    if _PATCHED:
        return
    from core.task_queue import TaskQueue

    # 兼容：某些精简实现的 TaskQueue 可能没有 _execute_task（例如占位实现）。
    # 此时跳过 patch，保证提交任务/列表接口不因 monkeypatch 失败而 500。
    orig = getattr(TaskQueue, "_execute_task", None)
    if orig is None:
        _PATCHED = True
        return

    def wrapped(self: Any, task: Any):  # type: ignore
        handler = _install_task_logger(getattr(task, "task_id", "unknown"))
        try:
            return orig(self, task)
        finally:
            _uninstall_task_logger(handler)

    TaskQueue._execute_task = wrapped  # type: ignore
    _PATCHED = True


@router.post("/import/tasks")
def submit_import_task(body: Dict[str, Any]) -> Dict[str, Any]:
    """
    提交导入任务：
    {
      "repo_url": "...",
      "branch": "xxx",           // branch 或 ""
      "commit_id": "xxxx",       // commit 或 ""
      "project_name": "forward", // 可选
      "maven_scan_enabled": true,// 是否解析 pom 外部依赖
      "force_maven": true        // 强制重新解析/拉取 maven 依赖并重扫 jar（忽略 marker 与 jar_metadata）
    }
    """
    _ensure_patched()
    q = _task_queue()

    repo_url = (body.get("repo_url") or "").strip()
    branch = (body.get("branch") or "").strip()
    commit_id = (body.get("commit_id") or "").strip()
    project_name = (body.get("project_name") or "").strip() or None
    java_source_dir = (body.get("java_source_dir") or "").strip() or None
    maven_scan_enabled = bool(body.get("maven_scan_enabled", True))
    force_maven = bool(body.get("force_maven", False))
    clear_database = bool(body.get("clear_database", False))

    if not repo_url:
        return {"ok": False, "message": "repo_url 不能为空"}
    if not branch and not commit_id:
        return {"ok": False, "message": "branch 与 commit_id 至少填写一个"}
    if branch and commit_id:
        return {"ok": False, "message": "branch 与 commit_id 二选一"}

    # repo_name 默认从 url 推断（与 importer 一致）
    repo_name = repo_url.split("/")[-1].replace(".git", "")
    task_id = q.submit_task(
        repo_url=repo_url,
        branch=branch or "main",
        repo_name=repo_name,
        project_name=project_name or repo_name,
        java_source_dir=java_source_dir,
        commit_id=commit_id or None,
        clear_database=clear_database,
        maven_scan_enabled=maven_scan_enabled,
        force_maven=force_maven,
        priority=getattr(__import__("core.task_queue", fromlist=["TaskPriority"]), "TaskPriority").NORMAL,
    )

    return {"ok": True, "task_id": task_id}


@router.get("/import/tasks")
def list_import_tasks(limit: int = Query(50, ge=1, le=500)) -> Dict[str, Any]:
    q = _task_queue()
    tasks = q.get_all_tasks()
    # 最新优先
    tasks = sorted(tasks, key=lambda x: (x.get("created_at") or ""), reverse=True)
    return {"ok": True, "items": tasks[: int(limit)], "stats": q.get_queue_stats()}


@router.get("/import/tasks/{task_id}")
def get_import_task(task_id: str) -> Dict[str, Any]:
    q = _task_queue()
    s = q.get_task_status(task_id)
    if not s:
        return {"ok": False, "message": "task not found"}
    return {"ok": True, "item": s}


@router.post("/import/tasks/{task_id}/cancel")
def cancel_import_task(task_id: str) -> Dict[str, Any]:
    """
    取消导入任务：
    - pending：立即取消
    - running：发送取消信号，任务会在可中断点尽快退出
    """
    q = _task_queue()
    ok = bool(q.cancel_task(task_id))
    if not ok:
        return {"ok": False, "message": "task not found or not cancellable"}
    return {"ok": True}


@router.get("/import/tasks/{task_id}/logs")
def get_import_task_logs(
    task_id: str,
    offset: int = Query(0, ge=0),
    limit: int = Query(200, ge=1, le=2000),
) -> Dict[str, Any]:
    """
    增量读取日志（按行）。返回：
    - lines: 本次读取的行
    - next_offset: 下一次 offset
    """
    path = _task_log_path(task_id)
    if not path.is_file():
        # 兜底：日志文件不存在时，返回任务的 error/result 摘要，避免前端“空白”
        try:
            q = _task_queue()
            s = q.get_task_status(task_id)
        except Exception:
            s = None
        lines: List[str] = []
        if isinstance(s, dict):
            if s.get("status"):
                lines.append(f"status: {s.get('status')}")
            if s.get("error"):
                lines.append(f"error: {s.get('error')}")
            if s.get("result"):
                lines.append(f"result: {s.get('result')}")
        return {"ok": True, "lines": lines, "next_offset": int(offset) + len(lines), "exists": False}

    lines: List[str] = []
    with path.open("r", encoding="utf-8", errors="ignore") as f:
        # 跳过 offset 行
        for _ in range(int(offset)):
            if not f.readline():
                return {"ok": True, "lines": [], "next_offset": offset, "exists": True}
        for _ in range(int(limit)):
            line = f.readline()
            if not line:
                break
            lines.append(line.rstrip("\n"))

    return {"ok": True, "lines": lines, "next_offset": int(offset) + len(lines), "exists": True}


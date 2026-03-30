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
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Query

from tools.constants import CACHE_TASK_LOGS_PATH, CACHE_GIT_REPOS_PATH

router = APIRouter(prefix="/api", tags=["import"])


def _log_dir() -> Path:
    d = CACHE_TASK_LOGS_PATH
    d.mkdir(parents=True, exist_ok=True)
    return d


def _task_queue():
    # 强制单并发：max_workers=1
    from core.task_queue import get_task_queue

    q = get_task_queue(max_workers=1, cache_base_dir=str(CACHE_GIT_REPOS_PATH))
    if not q.running:
        q.start()
    return q


def _task_log_path(task_id: str) -> Path:
    return _log_dir() / f"{task_id}.log"


class _TaskLogHandler(logging.Handler):
    def __init__(self, path: Path):
        super().__init__(level=logging.DEBUG)
        self.path = path
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._file = self.path.open("a", encoding="utf-8", errors="ignore", buffering=1)

    def emit(self, record: logging.LogRecord) -> None:
        try:
            msg = self.format(record)
            self._file.write(msg + "\n")
            self._file.flush()
        except Exception:
            pass

    def close(self) -> None:
        try:
            self._file.flush()
            self._file.close()
        except Exception:
            pass
        super().close()


def _install_task_logger(task_id: str) -> _TaskLogHandler:
    handler = _TaskLogHandler(_task_log_path(task_id))
    handler.setFormatter(logging.Formatter("%(asctime)s %(levelname)s %(name)s - %(message)s"))
    root = logging.getLogger()
    # 确保 root logger level 不会过滤掉 INFO
    if root.level == logging.NOTSET or root.level > logging.INFO:
        root.setLevel(logging.INFO)
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

    async def wrapped(self: Any, task: Any):  # type: ignore
        handler = _install_task_logger(getattr(task, "task_id", "unknown"))
        try:
            return await orig(self, task)
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
      "force_maven": true,       // 强制重新解析/拉取 maven 依赖并重扫 jar（忽略 marker 与 jar_metadata）
      "task_type": "auto",       // auto | full | incremental
      "acceptance_enabled": true, // 是否启用导入验收闭环（前后快照+delta）
      "acceptance_block_on_fail": false, // 验收失败时是否阻断后续 auto 任务
      "acceptance_max_drop_ratio": 0.3   // 非 clear_database 场景下最大允许降幅
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
    auto_link_external = bool(body.get("auto_link_external", False))
    task_type = str(body.get("task_type") or "auto").strip().lower()
    app_type = (body.get("app_type") or "backend").strip()
    language = (body.get("language") or "java").strip()
    app_version = (body.get("app_version") or "").strip()
    acceptance_enabled = bool(body.get("acceptance_enabled", True))
    acceptance_block_on_fail = bool(body.get("acceptance_block_on_fail", False))
    acceptance_max_drop_ratio = body.get("acceptance_max_drop_ratio", 0.3)
    if task_type not in {"auto", "full", "incremental"}:
        return {"ok": False, "message": "task_type 仅支持 auto/full/incremental"}

    if not repo_url:
        return {"ok": False, "message": "repo_url 不能为空"}
    if not project_name:
        return {"ok": False, "message": "应用名称（project_name）不能为空"}
    if not app_version:
        return {"ok": False, "message": "版本号（app_version）不能为空"}
    if not branch and not commit_id:
        return {"ok": False, "message": "branch 与 commit_id 至少填写一个"}
    if branch and commit_id:
        return {"ok": False, "message": "branch 与 commit_id 二选一"}

    # repo_name 统一使用 project_name，保证缓存目录与应用名称一致
    repo_name = project_name

    # 使用 TaskSubmitContext 封装参数，使调用更清晰
    from core.task_queue import TaskSubmitContext, TaskPriority
    
    ctx = TaskSubmitContext(
        repo_url=repo_url,
        branch=branch or "main",
        repo_name=repo_name,
        project_name=project_name,
        java_source_dir=java_source_dir,
        commit_id=commit_id or None,
        clear_database=clear_database,
        maven_scan_enabled=maven_scan_enabled,
        force_maven=force_maven,
        auto_link_external=auto_link_external,
        app_version=app_version,
        task_type=task_type,
        acceptance_enabled=acceptance_enabled,
        acceptance_block_on_fail=acceptance_block_on_fail,
        acceptance_max_drop_ratio=acceptance_max_drop_ratio,
        priority=TaskPriority.NORMAL,
    )
    
    try:
        task_id = q.submit_task(ctx)
    except Exception as e:
        return {"ok": False, "message": str(e)}

    # 确保应用在 SQLite 缓存表中存在（按 project_name 做 upsert，与版本无关）
    try:
        from api.main import get_business_db
        bdb = get_business_db()
        bdb.conn.execute(
            """INSERT INTO application_projects_cache
                 (repo_name, project_name, project_type, repo_url, app_type, language, created_at, updated_at)
               VALUES (?, ?, 'Application', ?, ?, ?, datetime('now'), datetime('now'))
               ON CONFLICT(repo_name) DO UPDATE SET
                 repo_url=excluded.repo_url,
                 app_type=excluded.app_type,
                 language=excluded.language,
                 updated_at=excluded.updated_at""",
            (repo_name, project_name, repo_url, app_type, language),
        )
        bdb.conn.commit()
    except Exception:
        pass  # 不影响主流程

    return {"ok": True, "task_id": task_id}


@router.post("/import/tasks/test-schedule")
async def create_test_import_task(
    project_name: str = Query(..., description="项目名称，用于测试调度"),
    wait_seconds: int = Query(10, ge=1, le=60, description="模拟等待秒数"),
) -> Dict[str, Any]:
    """
    创建测试导入任务：
    - 不执行真实的 Git 克隆和图谱导入
    - 仅模拟任务流程：pending → running → success/failed
    - 会产生日志并写入 SQLite
    - 用于验证任务调度框架的完整流程
    """
    import asyncio, time, uuid
    from core.task_queue import TaskStatus as _TaskStatus, GitImportTask, TaskPriority as _TaskPriority

    _ensure_patched()
    q = _task_queue()
    q._lazy_init()

    task_id = str(uuid.uuid4())
    task = GitImportTask(
        task_id=task_id,
        repo_url='https://example.com/test/repo.git',
        branch='test-branch',
        project_name=project_name,
        task_type='test',
        app_version='test-' + str(int(time.time())),
        priority=_TaskPriority.NORMAL,
        maven_scan_enabled=False,
        clear_database=False,
        acceptance_enabled=True,
    )
    q.tasks[task_id] = task
    try:
        q.task_repo.save(task.to_dict())
    except Exception:
        pass

    async def run_test_task():
        task = q.tasks.get(task_id)
        if not task:
            return

        log_handler = _install_task_logger(task_id)
        task.status = _TaskStatus.RUNNING
        task.started_at = datetime.now().isoformat()

        logger = logging.getLogger(f"test_task.{task_id}")
        logger.info(f"[TEST_TASK] 测试任务启动 task_id={task_id}")
        logger.info(f"[TEST_TASK] project={project_name}  wait={wait_seconds}s")
        logger.info(f"[TEST_TASK] 这是一个模拟任务，不执行真实导入")

        try:
            for i in range(wait_seconds):
                await asyncio.sleep(1)
                logger.info(f"[TEST_TASK] 进度 {i + 1}/{wait_seconds}s ...")

            task.status = _TaskStatus.SUCCESS
            task.result = {
                "extra": {
                    "is_test": True,
                    "wait_seconds": wait_seconds,
                    "acceptance": {"enabled": True, "status": "passed", "summary": "Test passed"},
                }
            }
            logger.info(f"[TEST_TASK] 任务完成 ✓")

        except asyncio.CancelledError:
            task.status = _TaskStatus.CANCELLED
            task.error = "cancelled"
            logger.warning(f"[TEST_TASK] 任务被取消")

        except Exception as e:
            task.status = _TaskStatus.FAILED
            task.error = str(e)
            logger.error(f"[TEST_TASK] 任务失败: {e}")

        finally:
            logger.info(f"[TEST_TASK] status={task.status.value}")
            _uninstall_task_logger(log_handler)
            task.completed_at = datetime.now().isoformat()
            try:
                q.task_repo.upsert_task(
                    task_id=task_id,
                    status=task.status.value,
                    priority=str(task.priority.name),
                    created_at=task.created_at,
                    started_at=task.started_at,
                    completed_at=task.completed_at,
                    retry_count=task.retry_count,
                    max_retries=task.max_retries,
                    task_obj=task.to_dict(),
                    updated_at=datetime.now().isoformat(),
                )
            except Exception as ex:
                logger.error(f"[TEST_TASK] save_state_failed: {ex}")
    
    # 在后台运行测试任务
    asyncio.create_task(run_test_task())
    
    return {"ok": True, "task_id": task_id, "message": f"Test task created, will wait for {wait_seconds}s"}


@router.get("/import/tasks")
def list_import_tasks(
    limit: int = Query(50, ge=1, le=500),
    project_name: Optional[str] = Query(None, description="按项目名称筛选"),
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(6, ge=1, le=100, description="每页数量"),
) -> Dict[str, Any]:
    q = _task_queue()
    # 1) 运行态任务（内存）
    mem_tasks = q.get_all_tasks()
    merged: Dict[str, Dict[str, Any]] = {
        str(t.get("task_id") or ""): t for t in mem_tasks if t.get("task_id")
    }

    # 2) 历史任务（sqlite），补齐重启后不可见的问题
    try:
        db_tasks = q.task_repo.list_all_tasks()  # type: ignore[attr-defined]
    except Exception:
        db_tasks = []
    for t in db_tasks:
        tid = str(t.get("task_id") or "")
        if not tid:
            continue
        # 内存优先（状态更实时），sqlite 作为历史补齐
        if tid not in merged:
            merged[tid] = t

    tasks = list(merged.values())
    
    # 按项目名筛选
    if project_name:
        tasks = [
            t for t in tasks
            if str(t.get("project_name") or "") == project_name
        ]
    
    # 最新优先
    tasks = sorted(tasks, key=lambda x: (x.get("created_at") or ""), reverse=True)
    
    # 分页
    total = len(tasks)
    start_idx = (page - 1) * page_size
    end_idx = start_idx + page_size
    paginated_tasks = tasks[start_idx:end_idx]
    
    stats = q.get_queue_stats()
    stats["history_total"] = total
    return {"ok": True, "items": paginated_tasks[: int(limit)], "total": total, "stats": stats}


@router.get("/import/tasks/{task_id}")
def get_import_task(task_id: str) -> Dict[str, Any]:
    q = _task_queue()
    s = q.get_task_status(task_id)
    if not s:
        # 内存中找不到时，回退 sqlite 历史
        try:
            s = q.task_repo.get_task(task_id)  # type: ignore[attr-defined]
        except Exception:
            s = None
    if not s:
        return {"ok": False, "message": "task not found"}
    return {"ok": True, "item": s}


@router.get("/import/tasks/{task_id}/acceptance-detail")
def get_import_task_acceptance_detail(task_id: str) -> Dict[str, Any]:
    q = _task_queue()
    detail = None
    try:
        detail = q.task_repo.get_task_delta_detail(task_id)  # type: ignore[attr-defined]
    except Exception:
        detail = None
    if detail is None:
        s = q.get_task_status(task_id)
        if not s:
            try:
                s = q.task_repo.get_task(task_id)  # type: ignore[attr-defined]
            except Exception:
                s = None
        extra = (s or {}).get("result", {}).get("extra", {}) if isinstance(s, dict) else {}
        detail = {
            "snapshot_before": extra.get("snapshot_before"),
            "snapshot_after": extra.get("snapshot_after"),
            "snapshot_delta": extra.get("snapshot_delta"),
            "acceptance": extra.get("acceptance"),
            "delta_detail": extra.get("delta_detail"),
            "task_type": extra.get("task_type"),
            "effective_mode": extra.get("effective_mode"),
            "fallback_reason": extra.get("fallback_reason"),
            "changed_files_list": extra.get("changed_files_list"),
            "added_files_list": extra.get("added_files_list"),
            "deleted_files_list": extra.get("deleted_files_list"),
        }
    return {"ok": True, "task_id": task_id, "detail": detail or {}}


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


@router.post("/import/tasks/unblock-auto")
def unblock_auto_import_tasks(body: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    """
    手动解除“后续 auto 任务阻断”状态。
    用于人工确认验收失败已处理后，恢复自动任务提交能力。
    """
    q = _task_queue()
    reason = ""
    if isinstance(body, dict):
        reason = str(body.get("reason") or "").strip()
    q.clear_auto_submission_block(reason=reason)
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


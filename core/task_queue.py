#!/usr/bin/env python3
"""
异步任务队列 - 完全重写版
基于 asyncio + ThreadPoolExecutor 的混合架构
- 保持同步 API 兼容
- 内部异步调度
- 按项目隔离的单并发（同一项目只能有一个任务运行）
- 多项目并行执行（默认最多 10 个项目同时运行）
- SQLite 持久化
"""

import asyncio
import json
import os
import sqlite3
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, asdict, field
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional

sys.path.insert(0, str(Path(__file__).parent.parent))

from core.importer import GitToNeo4jImporter, ImportCancelled
from parser.utils.logger import get_logger
from storage.sqlite.business.git_tasks_repo import get_git_import_tasks_repo
from tools.constants import CACHE_BUSINESS_DB_PATH, CACHE_GIT_REPOS_PATH, CACHE_ROOT_PATH

# 配置日志
logger = get_logger("git_task_queue")


class TaskStatus(Enum):
    """任务状态"""
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    FAILED = "failed"
    CANCELLED = "cancelled"


class TaskPriority(Enum):
    """任务优先"""
    LOW = 3
    NORMAL = 2
    HIGH = 1
    CRITICAL = 0


@dataclass
class TaskBase:
    """任务基础类"""
    repo_url: str
    branch: str = "main"
    commit_id: Optional[str] = None
    clone_timeout: Optional[int] = None
    git_config: Optional[Dict[str, str]] = None
    repo_name: Optional[str] = None
    project_name: Optional[str] = None
    java_source_dir: Optional[str] = None
    language: Optional[str] = None
    languages: Optional[List[str]] = None
    clear_database: bool = False
    include_unchanged_total: bool = False
    include_comment_nodes: bool = False
    maven_scan_enabled: bool = True
    force_maven: bool = False
    auto_link_external: bool = True
    task_type: str = "auto"
    app_version: str = ""
    priority: TaskPriority = TaskPriority.NORMAL
    acceptance_enabled: bool = True
    acceptance_block_on_fail: bool = False
    acceptance_max_drop_ratio: float = 0.3


@dataclass
class TaskSubmitContext(TaskBase):
    """任务提交上下文"""
    
    def to_task_dict(self) -> Dict[str, Any]:
        return {
            'repo_url': self.repo_url,
            'branch': self.branch,
            'repo_name': self.repo_name,
            'java_source_dir': self.java_source_dir,
            'project_name': self.project_name,
            'language': self.language,
            'languages': self.languages,
            'clear_database': self.clear_database,
            'commit_id': self.commit_id,
            'clone_timeout': self.clone_timeout,
            'git_config': self.git_config,
            'include_unchanged_total': self.include_unchanged_total,
            'include_comment_nodes': self.include_comment_nodes,
            'maven_scan_enabled': self.maven_scan_enabled,
            'force_maven': self.force_maven,
            'auto_link_external': self.auto_link_external,
            'app_version': self.app_version,
            'task_type': self.task_type,
            'acceptance_enabled': self.acceptance_enabled,
            'acceptance_block_on_fail': self.acceptance_block_on_fail,
            'acceptance_max_drop_ratio': self.acceptance_max_drop_ratio,
            'priority': self.priority,
        }


@dataclass
class GitImportTask(TaskBase):
    """Git 导入任务实体"""
    task_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    status: TaskStatus = TaskStatus.PENDING
    created_at: str = field(default_factory=lambda: datetime.now().isoformat())
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    result: Optional[Dict] = None
    error: Optional[str] = None
    retry_count: int = 0
    max_retries: int = 3
    
    def to_dict(self) -> Dict:
        data = asdict(self)
        data['status'] = self.status.value
        data['priority'] = self.priority.value
        return data
    
    @staticmethod
    def from_dict(data: Dict) -> 'GitImportTask':
        if 'status' in data and isinstance(data['status'], str):
            data['status'] = TaskStatus(data['status'])
        if 'priority' in data and isinstance(data['priority'], int):
            data['priority'] = TaskPriority(data['priority'])
        return GitImportTask(**data)
    
    def __lt__(self, other):
        if self.priority.value != other.priority.value:
            return self.priority.value < other.priority.value
        return self.created_at < other.created_at


class AsyncTaskQueue:
    """
    异步任务队列管理器
    基于 asyncio.PriorityQueue + 按项目隔离的调度策略
    - 同一个 project_name 只能有一个任务运行
    - 不同项目可以并行执行
    - SQLite 持久化
    """
    
    def __init__(self,
                 max_workers: int = 10,  # 最大并发项目数
                 cache_base_dir: str = str(CACHE_GIT_REPOS_PATH),
                 task_db_file: str = str(CACHE_ROOT_PATH / "git_tasks.json"),
                 task_db_sqlite_file: str = str(CACHE_BUSINESS_DB_PATH),
                 max_memory_mb: int = 1024):
        
        self.max_workers = max_workers
        self.cache_base_dir = cache_base_dir
        self.task_db_file = task_db_file
        self.task_db_sqlite_file = task_db_sqlite_file
        self.max_memory_mb = max_memory_mb
        
        os.makedirs(os.path.dirname(task_db_file), exist_ok=True)
        
        # 按项目隔离的队列 {project_name: asyncio.PriorityQueue}
        self.project_queues: Dict[str, asyncio.PriorityQueue] = {}
        self.global_queue: Optional[asyncio.PriorityQueue] = None  # 未指定 project_name 的任务
        
        self.tasks: Dict[str, GitImportTask] = {}
        self.cancel_events: Dict[str, asyncio.Event] = {}
        self.auto_submission_blocked = False
        self.auto_submission_block_reason = ""
        
        # 项目运行状态追踪 {project_name: current_running_task_id}
        self.running_tasks: Dict[str, str] = {}
        self.running_tasks_lock = asyncio.Lock()
        
        # 异步基础设施
        self.loop: Optional[asyncio.AbstractEventLoop] = None
        self.executor: Optional[ThreadPoolExecutor] = None
        self.worker_tasks: List[asyncio.Task] = []
        self.running = False
        self._shutdown_event: Optional[asyncio.Event] = None
        self.queue: Optional[asyncio.PriorityQueue] = None
        
        # SQLite
        self.task_repo = get_git_import_tasks_repo(self.task_db_sqlite_file)
        
        # 延迟初始化
        self._initialized = False
    
    def _lazy_init(self):
        """延迟初始化（在第一次使用时）"""
        if self._initialized:
            return
        
        self._initialized = True
        self._migrate_legacy_task_db_if_needed()
        self._log_task_db_diagnostics(stage="init")
        self._load_tasks()
    
    async def _ensure_initialized_async(self):
        """异步延迟初始化"""
        self._lazy_init()
        
        if self.queue is None:
            self.queue = asyncio.PriorityQueue()
        
        if self.loop is None:
            self.loop = asyncio.get_running_loop()
        
        if self.executor is None:
            self.executor = ThreadPoolExecutor(max_workers=self.max_workers)
        
        if self._shutdown_event is None:
            self._shutdown_event = asyncio.Event()
    
    def _generate_task_id(self) -> str:
        return str(uuid.uuid4())
    
    def submit_task(self, ctx: TaskSubmitContext) -> str:
        """同步提交接口（向后兼容）"""
        self._lazy_init()

        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

        if loop.is_running():
            # 主 loop 正在运行（FastAPI/uvicorn 场景）：
            # 用 run_coroutine_threadsafe 把协程提交到主 loop，
            # 确保 asyncio.PriorityQueue 在主 loop 里创建和使用，不跨 loop。
            import concurrent.futures
            future = asyncio.run_coroutine_threadsafe(self._submit_task_async(ctx), loop)
            return future.result(timeout=30)
        else:
            return loop.run_until_complete(self._submit_task_async(ctx))
    
    async def _submit_task_async(self, ctx: TaskSubmitContext) -> str:
        """异步提交任务"""
        await self._ensure_initialized_async()
        
        task_id = self._generate_task_id()
        params = ctx.to_task_dict()
        
        # 参数规范化
        normalized_task_type = str(params['task_type'] or "auto").strip().lower()
        if normalized_task_type not in {"auto", "full", "incremental"}:
            normalized_task_type = "auto"
        
        if self.auto_submission_blocked and normalized_task_type == "auto":
            raise RuntimeError(self.auto_submission_block_reason or "当前已阻断后续自动任务")
        
        try:
            normalized_drop_ratio = float(params['acceptance_max_drop_ratio'])
        except Exception:
            normalized_drop_ratio = 0.3
        normalized_drop_ratio = max(0.0, min(normalized_drop_ratio, 0.95))
        
        # 创建任务
        task = GitImportTask(
            task_id=task_id,
            repo_url=params['repo_url'],
            branch=params['branch'],
            repo_name=params['repo_name'],
            java_source_dir=(params['java_source_dir'] or None),
            project_name=params['project_name'],
            language=params['language'],
            languages=params['languages'],
            clear_database=params['clear_database'],
            commit_id=params['commit_id'],
            clone_timeout=params['clone_timeout'],
            git_config=params['git_config'],
            include_unchanged_total=params['include_unchanged_total'],
            include_comment_nodes=params['include_comment_nodes'],
            maven_scan_enabled=bool(params['maven_scan_enabled']),
            force_maven=bool(params['force_maven']),
            auto_link_external=bool(params['auto_link_external']),
            app_version=str(params['app_version'] or ""),
            task_type=normalized_task_type,
            acceptance_enabled=bool(params.get('acceptance_enabled', True)),
            acceptance_block_on_fail=bool(params.get('acceptance_block_on_fail', False)),
            acceptance_max_drop_ratio=normalized_drop_ratio,
            priority=params['priority'],
        )
        
        # 保存
        try:
            self.task_repo.save(task.to_dict())
            logger.info(f"[TASK_SUBMIT] task_id={task.task_id} saved to sqlite")
        except Exception as e:
            logger.error(f"[TASK_SUBMIT] save_failed: {e}")
        
        # 存储
        self.tasks[task.task_id] = task
        self.cancel_events[task.task_id] = asyncio.Event()
        
        # 按项目名分发到对应队列
        project_name = task.project_name or "__global__"
        
        if project_name not in self.project_queues:
            self.project_queues[project_name] = asyncio.PriorityQueue()
        
        await self.project_queues[project_name].put((task.priority.value, task))
        
        logger.info(f"[TASK_SUBMIT] task_id={task.task_id} project={project_name} priority={task.priority.name} queued")
        return task.task_id
    
    async def _worker(self):
        """
        工作线程 - 按项目隔离的调度器
        每个项目一个队列，不同项目可以并行执行
        """
        logger.info(f"[WORKER] started with max_workers={self.max_workers}")
        
        # 为每个项目创建独立的 worker coroutine
        project_workers = []
        
        while self.running:
            try:
                # 检查是否有新的项目队列
                for project_name in list(self.project_queues.keys()):
                    if project_name not in [w.get_name() for w in project_workers if hasattr(w, 'get_name')]:
                        # 创建新的项目 worker
                        worker = asyncio.create_task(
                            self._project_worker(project_name),
                            name=f"worker-{project_name}"
                        )
                        project_workers.append(worker)
                
                # 限制并发数
                if len(project_workers) > self.max_workers:
                    # 等待一些 worker 完成
                    done, pending = await asyncio.wait(
                        project_workers,
                        timeout=0.1,
                        return_when=asyncio.FIRST_COMPLETED
                    )
                    project_workers = list(pending)
                
                await asyncio.sleep(0.1)
                
            except asyncio.CancelledError:
                logger.info("[WORKER] main loop cancelled")
                break
            except Exception as e:
                logger.error(f"[WORKER] error: {e}")
                await asyncio.sleep(1)
        
        # 等待所有项目 worker 完成
        if project_workers:
            logger.info(f"[WORKER] waiting for {len(project_workers)} project workers to finish")
            await asyncio.gather(*project_workers, return_exceptions=True)
        
        logger.info("[WORKER] stopped")
    
    async def _project_worker(self, project_name: str):
        """
        单个项目的任务处理器
        确保同一个项目只有一个任务在运行
        """
        logger.info(f"[PROJECT_WORKER] {project_name} started")
        
        queue = self.project_queues.get(project_name)
        if not queue:
            logger.warning(f"[PROJECT_WORKER] {project_name} queue not found")
            return
        
        while self.running:
            try:
                # 检查该项目是否有任务在运行
                async with self.running_tasks_lock:
                    if project_name in self.running_tasks:
                        # 已有任务在运行，等待
                        await asyncio.sleep(0.5)
                        continue
                
                # 从队列获取任务
                try:
                    priority, task = await asyncio.wait_for(
                        queue.get(),
                        timeout=1.0
                    )
                except asyncio.TimeoutError:
                    continue
                
                # 标记为运行中
                async with self.running_tasks_lock:
                    self.running_tasks[project_name] = task.task_id
                
                logger.info(f"[PROJECT_WORKER] {project_name} executing task_id={task.task_id} priority={priority}")
                
                # 执行任务
                await self._execute_task(task)
                
                # 标记完成
                queue.task_done()
                
                async with self.running_tasks_lock:
                    if project_name in self.running_tasks:
                        del self.running_tasks[project_name]
                
            except asyncio.CancelledError:
                logger.info(f"[PROJECT_WORKER] {project_name} cancelled")
                break
            except Exception as e:
                logger.error(f"[PROJECT_WORKER] {project_name} error: {e}")
                await asyncio.sleep(1)
        
        logger.info(f"[PROJECT_WORKER] {project_name} stopped")
    
    async def _execute_task(self, task: GitImportTask):
        """执行任务（在后台线程）"""
        task.status = TaskStatus.RUNNING
        task.started_at = datetime.now().isoformat()
        
        try:
            # 在线程池中执行同步代码
            await self.loop.run_in_executor(
                self.executor,
                self._execute_task_sync,
                task
            )
            
            task.status = TaskStatus.SUCCESS
            
        except ImportCancelled:
            task.status = TaskStatus.CANCELLED
            logger.info(f"[EXECUTE] task_id={task.task_id} cancelled")
            
        except Exception as e:
            task.status = TaskStatus.FAILED
            task.error = str(e)
            logger.error(f"[EXECUTE] task_id={task.task_id} failed: {e}")
            
        finally:
            task.completed_at = datetime.now().isoformat()
            
            try:
                self.task_repo.save(task.to_dict())
            except Exception as e:
                logger.error(f"[EXECUTE] save_state_failed: {e}")
    
    def _execute_task_sync(self, task: GitImportTask):
        """同步执行任务（在线程池中运行）"""
        from core.importer import GitToNeo4jImporter, ImportCancelled as _ImportCancelled
        from typing import Optional as _Opt, Dict as _Dict, Any as _Any

        logger.info(f"[EXECUTE] 开始执行任务 {task.task_id}")

        importer = GitToNeo4jImporter(cache_base_dir=self.cache_base_dir)
        if not importer.connect():
            raise Exception("无法连接 Neo4j")

        try:
            project_for_snapshot = (task.project_name or task.repo_name or "").strip()
            snapshot_before = self._capture_project_snapshot(None, project_for_snapshot)
            logger.info(
                f"[SNAPSHOT][BEFORE] project={project_for_snapshot} "
                f"nodes={snapshot_before.get('node_count', 0)} rels={snapshot_before.get('relationship_count', 0)}"
            )

            cancel_event = self.cancel_events.get(task.task_id)
            result = importer.import_from_git(
                repo_url=task.repo_url,
                branch=task.branch,
                repo_name=task.repo_name,
                java_source_dir=task.java_source_dir,
                project_name=task.project_name,
                language=task.language,
                languages=task.languages,
                clear_database=task.clear_database,
                include_unchanged_total=task.include_unchanged_total,
                include_comment_nodes=task.include_comment_nodes,
                clone_timeout=task.clone_timeout,
                git_config=task.git_config,
                commit_id=task.commit_id,
                maven_scan_enabled=bool(getattr(task, "maven_scan_enabled", True)),
                force_maven=bool(getattr(task, "force_maven", False)),
                auto_link_external=bool(getattr(task, "auto_link_external", True)),
                app_version=str(getattr(task, "app_version", "") or ""),
                task_type=str(getattr(task, "task_type", "auto") or "auto"),
                cancel_event=cancel_event,
            )

            snapshot_after = self._capture_project_snapshot(None, project_for_snapshot)
            logger.info(
                f"[SNAPSHOT][AFTER] project={project_for_snapshot} "
                f"nodes={snapshot_after.get('node_count', 0)} rels={snapshot_after.get('relationship_count', 0)}"
            )

            acceptance = self._build_acceptance_report(
                task=task,
                import_success=bool(result.get("success")),
                snapshot_before=snapshot_before,
                snapshot_after=snapshot_after,
            )
            extra = result.get("extra")
            if not isinstance(extra, dict):
                extra = {}
            extra["snapshot_before"] = snapshot_before
            extra["snapshot_after"] = snapshot_after
            extra["snapshot_delta"] = acceptance.get("delta", {})
            extra["acceptance"] = acceptance
            result["extra"] = extra

            logger.info(
                f"[ACCEPTANCE] task={task.task_id} status={acceptance.get('status')} "
                f"summary={acceptance.get('summary')}"
            )

            if task.status == TaskStatus.CANCELLED:
                task.completed_at = datetime.now().isoformat()
                return

            if result.get("success"):
                task.result = result
                logger.info(f"[EXECUTE] 任务成功: {task.task_id}")
            else:
                task.error = result.get("error", "未知错误")
                logger.error(f"[EXECUTE] 任务失败: {task.task_id} - {task.error}")
                self._cleanup_if_no_graph_project(task)

            task.completed_at = datetime.now().isoformat()
            self._persist_task_delta_detail(task.task_id, result)

        except _ImportCancelled as e:
            logger.info(f"[EXECUTE] 任务取消: {task.task_id} - {e}")
            task.status = TaskStatus.CANCELLED
            task.error = str(e)
            task.completed_at = datetime.now().isoformat()
            raise

        except Exception as e:
            logger.error(f"[EXECUTE] 任务异常: {task.task_id} - {e}", exc_info=True)
            # 重试逻辑
            if task.retry_count < task.max_retries:
                task.retry_count += 1
                task.status = TaskStatus.PENDING
                logger.info(f"[EXECUTE] 任务重试 ({task.retry_count}/{task.max_retries}): {task.task_id}")
                project_name = task.project_name or "__global__"
                if project_name in self.project_queues:
                    import asyncio as _asyncio
                    try:
                        loop = _asyncio.get_event_loop()
                        if loop.is_running():
                            loop.call_soon_threadsafe(
                                lambda: _asyncio.ensure_future(
                                    self.project_queues[project_name].put((task.priority.value, task))
                                )
                            )
                    except Exception:
                        pass
            else:
                task.status = TaskStatus.FAILED
                task.error = str(e)
                task.completed_at = datetime.now().isoformat()
                logger.error(f"[EXECUTE] 任务最终失败: {task.task_id}")
                self._cleanup_if_no_graph_project(task)
            raise

        finally:
            try:
                importer.disconnect()
            except Exception:
                pass
            try:
                self.task_repo.save(task.to_dict())
            except Exception as e:
                logger.error(f"[EXECUTE] save_state_failed: {e}")
    
    def start(self):
        """启动 worker"""
        self._lazy_init()
        
        if self.running:
            return
        
        # 确保有 loop
        try:
            self.loop = asyncio.get_event_loop()
        except RuntimeError:
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)
        
        # 初始化队列和执行器（如果还未初始化）
        if not hasattr(self, 'project_queues') or not self.project_queues:
            self.project_queues = {}
        
        if not hasattr(self, 'global_queue') or self.global_queue is None:
            self.global_queue = asyncio.PriorityQueue()
        
        if not hasattr(self, 'executor') or self.executor is None:
            self.executor = ThreadPoolExecutor(max_workers=self.max_workers)
        
        if not hasattr(self, 'running_tasks'):
            self.running_tasks = {}
        
        self.running = True
        self._shutdown_event = asyncio.Event()
        
        # 启动 worker
        self.worker_task = self.loop.create_task(self._worker())
        
        logger.info(f"[START] worker started")
    
    def shutdown(self, wait: bool = True):
        """关闭 worker"""
        if not self.running:
            return
        
        self.running = False
        
        if self.worker_task:
            self.worker_task.cancel()
            if wait:
                try:
                    self.loop.run_until_complete(self.worker_task)
                except (asyncio.CancelledError, RuntimeError):
                    pass
        
        if self.executor:
            self.executor.shutdown(wait=wait)
        
        logger.info("[SHUTDOWN] worker stopped")

    # ── 辅助方法（从 backup 移植）────────────────────────────────────────────

    def _capture_project_snapshot(self, _connector_unused: Any, project_name: str) -> Dict[str, Any]:
        """采集项目子图快照，通过 neomodel 查询（不依赖 connector）。"""
        try:
            from storage.neo4j.dao.project_neo_dao import ProjectNeoDao
            return ProjectNeoDao().capture_project_snapshot(project_name)
        except Exception as e:
            return {
                "project_name": project_name, "node_count": 0, "relationship_count": 0,
                "project_node_count": 0, "node_count_by_label": {}, "relationship_count_by_type": {},
                "error": str(e),
            }

    @staticmethod
    def _delta(after: Dict[str, Any], before: Dict[str, Any], key: str) -> int:
        try:
            return int(after.get(key) or 0) - int(before.get(key) or 0)
        except Exception:
            return 0

    @staticmethod
    def _delta_count_map(after_map: Any, before_map: Any) -> Dict[str, int]:
        a = after_map if isinstance(after_map, dict) else {}
        b = before_map if isinstance(before_map, dict) else {}
        out: Dict[str, int] = {}
        for k in sorted(set(a.keys()) | set(b.keys())):
            try:
                d = int(a.get(k, 0) or 0) - int(b.get(k, 0) or 0)
            except Exception:
                d = 0
            if d != 0:
                out[str(k)] = d
        return out

    def _build_acceptance_report(self, *, task: "GitImportTask", import_success: bool,
                                  snapshot_before: Optional[Dict[str, Any]],
                                  snapshot_after: Optional[Dict[str, Any]]) -> Dict[str, Any]:
        before = snapshot_before or {}
        after = snapshot_after or {}
        delta = {
            "node_count": self._delta(after, before, "node_count"),
            "relationship_count": self._delta(after, before, "relationship_count"),
            "node_count_by_label": self._delta_count_map(after.get("node_count_by_label"), before.get("node_count_by_label")),
            "relationship_count_by_type": self._delta_count_map(after.get("relationship_count_by_type"), before.get("relationship_count_by_type")),
        }
        report: Dict[str, Any] = {
            "enabled": bool(task.acceptance_enabled), "status": "skipped",
            "summary": "验收未执行", "rules": [], "delta": delta,
            "max_drop_ratio": float(task.acceptance_max_drop_ratio),
            "block_on_fail": bool(task.acceptance_block_on_fail),
            "blocked_following_auto_tasks": False,
        }
        if not task.acceptance_enabled:
            report["summary"] = "验收开关关闭，已跳过"; return report
        if not import_success:
            report["summary"] = "导入失败，验收跳过"; return report

        rules: List[Dict[str, Any]] = []
        def add_rule(rid: str, title: str, ok: bool, detail: str) -> None:
            rules.append({"id": rid, "title": title, "status": "pass" if ok else "fail", "ok": bool(ok), "detail": detail})

        add_rule("project-node-exists", "Project 节点存在", int(after.get("project_node_count") or 0) >= 1, f"after.project_node_count={int(after.get('project_node_count') or 0)}")
        add_rule("subgraph-node-not-empty", "项目子图节点非空", int(after.get("node_count") or 0) > 0, f"after.node_count={int(after.get('node_count') or 0)}")
        add_rule("subgraph-rel-not-empty", "项目子图关系非空", int(after.get("relationship_count") or 0) > 0, f"after.relationship_count={int(after.get('relationship_count') or 0)}")

        max_drop = float(task.acceptance_max_drop_ratio)
        if not bool(task.clear_database):
            bn = int(before.get("node_count") or 0)
            an = int(after.get("node_count") or 0)
            if bn > 0:
                dr = max(0.0, (bn - an) / bn)
                add_rule("node-drop-ratio", "节点降幅阈值", dr <= max_drop, f"drop={dr:.2%}, threshold={max_drop:.2%}")
            br = int(before.get("relationship_count") or 0)
            ar = int(after.get("relationship_count") or 0)
            if br > 0:
                dr = max(0.0, (br - ar) / br)
                add_rule("rel-drop-ratio", "关系降幅阈值", dr <= max_drop, f"drop={dr:.2%}, threshold={max_drop:.2%}")

        passed = all(bool(r.get("ok")) for r in rules)
        report["rules"] = rules
        report["status"] = "passed" if passed else "failed"
        report["summary"] = "验收通过" if passed else "验收失败：存在关键规则未通过"
        if (not passed) and bool(task.acceptance_block_on_fail):
            self.auto_submission_blocked = True
            self.auto_submission_block_reason = f"任务 {task.task_id} 验收失败，已阻断后续 auto 任务"
            report["blocked_following_auto_tasks"] = True
        return report

    def _persist_task_delta_detail(self, task_id: str, result: Dict[str, Any]) -> None:
        if not task_id or not isinstance(result, dict):
            return
        try:
            extra = result.get("extra")
            if not isinstance(extra, dict):
                return
            detail = {k: extra.get(k) for k in (
                "snapshot_before", "snapshot_after", "snapshot_delta", "acceptance",
                "delta_detail", "task_type", "effective_mode", "fallback_reason",
                "changed_files_list", "added_files_list", "deleted_files_list",
            )}
            self.task_repo.upsert_task_delta_detail(task_id, detail)  # type: ignore[attr-defined]
        except Exception as e:
            logger.warning(f"[ACCEPTANCE] 持久化任务 diff 明细失败 task={task_id}: {e}")

    def _cleanup_if_no_graph_project(self, task: "GitImportTask") -> None:
        import shutil
        repo_name = (task.repo_name or task.project_name or "").strip()
        project_name = (task.project_name or repo_name).strip()
        if not repo_name:
            return
        try:
            from storage.neo4j.dao.project_neo_dao import ProjectNeoDao
            if ProjectNeoDao().project_exists(project_name):
                return
        except Exception as e:
            logger.warning(f"[CLEANUP] 检查 Neo4j Project 节点失败，跳过清理: {e}")
            return
        cache_base = Path(self.cache_base_dir)
        cache_root = cache_base.parent
        targets = [cache_base / repo_name, cache_root / "metadata" / f"{repo_name}.json"]
        merkle_dir = cache_root / "merkle_trees"
        if merkle_dir.exists():
            targets += list(merkle_dir.glob(f"{repo_name}_*.json"))
        for target in [Path(t) for t in targets]:
            try:
                if target.is_dir():
                    shutil.rmtree(target, ignore_errors=True)
                elif target.is_file():
                    target.unlink(missing_ok=True)
            except Exception as e:
                logger.warning(f"[CLEANUP] 删除 {target} 失败: {e}")

    def get_all_tasks(self, status: Optional[TaskStatus] = None) -> List[Dict]:
        """获取所有任务"""
        tasks = list(self.tasks.values())
        
        if status:
            tasks = [t for t in tasks if t.status == status]
        
        tasks = sorted(tasks, key=lambda t: t.created_at, reverse=True)
        return [t.to_dict() for t in tasks]
    
    def get_queue_stats(self) -> Dict:
        """获取统计信息"""
        pending_count = sum(1 for t in self.tasks.values() if str(t.status).lower() in ['pending', 'TaskStatus.PENDING'])
        running_count = sum(1 for t in self.tasks.values() if str(t.status).lower() in ['running', 'TaskStatus.RUNNING'])
        
        # 按项目的统计（如果队列已初始化）
        project_stats = {}
        if hasattr(self, 'project_queues') and self.project_queues:
            for project_name, queue in self.project_queues.items():
                try:
                    project_stats[project_name] = {
                        "queue_size": queue.qsize() if hasattr(queue, 'qsize') else 0,
                        "running": project_name in (self.running_tasks if hasattr(self, 'running_tasks') else {}),
                    }
                except Exception:
                    # 队列访问失败，跳过
                    pass
        
        return {
            "pending": pending_count,
            "running": running_count,
            "total_projects": len(self.project_queues) if hasattr(self, 'project_queues') else 0,
            "active_projects": len(self.running_tasks) if hasattr(self, 'running_tasks') else 0,
            "max_workers": self.max_workers,
            "projects": project_stats,
        }
    
    # ===== 以下方法需要从旧版本迁移 =====
    
    def _migrate_legacy_task_db_if_needed(self):
        """
        兼容迁移：历史版本任务队列使用 .cache/git_tasks.db。
        新版本默认统一到 business.db；启动时自动做一次幂等 upsert 迁移。
        """
        legacy_db_path = Path(CACHE_ROOT_PATH / "git_tasks.db").resolve()
        current_db_path = Path(self.task_db_sqlite_file).resolve()
        
        if current_db_path == legacy_db_path:
            return
        if not legacy_db_path.exists():
            return
        
        legacy_conn: Optional[sqlite3.Connection] = None
        try:
            legacy_conn = sqlite3.connect(str(legacy_db_path))
            legacy_conn.row_factory = sqlite3.Row
            cur = legacy_conn.cursor()
            
            table_rows = cur.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ).fetchall()
            tables = {str(r["name"]) for r in table_rows}
            
            migrated_tasks = 0
            migrated_delta_details = 0
            
            if "git_import_tasks" in tables:
                src_rows = cur.execute(
                    """
                    SELECT task_id, status, priority, created_at, started_at, completed_at,
                           retry_count, max_retries, task_json, updated_at
                    FROM git_import_tasks
                    """
                ).fetchall()
                
                upsert_rows: List[Dict[str, Any]] = []
                for r in src_rows:
                    payload = r["task_json"]
                    try:
                        task_obj = json.loads(payload) if payload else {}
                    except Exception:
                        task_obj = {"_task_json": payload}
                    
                    upsert_rows.append({
                        "task_id": r["task_id"],
                        "status": r["status"],
                        "priority": r["priority"],
                        "created_at": r["created_at"] or datetime.now().isoformat(),
                        "started_at": r["started_at"],
                        "completed_at": r["completed_at"],
                        "retry_count": int(r["retry_count"] or 0),
                        "max_retries": int(r["max_retries"] or 3),
                        "task_obj": task_obj,
                        "updated_at": r["updated_at"] or datetime.now().isoformat(),
                    })
                
                if upsert_rows:
                    self.task_repo.upsert_many(upsert_rows)
                    migrated_tasks = len(upsert_rows)
            
            if "import_task_delta_details" in tables:
                detail_rows = cur.execute(
                    "SELECT task_id, detail_json FROM import_task_delta_details"
                ).fetchall()
                
                for r in detail_rows:
                    try:
                        detail = json.loads(r["detail_json"]) if r["detail_json"] else {}
                    except Exception:
                        detail = {}
                    self.task_repo.upsert_task_delta_detail(str(r["task_id"]), detail)
                    migrated_delta_details += 1
            
            if migrated_tasks > 0 or migrated_delta_details > 0:
                logger.info(
                    f"[MIGRATE] 旧任务库迁移完成 legacy={legacy_db_path} -> current={current_db_path} "
                    f"(tasks={migrated_tasks}, delta_details={migrated_delta_details})"
                )
        
        except Exception as e:
            logger.warning(
                f"[MIGRATE] 旧任务库迁移失败 legacy={legacy_db_path} -> current={current_db_path}: {e}"
            )
        finally:
            if legacy_conn is not None:
                legacy_conn.close()
    
    def _log_task_db_diagnostics(self, stage: str):
        """
        固定诊断日志：用于快速确认任务队列实际连接库与迁移状态。
        """
        try:
            db = getattr(self.task_repo, "db", None)
            db_path = str(getattr(db, "db_path", self.task_db_sqlite_file))
            conn = getattr(db, "conn", None)
            if conn is None:
                logger.info(
                    f"[TASK_DB_DIAG] stage={stage} db_path={db_path} conn=none"
                )
                return

            cur = conn.cursor()
            # 检查表是否存在
            tables = set()
            for r in cur.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ).fetchall():
                tables.add(r[0])

            has_git_tasks = "git_import_tasks" in tables
            has_delta_details = "import_task_delta_details" in tables
            has_schema_migrations = "schema_migrations" in tables

            # 读取已迁移的版本号
            versions: List[int] = []
            if has_schema_migrations:
                rows = cur.execute(
                    "SELECT version FROM schema_migrations ORDER BY version"
                ).fetchall()
                for r in rows:
                    try:
                        versions.append(int(r[0]))
                    except Exception:
                        continue

            logger.info(
                f"[TASK_DB_DIAG] "
                f"stage={stage} "
                f"db_path={db_path} "
                f"has_git_import_tasks={has_git_tasks} "
                f"has_import_task_delta_details={has_delta_details} "
                f"has_schema_migrations={has_schema_migrations} "
                f"migration_versions={versions}"
            )
        except Exception as e:
            logger.warning(f"[TASK_DB_DIAG] stage={stage} diag_failed: {e}")
    
    def _load_tasks(self):
        """
        从 SQLite 加载 pending 和 running 状态的任务
        - running 任务 → 标记为 failed（因为进程重启了）
        - pending 任务 → 重新加入队列等待执行
        """
        try:
            # 查询所有 pending 和 running 任务
            pending_tasks = self.task_repo.load_tasks(statuses=["pending"])
            running_tasks = self.task_repo.load_tasks(statuses=["running"])
            
            logger.info(f"[LOAD_TASKS] found {len(pending_tasks)} pending, {len(running_tasks)} running tasks")
            
            # 处理 running 任务（进程重启了，这些任务实际上中断了）
            for task_dict in running_tasks:
                task_id = task_dict.get('task_id')
                project_name = task_dict.get('project_name', '__global__')
                
                logger.warning(
                    f"[LOAD_TASKS] task_id={task_id} project={project_name} "
                    f"was running but interrupted by restart, marking as failed"
                )
                
                # 标记为失败
                task_dict['status'] = 'failed'
                task_dict['error'] = 'Task interrupted by process restart'
                task_dict['completed_at'] = datetime.now().isoformat()
                
                # 保存到 SQLite
                self.task_repo.upsert_task(
                    task_id=task_id,
                    status='failed',
                    priority=task_dict.get('priority', 'NORMAL'),
                    created_at=task_dict.get('created_at'),
                    started_at=task_dict.get('started_at'),
                    completed_at=task_dict['completed_at'],
                    retry_count=task_dict.get('retry_count', 0),
                    max_retries=task_dict.get('max_retries', 3),
                    task_obj=task_dict,
                    updated_at=datetime.now().isoformat()
                )
            
            # 处理 pending 任务（重新加入队列）
            restored_count = 0
            for task_dict in pending_tasks:
                try:
                    # 从字典创建任务对象
                    task = GitImportTask.from_dict(task_dict)
                    
                    # 存储到内存
                    self.tasks[task.task_id] = task
                    self.cancel_events[task.task_id] = asyncio.Event()
                    
                    # 按项目名分发到对应队列
                    project_name = task.project_name or "__global__"
                    
                    if project_name not in self.project_queues:
                        self.project_queues[project_name] = asyncio.PriorityQueue()
                    
                    # 同步方法放入队列（此时 loop 可能还未运行）
                    import threading
                    result = [None]
                    exception = [None]
                    
                    def put_in_thread():
                        new_loop = asyncio.new_event_loop()
                        asyncio.set_event_loop(new_loop)
                        try:
                            new_loop.run_until_complete(
                                self.project_queues[project_name].put((task.priority.value, task))
                            )
                            result[0] = True
                        except Exception as e:
                            exception[0] = e
                        finally:
                            new_loop.close()
                    
                    thread = threading.Thread(target=put_in_thread)
                    thread.start()
                    thread.join()
                    
                    if exception[0]:
                        logger.error(f"[LOAD_TASKS] failed to restore task_id={task.task_id}: {exception[0]}")
                        continue
                    
                    restored_count += 1
                    logger.info(
                        f"[LOAD_TASKS] restored task_id={task.task_id} project={project_name} "
                        f"priority={task.priority.name} to queue"
                    )
                    
                except Exception as e:
                    logger.error(f"[LOAD_TASKS] failed to restore task: {e}")
            
            logger.info(f"[LOAD_TASKS] total restored {restored_count} pending tasks to queue")
            
        except Exception as e:
            logger.error(f"[LOAD_TASKS] error loading tasks: {e}")


# 全局单例
_task_queue_instance: Optional[AsyncTaskQueue] = None


def get_task_queue(**kwargs) -> AsyncTaskQueue:
    """
    获取或创建任务队列单例
    支持传入参数覆盖默认配置
    """
    global _task_queue_instance
    if _task_queue_instance is None:
        _task_queue_instance = AsyncTaskQueue(**kwargs)
    return _task_queue_instance


# 别名，保持 API 兼容
TaskQueue = AsyncTaskQueue

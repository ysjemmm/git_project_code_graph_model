#!/usr/bin/env python3

import json
import os
import sys
import threading
import time
from dataclasses import dataclass, asdict
from datetime import datetime
from enum import Enum
from pathlib import Path
from queue import PriorityQueue
from threading import Lock, Event
from typing import Dict, List, Optional

sys.path.insert(0, str(Path(__file__).parent.parent))

from core.importer import GitToNeo4jImporter, ImportCancelled
from parser.utils.logger import get_logger
from storage.sqlite.business.git_tasks_repo import get_git_import_tasks_repo

# 配置日志
logger = get_logger("git_task_queue")

class TaskStatus(Enum):
    """任务状态"""
    PENDING = "pending"          # 待处
    RUNNING = "running"          # 运行
    SUCCESS = "success"          # 成功
    FAILED = "failed"            # 失败
    CANCELLED = "cancelled"      # 已取

class TaskPriority(Enum):
    """任务优先"""
    LOW = 3
    NORMAL = 2
    HIGH = 1
    CRITICAL = 0

@dataclass
class GitImportTask:
    
    task_id: str
    repo_url: str
    branch: str = "main"
    repo_name: Optional[str] = None
    # 未显式指定时为 None，由语言适配器自动发现多模块源码目录
    java_source_dir: Optional[str] = None
    project_name: Optional[str] = None
    language: Optional[str] = None
    languages: Optional[List[str]] = None
    clear_database: bool = False
    commit_id: Optional[str] = None
    clone_timeout: Optional[int] = None
    git_config: Optional[Dict[str, str]] = None
    include_unchanged_total: bool = False
    include_comment_nodes: bool = False
    # 是否执行 Maven 扫描/解析 pom 外部依赖（用于 jar_classes.db 更新）
    maven_scan_enabled: bool = True
    # 强制重新解析 Maven（忽略 marker，并对依赖目录 force_rescan）
    force_maven: bool = False
    priority: TaskPriority = TaskPriority.NORMAL
    
    # 任务状
    status: TaskStatus = TaskStatus.PENDING
    created_at: str = None
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    
    # 结果
    result: Optional[Dict] = None
    error: Optional[str] = None
    
    # 重试
    retry_count: int = 0
    max_retries: int = 3
    
    def __post_init__(self):
        if self.created_at is None:
            self.created_at = datetime.now().isoformat()
    
    def to_dict(self) -> Dict:
        """转换为字典"""
        data = asdict(self)
        data['status'] = self.status.value
        data['priority'] = self.priority.value
        return data
    
    def __lt__(self, other):
        """用于优先级队列排序"""
        if self.priority.value != other.priority.value:
            return self.priority.value < other.priority.value
        return self.created_at < other.created_at

class TaskQueue:
    """任务队列管理"""
    
    def __init__(self,
                 max_workers: int = 2,
                 cache_base_dir: str = ".cache/git_repos",
                 task_db_file: str = ".cache/git_tasks.json",
                 task_db_sqlite_file: str = ".cache/git_tasks.db",
                 max_memory_mb: int = 1024):
        
        self.max_workers = max_workers
        self.cache_base_dir = cache_base_dir
        self.task_db_file = task_db_file
        self.task_db_sqlite_file = task_db_sqlite_file
        self.max_memory_mb = max_memory_mb
        
        # 创建缓存目录
        os.makedirs(os.path.dirname(task_db_file), exist_ok=True)
        
        # 任务队列
        self.task_queue: PriorityQueue = PriorityQueue()
        self.tasks: Dict[str, GitImportTask] = {}
        self.tasks_lock = Lock()
        self.cancel_events: Dict[str, Event] = {}
        
        # 工作线程
        self.workers: List[threading.Thread] = []
        self.running = False
        self.stop_event = Event()
        
        # 加载已保存的任务
        # 用 sqlite 作为业务持久化（替代/增强 JSON 文件），确保重启后任务可恢复
        self.task_repo = get_git_import_tasks_repo(self.task_db_sqlite_file)
        self._load_tasks()
    
    def submit_task(self,
                   repo_url: str,
                   branch: str = "main",
                   repo_name: Optional[str] = None,
                   java_source_dir: Optional[str] = None,
                   project_name: Optional[str] = None,
                   language: Optional[str] = None,
                   languages: Optional[List[str]] = None,
                   clear_database: bool = False,
                   commit_id: Optional[str] = None,
                   clone_timeout: Optional[int] = None,
                   git_config: Optional[Dict[str, str]] = None,
                   include_unchanged_total: bool = False,
                   include_comment_nodes: bool = False,
                   maven_scan_enabled: bool = True,
                   force_maven: bool = False,
                   priority: TaskPriority = TaskPriority.NORMAL) -> str:
        
        task_id = self._generate_task_id()
        
        task = GitImportTask(
            task_id=task_id,
            repo_url=repo_url,
            branch=branch,
            repo_name=repo_name,
            java_source_dir=(java_source_dir or None),
            project_name=project_name,
            language=language,
            languages=languages,
            clear_database=clear_database,
            commit_id=commit_id,
            clone_timeout=clone_timeout,
            git_config=git_config,
            include_unchanged_total=include_unchanged_total,
            include_comment_nodes=include_comment_nodes,
            maven_scan_enabled=bool(maven_scan_enabled),
            force_maven=bool(force_maven),
            priority=priority
        )
        
        with self.tasks_lock:
            self.tasks[task_id] = task
            self.cancel_events[task_id] = Event()
            self.task_queue.put((priority.value, task_id, task))
        
        logger.info(f"[SUBMIT] 任务已提 {task_id} ({repo_url})")
        
        # 保存任务
        self._save_tasks()
        
        return task_id
    
    def start(self):
        
        if self.running:
            logger.warning("任务队列已在运行")
            return
        
        self.running = True
        self.stop_event.clear()
        
        # 启动工作线程
        for i in range(self.max_workers):
            worker = threading.Thread(
                target=self._worker_loop,
                name=f"GitWorker-{i}",
                daemon=True
            )
            worker.start()
            self.workers.append(worker)
        
        logger.info(f"[START] 任务队列已启({self.max_workers} 个工作线")
    
    def stop(self, wait: bool = True):
        
        if not self.running:
            logger.warning("任务队列未运行")
            return
        
        logger.info("[STOP] 正在停止任务队列...")
        self.running = False
        self.stop_event.set()
        
        if wait:
            for worker in self.workers:
                worker.join(timeout=30)
            logger.info("[STOP] 任务队列已停止")
    
    def get_task_status(self, task_id: str) -> Optional[Dict]:
        """获取任务状态"""
        with self.tasks_lock:
            task = self.tasks.get(task_id)
            if task:
                return task.to_dict()
        return None
    
    def get_all_tasks(self, status: Optional[TaskStatus] = None) -> List[Dict]:
        """获取所有任务"""
        with self.tasks_lock:
            tasks = list(self.tasks.values())
            if status:
                tasks = [t for t in tasks if t.status == status]
            return [t.to_dict() for t in tasks]
    
    def cancel_task(self, task_id: str) -> bool:
        """
        取消任务：
        - PENDING：立即标记 CANCELLED，worker 取到时会跳过
        - RUNNING：设置 cancel_event，任务会在可中断点尽快退出并标记 CANCELLED
        """
        with self.tasks_lock:
            task = self.tasks.get(task_id)
            if not task:
                return False

            ev = self.cancel_events.get(task_id)
            if ev is None:
                ev = Event()
                self.cancel_events[task_id] = ev
            ev.set()

            if task.status in (TaskStatus.PENDING, TaskStatus.RUNNING):
                task.status = TaskStatus.CANCELLED
                task.error = "cancelled by user"
                if task.completed_at is None:
                    task.completed_at = datetime.now().isoformat()
                logger.info(f"[CANCEL] 任务已取消 {task_id}（原状态={task.status.value}）")
                self._save_tasks()
                return True
        return False
    
    def get_queue_stats(self) -> Dict:
        
        with self.tasks_lock:
            total = len(self.tasks)
            pending = sum(1 for t in self.tasks.values() if t.status == TaskStatus.PENDING)
            running = sum(1 for t in self.tasks.values() if t.status == TaskStatus.RUNNING)
            success = sum(1 for t in self.tasks.values() if t.status == TaskStatus.SUCCESS)
            failed = sum(1 for t in self.tasks.values() if t.status == TaskStatus.FAILED)
            
            return {
                'total': total,
                'pending': pending,
                'running': running,
                'success': success,
                'failed': failed,
                'queue_size': self.task_queue.qsize(),
                'max_workers': self.max_workers,
                'cache_base_dir': self.cache_base_dir,
                'max_memory_mb': self.max_memory_mb
            }
    
    def _worker_loop(self):
        """工作线程主循环"""

        logger.info(f"[WORKER] {threading.current_thread().name} 已启动")
        
        while self.running and not self.stop_event.is_set():
            try:
                # 获取任务(超1 秒)
                try:
                    priority, task_id, task = self.task_queue.get(timeout=1)
                except:
                    continue
                
                # 检查任务是否被取消
                with self.tasks_lock:
                    if task.status == TaskStatus.CANCELLED:
                        logger.info(f"[WORKER] 跳过已取消的任务: {task_id}")
                        continue
                
                # 检查内
                if not self._check_memory:
                    logger.warning(f"[WORKER] 内存不足,任{task_id} 重新入队")
                    self.task_queue.put((priority, task_id, task))
                    time.sleep(5)  # 等待 5 秒后重试
                    continue
                
                # 执行任务
                self._execute_task(task)
                
            except Exception as e:
                logger.error(f"[WORKER] 工作线程异常: {e}", exc_info=True)
        
        logger.info(f"[WORKER] {threading.current_thread().name} 已停止")
    
    def _execute_task(self, task: GitImportTask):
        
        logger.info(f"[EXECUTE] 开始执行任 {task.task_id}")
        
        try:
            # 更新任务状
            with self.tasks_lock:
                task.status = TaskStatus.RUNNING
                task.started_at = datetime.now().isoformat()
            
            # 创建导入器（Neo4j 配置读取优先级：环境变量 > .env.local > .env > 默认值）
            # 线上/生产环境请务必通过环境变量或 secret 注入 Neo4j 凭据，避免硬编码。
            importer = GitToNeo4jImporter(cache_base_dir=self.cache_base_dir)
            
            # 连接Neo4j
            if not importer.connect():
                raise Exception("无法连接Neo4j")
            
            try:
                # 执行导入
                cancel_event = None
                with self.tasks_lock:
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
                    cancel_event=cancel_event,
                )
                
                # 更新任务状
                with self.tasks_lock:
                    # 若用户已取消，则不覆盖状态
                    if task.status == TaskStatus.CANCELLED:
                        task.completed_at = datetime.now().isoformat()
                        logger.info(f"[EXECUTE] 任务已取消: {task.task_id}")
                        return
                    if result['success']:
                        task.status = TaskStatus.SUCCESS
                        task.result = result
                        logger.info(f"[EXECUTE] 任务成功: {task.task_id}")
                    else:
                        task.status = TaskStatus.FAILED
                        task.error = result.get('error', '未知错误')
                        logger.error(f"[EXECUTE] 任务失败: {task.task_id} - {task.error}")
                        self._cleanup_if_no_graph_project(task)
                    
                    task.completed_at = datetime.now().isoformat()
            
            finally:
                importer.disconnect()
        
        except ImportCancelled as e:
            logger.info(f"[EXECUTE] 任务取消: {task.task_id} - {e}")
            with self.tasks_lock:
                task.status = TaskStatus.CANCELLED
                task.error = str(e)
                task.completed_at = datetime.now().isoformat()
                self._save_tasks()
            return
        except Exception as e:
            logger.error(f"[EXECUTE] 任务异常: {task.task_id} - {e}", exc_info=True)
            
            # 兜底自愈：Windows 下偶发出现 clone/checkout 后 pom.xml 变成 0 字节，
            # Maven 会报 "Non-readable POM ... input contained no data"。
            # 此时重试若不清理缓存仓库，会重复失败；因此在进入重试前删除该 repo 目录，强制下次重克隆。
            try:
                msg = str(e) or ""
                if ("Non-readable POM" in msg) and ("input contained no data" in msg):
                    repo_name = (task.repo_name or task.project_name or "").strip()
                    if repo_name:
                        import shutil

                        repo_dir = Path(self.cache_base_dir) / repo_name
                        if repo_dir.exists():
                            shutil.rmtree(repo_dir, ignore_errors=True)
                            logger.warning(f"[CLEANUP] 检测到空 pom.xml，已删除损坏仓库缓存以便重试重克隆: {repo_dir}")
            except Exception as _cleanup_err:
                logger.warning(f"[CLEANUP] 尝试清理损坏仓库缓存失败，继续走重试逻辑: {_cleanup_err}")

            # 重试逻辑
            with self.tasks_lock:
                if task.retry_count < task.max_retries:
                    task.retry_count += 1
                    task.status = TaskStatus.PENDING
                    logger.info(f"[EXECUTE] 任务重试 ({task.retry_count}/{task.max_retries}): {task.task_id}")
                    # 重新入队
                    self.task_queue.put((task.priority.value, task.task_id, task))
                else:
                    task.status = TaskStatus.FAILED
                    task.error = str(e)
                    task.completed_at = datetime.now().isoformat()
                    logger.error(f"[EXECUTE] 任务最终失 {task.task_id}")
                    self._cleanup_if_no_graph_project(task)
        
        finally:
            # 保存任务
            self._save_tasks()

    def _cleanup_if_no_graph_project(self, task):
            """任务失败后：若 Neo4j 中不存在对应 Project 节点，则删除本地缓存（git_repos/xxx、merkle、metadata）。"""
            import shutil

            repo_name = (task.repo_name or task.project_name or "").strip()
            project_name = (task.project_name or repo_name).strip()
            if not repo_name:
                return

            # 1) 检查 Neo4j 是否有 Project 节点
            has_project = False
            try:
                from core.env_loader import load_env_vars
                load_env_vars({"NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD", "NEO4J_DATABASE"})
                uri = os.environ.get("NEO4J_URI", "")
                user = os.environ.get("NEO4J_USER", "neo4j")
                password = os.environ.get("NEO4J_PASSWORD", "")
                database = os.environ.get("NEO4J_DATABASE", "neo4j")
                if uri and password:
                    from storage.neo4j.connector import Neo4jConnector
                    conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
                    if conn.connect():
                        try:
                            rows = conn.execute_query(
                                "MATCH (p:Project) WHERE p.name = $name AND p.project_type = 'Application' RETURN p LIMIT 1",
                                {"name": project_name},
                            ) or []
                            has_project = len(rows) > 0
                        finally:
                            conn.disconnect()
            except Exception as e:
                logger.warning(f"[CLEANUP] 检查 Neo4j Project 节点失败，跳过清理: {e}")
                return

            if has_project:
                logger.info(f"[CLEANUP] Neo4j 中存在 Project({project_name})，保留本地缓存")
                return

            # 2) 删除本地缓存
            cache_base = Path(self.cache_base_dir)
            cache_root = cache_base.parent

            targets = [
                cache_base / repo_name,
                cache_root / "metadata" / f"{repo_name}.json",
            ]
            merkle_dir = cache_root / "merkle_trees"
            if merkle_dir.exists():
                targets += list(merkle_dir.glob(f"{repo_name}_*.json"))

            for target in targets:
                target = Path(target)
                try:
                    if target.is_dir():
                        shutil.rmtree(target, ignore_errors=True)
                        logger.info(f"[CLEANUP] 已删除目录: {target}")
                    elif target.is_file():
                        target.unlink(missing_ok=True)
                        logger.info(f"[CLEANUP] 已删除文件: {target}")
                except Exception as e:
                    logger.warning(f"[CLEANUP] 删除 {target} 失败: {e}")
    
    @property
    def _check_memory(self) -> bool:
        """检查内存是否充足"""
        try:
            import psutil
            memory_mb = psutil.virtual_memory().used / (1024 * 1024)
            
            if memory_mb > self.max_memory_mb:
                logger.warning(f"[MEMORY] 内存使用过高: {memory_mb:.2f}MB / {self.max_memory_mb}MB")
                return False
            
            return True
        except ImportError:
            # 如果没有 psutil,默认返True
            return True
    
    def _generate_task_id(self) -> str:
        
        import uuid
        return f"task_{uuid.uuid4().hex[:8]}"
    
    def _save_tasks(self):
        """保存任务（sqlite + 兼容 JSON 文件备份）"""
        try:
            now = datetime.now().isoformat()
            with self.tasks_lock:
                tasks = list(self.tasks.items())

            # 1) 写 sqlite：业务持久化
            rows = []
            for task_id, task in tasks:
                d = task.to_dict()
                rows.append(
                    {
                        "task_id": task_id,
                        "status": d["status"],
                        "priority": d["priority"],
                        "created_at": d.get("created_at") or now,
                        "started_at": d.get("started_at"),
                        "completed_at": d.get("completed_at"),
                        "retry_count": int(d.get("retry_count", 0)),
                        "max_retries": int(d.get("max_retries", 3)),
                        "task_obj": d,
                        "updated_at": now,
                    }
                )
            if rows:
                self.task_repo.upsert_many(rows)

            # 2) 写 JSON 兼容备份：便于你随时回退/排查
            tasks_data = {
                "tasks": {k: v.to_dict() for k, v in self.tasks.items()},
                "saved_at": now,
            }
            with open(self.task_db_file, "w", encoding="utf-8") as f:
                json.dump(tasks_data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.error(f"[SAVE] 保存任务失败: {e}")
    
    def _load_tasks(self):
        """从 sqlite/JSON 加载未完成任务"""
        try:
            pending_running = ["pending", "running"]

            # 1) 优先：sqlite
            task_dicts = self.task_repo.load_tasks(statuses=pending_running)
            if task_dicts:
                for td in task_dicts:
                    # 兜底：如果 task_json 里没有显式 task_id，就跳过
                    task_id = td.get("task_id")
                    if not task_id:
                        continue
                    if td.get("status") not in pending_running:
                        continue
                    task = self._dict_to_task(td)
                    self.tasks[task_id] = task
                    # 重新入队 + 重建 cancel_event（运行时用）
                    self.cancel_events[task_id] = Event()
                    self.task_queue.put((task.priority.value, task_id, task))

                logger.info(f"[LOAD] 从 sqlite 加载 {len(self.tasks)} 个任务")
                return

            # 2) 兼容：json
            if os.path.isfile(self.task_db_file):
                with open(self.task_db_file, "r", encoding="utf-8") as f:
                    tasks_data = json.load(f)

                for task_id, task_dict in tasks_data.get("tasks", {}).items():
                    # 只加载未完成的任务
                    if task_dict.get("status") in pending_running:
                        task = self._dict_to_task(task_dict)
                        self.tasks[task_id] = task
                        # 重新入队
                        self.cancel_events[task_id] = Event()
                        self.task_queue.put((task.priority.value, task_id, task))

                logger.info(f"[LOAD] 从 json 加载 {len(self.tasks)} 个任务")
        except Exception as e:
            logger.error(f"[LOAD] 加载任务失败: {e}")
    
    @staticmethod
    def _dict_to_task(data: Dict) -> GitImportTask:
        
        data['status'] = TaskStatus(data['status'])
        data['priority'] = TaskPriority(data['priority'])
        return GitImportTask(**data)

# 全局任务队列实例
_global_task_queue: Optional[TaskQueue] = None

def get_task_queue(max_workers: int = 2,
                   cache_base_dir: str = ".cache/git_repos",
                   task_db_file: str = ".cache/git_tasks.json",
                   task_db_sqlite_file: str = ".cache/git_tasks.db",
                   max_memory_mb: int = 1024) -> TaskQueue:
    
    global _global_task_queue
    
    if _global_task_queue is None:
        _global_task_queue = TaskQueue(
            max_workers=max_workers,
            cache_base_dir=cache_base_dir,
            task_db_file=task_db_file,
            task_db_sqlite_file=task_db_sqlite_file,
            max_memory_mb=max_memory_mb
        )
    
    return _global_task_queue

def main():
    print("=" * 70)
    print("Git 导入任务队列 - 示例")
    print("=" * 70)
    
    # 创建任务队列
    queue = get_task_queue(
        max_workers=2,
        cache_base_dir=".cache/git_repos",
        max_memory_mb=1024
    )
    
    # 启动队列
    queue.start()
    
    try:
        # 提交任务
        task_id_1 = queue.submit_task(
            repo_url="https://github.com/example/java-project.git",
            branch="main",
            priority=TaskPriority.HIGH
        )
        print(f"[SUBMIT] 任务 1: {task_id_1}")
        
        task_id_2 = queue.submit_task(
            repo_url="https://github.com/example/another-project.git",
            branch="dev",
            priority=TaskPriority.NORMAL
        )
        print(f"[SUBMIT] 任务 2: {task_id_2}")
        
        # 监控任务
        while True:
            stats = queue.get_queue_stats()
            print(f"\n[STATS] 队列状 {stats}")
            
            # 检查任务状
            status_1 = queue.get_task_status(task_id_1)
            status_2 = queue.get_task_status(task_id_2)
            
            print(f"[STATUS] 任务 1: {status_1['status']}")
            print(f"[STATUS] 任务 2: {status_2['status']}")
            
            # 如果所有任务都完成,退
            if stats['pending'] == 0 and stats['running'] == 0:
                break
            
            time.sleep(5)
    
    finally:
        # 停止队列
        queue.stop()

if __name__ == "__main__":
    main()
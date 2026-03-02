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

from core.importer import GitToNeo4jImporter
from parser.utils.logger import get_logger

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
    java_source_dir: str = "src/main/java"
    project_name: Optional[str] = None
    clear_database: bool = False
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
                 max_memory_mb: int = 1024):
        
        self.max_workers = max_workers
        self.cache_base_dir = cache_base_dir
        self.task_db_file = task_db_file
        self.max_memory_mb = max_memory_mb
        
        # 创建缓存目录
        os.makedirs(os.path.dirname(task_db_file), exist_ok=True)
        
        # 任务队列
        self.task_queue: PriorityQueue = PriorityQueue()
        self.tasks: Dict[str, GitImportTask] = {}
        self.tasks_lock = Lock()
        
        # 工作线程
        self.workers: List[threading.Thread] = []
        self.running = False
        self.stop_event = Event()
        
        # 加载已保存的任务
        self._load_tasks()
    
    def submit_task(self,
                   repo_url: str,
                   branch: str = "main",
                   repo_name: Optional[str] = None,
                   java_source_dir: str = "src/main/java",
                   project_name: Optional[str] = None,
                   clear_database: bool = False,
                   priority: TaskPriority = TaskPriority.NORMAL) -> str:
        
        task_id = self._generate_task_id()
        
        task = GitImportTask(
            task_id=task_id,
            repo_url=repo_url,
            branch=branch,
            repo_name=repo_name,
            java_source_dir=java_source_dir,
            project_name=project_name,
            clear_database=clear_database,
            priority=priority
        )
        
        with self.tasks_lock:
            self.tasks[task_id] = task
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
        
        with self.tasks_lock:
            task = self.tasks.get(task_id)
            if task and task.status == TaskStatus.PENDING:
                task.status = TaskStatus.CANCELLED
                logger.info(f"[CANCEL] 任务已取 {task_id}")
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
            
            # 创建导入
            importer = GitToNeo4jImporter(
                neo4j_uri="neo4j+s://26fa83e0.databases.neo4j.io",
                neo4j_user="neo4j",
                neo4j_password="kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4",
                neo4j_database="neo4j",
                cache_base_dir=self.cache_base_dir
            )
            
            # 连接Neo4j
            if not importer.connect():
                raise Exception("无法连接Neo4j")
            
            try:
                # 执行导入
                result = importer.import_from_git(
                    repo_url=task.repo_url,
                    branch=task.branch,
                    repo_name=task.repo_name,
                    java_source_dir=task.java_source_dir,
                    project_name=task.project_name,
                    clear_database=task.clear_database
                )
                
                # 更新任务状
                with self.tasks_lock:
                    if result['success']:
                        task.status = TaskStatus.SUCCESS
                        task.result = result
                        logger.info(f"[EXECUTE] 任务成功: {task.task_id}")
                    else:
                        task.status = TaskStatus.FAILED
                        task.error = result.get('error', '未知错误')
                        logger.error(f"[EXECUTE] 任务失败: {task.task_id} - {task.error}")
                    
                    task.completed_at = datetime.now().isoformat()
            
            finally:
                importer.disconnect()
        
        except Exception as e:
            logger.error(f"[EXECUTE] 任务异常: {task.task_id} - {e}", exc_info=True)
            
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
        
        finally:
            # 保存任务
            self._save_tasks()
    
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
        """保存任务到文件"""
        try:
            with self.tasks_lock:
                tasks_data = {
                    'tasks': {k: v.to_dict() for k, v in self.tasks.items()},
                    'saved_at': datetime.now().isoformat()
                }
            
            with open(self.task_db_file, 'w', encoding='utf-8') as f:
                json.dump(tasks_data, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.error(f"[SAVE] 保存任务失败: {e}")
    
    def _load_tasks(self):
        """从文件加载任"""
        try:
            if os.path.isfile(self.task_db_file):
                with open(self.task_db_file, 'r', encoding='utf-8') as f:
                    tasks_data = json.load(f)
                
                for task_id, task_dict in tasks_data.get('tasks', {}).items():
                    # 只加载未完成的任
                    if task_dict['status'] in ['pending', 'running']:
                        task = self._dict_to_task(task_dict)
                        self.tasks[task_id] = task
                        # 重新入队
                        self.task_queue.put((task.priority.value, task_id, task))
                
                logger.info(f"[LOAD] 加载{len(self.tasks)} 个任务")
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
                   max_memory_mb: int = 1024) -> TaskQueue:
    
    global _global_task_queue
    
    if _global_task_queue is None:
        _global_task_queue = TaskQueue(
            max_workers=max_workers,
            cache_base_dir=cache_base_dir,
            task_db_file=task_db_file,
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
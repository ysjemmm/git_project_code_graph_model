#!/usr/bin/env python3

import os
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional

from tools.constants import PROJECT_ROOT_PATH

sys.path.insert(0, str(Path(__file__).parent.parent))

from parser.utils.logger import get_logger
from git.incremental_analyzer import GitIncrementalAnalyzer
from storage.neo4j.connector import Neo4jConnector
from core.language_adapters import get_language_adapter
from core.language_adapters.protocols import ContextualLanguageAdapter, LanguageAdapter
from core.import_context import ProjectImportContext
from core.import_result import ImportResult
from core.env_loader import load_env_vars
from core.graph_store import Neo4jGraphStore
from core.project_ids import generate_project_symbol_id

logger = get_logger("git_importer")

class GitToNeo4jImporter:
    
    # 默认配置
    # 仓库缓存目录固定为「项目根目录/.cache/git_repos」
    DEFAULT_CACHE_BASE_DIR = str(Path(PROJECT_ROOT_PATH / ".cache/git_repos"))
    DEFAULT_NEO4J_URI = "neo4j+s://26fa83e0.databases.neo4j.io"
    DEFAULT_NEO4J_USER = "neo4j"
    # 安全：不要在代码中硬编码真实密码。请通过环境变量/secret 注入。
    DEFAULT_NEO4J_PASSWORD = "password"
    DEFAULT_NEO4J_DATABASE = "neo4j"
    
    def __init__(self,
                 neo4j_uri: str = None,
                 neo4j_user: str = None,
                 neo4j_password: str = None,
                 neo4j_database: str = None,
                 cache_base_dir: str = None,
                 language: str = "java",
                 adapter: LanguageAdapter | None = None):
        # 便捷配置：从 .env.local/.env 补齐缺失环境变量（不覆盖既有环境变量）
        load_env_vars({"NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD", "NEO4J_DATABASE"})

        # 从参数/环境变量获取配置（未配置时仍回退到默认值）
        self.neo4j_uri = neo4j_uri or os.getenv('NEO4J_URI', self.DEFAULT_NEO4J_URI)
        self.neo4j_user = neo4j_user or os.getenv('NEO4J_USER', self.DEFAULT_NEO4J_USER)
        self.neo4j_password = neo4j_password or os.getenv('NEO4J_PASSWORD', self.DEFAULT_NEO4J_PASSWORD)
        self.neo4j_database = neo4j_database or os.getenv('NEO4J_DATABASE', self.DEFAULT_NEO4J_DATABASE)
        # 仓库缓存目录统一为「项目根目录/.cache/git_repos」，不再受环境变量影响
        self.cache_base_dir = cache_base_dir or self.DEFAULT_CACHE_BASE_DIR

        # 延后依赖 cache_base_dir 的初始化，避免在 _load_env_file_if_needed 中访问未赋值属性
        self.connector = None
        self.git_analyzer = GitIncrementalAnalyzer(self.cache_base_dir)
        self.language = language
        # adapter 可选：显式传入时作为默认单语言 adapter 使用
        # 线上多语言场景建议不传 adapter，在 import_from_git() 按任务动态选择 language/languages
        self.adapter: LanguageAdapter | None = adapter

        # 安全提示：当使用代码内置默认凭据时发出警告（不改变运行结果）
        if (not neo4j_uri) and (os.getenv('NEO4J_URI') is None) and self.neo4j_uri == self.DEFAULT_NEO4J_URI:
            logger.warning("NEO4J_URI 未设置，正在使用代码默认值（建议使用环境变量覆盖）")
        if (not neo4j_user) and (os.getenv('NEO4J_USER') is None) and self.neo4j_user == self.DEFAULT_NEO4J_USER:
            logger.warning("NEO4J_USER 未设置，正在使用代码默认值（建议使用环境变量覆盖）")
        if (not neo4j_password) and (os.getenv('NEO4J_PASSWORD') is None) and self.neo4j_password == self.DEFAULT_NEO4J_PASSWORD:
            logger.warning("NEO4J_PASSWORD 未设置，正在使用代码默认值（强烈建议使用环境变量覆盖，并轮换已泄露的密码）")
        if (not neo4j_database) and (os.getenv('NEO4J_DATABASE') is None) and self.neo4j_database == self.DEFAULT_NEO4J_DATABASE:
            logger.warning("NEO4J_DATABASE 未设置，正在使用代码默认值（建议使用环境变量覆盖）")

    def connect(self) -> bool:
        
        try:
            self.connector = Neo4jConnector(
                self.neo4j_uri,
                self.neo4j_user,
                self.neo4j_password,
                self.neo4j_database
            )
            if not self.connector.connect():
                logger.error("无法连接到Neo4j")
                return False
            logger.info("已连接到 Neo4j")
            return True
        except Exception as e:
            logger.info(f"[ERROR] 连接异常: {e}")
            import traceback
            logger.exception("Exception occurred")
            return False
    
    def import_from_git(self,
                       repo_url: str,
                       branch: str = "master",
                       repo_name: Optional[str] = None,
                       source_dir: Optional[str] = None,
                       java_source_dir: Optional[str] = None,
                       language: Optional[str] = None,
                       languages: Optional[List[str]] = None,
                       project_name: Optional[str] = None,
                       clear_database: bool = False,
                       include_unchanged_total: bool = False,
                       include_comment_nodes: bool = False,
                       async_mode: bool = False,
                       clone_timeout: Optional[int] = None,
                       git_config: Optional[Dict[str, str]] = None,
                       commit_id: Optional[str] = None) -> Dict:
        # 兼容：历史参数 java_source_dir 仍然保留；如果两者都传，以 source_dir 为准
        if source_dir is None:
            source_dir = java_source_dir

        # 兼容：language 选择
        # - 显式传 adapter：视为单语言（忽略 language/languages）
        # - 显式传 languages：按列表顺序逐个执行
        # - 否则使用 language（若传）或实例默认 self.language
        if self.adapter is not None:
            resolved_languages: List[str] = []
        elif languages:
            resolved_languages = languages
        else:
            resolved_languages = [language or self.language]
        
        if async_mode:
            return self._import_async(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                source_dir=source_dir,
                language=language,
                languages=languages,
                project_name=project_name,
                clear_database=clear_database,
                include_unchanged_total=include_unchanged_total,
                include_comment_nodes=include_comment_nodes,
                commit_id=commit_id
            )
        else:
            return self._import_sync(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                source_dir=source_dir,
                language=language,
                languages=languages,
                project_name=project_name,
                clear_database=clear_database,
                include_unchanged_total=include_unchanged_total,
                include_comment_nodes=include_comment_nodes,
                clone_timeout=clone_timeout,
                git_config=git_config,
                commit_id=commit_id
            )
    
    def _import_async(self,
                      repo_url: str,
                      branch: str = "master",
                      repo_name: Optional[str] = None,
                      source_dir: Optional[str] = None,
                      language: Optional[str] = None,
                      languages: Optional[List[str]] = None,
                      project_name: Optional[str] = None,
                      clear_database: bool = False,
                      include_unchanged_total: bool = False,
                      include_comment_nodes: bool = False,
                      clone_timeout: Optional[int] = None,
                      git_config: Optional[Dict[str, str]] = None,
                      commit_id: Optional[str] = None) -> Dict:
        """异步导入(提交到任务队列)"""
        try:
            from core.task_queue import get_task_queue, TaskPriority
            
            # Git URL 动态解析仓库名
            extracted_repo_name = repo_url.split('/')[-1].replace('.git', '')
            
            # 如果指定repo_name,使用指定的;否则使用解析出来的
            if repo_name is None:
                repo_name = extracted_repo_name
            
            # 如果指定project_name,使用指定的;否则使repo_name
            if project_name is None:
                project_name = repo_name
            
            queue = get_task_queue(
                cache_base_dir=self.cache_base_dir
            )
            
            if not queue.running:
                queue.start()
            
            task_id = queue.submit_task(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                java_source_dir=source_dir,
                language=language,
                languages=languages,
                project_name=project_name,
                clear_database=clear_database,
                priority=TaskPriority.NORMAL,
                include_unchanged_total=include_unchanged_total,
                include_comment_nodes=include_comment_nodes,
                clone_timeout=clone_timeout,
                git_config=git_config,
                commit_id=commit_id
            )
            
            return {
                'success': True,
                'mode': 'async',
                'task_id': task_id,
                'message': f'任务已提交到后台队列: {task_id}',
                'status_url': f'查询状 queue.get_task_status("{task_id}")'
            }
        
        except Exception as e:
            return {
                'success': False,
                'mode': 'async',
                'error': str(e)
            }
    
    def _import_sync(self,
                    repo_url: str,
                    branch: str = "master",
                    repo_name: Optional[str] = None,
                    source_dir: Optional[str] = None,
                    language: Optional[str] = None,
                    languages: Optional[List[str]] = None,
                    project_name: Optional[str] = None,
                    clear_database: bool = False,
                    include_unchanged_total: bool = False,
                    include_comment_nodes: bool = False,
                    clone_timeout: Optional[int] = None,
                    git_config: Optional[Dict[str, str]] = None,
                    commit_id: Optional[str] = None) -> Dict:
        if not self.connector:
            return {
                'success': False,
                'error': '未连接到 Neo4j,请先调用connect()'
            }

        start_time = time.perf_counter()
        try:
            logger.info("\n" + "=" * 70)
            logger.info("Git 仓库导入工具")
            logger.info("=" * 70)
            
            # Git URL 动态解析仓库名
            # 支持格式: https://github.com/user/repo.git http://git.example.com/path/repo.git
            extracted_repo_name = repo_url.split('/')[-1].replace('.git', '')
            
            # 如果指定repo_name,使用指定的;否则使用解析出来的
            if repo_name is None:
                repo_name = extracted_repo_name
            
            # 如果指定project_name,使用指定的;否则使repo_name
            if project_name is None:
                project_name = repo_name
            
            logger.info(f"\n仓库信息:")
            logger.info(f"  URL: {repo_url}")
            if commit_id:
                logger.info(f"  Commit ID: {commit_id}")
            else:
                logger.info(f"  分支: {branch}")
            logger.info(f"  仓库 {repo_name}")
            logger.info(f"  项目 {project_name}")
            if source_dir:
                logger.info(f"  源代码目录 {source_dir}")
            else:
                logger.info(f"  源代码目录 自动查找所有目录")
            
            # 解析本次任务的 adapters（支持单语言或多语言列表）
            if self.adapter is not None:
                adapters: List[LanguageAdapter] = [self.adapter]
                adapter_extensions = getattr(self.adapter, "source_extensions", [".java"])
            else:
                langs = languages or [language or self.language]
                adapters = [get_language_adapter(lang) for lang in langs]
                adapter_extensions = sorted({ext for ad in adapters for ext in getattr(ad, "source_extensions", [])} or [".java"])

            # 第一步:Git 增量分析
            logger.info(f"\n执行 Git 增量分析...")
            git_result = self.git_analyzer.analyze_git_repo(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                source_dir=source_dir,
                extensions=adapter_extensions,
                include_unchanged_total=include_unchanged_total,
                clone_timeout=clone_timeout,
                git_config=git_config,
                commit_id=commit_id
            )
            
            if not git_result['success']:
                logger.info(f"[ERROR] Git 分析失败: {git_result.get('error', '未知错误')}")
                return {
                    'success': False,
                    'error': git_result.get('error', '未知错误'),
                    'duration_ms': int((time.perf_counter() - start_time) * 1000),
                }
            
            logger.info(f"[OK] Git 分析成功")
            logger.info(f"  Commit: {git_result['commit_hash'][:8]}")
            logger.info(f"  变化文件 {len(git_result.get('changed_files', []))}")

            # 项目根 symbol_id 只生成一次，用作 project_key（多仓库同名项目隔离）
            root_project_symbol_id = generate_project_symbol_id(project_name, project_type="Application")
            
            # 如果没有变化,直接返回（不会提交任何写入到 Neo4j）
            if not git_result['has_changes']:
                logger.info(f"\n[INFO] 代码未变化,无需重新分析，也不会对 Neo4j 执行任何写入（attempted/created 均为 0）")
                
                # 但如果用户显式要求清理数据库，则仍然执行“按项目根节点清理子图”
                if clear_database:
                    try:
                        deleted_count = self.connector.delete_project_data(
                            project_name,
                            project_key=root_project_symbol_id,
                        )
                        logger.info(f"[OK] 已清理项目子图: {project_name}，删除节点数: {deleted_count}")
                        return {
                            'success': True,
                            'status': 'cleared',
                            'message': '代码未变化，但已按项目清理子图（未重新导入）',
                            'repo_name': repo_name,
                            'project_name': project_name,
                            'commit_hash': git_result['commit_hash'],
                            'deleted_nodes': deleted_count,
                            'duration_ms': int((time.perf_counter() - start_time) * 1000),
                        }
                    except Exception as e:
                        logger.warning(f"[WARN] 请求清理子图但执行失败: {e}")

                ir = ImportResult(
                    success=True,
                    status="cached",
                    message="代码未变化,使用缓存结果（本次未对 Neo4j 执行写入）",
                    repo_name=repo_name,
                    project_name=project_name,
                    project_key=root_project_symbol_id,
                    commit_hash=git_result["commit_hash"],
                    duration_ms=int((time.perf_counter() - start_time) * 1000),
                )
                return ir.to_dict()
            
            # 第二步:获取仓库缓存目录
            repo_cache_dir = self.git_analyzer.cache_manager.get_repo_cache_dir(repo_name)

            ctx = ProjectImportContext(
                project_name=project_name,
                project_key=root_project_symbol_id,
                repo_cache_dir=repo_cache_dir,
                source_dir=source_dir,
                language=language,
                languages=languages,
                clear_database=bool(clear_database),
                include_comment_nodes=include_comment_nodes,
            )
            store = Neo4jGraphStore(self.connector)

            # 清理子图只做一次（多语言场景避免重复删除）
            # 优先按 project_key/symbol_id 精确删除，避免多仓库同名项目误删串库
            if ctx.clear_database:
                deleted_count = store.delete_project_subgraph(ctx)
                logger.info(f"[OK] 已清理项目子图: {ctx.project_name}，删除节点数: {deleted_count}")

            total_created_nodes = 0
            total_created_relationships = 0
            total_attempted_nodes = 0
            total_attempted_relationships = 0

            # 逐语言执行解析与导出
            for idx, adapter in enumerate(adapters):
                # 如果没有指定 source_dir，则由 adapter 自动查找源码目录
                source_dirs = adapter.find_source_dirs(ctx.repo_cache_dir, ctx.source_dir)
                if not source_dirs:
                    logger.warning(f"[WARN] 未找到任何源代码目录（adapter={adapter.__class__.__name__}），跳过")
                    continue

                logger.info(f"\n找到 {len(source_dirs)} 个源码目录（adapter={adapter.__class__.__name__}）")
                for dir_path in source_dirs:
                    logger.info(f"  - {dir_path}")

                # 扫描源码文件
                logger.info(f"\n扫描源码文件..（adapter={adapter.__class__.__name__}）")
                source_files = adapter.find_source_files(ctx.repo_cache_dir, source_dirs)
                logger.info(f"[OK] 总共找到 {len(source_files)} 个源码文件（adapter={adapter.__class__.__name__}）")

                if not source_files:
                    continue

                # 处理删除的文件（仅对本 adapter 的源文件生效）
                if git_result.get('deleted_files'):
                    deleted_files = [f for f in git_result['deleted_files'] if adapter.is_source_file(f)]
                    if deleted_files:
                        logger.info(f"\n处理删除的文件..（adapter={adapter.__class__.__name__}）")
                        logger.info(f"发现 {len(deleted_files)} 个删除的文件")
                        for deleted_file in deleted_files:
                            logger.info(f"  删除文件相关节点: {deleted_file}")
                            store.delete_file_subgraph(ctx, deleted_file)

                # 解析 AST
                logger.info(f"\n解析 AST...（adapter={adapter.__class__.__name__}）")
                if isinstance(adapter, ContextualLanguageAdapter):
                    parse_result = adapter.parse_files_with_context(ctx, source_files)
                else:
                    parse_result = adapter.parse_files(
                        ctx.repo_cache_dir,
                        ctx.project_name,
                        source_files,
                        project_root_symbol_id=ctx.project_key,
                    )
                logger.info(f"[OK] 成功解析 {len(parse_result.ast_data_list)}/{len(source_files)} 个文件（adapter={adapter.__class__.__name__}）")

                # 导出到Neo4j（多语言场景此处不再传 clear_database，避免重复删子图）
                logger.info(f"\n导出到Neo4j...（adapter={adapter.__class__.__name__}）")
                if isinstance(adapter, ContextualLanguageAdapter):
                    export_result = adapter.export_to_store_with_context(
                        ctx=ctx,
                        store=store,
                        ast_data_list=parse_result.ast_data_list,
                        symbol_table=parse_result.symbol_table,
                        clear_database=False,
                    )
                else:
                    export_result = adapter.export_to_neo4j(
                        connector=self.connector,
                        project_name=ctx.project_name,
                        project_root_symbol_id=ctx.project_key,
                        repo_path=ctx.repo_cache_dir,
                        ast_data_list=parse_result.ast_data_list,
                        symbol_table=parse_result.symbol_table,
                        clear_database=False,
                    )

                if not export_result.get("success"):
                    export_result["duration_ms"] = int((time.perf_counter() - start_time) * 1000)
                    return export_result

                total_created_nodes += int(export_result.get("created_nodes", 0))
                total_created_relationships += int(export_result.get("created_relationships", 0))
                total_attempted_nodes += int(export_result.get("attempted_nodes", 0))
                total_attempted_relationships += int(export_result.get("attempted_relationships", 0))
            
            logger.info(f"[OK] 导出成功（汇总）")
            logger.info(f"  - 本次提交写入条目（attempted）: 节点 {total_attempted_nodes}，关系 {total_attempted_relationships}")
            logger.info(f"  - 本次新建（created）: 节点 {total_created_nodes}，关系 {total_created_relationships}")
            
            # 第八步:获取统计信息
            stats = self.connector.get_statistics()
            logger.info(f"\n数据库统计")
            logger.info(f"  - 总节点数: {stats.get('total_nodes', 0)}")
            logger.info(f"  - 总关系数: {stats.get('total_relationships', 0)}")
            
            if stats.get('node_types'):
                logger.info(f"\n节点类型分布:")
                for label, count in sorted(stats['node_types'].items(), key=lambda x: x[1], reverse=True):
                    logger.info(f"  - {label}: {count}")
            
            logger.info("\n" + "=" * 70)
            logger.info("导入完成")
            logger.info("=" * 70)
            
            ir = ImportResult(
                success=True,
                status="imported",
                message="导入完成",
                repo_name=repo_name,
                branch=branch,
                project_name=project_name,
                project_key=root_project_symbol_id,
                commit_hash=git_result["commit_hash"],
                attempted_nodes=total_attempted_nodes,
                attempted_relationships=total_attempted_relationships,
                created_nodes=total_created_nodes,
                created_relationships=total_created_relationships,
                statistics=stats,
                duration_ms=int((time.perf_counter() - start_time) * 1000),
            )
            return ir.to_dict()
        
        except Exception as e:
            logger.info(f"[ERROR] 导入过程异常: {e}")
            import traceback
            logger.exception("Exception occurred")
            ir = ImportResult(
                success=False,
                status="failed",
                message=str(e),
                repo_name=repo_name,
                project_name=project_name,
                project_key=root_project_symbol_id,
                commit_hash=git_result.get("commit_hash") if git_result else None,
                duration_ms=int((time.perf_counter() - start_time) * 1000),
            )
            return ir.to_dict()
    
    # 语言相关的源码发现/解析/导出逻辑已下沉到 adapter（例如 JavaLanguageAdapter）
    
    def disconnect(self):
        
        if self.connector:
            self.connector.disconnect()
    
    def cleanup_repo(self, repo_name: str) -> bool:
        
        return self.git_analyzer.cleanup_repo(repo_name)
    
    def get_cache_info(self, repo_name: str) -> Dict:
        
        return self.git_analyzer.get_cache_info(repo_name)
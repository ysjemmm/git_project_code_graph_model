#!/usr/bin/env python3

import os
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional

from tools.constants import CACHE_MAVEN_DEPS_PATH, CACHE_GIT_REPOS_PATH

sys.path.insert(0, str(Path(__file__).parent.parent))

from parser.utils.logger import get_logger
from git.incremental_analyzer import GitIncrementalAnalyzer
from storage.neo4j.connector import Neo4jConnector
from core.language_adapters import get_language_adapter
from core.language_adapters.protocols import ContextualLanguageAdapter, LanguageAdapter
from core.import_context import ProjectImportContext
from core.import_result import ImportResult
from core.env_loader import load_env_vars
from threading import Event as ThreadEvent
from core.graph_store import Neo4jGraphStore
from core.project_ids import generate_project_symbol_id
from core.maven_manager import MavenIncrementalManager, MavenRunner, MavenSettings
from storage.sqlite import get_jar_class_db, JARScanner

logger = get_logger("git_importer")


class ImportCancelled(RuntimeError):
    pass


def _raise_if_cancelled(cancel_event: ThreadEvent | None) -> None:
    if cancel_event is not None and cancel_event.is_set():
        raise ImportCancelled("导入任务已被用户终止")


def _maven_prepare_and_scan(*, repo_root: str, repo_name: str, enabled: bool, force: bool = False) -> None:
    """
    enabled=True 且 repo_root 存在 pom.xml 时：
    - 增量执行 Maven 依赖拉取（dependency:copy-dependencies），把依赖 jar 收敛到缓存目录
    - 扫描依赖 jar 并更新 .cache/jar_classes.db（供外部类型解析/链接使用）
    """
    if not enabled:
        logger.info("[INFO] 已跳过 Maven 扫描（maven_scan_enabled=False）")
        return
    if not MavenIncrementalManager.is_maven_project(repo_root):
        logger.info("[INFO] 未检测到 pom.xml，跳过 Maven 扫描")
        return

    settings = MavenSettings.autodetect()
    mgr = MavenIncrementalManager(repo_root=repo_root, repo_cache_key=repo_name)
    skip, reason = mgr.should_skip(settings=settings)
    if force:
        skip = False
        reason = "force_maven=True（强制重新解析 Maven 依赖）"
    logger.info(f"[Maven] {reason}")

    out_dir = CACHE_MAVEN_DEPS_PATH / repo_name
    # 刷新诉求（force=True）时：清掉目录避免旧 jar 残留导致 jar_classes.db 出现“幽灵依赖”
    if force and out_dir.exists():
        import shutil

        shutil.rmtree(out_dir, ignore_errors=True)
    out_dir.mkdir(parents=True, exist_ok=True)

    if not skip:
        # 先执行一次 install，确保多模块 reactor 下的模块间依赖（如 gateway-common）可被解析，
        # 避免 dependency:copy-dependencies 将其当作远端依赖去 nexus 拉取而失败。
        MavenRunner.run(
            repo_root,
            ["install"] + MavenIncrementalManager.maven_base_flags(),
            settings=settings,
        )
        args = (
            ["dependency:copy-dependencies"]
            + MavenIncrementalManager.maven_base_flags()
            + [
                f"-DoutputDirectory={str(out_dir)}",
                "-DincludeScope=compile",
                "-DexcludeTransitive=false",
                # 关键：拷贝每个依赖 jar 时一并拷贝其 pom（同名 .pom），供 JARScanner 解析 parent/artifact；
                # includeTypes=jar,pom 只会多拷“pom 类型构件”，不会为每个 jar 配同名 pom，需用 copyPom。
                "-Dmdep.copyPom=true",
            ]
        )
        MavenRunner.run(repo_root, args, settings=settings)
        mgr.write_marker(settings=settings)

    logger.info(f"[JAR] 扫描依赖目录: {out_dir}")
    db = get_jar_class_db()
    db.initialize_schema()
    if force:
        try:
            cleared = db.delete_by_jar_path_prefix(str(out_dir))
            logger.info(f"[JAR] 刷新前已清空旧依赖记录：jar_classes={cleared}")
        except Exception as e:
            logger.warning(f"[WARN] 清空旧 jar_classes 失败（继续尝试重新扫描）：{e}")
    scanner = JARScanner(db)
    scan_result = scanner.scan_directory(
        str(out_dir),
        force_rescan=bool(force),
        include_anonymous=False,
    )
    # 自愈：如果发现 jar 很多但全部 skipped 且 classes=0，则强制全量重扫一次
    if (
        scan_result.total_jars_found > 0
        and scan_result.jars_scanned == 0
        and scan_result.total_classes == 0
    ):
        logger.warning(
            "[JAR] 检测到 found>0 但 scanned=0 且 classes=0，触发 force_rescan 以修复历史元数据/落库异常"
        )
        scan_result = scanner.scan_directory(str(out_dir), force_rescan=True, include_anonymous=False)
    logger.info(
        f"[JAR] 完成：found={scan_result.total_jars_found} scanned={scan_result.jars_scanned} "
        f"skipped={scan_result.jars_skipped} classes={scan_result.total_classes} "
        f"duration={scan_result.duration:.2f}s errors={len(scan_result.errors)}"
    )

class GitToNeo4jImporter:
    
    # 默认配置
    # 仓库缓存目录固定为「项目根目录/.cache/git_repos」
    DEFAULT_CACHE_BASE_DIR = str(CACHE_GIT_REPOS_PATH)
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
                       commit_id: Optional[str] = None,
                       maven_scan_enabled: bool = True,
                       force_maven: bool = False,
                       auto_link_external: bool = True,
                       task_type: str = "auto",
                       cancel_event: ThreadEvent | None = None) -> Dict:
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
                commit_id=commit_id,
                maven_scan_enabled=maven_scan_enabled,
                auto_link_external=auto_link_external,
                task_type=task_type,
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
                commit_id=commit_id,
                maven_scan_enabled=maven_scan_enabled,
                force_maven=force_maven,
                auto_link_external=auto_link_external,
                task_type=task_type,
                cancel_event=cancel_event,
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
                      commit_id: Optional[str] = None,
                      maven_scan_enabled: bool = True,
                      auto_link_external: bool = True,
                      task_type: str = "auto") -> Dict:
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
                commit_id=commit_id,
                maven_scan_enabled=bool(maven_scan_enabled),
                auto_link_external=bool(auto_link_external),
                task_type=str(task_type or "auto"),
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
                    commit_id: Optional[str] = None,
                    maven_scan_enabled: bool = True,
                    force_maven: bool = False,
                    auto_link_external: bool = True,
                    task_type: str = "auto",
                    cancel_event: ThreadEvent | None = None) -> Dict:
        if not self.connector:
            return {
                'success': False,
                'error': '未连接到 Neo4j,请先调用connect()'
            }

        start_time = time.perf_counter()
        try:
            _raise_if_cancelled(cancel_event)
            logger.info("[DEBUG] IMPORTER_VERSION=20260318b")
            logger.info("\n" + "=" * 70)
            logger.info("Git 仓库导入工具 [IMPORTER_VERSION=20260318b]")
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
                commit_id=commit_id,
                maven_scan_enabled=bool(maven_scan_enabled),
            )

            _raise_if_cancelled(cancel_event)
            
            if not git_result['success']:
                logger.info(f"[ERROR] Git 分析失败: {git_result.get('error', '未知错误')}")
                return {
                    'success': False,
                    'error': git_result.get('error', '未知错误'),
                    'duration_ms': int((time.perf_counter() - start_time) * 1000),
                }

            task_type_norm = str(task_type or "auto").strip().lower()
            if task_type_norm not in {"auto", "full", "incremental"}:
                task_type_norm = "auto"
            changed_files_raw = list(git_result.get('changed_files', []) or [])
            added_files_raw = list(git_result.get('added_files', []) or [])
            deleted_files_raw = list(git_result.get('deleted_files', []) or [])
            def _norm_rel_file_path(p: Any) -> str:
                return str(p or "").replace("\\", "/").strip()
            changed_or_added_files = changed_files_raw + added_files_raw
            changed_or_added_set = {
                os.path.normcase(os.path.normpath(str(p)))
                for p in changed_or_added_files
                if str(p).strip()
            }
            source_exts = tuple(sorted({str(x).lower() for x in (adapter_extensions or []) if str(x).strip()}))
            if source_exts:
                non_source_changes = [
                    p for p in changed_or_added_files
                    if not str(p).lower().endswith(source_exts)
                ]
            else:
                non_source_changes = list(changed_or_added_files)
            auto_fallback_reason: Optional[str] = None
            incremental_enabled = False
            if clear_database:
                auto_fallback_reason = "clear_database=true"
                incremental_enabled = False
            elif task_type_norm == "full":
                incremental_enabled = False
            elif task_type_norm == "incremental":
                incremental_enabled = True
            else:
                # auto：保守策略，只有“纯源码小范围变更”才走增量，其他场景自动降级全量
                if deleted_files_raw:
                    auto_fallback_reason = "存在删除文件，降级全量以确保关系一致性"
                elif non_source_changes:
                    auto_fallback_reason = "存在非源码文件变更，降级全量"
                elif len(changed_or_added_set) > 120:
                    auto_fallback_reason = f"变更文件过多({len(changed_or_added_set)})，降级全量"
                else:
                    incremental_enabled = True
            effective_mode = "incremental" if incremental_enabled else "full"
            logger.info(
                f"[INFO] task_type={task_type_norm} -> effective_mode={effective_mode}"
                + (f"（reason={auto_fallback_reason}）" if auto_fallback_reason else "")
            )
            
            logger.info(f"[OK] Git 分析成功")
            logger.info(f"  Commit: {git_result['commit_hash'][:8]}")
            logger.info(f"  变化文件 {len(git_result.get('changed_files', []))}")

            # Git 增量分析阶段可能自动探测了更准确的 source_dir（例如多模块仓库）。
            # 但对多模块仓库，如果调用方未显式指定 source_dir，则应保持为 None，
            # 让 LanguageAdapter 自动发现所有源码目录（否则会被“探测到的单一目录”限制，漏扫大量源码）。
            detected_source_dir = (git_result.get("source_dir") or "").strip()
            if detected_source_dir and detected_source_dir != (source_dir or "").strip():
                logger.info(f"[INFO] Git 分析探测到 source_dir={detected_source_dir}")
                if (source_dir or "").strip():
                    logger.info(f"[INFO] source_dir 采用自动探测结果: {detected_source_dir}")
                    source_dir = detected_source_dir

            # 项目根 symbol_id 只生成一次，用作 project_key（多仓库同名项目隔离）
            root_project_symbol_id = generate_project_symbol_id(project_name, project_type="Application")
            
            # 如果没有变化,直接返回（不会提交任何写入到 Neo4j）
            if not git_result['has_changes']:
                # clear_database=true 视为“重建”诉求：即使代码未变化，也继续走全量导入（先清理再导入）。
                # 否则默认跳过写入，但若库中尚无该项目数据，则仍执行一次全量导入，避免“首次导入却被判定无变化”。
                if clear_database:
                    try:
                        deleted_count = self.connector.delete_project_data(
                            project_name,
                            project_key=root_project_symbol_id,
                        )
                        logger.info(f"[OK] 已清理项目子图: {project_name}，删除节点数: {deleted_count}")
                    except Exception as e:
                        logger.warning(f"[WARN] 请求清理子图但执行失败: {e}")
                else:
                    try:
                        # 仅用 belong_project 统计可能误判：
                        # - 历史数据不完整 / belong_project 缺失
                        # - 本地有仓库但图谱未写入（此时应强制初始化）
                        # 因此优先判断 Project(Application) 根节点是否存在
                        rows = self.connector.execute_read_query(
                            "MATCH (p:Project) WHERE p.name = $name AND p.project_type = 'Application' RETURN count(p) AS c",
                            {"name": project_name},
                        ) or []
                        has_project = int((rows[0] or {}).get("c", 0) or 0) > 0 if rows else False

                        # 兜底：若 Project 节点存在，再用 belong_project 粗略判断是否有子图数据
                        existing = 0
                        if has_project:
                            rows2 = self.connector.execute_read_query(
                                "MATCH (n) WHERE coalesce(n.belong_project,'') = $p RETURN count(*) AS c",
                                {"p": project_name},
                            ) or []
                            existing = int((rows2[0] or {}).get("c", 0) or 0) if rows2 else 0
                    except Exception:
                        has_project = False
                        existing = 0

                    # 只要 Project(Application) 不存在，就必须执行一次全量导入初始化
                    if has_project and existing > 0:
                        logger.info(f"\n[INFO] 代码未变化,使用缓存结果（本次未对 Neo4j 执行写入）")
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
                    logger.info("[INFO] 代码未变化但库中无该项目数据，执行一次全量导入以初始化图谱")
            
            # 第二步:获取仓库缓存目录
            repo_cache_dir = self.git_analyzer.cache_manager.get_repo_cache_dir(repo_name)
            _raise_if_cancelled(cancel_event)

            # 约定：很多调用方会默认传 "src/main/java"（但对多模块仓库这会导致漏扫）。
            # 如果该默认目录在仓库根本不存在，则将其视为“未指定”，交给 adapter 自动发现所有源码目录。
            try:
                if (source_dir or "").strip() == "src/main/java":
                    default_path = Path(repo_cache_dir) / "src" / "main" / "java"
                    if not default_path.exists():
                        source_dir = None
            except Exception:
                pass

            # Maven 扫描（可选）：解析 pom 外部依赖并构建 jar 类索引
            try:
                _raise_if_cancelled(cancel_event)
                _maven_prepare_and_scan(
                    repo_root=repo_cache_dir,
                    repo_name=repo_name,
                    enabled=bool(maven_scan_enabled),
                    force=bool(force_maven),
                )
                _raise_if_cancelled(cancel_event)
            except Exception as e:
                return {
                    'success': False,
                    'error': f"Maven 扫描失败: {e}",
                    'duration_ms': int((time.perf_counter() - start_time) * 1000),
                }

            ctx = ProjectImportContext(
                project_name=project_name,
                project_key=root_project_symbol_id,
                repo_cache_dir=repo_cache_dir,
                source_dir=source_dir,
                language=language,
                languages=languages,
                clear_database=bool(clear_database),
                include_comment_nodes=include_comment_nodes,
                auto_link_external=bool(auto_link_external),
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
            delta_detail: Dict[str, Any] = {
                "files": {
                    "added": [_norm_rel_file_path(x) for x in added_files_raw if _norm_rel_file_path(x)],
                    "changed": [_norm_rel_file_path(x) for x in changed_files_raw if _norm_rel_file_path(x)],
                    "deleted": [_norm_rel_file_path(x) for x in deleted_files_raw if _norm_rel_file_path(x)],
                },
                "added_nodes": {
                    "total": 0,
                    "by_label": {},
                    "samples": [],
                    "truncated": False,
                },
                "added_relationships": {
                    "total": 0,
                    "by_type": {},
                    "samples": [],
                    "truncated": False,
                },
                "deleted_nodes": {
                    "total": 0,
                    "by_file": [],
                    "samples": [],
                    "truncated": False,
                },
                "deleted_relationships": {
                    "total": 0,
                    "by_file": [],
                },
                "changed_nodes": {
                    "files": [],
                },
                "changed_relationships": {
                    "files": [],
                    "by_type": {},
                },
            }
            _node_sample_keys = set()
            _rel_sample_keys = set()
            _deleted_sample_keys = set()

            # 逐语言执行解析与导出
            for idx, adapter in enumerate(adapters):
                _raise_if_cancelled(cancel_event)
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

                _raise_if_cancelled(cancel_event)

                # 处理删除的文件（仅对本 adapter 的源文件生效）
                if git_result.get('deleted_files'):
                    deleted_files = [f for f in git_result['deleted_files'] if adapter.is_source_file(f)]
                    if deleted_files:
                        logger.info(f"\n处理删除的文件..（adapter={adapter.__class__.__name__}）")
                        logger.info(f"发现 {len(deleted_files)} 个删除的文件")
                        for deleted_file in deleted_files:
                            logger.info(f"  删除文件相关节点: {deleted_file}")
                            detail = store.delete_file_subgraph_with_details(ctx, deleted_file)
                            deleted_nodes = int(detail.get("deleted_nodes") or 0)
                            deleted_rels = int(detail.get("deleted_relationships") or 0)
                            delta_detail["deleted_nodes"]["total"] += deleted_nodes
                            delta_detail["deleted_relationships"]["total"] += deleted_rels
                            delta_detail["deleted_nodes"]["by_file"].append(
                                {"file": _norm_rel_file_path(deleted_file), "count": deleted_nodes}
                            )
                            delta_detail["deleted_relationships"]["by_file"].append(
                                {"file": _norm_rel_file_path(deleted_file), "count": deleted_rels}
                            )
                            for n in list(detail.get("sample_nodes") or []):
                                if len(delta_detail["deleted_nodes"]["samples"]) >= 120:
                                    delta_detail["deleted_nodes"]["truncated"] = True
                                    break
                                key = f"{n.get('label','')}::{n.get('symbol_id','')}"
                                if key in _deleted_sample_keys:
                                    continue
                                _deleted_sample_keys.add(key)
                                delta_detail["deleted_nodes"]["samples"].append(
                                    {
                                        "file": _norm_rel_file_path(deleted_file),
                                        "label": n.get("label"),
                                        "symbol_id": n.get("symbol_id"),
                                        "display": n.get("display"),
                                    }
                                )

                # 处理修改文件：先删旧子图再重建，避免方法/字段变更后残留脏节点和脏边
                if git_result.get('changed_files'):
                    changed_files = [f for f in git_result['changed_files'] if adapter.is_source_file(f)]
                    if changed_files:
                        logger.info(f"\n处理修改文件（先删后建）..（adapter={adapter.__class__.__name__}）")
                        logger.info(f"发现 {len(changed_files)} 个修改文件")
                        delta_detail["changed_nodes"]["files"].extend(
                            [_norm_rel_file_path(x) for x in changed_files if _norm_rel_file_path(x)]
                        )
                        delta_detail["changed_relationships"]["files"].extend(
                            [_norm_rel_file_path(x) for x in changed_files if _norm_rel_file_path(x)]
                        )
                        for changed_file in changed_files:
                            logger.info(f"  重建文件相关节点: {changed_file}")
                            detail = store.delete_file_subgraph_with_details(ctx, changed_file)
                            deleted_nodes = int(detail.get("deleted_nodes") or 0)
                            deleted_rels = int(detail.get("deleted_relationships") or 0)
                            delta_detail["deleted_nodes"]["total"] += deleted_nodes
                            delta_detail["deleted_relationships"]["total"] += deleted_rels
                            delta_detail["deleted_nodes"]["by_file"].append(
                                {"file": _norm_rel_file_path(changed_file), "count": deleted_nodes, "reason": "changed-rebuild"}
                            )
                            delta_detail["deleted_relationships"]["by_file"].append(
                                {"file": _norm_rel_file_path(changed_file), "count": deleted_rels, "reason": "changed-rebuild"}
                            )

                # 增量模式：仅处理“新增 + 修改”文件
                if incremental_enabled:
                    before = len(source_files)
                    source_files = [
                        f for f in source_files
                        if os.path.normcase(os.path.normpath(str(f))) in changed_or_added_set
                    ]
                    logger.info(
                        f"[INFO] 增量过滤（adapter={adapter.__class__.__name__}）："
                        f"{before} -> {len(source_files)}（仅新增/修改）"
                    )

                if not source_files:
                    logger.info(f"[INFO] 无需解析文件（adapter={adapter.__class__.__name__}），跳过 AST 与导出")
                    continue

                # 解析 AST
                logger.info(f"\n解析 AST...（adapter={adapter.__class__.__name__}）")
                _raise_if_cancelled(cancel_event)
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
                _raise_if_cancelled(cancel_event)
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
                detail = export_result.get("detail") if isinstance(export_result, dict) else None
                if isinstance(detail, dict):
                    for k, v in (detail.get("created_nodes_by_label") or {}).items():
                        key = str(k or "").strip()
                        if not key:
                            continue
                        delta_detail["added_nodes"]["by_label"][key] = int(delta_detail["added_nodes"]["by_label"].get(key, 0) or 0) + int(v or 0)
                    for k, v in (detail.get("created_relationships_by_type") or {}).items():
                        key = str(k or "").strip()
                        if not key:
                            continue
                        delta_detail["added_relationships"]["by_type"][key] = int(delta_detail["added_relationships"]["by_type"].get(key, 0) or 0) + int(v or 0)
                    for k, v in (detail.get("attempted_relationships_by_type") or {}).items():
                        key = str(k or "").strip()
                        if not key:
                            continue
                        delta_detail["changed_relationships"]["by_type"][key] = int(delta_detail["changed_relationships"]["by_type"].get(key, 0) or 0) + int(v or 0)

                    for n in list(detail.get("node_samples") or []):
                        if len(delta_detail["added_nodes"]["samples"]) >= 120:
                            delta_detail["added_nodes"]["truncated"] = True
                            break
                        key = f"{n.get('label','')}::{n.get('symbol_id','')}"
                        if key in _node_sample_keys:
                            continue
                        _node_sample_keys.add(key)
                        delta_detail["added_nodes"]["samples"].append(
                            {
                                "label": n.get("label"),
                                "symbol_id": n.get("symbol_id"),
                                "display": n.get("display"),
                                "file_path": _norm_rel_file_path(n.get("file_path")),
                            }
                        )
                    for r in list(detail.get("relationship_samples") or []):
                        if len(delta_detail["added_relationships"]["samples"]) >= 120:
                            delta_detail["added_relationships"]["truncated"] = True
                            break
                        key = f"{r.get('type','')}::{r.get('source_id','')}::{r.get('target_id','')}"
                        if key in _rel_sample_keys:
                            continue
                        _rel_sample_keys.add(key)
                        delta_detail["added_relationships"]["samples"].append(
                            {
                                "type": r.get("type"),
                                "source_id": r.get("source_id"),
                                "target_id": r.get("target_id"),
                            }
                        )
            
            delta_detail["added_nodes"]["total"] = int(total_created_nodes)
            delta_detail["added_relationships"]["total"] = int(total_created_relationships)
            delta_detail["changed_nodes"]["files"] = sorted(set(delta_detail["changed_nodes"]["files"]))
            delta_detail["changed_relationships"]["files"] = sorted(set(delta_detail["changed_relationships"]["files"]))
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
                extra={
                    "task_type": task_type_norm,
                    "incremental_enabled": bool(incremental_enabled),
                    "effective_mode": effective_mode,
                    "fallback_reason": auto_fallback_reason,
                    "changed_or_added_files": len(changed_or_added_set),
                    "deleted_files": len(deleted_files_raw),
                    "non_source_changes": len(non_source_changes),
                    "changed_files_list": [_norm_rel_file_path(x) for x in changed_files_raw if _norm_rel_file_path(x)],
                    "added_files_list": [_norm_rel_file_path(x) for x in added_files_raw if _norm_rel_file_path(x)],
                    "deleted_files_list": [_norm_rel_file_path(x) for x in deleted_files_raw if _norm_rel_file_path(x)],
                    "delta_detail": delta_detail,
                },
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
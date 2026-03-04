#!/usr/bin/env python3

import os
import sys
from pathlib import Path
from typing import Dict, List, Optional

from loraxmod import Parser

from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from parser.symbol_table_builder import SymbolTableBuilder
from tools.constants import PROJECT_ROOT_PATH

# 修复 Windows 编码问题
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

sys.path.insert(0, str(Path(__file__).parent.parent))

from parser.utils.logger import get_logger
from git.incremental_analyzer import GitIncrementalAnalyzer
from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.exporter import Neo4jExporterAST
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.common.symbol_table import SymbolTable

logger = get_logger("git_importer")

class GitToNeo4jImporter:
    
    # 默认配置
    DEFAULT_CACHE_BASE_DIR = str(Path(PROJECT_ROOT_PATH / ".cache/git_repos"))
    DEFAULT_NEO4J_URI = "neo4j+s://26fa83e0.databases.neo4j.io"
    DEFAULT_NEO4J_USER = "neo4j"
    DEFAULT_NEO4J_PASSWORD = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"
    DEFAULT_NEO4J_DATABASE = "neo4j"
    
    def __init__(self,
                 neo4j_uri: str = None,
                 neo4j_user: str = None,
                 neo4j_password: str = None,
                 neo4j_database: str = None,
                 cache_base_dir: str = None):
        
        # 从环境变量或参数获取配置
        self.neo4j_uri = neo4j_uri or os.getenv('NEO4J_URI', self.DEFAULT_NEO4J_URI)
        self.neo4j_user = neo4j_user or os.getenv('NEO4J_USER', self.DEFAULT_NEO4J_USER)
        self.neo4j_password = neo4j_password or os.getenv('NEO4J_PASSWORD', self.DEFAULT_NEO4J_PASSWORD)
        self.neo4j_database = neo4j_database or os.getenv('NEO4J_DATABASE', self.DEFAULT_NEO4J_DATABASE)
        self.cache_base_dir = cache_base_dir or os.getenv('GIT_CACHE_BASE_DIR', self.DEFAULT_CACHE_BASE_DIR)
        
        self.connector = None
        self.git_analyzer = GitIncrementalAnalyzer(self.cache_base_dir)
    
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
                       java_source_dir: Optional[str] = None,
                       project_name: Optional[str] = None,
                       clear_database: bool = False,
                       async_mode: bool = False,
                       clone_timeout: Optional[int] = None,
                       git_config: Optional[Dict[str, str]] = None) -> Dict:
        
        if async_mode:
            return self._import_async(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                java_source_dir=java_source_dir,
                project_name=project_name,
                clear_database=clear_database
            )
        else:
            return self._import_sync(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                java_source_dir=java_source_dir,
                project_name=project_name,
                clear_database=clear_database,
                clone_timeout=clone_timeout,
                git_config=git_config
            )
    
    def _import_async(self,
                      repo_url: str,
                      branch: str = "master",
                      repo_name: Optional[str] = None,
                      java_source_dir: Optional[str] = None,
                      project_name: Optional[str] = None,
                      clear_database: bool = False) -> Dict:
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
                java_source_dir=java_source_dir,
                project_name=project_name,
                clear_database=clear_database,
                priority=TaskPriority.NORMAL
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
                    java_source_dir: Optional[str] = None,
                    project_name: Optional[str] = None,
                    clear_database: bool = False,
                    clone_timeout: Optional[int] = None,
                    git_config: Optional[Dict[str, str]] = None) -> Dict:
        if not self.connector:
            return {
                'success': False,
                'error': '未连接到 Neo4j,请先调用connect()'
            }
        
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
            logger.info(f"  分支: {branch}")
            logger.info(f"  仓库 {repo_name}")
            logger.info(f"  项目 {project_name}")
            if java_source_dir:
                logger.info(f"  Java 源代码目录 {java_source_dir}")
            else:
                logger.info(f"  Java 源代码目录 自动查找所有目录")
            
            # 第一步:Git 增量分析
            logger.info(f"\n执行 Git 增量分析...")
            git_result = self.git_analyzer.analyze_git_repo(
                repo_url=repo_url,
                branch=branch,
                repo_name=repo_name,
                java_source_dir=java_source_dir,
                clone_timeout=clone_timeout,
                git_config=git_config
            )
            
            if not git_result['success']:
                logger.info(f"[ERROR] Git 分析失败: {git_result.get('error', '未知错误')}")
                return {
                    'success': False,
                    'error': git_result.get('error', '未知错误')
                }
            
            logger.info(f"[OK] Git 分析成功")
            logger.info(f"  Commit: {git_result['commit_hash'][:8]}")
            logger.info(f"  变化文件 {len(git_result.get('changed_files', []))}")
            
            # 如果没有变化,直接返
            if not git_result['has_changes']:
                logger.info(f"\n[INFO] 代码未变化,无需重新分析")
                return {
                    'success': True,
                    'status': 'cached',
                    'message': '代码未变化,使用缓存结果',
                    'repo_name': repo_name,
                    'commit_hash': git_result['commit_hash']
                }
            
            # 第二步:获取仓库缓存目录
            repo_cache_dir = self.git_analyzer.cache_manager.get_repo_cache_dir(repo_name)
            
            # 如果没有指定 java_source_dir,自动查找所有Java 源代码目
            if java_source_dir:
                java_source_dirs = [java_source_dir]
            else:
                java_source_dirs = self._find_all_java_source_dirs(repo_cache_dir)
                if not java_source_dirs:
                    return {
                        'success': False,
                        'error': '未找到任何Java 源代码目录'
                    }
            
            logger.info(f"\n找到 {len(java_source_dirs)} 个Java 源代码目录")
            for dir_path in java_source_dirs:
                logger.info(f"  - {dir_path}")
            
            # 第三步:扫描所有Java 文件
            logger.info(f"\n扫描 Java 源代..")
            all_java_files = []
            for source_dir in java_source_dirs:
                java_source_path = os.path.join(repo_cache_dir, source_dir.replace('/', os.sep))
                java_files = self._find_java_files(java_source_path)
                all_java_files.extend(java_files)
                logger.info(f"  {source_dir}: {len(java_files)} 个文件")
            
            logger.info(f"[OK] 总共找到 {len(all_java_files)} 个Java 文件")
            
            if not all_java_files:
                return {
                    'success': False,
                    'error': '未找到任何Java 文件'
                }
            
            # 第四步:处理删除的文件
            if git_result.get('deleted_files'):
                logger.info(f"\n处理删除的文件..")
                deleted_files = git_result['deleted_files']
                logger.info(f"发现 {len(deleted_files)} 个删除的文件")
                
                for deleted_file in deleted_files:
                    if deleted_file.endswith('.java'):
                        logger.info(f"  删除文件相关节点: {deleted_file}")
                        self.connector.delete_nodes_by_file(deleted_file, project_name)
            
            # 第五步:解析 AST
            logger.info(f"\n解析 AST...")
            ast_data_list = []
            
            # 需要获取第一个源代码目录作为基础路径
            # first_source_dir = java_source_dirs[0]
            # first_source_path = os.path.join(repo_cache_dir, first_source_dir.replace('/', os.sep))
            
            # 创建全局符号表
            global_symbol_table = SymbolTable()

            # 创建分析器上下文
            context = AnalyzerContext(
                project_name=project_name,
                project_path=repo_cache_dir,
                root_project_symbol_id=AnalyzerHelper.generate_symbol_id_for_project(project_name),
                parser=Parser("java")
            )
            
            # 顺序解析 AST
            for i, java_file in enumerate(all_java_files):
                try:
                    ast_data = self._parse_java_file(java_file, context, global_symbol_table)
                    if ast_data is not None:
                        ast_data_list.append(ast_data)
                except Exception as e:
                    logger.warning(f"[WARN] 文件 {java_file} 解析失败: {e}")
            
            logger.info(f"[OK] 成功解析 {len(ast_data_list)}/{len(all_java_files)} 个文件")
            
            # 第六步:注册关系
            logger.info(f"\n注册关系...【待定，先不做】")
            # TODO 待定
            # builder = SymbolTableBuilder(symbol_table=global_symbol_table)
            # for java_file_structure in ast_data_list:
            #     builder.current_file = java_file_structure.file_path
            #     builder.register_all_method_calls(java_file_structure)
            #     builder.register_all_field_accesses(java_file_structure)
            
            # 第七步:导出到Neo4j
            logger.info(f"\n导出到Neo4j...")
            exporter = Neo4jExporterAST(self.connector)
            result = exporter.export_from_ast_data(
                ast_data_list,
                project_name,
                context.root_project_symbol_id,
                repo_cache_dir,
                clear_database,
                [global_symbol_table]
            )
            
            if result['success']:
                logger.info(f"[OK] 导出成功")
                logger.info(f"  - 节点 {result['created_nodes']}")
                logger.info(f"  - 关系 {result['created_relationships']}")
            else:
                logger.info(f"[ERROR] 导出失败: {result.get('error', '未知错误')}")
                return result
            
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
            
            return {
                'success': True,
                'repo_name': repo_name,
                'branch': branch,
                'commit_hash': git_result['commit_hash'],
                'nodes_count': result['created_nodes'],
                'relationships_count': result['created_relationships'],
                'statistics': stats
            }
        
        except Exception as e:
            logger.info(f"[ERROR] 导入过程异常: {e}")
            import traceback
            logger.exception("Exception occurred")
            return {
                'success': False,
                'error': str(e)
            }
    
    def _find_java_files(self, directory_path: str) -> List[str]:
        
        java_files = []
        for root, dirs, files in os.walk(directory_path):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        return sorted(java_files)
    
    def _find_all_java_source_dirs(self, repo_path: str) -> List[str]:
        """
        智能查找所有 Java 源代码目录
        支持多种结构:
        - src/main/java (Maven 标准)
        - src/test/java (Maven 测试)
        - src/java (简单结构)
        """
        found_dirs = set()
        
        for root, dirs, files in os.walk(repo_path):
            # 检查是否是 Maven 标准结构: src/main/java 或 src/test/java
            if 'src' in dirs:
                src_path = os.path.join(root, 'src')
                for src_subdir in os.listdir(src_path):
                    src_subdir_path = os.path.join(src_path, src_subdir)
                    if os.path.isdir(src_subdir_path) and 'java' in os.listdir(src_subdir_path):
                        java_path = os.path.join(src_subdir_path, 'java')
                        # 检查是否有 Java 文件
                        has_java_files = False
                        for r, d, f in os.walk(java_path):
                            if any(file.endswith('.java') for file in f):
                                has_java_files = True
                                break
                        
                        if has_java_files:
                            rel_path = os.path.relpath(java_path, repo_path)
                            found_dirs.add(rel_path)
            
            # 检查是否是简单结构: 直接的 java 目录
            elif 'java' in dirs:
                java_path = os.path.join(root, 'java')
                # 检查是否有 Java 文件
                has_java_files = False
                for r, d, f in os.walk(java_path):
                    if any(file.endswith('.java') for file in f):
                        has_java_files = True
                        break
                
                if has_java_files:
                    rel_path = os.path.relpath(java_path, repo_path)
                    found_dirs.add(rel_path)
        
        java_source_dirs = sorted(list(found_dirs))
        return java_source_dirs
    
    def _parse_java_file(self, java_file_path: str, context: AnalyzerContext, global_symbol_table=None):
        """解析单个Java文件"""
        try:
            # 使用全局符号表或创建新的
            if global_symbol_table is None:
                symbol_table = SymbolTable()
            else:
                symbol_table = global_symbol_table
            
            # 设置当前文件路径到 context（用于生成 symbol_id）
            # 计算相对于项目根目录的路径
            try:
                relative_path = os.path.relpath(java_file_path, context.project_path)
                context.file_path = relative_path.replace(os.sep, '/')  # 统一使用 / 分隔符
            except ValueError:
                # 如果无法计算相对路径，使用文件名
                context.file_path = os.path.basename(java_file_path)
            
            analyzer = JavaFileAnalyzer(
                context=context,
                symbol_table=symbol_table,
                auto_resolve_types=True,
                file_path=java_file_path
            )
            return analyzer.analyze_file()

        except Exception as e:
            logger.info(f"[ERROR] 解析 {java_file_path} 失败: {e}")
            import traceback
            logger.error(traceback.format_exc())
            return None
    
    def disconnect(self):
        
        if self.connector:
            self.connector.disconnect()
    
    def cleanup_repo(self, repo_name: str) -> bool:
        
        return self.git_analyzer.cleanup_repo(repo_name)
    
    def get_cache_info(self, repo_name: str) -> Dict:
        
        return self.git_analyzer.get_cache_info(repo_name)
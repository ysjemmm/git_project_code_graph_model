
from parser.utils.logger import get_logger

import os
from typing import Dict, List, Optional, Tuple

from git.manager import GitManager
from storage.cache.git_cache import GitCacheManager
from storage.cache.merkle_tree import MerkleTreeBuilder, MerkleTreeComparator
from parser.incremental_analyzer import IncrementalAnalyzer

logger = get_logger("git_incremental_analyzer")

class GitIncrementalAnalyzer:
    """Git 增量分析"""
    
    def __init__(self, cache_base_dir: str = ".cache/git_repos"):
        
        self.cache_manager = GitCacheManager(cache_base_dir)
        self.merkle_builder = MerkleTreeBuilder()
        self.merkle_comparator = MerkleTreeComparator()
    
    def analyze_git_repo(self, 
                        repo_url: str, 
                        branch: str = "master",
                        repo_name: Optional[str] = None,
                        java_source_dir: str = "src/main/java",
                        clone_timeout: Optional[int] = None,
                        git_config: Optional[Dict[str, str]] = None) -> Dict:
        try:
            # 提取仓库名称
            if repo_name is None:
                repo_name = repo_url.split('/')[-1].replace('.git', '')
            
            # 获取仓库缓存目录
            repo_cache_dir = self.cache_manager.get_repo_cache_dir(repo_name)
            
            # 初始化Git 管理
            git_manager = GitManager(repo_cache_dir)
            
            # 如果没有指定超时时间,动态计
            if clone_timeout is None:
                clone_timeout = GitManager.calculate_dynamic_timeout(repo_url)
            
            # 设置默认 git 配置(禁用CRLF 自动转换
            default_git_config = {
                'core.safecrlf': 'false',
                'core.autocrlf': 'false'
            }
            # 合并用户提供的配置
            if git_config:
                default_git_config.update(git_config)
            
            # 获取commit(切换前)
            source_commit = None
            source_branch = None
            if git_manager.is_repo_exists():
                source_commit = git_manager.get_current_commit()
                source_branch = git_manager.get_current_branch()
            
            # 第一步:克隆或拉取仓库
            if not git_manager.is_repo_exists():
                logger.info(f"[INFO] 首次克隆仓库: {repo_url}")
                success, msg = git_manager.clone(repo_url, branch, timeout=clone_timeout, git_config=default_git_config)
                if not success:
                    return {
                        'success': False,
                        'repo_name': repo_name,
                        'repo_url': repo_url,
                        'branch': branch,
                        'error': msg
                    }
            else:
                logger.info(f"[INFO] 更新本地仓库: {repo_name}")
                # fetch 获取最新的远程分支信息
                success, msg = git_manager.fetch()
                if not success:
                    return {
                        'success': False,
                        'repo_name': repo_name,
                        'repo_url': repo_url,
                        'branch': branch,
                        'error': msg
                    }
                
                # 获取当前分支
                current_branch = git_manager.get_current_branch()
                
                # 如果目标分支与当前分支不同,需要切换分
                if current_branch != branch:
                    logger.info(f"[INFO] 切换分支: {current_branch} -> {branch}")
                    success, msg = git_manager.checkout(branch)
                    if not success:
                        return {
                            'success': False,
                            'repo_name': repo_name,
                            'repo_url': repo_url,
                            'branch': branch,
                            'error': msg
                        }
                
                # 执行 pull 更新代码
                success, msg = git_manager.pull(branch)
                if not success:
                    return {
                        'success': False,
                        'repo_name': repo_name,
                        'repo_url': repo_url,
                        'branch': branch,
                        'error': msg
                    }
            
            # 第二步:获取当前 commit hash
            current_commit = git_manager.get_current_commit()
            if not current_commit:
                return {
                    'success': False,
                    'repo_name': repo_name,
                    'repo_url': repo_url,
                    'branch': branch,
                    'error': "无法获取 commit hash"
                }
            
            logger.info(f"[INFO] 当前 commit: {current_commit[:8]}")
            
            # 第三步:检查是否有代码变化
            has_changes, reason = self.cache_manager.has_changes(repo_name, branch, current_commit)
            logger.info(f"[INFO] {reason}")
            
            # 规范java_source_dir 路径
            if java_source_dir:
                java_source_dir_normalized = java_source_dir.replace('/', os.sep)
                java_source_path = os.path.join(repo_cache_dir, java_source_dir_normalized)
            else:
                # 如果没有指定 java_source_dir,使用仓库根目录
                java_source_path = repo_cache_dir
            
            if not os.path.isdir(java_source_path):
                return {
                    'success': False,
                    'repo_name': repo_name,
                    'repo_url': repo_url,
                    'branch': branch,
                    'commit_hash': current_commit,
                    'error': f"Java 源代码目录不存在: {java_source_path}"
                }
            
            # 第四步:构建 Merkle 树并对比
            logger.info(f"[INFO] 构建 Merkle  {java_source_path}")
            new_tree = self.merkle_builder.build(java_source_path)
            
            # 加载旧的 Merkle 树(从源分支或同分支的旧版本
            # 如果切换了分支,加载源分支的 Merkle 
            # 如果没切换分支,加载同分支的旧版
            if source_branch and source_branch != branch:
                # 切换了分支,加载源分支的 Merkle 
                old_tree = self.cache_manager.load_merkle_tree(repo_name, source_branch)
            else:
                # 没切换分支或首次克隆,加载同分支的旧版本
                old_tree = self.cache_manager.load_merkle_tree(repo_name, branch)
            
            # 对比 Merkle 树,找出变化的文
            changed_files = []
            deleted_files = []
            added_files = []
            
            if old_tree:
                logger.info(f"[INFO] 对比 Merkle 树,检测变化文..")
                logger.info(f"[DEBUG] 旧树哈希: {old_tree.hash[:16]}...")
                logger.info(f"[DEBUG] 新树哈希: {new_tree.hash[:16]}...")
                
                # 收集两个树中的所有文件及其哈希
                old_files_dict = {}  # {filename: (path, hash)}
                new_files_dict = {}  # {filename: (path, hash)}
                self._collect_all_files_with_hash(old_tree, old_files_dict)
                self._collect_all_files_with_hash(new_tree, new_files_dict)
                
                # 新增文件
                for name, (path, hash_val) in new_files_dict.items():
                    if name not in old_files_dict:
                        added_files.append(path)
                
                # 删除文件
                for name, (path, hash_val) in old_files_dict.items():
                    if name not in new_files_dict:
                        deleted_files.append(path)
                
                # 变更文件(名称相同但哈希不同
                for name in old_files_dict:
                    if name in new_files_dict:
                        old_path, old_hash = old_files_dict[name]
                        new_path, new_hash = new_files_dict[name]
                        # 比较文件内容哈希
                        if old_hash != new_hash:
                            changed_files.append(new_path)
                
                # 打印详细的分支切换信
                logger.info("")
                logger.info("=" * 70)
                logger.info("分支切换详情:")
                logger.info("=" * 70)
                
                # 源信息
                if source_commit and source_branch:
                    logger.info(f"commit = {source_commit[:8]} ({source_branch})")
                else:
                    logger.info("commit = 首次克隆")
                
                # 目标信息
                logger.info(f"目标 commit = {current_commit[:8]} ({branch})")
                
                # 文件变化统计
                logger.info("")
                logger.info("文件变化统计:")
                
                # 新增文件
                logger.info(f"新增文件: {len(added_files)} ")
                if added_files:
                    for file in added_files:
                        # 显示相对于仓库根目录的路
                        rel_file = os.path.relpath(file, repo_cache_dir)
                        logger.info(f"  + {rel_file}")
                
                # 变更文件
                logger.info(f"变更文件: {len(changed_files)} ")
                if changed_files:
                    for file in changed_files:
                        # 显示相对于仓库根目录的路
                        rel_file = os.path.relpath(file, repo_cache_dir)
                        logger.info(f"  ~ {rel_file}")
                
                # 删除文件
                logger.info(f"删除文件: {len(deleted_files)} ")
                if deleted_files:
                    for file in deleted_files:
                        # 显示相对于仓库根目录的路
                        rel_file = os.path.relpath(file, repo_cache_dir)
                        logger.info(f"  - {rel_file}")
                
                logger.info("=" * 70)
                logger.info("")
                
                logger.info(f"[INFO] 发现 {len(added_files)} 个新增文件 {len(changed_files)} 个变更文件  {len(deleted_files)} 个删除文件")
            else:
                logger.info(f"[INFO] 首次分析")
                added_files = self._collect_all_java_files(java_source_path)
                
                # 打印首次导入信息
                logger.info("")
                logger.info("=" * 70)
                logger.info("首次导入信息:")
                logger.info("=" * 70)
                logger.info(f"分支 = {branch}")
                logger.info(f"Commit = {current_commit[:8]}")
                logger.info(f"新增文件: {len(added_files)} ")
                if added_files:
                    for file in added_files:
                        # 显示相对于仓库根目录的路
                        rel_file = os.path.relpath(file, repo_cache_dir)
                        logger.info(f"  + {rel_file}")
                logger.info("=" * 70)
                logger.info("")
            
            # 如果没有变化,直接返回缓存结
            if not has_changes and not added_files and not changed_files and not deleted_files:
                logger.info(f"[INFO] 代码未变化,无需重新分析")
                return {
                    'success': True,
                    'repo_name': repo_name,
                    'repo_url': repo_url,
                    'branch': branch,
                    'commit_hash': current_commit,
                    'has_changes': False,
                    'changed_files': [],
                    'deleted_files': [],
                    'analysis_result': {
                        'status': 'cached',
                        'message': '代码未变化,使用缓存结果'
                    }
                }
            
            # 第五步:保存 Merkle 树和元数
            self.cache_manager.save_merkle_tree(repo_name, branch, new_tree)
            self.cache_manager.update_metadata(repo_name, repo_url, branch, current_commit)
            
            # 第六步:执行增量分析
            logger.info(f"[INFO] 执行增量分析...")
            incremental_analyzer = IncrementalAnalyzer(
                os.path.join(self.cache_manager.cache_base_dir, "analysis_cache")
            )
            
            analysis_result = incremental_analyzer.analyze_changes(repo_cache_dir)
            
            return {
                'success': True,
                'repo_name': repo_name,
                'repo_url': repo_url,
                'branch': branch,
                'commit_hash': current_commit,
                'has_changes': True,
                'changed_files': changed_files,
                'added_files': added_files,
                'deleted_files': deleted_files,
                'analysis_result': analysis_result
            }
        
        except Exception as e:
            import traceback
            logger.exception("Exception occurred")
            return {
                'success': False,
                'repo_name': repo_name,
                'repo_url': repo_url,
                'branch': branch,
                'error': f"分析异常: {str(e)}"
            }
    
    def get_changed_java_files(self, 
                              repo_url: str, 
                              branch: str = "master",
                              repo_name: Optional[str] = None,
                              java_source_dir: str = "src/main/java") -> Tuple[bool, List[str]]:
        
        result = self.analyze_git_repo(repo_url, branch, repo_name, java_source_dir)
        
        if result['success']:
            changed_files = result.get('changed_files', [])
            # 过滤只保java 文件
            java_files = [f for f in changed_files if f.endswith('.java')]
            return True, java_files
        else:
            return False, []
    
    def cleanup_repo(self, repo_name: str) -> bool:
        
        return self.cache_manager.cleanup_repo(repo_name)
    
    def get_cache_info(self, repo_name: str) -> Dict:
        
        return self.cache_manager.get_cache_info(repo_name)
    
    @staticmethod
    def _collect_all_java_files(directory: str) -> List[str]:
        
        java_files = []
        for root, dirs, files in os.walk(directory):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        return java_files
    
    def _collect_all_files_with_hash(self, node: 'MerkleNode', file_dict: dict):
        ""
        if node.is_file:
            # 使用文件名作为键,存(路径, 哈希
            filename = os.path.basename(node.path)
            file_dict[filename] = (node.path, node.hash)
        else:
            for child in (node.children or {}).values():
                self._collect_all_files_with_hash(child, file_dict)



import json
import os
import shutil
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from typing import Dict, List, Optional

from parser.incremental_analyzer import IncrementalAnalyzer
from parser.utils.logger import get_logger

# 获取日志记录
logger = get_logger("multi_repo_analyzer")

class MultiRepoAnalyzer:
    
    
    def __init__(self, cache_base_dir: str = ".kiro", max_workers: int = 4):
        
        self.cache_base_dir = cache_base_dir
        self.max_workers = max_workers
        self.repos_cache_dir = os.path.join(cache_base_dir, "repos")
        self.index_file = os.path.join(cache_base_dir, "repo_index.json")
        
        # 创建缓存目录
        os.makedirs(self.repos_cache_dir, exist_ok=True)
        
        # 加载仓库索引
        self.repo_index = self._load_index()
    
    def _load_index(self) -> Dict:
        
        if os.path.isfile(self.index_file):
            try:
                with open(self.index_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            except Exception as e:
                print(f"警告: 无法加载仓库索引: {e}")
        return {}
    
    def _save_index(self):
        
        try:
            with open(self.index_file, 'w', encoding='utf-8') as f:
                json.dump(self.repo_index, f, indent=2)
        except Exception as e:
            print(f"警告: 无法保存仓库索引: {e}")
    
    def _register_repo(self, repo_id: str, repo_path: str):
        
        self.repo_index[repo_id] = {
            'path': repo_path,
            'registered_at': datetime.now().isoformat(),
            'last_analyzed': None
        }
        self._save_index()
    
    def _update_repo_timestamp(self, repo_id: str):
        """更新仓库分析时间"""
        if repo_id in self.repo_index:
            self.repo_index[repo_id]['last_analyzed'] = datetime.now().isoformat()
            self._save_index()
    
    def analyze_repo(self, repo_path: str, repo_id: str) -> Dict:
        
        start_time = time.time()
        
        try:
            # 验证仓库路径
            if not os.path.isdir(repo_path):
                return {
                    'repo_id': repo_id,
                    'repo_path': repo_path,
                    'status': 'error',
                    'error': f'仓库路径不存 {repo_path}',
                    'analysis_time': 0
                }
            
            # 注册仓库
            self._register_repo(repo_id, repo_path)
            
            # 创建仓库缓存目录
            repo_cache_dir = os.path.join(self.repos_cache_dir, repo_id)
            os.makedirs(repo_cache_dir, exist_ok=True)
            
            # 创建增量分析
            analyzer = IncrementalAnalyzer(repo_cache_dir)
            
            # 分析变化
            changes = analyzer.analyze_changes(repo_path)
            
            # 更新时间
            self._update_repo_timestamp(repo_id)
            
            analysis_time = time.time() - start_time
            
            return {
                'repo_id': repo_id,
                'repo_path': repo_path,
                'status': 'success',
                'changes': changes,
                'analysis_time': analysis_time
            }
        
        except Exception as e:
            analysis_time = time.time() - start_time
            return {
                'repo_id': repo_id,
                'repo_path': repo_path,
                'status': 'error',
                'error': str(e),
                'analysis_time': analysis_time
            }
    
    def analyze_repos_sequential(self, repos: Dict[str, str]) -> Dict:
        
        start_time = time.time()
        results = {}
        success_count = 0
        failed_count = 0
        
        print(f"开始顺序分{len(repos)} 个仓..")
        
        for repo_id, repo_path in repos.items():
            print(f"  分析 {repo_id}...", end=' ', flush=True)
            result = self.analyze_repo(repo_path, repo_id)
            results[repo_id] = result
            
            if result['status'] == 'success':
                success_count += 1
                print(f"({result['analysis_time']:.4f}s)")
            else:
                failed_count += 1
                print(f"({result['error']})")
        
        total_time = time.time() - start_time
        
        return {
            'total': len(repos),
            'success': success_count,
            'failed': failed_count,
            'total_time': total_time,
            'results': results
        }
    
    def analyze_repos_concurrent(self, repos: Dict[str, str]) -> Dict:
        
        start_time = time.time()
        results = {}
        success_count = 0
        failed_count = 0
        
        print(f"开始并发分{len(repos)} 个仓库(最大并发数: {self.max_workers}..")
        
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            # 提交所有任
            futures = {
                executor.submit(self.analyze_repo, repo_path, repo_id): repo_id
                for repo_id, repo_path in repos.items()
            }
            
            # 处理完成的任
            for future in as_completed(futures):
                repo_id = futures[future]
                try:
                    result = future.result()
                    results[repo_id] = result
                    
                    if result['status'] == 'success':
                        success_count += 1
                        print(f"  {repo_id} ({result['analysis_time']:.4f}s)")
                    else:
                        failed_count += 1
                        print(f"  {repo_id} ({result['error']})")
                except Exception as e:
                    failed_count += 1
                    results[repo_id] = {
                        'repo_id': repo_id,
                        'status': 'error',
                        'error': str(e)
                    }
                    print(f"  {repo_id} (异常: {e})")
        
        total_time = time.time() - start_time
        
        return {
            'total': len(repos),
            'success': success_count,
            'failed': failed_count,
            'total_time': total_time,
            'results': results
        }
    
    def get_repo_cache_dir(self, repo_id: str) -> str:
        
        return os.path.join(self.repos_cache_dir, repo_id)
    
    def get_repo_info(self, repo_id: str) -> Optional[Dict]:
        
        return self.repo_index.get(repo_id)
    
    def list_repos(self) -> List[Dict]:
        """列出所有已注册的仓库"""
        repos = []
        for repo_id, info in self.repo_index.items():
            cache_dir = self.get_repo_cache_dir(repo_id)
            cache_size = self._get_dir_size(cache_dir)
            
            repos.append({
                'repo_id': repo_id,
                'path': info['path'],
                'registered_at': info['registered_at'],
                'last_analyzed': info['last_analyzed'],
                'cache_size': cache_size
            })
        
        return repos
    
    def cleanup_repo(self, repo_id: str):
        """清理单个仓库的缓存"""
        cache_dir = self.get_repo_cache_dir(repo_id)
        if os.path.isdir(cache_dir):
            shutil.rmtree(cache_dir)
            print(f"已清理仓库缓 {repo_id}")
        
        # 从索引中移除
        if repo_id in self.repo_index:
            del self.repo_index[repo_id]
            self._save_index()
    
    def cleanup_old_repos(self, max_age_days: int = 30):
        """清理超过指定天数的仓库缓存"""
        now = datetime.now()
        cleaned = []
        
        for repo_id, info in list(self.repo_index.items()):
            if info['last_analyzed']:
                last_analyzed = datetime.fromisoformat(info['last_analyzed'])
                age_days = (now - last_analyzed).days
                
                if age_days > max_age_days:
                    self.cleanup_repo(repo_id)
                    cleaned.append(repo_id)
        
        if cleaned:
            print(f"已清{len(cleaned)} 个过期仓库缓存")
        
        return cleaned
    
    def get_cache_size(self) -> int:
        """获取缓存总大小(字节)"""
        return self._get_dir_size(self.cache_base_dir)
    
    def _get_dir_size(self, directory: str) -> int:
        
        total_size = 0
        try:
            for root, dirs, files in os.walk(directory):
                for file in files:
                    total_size += os.path.getsize(os.path.join(root, file))
        except Exception as e:
            print(f"警告: 无法计算目录大小: {e}")
        
        return total_size
    
    def get_statistics(self) -> Dict:
        
        repos = self.list_repos()
        cache_size = self.get_cache_size()
        
        return {
            'total_repos': len(repos),
            'cache_size': cache_size,
            'cache_size_mb': cache_size / (1024 * 1024),
            'repos': repos
        }
    
    def print_statistics(self):
        
        stats = self.get_statistics()
        
        print("\n" + "=" * 60)
        print("多仓库分析统计")
        print("=" * 60)
        print(f"总仓库数: {stats['total_repos']}")
        print(f"缓存大小: {stats['cache_size_mb']:.2f} MB")
        print()
        
        if stats['repos']:
            print("仓库列表:")
            for repo in stats['repos']:
                print(f"  {repo['repo_id']}")
                print(f"    路径: {repo['path']}")
                print(f"    注册时间: {repo['registered_at']}")
                print(f"    最后分 {repo['last_analyzed']}")
                print(f"    缓存大小: {repo['cache_size'] / 1024:.2f} KB")
        
        print("=" * 60)
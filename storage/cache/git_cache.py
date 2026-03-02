
import json
import os
from datetime import datetime
from typing import Dict, Optional, Tuple

from storage.cache.merkle_tree import MerkleNode


class GitCacheManager:
    
    
    def __init__(self, cache_base_dir: str = ".cache/git_repos"):
        
        self.cache_base_dir = cache_base_dir
        self.merkle_cache_dir = os.path.join(cache_base_dir, "merkle_trees")
        self.metadata_dir = os.path.join(cache_base_dir, "metadata")
        
        # 创建缓存目录
        os.makedirs(self.merkle_cache_dir, exist_ok=True)
        os.makedirs(self.metadata_dir, exist_ok=True)
    
    def get_repo_cache_dir(self, repo_name: str) -> str:
        
        return os.path.join(self.cache_base_dir, repo_name)
    
    def get_metadata_file(self, repo_name: str) -> str:
        
        return os.path.join(self.metadata_dir, f"{repo_name}.json")
    
    def get_merkle_tree_file(self, repo_name: str, branch: str) -> str:
        
        safe_branch = branch.replace('/', '_')
        return os.path.join(self.merkle_cache_dir, f"{repo_name}_{safe_branch}.json")
    
    def save_metadata(self, repo_name: str, metadata: Dict) -> bool:
        
        try:
            metadata_file = self.get_metadata_file(repo_name)
            with open(metadata_file, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, indent=2, ensure_ascii=False)
            return True
        except Exception as e:
            print(f"[ERROR] 保存元数据失败: {e}")
            return False
    
    def load_metadata(self, repo_name: str) -> Optional[Dict]:
        
        try:
            metadata_file = self.get_metadata_file(repo_name)
            if os.path.isfile(metadata_file):
                with open(metadata_file, 'r', encoding='utf-8') as f:
                    return json.load(f)
            return None
        except Exception as e:
            print(f"[ERROR] 加载元数据失败: {e}")
            return None
    
    def save_merkle_tree(self, repo_name: str, branch: str, tree: MerkleNode) -> bool:
        
        try:
            merkle_file = self.get_merkle_tree_file(repo_name, branch)
            tree_dict = self._node_to_dict(tree)
            
            with open(merkle_file, 'w', encoding='utf-8') as f:
                json.dump(tree_dict, f, indent=2, ensure_ascii=False)
            return True
        except Exception as e:
            print(f"[ERROR] 保存 Merkle 树失败: {e}")
            return False
    
    def load_merkle_tree(self, repo_name: str, branch: str) -> Optional[MerkleNode]:
        
        try:
            merkle_file = self.get_merkle_tree_file(repo_name, branch)
            if os.path.isfile(merkle_file):
                with open(merkle_file, 'r', encoding='utf-8') as f:
                    tree_dict = json.load(f)
                return self._dict_to_node(tree_dict)
            return None
        except Exception as e:
            print(f"[ERROR] 加载 Merkle 树失败: {e}")
            return None
    
    def update_metadata(self, repo_name: str, repo_url: str, branch: str, commit_hash: str) -> bool:
        
        metadata = {
            'repo_name': repo_name,
            'repo_url': repo_url,
            'branch': branch,
            'commit_hash': commit_hash,
            'last_update_time': datetime.now().isoformat(),
            'cache_dir': self.get_repo_cache_dir(repo_name)
        }
        return self.save_metadata(repo_name, metadata)
    
    def get_cached_commit(self, repo_name: str) -> Optional[str]:
        
        metadata = self.load_metadata(repo_name)
        if metadata:
            return metadata.get('commit_hash')
        return None
    
    def has_changes(self, repo_name: str, branch: str, current_commit: str) -> Tuple[bool, str]:
        
        cached_commit = self.get_cached_commit(repo_name)
        
        if cached_commit is None:
            return True, "首次分析,无缓存"
        
        if cached_commit != current_commit:
            return True, f"commit 已更新: {cached_commit[:8]} → {current_commit[:8]}"
        
        return False, "commit 未变化,无需重新分析"
    
    def cleanup_repo(self, repo_name: str) -> bool:
        
        try:
            import shutil
            
            # 清理仓库目录
            repo_cache_dir = self.get_repo_cache_dir(repo_name)
            if os.path.isdir(repo_cache_dir):
                shutil.rmtree(repo_cache_dir)
            
            # 清理元数据
            metadata_file = self.get_metadata_file(repo_name)
            if os.path.isfile(metadata_file):
                os.remove(metadata_file)
            
            # 清理 Merkle 树缓存
            merkle_dir = self.merkle_cache_dir
            for file in os.listdir(merkle_dir):
                if file.startswith(f"{repo_name}_"):
                    os.remove(os.path.join(merkle_dir, file))
            
            return True
        except Exception as e:
            print(f"[ERROR] 清理缓存失败: {e}")
            return False
    
    def get_cache_info(self, repo_name: str) -> Dict:
        
        metadata = self.load_metadata(repo_name)
        repo_cache_dir = self.get_repo_cache_dir(repo_name)
        
        cache_size = 0
        if os.path.isdir(repo_cache_dir):
            for root, dirs, files in os.walk(repo_cache_dir):
                for file in files:
                    cache_size += os.path.getsize(os.path.join(root, file))
        
        return {
            'repo_name': repo_name,
            'metadata': metadata,
            'cache_dir': repo_cache_dir,
            'cache_size': cache_size,
            'cache_size_mb': cache_size / (1024 * 1024)
        }
    
    @staticmethod
    def _node_to_dict(node: MerkleNode) -> Dict:
        
        children_list = []
        if node.children:
            for child in node.children.values() if isinstance(node.children, dict) else node.children:
                children_list.append(GitCacheManager._node_to_dict(child))
        
        return {
            'path': str(node.path),
            'hash': node.hash,
            'is_file': node.is_file,
            'children': children_list
        }
    
    @staticmethod
    def _dict_to_node(data: Dict) -> MerkleNode:
        
        children_dict = {}
        for child_data in data.get('children', []):
            child_node = GitCacheManager._dict_to_node(child_data)
            child_name = os.path.basename(child_node.path)
            children_dict[child_name] = child_node
        
        return MerkleNode(
            path=data['path'],
            hash=data['hash'],
            is_file=data['is_file'],
            children=children_dict if children_dict else None
        )

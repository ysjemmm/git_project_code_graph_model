import hashlib
import json
import os
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Set

@dataclass
class MerkleNode:
    
    path: str                          # 文件或目录路径
    is_file: bool                      # 是否是文件
    hash: str                          # 节点哈希
    children: Dict[str, 'MerkleNode'] = None  # 子节点(目录)
    
    def __post_init__(self):
        if self.children is None:
            self.children = {}

class MerkleTreeBuilder:
    """默克尔树构建器"""
    
    def __init__(self):
        self.root: Optional[MerkleNode] = None
        self.file_hashes: Dict[str, str] = {}  # 缓存文件哈希
    
    def build(self, directory_path: str) -> MerkleNode:
        
        self.root = self._build_node(directory_path)
        return self.root
    
    def build_incremental(self, directory_path: str, old_tree: Optional[MerkleNode] = None) -> MerkleNode:
        
        if old_tree is None:
            # 没有旧树,进行全量构建
            return self.build(directory_path)
        
        # 增量构建
        self.root = self._build_node_incremental(directory_path, old_tree)
        return self.root
    
    def _build_node(self, path: str) -> MerkleNode:
        
        if os.path.isfile(path):
            # 文件节点
            file_hash = self._hash_file(path)
            return MerkleNode(
                path=path,
                is_file=True,
                hash=file_hash
            )
        else:
            # 目录节点
            children = {}
            child_hashes = []
            
            try:
                for item in sorted(os.listdir(path)):
                    item_path = os.path.join(path, item)
                    
                    # 跳过隐藏文件和非 Java 文件
                    if item.startswith('.'):
                        continue
                    if os.path.isfile(item_path) and not item.endswith('.java'):
                        continue
                    
                    child_node = self._build_node(item_path)
                    children[item] = child_node
                    child_hashes.append(child_node.hash)
            except PermissionError:
                pass
            
            # 计算目录哈希(所有子节点哈希的组合)
            dir_hash = self._hash_list(child_hashes)
            
            return MerkleNode(
                path=path,
                is_file=False,
                hash=dir_hash,
                children=children
            )
    
    def _hash_file(self, file_path: str) -> str:
        
        sha256_hash = hashlib.sha256()
        try:
            with open(file_path, 'rb') as f:
                for byte_block in iter(lambda: f.read(4096), b""):
                    sha256_hash.update(byte_block)
            return sha256_hash.hexdigest()
        except Exception as e:
            print(f"警告: 无法计算文件哈希 {file_path}: {e}")
            return ""
    
    def _hash_list(self, hashes: List[str]) -> str:
        """计算哈希列表的组合"""
        combined = ''.join(hashes)
        return hashlib.sha256(combined.encode()).hexdigest()
    
    def _build_node_incremental(self, path: str, old_node: Optional[MerkleNode]) -> MerkleNode:
        
        if os.path.isfile(path):
            # 文件节点
            file_hash = self._hash_file(path)
            
            # 如果旧节点存在且哈希相同,直接复制
            if old_node and old_node.is_file and old_node.hash == file_hash:
                return old_node
            
            return MerkleNode(
                path=path,
                is_file=True,
                hash=file_hash
            )
        else:
            # 目录节点
            children = {}
            child_hashes = []
            old_children = (old_node.children if old_node and not old_node.is_file else None) or {}
            
            try:
                for item in sorted(os.listdir(path)):
                    item_path = os.path.join(path, item)
                    
                    # 跳过隐藏文件和非 Java 文件
                    if item.startswith('.'):
                        continue
                    if os.path.isfile(item_path) and not item.endswith('.java'):
                        continue
                    
                    # 获取旧的子节点(如果存在)
                    old_child = old_children.get(item)
                    
                    # 增量构建子节点
                    child_node = self._build_node_incremental(item_path, old_child)
                    children[item] = child_node
                    child_hashes.append(child_node.hash)
            except PermissionError:
                pass
            
            # 计算目录哈希
            dir_hash = self._hash_list(child_hashes)
            
            # 如果旧节点存在且哈希相同,直接复制
            if old_node and not old_node.is_file and old_node.hash == dir_hash:
                return old_node
            
            return MerkleNode(
                path=path,
                is_file=False,
                hash=dir_hash,
                children=children
            )

class MerkleTreeComparator:
    """默克尔树比较"""
    
    def __init__(self):
        self.modified_files: Set[str] = set()
        self.new_files: Set[str] = set()
        self.deleted_files: Set[str] = set()
    
    def compare(self, old_tree: Optional[MerkleNode], new_tree: MerkleNode) -> Dict:
        
        self.modified_files = set()
        self.new_files = set()
        self.deleted_files = set()
        
        if old_tree is None:
            # 第一次构建,所有文件都是新的
            self._collect_all_files(new_tree, self.new_files)
        else:
            # 比较两棵树
            if old_tree.hash != new_tree.hash:
                # 根哈希不同,说明有变化
                self._compare_nodes(old_tree, new_tree)
            # 如果根哈希相同,说明完全没有变化
        
        return {
            'modified': sorted(list(self.modified_files)),
            'new': sorted(list(self.new_files)),
            'deleted': sorted(list(self.deleted_files)),
            'changed': len(self.modified_files) > 0 or len(self.new_files) > 0 or len(self.deleted_files) > 0
        }
    
    def _compare_nodes(self, old_node: MerkleNode, new_node: MerkleNode):
        
        # 如果哈希相同,说明该子树没有变化
        if old_node.hash == new_node.hash:
            return
        
        # 如果是文件且哈希不同,说明文件被修改
        if old_node.is_file and new_node.is_file:
            self.modified_files.add(new_node.path)
            return
        
        # 如果是目录,递归比较子节点
        if not old_node.is_file and not new_node.is_file:
            self._compare_directories(old_node, new_node)
    
    def _compare_directories(self, old_dir: MerkleNode, new_dir: MerkleNode):
        
        old_children = old_dir.children or {}
        new_children = new_dir.children or {}
        
        # 检查修改和新增的文件
        for name, new_child in new_children.items():
            if name in old_children:
                old_child = old_children[name]
                # 如果哈希不同,说明有变化
                if old_child.hash != new_child.hash:
                    if new_child.is_file:
                        # 文件被修改
                        self.modified_files.add(new_child.path)
                    else:
                        # 目录有变化,递归比较
                        self._compare_nodes(old_child, new_child)
            else:
                # 新增文件或目录
                self._collect_all_files(new_child, self.new_files)
        
        # 检查删除的文件
        for name, old_child in old_children.items():
            if name not in new_children:
                # 删除文件或目录
                self._collect_all_files(old_child, self.deleted_files)
    
    def _collect_all_files(self, node: MerkleNode, file_set: Set[str]):
        """收集节点下的所有文件"""
        if node.is_file:
            file_set.add(node.path)
        else:
            for child in (node.children or {}).values():
                self._collect_all_files(child, file_set)

class MerkleTreeCache:
    
    
    def __init__(self, cache_file: str = ".kiro/merkle_tree_cache.json"):
        self.cache_file = cache_file
        self.tree_data: Optional[Dict] = None
        self.load_cache()
    
    def load_cache(self):
        """从缓存文件加载默克尔树"""
        if os.path.isfile(self.cache_file):
            try:
                with open(self.cache_file, 'r', encoding='utf-8') as f:
                    self.tree_data = json.load(f)
            except Exception as e:
                print(f"警告: 无法加载默克尔树缓存 {self.cache_file}: {e}")
    
    def save_cache(self, tree: MerkleNode):
        """保存默克尔树到缓存文件"""
        os.makedirs(os.path.dirname(self.cache_file), exist_ok=True)
        try:
            tree_data = self._node_to_dict(tree)
            with open(self.cache_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'tree': tree_data,
                    'timestamp': datetime.now().isoformat()
                }, f, indent=2)
        except Exception as e:
            print(f"警告: 无法保存默克尔树缓存 {self.cache_file}: {e}")
    
    def get_cached_tree(self) -> Optional[MerkleNode]:
        """从缓存获取默克尔树"""
        if self.tree_data is None:
            return None
        
        tree_dict = self.tree_data.get('tree')
        if tree_dict is None:
            return None
        
        return self._dict_to_node(tree_dict)
    
    def _node_to_dict(self, node: MerkleNode) -> Dict:
        
        return {
            'path': node.path,
            'is_file': node.is_file,
            'hash': node.hash,
            'children': {
                name: self._node_to_dict(child)
                for name, child in (node.children or {}).items()
            }
        }
    
    def _dict_to_node(self, data: Dict) -> MerkleNode:
        
        children = {}
        for name, child_data in data.get('children', {}).items():
            children[name] = self._dict_to_node(child_data)
        
        return MerkleNode(
            path=data['path'],
            is_file=data['is_file'],
            hash=data['hash'],
            children=children if children else None
        )

class MerkleTreeAnalyzer:
    
    
    def __init__(self, cache_dir: str = ".kiro"):
        self.cache_dir = cache_dir
        self.cache = MerkleTreeCache(os.path.join(cache_dir, "merkle_tree_cache.json"))
        self.builder = MerkleTreeBuilder()
        self.comparator = MerkleTreeComparator()
    
    def analyze_changes(self, directory_path: str) -> Dict:
        
        # 获取缓存的旧树
        old_tree = self.cache.get_cached_tree()
        
        # 使用增量构建树
        new_tree = self.builder.build_incremental(directory_path, old_tree)
        
        # 比较两棵树
        changes = self.comparator.compare(old_tree, new_tree)
        
        return {
            **changes,
            'tree': new_tree,
            'method': 'incremental' if old_tree else 'full'
        }
    
    def analyze_changes_full(self, directory_path: str) -> Dict:
        
        # 全量构建(不使用缓存)
        new_tree = self.builder.build(directory_path)
        
        # 获取缓存的旧树
        old_tree = self.cache.get_cached_tree()
        
        # 比较两棵树
        changes = self.comparator.compare(old_tree, new_tree)
        
        return {
            **changes,
            'tree': new_tree,
            'method': 'full'
        }
    
    def update_cache(self, tree: MerkleNode):
        
        self.cache.save_cache(tree)
    
    def get_tree_statistics(self, tree: MerkleNode) -> Dict:
        
        file_count = 0
        dir_count = 0
        total_size = 0
        
        def traverse(node: MerkleNode):
            nonlocal file_count, dir_count, total_size
            
            if node.is_file:
                file_count += 1
                try:
                    total_size += os.path.getsize(node.path)
                except:
                    pass
            else:
                dir_count += 1
                for child in (node.children or {}).values():
                    traverse(child)
        
        traverse(tree)
        
        return {
            'file_count': file_count,
            'dir_count': dir_count,
            'total_size': total_size,
            'root_hash': tree.hash
        }
    
    def print_tree(self, node: Optional[MerkleNode] = None, indent: int = 0):
        
        if node is None:
            return
        
        prefix = "  " * indent
        if node.is_file:
            print(f"{prefix}📄 {os.path.basename(node.path)} ({node.hash[:8]}...)")
        else:
            print(f"{prefix}📁 {os.path.basename(node.path)} ({node.hash[:8]}...)")
            for child in (node.children or {}).values():
                self.print_tree(child, indent + 1)
    
    def compare_trees(self, old_tree: Optional[MerkleNode], new_tree: MerkleNode) -> Dict:
        
        return self.comparator.compare(old_tree, new_tree)
    
    def get_tree_size(self, node: Optional[MerkleNode] = None) -> int:
        
        if node is None:
            return 0
        
        count = 1
        for child in (node.children or {}).values():
            count += self.get_tree_size(child)
        
        return count
    
    def get_tree_depth(self, node: Optional[MerkleNode] = None) -> int:
        
        if node is None:
            return 0
        
        if node.is_file:
            return 1
        
        if not node.children:
            return 1
        
        max_depth = 0
        for child in node.children.values():
            depth = self.get_tree_depth(child)
            max_depth = max(max_depth, depth)
        
        return max_depth + 1
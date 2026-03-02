

import hashlib
import json
import os
from datetime import datetime
from typing import Dict, Set, List, Optional, Tuple

from storage.cache.merkle_tree import MerkleTreeAnalyzer


class FileChangeTracker:
    """文件变化追踪器"""
    
    def __init__(self, cache_file: str = ".kiro/file_cache.json"):
        self.cache_file = cache_file
        self.file_hashes: Dict[str, str] = {}
        self.file_mtimes: Dict[str, float] = {}
        self.load_cache()
    
    def load_cache(self):
        
        if os.path.isfile(self.cache_file):
            try:
                with open(self.cache_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    self.file_hashes = data.get('hashes', {})
                    self.file_mtimes = data.get('mtimes', {})
            except Exception as e:
                print(f"警告: 无法加载缓存文件 {self.cache_file}: {e}")
    
    def save_cache(self):
        
        os.makedirs(os.path.dirname(self.cache_file), exist_ok=True)
        try:
            with open(self.cache_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'hashes': self.file_hashes,
                    'mtimes': self.file_mtimes,
                    'timestamp': datetime.now().isoformat()
                }, f, indent=2)
        except Exception as e:
            print(f"警告: 无法保存缓存文件 {self.cache_file}: {e}")
    
    def get_file_hash(self, file_path: str) -> str:
        
        sha256_hash = hashlib.sha256()
        try:
            with open(file_path, 'rb') as f:
                for byte_block in iter(lambda: f.read(4096), b""):
                    sha256_hash.update(byte_block)
            return sha256_hash.hexdigest()
        except Exception as e:
            print(f"警告: 无法计算文件哈希 {file_path}: {e}")
            return ""
    
    def get_file_mtime(self, file_path: str) -> float:
        """获取文件的修改时"""
        try:
            return os.path.getmtime(file_path)
        except Exception as e:
            print(f"警告: 无法获取文件修改时间 {file_path}: {e}")
            return 0.0
    
    def detect_changes(self, file_paths: List[str]) -> Tuple[Set[str], Set[str], Set[str]]:
        
        current_files = set(file_paths)
        previous_files = set(self.file_hashes.keys())
        
        modified_files = set()
        new_files = current_files - previous_files
        deleted_files = previous_files - current_files
        
        # 检测修改的文件
        for file_path in current_files & previous_files:
            current_hash = self.get_file_hash(file_path)
            if current_hash and current_hash != self.file_hashes.get(file_path):
                modified_files.add(file_path)
        
        return modified_files, new_files, deleted_files
    
    def update_cache(self, file_paths: List[str]):
        
        for file_path in file_paths:
            self.file_hashes[file_path] = self.get_file_hash(file_path)
            self.file_mtimes[file_path] = self.get_file_mtime(file_path)
    
    def remove_from_cache(self, file_paths: List[str]):
        
        for file_path in file_paths:
            self.file_hashes.pop(file_path, None)
            self.file_mtimes.pop(file_path, None)

class SymbolTableCache:
    """符号表缓"""
    
    def __init__(self, cache_file: str = ".kiro/symbol_table_cache.json"):
        self.cache_file = cache_file
        self.file_symbols: Dict[str, List[str]] = {}  # file_path -> [symbol_ids]
        self.symbol_files: Dict[str, str] = {}  # symbol_id -> file_path
        self.load_cache()
    
    def load_cache(self):
        
        if os.path.isfile(self.cache_file):
            try:
                with open(self.cache_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    self.file_symbols = data.get('file_symbols', {})
                    self.symbol_files = data.get('symbol_files', {})
            except Exception as e:
                print(f"警告: 无法加载符号表缓{self.cache_file}: {e}")
    
    def save_cache(self):
        
        os.makedirs(os.path.dirname(self.cache_file), exist_ok=True)
        try:
            with open(self.cache_file, 'w', encoding='utf-8') as f:
                json.dump({
                    'file_symbols': self.file_symbols,
                    'symbol_files': self.symbol_files,
                    'timestamp': datetime.now().isoformat()
                }, f, indent=2)
        except Exception as e:
            print(f"警告: 无法保存符号表缓{self.cache_file}: {e}")
    
    def register_symbols(self, file_path: str, symbol_ids: List[str]):
        
        self.file_symbols[file_path] = symbol_ids
        for symbol_id in symbol_ids:
            self.symbol_files[symbol_id] = file_path
    
    def get_symbols_by_file(self, file_path: str) -> List[str]:
        """获取文件中的所有符"""
        return self.file_symbols.get(file_path, [])
    
    def get_file_by_symbol(self, symbol_id: str) -> Optional[str]:
        
        return self.symbol_files.get(symbol_id)
    
    def remove_file_symbols(self, file_path: str):
        """移除文件中的所有符"""
        symbol_ids = self.file_symbols.pop(file_path, [])
        for symbol_id in symbol_ids:
            self.symbol_files.pop(symbol_id, None)

class IncrementalAnalyzer:
    """增量分析- 使用默克尔树进行高效的文件变化检"""
    
    def __init__(self, cache_dir: str = ".kiro"):
        self.cache_dir = cache_dir
        self.file_tracker = FileChangeTracker(os.path.join(cache_dir, "file_cache.json"))
        self.symbol_cache = SymbolTableCache(os.path.join(cache_dir, "symbol_table_cache.json"))
        self.merkle_analyzer = MerkleTreeAnalyzer(cache_dir)
    
    def analyze_changes(self, directory_path: str) -> Dict:
        
        # 使用默克尔树检测变化
        result = self.merkle_analyzer.analyze_changes(directory_path)
        
        # 获取所有文件
        all_files = self._find_java_files(directory_path)
        unchanged = set(all_files) - set(result['modified']) - set(result['new']) - set(result['deleted'])
        
        return {
            'modified': sorted(result['modified']),
            'new': sorted(result['new']),
            'deleted': sorted(result['deleted']),
            'unchanged': sorted(list(unchanged)),
            'total': len(all_files),
            'need_reanalysis': len(result['modified']) > 0 or len(result['new']) > 0 or len(result['deleted']) > 0,
            'detection_method': 'merkle_tree'
        }
    
    def get_affected_symbols(self, modified_files: List[str]) -> Set[str]:
        
        affected_symbols = set()
        for file_path in modified_files:
            symbols = self.symbol_cache.get_symbols_by_file(file_path)
            affected_symbols.update(symbols)
        return affected_symbols
    
    def get_dependent_symbols(self, affected_symbols: Set[str], symbol_table) -> Set[str]:
        
        dependent_symbols = set()
        
        # 查找引用受影响符号的其他符号
        for edge in symbol_table.call_edges:
            if edge.target_symbol in affected_symbols:
                dependent_symbols.add(edge.source_symbol)
        
        for edge in symbol_table.access_edges:
            if edge.target_symbol in affected_symbols:
                dependent_symbols.add(edge.source_symbol)
        
        for edge in symbol_table.inheritance_edges:
            if edge.target_symbol in affected_symbols:
                dependent_symbols.add(edge.source_symbol)
        
        for edge in symbol_table.type_edges:
            if edge.target_symbol in affected_symbols:
                dependent_symbols.add(edge.source_symbol)
        
        return dependent_symbols
    
    def _find_java_files(self, directory_path: str) -> List[str]:
        
        java_files = []
        for root, dirs, files in os.walk(directory_path):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        return sorted(java_files)
    
    def update_cache(self, file_paths: List[str], symbol_table):
        
        # 更新文件哈希
        self.file_tracker.update_cache(file_paths)
        self.file_tracker.save_cache()
        
        # 更新符号表缓
        for file_path in file_paths:
            # 获取该文件中的所有符
            file_symbols = []
            for symbol_id, symbol in symbol_table.symbols.items():
                if symbol.file_path == file_path:
                    file_symbols.append(symbol_id)
            self.symbol_cache.register_symbols(file_path, file_symbols)
        
        self.symbol_cache.save_cache()
    
    def cleanup_deleted_files(self, deleted_files: List[str], symbol_table):
        
        for file_path in deleted_files:
            # 从符号表中移除该文件的符
            symbols_to_remove = self.symbol_cache.get_symbols_by_file(file_path)
            for symbol_id in symbols_to_remove:
                symbol_table.symbols.pop(symbol_id, None)
            
            # 从缓存中移除
            self.symbol_cache.remove_file_symbols(file_path)
        
        self.symbol_cache.save_cache()

class IncrementalCodeGraphBuilder:
    """增量代码图谱构建立"""
    
    def __init__(self, base_builder, cache_dir: str = ".kiro"):
        
        self.base_builder = base_builder
        self.incremental_analyzer = IncrementalAnalyzer(cache_dir)
    
    def build_incremental(self, directory_path: str, force_full: bool = False) -> Dict:
        
        import time
        start_time = time.time()
        
        # 使用默克尔树分析变化
        changes = self.incremental_analyzer.analyze_changes(directory_path)
        
        # 如果没有变化且不强制全量分析,直接返
        if not changes['need_reanalysis'] and not force_full:
            analysis_time = time.time() - start_time
            return {
                'status': 'unchanged',
                'changes': changes,
                'affected_symbols': [],
                'dependent_symbols': [],
                'analysis_time': analysis_time,
                'symbol_table': self.base_builder.symbol_table,
                'detection_method': 'merkle_tree'
            }
        
        # 如果没有变化或强制全量分析,进行全量分析
        if not changes['need_reanalysis'] or force_full:
            print("执行全量分析...")
            self.base_builder.build_from_directory(directory_path)
            
            # 获取所有文件并更新缓存
            java_files = self._find_java_files(directory_path)
            self.incremental_analyzer.update_cache(java_files, self.base_builder.symbol_table)
            
            # 更新默克尔树缓存
            result = self.incremental_analyzer.merkle_analyzer.analyze_changes(directory_path)
            self.incremental_analyzer.merkle_analyzer.update_cache(result['tree'])
            
            analysis_time = time.time() - start_time
            return {
                'status': 'full',
                'changes': changes,
                'affected_symbols': [],
                'dependent_symbols': [],
                'analysis_time': analysis_time,
                'symbol_table': self.base_builder.symbol_table,
                'detection_method': 'merkle_tree'
            }
        
        # 增量分析
        print("执行增量分析(使用默克尔树)...")
        
        # 获取受影响的符号
        affected_symbols = self.incremental_analyzer.get_affected_symbols(
            changes['modified'] + changes['new']
        )
        
        # 获取依赖的符
        dependent_symbols = self.incremental_analyzer.get_dependent_symbols(
            affected_symbols, self.base_builder.symbol_table
        )
        
        # 清理已删除文件的符号
        if changes['deleted']:
            self.incremental_analyzer.cleanup_deleted_files(
                changes['deleted'], self.base_builder.symbol_table
            )
        
        # 重新分析修改和新增的文件
        files_to_analyze = changes['modified'] + changes['new']
        if files_to_analyze:
            self.base_builder.build_from_files(files_to_analyze)
        
        # 获取所有文件并更新缓存
        java_files = self._find_java_files(directory_path)
        self.incremental_analyzer.update_cache(java_files, self.base_builder.symbol_table)
        
        # 更新默克尔树缓存
        result = self.incremental_analyzer.merkle_analyzer.analyze_changes(directory_path)
        self.incremental_analyzer.merkle_analyzer.update_cache(result['tree'])
        
        analysis_time = time.time() - start_time
        return {
            'status': 'incremental',
            'changes': changes,
            'affected_symbols': sorted(list(affected_symbols)),
            'dependent_symbols': sorted(list(dependent_symbols)),
            'analysis_time': analysis_time,
            'symbol_table': self.base_builder.symbol_table,
            'detection_method': 'merkle_tree'
        }
    
    def _find_java_files(self, directory_path: str) -> List[str]:
        
        java_files = []
        for root, dirs, files in os.walk(directory_path):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        return sorted(java_files)
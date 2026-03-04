"""
包路径前缀树 (Package Trie)

用于处理通配符 import 情况（如 import com.ysj.*）
支持按需构建和懒加载

结构:
  根节点 (虚拟)
  ├── com (非叶子 - package)
  │   └── ysj (非叶子 - package)
  │       ├── Car (叶子 - Symbol)
  │       ├── User (叶子 - Symbol)
  │       └── Service (叶子 - Symbol)
  └── java (非叶子 - package)
      └── util (非叶子 - package)
          ├── List (叶子 - Symbol)
          └── Map (叶子 - Symbol)
"""

from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class TrieNode:
    """前缀树节点"""
    
    # 子节点: 路径段 -> TrieNode
    # 例如: "com" -> TrieNode, "ysj" -> TrieNode
    children: Dict[str, 'TrieNode'] = field(default_factory=dict)
    
    # 叶子节点才有值，存储 Symbol 对象
    # 非叶子节点（package 节点）此值为 None
    symbol: Optional[object] = None
    
    # 是否是 package 节点（非叶子）
    is_package: bool = False
    
    def is_leaf(self) -> bool:
        """判断是否是叶子节点"""
        return self.symbol is not None
    
    def add_child(self, segment: str) -> 'TrieNode':
        """添加或获取子节点"""
        if segment not in self.children:
            self.children[segment] = TrieNode()
        return self.children[segment]
    
    def get_child(self, segment: str) -> Optional['TrieNode']:
        """获取子节点"""
        return self.children.get(segment)


class PackageTrie:
    """
    包路径前缀树
    
    用于快速查询和管理 Java 包路径和类名的映射关系
    支持精确查询、通配符查询和范围查询
    """
    
    def __init__(self):
        """初始化前缀树"""
        # 根节点（虚拟节点）
        self.root = TrieNode()
        self.root.is_package = True
        
        # 统计信息
        self._total_symbols = 0
        self._total_packages = 0
    
    def insert(self, fqn: str, symbol: object) -> None:
        """
        插入一个完全限定名和对应的 Symbol
        
        Args:
            fqn: 完全限定名，如 "com.ysj.Car"
            symbol: Symbol 对象
        
        Example:
            trie.insert("com.ysj.Car", car_symbol)
            trie.insert("com.ysj.User", user_symbol)
        """
        if not fqn or not symbol:
            return
        
        # 按 . 分割路径
        segments = fqn.split('.')
        
        # 从根节点开始遍历
        current = self.root
        
        # 遍历所有段，除了最后一个（最后一个是类名）
        for i, segment in enumerate(segments[:-1]):
            next_node = current.add_child(segment)
            next_node.is_package = True
            current = next_node
            
            # 统计 package 节点
            if i == len(segments) - 2:  # 最后一个 package 节点
                self._total_packages += 1
        
        # 最后一个段是类名，创建叶子节点
        class_name = segments[-1]
        leaf_node = current.add_child(class_name)
        leaf_node.symbol = symbol
        leaf_node.is_package = False
        
        # 统计 symbol
        self._total_symbols += 1
    
    def search(self, fqn: str) -> Optional[object]:
        """
        精确查询：查找完全限定名对应的 Symbol
        
        Args:
            fqn: 完全限定名，如 "com.ysj.Car"
        
        Returns:
            Symbol 对象，如果不存在返回 None
        
        Time Complexity: O(m)，m 是路径长度
        
        Example:
            symbol = trie.search("com.ysj.Car")
        """
        if not fqn:
            return None
        
        segments = fqn.split('.')
        current = self.root
        
        # 遍历所有段
        for segment in segments:
            current = current.get_child(segment)
            if current is None:
                return None
        
        # 返回叶子节点的 symbol
        return current.symbol if current.is_leaf() else None
    
    def search_wildcard(self, package_path: str) -> List[object]:
        """
        通配符查询：查找包下的所有直接类
        
        Args:
            package_path: 包路径，如 "com.ysj"
        
        Returns:
            该包下所有直接类的 Symbol 列表
        
        Time Complexity: O(m + n)，m 是路径长度，n 是该包下的类数量
        
        Example:
            symbols = trie.search_wildcard("com.ysj")
            # 返回 [Car, User, Service]
        """
        if not package_path:
            return []
        
        segments = package_path.split('.')
        current = self.root
        
        # 遍历到指定的 package 节点
        for segment in segments:
            current = current.get_child(segment)
            if current is None:
                return []
        
        # 返回该 package 下的所有直接子节点的 symbol
        result = []
        for child in current.children.values():
            if child.is_leaf():
                result.append(child.symbol)
        
        return result
    
    def search_recursive(self, package_path: str) -> List[object]:
        """
        递归查询：查找包及其子包下的所有类
        
        Args:
            package_path: 包路径，如 "com.ysj"
        
        Returns:
            该包及其子包下所有类的 Symbol 列表
        
        Time Complexity: O(m + k)，m 是路径长度，k 是结果数量
        
        Example:
            symbols = trie.search_recursive("com.ysj")
            # 返回 [Car, User, Service, 以及 com.ysj.* 下的所有类]
        """
        if not package_path:
            return []
        
        segments = package_path.split('.')
        current = self.root
        
        # 遍历到指定的 package 节点
        for segment in segments:
            current = current.get_child(segment)
            if current is None:
                return []
        
        # 递归收集所有叶子节点
        result = []
        self._collect_all_symbols(current, result)
        return result
    
    def contains(self, fqn: str) -> bool:
        """
        检查是否存在指定的完全限定名
        
        Args:
            fqn: 完全限定名
        
        Returns:
            True 如果存在，False 否则
        
        Example:
            if trie.contains("com.ysj.Car"):
                print("Found")
        """
        return self.search(fqn) is not None
    
    def get_package_symbols(self, package_path: str) -> Dict[str, object]:
        """
        获取包下的所有直接类（以字典形式）
        
        Args:
            package_path: 包路径
        
        Returns:
            {类名 -> Symbol} 字典
        
        Example:
            symbols = trie.get_package_symbols("com.ysj")
            # 返回 {"Car": car_symbol, "User": user_symbol, ...}
        """
        if not package_path:
            return {}
        
        segments = package_path.split('.')
        current = self.root
        
        # 遍历到指定的 package 节点
        for segment in segments:
            current = current.get_child(segment)
            if current is None:
                return {}
        
        # 收集直接子节点
        result = {}
        for class_name, child in current.children.items():
            if child.is_leaf():
                result[class_name] = child.symbol
        
        return result
    
    def get_all_packages(self) -> List[str]:
        """
        获取所有 package 路径
        
        Returns:
            所有 package 路径列表
        
        Example:
            packages = trie.get_all_packages()
            # 返回 ["com", "com.ysj", "java", "java.util", ...]
        """
        packages = []
        self._collect_packages(self.root, "", packages)
        return packages
    
    def get_all_symbols(self) -> List[tuple]:
        """
        获取所有 (fqn, symbol) 对
        
        Returns:
            [(完全限定名, Symbol), ...] 列表
        
        Example:
            all_symbols = trie.get_all_symbols()
        """
        result = []
        self._collect_all_fqn_symbols(self.root, "", result)
        return result
    
    def delete(self, fqn: str) -> bool:
        """
        删除指定的完全限定名
        
        Args:
            fqn: 完全限定名
        
        Returns:
            True 如果删除成功，False 如果不存在
        
        Note:
            删除后会清理空的 package 节点
        
        Example:
            if trie.delete("com.ysj.Car"):
                print("Deleted")
        """
        if not fqn:
            return False
        
        segments = fqn.split('.')
        
        # 找到叶子节点的父节点
        current = self.root
        path = [self.root]
        
        for segment in segments[:-1]:
            current = current.get_child(segment)
            if current is None:
                return False
            path.append(current)
        
        # 检查叶子节点
        class_name = segments[-1]
        leaf = current.get_child(class_name)
        if leaf is None or not leaf.is_leaf():
            return False
        
        # 删除叶子节点
        del current.children[class_name]
        self._total_symbols -= 1
        
        # 清理空的 package 节点（从下往上）
        for i in range(len(path) - 1, 0, -1):
            parent = path[i - 1]
            segment = segments[i - 1] if i - 1 < len(segments) - 1 else segments[-2]
            
            node = parent.get_child(segment)
            if node and len(node.children) == 0 and not node.is_leaf():
                del parent.children[segment]
                self._total_packages -= 1
            else:
                break
        
        return True
    
    def clear(self) -> None:
        """清空前缀树"""
        self.root = TrieNode()
        self.root.is_package = True
        self._total_symbols = 0
        self._total_packages = 0
    
    def get_statistics(self) -> Dict[str, int]:
        """
        获取统计信息
        
        Returns:
            包含统计数据的字典
        
        Example:
            stats = trie.get_statistics()
            # {"total_symbols": 100, "total_packages": 10, "depth": 5}
        """
        return {
            'total_symbols': self._total_symbols,
            'total_packages': self._total_packages,
            'max_depth': self._get_max_depth(self.root),
            'total_nodes': self._count_nodes(self.root)
        }
    
    def to_dict(self) -> Dict[str, any]:
        """
        将前缀树转换为字典（用于 JSON 序列化）
        
        Returns:
            包含所有 (fqn, symbol) 对的字典
        
        Example:
            data = trie.to_dict()
            # {"com.ysj.Car": symbol, "com.ysj.User": symbol, ...}
        """
        result = {}
        for fqn, symbol in self.get_all_symbols():
            # 如果 symbol 有 to_dict 方法，使用它；否则直接存储
            if hasattr(symbol, 'to_dict'):
                result[fqn] = symbol.to_dict()
            else:
                result[fqn] = symbol
        return result
    
    # ==================== 私有方法 ====================
    
    def _collect_all_symbols(self, node: TrieNode, result: List[object]) -> None:
        """递归收集所有叶子节点的 symbol"""
        if node.is_leaf():
            result.append(node.symbol)
        
        for child in node.children.values():
            self._collect_all_symbols(child, result)
    
    def _collect_packages(self, node: TrieNode, prefix: str, result: List[str]) -> None:
        """递归收集所有 package 路径"""
        if prefix and node.is_package:
            result.append(prefix.rstrip('.'))
        
        for segment, child in node.children.items():
            new_prefix = f"{prefix}{segment}." if prefix else f"{segment}."
            self._collect_packages(child, new_prefix, result)
    
    def _collect_all_fqn_symbols(self, node: TrieNode, prefix: str, result: List[tuple]) -> None:
        """递归收集所有 (fqn, symbol) 对"""
        if node.is_leaf():
            fqn = prefix.rstrip('.')
            result.append((fqn, node.symbol))
        
        for segment, child in node.children.items():
            new_prefix = f"{prefix}{segment}." if prefix else f"{segment}."
            self._collect_all_fqn_symbols(child, new_prefix, result)
    
    def _get_max_depth(self, node: TrieNode, depth: int = 0) -> int:
        """获取树的最大深度"""
        if not node.children:
            return depth
        
        max_child_depth = 0
        for child in node.children.values():
            child_depth = self._get_max_depth(child, depth + 1)
            max_child_depth = max(max_child_depth, child_depth)
        
        return max_child_depth
    
    def _count_nodes(self, node: TrieNode) -> int:
        """计算树中的总节点数"""
        count = 1
        for child in node.children.values():
            count += self._count_nodes(child)
        return count


class LazyPackageTrie:
    """
    懒加载的包路径前缀树
    
    只在需要时构建前缀树，避免不必要的初始化开销
    """
    
    def __init__(self, symbol_loader=None):
        """
        初始化懒加载前缀树
        
        Args:
            symbol_loader: 符号加载器回调函数，签名为 (fqn: str) -> Optional[Symbol]
                          当查询时找不到符号，会调用此回调动态加载
        """
        self._trie: Optional[PackageTrie] = None
        self._symbol_loader = symbol_loader
        self._is_built = False
    
    @property
    def trie(self) -> PackageTrie:
        """获取前缀树，如果未构建则自动构建"""
        if not self._is_built:
            self._build()
        return self._trie
    
    def _build(self) -> None:
        """构建前缀树"""
        self._trie = PackageTrie()
        self._is_built = True
    
    def search(self, fqn: str) -> Optional[object]:
        """精确查询"""
        result = self.trie.search(fqn)
        
        # 如果未找到且有加载器，尝试动态加载
        if result is None and self._symbol_loader:
            symbol = self._symbol_loader(fqn)
            if symbol:
                self.trie.insert(fqn, symbol)
                return symbol
        
        return result
    
    def search_wildcard(self, package_path: str) -> List[object]:
        """通配符查询"""
        result = self.trie.search_wildcard(package_path)
        
        # 如果结果为空且有加载器，尝试从加载器获取该包下的所有符号
        if not result and self._symbol_loader:
            # 这里需要特殊处理：加载器需要支持通配符查询
            # 或者我们遍历已知的符号，查找匹配的
            pass
        
        return result
    
    def search_recursive(self, package_path: str) -> List[object]:
        """递归查询"""
        return self.trie.search_recursive(package_path)
    
    def contains(self, fqn: str) -> bool:
        """检查是否存在"""
        return self.trie.contains(fqn)
    
    def insert(self, fqn: str, symbol: object) -> None:
        """插入符号"""
        self.trie.insert(fqn, symbol)
    
    def delete(self, fqn: str) -> bool:
        """删除符号"""
        return self.trie.delete(fqn)
    
    def is_built(self) -> bool:
        """检查是否已构建"""
        return self._is_built
    
    def reset(self) -> None:
        """重置前缀树"""
        self._trie = None
        self._is_built = False

from typing import Optional, Callable, Any

import psutil


class OptionalImport:
    
    
    def __init__(self, module_name: str, fallback: Optional[Callable] = None):
        
        self.module_name = module_name
        self.fallback = fallback
        self._module = None
        self._available = False
        self._load()
    
    def _load(self):
        
        try:
            self._module = __import__(self.module_name)
            self._available = True
        except ImportError:
            self._available = False
            if self.fallback:
                self.fallback()
    
    @property
    def available(self) -> bool:
        
        return self._available
    
    @property
    def module(self) -> Any:
        
        if not self._available:
            raise ImportError(f"模块 {self.module_name} 不可")
        return self._module
    
    def __getattr__(self, name: str) -> Any:
        """获取模块属"""
        if not self._available:
            raise ImportError(f"模块 {self.module_name} 不可用,无法访问 {name}")
        return getattr(self._module, name)

class MemoryProvider:
    """内存信息提供"""
    
    @staticmethod
    def get_memory_info() -> dict:
        
        # 尝试使用 psutil
        try:
            process = psutil.Process()
            return {
                'rss': process.memory_info().rss,
                'available': True,
                'source': 'psutil'
            }
        except ImportError:
            pass
        
        # 尝试使用 resource(Unix
        try:
            import resource
            usage = resource.getrusage(resource.RUSAGE_SELF)
            return {
                'rss': usage.ru_maxrss * 1024,
                'available': True,
                'source': 'resource'
            }
        except (ImportError, AttributeError):
            pass
        
        # 备用方案:使gc 模块估算
        import gc
        return {
            'rss': len(gc.get_objects()) * 100,  # 粗略估算
            'available': False,
            'source': 'gc_estimate'
        }

# 预定义的可选导
psutil_import = OptionalImport('psutil')
resource_import = OptionalImport('resource')

def get_memory_usage() -> int:
    
    info = MemoryProvider.get_memory_info()
    return info['rss']

def get_memory_source() -> str:
    
    info = MemoryProvider.get_memory_info()
    return info['source']

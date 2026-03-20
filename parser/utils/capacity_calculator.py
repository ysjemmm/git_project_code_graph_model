

import multiprocessing
from dataclasses import dataclass

@dataclass
class SystemResources:
    
    cpu_cores: int
    memory_gb: int
    
    @classmethod
    def detect(cls) -> 'SystemResources':
        """检测系统资源"""
        cpu_cores = multiprocessing.cpu_count()
        
        # 尝试检测内
        try:
            import psutil
            memory_gb = psutil.virtual_memory().total // (1024 ** 3)
        except ImportError:
            # 备用方案:假设内存为 CPU 核心× 2GB
            memory_gb = cpu_cores * 2
        
        return cls(cpu_cores=cpu_cores, memory_gb=memory_gb)

@dataclass
class ConcurrencyConfig:
    
    repo_workers: int          # 多仓库并发数
    project_workers: int       # 项目并发
    memory_per_project_mb: int # 每个项目的内存限
    total_concurrent_files: int  # 总并发文件数
    total_memory_mb: int       # 总内存使

class CapacityCalculator:
    """并发容量计算器"""
    
    # 内存消耗模型(MB
    BASE_MEMORY = 100          # 基础内存
    REPO_CACHE = 50            # 每个仓库缓存
    PROJECT_CACHE = 100        # 每个项目缓存
    FILE_CACHE = 50            # 每个文件缓存
    
    # 安全系数
    MEMORY_SAFETY_FACTOR = 0.8  # 只使80% 的可用内
    CPU_SAFETY_FACTOR = 0.75    # 只使75% CPU 核心
    
    def __init__(self, system_resources: SystemResources):
        
        self.cpu_cores = system_resources.cpu_cores
        self.memory_gb = system_resources.memory_gb
        self.memory_mb = system_resources.memory_gb * 1024
    
    def calculate_optimal_config(self) -> ConcurrencyConfig:
        
        # 计算可用资源
        available_cpu = int(self.cpu_cores * self.CPU_SAFETY_FACTOR)
        available_memory_mb = int(self.memory_mb * self.MEMORY_SAFETY_FACTOR)
        
        # 计算多仓库并发数
        repo_workers = max(1, available_cpu // 2)
        
        # 计算项目并发
        project_workers = max(1, available_cpu // repo_workers)
        
        # 计算每个项目的内存限
        memory_for_projects = available_memory_mb - self.BASE_MEMORY
        memory_per_project_mb = max(256, memory_for_projects // repo_workers)
        
        # 计算总并发文件数
        total_concurrent_files = repo_workers * project_workers
        
        # 计算总内存使
        total_memory_mb = (
            self.BASE_MEMORY +
            repo_workers * self.REPO_CACHE +
            repo_workers * project_workers * self.PROJECT_CACHE +
            total_concurrent_files * self.FILE_CACHE
        )
        
        return ConcurrencyConfig(
            repo_workers=repo_workers,
            project_workers=project_workers,
            memory_per_project_mb=memory_per_project_mb,
            total_concurrent_files=total_concurrent_files,
            total_memory_mb=total_memory_mb
        )
    
    def calculate_config_for_memory(self, memory_limit_mb: int) -> ConcurrencyConfig:
        
        # 计算可用资源
        available_cpu = int(self.cpu_cores * self.CPU_SAFETY_FACTOR)
        available_memory_mb = int(memory_limit_mb * self.MEMORY_SAFETY_FACTOR)
        
        # 计算多仓库并发数
        repo_workers = max(1, available_cpu // 2)
        
        # 计算项目并发
        project_workers = max(1, available_cpu // repo_workers)
        
        # 计算每个项目的内存限
        memory_for_projects = available_memory_mb - self.BASE_MEMORY
        memory_per_project_mb = max(256, memory_for_projects // repo_workers)
        
        # 如果内存不足,减少并发数
        while memory_per_project_mb < 256 and project_workers > 1:
            project_workers -= 1
            memory_per_project_mb = max(256, memory_for_projects // repo_workers)
        
        # 计算总并发文件数
        total_concurrent_files = repo_workers * project_workers
        
        # 计算总内存使
        total_memory_mb = (
            self.BASE_MEMORY +
            repo_workers * self.REPO_CACHE +
            repo_workers * project_workers * self.PROJECT_CACHE +
            total_concurrent_files * self.FILE_CACHE
        )
        
        return ConcurrencyConfig(
            repo_workers=repo_workers,
            project_workers=project_workers,
            memory_per_project_mb=memory_per_project_mb,
            total_concurrent_files=total_concurrent_files,
            total_memory_mb=total_memory_mb
        )
    
    def calculate_config_for_cpu(self, cpu_limit: int) -> ConcurrencyConfig:
        
        # 计算可用资源
        available_cpu = min(self.cpu_cores, cpu_limit)
        available_memory_mb = int(self.memory_mb * self.MEMORY_SAFETY_FACTOR)
        
        # 计算多仓库并发数
        repo_workers = max(1, available_cpu // 2)
        
        # 计算项目并发
        project_workers = max(1, available_cpu // repo_workers)
        
        # 计算每个项目的内存限
        memory_for_projects = available_memory_mb - self.BASE_MEMORY
        memory_per_project_mb = max(256, memory_for_projects // repo_workers)
        
        # 计算总并发文件数
        total_concurrent_files = repo_workers * project_workers
        
        # 计算总内存使
        total_memory_mb = (
            self.BASE_MEMORY +
            repo_workers * self.REPO_CACHE +
            repo_workers * project_workers * self.PROJECT_CACHE +
            total_concurrent_files * self.FILE_CACHE
        )
        
        return ConcurrencyConfig(
            repo_workers=repo_workers,
            project_workers=project_workers,
            memory_per_project_mb=memory_per_project_mb,
            total_concurrent_files=total_concurrent_files,
            total_memory_mb=total_memory_mb
        )
    
    def print_config(self, config: ConcurrencyConfig):
        
        print("=" * 60)
        print("并发配置")
        print("=" * 60)
        print(f"多仓库并发数: {config.repo_workers}")
        print(f"项目并发 {config.project_workers}")
        print(f"每个项目内存限制: {config.memory_per_project_mb}MB")
        print(f"总并发文件数: {config.total_concurrent_files}")
        print(f"总内存使 {config.total_memory_mb}MB")
        print("=" * 60)
    
    def print_system_info(self):
        
        print("=" * 60)
        print("系统信息")
        print("=" * 60)
        print(f"CPU 核心 {self.cpu_cores}")
        print(f"内存大小: {self.memory_gb}GB ({self.memory_mb}MB)")
        print(f"可用 CPU: {int(self.cpu_cores * self.CPU_SAFETY_FACTOR)}")
        print(f"可用内存: {int(self.memory_mb * self.MEMORY_SAFETY_FACTOR)}MB")
        print("=" * 60)

def get_optimal_config() -> ConcurrencyConfig:
    
    resources = SystemResources.detect()
    calculator = CapacityCalculator(resources)
    return calculator.calculate_optimal_config()

def get_config_for_memory(memory_limit_mb: int) -> ConcurrencyConfig:
    
    resources = SystemResources.detect()
    calculator = CapacityCalculator(resources)
    return calculator.calculate_config_for_memory(memory_limit_mb)

def print_capacity_report():
    
    resources = SystemResources.detect()
    calculator = CapacityCalculator(resources)
    
    calculator.print_system_info()
    print()
    
    config = calculator.calculate_optimal_config()
    calculator.print_config(config)
    print()
    
    # 打印不同内存限制下的配置
    print("不同内存限制下的配置:")
    print("-" * 60)
    for memory_limit in [512, 1024, 2048, 4096]:
        config = calculator.calculate_config_for_memory(memory_limit)
        print(f"内存限制 {memory_limit}MB: "
              f"{config.repo_workers} 仓库 × {config.project_workers} 项目 = "
              f"{config.total_concurrent_files} 个并发文件")

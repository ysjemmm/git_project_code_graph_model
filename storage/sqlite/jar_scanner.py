"""
JAR 文件扫描器模块
用于扫描目录中的 JAR 文件并提取类信息
"""
import os
import time
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional

from .class_name_parser import ClassNameParser
from .jar_class_db import JARClassDB, ClassInfo
from .pom_parser import PomParser, PomInfo


@dataclass
class ScanResult:
    """扫描结果统计"""
    total_jars_found: int = 0       # 找到的JAR文件总数
    jars_scanned: int = 0           # 实际扫描的JAR数量
    jars_skipped: int = 0           # 跳过的JAR数量
    total_classes: int = 0          # 提取的类总数
    errors: List[str] = field(default_factory=list)  # 错误消息列表
    start_time: float = 0.0         # 开始时间戳
    end_time: float = 0.0           # 结束时间戳
    
    @property
    def duration(self) -> float:
        """扫描持续时间（秒）"""
        return self.end_time - self.start_time


class JARScanner:
    """JAR文件扫描器"""
    
    def __init__(
        self, 
        db: JARClassDB, 
        parser: Optional[ClassNameParser] = None,
        batch_size: int = 1000
    ):
        """
        初始化扫描器
        
        参数:
            db: JARClassDB实例
            parser: ClassNameParser实例（可选，默认创建新实例）
            batch_size: 批量插入的大小（默认1000）
        """
        self.db = db
        self.parser = parser or ClassNameParser()
        self.batch_size = batch_size
    
    def scan_directory(
        self, 
        directory: str, 
        force_rescan: bool = False,
        include_anonymous: bool = False
    ) -> ScanResult:
        """
        扫描目录中的所有JAR文件
        
        参数:
            directory: 要扫描的目录路径
            force_rescan: 是否强制重新扫描所有JAR（默认False）
            include_anonymous: 是否包含匿名类（默认False，不导入匿名类）
        
        返回:
            ScanResult对象，包含统计信息
        
        异常:
            如果目录不存在，抛出FileNotFoundError
        """
        result = ScanResult()
        result.start_time = time.time()
        
        # 检查目录是否存在
        dir_path = Path(directory)
        if not dir_path.exists():
            raise FileNotFoundError(f"目录不存在: {directory}")
        
        if not dir_path.is_dir():
            raise NotADirectoryError(f"路径不是目录: {directory}")
        
        # 递归查找所有 JAR 文件
        jar_files = self._find_jar_files(directory)
        result.total_jars_found = len(jar_files)
        
        print(f"找到 {result.total_jars_found} 个 JAR 文件")
        
        # 扫描每个 JAR 文件
        for i, jar_path in enumerate(jar_files, 1):
            try:
                # 检查是否需要扫描
                if not force_rescan and not self.should_scan_jar(jar_path):
                    result.jars_skipped += 1
                    if i % 100 == 0:
                        print(f"进度: {i}/{result.total_jars_found} (跳过)")
                    continue
                
                # 扫描 JAR 文件
                class_count = self.scan_jar(jar_path, include_anonymous)
                result.jars_scanned += 1
                result.total_classes += class_count
                
                if i % 10 == 0:
                    print(f"进度: {i}/{result.total_jars_found} (已扫描 {result.jars_scanned} 个，提取 {result.total_classes} 个类)")
                
            except Exception as e:
                error_msg = f"扫描 {jar_path} 失败: {e}"
                result.errors.append(error_msg)
                print(f"[警告] {error_msg}")
        
        result.end_time = time.time()
        
        return result
    
    def scan_jar(self, jar_path: str, include_anonymous: bool = False) -> int:
        """
        扫描单个JAR文件
        
        参数:
            jar_path: JAR文件路径
            include_anonymous: 是否包含匿名类（默认False，不导入匿名类）
        
        返回:
            提取的类数量
        
        异常:
            如果JAR损坏，记录警告并返回0
        """
        classes = []
        jar_name = Path(jar_path).name  # 提取 JAR 文件名
        
        # 获取当前时间字符串
        from datetime import datetime
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        # 解析 POM 文件获取 artifact 信息
        pom_info = self._parse_pom_for_jar(jar_path)
        
        try:
            with zipfile.ZipFile(jar_path, 'r') as jar:
                # 提取所有 .class 文件
                for file_info in jar.namelist():
                    if file_info.endswith('.class'):
                        # 解析类路径
                        fqn, simple_name, package_name, is_anonymous = \
                            self.parser.parse_class_path(file_info)
                        
                        # 根据参数决定是否包含匿名类
                        if not include_anonymous and is_anonymous:
                            continue
                        
                        # 创建 ClassInfo 对象，包含 POM 信息
                        class_info = ClassInfo(
                            fqn=fqn,
                            simple_name=simple_name,
                            package_name=package_name,
                            jar_name=jar_name,
                            jar_path=jar_path,
                            is_anonymous=is_anonymous,
                            insert_time=current_time,
                            file_path=file_info,  # 保存类在 JAR 中的文件路径
                            parent_artifact_id=pom_info.parent_artifact_id if pom_info else None,
                            parent_group_id=pom_info.parent_group_id if pom_info else None,
                            parent_version=pom_info.parent_version if pom_info else None,
                            artifact_id=pom_info.artifact_id if pom_info else None,
                            artifact_group_id=pom_info.group_id if pom_info else None,
                            artifact_version=pom_info.version if pom_info else None
                        )
                        classes.append(class_info)
                        
                        # 批量插入
                        if len(classes) >= self.batch_size:
                            self.db.batch_insert_classes(classes)
                            classes.clear()
            
            # 插入剩余的类
            if classes:
                self.db.batch_insert_classes(classes)
            
            # 更新 JAR 元数据
            total_count = len(classes) + (
                (len(classes) // self.batch_size) * self.batch_size
            )
            
            # 重新计算实际插入的类数量
            actual_count = self._count_classes_in_jar(jar_path)
            self.db.update_jar_metadata(jar_path, actual_count)
            
            return actual_count
            
        except zipfile.BadZipFile:
            print(f"[警告] JAR 文件损坏: {jar_path}")
            return 0
        except Exception as e:
            print(f"[错误] 扫描 JAR 文件失败 {jar_path}: {e}")
            raise
    
    def should_scan_jar(self, jar_path: str) -> bool:
        """
        判断是否需要扫描JAR文件
        
        参数:
            jar_path: JAR文件路径
        
        返回:
            如果需要扫描返回True
        
        逻辑:
            - 如果JAR未在metadata表中，返回True
            - 如果JAR修改时间晚于scan_timestamp，返回True
            - 否则返回False
        """
        # 获取 JAR 元数据
        metadata = self.db.get_jar_metadata(jar_path)
        
        if metadata is None:
            # JAR 未扫描过
            return True
        
        # 获取 JAR 文件的修改时间
        jar_mtime = os.path.getmtime(jar_path)
        
        # 如果 JAR 修改时间晚于扫描时间，需要重新扫描
        if jar_mtime > metadata['scan_timestamp']:
            return True
        
        return False
    
    def _find_jar_files(self, directory: str) -> List[str]:
        """
        递归查找目录中的所有 JAR 文件
        
        参数:
            directory: 目录路径
        
        返回:
            JAR 文件路径列表
        """
        jar_files = []
        
        for root, dirs, files in os.walk(directory):
            for file in files:
                if file.endswith('.jar'):
                    jar_path = os.path.join(root, file)
                    jar_files.append(jar_path)
        
        return sorted(jar_files)
    
    def _count_classes_in_jar(self, jar_path: str) -> int:
        """
        计算 JAR 文件中实际插入的类数量
        
        参数:
            jar_path: JAR 文件路径
        
        返回:
            类数量
        """
        classes = self.db.query_by_jar(jar_path, include_anonymous=True)
        return len(classes)
    
    def _parse_pom_for_jar(self, jar_path: str) -> Optional[PomInfo]:
        """
        为 JAR 文件查找并解析对应的 POM 文件
        
        参数:
            jar_path: JAR 文件路径
        
        返回:
            PomInfo 对象，如果找不到或解析失败返回 None
        """
        pom_path = PomParser.find_pom_for_jar(jar_path)
        if pom_path:
            return PomParser.parse(pom_path)
        return None

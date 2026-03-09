"""
项目类扫描器模块
用于扫描 Java 项目文件并提取类信息到 ProjectClassDB
"""
import os
import time
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from parser.languages.java.core.ast_node_types import (
    JavaFileStructure, ClassInfo, InterfaceInfo, EnumInfo, 
    AnnotationTypeInfo, RecordInfo
)
from .project_class_db import ProjectClassDB, ProjectClassInfo


@dataclass
class ProjectScanResult:
    """项目扫描结果统计"""
    total_files_found: int = 0       # 找到的Java文件总数
    files_scanned: int = 0           # 实际扫描的文件数量
    files_skipped: int = 0           # 跳过的文件数量
    total_classes: int = 0           # 提取的类总数
    errors: List[str] = field(default_factory=list)  # 错误消息列表
    start_time: float = 0.0          # 开始时间戳
    end_time: float = 0.0            # 结束时间戳
    
    @property
    def duration(self) -> float:
        """扫描持续时间（秒）"""
        return self.end_time - self.start_time


class ProjectScanner:
    """项目类扫描器"""
    
    # 类型名称映射
    TYPE_NAME_MAPPING = {
        'ClassInfo': 'class_name',
        'InterfaceInfo': 'interface_name',
        'AnnotationTypeInfo': 'annotation_name',
        'EnumInfo': 'enum_name',
        'RecordInfo': 'record_name',
    }
    
    # 嵌套类字段映射
    NESTED_FIELDS = [
        'nested_classes', 
        'nested_interfaces', 
        'nested_enums', 
        'nested_annotations', 
        'nested_records'
    ]
    
    def __init__(self, db: ProjectClassDB, batch_size: int = 1000):
        """
        初始化扫描器
        
        参数:
            db: ProjectClassDB实例
            batch_size: 批量插入的大小（默认1000）
        """
        self.db = db
        self.batch_size = batch_size
    
    def scan_file(
        self, 
        project_name: str,
        java_file_structure: JavaFileStructure,
        include_anonymous: bool = False
    ) -> int:
        """
        扫描单个 Java 文件（增量更新）
        
        参数:
            project_name: 项目名称
            java_file_structure: JavaFileStructure 对象
            include_anonymous: 是否包含匿名类（默认False）
        
        返回:
            提取的类数量（如果跳过则返回 0）
        """
        file_path = java_file_structure.file_path

        # 检查文件是否存在
        if not os.path.exists(file_path):
            return 0
        
        file_mtime = os.path.getmtime(file_path)
        
        # 检查是否需要更新
        existing_classes = self.db.query_by_file(project_name, file_path)
        if existing_classes:
            # 检查最后修改时间（转换为 float 进行比较）
            last_modified_str = existing_classes[0].last_modified
            if last_modified_str:
                try:
                    # 将字符串时间转换为时间戳
                    from datetime import datetime
                    last_modified_dt = datetime.strptime(last_modified_str, '%Y-%m-%d %H:%M:%S')
                    last_modified_timestamp = last_modified_dt.timestamp()
                    
                    # 如果文件未修改（允许 1 秒的误差），跳过
                    if abs(file_mtime - last_modified_timestamp) < 1:
                        return 0
                except ValueError:
                    # 如果时间格式不正确，继续扫描
                    pass
            
            # 删除旧的类信息
            self.db.delete_by_file(project_name, file_path)
        
        # 提取类信息
        classes = self._extract_classes(
            project_name, 
            java_file_structure, 
            include_anonymous
        )
        
        # 批量插入
        if classes:
            self.db.batch_insert_classes(classes)
        
        return len(classes)
    
    def _extract_classes(
        self,
        project_name: str,
        java_file_structure: JavaFileStructure,
        include_anonymous: bool = False
    ) -> List[ProjectClassInfo]:
        """
        从 JavaFileStructure 提取类信息
        
        参数:
            project_name: 项目名称
            java_file_structure: JavaFileStructure 对象
            include_anonymous: 是否包含匿名类
        
        返回:
            ProjectClassInfo 列表
        """
        classes = []
        
        # 获取包名
        package_name = ""
        if java_file_structure.package_info:
            package_name = java_file_structure.package_info.name or ""
        
        # 获取文件路径和相对路径
        file_path = java_file_structure.file_path
        relative_path = java_file_structure.relative_path
        
        # 获取文件修改时间（转换为字符串格式）
        if os.path.exists(file_path):
            file_mtime = os.path.getmtime(file_path)
            last_modified_str = datetime.fromtimestamp(file_mtime).strftime('%Y-%m-%d %H:%M:%S')
        else:
            last_modified_str = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        # 获取当前时间字符串（用于 insert_time）
        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        # 获取文件的 symbol_id（作为顶层类的 parent_symbol_id）
        file_symbol_id = getattr(java_file_structure, 'symbol_id', '')
        if not file_symbol_id:
            # 如果文件没有 symbol_id，生成一个默认值
            file_symbol_id = f"{project_name}:file:{relative_path}"
        
        # 提取顶层类
        for cls in java_file_structure.classes:
            classes.extend(
                self._extract_class_recursive(
                    project_name, cls, package_name, file_path, 
                    relative_path, None, include_anonymous, 
                    last_modified_str, current_time, file_symbol_id
                )
            )
        
        for cls in java_file_structure.interfaces:
            classes.extend(
                self._extract_class_recursive(
                    project_name, cls, package_name, file_path, 
                    relative_path, None, include_anonymous, 
                    last_modified_str, current_time, file_symbol_id
                )
            )
        
        for cls in java_file_structure.enums:
            classes.extend(
                self._extract_class_recursive(
                    project_name, cls, package_name, file_path, 
                    relative_path, None, include_anonymous, 
                    last_modified_str, current_time, file_symbol_id
                )
            )
        
        for cls in java_file_structure.annotations:
            classes.extend(
                self._extract_class_recursive(
                    project_name, cls, package_name, file_path, 
                    relative_path, None, include_anonymous, 
                    last_modified_str, current_time, file_symbol_id
                )
            )
        
        for cls in java_file_structure.records:
            classes.extend(
                self._extract_class_recursive(
                    project_name, cls, package_name, file_path, 
                    relative_path, None, include_anonymous, 
                    last_modified_str, current_time, file_symbol_id
                )
            )
        
        return classes
    
    def _extract_class_recursive(
        self,
        project_name: str,
        class_obj,
        package_name: str,
        file_path: str,
        relative_path: str,
        parent_fqn: Optional[str],
        include_anonymous: bool,
        last_modified_str: str,
        insert_time: str,
        parent_symbol_id: Optional[str] = None
    ) -> List[ProjectClassInfo]:
        """
        递归提取类信息（包括嵌套类）
        
        参数:
            project_name: 项目名称
            class_obj: 类对象（ClassInfo, InterfaceInfo等）
            package_name: 包名
            file_path: 文件路径
            relative_path: 相对路径
            parent_fqn: 父类完全限定名（嵌套类）
            include_anonymous: 是否包含匿名类
            last_modified_str: 文件最后修改时间字符串
            insert_time: 插入时间字符串
        
        返回:
            ProjectClassInfo 列表
        """
        classes = []
        
        # 获取类名
        class_type = class_obj.__class__.__name__
        name_field = self.TYPE_NAME_MAPPING.get(class_type)
        if not name_field:
            return classes
        
        simple_name = getattr(class_obj, name_field, None)
        if not simple_name:
            return classes
        
        # 检查是否是匿名类
        is_anonymous = simple_name.isdigit() or '$' in simple_name
        if not include_anonymous and is_anonymous:
            return classes
        
        # 构建完全限定名
        if parent_fqn:
            # 嵌套类：使用点号分隔
            fqn = f"{parent_fqn}.{simple_name}"
            is_nested = True
            parent_class = parent_fqn
        else:
            # 顶层类
            if package_name:
                fqn = f"{package_name}.{simple_name}"
            else:
                fqn = simple_name
            is_nested = False
            parent_class = None
        
        # 获取 symbol_id（如果存在）
        symbol_id = getattr(class_obj, 'symbol_id', '')
        if not symbol_id:
            # 如果没有 symbol_id，生成一个默认值
            symbol_id = f"{project_name}:{fqn}"
        
        # 创建 ProjectClassInfo
        class_info = ProjectClassInfo(
            fqn=fqn,
            simple_name=simple_name,
            package_name=package_name,
            project_name=project_name,
            file_path=file_path,
            relative_path=relative_path,
            is_anonymous=is_anonymous,
            is_nested=is_nested,
            parent_class=parent_class,
            symbol_id=symbol_id,
            parent_symbol_id=parent_symbol_id,
            insert_time=insert_time,
            last_modified=last_modified_str
        )
        classes.append(class_info)
        
        # 递归处理嵌套类
        for nested_field in self.NESTED_FIELDS:
            nested_list = getattr(class_obj, nested_field, [])
            for nested_cls in nested_list:
                classes.extend(
                    self._extract_class_recursive(
                        project_name, nested_cls, package_name, file_path,
                        relative_path, fqn, include_anonymous,
                        last_modified_str, insert_time, symbol_id
                    )
                )
        
        return classes

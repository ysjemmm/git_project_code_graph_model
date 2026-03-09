"""
POM 文件解析器
用于从 Maven POM 文件中提取 artifact 和 parent 信息
"""
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Optional


@dataclass
class PomInfo:
    """POM 文件信息"""
    # 当前项目信息
    artifact_id: Optional[str] = None
    group_id: Optional[str] = None
    version: Optional[str] = None
    
    # 父项目信息
    parent_artifact_id: Optional[str] = None
    parent_group_id: Optional[str] = None
    parent_version: Optional[str] = None


class PomParser:
    """POM 文件解析器"""
    
    # Maven 命名空间
    NAMESPACES = {
        'maven': 'http://maven.apache.org/POM/4.0.0'
    }
    
    @staticmethod
    def parse(pom_path: str) -> Optional[PomInfo]:
        """
        解析 POM 文件
        
        参数:
            pom_path: POM 文件路径
        
        返回:
            PomInfo 对象，如果解析失败返回 None
        """
        try:
            tree = ET.parse(pom_path)
            root = tree.getroot()
            
            pom_info = PomInfo()
            
            # 尝试带命名空间和不带命名空间两种方式
            # 有些 POM 文件有命名空间，有些没有
            
            # 解析当前项目信息
            pom_info.artifact_id = PomParser._get_text(root, 'artifactId')
            pom_info.group_id = PomParser._get_text(root, 'groupId')
            pom_info.version = PomParser._get_text(root, 'version')
            
            # 如果当前项目没有 groupId，可能继承自 parent
            if not pom_info.group_id:
                parent_group = PomParser._get_text(root, 'parent/groupId')
                if parent_group:
                    pom_info.group_id = parent_group
            
            # 如果当前项目没有 version，可能继承自 parent
            if not pom_info.version:
                parent_version = PomParser._get_text(root, 'parent/version')
                if parent_version:
                    pom_info.version = parent_version
            
            # 解析父项目信息
            parent = root.find('parent') or root.find('{http://maven.apache.org/POM/4.0.0}parent')
            if parent is not None:
                pom_info.parent_artifact_id = PomParser._get_element_text(parent, 'artifactId')
                pom_info.parent_group_id = PomParser._get_element_text(parent, 'groupId')
                pom_info.parent_version = PomParser._get_element_text(parent, 'version')
            
            return pom_info
            
        except Exception as e:
            # 解析失败时返回 None
            return None
    
    @staticmethod
    def _get_text(root, path: str) -> Optional[str]:
        """
        获取元素文本，支持带命名空间和不带命名空间
        
        参数:
            root: XML 根元素
            path: 元素路径，例如 'artifactId' 或 'parent/groupId'
        
        返回:
            元素文本，如果不存在返回 None
        """
        # 尝试不带命名空间
        elem = root.find(path)
        if elem is not None and elem.text:
            return elem.text.strip()
        
        # 尝试带命名空间
        ns_path = '/'.join([f'{{http://maven.apache.org/POM/4.0.0}}{p}' for p in path.split('/')])
        elem = root.find(ns_path)
        if elem is not None and elem.text:
            return elem.text.strip()
        
        return None
    
    @staticmethod
    def _get_element_text(element, tag: str) -> Optional[str]:
        """
        从元素中获取子元素文本
        
        参数:
            element: XML 元素
            tag: 子元素标签名
        
        返回:
            子元素文本，如果不存在返回 None
        """
        # 尝试不带命名空间
        child = element.find(tag)
        if child is not None and child.text:
            return child.text.strip()
        
        # 尝试带命名空间
        child = element.find(f'{{http://maven.apache.org/POM/4.0.0}}{tag}')
        if child is not None and child.text:
            return child.text.strip()
        
        return None
    
    @staticmethod
    def find_pom_for_jar(jar_path: str) -> Optional[str]:
        """
        查找 JAR 文件对应的 POM 文件
        
        参数:
            jar_path: JAR 文件路径
        
        返回:
            POM 文件路径，如果不存在返回 None
        
        查找规则:
            1. 同目录下的 .pom 文件（文件名相同，扩展名不同）
            2. 同目录下的任意 .pom 文件
        """
        jar_path_obj = Path(jar_path)
        
        if not jar_path_obj.exists():
            return None
        
        # 规则 1: 同名 POM 文件
        # 例如: plugin-api-1.2-gateway-SNAPSHOT.jar -> plugin-api-1.2-gateway-SNAPSHOT.pom
        pom_path = jar_path_obj.with_suffix('.pom')
        if pom_path.exists():
            return str(pom_path)
        
        # 规则 2: 同目录下的任意 POM 文件
        # 处理带时间戳的情况，例如:
        # plugin-api-1.2-gateway-SNAPSHOT.jar
        # plugin-api-1.2-gateway-20250801.081708-1.pom
        parent_dir = jar_path_obj.parent
        if parent_dir.exists():
            pom_files = list(parent_dir.glob('*.pom'))
            if pom_files:
                # 优先选择文件名最相似的
                jar_stem = jar_path_obj.stem
                for pom_file in pom_files:
                    if pom_file.stem.startswith(jar_stem.split('-')[0]):
                        return str(pom_file)
                # 如果没有相似的，返回第一个
                return str(pom_files[0])
        
        return None

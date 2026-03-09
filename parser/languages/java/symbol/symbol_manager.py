from pathlib import Path
from typing import Optional
import threading

from parser.languages.java.core.ast_node_types import JavaFileStructure
from parser.languages.java.symbol.symbol_commons import ClassLocation, ClassLocationType


class SymbolManager:
    """
    符号管理器（每个项目单例）
    
    使用方式：
        # 方式 1: 直接创建（推荐）
        manager = SymbolManager.get_instance("my-project")
        
        # 方式 2: 使用构造函数（也会返回单例）
        manager = SymbolManager(project_name="my-project")
    """
    
    # 类级别的实例缓存：{project_name: SymbolManager实例}
    _instances = {}
    _lock = threading.Lock()

    def __new__(cls, project_name: str = "default", use_global_db: bool = True, auto_sync_db: bool = True):
        """
        单例模式：每个项目只有一个 SymbolManager 实例
        
        参数:
            project_name: 项目名称（作为单例的 key）
            use_global_db: 是否使用全局数据库（默认 True）
            auto_sync_db: 是否自动同步到数据库（默认 True）
        """
        with cls._lock:
            # 如果该项目的实例不存在，创建新实例
            if project_name not in cls._instances:
                instance = super(SymbolManager, cls).__new__(cls)
                instance._initialized = False
                cls._instances[project_name] = instance
            
            return cls._instances[project_name]

    def __init__(self, project_name: str = "default", use_global_db: bool = True, auto_sync_db: bool = True):
        """
        初始化 SymbolManager（只初始化一次）
        
        参数:
            project_name: 项目名称（默认 "default"）
            use_global_db: 是否使用全局数据库（默认 True）
            auto_sync_db: 是否自动同步到数据库（默认 True）
        """
        # 避免重复初始化
        if self._initialized:
            return
        
        self.project_name = project_name
        self.auto_sync_db = auto_sync_db
        self.project_scanner = None
        
        # 如果启用全局数据库，则自动使用单例
        if use_global_db:
            try:
                from storage.sqlite import get_jar_class_db, get_project_class_db, get_jdk_class_db, ProjectScanner
                self.jar_db = get_jar_class_db()           # JAR 类数据库（全局单例）
                self.project_db = get_project_class_db()   # 项目类数据库（单例）
                self.jdk_db = get_jdk_class_db()           # JDK 类数据库（单例，可能为 None）
                
                # 如果启用自动同步，初始化 ProjectScanner
                if auto_sync_db:
                    self.project_db.initialize_schema()
                    self.project_scanner = ProjectScanner(self.project_db)
            except Exception:
                # 如果导入失败或数据库不存在，保持为 None
                self.jar_db = None
                self.project_db = None
                self.jdk_db = None
                self.project_scanner = None
        else:
            self.jar_db = None
            self.project_db = None
            self.jdk_db = None
            self.project_scanner = None
        
        self._initialized = True
    
    @classmethod
    def get_instance(cls, project_name: str = "default", use_global_db: bool = True, auto_sync_db: bool = True):
        """
        获取指定项目的 SymbolManager 实例（推荐使用）
        
        参数:
            project_name: 项目名称
            use_global_db: 是否使用全局数据库（默认 True）
            auto_sync_db: 是否自动同步到数据库（默认 True）
        
        返回:
            SymbolManager 单例实例
        
        示例:
            manager = SymbolManager.get_instance("my-project")
        """
        return cls(project_name=project_name, use_global_db=use_global_db, auto_sync_db=auto_sync_db)
    
    @classmethod
    def clear_instance(cls, project_name: str):
        """
        清除指定项目的 SymbolManager 实例
        
        参数:
            project_name: 项目名称
        
        用途:
            在测试或需要重新初始化时使用
        """
        with cls._lock:
            if project_name in cls._instances:
                del cls._instances[project_name]
    
    @classmethod
    def clear_all_instances(cls):
        """
        清除所有 SymbolManager 实例
        
        用途:
            在测试或需要全局重置时使用
        """
        with cls._lock:
            cls._instances.clear()

    def collect_from_java_file(self, project_name: str, java_file_structure: JavaFileStructure, include_anonymous: bool = False):
        """
        从 Java 文件收集符号信息到数据库
        
        参数:
            project_name: 项目名称
            java_file_structure: JavaFileStructure 对象
            include_anonymous: 是否包含匿名类（默认 False，仅用于数据库同步）
        
        功能:
            如果启用 auto_sync_db，自动同步到数据库（用于持久化）
        """
        # 如果启用自动同步，同步到数据库
        if self.auto_sync_db and self.project_scanner:
            try:
                self.project_scanner.scan_file(
                    project_name=project_name,
                    java_file_structure=java_file_structure,
                    include_anonymous=include_anonymous
                )
            except Exception as e:
                # 数据库同步失败
                print(f"[警告] 数据库同步失败: {e}")

    def parse_java_object_where(
        self, 
        identifier: str,
        java_file_structure: JavaFileStructure,
        project_name: Optional[str] = None
    ) -> ClassLocation:
        """
        查找 identifier 类来自哪里
        
        参数:
            identifier: 类标识符，例如 "User" 或 "com.example.User"
            java_file_structure: Java 文件结构对象（包含文件路径、包名、import 等信息）
            project_name: 项目名称（可选，默认使用 self.project_name）
        
        返回:
            ClassLocation 对象
            - 内部类：type=INTERNAL, fqn=完全限定名, file_path=文件路径
            - 外部类：type=EXTERNAL, fqn=完全限定名, jar_path=JAR路径
            - 未知类：type=UNKNOWN, fqn=None
        
        解析优先级（按照 Java/IDE 规范）:
            1. 检查是否是当前文件的嵌套类
            2. 检查是否是项目内部的同包类
            3. 检查是否是项目内部的 import 类
            4. 检查是否是外部 JAR 的同包类
            5. 检查是否是外部 JAR 的 import 类
            6. 检查是否是 java.lang 包
            7. 未知
        """
        # 使用传入的 project_name 或 self.project_name
        if project_name is None:
            project_name = self.project_name
        
        # 从 JavaFileStructure 获取信息
        current_package = java_file_structure.package_info.name if java_file_structure.package_info else ""
        imports = [imp.import_path for imp in java_file_structure.import_details]
        
        # 处理嵌套类引用（例如 B.C 或 A.B.C）
        if '.' in identifier:
            # 检查是否是完全限定名（包含包名）
            # 启发式判断：如果第一部分是小写开头，可能是包名
            parts = identifier.split('.')
            first_part = parts[0]
            
            # 如果第一部分是小写开头，当作完全限定名处理
            if first_part and first_part[0].islower():
                return self._resolve_fqn_location_db(identifier, project_name)
            
            # 否则，可能是 OuterClass.InnerClass 或 A.B.C 格式
            # 策略：逐层解析，找到最长的有效前缀
            # 例如 A.B.C：先尝试 A，再尝试 A.B，最后尝试 A.B.C
            
            # 先尝试解析第一部分（最外层类）
            outer_class_name = first_part
            outer_location = self.parse_java_object_where(outer_class_name, java_file_structure, project_name)
            
            if outer_location.type != ClassLocationType.UNKNOWN and outer_location.fqn:
                # 外部类解析成功，拼接剩余部分
                nested_path = '.'.join(parts[1:])
                nested_fqn = f"{outer_location.fqn}.{nested_path}"
                
                # 尝试查询完整的嵌套类 FQN
                nested_location = self._resolve_fqn_location_db(nested_fqn, project_name)
                
                # 如果找到了，直接返回
                if nested_location.type != ClassLocationType.UNKNOWN:
                    return nested_location
                
                # 如果没找到，可能是中间层级不存在
                # 尝试逐层验证（可选优化）
                # 这里我们直接返回 UNKNOWN，因为完整路径不存在
                return nested_location
            
            # 如果第一部分解析失败，尝试作为完全限定名
            return self._resolve_fqn_location_db(identifier, project_name)
        
        # 1. 检查是否是项目内部的同包类
        if current_package:
            same_package_fqn = f"{current_package}.{identifier}"
        else:
            same_package_fqn = identifier
        
        # 先查询项目数据库
        if self.project_db:
            internal_cls = self.project_db.query_by_fqn(same_package_fqn, project_name)
            if internal_cls:
                return ClassLocation(
                    type=ClassLocationType.INTERNAL,
                    fqn=internal_cls.fqn,
                    jar_path=None,
                    file_path=internal_cls.file_path,
                    resolution_method="same_package_internal",
                    symbol_id=internal_cls.symbol_id
                )
        
        # 2. 检查是否是项目内部的 import 类
        for imp in imports:
            # 跳过通配符 import
            if imp.endswith('.*'):
                continue
            
            if imp.endswith('.' + identifier):
                # 查询项目数据库
                if self.project_db:
                    internal_cls = self.project_db.query_by_fqn(imp, project_name)
                    if internal_cls:
                        return ClassLocation(
                            type=ClassLocationType.INTERNAL,
                            fqn=internal_cls.fqn,
                            jar_path=None,
                            file_path=internal_cls.file_path,
                            resolution_method="explicit_import_internal",
                            symbol_id=internal_cls.symbol_id
                        )
        
        # 3. 检查是否是外部 JAR 的同包类
        if self.jar_db:
            external_cls = self.jar_db.query_by_fqn(same_package_fqn)
            if external_cls:
                return self._create_external_location(external_cls, "same_package_external")
        
        # 4. 检查是否是外部 JAR 的 import 类
        if self.jar_db:
            # 4.1 显式 import
            for imp in imports:
                if imp.endswith('.*'):
                    continue
                
                if imp.endswith('.' + identifier):
                    external_cls = self.jar_db.query_by_fqn(imp)
                    if external_cls:
                        return self._create_external_location(external_cls, "explicit_import_external")
            
            # 4.2 通配符 import
            for imp in imports:
                if not imp.endswith('.*'):
                    continue
                
                package = imp.rstrip('.*')
                potential_fqn = f"{package}.{identifier}"
                
                # 先检查项目内部
                if self.project_db:
                    internal_cls = self.project_db.query_by_fqn(potential_fqn, project_name)
                    if internal_cls:
                        return ClassLocation(
                            type=ClassLocationType.INTERNAL,
                            fqn=internal_cls.fqn,
                            jar_path=None,
                            file_path=internal_cls.file_path,
                            resolution_method="wildcard_import_internal",
                            symbol_id=internal_cls.symbol_id
                        )
                
                # 再检查外部 JAR
                external_cls = self.jar_db.query_by_fqn(potential_fqn)
                if external_cls:
                    return self._create_external_location(external_cls, "wildcard_import_external")
        
        # 5. 检查是否是 JDK 类（新增）
        if self.jdk_db:
            # 5.1 检查同包 JDK 类
            jdk_cls = self.jdk_db.query_by_fqn(same_package_fqn)
            if jdk_cls:
                return ClassLocation(
                    type=ClassLocationType.JDK,
                    fqn=jdk_cls.fqn,
                    jar_path=jdk_cls.jar_path,
                    file_path=None,
                    resolution_method="same_package_jdk",
                    symbol_id=None
                )
            
            # 5.2 检查显式 import 的 JDK 类
            for imp in imports:
                if imp.endswith('.*'):
                    continue
                
                if imp.endswith('.' + identifier):
                    jdk_cls = self.jdk_db.query_by_fqn(imp)
                    if jdk_cls:
                        return ClassLocation(
                            type=ClassLocationType.JDK,
                            fqn=jdk_cls.fqn,
                            jar_path=jdk_cls.jar_path,
                            file_path=None,
                            resolution_method="explicit_import_jdk",
                            symbol_id=None
                        )
            
            # 5.3 检查通配符 import 的 JDK 类
            for imp in imports:
                if not imp.endswith('.*'):
                    continue
                
                package = imp.rstrip('.*')
                potential_fqn = f"{package}.{identifier}"
                
                jdk_cls = self.jdk_db.query_by_fqn(potential_fqn)
                if jdk_cls:
                    return ClassLocation(
                        type=ClassLocationType.JDK,
                        fqn=jdk_cls.fqn,
                        jar_path=jdk_cls.jar_path,
                        file_path=None,
                        resolution_method="wildcard_import_jdk",
                        symbol_id=None
                    )
            
            # 5.4 检查 java.lang 包（JDK 类的特殊情况）
            java_lang_fqn = f"java.lang.{identifier}"
            jdk_cls = self.jdk_db.query_by_fqn(java_lang_fqn)
            if jdk_cls:
                return ClassLocation(
                    type=ClassLocationType.JDK,
                    fqn=jdk_cls.fqn,
                    jar_path=jdk_cls.jar_path,
                    file_path=None,
                    resolution_method="java_lang",
                    symbol_id=None
                )
        
        # 6. 未知
        return ClassLocation(
            type=ClassLocationType.UNKNOWN,
            fqn=identifier,  # 保留原始标识符作为 fqn
            jar_path=None,
            file_path=None,
            resolution_method="unresolved",
            symbol_id=None
        )
    
    def _create_external_location(self, external_cls, resolution_method: str) -> ClassLocation:
        """
        创建外部类的 ClassLocation，包含 POM 信息
        
        参数:
            external_cls: 外部类信息对象（来自 jar_db）
            resolution_method: 解析方法
        
        返回:
            ClassLocation 对象
        """
        return ClassLocation(
            type=ClassLocationType.EXTERNAL,
            fqn=external_cls.fqn,
            jar_path=external_cls.jar_path,
            file_path=external_cls.file_path,
            resolution_method=resolution_method,
            symbol_id=None,
            artifact_id=external_cls.artifact_id,
            artifact_group_id=external_cls.artifact_group_id,
            artifact_version=external_cls.artifact_version,
            parent_artifact_id=external_cls.parent_artifact_id,
            parent_group_id=external_cls.parent_group_id,
            parent_version=external_cls.parent_version
        )
    
    def _resolve_fqn_location_db(
        self, 
        fqn: str, 
        project_name: str
    ) -> ClassLocation:
        """解析完全限定名的位置（使用数据库）"""
        # 先检查项目内部
        if self.project_db:
            internal_cls = self.project_db.query_by_fqn(fqn, project_name)
            if internal_cls:
                return ClassLocation(
                    type=ClassLocationType.INTERNAL,
                    fqn=internal_cls.fqn,
                    jar_path=None,
                    file_path=internal_cls.file_path,
                    resolution_method="fqn_internal",
                    symbol_id=internal_cls.symbol_id
                )
        
        # 再检查外部 JAR
        if self.jar_db:
            external_cls = self.jar_db.query_by_fqn(fqn)
            if external_cls:
                return self._create_external_location(external_cls, "fqn_external")
        
        # 检查 JDK 类（新增）
        if self.jdk_db:
            jdk_cls = self.jdk_db.query_by_fqn(fqn)
            if jdk_cls:
                return ClassLocation(
                    type=ClassLocationType.JDK,
                    fqn=jdk_cls.fqn,
                    jar_path=jdk_cls.jar_path,
                    file_path=None,
                    resolution_method="fqn_jdk",
                    symbol_id=None
                )
        
        # 未知
        return ClassLocation(
            type=ClassLocationType.UNKNOWN,
            fqn=fqn,
            jar_path=None,
            file_path=None,
            resolution_method="unresolved",
            symbol_id=None
        )
    
    def _extract_package_from_file_path(self, file_path: str) -> str:
        """
        从文件路径提取包名
        
        例如: src/main/java/com/example/User.java -> com.example
        """
        # 移除文件扩展名
        if file_path.endswith('.java'):
            file_path = file_path[:-5]
        
        # 转换为 Path 对象
        path = Path(file_path).resolve()
        parts = path.parts
        
        # 查找 java 目录
        java_index = -1
        for i, part in enumerate(parts):
            if part == 'java':
                java_index = i
                break
        
        if java_index != -1 and java_index < len(parts) - 1:
            # 从 java 目录后面开始，但排除最后的文件名
            package_parts = parts[java_index + 1:-1]
            return '.'.join(package_parts)
        
        # 如果没有找到 java 目录，尝试从 src 开始
        src_index = -1
        for i, part in enumerate(parts):
            if part == 'src':
                src_index = i
                break
        
        if src_index != -1 and src_index < len(parts) - 1:
            package_parts = parts[src_index + 1:-1]
            # 跳过 main/test
            if package_parts and package_parts[0] in ['main', 'test']:
                package_parts = package_parts[1:]
            # 跳过 java
            if package_parts and package_parts[0] == 'java':
                package_parts = package_parts[1:]
            return '.'.join(package_parts)
        
        return ""
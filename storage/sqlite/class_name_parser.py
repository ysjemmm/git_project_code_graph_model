"""
类名解析模块
用于解析 Java .class 文件路径并提取类信息
"""
import re
from typing import Tuple


class ClassNameParser:
    """解析类文件路径和类名"""
    
    # 匿名类模式：以 $数字 结尾
    ANONYMOUS_CLASS_PATTERN = re.compile(r'.*\$\d+$')
    
    @staticmethod
    def parse_class_path(class_file_path: str) -> Tuple[str, str, str, bool]:
        """
        从.class文件路径解析类信息
        
        参数:
            class_file_path: .class文件路径，例如 "com/example/User.class"
        
        返回:
            (fqn, simple_name, package_name, is_anonymous)
        
        示例:
            "com/example/User.class" -> ("com.example.User", "User", "com.example", False)
            "com/example/User$1.class" -> ("com.example.User$1", "User$1", "com.example", True)
            "com/example/Outer$Inner.class" -> ("com.example.Outer.Inner", "Inner", "com.example.Outer", False)
            "com/example/Outer$1ConnectListener.class" -> ("com.example.Outer.ConnectListener", "ConnectListener", "com.example.Outer", False)
            "com/example/$Gson$Types.class" -> ("com.example.Gson.Types", "Types", "com.example.Gson", False)
            "classes/java/lang/String.class" -> ("java.lang.String", "String", "java.lang", False)  # JMOD 文件
        """
        # 移除 JMOD 文件的 classes/ 前缀（JDK 9+）
        if class_file_path.startswith('classes/') or class_file_path.startswith('classes\\'):
            class_file_path = class_file_path[8:]  # 移除 "classes/"
        
        # 移除 .class 后缀
        if class_file_path.endswith('.class'):
            class_file_path = class_file_path[:-6]
        
        # 将路径分隔符替换为点号
        path_with_dots = class_file_path.replace('/', '.').replace('\\', '.')
        
        # 处理以 $ 开头的类名（如 com.example.$Gson$Types）
        # 移除路径中的前导 $
        parts = path_with_dots.split('.')
        cleaned_parts = []
        for part in parts:
            if part.startswith('$'):
                # 移除前导 $
                part = part[1:]
            cleaned_parts.append(part)
        path_with_dots = '.'.join(cleaned_parts)
        
        # 判断是否为匿名类（在转换之前检查）
        is_anonymous = ClassNameParser.is_anonymous_class(path_with_dots)
        
        # 处理 $ 符号：
        # 1. 如果是匿名类（以 $数字 结尾），保留最后的 $数字，其他 $ 替换为 .
        # 2. 如果不是匿名类，处理 $数字+类名 的情况（如 $1ConnectListener -> ConnectListener）
        # 3. 其他情况，将所有 $ 替换为 .
        if is_anonymous:
            # 找到最后一个 $数字 的位置
            import re
            match = re.search(r'\$\d+$', path_with_dots)
            if match:
                # 分离出匿名类标记
                anon_marker_start = match.start()
                before_anon = path_with_dots[:anon_marker_start]
                anon_marker = path_with_dots[anon_marker_start:]
                
                # 将匿名类标记之前的 $ 替换为 .
                fqn = before_anon.replace('$', '.') + anon_marker
            else:
                # 不应该发生，但作为后备
                fqn = path_with_dots
        else:
            # 非匿名类：将所有 $ 替换为 .
            fqn = path_with_dots.replace('$', '.')
        
        # 分解 FQN 为包名和简单名称
        package_name, simple_name = ClassNameParser.split_fqn(fqn)
        
        return fqn, simple_name, package_name, is_anonymous
    
    @staticmethod
    def is_anonymous_class(class_name: str) -> bool:
        r"""
        判断类名是否为匿名类
        
        参数:
            class_name: 类名（简单名或FQN）
        
        返回:
            如果类名匹配 .*\$\d+$ 模式则返回True
        
        示例:
            "User$1" -> True
            "User$12" -> True
            "User$Inner" -> False
            "User" -> False
        """
        return bool(ClassNameParser.ANONYMOUS_CLASS_PATTERN.match(class_name))
    
    @staticmethod
    def split_fqn(fqn: str) -> Tuple[str, str]:
        """
        将FQN分解为包名和简单名称
        
        参数:
            fqn: 完全限定名
        
        返回:
            (package_name, simple_name)
        
        规则:
            - simple_name 只包含最后一个类名（不包含外部类名）
            - package_name 包含真正的包名 + 外部类名
        
        示例:
            "com.example.User" -> ("com.example", "User")
            "com.example.Outer.Inner" -> ("com.example.Outer", "Inner")
            "com.example.Outer.Inner.Deep" -> ("com.example.Outer.Inner", "Deep")
            "com.example.User$1" -> ("com.example", "User$1")
            "User" -> ("", "User")
            "com.example.package-info" -> ("com.example", "package-info")
        """
        if '.' not in fqn:
            # 没有包名，只有类名
            return "", fqn
        
        # 特殊处理 package-info 类
        if fqn.endswith('.package-info'):
            last_dot = fqn.rfind('.package-info')
            package_name = fqn[:last_dot]
            return package_name, "package-info"
        
        # 找到最后一个点的位置
        last_dot_index = fqn.rfind('.')
        
        # simple_name 是最后一个点之后的部分
        simple_name = fqn[last_dot_index + 1:]
        
        # package_name 是最后一个点之前的部分
        package_name = fqn[:last_dot_index]
        
        return package_name, simple_name

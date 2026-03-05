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
            "com/example/Outer$Inner.class" -> ("com.example.Outer.Inner", "Outer.Inner", "com.example", False)
            "com/example/Outer$Inner$1.class" -> ("com.example.Outer.Inner$1", "Outer.Inner$1", "com.example", True)
        """
        # 移除 .class 后缀
        if class_file_path.endswith('.class'):
            class_file_path = class_file_path[:-6]
        
        # 将路径分隔符替换为点号
        path_with_dots = class_file_path.replace('/', '.').replace('\\', '.')
        
        # 判断是否为匿名类（在转换之前检查）
        is_anonymous = ClassNameParser.is_anonymous_class(path_with_dots)
        
        # 处理 $ 符号：
        # 1. 如果是匿名类（以 $数字 结尾），保留最后的 $数字，其他 $ 替换为 .
        # 2. 如果不是匿名类，将所有 $ 替换为 .
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
        
        示例:
            "com.example.User" -> ("com.example", "User")
            "com.example.Outer.Inner" -> ("com.example", "Outer.Inner")
            "com.example.User$1" -> ("com.example", "User$1")
            "User" -> ("", "User")
        """
        if '.' not in fqn:
            # 没有包名，只有类名
            return "", fqn
        
        # 检查是否包含 $ (匿名类或内部类标记)
        if '$' in fqn:
            # 对于包含 $ 的类，找到最后一个包名部分
            # 例如: com.example.User$1 -> package=com.example, simple=User$1
            # 例如: com.example.Outer$Inner$1 -> package=com.example, simple=Outer$Inner$1
            parts = fqn.split('.')
            
            # 从后往前找第一个不包含 $ 的部分
            for i in range(len(parts) - 1, -1, -1):
                if '$' not in parts[i]:
                    # 找到了包名的结束位置
                    package_name = '.'.join(parts[:i + 1])
                    simple_name = '.'.join(parts[i + 1:])
                    return package_name, simple_name
            
            # 所有部分都包含 $，说明没有包名
            return "", fqn
        
        # 对于不包含 $ 的类，需要区分包名和类名
        # 规则：包名是小写开头的部分，类名是大写开头的部分
        parts = fqn.split('.')
        
        # 从后往前找第一个小写开头的部分
        for i in range(len(parts) - 1, -1, -1):
            part = parts[i]
            if part and part[0].islower():
                # 找到了包名的结束位置
                package_name = '.'.join(parts[:i + 1])
                simple_name = '.'.join(parts[i + 1:])
                return package_name, simple_name
        
        # 没有找到包名，整个都是类名
        return "", fqn

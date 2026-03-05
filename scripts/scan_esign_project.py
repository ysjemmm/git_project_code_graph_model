"""
扫描 esign-egress-gateway 项目并填充 project_classes 数据库
"""
import sys
import os
from pathlib import Path

# 添加项目根目录到 Python 路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from storage.sqlite import get_project_class_db, ProjectScanner
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from loraxmod import Parser


def find_java_files(directory: str):
    """递归查找所有 Java 文件"""
    java_files = []
    for root, dirs, files in os.walk(directory):
        # 跳过 target 目录
        if 'target' in dirs:
            dirs.remove('target')
        
        for file in files:
            if file.endswith('.java'):
                file_path = os.path.join(root, file)
                java_files.append(file_path)
    
    return java_files


def scan_project(project_path: str, project_name: str):
    """扫描项目并填充数据库"""
    
    # 初始化数据库和扫描器
    db = get_project_class_db()
    db.initialize_schema()
    scanner = ProjectScanner(db)
    
    # 初始化解析器和上下文
    parser = Parser("java")
    context = AnalyzerContext(
        project_name=project_name,
        project_path=project_path,
        root_project_symbol_id=AnalyzerHelper.generate_symbol_id_for_project(project_name),
        parser=parser
    )
    
    # 查找所有 Java 文件
    print(f"正在扫描项目: {project_path}")
    java_files = find_java_files(project_path)
    print(f"找到 {len(java_files)} 个 Java 文件\n")
    
    # 统计信息
    total_files = len(java_files)
    scanned_files = 0
    total_classes = 0
    errors = []
    
    # 扫描每个文件
    for i, file_path in enumerate(java_files, 1):
        try:
            # 计算相对路径
            relative_path = os.path.relpath(file_path, project_path)
            
            # 解析 Java 文件
            analyzer = JavaFileAnalyzer(
                context=context,
                file_path=file_path,
                lazy_parse=False
            )
            
            java_file_structure = analyzer.analyze_file()
            
            # 确保文件路径和相对路径正确设置
            if not java_file_structure.file_path:
                java_file_structure.file_path = file_path
            if not java_file_structure.relative_path:
                java_file_structure.relative_path = relative_path
            
            # 扫描文件
            count = scanner.scan_file(
                project_name=project_name,
                java_file_structure=java_file_structure,
                include_anonymous=False
            )
            
            scanned_files += 1
            total_classes += count
            
            if i % 10 == 0 or i == total_files:
                print(f"进度: {i}/{total_files} - 已扫描 {scanned_files} 个文件，提取 {total_classes} 个类")
            
        except Exception as e:
            error_msg = f"扫描 {file_path} 失败: {e}"
            errors.append(error_msg)
            # print(f"[警告] {error_msg}")
    
    # 打印统计信息
    print("\n" + "=" * 60)
    print("扫描完成!")
    print("=" * 60)
    print(f"总文件数: {total_files}")
    print(f"成功扫描: {scanned_files}")
    print(f"提取类数: {total_classes}")
    print(f"错误数量: {len(errors)}")
    
    if errors:
        print("\n错误列表:")
        for error in errors[:10]:  # 只显示前 10 个错误
            print(f"  - {error}")
        if len(errors) > 10:
            print(f"  ... 还有 {len(errors) - 10} 个错误")
    
    # 查询结果
    print("\n" + "=" * 60)
    print("数据库统计")
    print("=" * 60)
    
    classes = db.query_by_project(project_name, include_anonymous=False)
    print(f"项目 '{project_name}' 共有 {len(classes)} 个类")
    
    # 显示前 10 个类
    print("\n前 10 个类:")
    for cls in classes[:10]:
        nested_info = f" (嵌套在 {cls.parent_class})" if cls.is_nested else ""
        print(f"  - {cls.fqn}{nested_info}")
    
    if len(classes) > 10:
        print(f"  ... 还有 {len(classes) - 10} 个类")


def main():
    """主函数"""
    project_path = ".cache/git_repos/esign-egress-gateway"
    project_name = "esign-egress-gateway"
    
    if not os.path.exists(project_path):
        print(f"错误: 项目路径不存在: {project_path}")
        return
    
    scan_project(project_path, project_name)


if __name__ == "__main__":
    main()

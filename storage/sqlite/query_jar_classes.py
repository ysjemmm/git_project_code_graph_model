#!/usr/bin/env python3
"""
JAR 类查询脚本
用于查询 SQLite 数据库中的 Java 类信息
"""
import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from storage.sqlite import JARClassDB


def print_class_info(cls, index=None):
    """打印类信息"""
    prefix = f"{index}. " if index else ""
    print(f"{prefix}FQN: {cls.fqn}")
    print(f"   简单名称: {cls.simple_name}")
    print(f"   包名: {cls.package_name or '(默认包)'}")
    print(f"   JAR: {Path(cls.jar_path).name}")
    print(f"   匿名类: {'是' if cls.is_anonymous else '否'}")


def query_by_fqn(db: JARClassDB):
    """按完全限定名查询"""
    print("\n" + "=" * 70)
    print("按完全限定名查询")
    print("=" * 70)
    
    fqn = input("请输入完全限定名 (例如: java.util.ArrayList): ").strip()
    
    if not fqn:
        print("未输入任何内容")
        return
    
    result = db.query_by_fqn(fqn)
    
    if result:
        print("\n查询结果:")
        print_class_info(result)
    else:
        print(f"\n未找到类: {fqn}")


def query_by_simple_name(db: JARClassDB):
    """按简单名称查询"""
    print("\n" + "=" * 70)
    print("按简单名称查询")
    print("=" * 70)
    
    simple_name = input("请输入简单名称 (例如: ArrayList): ").strip()
    
    if not simple_name:
        print("未输入任何内容")
        return
    
    include_anon = input("是否包含匿名类? (y/n, 默认 n): ").strip().lower()
    include_anonymous = include_anon == 'y'
    
    results = db.query_by_simple_name(simple_name, include_anonymous)
    
    if results:
        print(f"\n找到 {len(results)} 个结果:")
        for i, cls in enumerate(results[:20], 1):  # 最多显示20个
            print(f"\n{i}. {cls.fqn}")
            print(f"   包名: {cls.package_name or '(默认包)'}")
            print(f"   JAR: {Path(cls.jar_path).name}")
            if cls.is_anonymous:
                print(f"   [匿名类]")
        
        if len(results) > 20:
            print(f"\n... 还有 {len(results) - 20} 个结果未显示")
    else:
        print(f"\n未找到类: {simple_name}")


def query_by_package(db: JARClassDB):
    """按包名查询"""
    print("\n" + "=" * 70)
    print("按包名查询")
    print("=" * 70)
    
    package_name = input("请输入包名 (例如: java.util): ").strip()
    
    if not package_name:
        print("未输入任何内容")
        return
    
    include_anon = input("是否包含匿名类? (y/n, 默认 n): ").strip().lower()
    include_anonymous = include_anon == 'y'
    
    results = db.query_by_package(package_name, include_anonymous)
    
    if results:
        print(f"\n找到 {len(results)} 个类:")
        for i, cls in enumerate(results[:20], 1):  # 最多显示20个
            print(f"\n{i}. {cls.simple_name}")
            print(f"   FQN: {cls.fqn}")
            print(f"   JAR: {Path(cls.jar_path).name}")
            if cls.is_anonymous:
                print(f"   [匿名类]")
        
        if len(results) > 20:
            print(f"\n... 还有 {len(results) - 20} 个结果未显示")
    else:
        print(f"\n包 '{package_name}' 中未找到任何类")


def query_by_jar(db: JARClassDB):
    """按JAR路径查询"""
    print("\n" + "=" * 70)
    print("按JAR路径查询")
    print("=" * 70)
    
    jar_path = input("请输入JAR文件路径: ").strip()
    
    if not jar_path:
        print("未输入任何内容")
        return
    
    include_anon = input("是否包含匿名类? (y/n, 默认 n): ").strip().lower()
    include_anonymous = include_anon == 'y'
    
    results = db.query_by_jar(jar_path, include_anonymous)
    
    if results:
        print(f"\n找到 {len(results)} 个类:")
        for i, cls in enumerate(results[:20], 1):  # 最多显示20个
            print(f"\n{i}. {cls.fqn}")
            print(f"   包名: {cls.package_name or '(默认包)'}")
            if cls.is_anonymous:
                print(f"   [匿名类]")
        
        if len(results) > 20:
            print(f"\n... 还有 {len(results) - 20} 个结果未显示")
    else:
        print(f"\n在 JAR 文件中未找到任何类")


def main():
    """主函数"""
    db_path = ".cache/jar_classes.db"
    
    # 检查数据库是否存在
    if not Path(db_path).exists():
        print(f"[错误] 数据库文件不存在: {db_path}")
        print("请先运行 scan_maven_jars.py 扫描 JAR 文件")
        sys.exit(1)
    
    # 连接数据库
    db = JARClassDB(db_path)
    
    try:
        while True:
            print("\n" + "=" * 70)
            print("JAR 类查询工具")
            print("=" * 70)
            print("1. 按完全限定名查询")
            print("2. 按简单名称查询")
            print("3. 按包名查询")
            print("4. 按JAR路径查询")
            print("0. 退出")
            print("=" * 70)
            
            choice = input("\n请选择操作 (0-4): ").strip()
            
            if choice == '0':
                print("\n再见!")
                break
            elif choice == '1':
                query_by_fqn(db)
            elif choice == '2':
                query_by_simple_name(db)
            elif choice == '3':
                query_by_package(db)
            elif choice == '4':
                query_by_jar(db)
            else:
                print("\n无效的选择，请重试")
            
            input("\n按回车键继续...")
    
    finally:
        db.close()


if __name__ == "__main__":
    main()

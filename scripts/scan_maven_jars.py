#!/usr/bin/env python3
"""
Maven JAR 扫描脚本
用于扫描 .cache/maven 目录中的所有 JAR 文件并导入到 SQLite 数据库
"""
import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from storage.sqlite import JARClassDB, JARScanner
from tools.constants import PROJECT_ROOT_PATH


def main():
    """主函数"""
    # 配置参数
    maven_cache_dir = PROJECT_ROOT_PATH / ".cache" / "maven"
    force_rescan = False  # 是否强制重新扫描
    include_anonymous = False  # 是否包含匿名类（默认不包含）
    
    print("=" * 70)
    print("Maven JAR 类扫描器")
    print("=" * 70)
    print(f"扫描目录: {maven_cache_dir}")
    print(f"数据库路径: 使用全局单例（PROJECT_ROOT_PATH/.cache/jar_classes.db）")
    print(f"强制重新扫描: {force_rescan}")
    print(f"包含匿名类: {include_anonymous}")
    print("=" * 70)
    print()
    
    # 初始化数据库（使用全局单例）
    print("初始化数据库...")
    db = JARClassDB()  # 自动使用 PROJECT_ROOT_PATH
    db.initialize_schema()
    print("[OK] 数据库初始化完成")
    print()
    
    # 创建扫描器
    scanner = JARScanner(db, batch_size=1000)
    
    # 开始扫描
    print("开始扫描 JAR 文件...")
    print()
    
    try:
        result = scanner.scan_directory(
            str(maven_cache_dir),
            force_rescan=force_rescan,
            include_anonymous=include_anonymous
        )
        
        # 打印结果
        print()
        print("=" * 70)
        print("[OK] 扫描完成")
        print("=" * 70)
        print(f"找到的 JAR 文件: {result.total_jars_found}")
        print(f"已扫描: {result.jars_scanned}")
        print(f"已跳过: {result.jars_skipped}")
        print(f"提取的类总数: {result.total_classes}")
        print(f"耗时: {result.duration:.2f} 秒")
        
        if result.errors:
            print(f"\n错误数量: {len(result.errors)}")
            print("错误列表:")
            for error in result.errors[:10]:  # 只显示前10个错误
                print(f"  - {error}")
            if len(result.errors) > 10:
                print(f"  ... 还有 {len(result.errors) - 10} 个错误")
        
        print("=" * 70)
        
        # 示例查询
        print("\n示例查询:")
        print("-" * 70)
        
        # 查询一个常见的类
        test_classes = ["String", "ArrayList", "HashMap"]
        for class_name in test_classes:
            results = db.query_by_simple_name(class_name, include_anonymous=False)
            if results:
                print(f"\n类名 '{class_name}' 的查询结果:")
                for i, cls in enumerate(results[:3], 1):  # 只显示前3个
                    print(f"  {i}. {cls.fqn}")
                    print(f"     JAR: {Path(cls.jar_path).name}")
                if len(results) > 3:
                    print(f"  ... 还有 {len(results) - 3} 个结果")
        
        print()
        
    except FileNotFoundError as e:
        print(f"[错误] {e}")
        sys.exit(1)
    except Exception as e:
        print(f"[错误] 扫描失败: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
    finally:
        db.close()


if __name__ == "__main__":
    main()

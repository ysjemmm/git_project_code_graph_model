"""
测试 JDK 索引功能

验证：
1. JDK 索引是否存在
2. 常用 JDK 类是否可以查询
3. SymbolManager 是否正确解析 JDK 类
"""
import sys
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from storage.sqlite import has_jdk_index, get_jdk_class_db
from storage.sqlite.jdk_class_db import JDKClassDB


def test_jdk_index_exists():
    """测试 1: 检查 JDK 索引是否存在"""
    print("=" * 80)
    print("测试 1: 检查 JDK 索引是否存在")
    print("=" * 80)
    
    if has_jdk_index():
        print("✓ JDK 索引已存在")
        
        # 显示可用版本
        versions = JDKClassDB.get_available_versions()
        if versions:
            print(f"  可用版本: {versions}")
        
        return True
    else:
        print("✗ JDK 索引不存在")
        print("\n请先运行构建脚本:")
        print("  python scripts/build_jdk_index.py --auto-detect")
        return False


def test_query_common_classes():
    """测试 2: 查询常用 JDK 类"""
    print("\n" + "=" * 80)
    print("测试 2: 查询常用 JDK 类")
    print("=" * 80)
    
    jdk_db = get_jdk_class_db()
    if not jdk_db:
        print("✗ 无法获取 JDK 数据库")
        return False
    
    # 常用类列表
    common_classes = [
        # java.lang
        "java.lang.Object",
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Double",
        "java.lang.Boolean",
        "java.lang.Exception",
        "java.lang.RuntimeException",
        "java.lang.Thread",
        "java.lang.System",
        
        # java.util
        "java.util.List",
        "java.util.ArrayList",
        "java.util.Map",
        "java.util.HashMap",
        "java.util.Set",
        "java.util.HashSet",
        "java.util.Date",
        
        # java.io
        "java.io.File",
        "java.io.InputStream",
        "java.io.OutputStream",
        "java.io.Serializable",
        
        # java.util.concurrent
        "java.util.concurrent.Executor",
        "java.util.concurrent.ExecutorService",
    ]
    
    found = 0
    not_found = []
    
    for fqn in common_classes:
        cls = jdk_db.query_by_fqn(fqn)
        if cls:
            found += 1
            print(f"  ✓ {fqn}")
        else:
            not_found.append(fqn)
            print(f"  ✗ {fqn}")
    
    print(f"\n结果: {found}/{len(common_classes)} 个类找到")
    
    if not_found:
        print(f"\n未找到的类:")
        for fqn in not_found:
            print(f"  - {fqn}")
    
    return found > 0


def test_symbol_manager_integration():
    """测试 3: SymbolManager 集成"""
    print("\n" + "=" * 80)
    print("测试 3: SymbolManager 集成")
    print("=" * 80)
    
    try:
        from parser.languages.java.symbol.symbol_manager import SymbolManager
        from parser.languages.java.symbol.symbol_commons import ClassLocationType
        from parser.languages.java.core.ast_node_types import JavaFileStructure, PackageInfo, ImportInfo
        
        # 创建测试用的 JavaFileStructure
        structure = JavaFileStructure()
        structure.package_info = PackageInfo()
        structure.package_info.name = "com.example"
        
        # 添加一些 import
        import1 = ImportInfo()
        import1.import_path = "java.util.ArrayList"
        
        import2 = ImportInfo()
        import2.import_path = "java.util.*"
        
        structure.import_details = [import1, import2]
        
        # 创建 SymbolManager
        manager = SymbolManager.get_instance("test-project")
        
        # 测试解析
        test_cases = [
            ("String", "java.lang.String", "java.lang 包"),
            ("ArrayList", "java.util.ArrayList", "显式 import"),
            ("HashMap", "java.util.HashMap", "通配符 import"),
        ]
        
        success = 0
        for identifier, expected_fqn, description in test_cases:
            location = manager.parse_java_object_where(identifier, structure, "test-project")
            
            if location.type == ClassLocationType.JDK and location.fqn == expected_fqn:
                print(f"  ✓ {identifier} -> {location.fqn} ({description})")
                success += 1
            else:
                print(f"  ✗ {identifier} -> {location.type.value}: {location.fqn} (期望: {expected_fqn})")
        
        print(f"\n结果: {success}/{len(test_cases)} 个测试通过")
        
        return success == len(test_cases)
    
    except Exception as e:
        print(f"✗ 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """主函数"""
    print("\n")
    print("╔" + "=" * 78 + "╗")
    print("║" + " " * 28 + "JDK 索引测试" + " " * 38 + "║")
    print("╚" + "=" * 78 + "╝")
    print()
    
    # 测试 1
    if not test_jdk_index_exists():
        return
    
    # 测试 2
    test_query_common_classes()
    
    # 测试 3
    test_symbol_manager_integration()
    
    print("\n✓ 测试完成！\n")


if __name__ == "__main__":
    main()

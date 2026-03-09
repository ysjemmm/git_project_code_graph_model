#!/usr/bin/env python3
"""
测试 file_path 字段是否正确添加到数据库中
"""
import tempfile
import zipfile
from pathlib import Path

from storage.sqlite.jar_class_db import JARClassDB, ClassInfo
from storage.sqlite.jar_scanner import JARScanner


def test_file_path_in_classinfo():
    """测试 ClassInfo 是否包含 file_path 字段"""
    print("测试 1: ClassInfo 包含 file_path 字段")
    
    class_info = ClassInfo(
        fqn="com.example.Test",
        simple_name="Test",
        package_name="com.example",
        jar_name="test.jar",
        jar_path="/path/to/test.jar",
        is_anonymous=False,
        insert_time="2024-01-01 00:00:00",
        file_path="com/example/Test.class"
    )
    
    assert hasattr(class_info, 'file_path'), "ClassInfo 应该有 file_path 属性"
    assert class_info.file_path == "com/example/Test.class", "file_path 值不正确"
    
    print("✓ ClassInfo 包含 file_path 字段")


def test_database_schema():
    """测试数据库表结构是否包含 file_path 列"""
    print("\n测试 2: 数据库表结构包含 file_path 列")
    
    with tempfile.TemporaryDirectory() as tmpdir:
        db_path = Path(tmpdir) / "test.db"
        db = JARClassDB(str(db_path))
        db.initialize_schema()
        
        # 检查表结构
        cursor = db.conn.cursor()
        cursor.execute("PRAGMA table_info(jar_classes)")
        columns = [row[1] for row in cursor.fetchall()]
        
        assert 'file_path' in columns, "jar_classes 表应该包含 file_path 列"
        
        db.close()
    
    print("✓ 数据库表结构包含 file_path 列")


def test_insert_and_query_with_file_path():
    """测试插入和查询包含 file_path 的数据"""
    print("\n测试 3: 插入和查询包含 file_path 的数据")
    
    with tempfile.TemporaryDirectory() as tmpdir:
        db_path = Path(tmpdir) / "test.db"
        
        # 重置单例以使用新的数据库路径
        JARClassDB._instance = None
        
        db = JARClassDB(str(db_path))
        db.initialize_schema()
        
        # 插入测试数据
        classes = [
            ClassInfo(
                fqn="com.example.User",
                simple_name="User",
                package_name="com.example",
                jar_name="test.jar",
                jar_path="/path/to/test.jar",
                is_anonymous=False,
                insert_time="2024-01-01 00:00:00",
                file_path="com/example/User.class"
            ),
            ClassInfo(
                fqn="com.example.Order",
                simple_name="Order",
                package_name="com.example",
                jar_name="test.jar",
                jar_path="/path/to/test.jar",
                is_anonymous=False,
                insert_time="2024-01-01 00:00:00",
                file_path="com/example/Order.class"
            )
        ]
        
        count = db.batch_insert_classes(classes)
        assert count == 2, f"应该插入 2 条记录，实际插入 {count} 条"
        
        # 查询数据
        user = db.query_by_fqn("com.example.User")
        assert user is not None, "应该能查询到 User 类"
        assert user.file_path == "com/example/User.class", f"file_path 应该是 'com/example/User.class'，实际是 '{user.file_path}'"
        
        order = db.query_by_fqn("com.example.Order")
        assert order is not None, "应该能查询到 Order 类"
        assert order.file_path == "com/example/Order.class", f"file_path 应该是 'com/example/Order.class'，实际是 '{order.file_path}'"
        
        db.close()
    
    print("✓ 成功插入和查询包含 file_path 的数据")


def test_scan_jar_with_file_path():
    """测试扫描 JAR 文件时保存 file_path"""
    print("\n测试 4: 扫描 JAR 文件时保存 file_path")
    
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = Path(tmpdir)
        
        # 创建测试 JAR 文件
        jar_path = tmpdir / "test.jar"
        with zipfile.ZipFile(jar_path, 'w') as jar:
            # 添加一些测试类文件
            jar.writestr("com/example/Test.class", b"fake class content")
            jar.writestr("com/example/model/User.class", b"fake class content")
            jar.writestr("org/utils/Helper.class", b"fake class content")
        
        # 创建数据库
        db_path = tmpdir / "test.db"
        
        # 重置单例以使用新的数据库路径
        JARClassDB._instance = None
        
        db = JARClassDB(str(db_path))
        db.initialize_schema()
        
        # 扫描 JAR
        scanner = JARScanner(db)
        count = scanner.scan_jar(str(jar_path), include_anonymous=False)
        
        assert count == 3, f"应该扫描到 3 个类，实际扫描到 {count} 个"
        
        # 验证 file_path 是否正确保存
        test_class = db.query_by_fqn("com.example.Test")
        assert test_class is not None, "应该能查询到 Test 类"
        assert test_class.file_path == "com/example/Test.class", \
            f"Test 类的 file_path 应该是 'com/example/Test.class'，实际是 '{test_class.file_path}'"
        
        user_class = db.query_by_fqn("com.example.model.User")
        assert user_class is not None, "应该能查询到 User 类"
        assert user_class.file_path == "com/example/model/User.class", \
            f"User 类的 file_path 应该是 'com/example/model/User.class'，实际是 '{user_class.file_path}'"
        
        helper_class = db.query_by_fqn("org.utils.Helper")
        assert helper_class is not None, "应该能查询到 Helper 类"
        assert helper_class.file_path == "org/utils/Helper.class", \
            f"Helper 类的 file_path 应该是 'org/utils/Helper.class'，实际是 '{helper_class.file_path}'"
        
        db.close()
    
    print("✓ 扫描 JAR 文件时正确保存 file_path")


def main():
    """运行所有测试"""
    print("=" * 60)
    print("测试 file_path 字段功能")
    print("=" * 60)
    
    try:
        test_file_path_in_classinfo()
        test_database_schema()
        test_insert_and_query_with_file_path()
        test_scan_jar_with_file_path()
        
        print("\n" + "=" * 60)
        print("✓ 所有测试通过！")
        print("=" * 60)
        
    except AssertionError as e:
        print(f"\n✗ 测试失败: {e}")
        raise
    except Exception as e:
        print(f"\n✗ 测试出错: {e}")
        raise


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
测试查询 simple_name 字段
"""
import sqlite3
from pathlib import Path
from tools.constants import PROJECT_ROOT_PATH


def test_direct_query():
    """直接使用 sqlite3 查询"""
    db_path = PROJECT_ROOT_PATH / ".cache" / "jar_classes.db"
    
    print(f"数据库路径: {db_path}")
    print(f"数据库存在: {db_path.exists()}")
    print(f"数据库大小: {db_path.stat().st_size / 1024 / 1024:.2f} MB\n")
    
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    # 测试 1: 总记录数
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes")
    total = cursor.fetchone()['count']
    print(f"总记录数: {total}")
    
    # 测试 2: simple_name 不为空的记录数
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name IS NOT NULL")
    not_null = cursor.fetchone()['count']
    print(f"simple_name 不为空: {not_null}")
    
    # 测试 3: simple_name 为空的记录数
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name IS NULL OR simple_name = ''")
    null_count = cursor.fetchone()['count']
    print(f"simple_name 为空: {null_count}\n")
    
    # 测试 4: 使用 LIMIT 30 查询
    print("=" * 60)
    print("测试查询: SELECT simple_name FROM jar_classes LIMIT 30")
    print("=" * 60)
    cursor.execute("SELECT simple_name FROM jar_classes LIMIT 30")
    rows = cursor.fetchall()
    print(f"查询到 {len(rows)} 条记录:\n")
    for i, row in enumerate(rows[:10], 1):
        print(f"{i:2d}. '{row['simple_name']}'")
    
    # 测试 5: 使用 LIMIT 30 OFFSET 0 查询
    print("\n" + "=" * 60)
    print("测试查询: SELECT simple_name FROM jar_classes LIMIT 30 OFFSET 0")
    print("=" * 60)
    cursor.execute("SELECT simple_name FROM jar_classes LIMIT 30 OFFSET 0")
    rows = cursor.fetchall()
    print(f"查询到 {len(rows)} 条记录:\n")
    for i, row in enumerate(rows[:10], 1):
        print(f"{i:2d}. '{row['simple_name']}'")
    
    # 测试 6: 查询完整信息
    print("\n" + "=" * 60)
    print("测试查询: SELECT * FROM jar_classes LIMIT 5")
    print("=" * 60)
    cursor.execute("SELECT simple_name, fqn, package_name, jar_name FROM jar_classes LIMIT 5")
    rows = cursor.fetchall()
    for i, row in enumerate(rows, 1):
        print(f"\n记录 {i}:")
        print(f"  simple_name: {row['simple_name']}")
        print(f"  fqn: {row['fqn']}")
        print(f"  package_name: {row['package_name']}")
        print(f"  jar_name: {row['jar_name']}")
    
    conn.close()


def test_with_jar_class_db():
    """使用 JARClassDB 查询"""
    from storage.sqlite.jar_class_db import get_jar_class_db
    
    print("\n" + "=" * 60)
    print("使用 JARClassDB 查询")
    print("=" * 60)
    
    db = get_jar_class_db()
    print(f"数据库路径: {db.db_path}\n")
    
    # 查询前 5 个类
    cursor = db.conn.cursor()
    cursor.execute("SELECT simple_name, fqn FROM jar_classes LIMIT 5")
    rows = cursor.fetchall()
    
    print("前 5 个类:")
    for i, row in enumerate(rows, 1):
        print(f"{i}. {row[0]} ({row[1]})")


if __name__ == "__main__":
    test_direct_query()
    test_with_jar_class_db()

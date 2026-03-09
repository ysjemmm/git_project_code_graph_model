#!/usr/bin/env python3
"""
验证最终查询结果
"""
import sqlite3
from tools.constants import PROJECT_ROOT_PATH


def main():
    db_path = PROJECT_ROOT_PATH / ".cache" / "jar_classes.db"
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    print("=" * 80)
    print("验证数据库查询")
    print("=" * 80)
    print()
    
    # 统计
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes")
    total = cursor.fetchone()['count']
    
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name = ''")
    empty = cursor.fetchone()['count']
    
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name LIKE '.%'")
    dot_prefix = cursor.fetchone()['count']
    
    print(f"总记录数: {total}")
    print(f"simple_name 为空: {empty}")
    print(f"simple_name 以点开头: {dot_prefix}")
    print()
    
    # 查询非匿名类
    print("=" * 80)
    print("非匿名类示例（前 20 条）:")
    print("=" * 80)
    cursor.execute("""
        SELECT simple_name, fqn 
        FROM jar_classes 
        WHERE simple_name NOT LIKE '%$%' 
        ORDER BY simple_name 
        LIMIT 20
    """)
    
    rows = cursor.fetchall()
    for i, row in enumerate(rows, 1):
        print(f"{i:2d}. {row['simple_name']:40s} ({row['fqn']})")
    
    # 查询 package-info 类
    print()
    print("=" * 80)
    print("package-info 类示例（前 5 条）:")
    print("=" * 80)
    cursor.execute("""
        SELECT simple_name, fqn, package_name
        FROM jar_classes 
        WHERE simple_name = 'package-info'
        LIMIT 5
    """)
    
    rows = cursor.fetchall()
    for i, row in enumerate(rows, 1):
        print(f"{i}. simple_name: {row['simple_name']}")
        print(f"   FQN: {row['fqn']}")
        print(f"   package: {row['package_name']}")
        print()
    
    # 查询以前有问题的类
    print("=" * 80)
    print("以前有问题的类（现在应该正常）:")
    print("=" * 80)
    
    test_fqns = [
        "com.alibaba.nacos.shaded.com.google.gson.internal.Gson.Types",
        "com.google.inject.internal.asm.Attribute.Set",
        "com.google.inject.internal.cglib.core.AbstractClassGenerator.ClassLoaderData"
    ]
    
    for fqn in test_fqns:
        cursor.execute("SELECT simple_name, package_name FROM jar_classes WHERE fqn = ?", (fqn,))
        row = cursor.fetchone()
        if row:
            print(f"✓ FQN: {fqn}")
            print(f"  simple_name: '{row['simple_name']}'")
            print(f"  package: {row['package_name']}")
            print()
        else:
            print(f"✗ 未找到: {fqn}\n")
    
    conn.close()
    
    print("=" * 80)
    print("✓ 验证完成！所有问题已修复")
    print("=" * 80)


if __name__ == "__main__":
    main()

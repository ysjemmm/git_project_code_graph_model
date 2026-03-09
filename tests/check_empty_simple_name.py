#!/usr/bin/env python3
"""
检查 simple_name 为空的记录
"""
import sqlite3
from tools.constants import PROJECT_ROOT_PATH


def main():
    db_path = PROJECT_ROOT_PATH / ".cache" / "jar_classes.db"
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    # 查询 simple_name 为空的记录
    cursor.execute("""
        SELECT fqn, simple_name, package_name, jar_name 
        FROM jar_classes 
        WHERE simple_name = '' OR simple_name IS NULL
        LIMIT 20
    """)
    
    rows = cursor.fetchall()
    
    print(f"找到 {len(rows)} 条 simple_name 为空的记录:\n")
    
    for i, row in enumerate(rows, 1):
        print(f"记录 {i}:")
        print(f"  FQN: {row['fqn']}")
        print(f"  simple_name: '{row['simple_name']}'")
        print(f"  package_name: {row['package_name']}")
        print(f"  jar_name: {row['jar_name']}")
        print()
    
    # 统计
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name = ''")
    empty_count = cursor.fetchone()['count']
    
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name IS NULL")
    null_count = cursor.fetchone()['count']
    
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name != '' AND simple_name IS NOT NULL")
    valid_count = cursor.fetchone()['count']
    
    print("=" * 60)
    print("统计:")
    print(f"  simple_name = '': {empty_count}")
    print(f"  simple_name IS NULL: {null_count}")
    print(f"  simple_name 有效: {valid_count}")
    print("=" * 60)
    
    # 正确的查询方式
    print("\n正确的查询方式:")
    print("=" * 60)
    cursor.execute("""
        SELECT simple_name 
        FROM jar_classes 
        WHERE simple_name != '' AND simple_name IS NOT NULL
        LIMIT 30
    """)
    
    rows = cursor.fetchall()
    print(f"查询到 {len(rows)} 条有效记录:\n")
    for i, row in enumerate(rows[:10], 1):
        print(f"{i:2d}. {row['simple_name']}")
    
    conn.close()


if __name__ == "__main__":
    main()

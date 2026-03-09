#!/usr/bin/env python3
"""
检查以点开头的 simple_name
"""
import sqlite3
from tools.constants import PROJECT_ROOT_PATH


def main():
    db_path = PROJECT_ROOT_PATH / ".cache" / "jar_classes.db"
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    # 查询以点开头的 simple_name
    cursor.execute("""
        SELECT fqn, simple_name, package_name, jar_name, file_path
        FROM jar_classes 
        WHERE simple_name LIKE '.%'
        LIMIT 20
    """)
    
    rows = cursor.fetchall()
    
    print(f"找到 {len(rows)} 条以点开头的 simple_name:\n")
    
    for i, row in enumerate(rows, 1):
        print(f"记录 {i}:")
        print(f"  FQN: {row['fqn']}")
        print(f"  simple_name: '{row['simple_name']}'")
        print(f"  package_name: {row['package_name']}")
        print(f"  file_path: {row['file_path']}")
        print(f"  jar_name: {row['jar_name']}")
        print()
    
    # 统计
    cursor.execute("SELECT COUNT(*) as count FROM jar_classes WHERE simple_name LIKE '.%'")
    dot_count = cursor.fetchone()['count']
    
    print("=" * 60)
    print(f"以点开头的 simple_name 总数: {dot_count}")
    print("=" * 60)
    
    conn.close()


if __name__ == "__main__":
    main()

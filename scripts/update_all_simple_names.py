#!/usr/bin/env python3
"""
更新数据库中的所有 simple_name

这个脚本会：
1. 使用 file_path 重新解析所有记录
2. 更新 FQN, simple_name 和 package_name 字段
"""
import sqlite3
from pathlib import Path
from tools.constants import PROJECT_ROOT_PATH
from storage.sqlite.class_name_parser import ClassNameParser


def update_all_simple_names(db_path: Path):
    """更新所有 simple_name"""
    print(f"正在处理数据库: {db_path}")
    
    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    
    # 检查是否有 file_path 列
    cursor.execute("PRAGMA table_info(jar_classes)")
    columns = [row[1] for row in cursor.fetchall()]
    has_file_path = 'file_path' in columns
    
    if not has_file_path:
        print("  警告: 数据库没有 file_path 列，无法重新解析\n")
        conn.close()
        return
    
    # 查找所有有 file_path 的记录
    cursor.execute("""
        SELECT id, fqn, simple_name, package_name, file_path
        FROM jar_classes 
        WHERE file_path IS NOT NULL AND file_path != ''
    """)
    
    rows = cursor.fetchall()
    
    if not rows:
        print("  没有需要更新的记录\n")
        conn.close()
        return
    
    print(f"  找到 {len(rows)} 条记录")
    
    parser = ClassNameParser()
    updated_count = 0
    batch_size = 1000
    updates = []
    
    for i, row in enumerate(rows, 1):
        file_path = row['file_path']
        
        # 使用 file_path 重新解析
        fqn, simple_name, package_name, is_anonymous = parser.parse_class_path(file_path)
        
        # 只有当值改变时才更新
        if (fqn != row['fqn'] or simple_name != row['simple_name'] or 
            package_name != row['package_name']):
            updates.append((fqn, simple_name, package_name, row['id']))
            updated_count += 1
            
            if updated_count <= 5:
                print(f"    更新: {row['fqn']}")
                if fqn != row['fqn']:
                    print(f"      新 FQN: {fqn}")
                if simple_name != row['simple_name']:
                    print(f"      旧 simple_name: '{row['simple_name']}'")
                    print(f"      新 simple_name: '{simple_name}'")
        
        # 批量提交
        if len(updates) >= batch_size:
            cursor.executemany("""
                UPDATE jar_classes 
                SET fqn = ?, simple_name = ?, package_name = ?
                WHERE id = ?
            """, updates)
            conn.commit()
            updates.clear()
            if i % 5000 == 0:
                print(f"    进度: {i}/{len(rows)} ({i/len(rows)*100:.1f}%)")
    
    # 提交剩余的更新
    if updates:
        cursor.executemany("""
            UPDATE jar_classes 
            SET fqn = ?, simple_name = ?, package_name = ?
            WHERE id = ?
        """, updates)
        conn.commit()
    
    if updated_count > 5:
        print(f"    ... 还更新了 {updated_count - 5} 条记录")
    conn.close()
    
    print(f"  ✓ 成功更新 {updated_count} 条记录\n")


def main():
    """主函数"""
    print("=" * 60)
    print("更新所有 simple_name")
    print("=" * 60)
    print()
    
    cache_dir = PROJECT_ROOT_PATH / ".cache"
    
    # 更新 Maven JAR 数据库
    jar_db = cache_dir / "jar_classes.db"
    if jar_db.exists():
        print("[1] Maven JAR 数据库")
        update_all_simple_names(jar_db)
    else:
        print("[1] Maven JAR 数据库不存在\n")
    
    # 更新所有 JDK 数据库
    jdk_dbs = list(cache_dir.glob("jdk*_classes.db"))
    
    if jdk_dbs:
        print(f"[2] JDK 数据库 (找到 {len(jdk_dbs)} 个)")
        for jdk_db in jdk_dbs:
            update_all_simple_names(jdk_db)
    else:
        print("[2] 未找到 JDK 数据库\n")
    
    print("=" * 60)
    print("更新完成！")
    print("=" * 60)


if __name__ == "__main__":
    main()

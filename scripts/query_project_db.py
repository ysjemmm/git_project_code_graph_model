"""
查询项目类数据库
"""
import sys
from pathlib import Path

# 添加项目根目录到 Python 路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from storage.sqlite import get_project_class_db


def main():
    """查询项目类数据库"""
    db = get_project_class_db()
    
    # 查询所有项目
    cursor = db.conn.cursor()
    cursor.execute("SELECT DISTINCT project_name FROM project_classes")
    projects = [row[0] for row in cursor.fetchall()]
    
    if not projects:
        print("数据库为空")
        return
    
    print(f"找到 {len(projects)} 个项目:\n")
    
    for project in projects:
        classes = db.query_by_project(project, include_anonymous=False)
        nested_classes = [c for c in classes if c.is_nested]
        top_level_classes = [c for c in classes if not c.is_nested]
        
        print(f"项目: {project}")
        print(f"  总类数: {len(classes)}")
        print(f"  顶层类: {len(top_level_classes)}")
        print(f"  嵌套类: {len(nested_classes)}")
        
        # 按包名分组统计
        packages = {}
        for cls in classes:
            pkg = cls.package_name or "(default)"
            packages[pkg] = packages.get(pkg, 0) + 1
        
        print(f"  包数量: {len(packages)}")
        print(f"\n  前 10 个包:")
        for pkg, count in sorted(packages.items(), key=lambda x: -x[1])[:10]:
            print(f"    - {pkg}: {count} 个类")
        
        print(f"\n  前 20 个类:")
        for cls in classes[:20]:
            nested_info = f" (嵌套在 {cls.parent_class})" if cls.is_nested else ""
            print(f"    - {cls.fqn}{nested_info}")
        
        if len(classes) > 20:
            print(f"    ... 还有 {len(classes) - 20} 个类")
        
        print()


if __name__ == "__main__":
    main()

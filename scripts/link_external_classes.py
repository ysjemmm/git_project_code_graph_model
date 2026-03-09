#!/usr/bin/env python3
"""
链接外部类到内部实现

使用场景：
- 类 A 作为 JAR 被引用时，创建 EXTERNAL_DEFINITION 节点
- 类 A 作为项目源码导入时，创建 INNER_DEFINITION 节点
- 此脚本将这两个节点通过 LIB_LINK 关系连接起来

可以在任何时候运行，支持增量链接
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.external_linker import ExternalClassLinker


def main():
    """主函数"""
    print("=" * 70)
    print("外部类链接工具")
    print("=" * 70)
    print("\n说明：将同一个类的外部定义（JAR）和内部定义（源码）链接起来")
    print("匹配规则：fqn 和 belong_project 必须相同\n")
    
    # Neo4j 配置
    neo4j_uri = "neo4j+s://26fa83e0.databases.neo4j.io"
    neo4j_user = "neo4j"
    neo4j_password = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"
    neo4j_database = "neo4j"
    
    # 连接 Neo4j
    print("连接到 Neo4j...")
    connector = Neo4jConnector(neo4j_uri, neo4j_user, neo4j_password, neo4j_database)
    
    try:
        linker = ExternalClassLinker(connector)
        
        # 步骤 1: 查看当前统计
        print("\n" + "=" * 70)
        print("当前统计")
        print("=" * 70)
        
        stats = linker.get_statistics()
        print(f"\n已链接的类: {stats['linked_count']}")
        print(f"未链接的外部类: {stats['unlinked_count']}")
        
        # 步骤 2: 预览可以链接的类
        print("\n" + "=" * 70)
        print("预览匹配结果")
        print("=" * 70)
        
        dry_run_result = linker.link_all(dry_run=True)
        print(f"\n找到 {dry_run_result['matches_found']} 个可以链接的类")
        
        if dry_run_result['matches_found'] == 0:
            print("\n[INFO] 没有找到可以链接的类")
            
            # 显示一些未链接的外部类
            unlinked = linker.find_unlinked_external_classes(limit=10)
            if unlinked:
                print(f"\n未链接的外部类示例（前 10 个）:")
                for i, cls in enumerate(unlinked, 1):
                    print(f"  {i}. {cls['fqn']}")
                    print(f"     项目: {cls['belong_project']}")
            
            return 0
        
        # 显示一些重复定义的类
        duplicates = linker.find_duplicate_definitions(limit=10)
        if duplicates:
            print(f"\n重复定义的类示例（前 10 个）:")
            for i, dup in enumerate(duplicates, 1):
                status = "✓ 已链接" if dup['is_linked'] else "✗ 未链接"
                print(f"  {i}. {dup['fqn']} ({dup['project']}) - {status}")
        
        # 步骤 3: 确认是否继续
        print("\n" + "=" * 70)
        print("创建链接")
        print("=" * 70)
        
        response = input(f"\n是否创建 {dry_run_result['matches_found']} 个链接关系? (y/n): ")
        if response.lower() != 'y':
            print("[INFO] 用户取消操作")
            return 0
        
        # 步骤 4: 实际创建关系
        result = linker.link_all(dry_run=False)
        print(f"\n[OK] 成功创建 {result['relationships_created']} 个链接关系")
        
        # 步骤 5: 显示最终统计
        print("\n" + "=" * 70)
        print("最终统计")
        print("=" * 70)
        
        final_stats = linker.get_statistics()
        print(f"\n已链接的类: {final_stats['linked_count']}")
        print(f"未链接的外部类: {final_stats['unlinked_count']}")
        
        print("\n" + "=" * 70)
        print("完成")
        print("=" * 70)
        
        return 0
        
    finally:
        connector.close()


if __name__ == "__main__":
    sys.exit(main())

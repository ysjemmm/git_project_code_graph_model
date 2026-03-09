#!/usr/bin/env python3
"""简化版导入测试：克隆仓库 -> 创建 Merkle Tree -> AST 解析 -> 导入 Neo4j"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))


def main():
    """简化版工作流：
    1. 克隆 Git 仓库
    2. 创建 Merkle Tree
    3. AST 解析所有 Java 文件
    4. 导入到 Neo4j
    """
    from core.importer import GitToNeo4jImporter

    print("=" * 70)
    print("简化版导入测试")
    print("=" * 70)

    # Neo4j 配置
    neo4j_uri = "neo4j+s://26fa83e0.databases.neo4j.io"
    neo4j_user = "neo4j"
    neo4j_password = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"
    neo4j_database = "neo4j"

    # Git 配置
    # repo_url = "http://git.timevale.cn:8081/infra-frame/epaas-gateway.git"
    # branch = "master"

    repo_url = "http://git.timevale.cn:8081/infra-frame/epaas-gateway.git"
    commit_id = "bedabddf3d8c6f9d3da1562aeca66f2fac9fbeb0"

    # 创建导入器
    git_importer = GitToNeo4jImporter(neo4j_uri, neo4j_user, neo4j_password, neo4j_database)

    if not git_importer.connect():
        print("[ERROR] 无法连接到 Neo4j")
        return 1

    try:
        # 步骤 1: 克隆仓库
        print("\n" + "=" * 70)
        print(f"步骤 1: 克隆仓库 {repo_url}")
        print(f"commit id: {commit_id}")
        print("=" * 70)

        result = git_importer.import_from_git(
            repo_url=repo_url,
            commit_id=commit_id,
            java_source_dir=None,
            clear_database=False,  # 清空数据库
            async_mode=False
        )

        if not result['success']:
            print(f"[ERROR] 导入失败: {result.get('error', '未知错误')}")
            return 1

        # 步骤 2: 显示结果
        print("\n" + "=" * 70)
        print("导入完成")
        print("=" * 70)

        print(f"\n✅ 导入成功")
        print(f"  - 新增节点: {result.get('added_nodes', 0)}")
        print(f"  - 新增关系: {result.get('added_relationships', 0)}")

        # 步骤 3: 获取数据库统计
        print("\n" + "=" * 70)
        print("数据库统计")
        print("=" * 70)

        stats = git_importer.connector.get_statistics()
        print(f"\n总体统计:")
        print(f"  - 总节点数: {stats.get('total_nodes', 0)}")
        print(f"  - 总关系数: {stats.get('total_relationships', 0)}")

        if stats.get('node_types'):
            print(f"\n节点类型分布:")
            for label, count in sorted(stats['node_types'].items(), key=lambda x: x[1], reverse=True):
                print(f"  - {label}: {count}")

        if stats.get('relationship_types'):
            print(f"\n关系类型分布:")
            for rel_type, count in sorted(stats['relationship_types'].items(), key=lambda x: x[1], reverse=True):
                print(f"  - {rel_type}: {count}")

        print("\n" + "=" * 70)
        print("✅ 测试完成")
        print("=" * 70)

        return 0

    finally:
        git_importer.disconnect()


if __name__ == "__main__":
    sys.exit(main())

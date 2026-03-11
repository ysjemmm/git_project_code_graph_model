#!/usr/bin/env python3
"""简化版导入测试：克隆仓库 -> 创建 Merkle Tree -> AST 解析 -> 导入 Neo4j"""

import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))


def _load_repo_env_from_local():
    """
    在调用 os.getenv 之前，先从项目根目录的 .env.local/.env 中加载 REPO_URL/REPO_BRANCH。
    规则：
    - 不覆盖已有环境变量（例如你在终端里手动 export 的）
    - .env.local 优先于 .env
    """
    root = Path(__file__).resolve().parent.parent
    for filename in [".env.local", ".env"]:
        env_path = root / filename
        if not env_path.exists():
            continue
        try:
            content = env_path.read_text(encoding="utf-8")
        except Exception:
            continue
        for raw in content.splitlines():
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip("'").strip('"')
            if key in ("REPO_URL", "REPO_BRANCH") and key not in os.environ and value:
                os.environ[key] = value


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

    # 先从 .env.local/.env 补充 REPO_URL/REPO_BRANCH
    _load_repo_env_from_local()

    # Git 仓库配置：优先从环境变量/本地配置读取
    repo_url = os.getenv("REPO_URL", "http://example")
    branch = os.getenv("REPO_BRANCH", "master")

    # 创建导入器
    # Neo4j 配置读取优先级：环境变量 > .env.local > .env > 默认值
    # 为了避免测试脚本中的占位符覆盖真实配置，这里不再传入连接参数。
    git_importer = GitToNeo4jImporter()

    if not git_importer.connect():
        print("[ERROR] 无法连接到 Neo4j")
        return 1

    try:
        # 步骤 1: 克隆仓库
        print("\n" + "=" * 70)
        print(f"步骤 1: 克隆仓库 {repo_url}")
        print(f"分支: {branch}")
        print("=" * 70)

        result = git_importer.import_from_git(
            repo_url=repo_url,
            branch=branch,
            java_source_dir=None,
            clear_database=True,  # 清空数据库
            async_mode=False
        )

        if not result['success']:
            print(f"[ERROR] 导入失败: {result.get('error', '未知错误')}")
            return 1

        # 步骤 2: 显示结果
        print("\n" + "=" * 70)
        print("导入完成")
        print("=" * 70)

        print("\n导入成功")
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
        print("测试完成")
        print("=" * 70)

        return 0

    finally:
        git_importer.disconnect()


if __name__ == "__main__":
    sys.exit(main())


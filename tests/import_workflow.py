#!/usr/bin/env python3

import sys
import os
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

def _load_env_vars(keys: set[str]) -> None:
    """
    从项目根目录的 .env.local/.env 中补齐指定环境变量（不覆盖已存在的环境变量）。
    .env.local 优先于 .env
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
            k, v = line.split("=", 1)
            k = k.strip()
            v = v.strip().strip("'").strip('"')
            if k in keys and k not in os.environ and v:
                os.environ[k] = v

def main():
    """完整工作流测试：
    1. 清空数据库和缓存
    2. 拉取 master 分支代码
    3. 切换到 feature/aaa-test-dijiu 分支
    4. 比较两个分支的变化
    5. 增量更新图数据库的节点与关系
    """
    import shutil
    from pathlib import Path
    from core.importer import GitToNeo4jImporter

    print("=" * 70)
    print("完整工作流测试")
    print("=" * 70)

    # 从本地配置加载（不覆盖系统环境变量）
    _load_env_vars({"REPO_URL", "REPO_BRANCH"})
    repo_url = os.getenv("REPO_URL", "http://example.git")
    master_branch = os.getenv("REPO_MASTER_BRANCH", "master")
    # 默认使用 REPO_BRANCH 作为“对比分支”；如果未设置则使用旧默认值
    feature_branch = os.getenv("REPO_FEATURE_BRANCH", os.getenv("REPO_BRANCH", "feature/aaa-test-dijiu"))

    # 步骤 0: 清空数据库和缓存
    print("\n" + "=" * 70)
    print("步骤 0: 清空数据库和缓存")
    print("=" * 70)

    # Neo4j 配置读取优先级：环境变量 > .env.local > .env > 默认值
    git_importer = GitToNeo4jImporter()

    if not git_importer.connect():
        print("[ERROR] 无法连接到 Neo4j")
        return 1

    try:
        # 清空当前项目子图（不会清空整库）
        print("\n清空当前项目子图...")
        stats_before = git_importer.connector.get_statistics()
        print(
            f"  清空前: {stats_before.get('total_nodes', 0)} 个节点, {stats_before.get('total_relationships', 0)} 个关系")

        project_name = repo_url.split("/")[-1].replace(".git", "")
        deleted_count = git_importer.connector.delete_project_data(project_name)
        print(f"  本次删除节点数: {deleted_count}")

        stats_after = git_importer.connector.get_statistics()
        print(
            f"  清空后: {stats_after.get('total_nodes', 0)} 个节点, {stats_after.get('total_relationships', 0)} 个关系")

        # 清空缓存
        cache_dir = Path(".cache/git_repos/lops-flight")
        if cache_dir.exists():
            print("\n清空缓存目录...")
            try:
                shutil.rmtree(cache_dir)
                print(f"  已删除: {cache_dir}")
            except PermissionError:
                print(f"  权限不足，尝试使用系统命令...")
                import subprocess
                try:
                    if sys.platform == 'win32':
                        subprocess.run(['powershell', '-Command', f'Remove-Item -Recurse -Force "{cache_dir}"'],
                                       capture_output=True, timeout=30)
                    else:
                        subprocess.run(['rm', '-rf', str(cache_dir)],
                                       capture_output=True, timeout=30)
                    print(f"  已删除: {cache_dir}")
                except Exception as e:
                    print(f"  [WARNING] 无法删除缓存目录: {str(e)}")

        # 步骤 1: 拉取 master 分支代码
        print("\n" + "=" * 70)
        print("步骤 1: 拉取 master 分支代码")
        print("=" * 70)

        print("\n导入 master 分支...")
        result = git_importer.import_from_git(
            repo_url=repo_url,
            branch=master_branch,
            java_source_dir=None,
            clear_database=False,
            async_mode=False
        )

        if not result['success']:
            print(f"[ERROR] 导入失败: {result.get('error', '未知错误')}")
            return 1

        print(f"\nMaster 分支导入完成")
        print(f"  - 新增节点: {result.get('added_nodes', 0)}")
        print(f"  - 新增关系: {result.get('added_relationships', 0)}")

        stats = git_importer.connector.get_statistics()
        print(f"\n数据库统计:")
        print(f"  - 总节点数: {stats.get('total_nodes', 0)}")
        print(f"  - 总关系数: {stats.get('total_relationships', 0)}")

        if stats.get('node_types'):
            print(f"\n节点类型分布:")
            for label, count in sorted(stats['node_types'].items(), key=lambda x: x[1], reverse=True):
                print(f"  - {label}: {count}")

        # 步骤 2 & 3: 切换到 xxx 分支并比较变化
        print("\n" + "=" * 70)
        print("步骤 2 & 3: 切换到 feature/aaa-test-dijiu 分支并比较变化")
        print("=" * 70)

        print(f"\n切换到 {feature_branch} 分支...")
        result = git_importer.import_from_git(
            repo_url=repo_url,
            branch=feature_branch,
            java_source_dir=None,
            clear_database=False,
            async_mode=False
        )

        if not result['success']:
            print(f"[WARNING] 切换失败: {result.get('error', '未知错误')}")
            print("  可能原因：远程不存在该分支，或没有权限访问。")
            print("  你可以通过设置环境变量 REPO_FEATURE_BRANCH 来指定一个存在的分支。")
            return 0

        print(f"\n分支切换完成")

        # 显示文件变化
        print(f"\n文件变化统计:")
        print(f"  - 新增文件: {len(result.get('added_files', []))} 个")
        print(f"  - 变更文件: {len(result.get('changed_files', []))} 个")
        print(f"  - 删除文件: {len(result.get('deleted_files', []))} 个")

        if result.get('added_files'):
            print(f"\n新增文件:")
            for file in result['added_files']:
                print(f"  + {file}")

        if result.get('changed_files'):
            print(f"\n变更文件:")
            for file in result['changed_files']:
                print(f"  ~ {file}")

        if result.get('deleted_files'):
            print(f"\n删除文件:")
            for file in result['deleted_files']:
                print(f"  - {file}")

        # 显示导入统计
        print(f"\n导入统计:")
        print(f"  - 新增节点: {result.get('added_nodes', 0)}")
        print(f"  - 新增关系: {result.get('added_relationships', 0)}")

        # 获取数据库统计
        stats = git_importer.connector.get_statistics()
        print(f"\n数据库统计:")
        print(f"  - 总节点数: {stats.get('total_nodes', 0)}")
        print(f"  - 总关系数: {stats.get('total_relationships', 0)}")

        if stats.get('node_types'):
            print(f"\n节点类型分布:")
            for label, count in sorted(stats['node_types'].items(), key=lambda x: x[1], reverse=True):
                print(f"  - {label}: {count}")

        # 总结
        print("\n" + "=" * 70)
        print("完整工作流测试成功完成")
        print("=" * 70)
        print("\n总结:")
        print("  1. 清空了数据库和缓存")
        print("  2. 拉取了 master 分支代码")
        print("  3. 切换到 feature/aaa-test-dijiu 分支")
        print("  4. 比较了两个分支的变化")
        print("  5. 增量更新了图数据库的节点与关系")
        print("=" * 70)

        return 0

    finally:
        git_importer.disconnect()


if __name__ == "__main__":
    sys.exit(main())
#!/usr/bin/env python3

import sys
import os
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

# from parser.java.import_from_ast import ASTToNeo4jImporter
# from storage.neo4j.connector import Neo4jConnectorPool
# from git.incremental_analyzer import GitIncrementalAnalyzer, GitIncrementalAnalyzer


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

    # Neo4j 配置
    neo4j_uri = "neo4j+s://26fa83e0.databases.neo4j.io"
    neo4j_user = "neo4j"
    neo4j_password = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"
    neo4j_database = "neo4j"

    # 步骤 0: 清空数据库和缓存
    print("\n" + "=" * 70)
    print("步骤 0: 清空数据库和缓存")
    print("=" * 70)

    git_importer = GitToNeo4jImporter(neo4j_uri, neo4j_user, neo4j_password, neo4j_database)

    if not git_importer.connect():
        print("[ERROR] 无法连接到 Neo4j")
        return 1

    try:
        # 清空数据库
        print("\n清空 Neo4j 数据库...")
        stats_before = git_importer.connector.get_statistics()
        print(
            f"  清空前: {stats_before.get('total_nodes', 0)} 个节点, {stats_before.get('total_relationships', 0)} 个关系")

        git_importer.connector.clear_database()

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
            repo_url="http://git.timevale.cn:8081/devops/lops-flight.git",
            branch="master",
            java_source_dir=None,
            clear_database=False,
            async_mode=False
        )

        if not result['success']:
            print(f"[ERROR] 导入失败: {result.get('error', '未知错误')}")
            return 1

        print(f"\n✅ Master 分支导入完成")
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

        # 步骤 2 & 3: 切换到 feature/aaa-test-dijiu 分支并比较变化
        print("\n" + "=" * 70)
        print("步骤 2 & 3: 切换到 feature/aaa-test-dijiu 分支并比较变化")
        print("=" * 70)

        print("\n切换到 feature/aaa-test-dijiu 分支...")
        result = git_importer.import_from_git(
            repo_url="http://git.timevale.cn:8081/devops/lops-flight.git",
            branch="feature/aaa-test-dijiu",
            java_source_dir=None,
            clear_database=False,
            async_mode=False
        )

        if not result['success']:
            print(f"[ERROR] 切换失败: {result.get('error', '未知错误')}")
            return 1

        print(f"\n✅ 分支切换完成")

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
        print("✅ 完整工作流测试成功完成")
        print("=" * 70)
        print("\n总结:")
        print("  1. ✅ 清空了数据库和缓存")
        print("  2. ✅ 拉取了 master 分支代码")
        print("  3. ✅ 切换到 feature/aaa-test-dijiu 分支")
        print("  4. ✅ 比较了两个分支的变化")
        print("  5. ✅ 增量更新了图数据库的节点与关系")
        print("=" * 70)

        return 0

    finally:
        git_importer.disconnect()


if __name__ == "__main__":
    sys.exit(main())

# def main():
#     """AST 到 Neo4j 导入工作流主函数 - 完整测试流程（包括增量更新）"""
#
#     print("=" * 70)
#     print("AST 到 Neo4j 导入工作流 - 完整测试流程（包括增量更新）")
#     print("=" * 70)
#
#     # Neo4j 配置
#     neo4j_uri = "neo4j+s://26fa83e0.databases.neo4j.io"
#     neo4j_user = "neo4j"
#     neo4j_password = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"
#     neo4j_database = "neo4j"
#
#     # Git 配置
#     git_repo_url = "http://git.timevale.cn:8081/devops/lops-flight.git"
#     cache_base_dir = ".cache/git_repos"
#     git_branch_target = "feature/aaa-test-dijiu"
#
#     # 从 git_repo_url 智能解析项目名称
#     repo_name = git_repo_url.split('/')[-1].replace('.git', '')
#
#     # 仓库缓存路径
#     git_repo_path = os.path.join(cache_base_dir, repo_name)
#
#     # 创建导入器
#     importer = ASTToNeo4jImporter(neo4j_uri, neo4j_user, neo4j_password, neo4j_database)
#
#     if not importer.connect():
#         print("[ERROR] 无法连接Neo4j")
#         return 1
#
#     try:
#         # 步骤 0: 清理之前的项目、Merkle Tree 及对应 Metadata
#         print("\n" + "=" * 70)
#         print("步骤 0: 清理之前的项目、Merkle Tree 及对应 Metadata")
#         print("=" * 70)
#
#         from storage.cache.git_cache import GitCacheManager
#         cache_manager = GitCacheManager(cache_base_dir)
#         #
#         print(f"[INFO] 清理仓库缓存: {repo_name}")
#         success = cache_manager.cleanup_repo(repo_name)
#         if success:
#             print(f"[OK] 已清理仓库缓存")
#             print(f"  - 删除仓库目录: {cache_manager.get_repo_cache_dir(repo_name)}")
#             print(f"  - 删除元数据文件: {cache_manager.get_metadata_file(repo_name)}")
#             print(f"  - 删除 Merkle Tree 缓存")
#         else:
#             print(f"[WARN] 清理缓存失败，继续执行...")
#
#         # 步骤 1: 清空图数据库
#         print("\n" + "=" * 70)
#         print("步骤 1: 清空图数据库")
#         print("=" * 70)
#
#         if not importer.connector.clear_database():
#             print("[ERROR] 清空数据库失败")
#             return 1
#         print("[OK] 数据库已清空")
#
#         # ========== 第一阶段：克隆 master 分支并导入 ==========
#         print("\n" + "=" * 70)
#         print("第一阶段：克隆 master 分支并导入到图数据库")
#         print("=" * 70)
#
#         # 步骤 2: 从 Git 仓库下载（master 分支）
#         print("\n" + "=" * 70)
#         print("步骤 2: 从 Git 仓库下载（master 分支）")
#         print("=" * 70)
#
#         from git.manager import GitManager
#         git_manager = GitManager(git_repo_path)
#
#         incremental_analyzer = GitIncrementalAnalyzer(cache_base_dir)
#         incremental_analyzer.analyze_git_repo(
#             repo_url=
#         )
#
#         # 计算动态超时
#         clone_timeout = GitManager.calculate_dynamic_timeout(git_repo_url)
#         print(f"[INFO] 动态超时: {clone_timeout} 秒")
#
#         # 如果仓库已存在，执行 fetch 和 checkout
#         if git_manager.is_repo_exists():
#             print(f"[INFO] 仓库已存在，执行 fetch 和 checkout...")
#
#             # fetch 获取最新的远程分支信息
#             success, msg = git_manager.fetch()
#             if not success:
#                 print(f"[ERROR] Fetch 失败: {msg}")
#                 return 1
#             print(f"[OK] Fetch 成功")
#
#             # 切换到 master 分支
#             current_branch = git_manager.get_current_branch()
#             if current_branch != "master":
#                 success, msg = git_manager.checkout("master")
#                 if not success:
#                     print(f"[ERROR] 切换到 master 失败: {msg}")
#                     return 1
#                 print(f"[OK] 切换到 master 成功")
#         else:
#             # 执行浅克隆
#             print(f"[INFO] 开始克隆仓库: {git_repo_url}")
#             success, msg = git_manager.clone(
#                 repo_url=git_repo_url,
#                 branch="master",
#                 shallow=True,
#                 skip_fsck=True,
#                 timeout=clone_timeout
#             )
#
#             if not success:
#                 print(f"[ERROR] 克隆失败: {msg}")
#                 return 1
#             print(f"[OK] {msg}")
#
#         # 获取当前分支和提交信息
#         current_branch = git_manager.get_current_branch()
#         current_commit = git_manager.get_current_commit()
#         print(f"[INFO] 当前分支: {current_branch}")
#         print(f"[INFO] 当前提交: {current_commit}")
#
#         # 步骤 3: AST 解析 -> 导入到图数据库（master 分支）
#         print("\n" + "=" * 70)
#         print("步骤 3: AST 解析 -> 导入到图数据库（master 分支）")
#         print("=" * 70)
#
#         result = importer.import_from_java_directory(
#             java_source_path=None,
#             project_name=repo_name,
#             clear_database=False
#         )
#
#         if not result['success']:
#             print(f"[ERROR] 导入失败: {result.get('error', '未知错误')}")
#             return 1
#
#         print(f"\n[OK] Master 分支导入成功")
#         print(f"  - 节点数: {result.get('nodes_count', 0)}")
#         print(f"  - 关系数: {result.get('relationships_count', 0)}")
#
#         # ========== 第二阶段：切换分支并进行增量更新 ==========
#         print("\n" + "=" * 70)
#         print("第二阶段：切换分支并进行增量更新")
#         print("=" * 70)
#
#         # 步骤 4: 切换到目标分支
#         print("\n" + "=" * 70)
#         print(f"步骤 4: 切换到分支 {git_branch_target}")
#         print("=" * 70)
#
#         success, msg = git_manager.checkout(git_branch_target)
#         if not success:
#             print(f"[ERROR] 切换分支失败: {msg}")
#             return 1
#         print(f"[OK] {msg}")
#
#         # 获取当前分支和提交信息
#         current_branch = git_manager.get_current_branch()
#         current_commit = git_manager.get_current_commit()
#         print(f"[INFO] 当前分支: {current_branch}")
#         print(f"[INFO] 当前提交: {current_commit}")
#
#         # 步骤 5: 使用 Merkle Tree 判断文件变更
#         print("\n" + "=" * 70)
#         print("步骤 5: 使用 Merkle Tree 判断文件变更")
#         print("=" * 70)
#
#         incremental_analyzer = GitIncrementalAnalyzer(cache_base_dir)
#
#         # 使用 analyze_git_repo 来进行 Merkle Tree 对比（不检查 Java 源代码目录）
#         # 但我们需要先构建 Merkle Tree，所以直接调用
#         from storage.cache.merkle_tree import MerkleTreeBuilder
#         from storage.cache.git_cache import GitCacheManager
#
#         cache_mgr = GitCacheManager(cache_base_dir)
#         merkle_builder = MerkleTreeBuilder()
#
#         # 构建当前分支的 Merkle 树
#         new_tree = merkle_builder.build(git_repo_path)
#
#         # 加载 master 分支的 Merkle 树
#         old_tree = cache_mgr.load_merkle_tree(repo_name, "master")
#
#         # 对比 Merkle 树，找出变化的文件
#         added_files = []
#         changed_files = []
#         deleted_files = []
#
#         if old_tree:
#             print(f"[INFO] 对比 Merkle 树，检测变化文件...")
#
#             # 收集两个树中的所有文件及其哈希
#             old_files_dict = {}
#             new_files_dict = {}
#             incremental_analyzer._collect_all_files_with_hash(old_tree, old_files_dict)
#             incremental_analyzer._collect_all_files_with_hash(new_tree, new_files_dict)
#
#             # 新增文件
#             for name, (path, hash_val) in new_files_dict.items():
#                 if name not in old_files_dict:
#                     added_files.append(path)
#
#             # 删除文件
#             for name, (path, hash_val) in old_files_dict.items():
#                 if name not in new_files_dict:
#                     deleted_files.append(path)
#
#             # 变更文件
#             for name in old_files_dict:
#                 if name in new_files_dict:
#                     old_path, old_hash = old_files_dict[name]
#                     new_path, new_hash = new_files_dict[name]
#                     if old_hash != new_hash:
#                         changed_files.append(new_path)
#         else:
#             print(f"[INFO] 首次分析，无旧 Merkle 树")
#             # 收集所有 Java 文件作为新增
#             for root, dirs, files in os.walk(git_repo_path):
#                 for file in files:
#                     if file.endswith('.java'):
#                         added_files.append(os.path.join(root, file))
#
#         # 保存当前分支的 Merkle 树
#         cache_mgr.save_merkle_tree(repo_name, git_branch_target, new_tree)
#
#         print(f"[OK] 增量分析完成")
#
#         print(f"\n[INFO] 文件变更统计:")
#         print(f"  - 新增文件: {len(added_files)}")
#         print(f"  - 变更文件: {len(changed_files)}")
#         print(f"  - 删除文件: {len(deleted_files)}")
#
#         # 步骤 6: 增量更新图数据库
#         print("\n" + "=" * 70)
#         print("步骤 6: 增量更新图数据库")
#         print("=" * 70)
#
#         if not added_files and not changed_files and not deleted_files:
#             print("[INFO] 没有文件变更，无需更新")
#         else:
#             print(f"[INFO] 开始增量更新...")
#
#             # 处理删除的文件
#             if deleted_files:
#                 print(f"\n[INFO] 处理 {len(deleted_files)} 个删除的文件...")
#                 for file_path in deleted_files:
#                     importer.connector.delete_nodes_by_file(file_path, repo_name)
#                 print(f"[OK] 删除完成")
#
#             # 处理新增和变更的文件
#             if added_files or changed_files:
#                 files_to_process = added_files + changed_files
#                 print(f"\n[INFO] 处理 {len(files_to_process)} 个新增/变更的文件...")
#
#                 # 重新解析这些文件并更新
#                 result = importer.import_from_java_directory(
#                     java_source_path=None,
#                     project_name=repo_name,
#                     clear_database=False
#                 )
#
#                 if not result['success']:
#                     print(f"[ERROR] 增量更新失败: {result.get('error', '未知错误')}")
#                     return 1
#
#                 print(f"[OK] 增量更新完成")
#                 print(f"  - 节点数: {result.get('nodes_count', 0)}")
#                 print(f"  - 关系数: {result.get('relationships_count', 0)}")
#
#         # 获取最终统计信息
#         print("\n" + "=" * 70)
#         print("最终统计信息")
#         print("=" * 70)
#
#         stats = importer.connector.get_statistics()
#         if stats:
#             print(f"[INFO] 数据库最终统计:")
#             print(f"  - 总节点数: {stats.get('total_nodes', 0)}")
#             print(f"  - 总关系数: {stats.get('total_relationships', 0)}")
#
#             if stats.get('node_types'):
#                 print(f"\n[INFO] 节点类型分布:")
#                 for label, count in sorted(stats['node_types'].items(), key=lambda x: x[1], reverse=True):
#                     print(f"  - {label}: {count}")
#
#         print("\n" + "=" * 70)
#         print("[OK] 完整工作流成功完成")
#         print("=" * 70)
#
#         return 0
#
#     except Exception as e:
#         print(f"\n[ERROR] 工作流异常: {e}")
#         import traceback
#         traceback.print_exc()
#         return 1
#
#     finally:
#         importer.disconnect()
#         Neo4jConnectorPool.close_all()
#
#
# if __name__ == "__main__":
#     sys.exit(main())
#!/usr/bin/env python3
"""
链接外部类到内部实现

使用场景：
- 类 A 作为 JAR 被引用时，创建 EXTERNAL_DEFINITION 节点
- 类 A 作为项目源码导入时，创建 INNER_DEFINITION 节点
- 此脚本将这两个节点通过 LIB_LINK 关系连接起来

注意：
- 不在脚本中硬编码任何真实地址/密码
- 所有 Neo4j 配置均来自：环境变量 > 项目根目录 .env.local > .env
"""

import os
import sys
import argparse
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from storage.neo4j.connector import Neo4jConnector
from storage.neo4j.external_linker import ExternalClassLinker

from core.env_loader import load_env_vars


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="外部类链接工具（LIB_LINK）")
    parser.add_argument("--project-name", default=None, help="项目名称（兜底过滤 belong_project）")
    parser.add_argument("--project-key", default=None, help="项目唯一键（推荐，精确过滤 project_key）")
    parser.add_argument("--dry-run", action="store_true", help="仅统计/采样，不创建关系")
    parser.add_argument("--sample-limit", type=int, default=10, help="采样返回数量（默认 10）")
    parser.add_argument(
        "--no-sample",
        action="store_true",
        help="不返回 sample（只输出统计）",
    )
    return parser


def _print_sample(sample: list[dict] | None) -> None:
    if not sample:
        print("  (无 sample)")
        return
    for i, row in enumerate(sample, 1):
        status = "✓ 已链接" if row.get("is_linked") else "✗ 未链接"
        print(f"  {i}. {row.get('fqn')} ({row.get('project')}) - {status}")
        print(f"     external: {row.get('external_symbol_id')}")
        print(f"     internal: {row.get('internal_symbol_id')}")


def main() -> int:
    """主函数"""
    args = build_arg_parser().parse_args()

    print("=" * 70)
    print("外部类链接工具")
    print("=" * 70)
    print("\n说明：将同一个类的外部定义（JAR）和内部定义（源码）链接起来")
    print("匹配规则：fqn 和 belong_project 必须相同\n")

    # Neo4j 配置：优先使用环境变量/本地配置，不在脚本中硬编码真实凭据
    load_env_vars({"NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD", "NEO4J_DATABASE"})

    neo4j_uri = os.getenv("NEO4J_URI", "bolt://localhost:7687")
    neo4j_user = os.getenv("NEO4J_USER", "neo4j")
    neo4j_password = os.getenv("NEO4J_PASSWORD", "password")
    neo4j_database = os.getenv("NEO4J_DATABASE", "neo4j")

    # 连接 Neo4j
    print("连接到 Neo4j...")
    connector = Neo4jConnector(neo4j_uri, neo4j_user, neo4j_password, neo4j_database)

    try:
        linker = ExternalClassLinker(connector)

        project_name = args.project_name
        project_key = args.project_key or ""
        include_sample = not bool(args.no_sample)
        sample_limit = int(args.sample_limit)

        # 步骤 1: 查看当前统计
        print("\n" + "=" * 70)
        print("当前统计")
        print("=" * 70)

        stats = linker.get_statistics()
        print(f"\n已链接的类: {stats['linked_count']}")
        print(f"未链接的外部类: {stats['unlinked_count']}")

        # 步骤 2: 预览/统计可以链接的类
        print("\n" + "=" * 70)
        print("预览匹配结果（可观测）")
        print("=" * 70)

        if project_name:
            dry_run_result = linker.link_by_project(
                project_name=project_name,
                project_key=project_key,
                dry_run=True,
                sample_limit=sample_limit,
                include_sample=include_sample,
            )
        else:
            dry_run_result = linker.link_all(dry_run=True, sample_limit=sample_limit)

        print(f"\n匹配到 {dry_run_result.get('matches_found', 0)} 对外部/内部定义（matched）")
        print("Sample（前 N 条，含是否已链接）:")
        _print_sample(dry_run_result.get("sample"))

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

        if args.dry_run:
            print("\n[OK] dry-run 模式：未创建任何关系")
            return 0

        # 步骤 3: 确认是否继续
        print("\n" + "=" * 70)
        print("创建链接")
        print("=" * 70)

        scope_desc = f"项目 '{project_name}'" if project_name else "全库"
        response = input(
            f"\n是否在 {scope_desc} 下创建 LIB_LINK 关系？匹配对数={dry_run_result['matches_found']} (y/n): "
        )
        if response.lower() != "y":
            print("[INFO] 用户取消操作")
            return 0

        # 步骤 4: 实际创建关系
        if project_name:
            result = linker.link_by_project(
                project_name=project_name,
                project_key=project_key,
                dry_run=False,
                sample_limit=sample_limit,
                include_sample=include_sample,
            )
        else:
            result = linker.link_all(dry_run=False, sample_limit=sample_limit)

        print(
            f"\n[OK] 匹配对数={result.get('matched_count', result.get('matches_found', 0))}，"
            f"实际新建 LIB_LINK={result.get('created_count', result.get('relationships_created', 0))}"
        )

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
        connector.disconnect()


if __name__ == "__main__":
    sys.exit(main())

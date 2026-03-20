#!/usr/bin/env python3

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from core.console_utf8 import setup_console_utf8
from core.env_loader import load_env_vars
from storage.neo4j import Neo4jConnector


def resolve_project_key(
    connector: Neo4jConnector,
    *,
    project_name: str,
) -> Optional[str]:
    """
    通过 Project 节点解析 project_key（symbol_id）。

    规则：
    - 优先匹配 project_type='Application' 且 name=project_name
    - 若匹配到唯一结果：返回其 symbol_id
    - 若无匹配：返回 None
    - 若多匹配：打印候选并返回 None（避免误选）
    """
    cypher = """
    MATCH (p:Project)
    WHERE p.name = $project_name AND p.project_type = 'Application'
    RETURN p.symbol_id AS project_key, p.name AS name, p.project_type AS project_type
    ORDER BY p.symbol_id
    """
    rows = connector.execute_query(cypher, {"project_name": project_name}) or []
    if not rows:
        return None
    if len(rows) == 1:
        return rows[0].get("project_key")

    print("\n" + "=" * 70)
    print("[WARN] 找到多个同名 Project(Application) 节点，无法自动选择，请显式指定 --project-key")
    print("=" * 70)
    for r in rows:
        print(f"  - name={r.get('name')} project_type={r.get('project_type')} project_key={r.get('project_key')}")
    return None


def list_projects(connector: Neo4jConnector, limit: int = 50) -> None:
    """列出 Project(Application) 候选（便于复制 project_key）。"""
    cypher = """
    MATCH (p:Project)
    WHERE p.project_type = 'Application'
    RETURN p.name AS name, p.symbol_id AS project_key, p.project_type AS project_type
    ORDER BY p.name, p.symbol_id
    LIMIT $limit
    """
    rows = connector.execute_query(cypher, {"limit": int(limit)}) or []
    print("\n" + "=" * 70)
    print("Project(Application) 列表")
    print("=" * 70)
    if not rows:
        print("  (无数据)")
        return
    for r in rows:
        print(f"  - {r.get('name')}  project_key={r.get('project_key')}")


def query_relationships(
    connector: Neo4jConnector,
    project_name: Optional[str] = None,
    project_key: Optional[str] = None,
) -> None:
    """查询关系类型分布（可选按项目过滤）。"""
    print("\n" + "=" * 70)
    print("关系类型分布")
    print("=" * 70)

    # 说明：为了兼容多仓库，我们尽量按 project_key 过滤两端节点；
    # 若未提供 project_key/project_name，则统计全库关系。
    params = {
        "project_name": project_name or "",
        "project_key": project_key or "",
    }

    cypher = """
    MATCH (s)-[r]->(t)
    WHERE
      ($project_key IS NULL OR $project_key = "" OR
       coalesce(s.project_key, t.project_key) = $project_key) AND
      ($project_name IS NULL OR $project_name = "" OR
       coalesce(s.belong_project, t.belong_project, "") = $project_name)
    RETURN type(r) as relationship_type, count(*) as count
    ORDER BY count DESC
    """

    result = connector.execute_query(cypher, params)

    if not result:
        print("  (无数据)")
        return

    for record in result:
        print(f"  {record['relationship_type']}: {record['count']}")


def query_method_calls(
    connector: Neo4jConnector,
    project_name: Optional[str] = None,
    project_key: Optional[str] = None,
) -> None:
    print("\n" + "=" * 70)
    print("方法调用关系 (CALLS)")
    print("=" * 70)

    params = {
        "project_name": project_name or "",
        "project_key": project_key or "",
    }

    cypher = """
    MATCH (m1:Method)-[r:CALLS]->(m2:Method)
    WHERE
      ($project_key IS NULL OR $project_key = "" OR
       coalesce(m1.project_key, m2.project_key) = $project_key) AND
      ($project_name IS NULL OR $project_name = "" OR
       coalesce(m1.belong_project, m2.belong_project, "") = $project_name)
    RETURN m1.name as caller, m2.name as callee
    ORDER BY m1.name, m2.name
    """

    result = connector.execute_query(cypher, params)

    if result:
        for record in result:
            print(f"  {record['caller']} -> {record['callee']}")
    else:
        print("  (无数据)")


def query_field_access(
    connector: Neo4jConnector,
    project_name: Optional[str] = None,
    project_key: Optional[str] = None,
) -> None:
    print("\n" + "=" * 70)
    print("字段访问关系 (ACCESSES)")
    print("=" * 70)

    params = {
        "project_name": project_name or "",
        "project_key": project_key or "",
    }

    cypher = """
    MATCH (m:Method)-[r:ACCESSES]->(f:Field)
    WHERE
      ($project_key IS NULL OR $project_key = "" OR
       coalesce(m.project_key, f.project_key) = $project_key) AND
      ($project_name IS NULL OR $project_name = "" OR
       coalesce(m.belong_project, f.belong_project, "") = $project_name)
    RETURN m.name as method, f.name as field
    ORDER BY m.name, f.name
    """

    result = connector.execute_query(cypher, params)

    if result:
        for record in result:
            print(f"  {record['method']} -> {record['field']}")
    else:
        print("  (无数据)")


def query_api_endpoints(
    connector: Neo4jConnector,
    project_name: Optional[str] = None,
    project_key: Optional[str] = None,
) -> None:
    print("\n" + "=" * 70)
    print("API 端点")
    print("=" * 70)

    params = {
        "project_name": project_name or "",
        "project_key": project_key or "",
    }

    cypher = """
    MATCH (c:JavaObject)-[:MEMBER_OF]-(m:Method)
    WHERE m.request_mapping_path IS NOT NULL
      AND ($project_key IS NULL OR $project_key = "" OR
           coalesce(c.project_key, m.project_key) = $project_key)
      AND ($project_name IS NULL OR $project_name = "" OR
           coalesce(c.belong_project, m.belong_project, "") = $project_name)
    RETURN c.name as controller,
           m.name as method,
           m.request_mapping_path as path,
           m.request_methods as methods
    ORDER BY controller, method
    """

    result = connector.execute_query(cypher, params)

    if result:
        for record in result:
            methods = record.get("methods") or ["GET"]
            print(f"  {record['controller']}.{record['method']}")
            print(f"    Path: {record['path']}")
            print(f"    Methods: {', '.join(methods)}")
    else:
        print("  (无数据)")


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Neo4j 代码图谱查询工具（支持按 project_name / project_key 过滤）"
    )
    parser.add_argument(
        "--project-name",
        help="项目名称（对应节点上的 belong_project，用作兜底过滤）",
        default=None,
    )
    parser.add_argument(
        "--project-key",
        help="项目唯一键（通常为项目根节点的 symbol_id，强推荐优先使用）",
        default=None,
    )
    parser.add_argument(
        "--list-projects",
        action="store_true",
        help="列出 Project(Application) 候选（用于查找/复制 project_key）",
    )
    parser.add_argument(
        "--project-list-limit",
        type=int,
        default=50,
        help="--list-projects 的最大返回数量（默认 50）",
    )
    parser.add_argument(
        "--skip-relationships",
        action="store_true",
        help="跳过关系类型分布查询",
    )
    parser.add_argument(
        "--only-api",
        action="store_true",
        help="仅查询 API 端点（跳过其他查询）",
    )
    return parser


def main(argv: Optional[list[str]] = None) -> int:
    setup_console_utf8()
    parser = build_arg_parser()
    args = parser.parse_args(argv)

    project_name: Optional[str] = args.project_name
    project_key: Optional[str] = args.project_key

    print("=" * 70)
    print("Neo4j 代码图谱查询工具")
    print("=" * 70)
    if project_key or project_name:
        print("查询范围(输入):")
        if project_name:
            print(f"  - project_name: {project_name}")
        if project_key:
            print(f"  - project_key:  {project_key}")
    else:
        print("查询范围(输入): 全库（未指定 project_name / project_key）")

    # Neo4j 配置：环境变量 > .env.local > .env > 默认值（不在代码中硬编码真实凭据）
    load_env_vars({"NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD", "NEO4J_DATABASE"})
    connector = Neo4jConnector(
        os.getenv("NEO4J_URI", "bolt://localhost:7687"),
        os.getenv("NEO4J_USER", "neo4j"),
        os.getenv("NEO4J_PASSWORD", "password"),
        os.getenv("NEO4J_DATABASE", "neo4j"),
    )

    if not connector.connect():
        print("[ERROR] 无法连接 Neo4j")
        return 1

    try:
        if args.list_projects:
            list_projects(connector, limit=args.project_list_limit)
            return 0

        # 如果只给了 project_name 而没给 project_key，则自动解析一次（避免手动抄 symbol_id）
        if project_name and not project_key:
            resolved = resolve_project_key(connector, project_name=project_name)
            if resolved:
                project_key = resolved
                print("\n" + "=" * 70)
                print("[OK] 已解析 project_key")
                print("=" * 70)
                print(f"  - project_name: {project_name}")
                print(f"  - project_key:  {project_key}")
            else:
                print("\n[ERROR] 无法从 Neo4j 解析唯一 project_key，请使用 --list-projects 或显式传入 --project-key")
                return 2

        if not args.only_api and not args.skip_relationships:
            query_relationships(connector, project_name=project_name, project_key=project_key)

        if not args.only_api:
            query_method_calls(connector, project_name=project_name, project_key=project_key)
            query_field_access(connector, project_name=project_name, project_key=project_key)

        query_api_endpoints(connector, project_name=project_name, project_key=project_key)

        print("\n" + "=" * 70)
        print("[OK] 查询完成")
        print("=" * 70)
        return 0
    finally:
        connector.disconnect()


if __name__ == "__main__":
    sys.exit(main())
"""
代码图谱管理接口：列出已构建的项目、版本信息、以及缓存元数据。
"""
from __future__ import annotations

import os
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

from fastapi import APIRouter, Query

from api.schemas import GraphProjectItem, GraphProjectListResponse

router = APIRouter(prefix="/api", tags=["graph"])

_CACHE_TTL_SEC = 15
_projects_cache: dict[bool, tuple[float, GraphProjectListResponse]] = {}


def _neo4j_connector():
    uri = os.environ.get("NEO4J_URI", "")
    user = os.environ.get("NEO4J_USER", "neo4j")
    password = os.environ.get("NEO4J_PASSWORD", "")
    database = os.environ.get("NEO4J_DATABASE", "neo4j")
    if not uri or not password:
        return None, "[图数据库未配置：缺少 NEO4J_URI / NEO4J_PASSWORD]"
    try:
        from storage.neo4j.connector import Neo4jConnector
    except Exception:
        return None, "[无法导入 Neo4j 连接器]"

    conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
    if not conn.connect():
        return None, "[图数据库连接失败]"
    return conn, None


@router.get("/graph/projects", response_model=GraphProjectListResponse)
def graph_projects(
    include_counts: bool = Query(
        False,
        description="是否包含节点/关系统计（可能较慢）。默认 false 以保证响应速度。",
    ),
) -> GraphProjectListResponse:
    """
    列出当前 Neo4j 中已存在的 Project 节点（去重），并尽力补全：
    - branch / commit_hash（若导入时写入）
    - repo_url / last_update_time（从本地 .cache/metadata/<repo_name>.json 读取；repo_name 默认等于 project_name）
    - node_count / relationship_count（按 project_key 或 project_name 粗略统计）
    """
    # 轻量缓存：前端页面会频繁刷新/切换 tab，避免每次都打 Neo4j
    import time as _time

    now = _time.time()
    cached = _projects_cache.get(include_counts)
    if cached and (now - cached[0]) <= _CACHE_TTL_SEC:
        return cached[1]

    conn, err = _neo4j_connector()
    if conn is None:
        # 前端仍需要结构化返回
        return GraphProjectListResponse(items=[GraphProjectItem(project_name="(error)", repo_url=err)])

    try:
        # 默认只展示 active 版本，避免版本化后列表出现多份重复
        active_where = "WHERE coalesce(p.is_active,false) = true OR p.is_active IS NULL"

        if include_counts:
            # 说明：避免 N+1 统计查询，node_count / relationship_count 用全库聚合后再 join 到 Project。
            # 目前以 belong_project 作为聚合键（兼容旧数据不含 project_key 的情况）。
            q = """
            CALL () {
              MATCH (n)
              WITH coalesce(n.belong_project, '') AS project_name
              WHERE project_name <> ""
              RETURN project_name, count(*) AS node_count
            }
            WITH collect({project_name: project_name, node_count: node_count}) AS node_counts
            CALL () {
              MATCH (n)-[r]-()
              WITH coalesce(n.belong_project, '') AS project_name, r
              WHERE project_name <> ""
              RETURN project_name, count(DISTINCT r) AS relationship_count
            }
            WITH node_counts, collect({project_name: project_name, relationship_count: relationship_count}) AS rel_counts
            MATCH (p:Project)
            __ACTIVE_WHERE__
            WITH p, node_counts, rel_counts
            WITH
              coalesce(p.name, p.belong_project, '') AS project_name,
              coalesce(p.symbol_id, '') AS project_key,
              coalesce(p.project_type, '') AS project_type,
              coalesce(p.branch, '') AS branch,
              coalesce(p.commit_hash, '') AS commit_hash,
              node_counts,
              rel_counts
            WITH
              project_name,
              project_key,
              project_type,
              branch,
              commit_hash,
              [x IN node_counts WHERE x.project_name = project_name | x.node_count][0] AS node_count,
              [x IN rel_counts WHERE x.project_name = project_name | x.relationship_count][0] AS relationship_count
            RETURN project_name, project_key, project_type, branch, commit_hash,
                   coalesce(node_count, 0) AS node_count,
                   coalesce(relationship_count, 0) AS relationship_count
            """
            q = q.replace("__ACTIVE_WHERE__", active_where)
            rows: List[Dict[str, Any]] = conn.execute_read_query(q) or []
        else:
            q = """
            MATCH (p:Project)
            WHERE coalesce(p.is_active,false) = true OR p.is_active IS NULL
            RETURN
              coalesce(p.name, p.belong_project, '') AS project_name,
              coalesce(p.symbol_id, '') AS project_key,
              coalesce(p.project_type, '') AS project_type,
              coalesce(p.branch, '') AS branch,
              coalesce(p.commit_hash, '') AS commit_hash
            """
            rows = conn.execute_read_query(q) or []

        # 去重：同一 (project_name, project_type, project_key) 只保留一条
        seen = set()
        deduped = []
        for r in rows:
            k = (
                (r.get("project_name") or "").strip(),
                (r.get("project_type") or "").strip(),
                (r.get("project_key") or "").strip(),
            )
            if k in seen:
                continue
            seen.add(k)
            deduped.append(r)
        rows = deduped

        # 加载本地 Git 缓存 metadata（repo_url/last_update_time）
        meta_map: Dict[str, Dict[str, Any]] = {}
        try:
            from storage.cache.git_cache import GitCacheManager

            mgr = GitCacheManager(cache_base_dir=".cache/git_repos")
            # metadata 文件按 repo_name.json 命名；这里不列目录，按 project_name 时再懒加载
            def _load_meta(repo_name: str) -> Optional[Dict[str, Any]]:
                if repo_name in meta_map:
                    return meta_map[repo_name]
                md = mgr.load_metadata(repo_name)
                if md:
                    meta_map[repo_name] = md
                return md

        except Exception:
            _load_meta = lambda _: None  # type: ignore

        items: List[GraphProjectItem] = []
        for r in rows:
            project_name = (r.get("project_name") or "").strip() or "(unknown)"
            project_key = (r.get("project_key") or "").strip() or None
            project_type = (r.get("project_type") or "").strip() or None
            branch = (r.get("branch") or "").strip() or None
            commit_hash = (r.get("commit_hash") or "").strip() or None

            md = _load_meta(project_name)
            repo_url = (md or {}).get("repo_url") if md else None
            last_update_time = (md or {}).get("last_update_time") if md else None

            node_count = int(r.get("node_count") or 0) if include_counts else None
            rel_count = int(r.get("relationship_count") or 0) if include_counts else None

            items.append(
                GraphProjectItem(
                    project_name=project_name,
                    project_key=project_key,
                    project_type=project_type,
                    branch=branch,
                    commit_hash=commit_hash,
                    repo_url=repo_url,
                    last_update_time=last_update_time,
                    node_count=node_count,
                    relationship_count=rel_count,
                )
            )

        def _ts(s: Optional[str]) -> Tuple[int, str]:
            if not s:
                return (0, "")
            try:
                # git_cache.py 用 datetime.now().isoformat()，这里按 ISO 解析
                return (int(datetime.fromisoformat(s).timestamp()), s)
            except Exception:
                return (0, s)

        def _type_rank(t: Optional[str]) -> int:
            # Application > Lib，其它排后
            if (t or "").lower() == "application":
                return 0
            if (t or "").lower() == "lib":
                return 1
            return 2

        # 排序：添加时间（last_update_time）优先（新->旧），其次 Application > Lib
        items.sort(
            key=lambda x: (
                -_ts(x.last_update_time)[0],
                _type_rank(x.project_type),
                x.project_name,
            )
        )
        resp = GraphProjectListResponse(items=items)
        _projects_cache[include_counts] = (now, resp)
        return resp
    finally:
        conn.disconnect()


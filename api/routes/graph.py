"""
代码图谱管理接口：列出已构建的项目、版本信息、以及缓存元数据。
"""
from __future__ import annotations

import os
import re
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

from fastapi import APIRouter, Body, Query

from api.schemas import GraphProjectItem, GraphProjectListResponse
from storage.neo4j.query_diagnostics import list_neo4j_operations, summarize_neo4j_operations

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


_WRITE_CYPHER_RE = re.compile(
    r"\b(CREATE|MERGE|DELETE|DETACH|SET|REMOVE|DROP|LOAD\s+CSV|FOREACH)\b",
    re.IGNORECASE,
)


def _is_readonly_cypher(cypher: str) -> bool:
    text = (cypher or "").strip()
    if not text:
        return False
    if _WRITE_CYPHER_RE.search(text):
        return False
    return True


def _serialize_neo4j_value(v: Any) -> Any:
    if v is None or isinstance(v, (str, int, float, bool)):
        return v
    if isinstance(v, list):
        return [_serialize_neo4j_value(x) for x in v]
    if isinstance(v, dict):
        return {str(k): _serialize_neo4j_value(val) for k, val in v.items()}
    if isinstance(v, tuple):
        return [_serialize_neo4j_value(x) for x in v]

    # neo4j.graph.Node
    if hasattr(v, "labels") and hasattr(v, "items"):
        try:
            return {
                "__type__": "node",
                "labels": list(v.labels),
                "properties": {str(k): _serialize_neo4j_value(val) for k, val in dict(v.items()).items()},
            }
        except Exception:
            return str(v)

    # neo4j.graph.Relationship
    if hasattr(v, "type") and hasattr(v, "items"):
        try:
            start = None
            end = None
            if hasattr(v, "start_node"):
                start = _serialize_neo4j_value(getattr(v, "start_node"))
            if hasattr(v, "end_node"):
                end = _serialize_neo4j_value(getattr(v, "end_node"))
            return {
                "__type__": "relationship",
                "rel_type": str(v.type),
                "start": start,
                "end": end,
                "properties": {str(k): _serialize_neo4j_value(val) for k, val in dict(v.items()).items()},
            }
        except Exception:
            return str(v)

    return str(v)


def _to_int(v: Any) -> int:
    try:
        return int(v or 0)
    except Exception:
        return 0


def _to_float(v: Any) -> float:
    try:
        return float(v or 0)
    except Exception:
        return 0.0


def _show_indexes_rows(session: Any) -> List[Dict[str, Any]]:
    # Neo4j 4+/5+ 优先：SHOW INDEXES
    try:
        rows = [dict(r) for r in session.run("SHOW INDEXES")]
        if rows:
            return rows
    except Exception:
        pass

    # 兼容部分环境：CALL db.indexes()
    try:
        q = """
        CALL db.indexes() YIELD name, state, type, entityType, labelsOrTypes, properties, populationPercent
        RETURN name, state, type, entityType, labelsOrTypes, properties, populationPercent
        """
        return [dict(r) for r in session.run(q)]
    except Exception:
        return []


def _show_constraints_rows(session: Any) -> List[Dict[str, Any]]:
    try:
        return [dict(r) for r in session.run("SHOW CONSTRAINTS")]
    except Exception:
        return []


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
            # 说明：
            # - 旧写法先全图聚合后再按 project_name 过滤，数据量大时会慢（collect + 二次过滤）。
            # - 新写法先拿 active 项目，再按项目子查询计数，避免构造大型中间列表。
            # 目前仍以 belong_project 作为聚合键（兼容旧数据不含 project_key 的情况）。
            q = """
            MATCH (p:Project)
            __ACTIVE_WHERE__
            WITH
              coalesce(p.name, p.belong_project, '') AS project_name,
              coalesce(p.symbol_id, '') AS project_key,
              coalesce(p.project_type, '') AS project_type,
              coalesce(p.branch, '') AS branch,
              coalesce(p.commit_hash, '') AS commit_hash
            WHERE project_name <> ""
            WITH DISTINCT project_name, project_key, project_type, branch, commit_hash
            CALL (project_name) {
              MATCH (n)
              WHERE coalesce(n.belong_project, '') = project_name
              RETURN count(n) AS node_count
            }
            CALL (project_name) {
              MATCH (n)-[r]-()
              WHERE coalesce(n.belong_project, '') = project_name
              RETURN count(DISTINCT r) AS relationship_count
            }
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
            from tools.constants import CACHE_GIT_REPOS_PATH

            mgr = GitCacheManager(cache_base_dir=str(CACHE_GIT_REPOS_PATH))
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


@router.get("/graph/projects/{project_name}/second-party-deps")
def project_second_party_deps(project_name: str) -> Dict[str, Any]:
    """
    查询某个应用依赖的二方包列表。
    步骤：
    1. 从 Neo4j 查该项目引用的所有 ExternalDefinition 节点，取 jar_name 去重
    2. 从 jar_classes.db 补全 groupId / artifactId
    3. 用二方包规则匹配，标记是否为二方包
    返回全部依赖 jar，每条带 is_second_party 标记。
    """
    import re as _re
    from tools.constants import CACHE_JAR_CLASSES_DB_PATH

    conn, err = _neo4j_connector()
    if err:
        return {"ok": False, "message": err, "items": []}

    # 1. 从 Neo4j 取该项目引用的所有外部 jar（ExternalDefinition 节点）
    jar_names: List[str] = []
    try:
        cypher = """
        MATCH (p:Project {project_type: 'Application'})
        WHERE p.name = $project_name
        MATCH (p)-[:CONTAINS_LIB]->(ext:JavaObject)
        WHERE ext.from_type = 'ExternalDefinition' AND ext.jar_name IS NOT NULL AND ext.jar_name <> ''
        RETURN DISTINCT ext.jar_name AS jar_name
        ORDER BY jar_name
        """
        with conn.driver.session(database=conn.database) as session:
            result = session.run(cypher, {"project_name": project_name})
            jar_names = [r["jar_name"] for r in result if r.get("jar_name")]
    except Exception as e:
        return {"ok": False, "message": f"Neo4j 查询失败: {e}", "items": []}
    finally:
        conn.disconnect()

    if not jar_names:
        return {"ok": True, "items": []}

    # 2. 从 jar_classes.db 补全 groupId / artifactId（取每个 jar 的第一条记录即可）
    jar_meta: Dict[str, Dict[str, Any]] = {}
    try:
        from storage.sqlite.jar_class_db import JarClassDB
        db_path = str(CACHE_JAR_CLASSES_DB_PATH)
        jdb = JarClassDB(db_path=db_path)
        cur = jdb.conn.cursor()
        placeholders = ",".join("?" * len(jar_names))
        rows = cur.execute(
            f"""
            SELECT jar_name,
                   MAX(artifact_group_id) AS group_id,
                   MAX(artifact_id)       AS artifact_id,
                   MAX(artifact_version)  AS version
            FROM jar_classes
            WHERE jar_name IN ({placeholders})
            GROUP BY jar_name
            """,
            jar_names,
        ).fetchall()
        for r in rows:
            jar_meta[r["jar_name"]] = {
                "group_id": r["group_id"] or "",
                "artifact_id": r["artifact_id"] or "",
                "version": r["version"] or "",
            }
    except Exception:
        pass  # jar_classes.db 不存在时降级，只展示 jar_name

    # 3. 加载二方包规则并匹配
    rules: List[Dict[str, Any]] = []
    try:
        from storage.sqlite.business import get_business_db
        from storage.sqlite.business.second_party_rules_repo import SecondPartyRulesRepo
        bdb = get_business_db()
        rules = [r for r in SecondPartyRulesRepo(db=bdb).list_rules() if r.get("enabled")]
    except Exception:
        pass

    def _is_second_party(group_id: str, artifact_id: str) -> bool:
        for rule in rules:
            try:
                if _re.search(rule["group_id_regex"], group_id) and \
                   _re.search(rule["artifact_id_regex"], artifact_id):
                    return True
            except Exception:
                pass
        return False

    items = []
    for jar_name in jar_names:
        meta = jar_meta.get(jar_name, {})
        group_id = meta.get("group_id", "")
        artifact_id = meta.get("artifact_id", "")
        version = meta.get("version", "")
        items.append({
            "jar_name": jar_name,
            "group_id": group_id,
            "artifact_id": artifact_id,
            "version": version,
            "is_second_party": _is_second_party(group_id, artifact_id),
        })

    # 二方包排前面，其次按 jar_name 排序
    items.sort(key=lambda x: (0 if x["is_second_party"] else 1, x["jar_name"]))
    return {"ok": True, "items": items}


@router.post("/graph/query")
def graph_query(payload: Dict[str, Any] = Body(...)) -> Dict[str, Any]:
    """
    执行只读 Cypher 查询（用于前端“查询模板”页内联查询）。
    """
    cypher = str(payload.get("cypher") or "").strip()
    if not cypher:
        return {"ok": False, "message": "cypher 不能为空", "columns": [], "rows": []}
    if not _is_readonly_cypher(cypher):
        return {"ok": False, "message": "仅允许只读查询（禁止 CREATE/MERGE/DELETE/SET 等写操作）", "columns": [], "rows": []}

    try:
        limit = int(payload.get("limit", 200) or 200)
    except Exception:
        limit = 200
    limit = max(1, min(limit, 2000))

    raw_params = payload.get("params", {})
    params: Dict[str, Any] = raw_params if isinstance(raw_params, dict) else {}
    params["__limit__"] = limit

    conn, err = _neo4j_connector()
    if conn is None:
        return {"ok": False, "message": err or "图数据库不可用", "columns": [], "rows": []}

    rows: List[Dict[str, Any]] = []
    columns: List[str] = []
    try:
        with conn.driver.session(database=conn.database) as session:
            result = session.run(cypher, params)
            columns = list(result.keys())
            for record in result:
                row = {}
                for col in columns:
                    row[col] = _serialize_neo4j_value(record.get(col))
                rows.append(row)
                if len(rows) >= limit:
                    break
        return {"ok": True, "columns": columns, "rows": rows, "limit": limit}
    except Exception as e:
        return {"ok": False, "message": f"查询失败: {e}", "columns": [], "rows": []}
    finally:
        conn.disconnect()


@router.post("/graph/clear")
def graph_clear(payload: Dict[str, Any] = Body({})) -> Dict[str, Any]:
    """
    清空整个图谱（危险操作）。
    需要 confirm = "CLEAR_ALL" 才执行。
    """
    confirm = str(payload.get("confirm") or "").strip().upper()
    if confirm != "CLEAR_ALL":
        return {
            "ok": False,
            "message": "请提供 confirm=CLEAR_ALL 以确认清理全量图谱",
        }

    conn, err = _neo4j_connector()
    if conn is None:
        return {"ok": False, "message": err or "图数据库不可用"}

    try:
        with conn.driver.session(database=conn.database) as session:
            # 先统计，便于前端展示“清理了多少”
            before_nodes = _to_int(session.run("MATCH (n) RETURN count(n) AS c").single()["c"])
            before_rels = _to_int(session.run("MATCH ()-[r]-() RETURN count(DISTINCT r) AS c").single()["c"])
            session.run("MATCH (n) DETACH DELETE n")
            after_nodes = _to_int(session.run("MATCH (n) RETURN count(n) AS c").single()["c"])
            return {
                "ok": True,
                "message": "图谱已清理",
                "before_nodes": before_nodes,
                "before_relationships": before_rels,
                "after_nodes": after_nodes,
            }
    except Exception as e:
        return {"ok": False, "message": f"清理失败: {e}"}
    finally:
        conn.disconnect()


@router.get("/graph/diagnostics/summary")
def graph_diagnostics_summary() -> Dict[str, Any]:
    """
    诊断页汇总（MVP）：
    - 图数据库连接/配置检查
    - 基础数据规模统计
    - 项目覆盖与孤立节点的轻量检查
    """
    checks: List[Dict[str, Any]] = []
    stats: Dict[str, int] = {
        "project_count": 0,
        "application_project_count": 0,
        "node_count": 0,
        "relationship_count": 0,
        "unknown_project_node_count": 0,
    }
    schema: Dict[str, Any] = {
        "indexes": [],
        "constraints": [],
        "index_total": 0,
        "index_online": 0,
        "index_failed": 0,
    }

    score = 100

    conn, err = _neo4j_connector()
    if conn is None:
        checks.append({
            "id": "neo4j_connectivity",
            "title": "Neo4j 连接",
            "status": "error",
            "message": err or "图数据库不可用",
            "suggestion": "请检查 NEO4J_URI / NEO4J_USER / NEO4J_PASSWORD / NEO4J_DATABASE。",
        })
        score = 0
        return {
            "ok": True,
            "generated_at": datetime.now().isoformat(),
            "score": score,
            "stats": stats,
            "schema": schema,
            "ops": {
                "summary": summarize_neo4j_operations(window_minutes=60),
                "recent": list_neo4j_operations(limit=20),
            },
            "checks": checks,
            "alerts": {"error": 1, "warning": 0, "ok": 0},
        }

    try:
        # 基础统计
        with conn.driver.session(database=conn.database) as session:
            stats["project_count"] = _to_int(session.run("MATCH (p:Project) RETURN count(p) AS c").single()["c"])
            stats["application_project_count"] = _to_int(
                session.run("MATCH (p:Project) WHERE toLower(coalesce(p.project_type,'')) = 'application' RETURN count(p) AS c").single()["c"]
            )
            stats["node_count"] = _to_int(session.run("MATCH (n) RETURN count(n) AS c").single()["c"])
            stats["relationship_count"] = _to_int(session.run("MATCH ()-[r]-() RETURN count(DISTINCT r) AS c").single()["c"])
            stats["unknown_project_node_count"] = _to_int(
                session.run(
                    "MATCH (n) WHERE coalesce(n.belong_project,'') = '' RETURN count(n) AS c"
                ).single()["c"]
            )
            raw_indexes = _show_indexes_rows(session)
            raw_constraints = _show_constraints_rows(session)
            schema["indexes"] = [
                {
                    "name": str(x.get("name") or ""),
                    "state": str(x.get("state") or ""),
                    "type": str(x.get("type") or ""),
                    "entity_type": str(x.get("entityType") or ""),
                    "labels_or_types": x.get("labelsOrTypes") or [],
                    "properties": x.get("properties") or [],
                    "population_percent": _to_float(x.get("populationPercent")),
                }
                for x in raw_indexes
            ]
            schema["constraints"] = [
                {
                    "name": str(x.get("name") or ""),
                    "type": str(x.get("type") or ""),
                    "entity_type": str(x.get("entityType") or ""),
                    "labels_or_types": x.get("labelsOrTypes") or [],
                    "properties": x.get("properties") or [],
                }
                for x in raw_constraints
            ]
            schema["index_total"] = len(schema["indexes"])
            schema["index_online"] = len([x for x in schema["indexes"] if str(x.get("state") or "").upper() == "ONLINE"])
            schema["index_failed"] = len([x for x in schema["indexes"] if str(x.get("state") or "").upper() in {"FAILED", "POPULATING"}])

        # 检查项 1：连接
        checks.append({
            "id": "neo4j_connectivity",
            "title": "Neo4j 连接",
            "status": "ok",
            "message": "连接正常",
            "suggestion": "",
        })

        # 检查项 2：图谱是否为空
        if stats["node_count"] <= 0 or stats["project_count"] <= 0:
            score -= 45
            checks.append({
                "id": "graph_data_presence",
                "title": "图谱数据存在性",
                "status": "error",
                "message": "当前图谱为空或项目节点为 0。",
                "suggestion": "请先在“导入/重建”中执行导入任务。",
            })
        else:
            checks.append({
                "id": "graph_data_presence",
                "title": "图谱数据存在性",
                "status": "ok",
                "message": f"已发现 {stats['node_count']} 个节点，{stats['relationship_count']} 条关系。",
                "suggestion": "",
            })

        # 检查项 3：Application 覆盖
        if stats["application_project_count"] <= 0:
            score -= 25
            checks.append({
                "id": "application_presence",
                "title": "应用项目覆盖",
                "status": "warning",
                "message": "未发现 project_type=Application 的项目。",
                "suggestion": "请确认导入来源是否为应用仓库，或检查导入流程中的项目类型标注。",
            })
        else:
            checks.append({
                "id": "application_presence",
                "title": "应用项目覆盖",
                "status": "ok",
                "message": f"Application 项目数：{stats['application_project_count']}。",
                "suggestion": "",
            })

        # 检查项 4：归属缺失节点
        unknown = stats["unknown_project_node_count"]
        if unknown > 0:
            ratio = (unknown / max(1, stats["node_count"])) * 100
            if ratio >= 10:
                score -= 20
                status = "warning"
            else:
                score -= 8
                status = "warning"
            checks.append({
                "id": "unknown_project_nodes",
                "title": "节点归属完整性",
                "status": status,
                "message": f"存在 {unknown} 个节点未标注 belong_project（占比 {ratio:.1f}%）。",
                "suggestion": "建议复查导入流程，确保节点归属字段正确写入。",
            })
        else:
            checks.append({
                "id": "unknown_project_nodes",
                "title": "节点归属完整性",
                "status": "ok",
                "message": "所有节点都已标注 belong_project。",
                "suggestion": "",
            })

        # 检查项 5：索引健康
        total_idx = _to_int(schema.get("index_total"))
        online_idx = _to_int(schema.get("index_online"))
        failed_idx = _to_int(schema.get("index_failed"))
        if total_idx <= 0:
            score -= 12
            checks.append({
                "id": "schema_indexes",
                "title": "索引健康",
                "status": "warning",
                "message": "未检测到索引。",
                "suggestion": "建议为高频查询字段创建索引，提升查询性能。",
            })
        elif failed_idx > 0:
            score -= 20
            checks.append({
                "id": "schema_indexes",
                "title": "索引健康",
                "status": "warning",
                "message": f"索引总数 {total_idx}，ONLINE={online_idx}，异常/构建中={failed_idx}。",
                "suggestion": "请检查 FAILED/POPULATING 索引状态，确认索引构建是否完成。",
            })
        else:
            checks.append({
                "id": "schema_indexes",
                "title": "索引健康",
                "status": "ok",
                "message": f"索引总数 {total_idx}，全部 ONLINE。",
                "suggestion": "",
            })

    except Exception as e:
        score = max(0, score - 40)
        checks.append({
            "id": "diagnostics_runtime",
            "title": "诊断执行",
            "status": "error",
            "message": f"诊断执行异常：{e}",
            "suggestion": "请检查 Neo4j 查询权限和连接稳定性。",
        })
    finally:
        conn.disconnect()

    score = max(0, min(100, score))
    err_count = len([x for x in checks if x.get("status") == "error"])
    warn_count = len([x for x in checks if x.get("status") == "warning"])
    ok_count = len([x for x in checks if x.get("status") == "ok"])

    return {
        "ok": True,
        "generated_at": datetime.now().isoformat(),
        "score": score,
        "stats": stats,
        "schema": schema,
        "ops": {
            "summary": summarize_neo4j_operations(window_minutes=60),
            "recent": list_neo4j_operations(limit=20),
        },
        "checks": checks,
        "alerts": {"error": err_count, "warning": warn_count, "ok": ok_count},
    }


@router.get("/graph/diagnostics/neo4j-ops")
def graph_diagnostics_neo4j_ops(
    limit: int = Query(100, ge=1, le=500),
    only_errors: bool = Query(False),
    min_elapsed_ms: float = Query(0, ge=0),
) -> Dict[str, Any]:
    return {
        "ok": True,
        "items": list_neo4j_operations(limit=limit, only_errors=only_errors, min_elapsed_ms=min_elapsed_ms),
        "summary_60m": summarize_neo4j_operations(window_minutes=60),
    }

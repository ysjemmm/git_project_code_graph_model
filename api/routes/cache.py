"""
本地 Git 缓存/元数据/Merkle 树管理接口（面向后台管理页面）。
"""
from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Query

from api.config import GIT_CACHE_DIR
from tools.constants import (
    CACHE_ROOT_PATH,
    CACHE_GIT_REPOS_PATH,
    CACHE_MAVEN_PATH,
    CACHE_MAVEN_DEPS_PATH,
)
from api.schemas import (
    CacheProjectItem,
    CacheProjectListResponse,
)
from api.utils import run_git
from api.routes.cache_link_stats import attach_link_stats_to_projects, list_linked_by_items

router = APIRouter(prefix="/api", tags=["cache"])


def _find_child_any_ns(elem: Optional[Any], tag: str) -> Optional[Any]:
    if elem is None:
        return None
    child = elem.find(tag)
    if child is not None:
        return child
    return elem.find(f"{{*}}{tag}")


def _text_path_any_ns(elem: Any, *tags: str) -> str:
    cur = elem
    for tag in tags:
        if cur is None:
            return ""
        cur = _find_child_any_ns(cur, tag)
    if cur is not None and cur.text:
        return str(cur.text).strip()
    return ""

def _scan_application_projects_from_filesystem(*, include_libs: bool) -> List[CacheProjectItem]:
    """
    从本地 `.cache/git_repos` + metadata/merkle 文件扫描应用列表，并转换为 `CacheProjectItem`。

    用于：初始化/刷新业务数据库里的“应用信息（必须落盘）”。
    """
    items: List[CacheProjectItem] = []
    base = GIT_CACHE_DIR
    if not base.exists():
        return items

    for repo_dir in base.iterdir():
        if not repo_dir.is_dir():
            continue

        # 忽略空壳目录（例如删除后残留的空文件夹）
        try:
            children = list(repo_dir.iterdir())
            if not children:
                continue
            if len(children) == 1 and children[0].name == ".git":
                continue
        except Exception:
            pass

        repo_name = repo_dir.name
        md = _read_metadata(repo_name) or {}
        project_type = (md.get("project_type") or "Application") if isinstance(md, dict) else "Application"
        if not include_libs and str(project_type).strip() == "Lib":
            continue

        git_info = _git_repo_info(repo_dir)
        size_mb = None
        try:
            size_mb = _dir_size_bytes(repo_dir) / (1024 * 1024)
        except Exception:
            pass

        repo_url = md.get("repo_url") if isinstance(md, dict) else None
        if not repo_url:
            repo_url = git_info.get("remote_url")

        branch = md.get("branch") if isinstance(md, dict) else None
        commit_hash = md.get("commit_hash") if isinstance(md, dict) else None
        if not branch:
            branch = git_info.get("head_branch")
        if not commit_hash:
            commit_hash = git_info.get("head_commit")

        items.append(
            CacheProjectItem(
                repo_name=repo_name,
                project_name=(md.get("project_name") if isinstance(md, dict) else None) or repo_name,
                project_key=(md.get("project_key") if isinstance(md, dict) else None) or None,
                project_type=str(project_type) if project_type is not None else None,
                repo_url=repo_url,
                branch=branch,
                commit_hash=commit_hash,
                last_update_time=(md.get("last_update_time") if isinstance(md, dict) else None),
                cache_dir=(md.get("cache_dir") if isinstance(md, dict) else None) or str(repo_dir),
                cache_size_mb=size_mb,
                merkle_branches=_list_merkle_branches(repo_name),
                app_type=(md.get("app_type") if isinstance(md, dict) else None) or "backend",
                language=(md.get("language") if isinstance(md, dict) else None) or "java",
                **git_info,
            )
        )

    # 稳定排序：更新时间 desc，其次 repo_name
    def _k(x: CacheProjectItem):
        t = x.last_update_time or ""
        return (t, x.repo_name or "")

    items.sort(key=_k, reverse=True)
    return items


def _has_active_import_tasks(repo_name: str, project_name: str) -> Optional[str]:
    """
    返回阻止删除的原因字符串；若无阻止条件返回 None。
    仅阻止 pending/running 的导入任务（已完成/失败的历史任务不阻止删除元数据）。
    """
    try:
        from core.task_queue import get_task_queue
    except Exception:
        return None
    try:
        q = get_task_queue(max_workers=1, cache_base_dir=str(CACHE_GIT_REPOS_PATH))
        if not q.running:
            q.start()
        tasks = q.get_all_tasks() or []
        active = []
        for t in tasks:
            if not isinstance(t, dict):
                continue
            s = str(t.get("status") or "").lower()
            if s not in ("pending", "running"):
                continue
            rn = str(t.get("repo_name") or "").strip()
            pn = str(t.get("project_name") or "").strip()
            if rn == repo_name or pn == project_name:
                active.append(str(t.get("task_id") or "").strip() or "?")
        if active:
            return f"存在未结束的导入任务（{len(active)} 个）：{', '.join(active[:8])}{'...' if len(active) > 8 else ''}。请先在「导入/重建」页终止或等待任务结束后再删除。"
    except Exception:
        return None
    return None

def _rmtree_force(path: Path) -> None:
    """
    Windows 上删除包含只读文件/隐藏目录时，shutil.rmtree + ignore_errors 往往会“假成功”。
    这里用 onerror 去掉只读位并重试，若仍失败则抛异常让接口返回失败。
    """
    import shutil
    import stat

    def _onerror(func, p, exc_info):
        try:
            os.chmod(p, stat.S_IWRITE)
            func(p)
        except Exception:
            raise

    shutil.rmtree(path, onerror=_onerror)


def _cache_root() -> Path:
    # .cache 目录
    return CACHE_ROOT_PATH


def _metadata_dir() -> Path:
    return _cache_root() / "metadata"


def _merkle_dir() -> Path:
    return _cache_root() / "merkle_trees"


def _safe_branch_from_merkle_filename(name: str, repo_name: str) -> Optional[str]:
    # <repo>_<safe_branch>.json
    prefix = f"{repo_name}_"
    if not name.startswith(prefix) or not name.endswith(".json"):
        return None
    safe = name[len(prefix) : -len(".json")]
    # safe_branch = branch.replace('/', '_')，这里只能还原成“展示用”
    return safe.replace("_", "/")


def _list_merkle_branches(repo_name: str) -> List[str]:
    d = _merkle_dir()
    if not d.exists():
        return []
    out: List[str] = []
    for p in d.iterdir():
        if not p.is_file():
            continue
        b = _safe_branch_from_merkle_filename(p.name, repo_name)
        if b:
            out.append(b)
    # 去重
    seen = set()
    uniq = []
    for x in out:
        if x in seen:
            continue
        seen.add(x)
        uniq.append(x)
    return uniq


def _read_metadata(repo_name: str) -> Optional[Dict[str, Any]]:
    path = _metadata_dir() / f"{repo_name}.json"
    if not path.is_file():
        return None
    try:
        import json

        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None


def _dir_size_bytes(path: Path) -> int:
    total = 0
    if not path.exists():
        return 0
    for root, _, files in os.walk(str(path)):
        for f in files:
            fp = Path(root) / f
            try:
                total += fp.stat().st_size
            except OSError:
                pass
    return total


def _git_repo_info(repo_dir: Path) -> Dict[str, Any]:
    if not repo_dir.is_dir():
        return {
            "repo_exists": False,
            "remote_url": None,
            "head_branch": None,
            "head_commit": None,
            "dirty": None,
        }
    remote = (run_git(repo_dir, ["config", "--get", "remote.origin.url"]) or [None])[0]
    # 处于 detached HEAD 时会返回 "HEAD"，这里额外尝试取远端分支名（若存在）
    head_branch = (run_git(repo_dir, ["rev-parse", "--abbrev-ref", "HEAD"]) or [None])[0]
    if head_branch == "HEAD":
        maybe = (run_git(repo_dir, ["name-rev", "--name-only", "HEAD"]) or [None])[0]
        if maybe and maybe != "undefined":
            head_branch = maybe
    head_commit = (run_git(repo_dir, ["rev-parse", "HEAD"]) or [None])[0]
    dirty = True
    try:
        s = run_git(repo_dir, ["status", "--porcelain"])
        dirty = bool(s)
    except Exception:
        dirty = None
    return {
        "repo_exists": True,
        "remote_url": remote,
        "head_branch": head_branch,
        "head_commit": head_commit,
        "dirty": dirty,
    }


@router.get("/cache/application-projects")
def cache_application_projects(
    include_libs: bool = Query(False, description="是否包含 Lib 项目（默认只返回 Application）"),
    refresh: bool = Query(False, description="是否强制从本地缓存重新扫描并写入数据库（再返回）"),
    java_only: bool = Query(False, description="只返回后端 Java 应用（用于图谱导入场景）"),
) -> CacheProjectListResponse:
    # 应用信息必须落盘数据库：DB 优先，只有当 DB 空或 refresh=true 时才扫描本地。
    try:
        from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

        db = get_business_db()
        repo = ApplicationProjectsRepo(db=db)
        db_count = repo.count()

        if not refresh and db_count > 0:
            return CacheProjectListResponse(
                items=attach_link_stats_to_projects(db, repo.list_all(include_libs=include_libs, java_only=java_only))
            )

        scanned = _scan_application_projects_from_filesystem(include_libs=include_libs)
        repo.upsert_many(scanned)
        return CacheProjectListResponse(
            items=attach_link_stats_to_projects(db, repo.list_all(include_libs=include_libs, java_only=java_only))
        )
    except Exception as e:
        # 尽量仍尝试把扫描结果落盘；若也失败，直接让接口报错，避免"看到了但其实没落盘"
        scanned = _scan_application_projects_from_filesystem(include_libs=include_libs)
        try:
            from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

            db = get_business_db()
            repo = ApplicationProjectsRepo(db=db)
            repo.upsert_many(scanned)
            items = attach_link_stats_to_projects(db, repo.list_all(include_libs=include_libs, java_only=java_only))
            return CacheProjectListResponse(items=items)
        except Exception:
            # 兜底失败：抛出原错误 + 扫描结果无法落盘
            raise RuntimeError(f"应用信息落盘数据库失败：{e}")



@router.get("/cache/application-projects/{app_id}/linked-by")
def list_application_linked_by(app_id: int) -> Dict[str, Any]:
    """
    查询“哪些项目关联到了当前项目”（入向关联）。
    """
    from storage.sqlite.business import get_business_db
    db = get_business_db()
    cur = db.conn.cursor()
    exists = cur.execute("SELECT id FROM application_projects_cache WHERE id = ?", (app_id,)).fetchone()
    if exists is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    items = list_linked_by_items(db, app_id)
    return {"ok": True, "app_id": app_id, "items": items}


def _load_merkle_tree(repo_name: str, branch: str) -> Optional[Dict[str, Any]]:
    # 文件名使用 safe_branch（与 GitCacheManager 一致）
    safe_branch = (branch or "").replace("/", "_")
    path = _merkle_dir() / f"{repo_name}_{safe_branch}.json"
    if not path.is_file():
        return None
    try:
        import json

        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None


def _merkle_stats(node: Dict[str, Any], depth: int = 0) -> tuple[int, int, int, int]:
    # (nodes, files, dirs, max_depth)
    nodes = 1
    is_file = bool(node.get("is_file"))
    files = 1 if is_file else 0
    dirs = 0 if is_file else 1
    max_depth = depth
    for ch in node.get("children") or []:
        n2, f2, d2, md2 = _merkle_stats(ch, depth + 1)
        nodes += n2
        files += f2
        dirs += d2
        max_depth = max(max_depth, md2)
    return nodes, files, dirs, max_depth


def _truncate_merkle(node: Dict[str, Any], *, max_depth: int, max_children: int, depth: int = 0) -> Dict[str, Any]:
    out = {k: node.get(k) for k in ("path", "hash", "is_file")}
    children = node.get("children") or []
    if depth >= max_depth or not children:
        out["children"] = []
        if children:
            out["children_truncated"] = True
            out["children_total"] = len(children)
        return out
    cut = children[:max_children]
    out["children"] = [
        _truncate_merkle(ch, max_depth=max_depth, max_children=max_children, depth=depth + 1)
        for ch in cut
        if isinstance(ch, dict)
    ]
    if len(children) > len(cut):
        out["children_truncated"] = True
        out["children_total"] = len(children)
    return out




@router.post("/cache/application-projects")
def create_application(body: Dict[str, Any]) -> Dict[str, Any]:
    """
    新建应用（仅登记信息，不发起导入任务）。
    body: { project_name, app_type, language, repo_url, maven_scan_enabled, force_maven, clear_database, auto_link_external }
    """
    from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

    project_name       = (body.get("project_name") or "").strip()
    app_type           = (body.get("app_type") or "backend").strip()
    language           = (body.get("language") or "java").strip()
    repo_url           = (body.get("repo_url") or "").strip()
    maven_scan_enabled = 1 if body.get("maven_scan_enabled", True) else 0
    force_maven        = 1 if body.get("force_maven", False) else 0
    clear_database     = 1 if body.get("clear_database", False) else 0
    auto_link_external = 1 if body.get("auto_link_external", False) else 0

    if not project_name:
        return {"ok": False, "message": "应用名称不能为空"}
    if not repo_url:
        return {"ok": False, "message": "Git 仓库地址不能为空"}

    db = get_business_db()

    # 查重
    dup = db.conn.execute(
        "SELECT id FROM application_projects_cache WHERE project_name = ?", (project_name,)
    ).fetchone()
    if dup:
        return {"ok": False, "message": f"应用名称 '{project_name}' 已存在"}

    try:
        db.conn.execute(
            """INSERT INTO application_projects_cache
               (repo_name, project_name, project_type, repo_url, app_type, language,
                maven_scan_enabled, force_maven, clear_database, auto_link_external,
                created_at, updated_at)
               VALUES (?, ?, 'Application', ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))""",
            (project_name, project_name, repo_url, app_type, language,
             maven_scan_enabled, force_maven, clear_database, auto_link_external),
        )
        db.conn.commit()
    except Exception as e:
        return {"ok": False, "message": f"写入失败：{e}"}

    row = db.conn.execute(
        "SELECT id FROM application_projects_cache WHERE project_name = ?", (project_name,)
    ).fetchone()
    return {"ok": True, "id": row["id"] if row else None}


@router.patch("/cache/application-projects/{app_id}/import-settings")
def update_app_import_settings(app_id: int, body: Dict[str, Any]) -> Dict[str, Any]:
    """更新应用的导入配置（maven_scan_enabled / force_maven / clear_database）。"""
    from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

    db = get_business_db()
    repo = ApplicationProjectsRepo(db=db)

    maven_scan_enabled = body.get("maven_scan_enabled")
    force_maven = body.get("force_maven")
    clear_database = body.get("clear_database")
    auto_link_external = body.get("auto_link_external")

    updated = repo.update_import_settings(
        app_id,
        maven_scan_enabled=None if maven_scan_enabled is None else bool(maven_scan_enabled),
        force_maven=None if force_maven is None else bool(force_maven),
        clear_database=None if clear_database is None else bool(clear_database),
        auto_link_external=None if auto_link_external is None else bool(auto_link_external),
    )
    if not updated:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    settings = repo.get_import_settings(app_id)
    return {"ok": True, "app_id": app_id, "settings": settings}


@router.get("/cache/application-projects/{app_id}/import-settings")
def get_app_import_settings(app_id: int) -> Dict[str, Any]:
    """获取应用的导入配置。"""
    from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

    db = get_business_db()
    repo = ApplicationProjectsRepo(db=db)
    settings = repo.get_import_settings(app_id)
    if settings is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")
    return {"ok": True, "app_id": app_id, "settings": settings}


@router.get("/cache/application-projects/{app_id}/can-delete")
def can_delete_application(app_id: int) -> Dict[str, Any]:
    """删除前检查：是否存在进行中的导入任务。"""
    from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

    db = get_business_db()
    repo = ApplicationProjectsRepo(db=db)
    settings = repo.get_import_settings(app_id)
    if settings is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    # 取 repo_name / project_name 用于任务检查
    cur = db.conn.cursor()
    row = cur.execute(
        "SELECT repo_name, project_name FROM application_projects_cache WHERE id = ?", (app_id,)
    ).fetchone()
    if row is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    block = _has_active_import_tasks(str(row["repo_name"] or ""), str(row["project_name"] or ""))
    return {"ok": block is None, "block_reason": block}


@router.delete("/cache/application-projects/{app_id}")
def delete_application(app_id: int) -> Dict[str, Any]:
    """
    删除应用：
    1. 检查是否有进行中的导入任务（有则拒绝）
    2. 删除 Neo4j 图谱子图
    3. 删除本地 Git 缓存目录、元数据、Merkle 树文件
    4. 删除数据库记录
    """
    from fastapi import HTTPException
    from storage.sqlite.business import get_business_db, ApplicationProjectsRepo

    db = get_business_db()
    app_repo = ApplicationProjectsRepo(db=db)

    cur = db.conn.cursor()
    row = cur.execute(
        "SELECT id, repo_name, project_name, project_key FROM application_projects_cache WHERE id = ?",
        (app_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    repo_name = str(row["repo_name"] or "")
    project_name = str(row["project_name"] or "") or repo_name
    project_key = str(row["project_key"] or "") or None

    # 1. 阻止删除检查
    block = _has_active_import_tasks(repo_name, project_name)
    if block:
        raise HTTPException(status_code=409, detail=block)

    result: Dict[str, Any] = {
        "ok": True,
        "app_id": app_id,
        "repo_name": repo_name,
        "deleted_graph_nodes": 0,
        "last_application_removed": False,
        "cleared_all_relationships": False,
        "deleted_lib_nodes": 0,
        "cache_deleted": False,
        "db_deleted": False,
        "errors": [],
    }

    # 2. 删除 Neo4j 图谱
    try:
        import os as _os
        uri = _os.environ.get("NEO4J_URI", "")
        password = _os.environ.get("NEO4J_PASSWORD", "")
        if uri and password:
            from storage.neo4j.connector import Neo4jConnector
            user = _os.environ.get("NEO4J_USER", "neo4j")
            database = _os.environ.get("NEO4J_DATABASE", "neo4j")
            conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
            if conn.connect():
                deleted = conn.delete_project_data(project_name, project_key=project_key)
                result["deleted_graph_nodes"] = deleted
    except Exception as e:
        result["errors"].append(f"图谱删除失败: {e}")

    # 3. 删除本地缓存（git 目录 + metadata + merkle）
    try:
        from storage.cache.git_cache import GitCacheManager
        mgr = GitCacheManager(cache_base_dir=str(CACHE_GIT_REPOS_PATH))
        result["cache_deleted"] = mgr.cleanup_repo(repo_name)
    except Exception as e:
        result["errors"].append(f"缓存删除失败: {e}")

    # 4. 删除数据库记录
    try:
        app_repo.delete_by_id(app_id)
        result["db_deleted"] = True
    except Exception as e:
        result["errors"].append(f"数据库记录删除失败: {e}")

    # 5. 若删除的是最后一个 Application：清理所有关系 + 所有 Lib 节点
    try:
        if result["db_deleted"]:
            cur2 = db.conn.cursor()
            row2 = cur2.execute(
                """
                SELECT count(*) AS c
                FROM application_projects_cache
                WHERE lower(coalesce(project_type, 'Application')) = 'application'
                """
            ).fetchone()
            remaining_apps = int(row2["c"] if row2 else 0)
            if remaining_apps == 0:
                result["last_application_removed"] = True
                import os as _os
                uri = _os.environ.get("NEO4J_URI", "")
                password = _os.environ.get("NEO4J_PASSWORD", "")
                if uri and password:
                    from storage.neo4j.connector import Neo4jConnector
                    user = _os.environ.get("NEO4J_USER", "neo4j")
                    database = _os.environ.get("NEO4J_DATABASE", "neo4j")
                    conn2 = Neo4jConnector(uri=uri, username=user, password=password, database=database)
                    if conn2.connect():
                        conn2.execute_write_query("MATCH ()-[r]-() DELETE r")
                        result["cleared_all_relationships"] = True
                        deleted_lib = conn2.execute_write_query(
                            """
                            MATCH (p:Project {project_type: 'Lib'})
                            WITH collect(p) AS libs
                            FOREACH (n IN libs | DELETE n)
                            RETURN size(libs) AS deleted_count
                            """
                        )
                        result["deleted_lib_nodes"] = int(
                            deleted_lib[0].get("deleted_count", 0) if deleted_lib else 0
                        )
    except Exception as e:
        result["errors"].append(f"最后一个应用清理失败: {e}")

    if result["errors"]:
        result["ok"] = False

    return result


@router.get("/cache/application-projects/{app_id}/dependencies")
def get_app_dependencies(app_id: int) -> Dict[str, Any]:
    """获取应用的 pom 依赖列表（从数据库读取，需先调用 refresh-dependencies 扫描）。"""
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependencies_repo import AppDependenciesRepo

    db = get_business_db()
    # 检查 app 存在
    row = db.conn.cursor().execute(
        "SELECT id FROM application_projects_cache WHERE id = ?", (app_id,)
    ).fetchone()
    if row is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    dep_repo = AppDependenciesRepo(db=db)
    items = dep_repo.list_by_app(app_id)
    scanned_at = dep_repo.scanned_at(app_id)
    return {"ok": True, "app_id": app_id, "scanned_at": scanned_at, "items": items}


@router.get("/cache/application-projects/{app_id}/maven-info")
def get_app_maven_info(app_id: int) -> Dict[str, Any]:
    """
    读取应用根 pom.xml 的 Maven 坐标信息（groupId/artifactId/version/parent）。
    """
    import xml.etree.ElementTree as _ET
    from fastapi import HTTPException
    from storage.sqlite.business import get_business_db

    db = get_business_db()
    row = db.conn.cursor().execute(
        "SELECT id, repo_name, cache_dir FROM application_projects_cache WHERE id = ?",
        (app_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    repo_name = str(row["repo_name"] or "")
    cache_dir = str(row["cache_dir"] or "") or str(CACHE_GIT_REPOS_PATH / repo_name)
    repo_root = Path(cache_dir)
    pom_path = repo_root / "pom.xml"
    if not pom_path.exists():
        raise HTTPException(status_code=422, detail=f"未找到 pom.xml：{pom_path}")

    try:
        root = _ET.parse(str(pom_path)).getroot()
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"解析 pom.xml 失败：{e}")

    parent_group_id = _text_path_any_ns(root, "parent", "groupId")
    parent_artifact_id = _text_path_any_ns(root, "parent", "artifactId")
    parent_version = _text_path_any_ns(root, "parent", "version")
    group_id = _text_path_any_ns(root, "groupId") or parent_group_id
    artifact_id = _text_path_any_ns(root, "artifactId")
    version = _text_path_any_ns(root, "version") or parent_version
    packaging = _text_path_any_ns(root, "packaging") or "jar"

    return {
        "ok": True,
        "app_id": app_id,
        "maven": {
            "group_id": group_id or "",
            "artifact_id": artifact_id or "",
            "version": version or "",
            "packaging": packaging or "",
            "parent_group_id": parent_group_id or "",
            "parent_artifact_id": parent_artifact_id or "",
            "parent_version": parent_version or "",
            "pom_path": str(pom_path),
        },
    }


@router.get("/cache/application-projects/{app_id}/dependencies-tree")
def get_app_dependencies_tree(
    app_id: int,
    scope: str = Query("all", description="all/compile/test/provided/runtime"),
    second_only: bool = Query(False, description="是否只返回二方包"),
) -> Dict[str, Any]:
    """
    返回后端已分组的 parent-tree 依赖结构，前端直接渲染，避免大规模计算导致卡顿。
    """
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependencies_repo import AppDependenciesRepo
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    db = get_business_db()
    row = db.conn.cursor().execute(
        "SELECT id FROM application_projects_cache WHERE id = ?", (app_id,)
    ).fetchone()
    if row is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    dep_repo = AppDependenciesRepo(db=db)
    all_items = dep_repo.list_by_app(app_id)
    scanned_at = dep_repo.scanned_at(app_id)
    total_count = len(all_items)
    second_party_count = sum(1 for x in all_items if bool(x.get("is_second_party")))
    third_party_count = max(0, total_count - second_party_count)

    scope_norm = str(scope or "all").strip().lower()
    if scope_norm not in {"all", "compile", "test", "provided", "runtime"}:
        scope_norm = "all"
    filtered = all_items
    if scope_norm != "all":
        filtered = [x for x in filtered if str(x.get("scope") or "").strip().lower() == scope_norm]
    if bool(second_only):
        filtered = [x for x in filtered if bool(x.get("is_second_party"))]

    links_repo = AppDependencyLinksRepo(db=db)
    links = links_repo.list_by_app(app_id)
    link_map: Dict[str, Dict[str, Any]] = {
        f"{str(l.get('group_id') or '')}::{str(l.get('artifact_id') or '')}": l for l in links
    }

    DEFAULT_PARENT_LABEL = "default"
    grouped: Dict[str, List[Dict[str, Any]]] = {}
    for d in filtered:
        pg = str(d.get("parent_group_id") or "").strip()
        pa = str(d.get("parent_artifact_id") or "").strip()
        pv = str(d.get("parent_version") or "").strip()
        parent = f"{pg}:{pa}{(':' + pv) if pv else ''}" if pg and pa else DEFAULT_PARENT_LABEL

        group_id = str(d.get("group_id") or "")
        artifact_id = str(d.get("artifact_id") or "")
        linked = link_map.get(f"{group_id}::{artifact_id}")
        child = {
            **d,
            "key": f"dep::{group_id}:{artifact_id}:{str(d.get('scope') or '')}:{pg}:{pa}:{pv}",
            "__is_parent_group": False,
            "parent": parent,
            "linked": linked,
            "locked_by_parent": False,
        }
        grouped.setdefault(parent, []).append(child)

    rows: List[Dict[str, Any]] = []
    for parent, children in grouped.items():
        children.sort(
            key=lambda x: (
                str(x.get("artifact_id") or ""),
                str(x.get("group_id") or ""),
                str(x.get("version") or ""),
                str(x.get("scope") or ""),
            )
        )

        second_count = sum(1 for x in children if bool(x.get("is_second_party")))
        child_links = [x.get("linked") for x in children if x.get("linked")]
        linked_ids = {int(x.get("linked_app_id")) for x in child_links if x and x.get("linked_app_id") is not None}
        uniform = len(children) > 0 and len(child_links) == len(children) and len(linked_ids) == 1
        uniform_link = child_links[0] if uniform and child_links else None
        parent_locked = bool(parent != DEFAULT_PARENT_LABEL and len(children) > 0 and second_count == len(children) and uniform)
        if parent_locked:
            for c in children:
                c["locked_by_parent"] = True

        parts = parent.split(":")
        parent_group_id = parts[0] if len(parts) >= 1 and parent != DEFAULT_PARENT_LABEL else ""
        parent_artifact_id = parts[1] if len(parts) >= 2 and parent != DEFAULT_PARENT_LABEL else ""
        parent_version = ":".join(parts[2:]) if len(parts) >= 3 and parent != DEFAULT_PARENT_LABEL else ""
        rows.append(
            {
                "key": f"parent::{parent}",
                "__is_parent_group": True,
                "parent": parent,
                "parent_group_id": parent_group_id,
                "parent_artifact_id": parent_artifact_id,
                "parent_version": parent_version,
                "child_count": len(children),
                "second_party_count": second_count,
                "uniform_link": uniform_link,
                "parent_locked": parent_locked,
                "children": children,
            }
        )

    rows.sort(
        key=lambda x: (
            0 if int(x.get("second_party_count") or 0) > 0 else 1,
            1 if str(x.get("parent") or "") == DEFAULT_PARENT_LABEL else 0,
            str(x.get("parent") or ""),
        )
    )

    return {
        "ok": True,
        "app_id": app_id,
        "scanned_at": scanned_at,
        "scope": scope_norm,
        "second_only": bool(second_only),
        "total_count": total_count,
        "second_party_count": second_party_count,
        "third_party_count": third_party_count,
        "filtered_count": len(filtered),
        "items": rows,
    }


@router.post("/cache/application-projects/{app_id}/refresh-dependencies")
def refresh_app_dependencies(app_id: int) -> Dict[str, Any]:
    """
    重新解析该应用本地 pom.xml，将依赖写入数据库。
    需要本地 Git 缓存目录存在且包含 pom.xml。
    """
    import re as _re
    import xml.etree.ElementTree as _ET
    from fastapi import HTTPException
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependencies_repo import AppDependenciesRepo
    from core.pom_dependency_parser import parse_project_dependencies

    db = get_business_db()
    row = db.conn.cursor().execute(
        "SELECT id, repo_name, project_name, cache_dir, maven_scan_enabled FROM application_projects_cache WHERE id = ?",
        (app_id,),
    ).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    repo_name = str(row["repo_name"] or "")
    cache_dir = str(row["cache_dir"] or "")
    if not cache_dir:
        cache_dir = str(CACHE_GIT_REPOS_PATH / repo_name)

    repo_root = Path(cache_dir)
    if not repo_root.exists():
        raise HTTPException(status_code=422, detail=f"本地缓存目录不存在：{cache_dir}")

    pom_path = repo_root / "pom.xml"
    if not pom_path.exists():
        raise HTTPException(status_code=422, detail=f"未找到 pom.xml，该项目可能不是 Maven 项目：{cache_dir}")

    # 解析依赖
    raw_deps = parse_project_dependencies(str(repo_root))

    # 加载二方包规则并标记
    rules: list = []
    try:
        from storage.sqlite.business.second_party_rules_repo import SecondPartyRulesRepo
        rules = [r for r in SecondPartyRulesRepo(db=db).list_rules() if r.get("enabled")]
    except Exception:
        pass

    def _match_rule(group_id: str, artifact_id: str) -> Optional[int]:
        """返回第一条匹配规则的 id，未匹配返回 None。"""
        for rule in rules:
            try:
                if _re.search(rule["group_id_regex"], group_id) and \
                   _re.search(rule["artifact_id_regex"], artifact_id):
                    return rule["id"]
            except Exception:
                pass
        return None

    def _match_rule_with_parent(
        group_id: str,
        artifact_id: str,
        parent_group_id: str,
        parent_artifact_id: str,
    ) -> Optional[int]:
        """
        命中顺序：
        1) 依赖自身坐标（group_id/artifact_id）
        2) 依赖 parent 坐标（parent_group_id/parent_artifact_id）
        """
        rid = _match_rule(group_id, artifact_id)
        if rid is not None:
            return rid
        if parent_group_id and parent_artifact_id:
            return _match_rule(parent_group_id, parent_artifact_id)
        return None

    def _find_child_any_ns(elem: Optional[_ET.Element], tag: str) -> Optional[_ET.Element]:
        if elem is None:
            return None
        child = elem.find(tag)
        if child is not None:
            return child
        return elem.find(f"{{*}}{tag}")

    def _text_path_any_ns(elem: _ET.Element, *tags: str) -> str:
        cur = elem
        for tag in tags:
            if cur is None:
                return ""
            cur = _find_child_any_ns(cur, tag)
        if cur is not None and cur.text:
            return str(cur.text).strip()
        return ""

    parent_from_pom_cache: Dict[str, tuple[str, str, str]] = {}
    source_parent_index_loaded = False
    source_parent_index: Dict[str, tuple[str, str, str]] = {}

    def _load_source_parent_index_once() -> None:
        nonlocal source_parent_index_loaded
        if source_parent_index_loaded:
            return
        source_parent_index_loaded = True
        try:
            rows = db.conn.cursor().execute(
                """
                SELECT cache_dir
                FROM application_projects_cache
                WHERE cache_dir IS NOT NULL AND TRIM(cache_dir) != ''
                """
            ).fetchall()
        except Exception:
            rows = []

        def _index_pom(pom_file: Path) -> None:
            try:
                root = _ET.parse(str(pom_file)).getroot()
                parent_group = _text_path_any_ns(root, "parent", "groupId")
                parent_artifact = _text_path_any_ns(root, "parent", "artifactId")
                parent_version = _text_path_any_ns(root, "parent", "version")
                group_id = _text_path_any_ns(root, "groupId") or parent_group
                artifact_id = _text_path_any_ns(root, "artifactId")
                version = _text_path_any_ns(root, "version") or parent_version
                if not group_id or not artifact_id or not parent_group or not parent_artifact:
                    return
                if version:
                    source_parent_index[f"{group_id}:{artifact_id}:{version}"] = (
                        parent_group,
                        parent_artifact,
                        parent_version,
                    )
                source_parent_index.setdefault(
                    f"{group_id}:{artifact_id}",
                    (parent_group, parent_artifact, parent_version),
                )
            except Exception:
                return

        for row_item in rows:
            cache_dir = str(row_item["cache_dir"] or "").strip()
            if not cache_dir:
                continue
            repo_root_path = Path(cache_dir)
            if not repo_root_path.exists() or not repo_root_path.is_dir():
                continue
            try:
                for pom_file in repo_root_path.rglob("pom.xml"):
                    if "target" in pom_file.parts:
                        continue
                    _index_pom(pom_file)
            except Exception:
                continue

    def _resolve_dep_parent_from_local_pom(group_id: str, artifact_id: str, version: str) -> tuple[str, str, str]:
        """
        从本地 Maven 缓存里的“依赖自身 pom”读取 parent。
        优先级：
        1) .cache/maven/<group>/<artifact>/<version>/<artifact>-<version>.pom
        2) 上述目录内任意 <artifact>-*.pom（SNAPSHOT 时间戳兜底）
        3) .cache/maven_deps/<repo_name>/<artifact>-<version>.pom
        4) 已缓存应用源码仓库中的对应 pom.xml（按 group/artifact/version 匹配）

        兼容：
        - 当 version 缺失时，改为扫描 maven/maven_deps 下该 artifact 的所有 pom 候选。
        """
        key = f"{group_id}:{artifact_id}:{version}"
        if key in parent_from_pom_cache:
            return parent_from_pom_cache[key]

        candidates: List[Path] = []
        gpath = Path(str(group_id).replace(".", "/"))
        if artifact_id and version:
            mvn_dir = CACHE_MAVEN_PATH / gpath / artifact_id / version
            candidates.append(mvn_dir / f"{artifact_id}-{version}.pom")
            if mvn_dir.exists():
                # SNAPSHOT 可能是带时间戳的 pom，按修改时间倒序兜底
                snapshot_poms = sorted(
                    [p for p in mvn_dir.glob(f"{artifact_id}-*.pom") if p.is_file()],
                    key=lambda p: p.stat().st_mtime,
                    reverse=True,
                )
                candidates.extend(snapshot_poms)

            candidates.append(CACHE_MAVEN_DEPS_PATH / repo_name / f"{artifact_id}-{version}.pom")
        elif artifact_id:
            # version 缺失：退化为同 artifact 的全量候选扫描（按修改时间倒序）
            mvn_artifact_dir = CACHE_MAVEN_PATH / gpath / artifact_id
            if mvn_artifact_dir.exists():
                try:
                    version_poms = sorted(
                        [p for p in mvn_artifact_dir.glob(f"*/{artifact_id}-*.pom") if p.is_file()],
                        key=lambda p: p.stat().st_mtime,
                        reverse=True,
                    )
                    candidates.extend(version_poms)
                except Exception:
                    pass
            mvn_deps_repo_dir = CACHE_MAVEN_DEPS_PATH / repo_name
            if mvn_deps_repo_dir.exists():
                try:
                    repo_poms = sorted(
                        [p for p in mvn_deps_repo_dir.glob(f"{artifact_id}-*.pom") if p.is_file()],
                        key=lambda p: p.stat().st_mtime,
                        reverse=True,
                    )
                    candidates.extend(repo_poms)
                except Exception:
                    pass

        for pom in candidates:
            try:
                if not pom.exists():
                    continue
                root = _ET.parse(str(pom)).getroot()
                pg = _text_path_any_ns(root, "parent", "groupId")
                pa = _text_path_any_ns(root, "parent", "artifactId")
                pv = _text_path_any_ns(root, "parent", "version")
                if pg and pa:
                    parent_from_pom_cache[key] = (pg, pa, pv)
                    return parent_from_pom_cache[key]
            except Exception:
                continue

        # maven 缓存找不到时，回退到“应用源码仓库中的实际 pom.xml”
        _load_source_parent_index_once()
        if key in source_parent_index:
            parent_from_pom_cache[key] = source_parent_index[key]
            return parent_from_pom_cache[key]
        ga_key = f"{group_id}:{artifact_id}"
        if ga_key in source_parent_index:
            parent_from_pom_cache[key] = source_parent_index[ga_key]
            return parent_from_pom_cache[key]

        parent_from_pom_cache[key] = ("", "", "")
        return parent_from_pom_cache[key]

    def _is_parent_like_dep(dep: Dict[str, Any]) -> bool:
        """
        判断是否“父级/BOM/依赖管理”依赖，用于二方包归类扩散。
        - scope=import 常见于 BOM
        - artifact 命名含 parent/bom/dependencies 等
        """
        artifact = str(dep.get("artifact_id") or "").lower()
        scope = str(dep.get("scope") or "").lower()
        if scope == "import":
            return True
        return artifact.endswith("-parent") or artifact.endswith("-bom") or artifact.endswith("-dependencies")

    deps_to_save = []
    for d in raw_deps:
        # 仅以“依赖自身 pom”的 parent 作为 parent 分组来源。
        # 不再回退使用“当前项目/模块的 parent”，避免 fastjson 等三方依赖被错误归到业务 parent。
        pg, pa, pv = "", "", ""
        lg, la, lv = _resolve_dep_parent_from_local_pom(d.group_id, d.artifact_id, d.version)
        if lg and la:
            pg, pa, pv = lg, la, lv
        rule_id = _match_rule_with_parent(d.group_id, d.artifact_id, pg, pa)
        deps_to_save.append({
            "group_id": d.group_id,
            "artifact_id": d.artifact_id,
            "version": d.version,
            "scope": d.scope,
            "parent_group_id": pg,
            "parent_artifact_id": pa,
            "parent_version": pv,
            "is_second_party": rule_id is not None,
            "matched_rule_id": rule_id,
        })

    # 二次分类：若某个“父级/BOM 依赖”命中二方包，则同 group 且同版本的依赖也按二方包处理
    parent_hits = [
        x for x in deps_to_save
        if x.get("is_second_party") and x.get("matched_rule_id") is not None and _is_parent_like_dep(x)
    ]
    parent_classified = 0
    if parent_hits:
        for x in deps_to_save:
            if x.get("is_second_party"):
                continue
            g = str(x.get("group_id") or "")
            v = str(x.get("version") or "")
            for p in parent_hits:
                if str(p.get("group_id") or "") != g:
                    continue
                # 同 group 且版本一致（版本缺失时允许按 group 回退）
                pv = str(p.get("version") or "")
                if v and pv and v != pv:
                    continue
                x["is_second_party"] = True
                x["matched_rule_id"] = p.get("matched_rule_id")
                parent_classified += 1
                break

    dep_repo = AppDependenciesRepo(db=db)
    dep_repo.replace_all(app_id, deps_to_save)

    return {
        "ok": True,
        "app_id": app_id,
        "total": len(deps_to_save),
        "second_party_count": sum(1 for d in deps_to_save if d["is_second_party"]),
        "parent_classified_count": parent_classified,
    }


# ─── 二方包依赖关联（手动） ────────────────────────────────────────────────────

@router.get("/cache/application-projects/{app_id}/dependency-links")
def list_dependency_links(app_id: int) -> Dict[str, Any]:
    """列出项目 A 手动关联的二方包依赖项目。"""
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    db = get_business_db()
    row = db.conn.cursor().execute(
        "SELECT id FROM application_projects_cache WHERE id = ?", (app_id,)
    ).fetchone()
    if row is None:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail=f"应用 id={app_id} 不存在")

    repo = AppDependencyLinksRepo(db=db)
    return {"ok": True, "app_id": app_id, "items": repo.list_by_app(app_id)}


def _is_dep_locked_by_parent_group(db: Any, app_id: int, group_id: str, artifact_id: str) -> bool:
    """
    判断某条依赖是否处于“整组关联锁定”状态：
    - 该依赖属于某个非空 parent 组；
    - 该 parent 组下依赖全部是二方包；
    - 该 parent 组下每个子依赖都已有关联，且 linked_app_id 完全一致。
    """
    cur = db.conn.cursor()
    dep_row = cur.execute(
        """
        SELECT parent_group_id, parent_artifact_id, parent_version
        FROM application_dependencies
        WHERE app_id = ? AND group_id = ? AND artifact_id = ?
        ORDER BY CASE WHEN parent_group_id != '' AND parent_artifact_id != '' THEN 0 ELSE 1 END, id DESC
        LIMIT 1
        """,
        (app_id, group_id, artifact_id),
    ).fetchone()
    if dep_row is None:
        return False

    pg = str(dep_row["parent_group_id"] or "").strip()
    pa = str(dep_row["parent_artifact_id"] or "").strip()
    pv = str(dep_row["parent_version"] or "").strip()
    if not pg or not pa:
        return False

    if pv:
        rows = cur.execute(
            """
            SELECT d.group_id, d.artifact_id, MAX(d.is_second_party) AS is_second_party, l.linked_app_id
            FROM application_dependencies d
            LEFT JOIN app_dependency_links l
              ON l.app_id = d.app_id AND l.group_id = d.group_id AND l.artifact_id = d.artifact_id
            WHERE d.app_id = ?
              AND d.parent_group_id = ?
              AND d.parent_artifact_id = ?
              AND d.parent_version = ?
            GROUP BY d.group_id, d.artifact_id, l.linked_app_id
            """,
            (app_id, pg, pa, pv),
        ).fetchall()
    else:
        rows = cur.execute(
            """
            SELECT d.group_id, d.artifact_id, MAX(d.is_second_party) AS is_second_party, l.linked_app_id
            FROM application_dependencies d
            LEFT JOIN app_dependency_links l
              ON l.app_id = d.app_id AND l.group_id = d.group_id AND l.artifact_id = d.artifact_id
            WHERE d.app_id = ?
              AND d.parent_group_id = ?
              AND d.parent_artifact_id = ?
            GROUP BY d.group_id, d.artifact_id, l.linked_app_id
            """,
            (app_id, pg, pa),
        ).fetchall()

    if not rows:
        return False

    # 1) 该 parent 组必须全是二方包
    if any(not bool(r["is_second_party"]) for r in rows):
        return False

    # 2) 每个子依赖都必须有链接，且 linked_app_id 必须一致
    linked_ids = [r["linked_app_id"] for r in rows]
    if any(x is None for x in linked_ids):
        return False
    distinct_ids = {int(x) for x in linked_ids if x is not None}
    return len(distinct_ids) == 1


@router.put("/cache/application-projects/{app_id}/dependency-links")
def upsert_dependency_link(app_id: int, body: Dict[str, Any]) -> Dict[str, Any]:
    """
    新增或更新一条手动关联：
    body: { group_id, artifact_id, linked_app_id, note? }
    保存后立即同步一条 DEPENDS_ON 边到 Neo4j（幂等）。
    """
    from fastapi import HTTPException
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    group_id = str(body.get("group_id") or "").strip()
    artifact_id = str(body.get("artifact_id") or "").strip()
    linked_app_id = body.get("linked_app_id")
    note = str(body.get("note") or "").strip()

    if not group_id or not artifact_id or not linked_app_id:
        raise HTTPException(status_code=422, detail="缺少必填字段：group_id / artifact_id / linked_app_id")

    db = get_business_db()
    if _is_dep_locked_by_parent_group(db, app_id, group_id, artifact_id):
        raise HTTPException(status_code=422, detail="该依赖已由 parent 整组关联锁定，请在父级行使用“关联整组”统一修改")

    # 校验双方都存在，同时取项目名用于 Neo4j 同步
    project_names: Dict[int, str] = {}
    for check_id, label in [(app_id, "app_id"), (int(linked_app_id), "linked_app_id")]:
        row = db.conn.cursor().execute(
            "SELECT id, project_name FROM application_projects_cache WHERE id = ?", (check_id,)
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail=f"应用 id={check_id}（{label}）不存在")
        project_names[check_id] = str(row["project_name"] or "")

    # 从 application_dependencies 表查找版本号
    dep_version = str(body.get("dep_version") or "").strip()
    if not dep_version:
        ver_row = db.conn.cursor().execute(
            "SELECT version FROM application_dependencies WHERE app_id=? AND group_id=? AND artifact_id=? LIMIT 1",
            (app_id, group_id, artifact_id),
        ).fetchone()
        if ver_row:
            dep_version = str(ver_row["version"] or "")

    repo = AppDependencyLinksRepo(db=db)
    link_id = repo.upsert(app_id, group_id, artifact_id, int(linked_app_id), dep_version, note)

    # 立即同步到 Neo4j（幂等，失败不阻断保存结果）
    neo4j_synced = False
    neo4j_message = None
    try:
        import os as _os
        from storage.neo4j.connector import Neo4jConnector
        from storage.neo4j.queries import Neo4jQueries

        uri = _os.environ.get("NEO4J_URI", "")
        password = _os.environ.get("NEO4J_PASSWORD", "")
        if uri and password:
            user = _os.environ.get("NEO4J_USER", "neo4j")
            database = _os.environ.get("NEO4J_DATABASE", "neo4j")
            conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
            if conn.connect():
                try:
                    links_payload = [{
                        "from_project": project_names[app_id],
                        "to_project": project_names[int(linked_app_id)],
                        "group_id": group_id,
                        "artifact_id": artifact_id,
                        "dep_version": dep_version,
                    }]
                    conn.execute_write_query(Neo4jQueries.merge_depends_on_links(), {"links": links_payload})
                    neo4j_synced = True
                finally:
                    conn.disconnect()
    except Exception as e:
        neo4j_message = str(e)

    return {"ok": True, "id": link_id, "neo4j_synced": neo4j_synced, "neo4j_message": neo4j_message}


@router.put("/cache/application-projects/{app_id}/dependency-links/by-parent")
def upsert_dependency_links_by_parent(app_id: int, body: Dict[str, Any]) -> Dict[str, Any]:
    """
    按 parent 维度批量关联：
    body: { parent_group_id, parent_artifact_id, parent_version?, linked_app_id, note? }
    仅处理该 parent 组下 is_second_party=1 的依赖。
    """
    from fastapi import HTTPException
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    parent_group_id = str(body.get("parent_group_id") or "").strip()
    parent_artifact_id = str(body.get("parent_artifact_id") or "").strip()
    parent_version = str(body.get("parent_version") or "").strip()
    linked_app_id = body.get("linked_app_id")
    note = str(body.get("note") or "").strip()

    if not parent_group_id or not parent_artifact_id or not linked_app_id:
        raise HTTPException(status_code=422, detail="缺少必填字段：parent_group_id / parent_artifact_id / linked_app_id")

    db = get_business_db()

    # 校验双方都存在，同时取项目名用于 Neo4j 同步
    project_names: Dict[int, str] = {}
    for check_id, label in [(app_id, "app_id"), (int(linked_app_id), "linked_app_id")]:
        row = db.conn.cursor().execute(
            "SELECT id, project_name FROM application_projects_cache WHERE id = ?", (check_id,)
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=404, detail=f"应用 id={check_id}（{label}）不存在")
        project_names[check_id] = str(row["project_name"] or "")

    # 拉取 parent 组下所有二方包依赖
    cur = db.conn.cursor()
    if parent_version:
        all_rows = cur.execute(
            """
            SELECT group_id, artifact_id, version, is_second_party
            FROM application_dependencies
            WHERE app_id = ?
              AND parent_group_id = ?
              AND parent_artifact_id = ?
              AND parent_version = ?
            """,
            (app_id, parent_group_id, parent_artifact_id, parent_version),
        ).fetchall()
    else:
        all_rows = cur.execute(
            """
            SELECT group_id, artifact_id, version, is_second_party
            FROM application_dependencies
            WHERE app_id = ?
              AND parent_group_id = ?
              AND parent_artifact_id = ?
            """,
            (app_id, parent_group_id, parent_artifact_id),
        ).fetchall()

    if not all_rows:
        return {
            "ok": True,
            "affected_count": 0,
            "message": "该 parent 组下没有依赖记录",
            "neo4j_synced": False,
        }

    total_count = len(all_rows)
    second_party_total = sum(1 for r in all_rows if bool(r["is_second_party"]))
    # 仅允许“全二方包 parent”做整组关联，避免混合组误关联
    if second_party_total != total_count:
        raise HTTPException(
            status_code=422,
            detail=f"仅支持全二方包 parent 进行整组关联（当前 {second_party_total}/{total_count}）",
        )

    if parent_version:
        dep_rows = cur.execute(
            """
            SELECT group_id, artifact_id, version
            FROM application_dependencies
            WHERE app_id = ?
              AND parent_group_id = ?
              AND parent_artifact_id = ?
              AND parent_version = ?
            ORDER BY group_id, artifact_id
            """,
            (app_id, parent_group_id, parent_artifact_id, parent_version),
        ).fetchall()
    else:
        dep_rows = cur.execute(
            """
            SELECT group_id, artifact_id, version
            FROM application_dependencies
            WHERE app_id = ?
              AND parent_group_id = ?
              AND parent_artifact_id = ?
            ORDER BY group_id, artifact_id
            """,
            (app_id, parent_group_id, parent_artifact_id),
        ).fetchall()

    if not dep_rows:
        return {
            "ok": True,
            "affected_count": 0,
            "message": "该 parent 组下没有可关联的二方包依赖",
            "neo4j_synced": False,
        }

    repo = AppDependencyLinksRepo(db=db)
    link_ids: List[int] = []
    links_payload: List[Dict[str, Any]] = []
    for r in dep_rows:
        group_id = str(r["group_id"] or "").strip()
        artifact_id = str(r["artifact_id"] or "").strip()
        dep_version = str(r["version"] or "").strip()
        if not group_id or not artifact_id:
            continue
        link_id = repo.upsert(app_id, group_id, artifact_id, int(linked_app_id), dep_version, note)
        link_ids.append(link_id)
        links_payload.append({
            "from_project": project_names[app_id],
            "to_project": project_names[int(linked_app_id)],
            "group_id": group_id,
            "artifact_id": artifact_id,
            "dep_version": dep_version,
        })

    # 批量同步到 Neo4j
    neo4j_synced = False
    neo4j_message = None
    try:
        import os as _os
        from storage.neo4j.connector import Neo4jConnector
        from storage.neo4j.queries import Neo4jQueries

        uri = _os.environ.get("NEO4J_URI", "")
        password = _os.environ.get("NEO4J_PASSWORD", "")
        if uri and password and links_payload:
            user = _os.environ.get("NEO4J_USER", "neo4j")
            database = _os.environ.get("NEO4J_DATABASE", "neo4j")
            conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
            if conn.connect():
                try:
                    conn.execute_write_query(Neo4jQueries.merge_depends_on_links(), {"links": links_payload})
                    neo4j_synced = True
                finally:
                    conn.disconnect()
    except Exception as e:
        neo4j_message = str(e)

    return {
        "ok": True,
        "affected_count": len(link_ids),
        "ids": link_ids,
        "neo4j_synced": neo4j_synced,
        "neo4j_message": neo4j_message,
    }


@router.post("/cache/dependency-links/sync-to-neo4j")
def sync_dependency_links_to_neo4j() -> Dict[str, Any]:
    """
    将所有手动关联同步为 Neo4j 中的 DEPENDS_ON 边。
    幂等操作（MERGE），可重复调用。
    """
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    db = get_business_db()
    links = AppDependencyLinksRepo(db=db).get_all_for_neo4j_sync()
    if not links:
        return {"ok": True, "synced": 0, "message": "暂无关联记录"}

    import os as _os
    uri = _os.environ.get("NEO4J_URI", "")
    password = _os.environ.get("NEO4J_PASSWORD", "")
    if not uri or not password:
        return {"ok": False, "message": "Neo4j 未配置，跳过同步"}

    try:
        from storage.neo4j.connector import Neo4jConnector
        from storage.neo4j.queries import Neo4jQueries
        user = _os.environ.get("NEO4J_USER", "neo4j")
        database = _os.environ.get("NEO4J_DATABASE", "neo4j")
        conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
        if not conn.connect():
            return {"ok": False, "message": "Neo4j 连接失败"}
        try:
            result = conn.execute_write_query(Neo4jQueries.merge_depends_on_links(), {"links": links})
            synced = int((result[0].get("synced") if result else None) or 0)
        finally:
            conn.disconnect()
        return {"ok": True, "synced": synced, "total_links": len(links)}
    except Exception as e:
        return {"ok": False, "message": str(e)}


@router.delete("/cache/dependency-links/{link_id}")
def delete_dependency_link(link_id: int) -> Dict[str, Any]:
    """删除一条手动关联记录，同时删除 Neo4j 中对应的 DEPENDS_ON 边。"""
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.app_dependency_links_repo import AppDependencyLinksRepo

    db = get_business_db()
    row = db.conn.cursor().execute(
        """
        SELECT l.app_id, l.group_id, l.artifact_id,
               a.project_name AS from_project,
               b.project_name AS to_project
        FROM app_dependency_links l
        JOIN application_projects_cache a ON a.id = l.app_id
        JOIN application_projects_cache b ON b.id = l.linked_app_id
        WHERE l.id = ?
        """,
        (link_id,),
    ).fetchone()

    if row is not None:
        app_id = int(row["app_id"])
        group_id = str(row["group_id"] or "").strip()
        artifact_id = str(row["artifact_id"] or "").strip()
        if app_id and group_id and artifact_id and _is_dep_locked_by_parent_group(db, app_id, group_id, artifact_id):
            from fastapi import HTTPException
            raise HTTPException(status_code=422, detail="该依赖已由 parent 整组关联锁定，请在父级行调整整组关联")

    AppDependencyLinksRepo(db=db).delete(link_id)

    neo4j_message = None
    if row:
        try:
            import os as _os
            from storage.neo4j.connector import Neo4jConnector
            from storage.neo4j.queries import Neo4jQueries

            uri = _os.environ.get("NEO4J_URI", "")
            password = _os.environ.get("NEO4J_PASSWORD", "")
            if uri and password:
                user = _os.environ.get("NEO4J_USER", "neo4j")
                database = _os.environ.get("NEO4J_DATABASE", "neo4j")
                conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
                if conn.connect():
                    try:
                        conn.execute_write_query(
                            Neo4jQueries.delete_depends_on_link(),
                            {
                                "from_project": str(row["from_project"] or ""),
                                "to_project": str(row["to_project"] or ""),
                                "group_id": str(row["group_id"] or ""),
                                "artifact_id": str(row["artifact_id"] or ""),
                            },
                        )
                    finally:
                        conn.disconnect()
        except Exception as e:
            neo4j_message = str(e)

    return {"ok": True, "deleted_id": link_id, "neo4j_message": neo4j_message}


# ─── JAR 类索引管理 ────────────────────────────────────────────────────────────

@router.get("/cache/application-projects/{app_id}/jar-index-stats")
def get_jar_index_stats(app_id: int) -> Dict[str, Any]:
    """
    查询某应用的 JAR 类索引统计：总类数、jar 包数、最后扫描时间。
    通过 jar_path 前缀（.cache/maven_deps/<project_name>/）过滤。
    """
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.application_projects_repo import ApplicationProjectsRepo
    from storage.sqlite.jar_class_db import get_jar_class_db

    bdb = get_business_db()
    repo = ApplicationProjectsRepo(bdb)
    apps = repo.list_all()
    app = next((a for a in apps if a.id == app_id), None)
    if not app:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="应用不存在")

    project_name = str(app.project_name or app.repo_name or "").strip()
    if not project_name:
        return {"ok": True, "app_id": app_id, "total_classes": 0, "total_jars": 0, "last_scan_time": None, "jar_path_prefix": ""}

    jar_path_prefix = str(CACHE_MAVEN_DEPS_PATH / project_name)

    db = get_jar_class_db()
    cur = db.conn.cursor()

    row = cur.execute(
        "SELECT COUNT(*) AS cnt FROM jar_classes WHERE jar_path LIKE ?",
        (jar_path_prefix + "%",),
    ).fetchone()
    total_classes = int(row["cnt"] if row else 0)

    row2 = cur.execute(
        "SELECT COUNT(DISTINCT jar_path) AS cnt FROM jar_classes WHERE jar_path LIKE ?",
        (jar_path_prefix + "%",),
    ).fetchone()
    total_jars = int(row2["cnt"] if row2 else 0)

    row3 = cur.execute(
        "SELECT MAX(insert_time) AS last_time FROM jar_classes WHERE jar_path LIKE ?",
        (jar_path_prefix + "%",),
    ).fetchone()
    last_scan_time = str(row3["last_time"]) if row3 and row3["last_time"] else None

    return {
        "ok": True,
        "app_id": app_id,
        "project_name": project_name,
        "total_classes": total_classes,
        "total_jars": total_jars,
        "last_scan_time": last_scan_time,
        "jar_path_prefix": jar_path_prefix,
    }


@router.post("/cache/application-projects/{app_id}/rebuild-jar-index")
def rebuild_jar_index(app_id: int) -> Dict[str, Any]:
    """
    重建某应用的 JAR 类索引：
    1. 清空该应用旧的 jar_classes 记录
    2. 对 .cache/maven_deps/<project_name>/ 目录重新扫描所有 jar
    不重新执行 Maven（不拉取依赖），仅重建索引。
    若 maven_deps 目录不存在，返回提示。
    """
    from storage.sqlite.business import get_business_db
    from storage.sqlite.business.application_projects_repo import ApplicationProjectsRepo
    from storage.sqlite.jar_class_db import get_jar_class_db
    from storage.sqlite.jar_scanner import JARScanner

    bdb = get_business_db()
    repo = ApplicationProjectsRepo(bdb)
    apps = repo.list_all()
    app = next((a for a in apps if a.id == app_id), None)
    if not app:
        from fastapi import HTTPException
        raise HTTPException(status_code=404, detail="应用不存在")

    project_name = str(app.project_name or app.repo_name or "").strip()
    if not project_name:
        return {"ok": False, "message": "应用缺少 project_name，无法定位 maven_deps 目录"}

    out_dir = CACHE_MAVEN_DEPS_PATH / project_name
    if not out_dir.exists():
        return {
            "ok": False,
            "message": f"maven_deps 目录不存在（{out_dir}），请先在「导入/重建」页触发一次完整导入以拉取 Maven 依赖",
        }

    db = get_jar_class_db()
    db.initialize_schema()

    # 清空旧记录
    cleared = db.delete_by_jar_path_prefix(str(out_dir))

    # 重新扫描
    scanner = JARScanner(db)
    result = scanner.scan_directory(str(out_dir), force_rescan=True, include_anonymous=False)

    return {
        "ok": True,
        "project_name": project_name,
        "cleared_classes": cleared,
        "jars_found": result.total_jars_found,
        "jars_scanned": result.jars_scanned,
        "jars_skipped": result.jars_skipped,
        "total_classes": result.total_classes,
        "errors": result.errors[:10],  # 最多返回前10条错误
        "duration_seconds": round(result.duration, 2),
    }


@router.get("/cache/jar-index/search")
def search_jar_index(
    q: str = "",
    page: int = 1,
    page_size: int = 50,
) -> Dict[str, Any]:
    """
    全局搜索 jar_classes.db，支持按 fqn / simple_name / package_name 模糊查询。
    返回分页结果。
    """
    from storage.sqlite.jar_class_db import get_jar_class_db

    db = get_jar_class_db()
    cur = db.conn.cursor()

    offset = (max(1, page) - 1) * page_size
    q = (q or "").strip()

    if q:
        like = f"%{q}%"
        count_row = cur.execute(
            "SELECT COUNT(*) AS cnt FROM jar_classes WHERE fqn LIKE ? OR simple_name LIKE ? OR package_name LIKE ?",
            (like, like, like),
        ).fetchone()
        total = int(count_row["cnt"] if count_row else 0)
        rows = cur.execute(
            """SELECT fqn, simple_name, package_name, jar_name,
                      artifact_group_id, artifact_id, artifact_version,
                      parent_group_id, parent_artifact_id, parent_version
               FROM jar_classes
               WHERE fqn LIKE ? OR simple_name LIKE ? OR package_name LIKE ?
               ORDER BY fqn LIMIT ? OFFSET ?""",
            (like, like, like, page_size, offset),
        ).fetchall()
    else:
        count_row = cur.execute("SELECT COUNT(*) AS cnt FROM jar_classes").fetchone()
        total = int(count_row["cnt"] if count_row else 0)
        rows = cur.execute(
            """SELECT fqn, simple_name, package_name, jar_name,
                      artifact_group_id, artifact_id, artifact_version,
                      parent_group_id, parent_artifact_id, parent_version
               FROM jar_classes ORDER BY fqn LIMIT ? OFFSET ?""",
            (page_size, offset),
        ).fetchall()

    items = []
    for r in rows:
        g = r["artifact_group_id"] or r["parent_group_id"] or ""
        a = r["artifact_id"] or r["parent_artifact_id"] or ""
        v = r["artifact_version"] or r["parent_version"] or ""
        items.append({
            "fqn": r["fqn"],
            "simple_name": r["simple_name"],
            "package_name": r["package_name"],
            "jar_name": r["jar_name"],
            "coord": f"{g}:{a}:{v}" if g or a else r["jar_name"],
        })

    return {"ok": True, "total": total, "page": page, "page_size": page_size, "items": items}

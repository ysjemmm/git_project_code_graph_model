"""
本地 Git 缓存/元数据/Merkle 树管理接口（面向后台管理页面）。
"""
from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import APIRouter, Query

from api.config import GIT_CACHE_DIR, PROJECT_ROOT
from api.runtime_state import is_bugfix_repo_active
from api.schemas import (
    CacheProjectItem,
    CacheProjectListResponse,
    CanDeleteApplicationResponse,
    DeleteApplicationRequest,
    DeleteApplicationResponse,
    MerkleTreeResponse,
    MerkleTreeSummary,
)
from api.utils import run_git

router = APIRouter(prefix="/api", tags=["cache"])


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
        q = get_task_queue(max_workers=1, cache_base_dir=str(PROJECT_ROOT / ".cache" / "git_repos"))
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
    return PROJECT_ROOT / ".cache"


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


@router.get("/cache/application-projects", response_model=CacheProjectListResponse)
def cache_application_projects(
    include_libs: bool = Query(False, description="是否包含 Lib 项目（默认只返回 Application）"),
) -> CacheProjectListResponse:
    """
    汇总 Application 项目缓存信息（git 仓库、metadata、merkle 分支、缓存大小）。

    repo_name 默认等于 project_name（与当前 import/cache 约定一致）。
    """
    # 以本地缓存目录为准：只要 `.cache/git_repos/<repo_name>` 存在，就应该在“项目元数据”里可见。
    # Neo4j/图谱仅作为增强信息，不作为“是否展示”的前置条件。
    items: List[CacheProjectItem] = []
    base = GIT_CACHE_DIR
    if not base.exists():
        return CacheProjectListResponse(items=[])

    for repo_dir in base.iterdir():
        if not repo_dir.is_dir():
            continue
        # 忽略空壳目录（例如删除后残留的空文件夹）
        try:
            children = list(repo_dir.iterdir())
            if not children:
                continue
            # 进一步忽略“只剩 .git 的壳目录”（Windows 上删除失败时常见），避免误展示
            if len(children) == 1 and children[0].name == ".git":
                continue
        except Exception:
            pass
        # git 仓库通常包含 .git；但为兼容“已解压源码/残留目录”，这里不强制要求
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

        # 元数据字段优先级：metadata > git 实际信息
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
                **git_info,
            )
        )

    # 稳定排序：更新时间 desc，其次 repo_name
    def _k(x: CacheProjectItem):
        t = x.last_update_time or ""
        return (t, x.repo_name or "")

    items.sort(key=_k, reverse=True)
    return CacheProjectListResponse(items=items)


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


@router.get("/cache/merkle", response_model=MerkleTreeResponse)
def cache_merkle(
    repo_name: str = Query(..., description="仓库名/项目名（缓存目录名）"),
    branch: str = Query(..., description="分支名（用于定位 Merkle 文件）"),
    depth: int = Query(2, ge=0, le=6, description="返回树的最大深度（防止响应过大）"),
    max_children: int = Query(200, ge=10, le=2000, description="每层最大 children 数量"),
) -> MerkleTreeResponse:
    """
    返回指定 repo+branch 的 Merkle 树（截断版）+ 摘要统计。
    """
    tree = _load_merkle_tree(repo_name, branch)
    if not tree:
        summary = MerkleTreeSummary(
            repo_name=repo_name,
            branch=branch,
            node_count=0,
            file_count=0,
            dir_count=0,
            max_depth=0,
        )
        return MerkleTreeResponse(summary=summary, tree={})

    n, f, d, md = _merkle_stats(tree, 0)
    summary = MerkleTreeSummary(
        repo_name=repo_name,
        branch=branch,
        node_count=n,
        file_count=f,
        dir_count=d,
        max_depth=md,
    )
    truncated = _truncate_merkle(tree, max_depth=depth, max_children=max_children)
    return MerkleTreeResponse(summary=summary, tree=truncated)


@router.get("/cache/application/can-delete", response_model=CanDeleteApplicationResponse)
def can_delete_application(
    repo_name: str = Query(..., description="仓库名，与删除接口一致"),
    project_name: str = Query(..., description="项目名，与删除接口一致"),
) -> CanDeleteApplicationResponse:
    """
    删除前检查：是否存在进行中的导入任务或 AI 会话。
    前端可在展示删除确认前调用，用于禁用按钮或提示用户。
    """
    repo = (repo_name or "").strip() or (project_name or "").strip()
    proj = (project_name or "").strip() or repo
    if not repo:
        return CanDeleteApplicationResponse(ok=False, block_reason="repo_name / project_name 不能为空")
    block = _has_active_import_tasks(repo_name=repo, project_name=proj)
    if block:
        return CanDeleteApplicationResponse(ok=False, block_reason=block)
    if is_bugfix_repo_active(repo):
        return CanDeleteApplicationResponse(
            ok=False,
            block_reason=f"当前项目 {repo} 存在进行中的 AI 对话分析任务，请先在 Bugfix 对话页点击「结束」或等待完成后再删除。",
        )
    return CanDeleteApplicationResponse(ok=True, block_reason=None)


@router.post("/cache/application/delete", response_model=DeleteApplicationResponse)
def delete_application(req: DeleteApplicationRequest) -> DeleteApplicationResponse:
    """
    删除一个 Application：
    - 删除 Neo4j 中该 Application 项目子图（优先 project_key 精确删除）
    - 删除本地缓存：.cache/git_repos/<repo_name> + .cache/metadata/<repo_name>.json + .cache/merkle_trees/<repo_name>_*.json
    """
    project_name = (req.project_name or "").strip()
    # 版本化后：删除 Application 应当删除同 project_name 的所有版本（避免残留旧版本）
    project_key = None
    repo_name = (req.repo_name or "").strip() or project_name

    if not project_name:
        return DeleteApplicationResponse(ok=False, message="project_name 不能为空")

    # 0) 安全校验：存在导入任务/AI 对话任务时禁止删除
    block_reason = _has_active_import_tasks(repo_name=repo_name, project_name=project_name)
    if block_reason:
        return DeleteApplicationResponse(ok=False, message=block_reason)
    if is_bugfix_repo_active(repo_name):
        return DeleteApplicationResponse(
            ok=False,
            message=f"当前项目 {repo_name} 存在进行中的 AI 对话分析任务，请先在 Bugfix 对话页点击「结束」或等待完成后再删除。",
        )

    deleted_nodes = 0
    # 1) 删除 Neo4j 子图
    uri = os.environ.get("NEO4J_URI", "")
    user = os.environ.get("NEO4J_USER", "neo4j")
    password = os.environ.get("NEO4J_PASSWORD", "")
    database = os.environ.get("NEO4J_DATABASE", "neo4j")
    if uri and password:
        try:
            from storage.neo4j.connector import Neo4jConnector

            conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
            if conn.connect():
                try:
                    deleted_nodes = int(conn.delete_project_data(project_name=project_name, project_key=None) or 0)
                finally:
                    conn.disconnect()
        except Exception as e:
            return DeleteApplicationResponse(ok=False, message=f"删除图谱失败：{e}")
    else:
        # 未配置 Neo4j 时允许仅删除本地缓存
        deleted_nodes = 0

    # 2) 删除本地缓存（仓库/metadata/merkle）
    cache_deleted = True
    try:
        # repo dir
        repo_dir = GIT_CACHE_DIR / repo_name
        if repo_dir.exists():
            _rmtree_force(repo_dir)
        # 删除后必须不存在；否则认为失败（避免“假成功”导致前端还显示）
        if repo_dir.exists():
            raise RuntimeError(f"删除缓存目录失败（仍存在）: {repo_dir}")

        # metadata
        md = _metadata_dir() / f"{repo_name}.json"
        if md.is_file():
            md.unlink(missing_ok=True)  # py3.12+

        # merkle trees
        mdir = _merkle_dir()
        if mdir.exists():
            for p in mdir.iterdir():
                if p.is_file() and p.name.startswith(f"{repo_name}_") and p.name.endswith(".json"):
                    try:
                        p.unlink(missing_ok=True)
                    except Exception:
                        pass
    except Exception as e:
        cache_deleted = False
        return DeleteApplicationResponse(
            ok=False,
            message=f"图谱已删除 {deleted_nodes} 个节点，但清理本地缓存失败：{e}",
            deleted_graph_nodes=deleted_nodes,
            cache_deleted=False,
        )

    return DeleteApplicationResponse(
        ok=True,
        message=f"删除完成：图谱删除节点数={deleted_nodes}，本地缓存清理={'成功' if cache_deleted else '失败'}",
        deleted_graph_nodes=deleted_nodes,
        cache_deleted=cache_deleted,
    )


"""
Git 仓库、分支、提交列表接口。
"""
from __future__ import annotations

from typing import List, Optional

from fastapi import APIRouter, Query

from api.schemas import GitRefListResponse, GitRepoItem, GitRepoListResponse
from api.config import GIT_REFS_CACHE_DIR
from api.utils import run_git, run_git_global, repo_name_from_url

router = APIRouter(prefix="/api", tags=["git"])

def _ensure_full_remote_refspec(repo_dir) -> None:
    """
    某些缓存仓库可能是 single-branch clone 或被改过 fetch refspec，
    导致 `git fetch --all` 也只会更新极少量远端分支引用。
    这里尽量补全 origin 的 refspec：+refs/heads/*:refs/remotes/origin/*
    """
    try:
        specs = run_git(repo_dir, ["config", "--get-all", "remote.origin.fetch"])
        wanted = "+refs/heads/*:refs/remotes/origin/*"
        if wanted not in (specs or []):
            _ = run_git(repo_dir, ["config", "--add", "remote.origin.fetch", wanted])
    except Exception:
        pass


def _ensure_repo_cached(repoUrl: str) -> Optional[object]:
    """
    确保 repoUrl 对应的本地缓存仓库存在且 remote 正确。
    返回 repo_dir(Path-like)，失败返回 None（接口再自行兜底）。
    """
    from pathlib import Path
    import subprocess

    repo_dir = GIT_REFS_CACHE_DIR / repo_name_from_url(repoUrl)
    if not repo_dir.exists():
        repo_dir.parent.mkdir(parents=True, exist_ok=True)
        # 轻量克隆：不 checkout，尽量减少体积；若 git 版本不支持 filter，则自动失败后再用普通浅克隆兜底
        r = subprocess.run(
            ["git", "clone", "--no-checkout", "--depth", "200", "--filter=blob:none", repoUrl, str(repo_dir)],
            cwd=str(repo_dir.parent),
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        if r.returncode != 0:
            r2 = subprocess.run(
                ["git", "clone", "--no-checkout", "--depth", "200", repoUrl, str(repo_dir)],
                cwd=str(repo_dir.parent),
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
            if r2.returncode != 0:
                return None

    # 修正 remote（避免 repo_name 冲突时缓存目录指向了别的仓库）
    try:
        current = (run_git(repo_dir, ["config", "--get", "remote.origin.url"]) or [""])[0].strip()
        if current and current != repoUrl:
            _ = run_git(repo_dir, ["remote", "set-url", "origin", repoUrl])
    except Exception:
        pass

    _ensure_full_remote_refspec(repo_dir)
    return repo_dir


def _default_branch_from_remote(repoUrl: str) -> Optional[str]:
    """
    通过远端 HEAD 推断默认分支（不依赖本地缓存）。
    """
    # 输出示例：ref: refs/heads/main\tHEAD
    lines = run_git_global(["ls-remote", "--symref", repoUrl, "HEAD"]) or []
    for ln in lines:
        ln = (ln or "").strip()
        if ln.startswith("ref:") and "\tHEAD" in ln:
            # ref: refs/heads/main\tHEAD
            try:
                ref = ln.split()[1]
                if ref.startswith("refs/heads/"):
                    return ref[len("refs/heads/") :]
            except Exception:
                continue
    return None


@router.get("/git-repos", response_model=GitRepoListResponse)
def git_repos():
    """当前固定列表；后续可接入配置中心/数据库。"""
    return GitRepoListResponse(
        gitList=[
            GitRepoItem(name="forward", url="http://git.timevale.cn:8081/oper-crm/forward.git"),
        ]
    )


@router.get("/git-branches", response_model=GitRefListResponse)
def git_branches(
    repoUrl: str = Query(..., description="Git 仓库 URL"),
    q: Optional[str] = Query(None, description="模糊搜索关键字"),
    limit: int = Query(200, ge=1, le=2000),
):
    # 直接从远端列 heads，避免本地缓存仓库 fetch 不全导致“只看到少量分支”
    # 输出格式：<sha>\trefs/heads/<branch>
    lines = run_git_global(["ls-remote", "--heads", repoUrl]) or []
    branches: List[str] = []
    for ln in lines:
        ln = (ln or "").strip()
        if not ln:
            continue
        parts = ln.split()
        if len(parts) < 2:
            continue
        ref = parts[1]
        if ref.startswith("refs/heads/"):
            b = ref[len("refs/heads/") :]
            if b:
                branches.append(b)

    # 兜底：若远端不可达，则退回本地缓存已有引用（保持接口可用）
    if not branches:
        repo_dir = repo_dir_from_url(repoUrl)
        _ensure_full_remote_refspec(repo_dir)
        _ = run_git(repo_dir, ["fetch", "--all", "--prune"])
        fallback = run_git(repo_dir, ["for-each-ref", "--format=%(refname:strip=3)", "refs/remotes/origin"]) or []
        for ln in fallback:
            ln = (ln or "").strip()
            if not ln or ln == "HEAD" or "->" in ln:
                continue
            branches.append(ln)
    seen = set()
    items = []
    for b in branches:
        if b in seen:
            continue
        seen.add(b)
        items.append(b)
    if q:
        qq = q.strip().lower()
        if qq:
            items = [x for x in items if qq in x.lower()]
    return GitRefListResponse(items=items[: int(limit)])


@router.get("/git-commits", response_model=GitRefListResponse)
def git_commits(
    repoUrl: str = Query(..., description="Git 仓库 URL"),
    branch: Optional[str] = Query(None, description="可选：指定分支"),
    limit: int = Query(50, ge=1, le=200),
    q: Optional[str] = Query(None, description="模糊搜索关键字"),
):
    repo_dir = _ensure_repo_cached(repoUrl)
    if repo_dir is None:
        return GitRefListResponse(items=[])

    # 选择目标分支：优先用户传入，否则用远端默认分支兜底
    target_branch = (branch or "").strip() or _default_branch_from_remote(repoUrl)
    if target_branch:
        # 只 fetch 该分支的近期历史，避免全量拉取过慢
        _ = run_git(repo_dir, ["fetch", "--prune", "origin", target_branch, "--depth", "200"])
        items = run_git(repo_dir, ["log", f"origin/{target_branch}", f"-n{int(limit)}", "--pretty=%H"])
    else:
        _ = run_git(repo_dir, ["fetch", "--all", "--prune"])
        items = run_git(repo_dir, ["rev-list", "--all", f"--max-count={int(limit)}"])
    if q:
        qq = q.strip().lower()
        if qq:
            items = [x for x in items if qq in x.lower()]
    return GitRefListResponse(items=items)

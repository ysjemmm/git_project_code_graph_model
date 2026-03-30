"""
Git 仓库、分支、提交列表接口。
"""
from __future__ import annotations

import subprocess
import time
from typing import Dict, List, Optional, Tuple

from fastapi import APIRouter, Query

from api.schemas import GitRefListResponse, GitRepoItem, GitRepoListResponse
from api.config import GIT_REFS_CACHE_DIR
from api.utils import run_git, run_git_global, repo_name_from_url, repo_dir_from_url

router = APIRouter(prefix="/api", tags=["git"])

# 内存缓存：{cache_key: (timestamp, data)}
_branches_cache: Dict[str, Tuple[float, List[str]]] = {}
_BRANCHES_TTL = 60  # 秒


def _run_git_text(repo_dir, args: List[str]) -> tuple[int, str, str]:
    r = subprocess.run(
        ["git", *args],
        cwd=str(repo_dir),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return int(r.returncode), (r.stdout or ""), (r.stderr or "")


def _resolve_commit(repo_dir, ref: str) -> Optional[str]:
    cand = (ref or "").strip()
    if not cand:
        return None
    candidates = [cand]
    if not cand.startswith("origin/"):
        candidates.append(f"origin/{cand}")
    for c in candidates:
        rc, out, _ = _run_git_text(repo_dir, ["rev-parse", "--verify", f"{c}^{{commit}}"])
        if rc == 0:
            v = (out or "").strip()
            if v:
                return v
    return None

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


def _get_diff_repo_dir(project_name: Optional[str], repo_url: str) -> Optional[object]:
    """
    优先使用图谱导入缓存目录（CACHE_GIT_REPOS_PATH/{project_name}），
    fallback 到轻量 refs 缓存（_ensure_repo_cached）。
    """
    from pathlib import Path
    if project_name:
        try:
            from tools.constants import CACHE_GIT_REPOS_PATH
            graph_dir = CACHE_GIT_REPOS_PATH / project_name
            if graph_dir.exists():
                return graph_dir
        except Exception:
            pass
    return _ensure_repo_cached(repo_url)


def _ensure_repo_cached(repoUrl: str) -> Optional[object]:
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
    refresh: bool = Query(False, description="强制刷新缓存"),
):
    cache_key = repoUrl
    now = time.time()

    # 命中缓存（无搜索词时才用缓存）
    if not refresh and not q:
        cached = _branches_cache.get(cache_key)
        if cached and (now - cached[0]) < _BRANCHES_TTL:
            return GitRefListResponse(items=cached[1][:int(limit)])

    # 直接从远端列 heads
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
            b = ref[len("refs/heads/"):]
            if b:
                branches.append(b)

    # 兜底：远端不可达时退回本地缓存
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

    # 去重
    seen: set = set()
    deduped: List[str] = []
    for b in branches:
        if b not in seen:
            seen.add(b)
            deduped.append(b)

    # 写缓存（只缓存无搜索词的全量结果）
    if not q:
        _branches_cache[cache_key] = (now, deduped)

    # 搜索过滤
    if q:
        qq = q.strip().lower()
        if qq:
            deduped = [x for x in deduped if qq in x.lower()]

    return GitRefListResponse(items=deduped[:int(limit)])


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


@router.get("/git-diff/summary")
def git_diff_summary(
    repoUrl: str = Query(..., description="Git 仓库 URL"),
    fromRef: str = Query(..., description="基线 ref（上次发布）"),
    toRef: str = Query(..., description="目标 ref（本次发布）"),
    maxFiles: int = Query(500, ge=1, le=2000),
    projectName: Optional[str] = Query(None, description="项目名称，用于定位图谱缓存目录"),
) -> dict:
    repo_dir = _get_diff_repo_dir(projectName, repoUrl)
    if repo_dir is None:
        return {"ok": False, "message": "仓库缓存失败，请检查 repoUrl"}

    _ = run_git(repo_dir, ["fetch", "--all", "--prune", "--tags"])
    from_commit = _resolve_commit(repo_dir, fromRef)
    to_commit = _resolve_commit(repo_dir, toRef)
    if not from_commit:
        return {"ok": False, "message": f"无法解析 fromRef: {fromRef}"}
    if not to_commit:
        return {"ok": False, "message": f"无法解析 toRef: {toRef}"}
    if from_commit == to_commit:
        return {
            "ok": True,
            "from_ref": fromRef,
            "to_ref": toRef,
            "from_commit": from_commit,
            "to_commit": to_commit,
            "same_commit": True,
            "stats": {"files": 0, "additions": 0, "deletions": 0},
            "files": [],
        }

    rc_num, out_num, err_num = _run_git_text(repo_dir, ["diff", "--numstat", f"{from_commit}..{to_commit}"])
    if rc_num != 0:
        return {"ok": False, "message": f"读取 diff numstat 失败: {(err_num or '').strip()}"}
    rc_name, out_name, err_name = _run_git_text(repo_dir, ["diff", "--name-status", f"{from_commit}..{to_commit}"])
    if rc_name != 0:
        return {"ok": False, "message": f"读取 diff name-status 失败: {(err_name or '').strip()}"}

    stats_map: dict[str, dict] = {}
    total_add = 0
    total_del = 0
    for raw in (out_num or "").splitlines():
        line = (raw or "").rstrip("\n")
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        add_s, del_s, path = parts[0], parts[1], parts[2]
        add_n = int(add_s) if add_s.isdigit() else 0
        del_n = int(del_s) if del_s.isdigit() else 0
        total_add += add_n
        total_del += del_n
        stats_map[path] = {"additions": add_n, "deletions": del_n}

    files: list[dict] = []
    for raw in (out_name or "").splitlines():
        line = (raw or "").rstrip("\n")
        if not line.strip():
            continue
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        status_raw = parts[0]
        status = (status_raw[:1] or "M").upper()
        old_path = None
        path = parts[-1]
        if status == "R" and len(parts) >= 3:
            old_path = parts[1]
            path = parts[2]
        st = stats_map.get(path, {"additions": 0, "deletions": 0})
        files.append({
            "status": status,
            "path": path,
            "old_path": old_path,
            "additions": int(st.get("additions", 0)),
            "deletions": int(st.get("deletions", 0)),
        })

    files = files[: int(maxFiles)]
    return {
        "ok": True,
        "from_ref": fromRef,
        "to_ref": toRef,
        "from_commit": from_commit,
        "to_commit": to_commit,
        "same_commit": False,
        "stats": {
            "files": len(files),
            "additions": int(total_add),
            "deletions": int(total_del),
        },
        "files": files,
    }


@router.get("/git-diff/file")
def git_diff_file(
    repoUrl: str = Query(..., description="Git 仓库 URL"),
    fromRef: str = Query(..., description="基线 ref（上次发布）"),
    toRef: str = Query(..., description="目标 ref（本次发布）"),
    filePath: str = Query(..., description="文件路径"),
    context: int = Query(3, ge=0, le=20),
    maxLines: int = Query(1200, ge=50, le=4000),
    projectName: Optional[str] = Query(None, description="项目名称，用于定位图谱缓存目录"),
) -> dict:
    repo_dir = _get_diff_repo_dir(projectName, repoUrl)
    if repo_dir is None:
        return {"ok": False, "message": "仓库缓存失败，请检查 repoUrl"}

    _ = run_git(repo_dir, ["fetch", "--all", "--prune", "--tags"])
    from_commit = _resolve_commit(repo_dir, fromRef)
    to_commit = _resolve_commit(repo_dir, toRef)
    if not from_commit:
        return {"ok": False, "message": f"无法解析 fromRef: {fromRef}"}
    if not to_commit:
        return {"ok": False, "message": f"无法解析 toRef: {toRef}"}

    rc, out, err = _run_git_text(
        repo_dir,
        ["diff", f"--unified={int(context)}", f"{from_commit}..{to_commit}", "--", filePath],
    )
    if rc != 0:
        return {"ok": False, "message": f"读取文件 diff 失败: {(err or '').strip()}"}

    lines = (out or "").splitlines()
    truncated = len(lines) > int(maxLines)
    if truncated:
        lines = lines[: int(maxLines)]
    patch = "\n".join(lines)
    return {
        "ok": True,
        "from_ref": fromRef,
        "to_ref": toRef,
        "from_commit": from_commit,
        "to_commit": to_commit,
        "file_path": filePath,
        "truncated": truncated,
        "patch": patch,
    }


@router.post("/git-apply-and-commit")
def git_apply_and_commit(body: dict):
    """
    将 unified diff 应用到本地缓存仓库，建分支、commit、push。
    body: { git_url, project_name?, branch, commit_msg, diff, reviewer? }
    project_name 优先用于定位本地缓存目录，没有则 fallback 到 git_url 解析。
    """
    import tempfile, os, re

    git_url    = (body.get("git_url") or "").strip()
    project_name = (body.get("project_name") or "").strip()
    branch     = (body.get("branch") or "").strip()
    commit_msg = (body.get("commit_msg") or "").strip()
    diff_text  = (body.get("diff") or "").strip()

    if not git_url:
        return {"ok": False, "message": "git_url 不能为空"}
    if not project_name:
        return {"ok": False, "message": "project_name 不能为空，请确保已选择应用"}
    if not branch:
        return {"ok": False, "message": "branch 不能为空"}
    if not commit_msg:
        return {"ok": False, "message": "commit_msg 不能为空"}
    if not diff_text:
        return {"ok": False, "message": "diff 不能为空"}

    # 直接用 project_name 定位缓存目录（专用 push 目录）
    from tools.constants import CACHE_GIT_REPOS_FOR_PUSH_PATH, CACHE_GIT_REPOS_PATH
    repo_dir = CACHE_GIT_REPOS_FOR_PUSH_PATH / project_name
    if not repo_dir.exists():
        return {"ok": False, "message": f"本地未找到 push 仓库缓存目录 '{project_name}'，请先完成图谱导入"}

    # ── 预检：验证 diff 里的文件路径在仓库中真实存在 ──
    # 解析 +++ b/path 行，提取目标文件路径
    target_paths = re.findall(r'^\+\+\+ b/(.+)$', diff_text, re.MULTILINE)
    # 过滤掉 /dev/null（新增文件不需要预检）
    missing = []
    for p in target_paths:
        p = p.strip()
        if p == '/dev/null':
            continue
        full = repo_dir / p
        if not full.exists():
            missing.append(p)
    if missing:
        return {
            "ok": False,
            "message": (
                f"diff 中以下文件路径在仓库目录 '{repo_dir.name}' 下不存在，"
                f"请检查 AI 生成的 diff 路径是否正确：\n" +
                "\n".join(f"  • {p}" for p in missing)
            ),
            "missing_paths": missing,
        }

    steps = []

    # 1. fetch 最新
    rc, _, err = _run_git_text(repo_dir, ["fetch", "--all", "--prune"])
    if rc != 0:
        return {"ok": False, "message": f"git fetch 失败：{err.strip()}", "steps": steps}
    steps.append("git fetch --all")

    # 2. 切到默认分支（main/master）再建新分支
    for base in ("main", "master", "origin/main", "origin/master"):
        rc, _, _ = _run_git_text(repo_dir, ["checkout", base])
        if rc == 0:
            steps.append(f"git checkout {base}")
            break
    else:
        return {"ok": False, "message": "无法切换到 main/master 分支", "steps": steps}

    # 3. 建新分支
    rc, _, err = _run_git_text(repo_dir, ["checkout", "-b", branch])
    if rc != 0:
        return {"ok": False, "message": f"git checkout -b {branch} 失败：{err.strip()}", "steps": steps}
    steps.append(f"git checkout -b {branch}")

    # 4. 写 patch 文件并 apply
    with tempfile.NamedTemporaryFile(mode="w", suffix=".patch", delete=False, encoding="utf-8") as f:
        f.write(diff_text)
        patch_path = f.name
    try:
        rc, out, err = _run_git_text(repo_dir, ["apply", "--whitespace=fix", patch_path])
        if rc != 0:
            return {"ok": False, "message": f"git apply 失败：{err.strip()}", "steps": steps}
        steps.append("git apply patch")
    finally:
        os.unlink(patch_path)

    # 5. git add -A
    rc, _, err = _run_git_text(repo_dir, ["add", "-A"])
    if rc != 0:
        return {"ok": False, "message": f"git add 失败：{err.strip()}", "steps": steps}
    steps.append("git add -A")

    # 6. commit
    rc, _, err = _run_git_text(repo_dir, ["commit", "-m", commit_msg])
    if rc != 0:
        return {"ok": False, "message": f"git commit 失败：{err.strip()}", "steps": steps}
    steps.append(f"git commit -m '{commit_msg}'")

    # 7. push
    rc, _, err = _run_git_text(repo_dir, ["push", "-u", "origin", branch])
    if rc != 0:
        return {"ok": False, "message": f"git push 失败：{err.strip()}", "steps": steps}
    steps.append(f"git push origin {branch}")

    # 8. 可选：push 成功后对图谱缓存目录执行 git pull
    pull_after_commit = bool(body.get("pull_after_commit", False))
    pull_result = None
    if pull_after_commit:
        graph_repo_dir = CACHE_GIT_REPOS_PATH / project_name
        if graph_repo_dir.exists():
            rc_pull, _, err_pull = _run_git_text(graph_repo_dir, ["pull"])
            if rc_pull == 0:
                steps.append(f"git pull (图谱缓存目录 {project_name})")
                pull_result = "success"
            else:
                pull_result = f"失败：{err_pull.strip()}"
                steps.append(f"git pull 失败（图谱缓存目录）：{err_pull.strip()}")
        else:
            pull_result = "skip（图谱缓存目录不存在）"

    result = {"ok": True, "message": "已成功提交并推送", "branch": branch, "steps": steps}
    if pull_result is not None:
        result["pull_result"] = pull_result
    return result


@router.post("/git-pull-cache")
def git_pull_cache(body: dict):
    """
    对图谱缓存目录（CACHE_GIT_REPOS_PATH / project_name）执行 git pull。
    body: { project_name }
    """
    from tools.constants import CACHE_GIT_REPOS_PATH

    project_name = (body.get("project_name") or "").strip()
    if not project_name:
        return {"ok": False, "message": "project_name 不能为空"}

    repo_dir = CACHE_GIT_REPOS_PATH / project_name
    if not repo_dir.exists():
        return {"ok": False, "message": f"本地缓存目录不存在：{project_name}"}

    rc, out, err = _run_git_text(repo_dir, ["pull"])
    if rc != 0:
        return {"ok": False, "message": f"git pull 失败：{err.strip() or out.strip()}"}

    return {"ok": True, "message": out.strip() or "已完成 git pull"}

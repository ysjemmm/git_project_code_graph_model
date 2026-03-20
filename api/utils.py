"""
通用工具函数：路径、文件名安全化、Git 命令。供 routes 与 agent 使用。
"""
from __future__ import annotations

import re
import subprocess
from pathlib import Path
from typing import List

from api.config import GIT_CACHE_DIR, PROJECT_ROOT


def repo_name_from_url(repo_url: str) -> str:
    """从 Git URL 解析出仓库名。"""
    name = (repo_url or "").rstrip("/").split("/")[-1]
    if name.endswith(".git"):
        name = name[: -len(".git")]
    return name or "unknown"


def repo_dir_from_url(repo_url: str) -> Path:
    """根据仓库 URL 返回本地缓存目录。"""
    return GIT_CACHE_DIR / repo_name_from_url(repo_url)


def safe_filename(s: str) -> str:
    """用于保存到磁盘的文件名安全化。"""
    return re.sub(r"[^\w\-.]", "_", (s or "").strip()) or "unknown"


def run_git(repo_dir: Path, args: List[str], *, capture_stderr: bool = False) -> List[str]:
    """在仓库目录执行 git 命令，返回 stdout 非空行列表；失败返回 []。"""
    if not repo_dir.exists():
        return []
    r = subprocess.run(
        ["git", *args],
        cwd=str(repo_dir),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE if capture_stderr else subprocess.DEVNULL,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0:
        return []
    return [line.strip() for line in (r.stdout or "").splitlines() if line.strip()]


def run_git_global(args: List[str], *, capture_stderr: bool = False) -> List[str]:
    """
    在项目根目录执行 git 命令（不依赖本地缓存仓库 cwd）。
    适用于 `git ls-remote` 这类只需要远端 URL 的命令。
    """
    r = subprocess.run(
        ["git", *args],
        cwd=str(PROJECT_ROOT),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE if capture_stderr else subprocess.DEVNULL,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0:
        return []
    return [line.strip() for line in (r.stdout or "").splitlines() if line.strip()]


def git_checkout(repo_dir: Path, ref: str) -> tuple[bool, str | None]:
    """
    在仓库目录执行 git checkout -f ref。成功返回 (True, None)，失败返回 (False, 错误信息)。
    """
    if not repo_dir.exists():
        return False, f"仓库目录不存在：{repo_dir}"
    r = subprocess.run(
        ["git", "checkout", "-f", ref],
        cwd=str(repo_dir),
        stdout=subprocess.DEVNULL,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if r.returncode != 0:
        return False, (r.stderr or "unknown error").strip()
    return True, None

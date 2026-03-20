from __future__ import annotations

from dataclasses import dataclass
from threading import Lock
from typing import Dict


@dataclass
class _Counters:
    lock: Lock
    counts: Dict[str, int]


_bugfix_repos = _Counters(lock=Lock(), counts={})


def bugfix_repo_enter(repo_name: str) -> None:
    repo = (repo_name or "").strip()
    if not repo:
        return
    with _bugfix_repos.lock:
        _bugfix_repos.counts[repo] = int(_bugfix_repos.counts.get(repo, 0) or 0) + 1


def bugfix_repo_exit(repo_name: str) -> None:
    repo = (repo_name or "").strip()
    if not repo:
        return
    with _bugfix_repos.lock:
        cur = int(_bugfix_repos.counts.get(repo, 0) or 0)
        if cur <= 1:
            _bugfix_repos.counts.pop(repo, None)
        else:
            _bugfix_repos.counts[repo] = cur - 1


def is_bugfix_repo_active(repo_name: str) -> bool:
    repo = (repo_name or "").strip()
    if not repo:
        return False
    with _bugfix_repos.lock:
        return int(_bugfix_repos.counts.get(repo, 0) or 0) > 0


from __future__ import annotations

from pathlib import Path

def _detect_project_root() -> Path:
    """
    运行时自动探测项目根目录：
    1) 优先按 .gitignore 判断（用户期望）
    2) 其次按 .git 目录兜底
    3) 最后回退到本文件上两级目录（兼容旧逻辑）
    """
    seeds = [
        Path.cwd().resolve(),
        Path(__file__).resolve().parent.parent,
    ]
    seen = set()
    for seed in seeds:
        for cur in [seed, *seed.parents]:
            cur_str = str(cur)
            if cur_str in seen:
                continue
            seen.add(cur_str)
            if (cur / ".gitignore").is_file():
                return cur
            if (cur / ".git").exists():
                return cur
    return Path(__file__).resolve().parent.parent


# 项目根目录（动态探测）
PROJECT_ROOT_PATH = _detect_project_root()

# 统一缓存目录（避免各模块散落硬编码 ".cache"）
CACHE_ROOT_PATH = PROJECT_ROOT_PATH / ".cache"
CACHE_GIT_REPOS_PATH = CACHE_ROOT_PATH / "git_repos"
CACHE_GIT_REPOS_FOR_PUSH_PATH = CACHE_ROOT_PATH / "git_repos_for_push"
CACHE_GIT_REFS_PATH = CACHE_ROOT_PATH / "git_refs"
CACHE_TASK_LOGS_PATH = CACHE_ROOT_PATH / "task_logs"
CACHE_INCREMENTAL_PATH = CACHE_ROOT_PATH / "incremental"
CACHE_METADATA_PATH = CACHE_ROOT_PATH / "metadata"
CACHE_MERKLE_TREES_PATH = CACHE_ROOT_PATH / "merkle_trees"
CACHE_MAVEN_PATH = CACHE_ROOT_PATH / "maven"
CACHE_MAVEN_DEPS_PATH = CACHE_ROOT_PATH / "maven_deps"
CACHE_NEO4J_OP_LOG_PATH = CACHE_ROOT_PATH / "neo4j_op_logs.jsonl"

# 常用 sqlite 路径
CACHE_BUSINESS_DB_PATH = CACHE_ROOT_PATH / "business.db"
CACHE_JAR_CLASSES_DB_PATH = CACHE_ROOT_PATH / "jar_classes.db"
CACHE_PROJECT_CLASSES_DB_PATH = CACHE_ROOT_PATH / "project_classes.db"


"""
应用配置：环境变量加载、路径常量。在应用入口最早加载，供各模块使用。
"""
from __future__ import annotations

from pathlib import Path

# 项目根目录（api 的上一级）
PROJECT_ROOT = Path(__file__).resolve().parents[1]
STATIC_DIR = PROJECT_ROOT / "static"
GIT_CACHE_DIR = PROJECT_ROOT / ".cache" / "git_repos"
# 仅用于 refs 下拉（branches/commits）的轻量缓存，避免污染“项目元数据”的 git_repos 列表
GIT_REFS_CACHE_DIR = PROJECT_ROOT / ".cache" / "git_refs"


def load_env() -> None:
    """加载 .env.local / .env，应在 main 入口最早调用。优先用 dotenv；若无则用 core.env_loader 兜底。"""
    try:
        from dotenv import load_dotenv
        load_dotenv(dotenv_path=PROJECT_ROOT / ".env.local", override=False)
        load_dotenv(dotenv_path=PROJECT_ROOT / ".env", override=False)
    except Exception:
        try:
            from core.env_loader import load_env_vars
            load_env_vars({
                "ANTHROPIC_API_BASE", "ANTHROPIC_BASE_URL", "ANTHROPIC_API_KEY", "ANTHROPIC_MODEL",
                "DEEPSEEK_API_BASE", "DEEPSEEK_API_KEY", "DEEPSEEK_MODEL", "OPENAI_API_KEY",
                "NEO4J_URI", "NEO4J_USER", "NEO4J_PASSWORD", "NEO4J_DATABASE",
            }, root=PROJECT_ROOT)
        except Exception:
            pass

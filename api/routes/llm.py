"""
LLM 相关接口：健康检查、Claude 连通性测试、调试信息。
"""
from __future__ import annotations

import os
from typing import Optional

from fastapi import APIRouter, Query

from api.config import PROJECT_ROOT
from api.llm import test_claude_connectivity

router = APIRouter(prefix="/api", tags=["llm"])


@router.get("/health")
def health():
    return {"ok": True}


@router.get("/llm/test-claude")
async def llm_test_claude(
    model: Optional[str] = Query(None, description="Claude 模型 id，如 claude-sonnet-4-6"),
):
    """测试当前配置的 Claude 是否连通，仅测 Claude，不测 DeepSeek。"""
    ok, err = await test_claude_connectivity(model=model)
    if ok:
        return {"ok": True, "model": model or "(default)"}
    return {"ok": False, "error": err}


@router.get("/llm/debug")
def llm_debug():
    """
    调试用：查看当前进程实际使用的 LLM 配置（不暴露完整密钥）。
    """
    base = (os.environ.get("DEEPSEEK_API_BASE") or "").strip() or None
    key = os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")
    a_base = (
        (os.environ.get("ANTHROPIC_API_BASE") or os.environ.get("ANTHROPIC_BASE_URL") or "").strip()
        or None
    )
    a_key = os.environ.get("ANTHROPIC_API_KEY", "") or key
    env_file = (
        ".env.local"
        if (PROJECT_ROOT / ".env.local").exists()
        else (".env" if (PROJECT_ROOT / ".env").exists() else None)
    )
    return {
        "project_root": str(PROJECT_ROOT),
        "env_file_checked": env_file,
        "deepseek_base": base,
        "deepseek_key_suffix": f"****{key[-4:]}" if len(key) >= 4 else "(empty or too short)",
        "anthropic_base": a_base,
        "anthropic_key_suffix": f"****{a_key[-4:]}" if len(a_key) >= 4 else "(empty or too short)",
    }

"""
LLM 抽象：支持 DeepSeek（OpenAI 兼容）、Claude 等，统一流式输出。

中转配置（环境变量）：
- DeepSeek：DEEPSEEK_API_BASE（中转 HTTP 地址）、DEEPSEEK_API_KEY 或 OPENAI_API_KEY（密钥）
- Claude：ANTHROPIC_API_BASE 或 ANTHROPIC_BASE_URL（中转地址）、ANTHROPIC_API_KEY（密钥）
"""
from __future__ import annotations

import logging
import os
from typing import AsyncIterator, List, Literal, Optional

Provider = Literal["deepseek", "claude"]
logger = logging.getLogger("api.llm")


def _get_deepseek_config() -> tuple[str, str, str]:
    """每次请求时从环境变量读取，避免进程未加载 .env 时用错配置。"""
    try:
        from core.env_loader import load_env_vars
        load_env_vars({"DEEPSEEK_API_BASE", "DEEPSEEK_API_KEY", "DEEPSEEK_MODEL", "OPENAI_API_KEY"})
    except Exception:
        pass
    base = (os.environ.get("DEEPSEEK_API_BASE") or "https://api.deepseek.com").strip().rstrip("/")
    key = os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")
    model = os.environ.get("DEEPSEEK_MODEL", "deepseek-chat")
    return base, key, model


def _get_anthropic_config() -> tuple[str, str, str]:
    """每次请求时从环境变量读取。Claude base 不要带 /v1，SDK 会自己加。"""
    try:
        from core.env_loader import load_env_vars
        load_env_vars({"ANTHROPIC_API_BASE", "ANTHROPIC_BASE_URL", "ANTHROPIC_API_KEY", "ANTHROPIC_MODEL", "DEEPSEEK_API_BASE", "DEEPSEEK_API_KEY", "OPENAI_API_KEY"})
    except Exception:
        pass
    a_base = os.environ.get("ANTHROPIC_API_BASE") or os.environ.get("ANTHROPIC_BASE_URL", "").strip() or None
    deepseek_base = (os.environ.get("DEEPSEEK_API_BASE") or "").rstrip("/")
    if deepseek_base.endswith("/v1"):
        deepseek_base = deepseek_base[: -len("/v1")]
    fallback = deepseek_base or "https://api.anthropic.com"
    base = (a_base or fallback).rstrip("/")
    key = os.environ.get("ANTHROPIC_API_KEY", "") or os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")
    model = os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-20250514")
    return base, key, model


async def stream_chat(
    messages: List[dict],
    *,
    provider: Provider = "deepseek",
    model: Optional[str] = None,
) -> AsyncIterator[str]:
    """
    流式调用大模型，逐个 yield 文本片段。
    messages: [{"role":"system"|"user"|"assistant", "content": "..."}]
    """
    if provider == "deepseek":
        async for chunk in _stream_deepseek(messages, model=model):
            yield chunk
    elif provider == "claude":
        async for chunk in _stream_claude(messages, model=model):
            yield chunk
    else:
        raise ValueError(f"Unknown provider: {provider}")


async def _stream_deepseek(
    messages: List[dict],
    model: Optional[str] = None,
) -> AsyncIterator[str]:
    """DeepSeek（OpenAI 兼容）流式调用。"""
    base, key, default_model = _get_deepseek_config()
    logger.info("LLM request: provider=deepseek base=%s key_suffix=****%s", base, key[-4:] if len(key) >= 4 else "(none)")
    if not key:
        yield "[错误：未配置 DEEPSEEK_API_KEY 或 OPENAI_API_KEY；请检查 .env.local 并重启 uvicorn]"
        return
    try:
        from openai import AsyncOpenAI
    except ImportError:
        yield "[错误：请安装 openai：pip install openai]"
        return

    client = AsyncOpenAI(api_key=key, base_url=base)
    m = model or default_model
    stream = await client.chat.completions.create(
        model=m,
        messages=messages,
        stream=True,
    )
    async for chunk in stream:
        if chunk.choices and chunk.choices[0].delta.content:
            yield chunk.choices[0].delta.content


async def _stream_claude(
    messages: List[dict],
    model: Optional[str] = None,
) -> AsyncIterator[str]:
    """Claude 流式调用，支持中转（ANTHROPIC_API_BASE / ANTHROPIC_BASE_URL）。"""
    base, key, default_model = _get_anthropic_config()
    logger.info("LLM request: provider=claude base=%s key_suffix=****%s", base, key[-4:] if len(key) >= 4 else "(none)")
    if not key:
        yield "[错误：未配置 ANTHROPIC_API_KEY 或 DEEPSEEK_API_KEY；请检查 .env.local 并重启 uvicorn]"
        return
    try:
        from anthropic import AsyncAnthropic
    except ImportError:
        yield "[错误：请安装 anthropic：pip install anthropic]"
        return

    # 将 messages 转为 Claude 格式（system 单独，其余 user/assistant）
    system = ""
    claude_messages: List[dict] = []
    for m in messages:
        if m.get("role") == "system":
            system += (m.get("content") or "") + "\n"
        else:
            claude_messages.append({"role": m["role"], "content": m.get("content") or ""})

    # 显式拉长超时，避免长上下文/多轮时被 SDK 或网络提前断开（Anthropic 报 Request timed out or interrupted）
    timeout_sec = float(os.environ.get("ANTHROPIC_TIMEOUT_SEC", "600") or "600")
    client = AsyncAnthropic(api_key=key, base_url=base, timeout=timeout_sec)
    m = model or default_model
    async with client.messages.stream(
        model=m,
        max_tokens=8192,
        system=system or None,
        messages=claude_messages,
    ) as stream:
        async for text in stream.text_stream:
            yield text


async def test_claude_connectivity(model: Optional[str] = None) -> tuple[bool, str]:
    """
    测试 Claude 是否连通：发一条极简非流式请求，成功返回 (True, "")，失败返回 (False, error_message)。
    """
    base, key, default_model = _get_anthropic_config()
    if not key:
        return False, "未配置 ANTHROPIC_API_KEY 或 DEEPSEEK_API_KEY"
    try:
        from anthropic import AsyncAnthropic
    except ImportError:
        return False, "请安装 anthropic：pip install anthropic"
    m = (model or default_model).strip() or default_model
    try:
        timeout_sec = float(os.environ.get("ANTHROPIC_TIMEOUT_SEC", "120") or "120")
        client = AsyncAnthropic(api_key=key, base_url=base, timeout=timeout_sec)
        msg = await client.messages.create(
            model=m,
            max_tokens=16,
            messages=[{"role": "user", "content": "Say pong."}],
        )
        if msg and (msg.content or msg.stop_reason):
            return True, ""
        return True, ""
    except Exception as e:
        return False, str(e)

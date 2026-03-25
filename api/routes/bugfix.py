"""
Bugfix 流式分析接口：SSE 推送 AI 分析结果与工具调用事件。
"""
from __future__ import annotations

import asyncio
import json
import logging
import os
from pathlib import Path
from typing import AsyncIterator, List, Optional

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from api.agent import run_bugfix_agent, BugInfoUnavailableError
from api.prompts import BUG_EXPERT_SYSTEM_PROMPT, STRUCTURED_RESULT_PROMPT
from api.schemas import BugfixRequest
from api.runtime_state import bugfix_repo_enter, bugfix_repo_exit
from api.utils import repo_dir_from_url, repo_name_from_url, git_checkout

router = APIRouter(prefix="/api", tags=["bugfix"])
logger = logging.getLogger("api.bugfix")


def _sse_event(data: dict) -> str:
    return f"data: {json.dumps(data, ensure_ascii=False)}\n\n"


async def _stream_bugfix_async(
    request: Request,
    message: str,
    git_url: Optional[str],
    git_branch: Optional[str],
    git_commit: Optional[str],
    apply: bool,
    provider: str = "deepseek",
    model: Optional[str] = None,
    uploaded_file_names: Optional[List[str]] = None,
    session_id: Optional[str] = None,
    history: Optional[List[dict]] = None,
) -> AsyncIterator[str]:
    """流式 Bugfix：拉取工具上下文 + 调用大模型 SSE 流式输出。仅将 uploaded_file_names 中的文件纳入可读上下文。"""
    repo_dir: Optional[Path] = None
    repo_name = repo_name_from_url(git_url or "")
    ref_display = (git_branch or "").strip() or ((git_commit or "")[:8] if (git_commit or "").strip() else "")
    ref_checkout = (git_commit or "").strip() or (f"origin/{git_branch.strip()}" if (git_branch or "").strip() else "")

    if git_url:
        repo_dir = repo_dir_from_url(git_url)
        if not repo_dir.exists():
            yield _sse_event({"type": "error", "content": f"本地未找到仓库缓存目录：{repo_dir}"})
            yield _sse_event({"type": "done", "ok": False, "message": "repo not found"})
            return

        if ref_checkout:
            ok, err = git_checkout(repo_dir, ref_checkout)
            if not ok:
                yield _sse_event({"type": "error", "content": f"切换到 {ref_checkout} 失败：{err}"})
                yield _sse_event({"type": "done", "ok": False, "message": "git checkout failed"})
                return

    project_name = repo_name
    names_for_ctx = uploaded_file_names if uploaded_file_names is not None else None
    user_parts = [f"用户问题：{message}"]
    if git_url:
        user_parts.append(f"目标仓库：{git_url}")
    if ref_display:
        user_parts.append(f"目标版本：{ref_display}")
    user_content = "\n".join(user_parts)

    timeout_sec = int(os.environ.get("BUGFIX_STREAM_TIMEOUT_SEC", "480") or "480")

    # 收集完整 AI 文本，用于结构化提取
    full_ai_text: List[str] = []

    bugfix_repo_enter(repo_name)
    try:
        async with asyncio.timeout(timeout_sec):
            async for ev_type, payload in run_bugfix_agent(
                BUG_EXPERT_SYSTEM_PROMPT,
                user_content,
                project_name=project_name or None,
                ref=ref_display or None,
                allowed_file_names=names_for_ctx,
                session_id=session_id or None,
                provider=provider if provider in ("deepseek", "claude") else "deepseek",
                model=model or None,
                history=history or [],
            ):
                if await request.is_disconnected():
                    return
                if ev_type == "tool":
                    kind = payload.get("kind", "")
                    if kind in ("list_uploaded_files", "file_list"):
                        continue
                    out = {"type": "tool", "kind": kind, "title": payload.get("title", ""), "content": payload.get("content", "")}
                    if payload.get("summary") is not None:
                        out["summary"] = payload.get("summary")
                    yield _sse_event(out)
                elif ev_type == "analysis_chain":
                    yield _sse_event({"type": "analysis_chain", "steps": payload.get("steps", [])})
                elif ev_type == "text_chunk":
                    content = payload.get("content") or ""
                    if content:
                        full_ai_text.append(content)
                        yield _sse_event({"type": "text_chunk", "content": content})
                elif ev_type == "text":
                    content = (payload.get("content") or "").strip()
                    if content:
                        full_ai_text.append(content)
                        yield _sse_event({"type": "text", "content": content})
    except asyncio.CancelledError:
        return
    except BugInfoUnavailableError as e:
        yield _sse_event({"type": "error", "content": str(e)})
        yield _sse_event({"type": "done", "ok": False, "message": "bug_info_unavailable"})
        return
    except TimeoutError:
        yield _sse_event({"type": "error", "content": f"请求超时：超过 {timeout_sec}s 未完成（已中止）。"})
        yield _sse_event({"type": "done", "ok": False, "message": "timeout"})
        return
    except Exception as e:
        yield _sse_event({"type": "error", "content": str(e)})
        yield _sse_event({"type": "done", "ok": False, "message": str(e)})
        return
    finally:
        bugfix_repo_exit(repo_name)

    # ── 结构化提取：把 AI 完整输出整理成 bug_cause / bug_location / fix_plans ──
    if full_ai_text:
        try:
            from api.llm import stream_chat
            combined = "".join(full_ai_text)
            extract_messages = [
                {"role": "system", "content": STRUCTURED_RESULT_PROMPT},
                {"role": "user", "content": f"以下是 AI Bug 分析的完整内容：\n\n{combined}"},
            ]
            json_buf: List[str] = []
            async for chunk in stream_chat(
                extract_messages,
                provider=provider if provider in ("deepseek", "claude") else "deepseek",
                model=model or None,
            ):
                json_buf.append(chunk)
            raw_json = "".join(json_buf).strip()
            # 去掉可能的 markdown 代码块包裹
            if raw_json.startswith("```"):
                raw_json = raw_json.split("```", 2)[1]
                if raw_json.startswith("json"):
                    raw_json = raw_json[4:]
                raw_json = raw_json.rsplit("```", 1)[0].strip()
            structured = json.loads(raw_json)
            # 过滤掉没有 diff 的修复方案
            if isinstance(structured.get("fix_plans"), list):
                structured["fix_plans"] = [
                    p for p in structured["fix_plans"]
                    if isinstance(p, dict) and str(p.get("diff", "")).strip()
                ]
            yield _sse_event({"type": "structured_result", "data": structured})
        except Exception as e:
            logger.warning("结构化提取失败（不影响主流程）: %s", e)

    yield _sse_event({"type": "done", "ok": True, "message": "分析完成"})


async def _stream_bugfix_test_async(
    message: str,
    git_url: Optional[str],
    git_branch: Optional[str],
    git_commit: Optional[str],
) -> AsyncIterator[str]:
    """测试用 SSE：不调用大模型，直接模拟事件流。"""
    ref_display = (git_branch or "").strip() or ((git_commit or "")[:8] if (git_commit or "").strip() else "")

    yield _sse_event({"type": "tool", "kind": "file_read", "title": "读取文件：bugfix-context-Example-1.csv", "content": "colA,colB\nfoo,bar\nbaz,qux\n..."})
    await asyncio.sleep(0.4)
    yield _sse_event({"type": "tool", "kind": "read_source_file", "title": "读取源代码文件", "content": "```java\nsrc/main/java/com/timevale/forward/service/controller/BugfixController.java\n```"})
    await asyncio.sleep(0.4)
    yield _sse_event({"type": "tool", "kind": "reasoning", "title": "测试：生成模拟回复", "content": f"收到问题：{message or '-'}"})
    await asyncio.sleep(0.6)
    yield _sse_event({"type": "analysis_chain", "steps": [
        {"type": "problem", "label": "拿到问题", "detail": "用户描述的现象或报错信息"},
        {"type": "query_code_graph", "label": "查图", "detail": "查询 ProjectServiceImpl.processFlow 方法"},
        {"type": "step", "label": "结论", "detail": "定位到 NPE 根因"},
    ]})
    await asyncio.sleep(0.2)

    chunks = ["好的，我进入「测试回复」模式。\n", "流式渲染测试正常。\n", "测试完成！"]
    for c in chunks:
        yield _sse_event({"type": "text_chunk", "content": c})
        await asyncio.sleep(0.15)

    # 测试结构化结果
    yield _sse_event({"type": "structured_result", "data": {
        "bug_cause": "processFlow 入参未做 null 校验，导致 NPE",
        "bug_location": [{"file": "src/main/java/com/example/ProjectServiceImpl.java", "line_range": "142", "description": "入参未校验"}],
        "fix_plans": [{"id": 1, "title": "增加 null 校验", "description": "在调用前判断入参是否为 null", "diff": "--- a/src/main/java/com/example/ProjectServiceImpl.java\n+++ b/src/main/java/com/example/ProjectServiceImpl.java\n@@ -140,6 +140,9 @@\n     public void processFlow(ProjectModifyReq req) {\n+        if (req == null) {\n+            return;\n+        }\n         req.setProjectName(convert(req));\n     }"}],
    }})
    yield _sse_event({"type": "done", "ok": True, "message": "测试完成"})


@router.post("/bugfix/analyze")
def bugfix_analyze(body: BugfixRequest, request: Request):
    """AI Bug 专家流式分析。"""
    return StreamingResponse(
        _stream_bugfix_async(
            request=request,
            message=body.message,
            git_url=body.git_url,
            git_branch=body.git_branch,
            git_commit=body.git_commit,
            apply=body.apply,
            provider=body.provider or "deepseek",
            model=body.model,
            uploaded_file_names=body.uploaded_file_names,
            session_id=body.session_id,
            history=[{"role": m.role, "content": m.content} for m in (body.history or [])],
        ),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/bugfix/test-analyze")
def bugfix_test_analyze(body: BugfixRequest):
    """测试用 AI 回复。"""
    return StreamingResponse(
        _stream_bugfix_test_async(
            message=body.message,
            git_url=body.git_url,
            git_branch=body.git_branch,
            git_commit=body.git_commit,
        ),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/bugfix/tool-test")
async def bugfix_tool_test(body: dict):
    """
    单工具直接调用测试接口。
    body: { tool: str, arguments: dict, project_name?: str, session_id?: str }
    """
    from api.agent import _execute_tool, BugInfoUnavailableError

    tool_name = str(body.get("tool") or "").strip()
    arguments = body.get("arguments") or {}
    project_name = str(body.get("project_name") or "").strip() or None
    session_id = str(body.get("session_id") or "").strip() or None

    if not tool_name:
        return {"ok": False, "error": "缺少 tool 参数"}

    try:
        result = _execute_tool(
            tool_name,
            arguments,
            project_name=project_name,
            session_id=session_id,
        )
        return {"ok": True, "tool": tool_name, "result": result}
    except BugInfoUnavailableError as e:
        return {"ok": False, "tool": tool_name, "error": str(e)}
    except Exception as e:
        return {"ok": False, "tool": tool_name, "error": str(e)}

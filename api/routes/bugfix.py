"""
Bugfix 流式分析接口：SSE 推送 AI 分析结果与工具调用事件。
"""
from __future__ import annotations

import asyncio
import json
import os
from pathlib import Path
from typing import AsyncIterator, List, Optional

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from api.agent import run_bugfix_agent
from api.prompts import BUG_EXPERT_SYSTEM_PROMPT
from api.schemas import BugfixRequest
from api.runtime_state import bugfix_repo_enter, bugfix_repo_exit
from api.utils import repo_dir_from_url, repo_name_from_url, git_checkout

router = APIRouter(prefix="/api", tags=["bugfix"])


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

    # 兜底：防止模型/工具链长时间无响应导致前端一直卡在最后一个 tool
    # 可通过环境变量 BUGFIX_STREAM_TIMEOUT_SEC 调整（默认 480s，长分析建议 600+）
    timeout_sec = int(os.environ.get("BUGFIX_STREAM_TIMEOUT_SEC", "480") or "480")

    bugfix_repo_enter(repo_name)
    try:
        async with asyncio.timeout(timeout_sec):
            async for ev_type, payload in run_bugfix_agent(
                BUG_EXPERT_SYSTEM_PROMPT,
                user_content,
                project_name=project_name or None,
                ref=ref_display or None,
                allowed_file_names=names_for_ctx,
                provider=provider if provider in ("deepseek", "claude") else "deepseek",
                model=model or None,
                history=history or [],
            ):
                # 客户端已断开（前端点了“结束”或网络断开）→ 尽快退出，释放模型/工具链资源
                if await request.is_disconnected():
                    return
                if ev_type == "tool":
                    # 真实发送时不展示“读取文件列表”工具步骤（避免噪音/重复），但保留其它工具与思考过程
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
                        yield _sse_event({"type": "text_chunk", "content": content})
                elif ev_type == "text":
                    content = (payload.get("content") or "").strip()
                    if content:
                        yield _sse_event({"type": "text", "content": content})
    except asyncio.CancelledError:
        # StreamingResponse 在客户端断开时会取消任务，这里吞掉即可，避免噪音堆栈
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

    yield _sse_event({"type": "done", "ok": True, "message": "分析完成"})


async def _stream_bugfix_test_async(
    message: str,
    git_url: Optional[str],
    git_branch: Optional[str],
    git_commit: Optional[str],
) -> AsyncIterator[str]:
    """
    测试用 SSE：不调用大模型，直接模拟一段 tool + 流式文本 + done 事件，
    用于验证前端流式渲染与 loading 状态机。
    """
    import asyncio

    ref_display = (git_branch or "").strip() or ((git_commit or "")[:8] if (git_commit or "").strip() else "")

    # 1) 读取上传文件
    yield _sse_event(
        {
            "type": "tool",
            "kind": "file_read",
            "title": "读取文件：bugfix-context-Example-1.csv",
            "content": "colA,colB\nfoo,bar\nbaz,qux\n...",
        }
    )
    await asyncio.sleep(0.4)

    # 3) 合并两个源码文件到同一个 tool step
    yield _sse_event(
        {
            "type": "tool",
            "kind": "read_source_file",
            "title": "读取源代码文件",
            "content": (
                "```java\n"
                "src/main/java/com/timevale/forward/service/controller/BugfixController.java  (lines 12-34 / 120)\n"
                "  12: @RestController\n"
                "  13: public class BugfixController {\n"
                "  14:   // ...\n"
                "  33: }\n"
                "```\n"
                "```typescript\n"
                "frontend/src/pages/BugfixChatPage.vue  (lines 200-245 / 450)\n"
                "  200: async function runBugfix(userMessage?: string) {\n"
                "  201:   // ...\n"
                "  245: }\n"
                "```\n"
            ),
        }
    )
    await asyncio.sleep(0.4)

    # 4) reasoning
    yield _sse_event(
        {
            "type": "tool",
            "kind": "reasoning",
            "title": "测试：生成模拟回复",
            "content": f"收到问题：{message or '-'}\n\n"
            f"{'目标仓库：' + git_url if git_url else '未提供仓库'}\n"
            f"{'目标版本：' + ref_display if ref_display else '未提供版本'}",
        }
    )
    await asyncio.sleep(0.6)

    # 4.5) 分析链路（测试用，便于验证前端展示）
    yield _sse_event(
        {
            "type": "analysis_chain",
            "steps": [
                {"type": "problem", "label": "拿到问题", "detail": "用户描述的现象或报错信息，需要定位根因并给出修复建议"},
                {"type": "query_code_graph", "label": "查图", "detail": "在图谱中查询 ProjectServiceImpl.processFlow 方法的定义与调用关系，定位实现位置"},
                {"type": "read_uploaded_file", "label": "查文件", "detail": "读取用户上传的 bugfix-context-Example-1.csv，确认第 19 行报错内容与上下文"},
                {"type": "read_source_file", "label": "读源码", "detail": "查看 BugfixController.java 第 12-34 行与 BugfixChatPage.vue 第 200-245 行实现"},
                {"type": "step", "label": "结论", "detail": "综合图谱与源码，给出 NPE 根因与修改建议（含文件与行号）"},
            ],
        }
    )
    await asyncio.sleep(0.2)

    # 5) 流式文本块，每块之间加延迟模拟打字效果
    chunks = [
        "好的，我进入「测试回复」模式。\n",
        "下面我展示一组模拟的工具调用事件：包括读取上传文件、读取源码文件（带行号范围），以及折叠面板里的标签渲染效果。\n",
        "我会模拟后端的 SSE 输出，看看你前端的流式渲染和动画是否正常。\n",
        "如果看到这里连续出现文本块，说明 `text_chunk`/`done` 流程工作正常。\n",
        "测试完成！你可以继续发送真实请求。",
    ]
    for c in chunks:
        yield _sse_event({"type": "text_chunk", "content": c})
        await asyncio.sleep(0.15)

    yield _sse_event({"type": "done", "ok": True, "message": "测试完成"})



@router.post("/bugfix/analyze")
def bugfix_analyze(body: BugfixRequest, request: Request):
    """
    AI Bug 专家流式分析：调用大模型（DeepSeek/Claude）+ 图库与上传文件上下文，以 SSE 流式返回回答。
    """
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
            history=[{"role": m.role, "content": m.content} for m in (body.history or [])],
        ),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.post("/bugfix/test-analyze")
def bugfix_test_analyze(body: BugfixRequest):
    """
    测试用 AI 回复：返回固定的 SSE 事件流，不依赖 git 缓存、不调用大模型。
    """
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

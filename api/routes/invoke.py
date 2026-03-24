"""
统一云函数调度网关。

前端只需发送一个请求：
    POST /api/invoke
    {
        "client": "forward",        // 必填：client 名称
        "function": "listOnlineBugs", // 必填：云函数名称
        "params": { ... }           // 可选：透传给函数的参数
    }

后端根据 (client, function) 查找注册表，执行对应函数并返回：
    {
        "ok": true,
        "data": <函数返回值>
    }
    或
    {
        "ok": false,
        "error": "错误信息"
    }

额外接口：
    GET /api/invoke/schema   — 列出所有已注册的云函数及参数文档
"""

from __future__ import annotations

import asyncio
import logging
from typing import Any, Optional

from fastapi import APIRouter, Header, HTTPException, Request
from pydantic import BaseModel, Field

# 触发各 client 模块的注册代码
import api.clients.forward_client  # noqa: F401

from api.clients.registry import InvokeContext, registry

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/invoke", tags=["invoke"])


# ─── Schema ───────────────────────────────────────────────────────────────────

class InvokeRequest(BaseModel):
    client: str = Field(..., description="客户端名称，如 forward")
    function: str = Field(..., description="云函数名称，如 listOnlineBugs")
    params: dict = Field(default_factory=dict, description="传给函数的参数，任意 JSON 对象")


class InvokeResponse(BaseModel):
    ok: bool
    data: Any = None
    error: Optional[str] = None


class FnSchemaItem(BaseModel):
    client: str
    function: str
    description: str
    params_doc: dict[str, str]


class SchemaResponse(BaseModel):
    functions: list[FnSchemaItem]


# ─── 主调度接口 ───────────────────────────────────────────────────────────────

@router.post("", response_model=InvokeResponse, summary="统一云函数调度")
async def invoke(
    req: InvokeRequest,
    x_token: Optional[str] = Header(default=None, alias="X-Token"),
) -> Any:
    """
    通过 client + function 两个必填参数调度后端注册的云函数。

    - **client**  ：目标系统名称（如 `forward`）
    - **function** ：云函数名称（如 `listOnlineBugs`、`searchOnlineBugs`）
    - **params**   ：透传给函数的参数对象（可选）

    认证：通过请求头 `X-Token` 透传 Bearer Token。
    """
    entry = registry.get(req.client, req.function)
    if entry is None:
        known = [
            f"{e.client_name}.{e.fn_name}"
            for e in registry.list_entries()
        ]
        raise HTTPException(
            status_code=404,
            detail=(
                f"未找到云函数 {req.client}.{req.function}，"
                f"已注册：{known}"
            ),
        )

    context = InvokeContext(
        client_name=req.client,
        fn_name=req.function,
        token=x_token,
    )

    try:
        result = await asyncio.get_event_loop().run_in_executor(
            None,
            lambda: entry.fn(req.params, context=context),
        )
        return InvokeResponse(ok=True, data=result)
    except Exception as e:
        logger.exception("invoke %s.%s failed", req.client, req.function)
        return InvokeResponse(ok=False, error=str(e))


# ─── Schema 文档接口 ──────────────────────────────────────────────────────────

@router.get("/schema", response_model=SchemaResponse, summary="查询所有已注册云函数")
async def get_schema() -> Any:
    """列出所有通过 @registry.register 注册的云函数及其参数说明。"""
    items = [
        FnSchemaItem(
            client=e.client_name,
            function=e.fn_name,
            description=e.description,
            params_doc=e.params_doc,
        )
        for e in registry.list_entries()
    ]
    return SchemaResponse(functions=items)

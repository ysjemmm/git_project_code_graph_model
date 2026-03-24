"""
云函数注册中心。

设计：
    每个外部系统 Client（forward / jira / ...）在自己的模块里用
    @fn_register(client_name, fn_name) 将可调函数注册进来。

    调度网关 /api/invoke 收到请求后，通过
        (client_name, fn_name) → FnEntry
    查找对应函数并执行。

注册函数签名约定：
    def my_fn(params: dict, *, context: InvokeContext) -> Any: ...

    - params   : 前端传入的任意 JSON 对象（已解析为 dict）
    - context  : 请求上下文（token、request_id 等）
    - 返回值    : 任意可序列化对象，最终包在 { ok: true, data: ... } 里返回
"""

from __future__ import annotations

import inspect
import logging
from dataclasses import dataclass, field
from typing import Any, Callable, Optional

logger = logging.getLogger(__name__)

# ─── InvokeContext ────────────────────────────────────────────────────────────

@dataclass
class InvokeContext:
    """请求级上下文，由网关层构造后注入到每个函数调用。"""
    client_name: str
    fn_name: str
    token: Optional[str] = None         # Bearer Token（由前端请求头透传）
    request_id: Optional[str] = None    # 可选追踪 ID


# ─── FnEntry ─────────────────────────────────────────────────────────────────

@dataclass
class FnEntry:
    """注册表中的单条函数记录。"""
    client_name: str
    fn_name: str
    fn: Callable[[dict, InvokeContext], Any]
    description: str = ""
    # 参数文档（可选），key=参数名, value=说明
    params_doc: dict[str, str] = field(default_factory=dict)


# ─── Registry ─────────────────────────────────────────────────────────────────

class FnRegistry:
    """全局函数注册表（单例）。"""

    def __init__(self):
        # { client_name: { fn_name: FnEntry } }
        self._table: dict[str, dict[str, FnEntry]] = {}

    def register(
        self,
        client_name: str,
        fn_name: str,
        *,
        description: str = "",
        params_doc: Optional[dict[str, str]] = None,
    ) -> Callable:
        """
        装饰器：将函数注册到注册表。

        用法：
            @registry.register("forward", "listOnlineBugs",
                                description="查询线上 Bug 分页列表",
                                params_doc={"name": "标题关键词", "page": "页码"})
            def list_online_bugs(params: dict, *, context: InvokeContext) -> Any:
                ...
        """
        def decorator(fn: Callable) -> Callable:
            entry = FnEntry(
                client_name=client_name,
                fn_name=fn_name,
                fn=fn,
                description=description,
                params_doc=params_doc or {},
            )
            self._table.setdefault(client_name, {})[fn_name] = entry
            logger.debug("fn_registry: registered %s.%s", client_name, fn_name)
            return fn
        return decorator

    def get(self, client_name: str, fn_name: str) -> Optional[FnEntry]:
        return self._table.get(client_name, {}).get(fn_name)

    def list_entries(self) -> list[FnEntry]:
        """列出所有已注册函数（用于 /api/invoke/schema 等文档接口）。"""
        result = []
        for client_map in self._table.values():
            result.extend(client_map.values())
        return result

    def client_names(self) -> list[str]:
        return list(self._table.keys())


# ─── 全局单例 ─────────────────────────────────────────────────────────────────

registry = FnRegistry()

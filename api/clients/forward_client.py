"""
ForwardClient — 产研系统（forward）HTTP 对接客户端。

基于 Http2Client 封装，统一处理：
- BaseResult / BusinessResult 通用响应拆包
- 分页响应 PageQueryResult 解包
- 认证头透传（Bearer Token 或 Cookie）

使用方式（单次请求，无连接池复用）：
    client = ForwardClient.from_env()
    bugs = client.list_bugs(name="NPE", page=1, page_size=20)

使用方式（长期持有连接池，推荐在 FastAPI lifespan 中管理）：
    client = ForwardClient.from_env()
    client.open()
    try:
        ...
    finally:
        client.close()
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Any, Optional

from .http2_client import Http2Client, Http2ClientConfig, HttpClientError, RetryConfig

# ─── 响应类型 ─────────────────────────────────────────────────────────────────

@dataclass
class PageResult:
    """分页查询结果。"""
    total: int
    page: int
    page_size: int
    items: list[dict]


@dataclass
class BugItem:
    """线上 Bug 简要信息（前端用）。"""
    id: int
    name: str
    status: Optional[int] = None
    status_name: Optional[str] = None
    priority_name: Optional[str] = None
    proposer: Optional[str] = None
    proposer_id: Optional[str] = None
    operator: Optional[str] = None
    operator_id: Optional[str] = None
    env_name: Optional[str] = None
    belong_name: Optional[str] = None
    category_name: Optional[str] = None
    source_name: Optional[str] = None
    create_date: Optional[str] = None
    modify_date: Optional[str] = None
    product_line_names: Optional[list[str]] = None
    biz_domain_names: Optional[list[str]] = None
    describe: Optional[str] = None
    sla_remain_hours: Optional[float] = None

    @classmethod
    def from_vo(cls, vo: dict) -> "BugItem":
        return cls(
            id=vo.get("id", 0),
            name=vo.get("name") or "",
            status=vo.get("status"),
            status_name=vo.get("statusName"),
            priority_name=vo.get("priorityName"),
            proposer=vo.get("proposer"),
            proposer_id=vo.get("proposerId"),
            operator=vo.get("operator"),
            operator_id=vo.get("operatorId"),
            env_name=vo.get("envName"),
            belong_name=vo.get("belongName"),
            category_name=vo.get("categoryName"),
            source_name=vo.get("sourceName"),
            create_date=_fmt_date(vo.get("createDate")),
            modify_date=_fmt_date(vo.get("modifyDate")),
            product_line_names=vo.get("productLineNameList") or [],
            biz_domain_names=vo.get("bizDomainNameList") or [],
            describe=vo.get("describe"),
            sla_remain_hours=vo.get("slaRemainHours"),
        )

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "status": self.status,
            "statusName": self.status_name,
            "priorityName": self.priority_name,
            "proposer": self.proposer,
            "proposerId": self.proposer_id,
            "operator": self.operator,
            "operatorId": self.operator_id,
            "envName": self.env_name,
            "belongName": self.belong_name,
            "categoryName": self.category_name,
            "sourceName": self.source_name,
            "createDate": self.create_date,
            "modifyDate": self.modify_date,
            "productLineNames": self.product_line_names,
            "bizDomainNames": self.biz_domain_names,
            "describe": self.describe,
            "slaRemainHours": self.sla_remain_hours,
        }


def _fmt_date(val: Any) -> Optional[str]:
    """将时间戳（毫秒整数）或日期字符串统一转为 'YYYY-MM-DD HH:MM' 字符串。"""
    if val is None:
        return None
    if isinstance(val, (int, float)):
        import datetime
        try:
            dt = datetime.datetime.utcfromtimestamp(val / 1000)
            return dt.strftime("%Y-%m-%d %H:%M")
        except Exception:
            return str(val)
    return str(val)


# ─── 响应解包工具 ──────────────────────────────────────────────────────────────

def _unwrap(resp: dict, context: str = "") -> Any:
    """
    拆包 BaseResult / BusinessResult 通用响应格式。
    返回 data / result 字段；失败时抛出 HttpClientError。
    """
    # forward 系统常见格式：{ "success": true/false, "data": ..., "msg": ... }
    # 也有 businessResult 格式：{ "code": 0, "msg": ..., "result": ... }
    # 也有 { "code": "OK", "data": ..., "message": ... } 格式
    if not isinstance(resp, dict):
        return resp

    def _is_success_code(code: Any) -> bool:
        """判断 code 是否表示成功：0、None、'OK'、'ok'、'SUCCESS'、'success' 均视为成功。"""
        if code is None or code == 0:
            return True
        if isinstance(code, str) and code.upper() in ("OK", "SUCCESS"):
            return True
        return False

    # 外层 { code, message, data } 格式（实际接口格式）
    if "code" in resp and "data" in resp:
        code = resp.get("code")
        if not _is_success_code(code):
            msg = resp.get("message") or resp.get("msg") or f"forward 系统返回 code={code}"
            raise HttpClientError(f"{context}: {msg}")
        return resp.get("data")

    # BaseResult 格式 { success, data, msg }
    if "success" in resp:
        if not resp.get("success"):
            msg = resp.get("msg") or resp.get("message") or "forward 系统返回 success=false"
            raise HttpClientError(f"{context}: {msg}")
        return resp.get("data")

    # BusinessResult 格式 { code, msg, result }
    if "code" in resp:
        code = resp.get("code")
        if not _is_success_code(code):
            msg = resp.get("msg") or resp.get("message") or f"forward 系统返回 code={code}"
            raise HttpClientError(f"{context}: {msg}")
        return resp.get("result")

    # 未知格式直接返回
    return resp


# ─── ForwardClient ───────────────────────────────────────────────────────────

class ForwardClient:
    """
    产研系统 forward 的 HTTP 客户端。

    所有业务方法均为同步调用，适合在 FastAPI run_in_executor / 线程池中使用。
    """

    def __init__(self, http_client: Http2Client):
        self._http = http_client

    # ── 生命周期 ──────────────────────────────────────────────────────────────

    def open(self) -> None:
        self._http.open()

    def close(self) -> None:
        self._http.close()

    def __enter__(self) -> "ForwardClient":
        self.open()
        return self

    def __exit__(self, *args) -> None:
        self.close()

    # ── 工厂方法 ──────────────────────────────────────────────────────────────

    @classmethod
    def from_env(cls, *, token: Optional[str] = None) -> "ForwardClient":
        """
        从环境变量构建客户端。

        环境变量：
            FORWARD_API_BASE   产研系统 base URL，必须设置
            FORWARD_API_TOKEN  Bearer Token（可选，优先使用参数 token）
        """
        base_url = os.environ.get("FORWARD_API_BASE", "").rstrip("/")
        if not base_url:
            raise RuntimeError(
                "环境变量 FORWARD_API_BASE 未设置，"
                "请在 .env.local 中配置产研系统地址"
            )
        api_token = token or os.environ.get("FORWARD_API_TOKEN")
        api_cookie = os.environ.get("FORWARD_API_COOKIE")

        cfg = Http2ClientConfig(
            base_url=base_url,
            bearer_token=api_token,
            cookie=api_cookie,
            timeout_sec=15.0,
            read_timeout_sec=20.0,
            verify_tls=False,           # 内网自签证书场景
            follow_redirects=True,      # 跟随 302 跳转
            retry=RetryConfig(
                max_attempts=2,
                retryable_methods={"GET"},  # POST 不重试，避免重复写入
            ),
        )
        return cls(Http2Client(cfg))

    # ── Bug 相关接口 ──────────────────────────────────────────────────────────

    def list_bugs(
        self,
        *,
        name: Optional[str] = None,
        status: Optional[list[int]] = None,
        page: int = 1,
        page_size: int = 20,
        ascription: Optional[str] = None,
        product_line_ids: Optional[list[int]] = None,
        biz_domain_ids: Optional[list[int]] = None,
        contain_label: bool = False,
        extra: Optional[dict] = None,
    ) -> PageResult:
        """
        查询线上 Bug 列表（分页）。

        实际接口：POST /bugOnline/list

        Args:
            name:             Bug 标题模糊搜索
            status:           Bug 状态列表（整数）
            page:             当前页（1-based）
            page_size:        每页条数
            ascription:       归属范围 ALL/CURRENT_USER/TEAM 等，默认 ALL
            product_line_ids: 产品线 id 列表
            biz_domain_ids:   业务域 id 列表
            contain_label:    是否包含标签
            extra:            透传额外字段

        Returns:
            PageResult 含 items 列表，每项为 BugItem.to_dict()
        """
        payload: dict[str, Any] = {
            "pageNum": page,
            "pageSize": page_size,
            "ascription": ascription or "ALL",
        }
        if name:
            payload["name"] = name
        if status:
            payload["status"] = status
        if contain_label:
            payload["containLabel"] = contain_label
        if product_line_ids:
            payload["productLineIdList"] = product_line_ids
        if biz_domain_ids:
            payload["bizDomainIdList"] = biz_domain_ids
        if extra:
            payload.update(extra)

        raw = self._http.post_json("/bugOnline/list", json_body=payload)
        data = _unwrap(raw, context="list_bugs")

        # data.pageQueryResult 包含分页数据
        if data is None:
            return PageResult(total=0, page=page, page_size=page_size, items=[])

        pqr: dict = data.get("pageQueryResult") or {}
        items_raw: list[dict] = pqr.get("resultList") or []
        total: int = pqr.get("totalItems") or len(items_raw)
        items = [BugItem.from_vo(vo).to_dict() for vo in items_raw]
        return PageResult(
            total=total,
            page=pqr.get("currentPage", page),
            page_size=pqr.get("itemsPerPage", page_size),
            items=items,
        )

    def get_bug(self, bug_id: int) -> dict:
        """
        查询线上 Bug 详情。

        对应 facade: BugOnlineService.get(BugOnlineDetailReq)
        HTTP: POST /bugOnline/get
        """
        raw = self._http.post_json("/bugOnline/get", json_body={"id": bug_id})
        data = _unwrap(raw, context=f"get_bug({bug_id})")
        return data or {}

    def search_bugs_by_name(
        self,
        name: str,
        *,
        page: int = 1,
        page_size: int = 20,
    ) -> PageResult:
        """
        按标题模糊搜索 Bug（便捷方法，封装 list_bugs）。
        """
        return self.list_bugs(name=name, page=page, page_size=page_size)

    def get_user_info(self) -> dict:
        """
        获取当前登录用户信息。

        实际接口：GET https://testmanage.esign.cn/user/infor
        注意：该接口不在 forward 路径下，需要用根域名请求。
        依赖 Cookie 认证（由 CAS 后端回调写入）。

        Returns:
            包含 id, name, alias, mail 等字段的用户信息字典
        """
        # FORWARD_API_BASE = https://testmanage.esign.cn/infocenter-manager/forward
        # /user/infor 在根域名下，需要提取根域名部分
        from urllib.parse import urlparse
        base_url = os.environ.get("FORWARD_API_BASE", "").rstrip("/")
        parsed = urlparse(base_url)
        root_url = f"{parsed.scheme}://{parsed.netloc}"

        cookie = os.environ.get("FORWARD_API_COOKIE")
        cfg = Http2ClientConfig(
            base_url=root_url,
            cookie=cookie,
            timeout_sec=10.0,
            verify_tls=False,
            follow_redirects=True,
        )
        with Http2Client(cfg) as client:
            raw = client.get_json("/user/infor")
        # /user/infor 直接返回用户对象，无外层包装，不走 _unwrap
        return raw if isinstance(raw, dict) else {}


# ─── 云函数注册 ───────────────────────────────────────────────────────────────
# 注册到全局调度注册表，供 /api/invoke 网关调用。
# 每个函数签名：fn(params: dict, *, context: InvokeContext) -> Any

from .registry import registry, InvokeContext  # noqa: E402


@registry.register(
    "forward", "listOnlineBugs",
    description="查询线上 Bug 分页列表",
    params_doc={
        "name":            "标题模糊搜索（可选）",
        "status":          "状态列表，整数数组（可选）",
        "page":            "页码，默认 1",
        "page_size":       "每页条数，默认 20",
        "ascription":      "归属范围 ALL/CURRENT_USER/TEAM（可选）",
        "product_line_ids": "产品线 id 列表（可选）",
        "biz_domain_ids":  "业务域 id 列表（可选）",
    },
)
def _fn_list_online_bugs(params: dict, *, context: InvokeContext) -> dict:
    with ForwardClient.from_env(token=context.token) as c:
        result = c.list_bugs(
            name=params.get("name"),
            status=params.get("status"),
            page=int(params.get("page", 1)),
            page_size=int(params.get("page_size", 20)),
            ascription=params.get("ascription"),
            product_line_ids=params.get("product_line_ids"),
            biz_domain_ids=params.get("biz_domain_ids"),
        )
    return {
        "total": result.total,
        "page": result.page,
        "page_size": result.page_size,
        "items": result.items,
    }


@registry.register(
    "forward", "getOnlineBug",
    description="查询线上 Bug 详情",
    params_doc={"id": "Bug ID（整数，必填）"},
)
def _fn_get_online_bug(params: dict, *, context: InvokeContext) -> dict:
    bug_id = int(params["id"])
    with ForwardClient.from_env(token=context.token) as c:
        return c.get_bug(bug_id)


@registry.register(
    "forward", "searchOnlineBugs",
    description="按标题关键词搜索线上 Bug",
    params_doc={
        "q":         "搜索关键词（必填）",
        "page":      "页码，默认 1",
        "page_size": "每页条数，默认 20",
    },
)
def _fn_search_online_bugs(params: dict, *, context: InvokeContext) -> dict:
    with ForwardClient.from_env(token=context.token) as c:
        result = c.search_bugs_by_name(
            params["q"],
            page=int(params.get("page", 1)),
            page_size=int(params.get("page_size", 20)),
        )
    return {
        "total": result.total,
        "page": result.page,
        "page_size": result.page_size,
        "items": result.items,
    }


@registry.register(
    "forward", "getUserInfo",
    description="获取当前登录用户信息（依赖 CAS Cookie 认证）",
    params_doc={},
)
def _fn_get_user_info(params: dict, *, context: InvokeContext) -> dict:
    with ForwardClient.from_env(token=context.token) as c:
        return c.get_user_info()

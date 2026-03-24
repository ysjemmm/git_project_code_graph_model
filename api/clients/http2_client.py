"""
通用 HTTP/2 Client（面向内部系统 API 对接）。

设计目标：
- 强制开启 HTTP/2（基于 httpx）
- 统一超时、重试、错误处理
- 便于后续按系统封装具体 Client（继承或组合）
"""

from __future__ import annotations

import random
import time
from dataclasses import dataclass, field
from typing import Any, Mapping, Optional, Iterable

try:
    import httpx
except Exception:  # pragma: no cover - 运行环境无 httpx 时给出可读错误
    httpx = None  # type: ignore


import logging as _logging
_logger = _logging.getLogger(__name__)


class HttpClientError(RuntimeError):
    """HTTP 客户端统一异常。"""

    def __init__(
        self,
        message: str,
        *,
        method: str = "",
        url: str = "",
        status_code: Optional[int] = None,
        response_text: Optional[str] = None,
    ):
        super().__init__(message)
        self.method = method
        self.url = url
        self.status_code = status_code
        self.response_text = response_text


@dataclass(frozen=True)
class RetryConfig:
    """重试策略配置。"""

    max_attempts: int = 3
    initial_backoff_sec: float = 0.15
    max_backoff_sec: float = 1.5
    retryable_status_codes: set[int] = field(
        default_factory=lambda: {408, 429, 500, 502, 503, 504}
    )
    retryable_methods: set[str] = field(
        default_factory=lambda: {"GET", "HEAD", "OPTIONS", "DELETE", "PUT"}
    )
    jitter_ratio: float = 0.2


@dataclass(frozen=True)
class Http2ClientConfig:
    """HTTP/2 客户端配置。"""

    base_url: str
    default_headers: Mapping[str, str] = field(default_factory=dict)
    bearer_token: Optional[str] = None
    bearer_header_name: str = "Authorization"
    cookie: Optional[str] = None
    timeout_sec: float = 10.0
    connect_timeout_sec: float = 3.0
    read_timeout_sec: float = 10.0
    write_timeout_sec: float = 10.0
    pool_timeout_sec: float = 3.0
    verify_tls: bool = True
    follow_redirects: bool = False
    max_connections: int = 100
    max_keepalive_connections: int = 20
    keepalive_expiry_sec: float = 20.0
    retry: RetryConfig = field(default_factory=RetryConfig)


class Http2Client:
    """
    通用 HTTP/2 客户端（同步版）。

    用法：
        cfg = Http2ClientConfig(base_url="https://internal.example.com", bearer_token="xxx")
        with Http2Client(cfg) as c:
            data = c.get_json("/api/ping")
    """

    def __init__(self, config: Http2ClientConfig):
        self.config = config
        self._client: Optional["httpx.Client"] = None

    def __enter__(self) -> "Http2Client":
        self.open()
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.close()

    def open(self) -> None:
        if self._client is not None:
            return
        if httpx is None:
            raise RuntimeError(
                "缺少依赖 httpx。请先安装：pip install 'httpx[http2]'"
            )

        headers = dict(self.config.default_headers or {})
        if self.config.bearer_token:
            headers[self.config.bearer_header_name] = f"Bearer {self.config.bearer_token}"
        if self.config.cookie:
            headers["Cookie"] = self.config.cookie

        timeout = httpx.Timeout(
            timeout=self.config.timeout_sec,
            connect=self.config.connect_timeout_sec,
            read=self.config.read_timeout_sec,
            write=self.config.write_timeout_sec,
            pool=self.config.pool_timeout_sec,
        )
        limits = httpx.Limits(
            max_connections=self.config.max_connections,
            max_keepalive_connections=self.config.max_keepalive_connections,
            keepalive_expiry=self.config.keepalive_expiry_sec,
        )

        self._client = httpx.Client(
            http2=True,
            base_url=self.config.base_url,
            headers=headers,
            timeout=timeout,
            verify=self.config.verify_tls,
            follow_redirects=self.config.follow_redirects,
            limits=limits,
        )

    def close(self) -> None:
        if self._client is not None:
            self._client.close()
            self._client = None

    @property
    def raw_client(self) -> "httpx.Client":
        if self._client is None:
            self.open()
        assert self._client is not None
        return self._client

    def _sleep_backoff(self, attempt: int) -> None:
        cfg = self.config.retry
        if attempt <= 1:
            return
        base = min(cfg.max_backoff_sec, cfg.initial_backoff_sec * (2 ** (attempt - 2)))
        jitter = base * cfg.jitter_ratio
        delay = base + random.uniform(-jitter, jitter)
        time.sleep(max(0.0, delay))

    def _should_retry(self, method: str, status_code: Optional[int], err: Optional[BaseException]) -> bool:
        cfg = self.config.retry
        m = method.upper()
        if m not in cfg.retryable_methods:
            return False
        if err is not None:
            return True
        if status_code is None:
            return False
        return status_code in cfg.retryable_status_codes

    def request(
        self,
        method: str,
        path: str,
        *,
        params: Optional[Mapping[str, Any]] = None,
        json_body: Any = None,
        data: Any = None,
        headers: Optional[Mapping[str, str]] = None,
        expected_statuses: Optional[Iterable[int]] = None,
    ) -> "httpx.Response":
        expected = set(expected_statuses or {200})
        last_err: Optional[BaseException] = None
        last_resp: Optional["httpx.Response"] = None

        attempts = max(1, int(self.config.retry.max_attempts))
        for attempt in range(1, attempts + 1):
            self._sleep_backoff(attempt)
            try:
                resp = self.raw_client.request(
                    method=method.upper(),
                    url=path,
                    params=params,
                    json=json_body,
                    data=data,
                    headers=headers,
                )
                last_resp = resp
                if resp.status_code in expected:
                    return resp

                if not self._should_retry(method, resp.status_code, None) or attempt >= attempts:
                    msg = (
                        f"HTTP 请求失败 method={method.upper()} path={path} "
                        f"status={resp.status_code}"
                    )
                    raise HttpClientError(
                        msg,
                        method=method.upper(),
                        url=str(resp.request.url),
                        status_code=resp.status_code,
                        response_text=resp.text,
                    )
            except HttpClientError:
                raise
            except Exception as e:
                last_err = e
                if not self._should_retry(method, None, e) or attempt >= attempts:
                    raise HttpClientError(
                        f"HTTP 请求异常 method={method.upper()} path={path}: {e}",
                        method=method.upper(),
                        url=path,
                    ) from e

        # 理论上不会到这里，仅做防御
        if last_resp is not None:
            raise HttpClientError(
                f"HTTP 请求失败 method={method.upper()} path={path} status={last_resp.status_code}",
                method=method.upper(),
                url=str(last_resp.request.url),
                status_code=last_resp.status_code,
                response_text=last_resp.text,
            )
        raise HttpClientError(
            f"HTTP 请求失败 method={method.upper()} path={path}: {last_err}",
            method=method.upper(),
            url=path,
        )

    def get_json(
        self,
        path: str,
        *,
        params: Optional[Mapping[str, Any]] = None,
        headers: Optional[Mapping[str, str]] = None,
        expected_statuses: Optional[Iterable[int]] = None,
    ) -> Any:
        resp = self.request(
            "GET",
            path,
            params=params,
            headers=headers,
            expected_statuses=expected_statuses or {200},
        )
        return resp.json()

    def post_json(
        self,
        path: str,
        *,
        json_body: Any = None,
        params: Optional[Mapping[str, Any]] = None,
        headers: Optional[Mapping[str, str]] = None,
        expected_statuses: Optional[Iterable[int]] = None,
    ) -> Any:
        resp = self.request(
            "POST",
            path,
            params=params,
            json_body=json_body,
            headers=headers,
            expected_statuses=expected_statuses or {200, 201},
        )
        if resp.status_code == 204:
            return None
        return resp.json()


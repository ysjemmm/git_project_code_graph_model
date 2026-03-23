from __future__ import annotations

import hashlib
import json
import os
from collections import deque
from datetime import datetime, timedelta
from threading import Lock
from typing import Any, Deque, Dict, List, Optional

from tools.constants import CACHE_NEO4J_OP_LOG_PATH

_LOCK = Lock()
_RECORDS: Deque[Dict[str, Any]] = deque()


def _enabled() -> bool:
    return os.environ.get("NEO4J_OPLOG_ENABLED", "1").strip().lower() not in {"0", "false", "no", "off"}


def _slow_ms() -> int:
    try:
        return max(1, int(os.environ.get("NEO4J_OPLOG_SLOW_MS", "800")))
    except Exception:
        return 800


def _max_records() -> int:
    try:
        return max(100, int(os.environ.get("NEO4J_OPLOG_MAX_RECORDS", "2000")))
    except Exception:
        return 2000


def _truncate_query(query: str, max_len: int = 1200) -> str:
    q = " ".join(str(query or "").split())
    if len(q) <= max_len:
        return q
    return q[: max_len - 3] + "..."


def _safe_params(params: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    src = params or {}
    out: Dict[str, Any] = {}
    for k, v in src.items():
        if isinstance(v, (str, int, float, bool)) or v is None:
            out[str(k)] = v
        elif isinstance(v, list):
            out[str(k)] = f"[list:{len(v)}]"
        elif isinstance(v, dict):
            out[str(k)] = "{...}"
        else:
            out[str(k)] = str(type(v).__name__)
    return out


def record_neo4j_operation(
    *,
    op_type: str,
    query: str,
    parameters: Optional[Dict[str, Any]],
    elapsed_ms: float,
    ok: bool,
    error: str = "",
    row_count: Optional[int] = None,
) -> None:
    if not _enabled():
        return

    slow_threshold = _slow_ms()
    is_slow = elapsed_ms >= slow_threshold
    if ok and not is_slow:
        return

    query_text = _truncate_query(query)
    event = {
        "ts": datetime.now().isoformat(),
        "op_type": op_type,
        "ok": bool(ok),
        "is_slow": bool(is_slow),
        "elapsed_ms": round(float(elapsed_ms), 2),
        "slow_threshold_ms": slow_threshold,
        "row_count": int(row_count or 0),
        "query": query_text,
        "query_hash": hashlib.sha1(query_text.encode("utf-8")).hexdigest()[:12],
        "params": _safe_params(parameters),
        "error": str(error or ""),
    }

    with _LOCK:
        _RECORDS.appendleft(event)
        while len(_RECORDS) > _max_records():
            _RECORDS.pop()

    # 轻量持久化，便于重启后抽样排查
    try:
        CACHE_NEO4J_OP_LOG_PATH.parent.mkdir(parents=True, exist_ok=True)
        with CACHE_NEO4J_OP_LOG_PATH.open("a", encoding="utf-8") as f:
            f.write(json.dumps(event, ensure_ascii=False) + "\n")
    except Exception:
        pass


def list_neo4j_operations(limit: int = 50, only_errors: bool = False, min_elapsed_ms: float = 0) -> List[Dict[str, Any]]:
    lim = max(1, min(500, int(limit or 50)))
    ms = max(0.0, float(min_elapsed_ms or 0))
    with _LOCK:
        arr = list(_RECORDS)
    out: List[Dict[str, Any]] = []
    for x in arr:
        if only_errors and x.get("ok", True):
            continue
        if float(x.get("elapsed_ms") or 0) < ms:
            continue
        out.append(x)
        if len(out) >= lim:
            break
    return out


def summarize_neo4j_operations(window_minutes: int = 60) -> Dict[str, Any]:
    minutes = max(1, int(window_minutes or 60))
    cutoff = datetime.now() - timedelta(minutes=minutes)
    with _LOCK:
        arr = list(_RECORDS)
    in_window = []
    for x in arr:
        ts = str(x.get("ts") or "")
        try:
            dt = datetime.fromisoformat(ts)
        except Exception:
            continue
        if dt >= cutoff:
            in_window.append(x)
    total = len(in_window)
    errors = len([x for x in in_window if not bool(x.get("ok", True))])
    slow = len([x for x in in_window if bool(x.get("is_slow", False))])
    return {
        "window_minutes": minutes,
        "total": total,
        "error_count": errors,
        "slow_count": slow,
    }


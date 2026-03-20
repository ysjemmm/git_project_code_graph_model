from __future__ import annotations

import os
import sys


def setup_console_utf8() -> None:
    """
    让当前进程的控制台输出尽量使用 UTF-8（主要针对 Windows）。

    说明：
    - 只影响当前 Python 进程，不会修改系统全局设置
    - 优先使用 sys.stdout/stderr.reconfigure（py3.7+）
    - 同时补充 PYTHONIOENCODING，避免某些库读取默认编码导致乱码
    """

    # 尽量避免覆盖用户显式设置
    os.environ.setdefault("PYTHONIOENCODING", "utf-8")

    for stream_name in ("stdout", "stderr"):
        stream = getattr(sys, stream_name, None)
        if stream is None:
            continue
        try:
            reconfigure = getattr(stream, "reconfigure", None)
            if callable(reconfigure):
                reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            # 保持原行为：不因编码设置失败而影响主流程
            pass


from __future__ import annotations

import os
from pathlib import Path
from typing import Dict, Iterable, Optional, Set

from tools.constants import PROJECT_ROOT_PATH


def _parse_env_lines(lines: Iterable[str]) -> Dict[str, str]:
    env: Dict[str, str] = {}
    for raw in lines:
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip("'").strip('"')
        if key:
            env[key] = value
    return env


def load_env_vars(keys: Set[str], root: Optional[Path] = None) -> None:
    """
    从项目根目录的 .env.local/.env 中补齐指定环境变量（不覆盖已存在的环境变量）。

    优先级（高 -> 低）：
    1) 系统环境变量
    2) .env.local
    3) .env
    4) 调用者代码默认值（由调用者负责回退）
    """
    if not keys:
        return

    base = root or PROJECT_ROOT_PATH
    try:
        for filename in [".env.local", ".env"]:
            env_path = base / filename
            if not env_path.exists():
                continue
            try:
                content = env_path.read_text(encoding="utf-8")
            except Exception:
                continue
            parsed = _parse_env_lines(content.splitlines())
            if not parsed:
                continue
            for k in keys:
                if k in os.environ:
                    continue
                v = parsed.get(k)
                if v:
                    os.environ[k] = v
    except Exception:
        # 静默忽略，保持调用方原有行为不变
        return


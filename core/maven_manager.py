from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple

from parser.utils.logger import get_logger
from tools.constants import CACHE_MAVEN_PATH

logger = get_logger("maven_manager")


@dataclass(frozen=True, slots=True)
class MavenSettings:
    settings_path: Optional[str] = None

    @staticmethod
    def autodetect() -> "MavenSettings":
        settings = os.getenv("MAVEN_SETTINGS")
        if not settings:
            candidates = [
                r"D:\Env\mvn3.6.3\settings-inner.xml",
                r"D:\Env\maven\settings-inner.xml",
                r"C:\Env\mvn3.6.3\settings-inner.xml",
                r"C:\Env\maven\settings-inner.xml",
            ]
            for p in candidates:
                if Path(p).exists():
                    settings = p
                    break
        return MavenSettings(settings_path=settings or None)


class MavenRunner:
    """
    负责执行 Maven，并将输出流式打印到日志，避免长时间无输出“卡住”。
    """

    @staticmethod
    def run(repo_root: str, args: List[str], *, settings: MavenSettings) -> None:
        mvn_cmd = os.getenv("MAVEN_CMD")
        if not mvn_cmd:
            mvn_cmd = shutil.which("mvn.cmd") or shutil.which("mvn") or "mvn"

        cmd = [mvn_cmd]
        if settings.settings_path:
            cmd += ["-s", settings.settings_path]
        cmd += args

        try:
            logger.info(f"[Maven] 开始执行: {' '.join(cmd)}")
            logger.info(f"[Maven] 工作目录: {repo_root}")
            proc = subprocess.Popen(
                cmd,
                cwd=repo_root,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                bufsize=1,
            )
        except FileNotFoundError as e:
            raise RuntimeError(
                "未找到 Maven 可执行文件。请将 Maven 加入 PATH，或设置环境变量 MAVEN_CMD 指向 mvn/mvn.cmd 的绝对路径。"
                f"\n当前尝试命令: {cmd}"
            ) from e

        output_lines: List[str] = []
        assert proc.stdout is not None
        for line in proc.stdout:
            line = line.rstrip("\n")
            output_lines.append(line)
            logger.info(f"[mvn] {line}")

        returncode = proc.wait()
        if returncode != 0:
            tail = "\n".join(output_lines[-200:])
            raise RuntimeError(
                f"Maven 执行失败（exit={returncode}）\n命令: {' '.join(cmd)}\n输出(末尾200行):\n{tail}"
            )
        logger.info(f"[Maven] 执行完成（exit=0）: {' '.join(args)}")


class MavenIncrementalManager:
    """
    Maven “增量拉取/可跳过”管理：
    - pom.xml / settings 未变：跳过 Maven 依赖拉取（复用 ~/.m2 与既有 jar 索引缓存）
    - 默认加 -nsu，减少每次远端检查 SNAPSHOT
    - 可通过 MAVEN_OFFLINE=1 附加 -o 完全离线
    """

    def __init__(self, *, repo_root: str, repo_cache_key: Optional[str] = None):
        self.repo_root = str(Path(repo_root).resolve())
        # marker 文件名：尽量稳定且不泄露路径（使用 repo_root 的目录名 + hash 做区分）
        root_name = Path(self.repo_root).name
        root_fp = hashlib.sha256(self.repo_root.encode("utf-8")).hexdigest()[:10]
        self.repo_cache_key = repo_cache_key or f"{root_name}-{root_fp}"

    def marker_path(self) -> Path:
        return CACHE_MAVEN_PATH / f".{self.repo_cache_key}.marker.json"

    @staticmethod
    def is_maven_project(repo_root: str) -> bool:
        return Path(repo_root, "pom.xml").exists()

    @staticmethod
    def _sha256_file(path: Path) -> str:
        h = hashlib.sha256()
        with path.open("rb") as f:
            for chunk in iter(lambda: f.read(1024 * 1024), b""):
                h.update(chunk)
        return h.hexdigest()

    @staticmethod
    def _read_text_if_exists(path: Path) -> str:
        try:
            return path.read_text(encoding="utf-8", errors="ignore")
        except Exception:
            return ""

    @staticmethod
    def _git_head_commit(repo_root: str) -> Optional[str]:
        try:
            r = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                cwd=repo_root,
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL,
                text=True,
            )
            if r.returncode == 0:
                v = (r.stdout or "").strip()
                return v or None
        except Exception:
            return None
        return None

    def should_skip(self, *, settings: MavenSettings) -> Tuple[bool, str]:
        pom = Path(self.repo_root) / "pom.xml"
        if not pom.exists():
            return False, "pom.xml 不存在"

        marker = self.marker_path()
        marker.parent.mkdir(parents=True, exist_ok=True)

        pom_hash = self._sha256_file(pom)
        settings_hash = (
            self._sha256_file(Path(settings.settings_path))
            if settings.settings_path and Path(settings.settings_path).exists()
            else ""
        )
        commit_hash = self._git_head_commit(self.repo_root) or ""

        old = {}
        if marker.exists():
            try:
                old = json.loads(self._read_text_if_exists(marker) or "{}") or {}
            except Exception:
                old = {}

        if old.get("pom_hash") != pom_hash:
            return False, "pom.xml 已变化"
        if (old.get("settings_hash") or "") != (settings_hash or ""):
            return False, "Maven settings 已变化"
        if commit_hash and old.get("commit_hash") and old.get("commit_hash") != commit_hash:
            return True, "commit 变化但 pom/settings 未变（跳过 Maven 依赖拉取）"
        return True, "pom/settings 未变（跳过 Maven 依赖拉取）"

    def write_marker(self, *, settings: MavenSettings) -> None:
        pom = Path(self.repo_root) / "pom.xml"
        marker = self.marker_path()
        marker.parent.mkdir(parents=True, exist_ok=True)

        commit_hash = self._git_head_commit(self.repo_root) or ""
        data = {
            "repo_root": self.repo_root,
            "commit_hash": commit_hash,
            "pom_hash": self._sha256_file(pom) if pom.exists() else "",
            "settings_path": settings.settings_path or "",
            "settings_hash": (
                self._sha256_file(Path(settings.settings_path))
                if settings.settings_path and Path(settings.settings_path).exists()
                else ""
            ),
            "ts": time.time(),
        }
        marker.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    @staticmethod
    def maven_base_flags() -> List[str]:
        offline = os.getenv("MAVEN_OFFLINE") in ("1", "true", "TRUE", "yes", "YES")
        # 说明：
        # -DskipTests：跳过运行测试，但仍可能编译测试代码
        # -Dmaven.test.skip=true：连测试编译也跳过（更彻底，适合仅做依赖拉取/类索引的场景）
        flags = ["-DskipTests", "-Dmaven.test.skip=true", "-nsu"]
        if offline:
            flags.append("-o")
        return flags


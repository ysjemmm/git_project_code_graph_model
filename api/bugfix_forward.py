from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Optional, Tuple
from tools.constants import CACHE_GIT_REPOS_PATH


@dataclass(frozen=True, slots=True)
class PatchResult:
    ok: bool
    message: str
    target_file: str
    unified_diff: str
    applied: bool


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def _write_text(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def _unified_diff(old: str, new: str, *, file_path: str) -> str:
    import difflib

    return "".join(
        difflib.unified_diff(
            old.splitlines(keepends=True),
            new.splitlines(keepends=True),
            fromfile=file_path,
            tofile=file_path,
        )
    )


def _find_processflow_block(src: str) -> Optional[Tuple[int, int, str]]:
    """
    定位 processFlow 方法文本块：返回 (start_idx, end_idx, block_text)
    这里用最小的正则定位方法头，再用括号计数切出方法体，避免依赖 Java AST。
    """
    m = re.search(r"\bprivate\s+void\s+processFlow\s*\(\s*ProjectModifyReq\s+\w+\s*\)\s*\{", src)
    if not m:
        return None
    start = m.start()
    i = m.end()  # points after "{"
    depth = 1
    while i < len(src) and depth > 0:
        ch = src[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        i += 1
    end = i
    if depth != 0:
        return None
    return start, end, src[start:end]


def generate_forward_processflow_npe_fix_patch(
    *,
    workspace_root: Path,
    apply: bool = False,
    target_file: Optional[Path] = None,
) -> PatchResult:
    """
    针对线上 NPE：delayType>=1 且 projectNodeFlow=null 时，processFlow 直接解引用导致 NPE。
    默认只生成 diff；apply=True 才会真正写文件。
    """
    target = target_file or (
        CACHE_GIT_REPOS_PATH
        / "forward"
        / "service"
        / "src"
        / "main"
        / "java"
        / "com"
        / "timevale"
        / "forward"
        / "service"
        / "impl"
        / "ProjectServiceImpl.java"
    )
    if not target.exists():
        return PatchResult(
            ok=False,
            message="未找到目标文件（请确认已在缓存仓库目录拉取 forward）",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )

    old = _read_text(target)
    found = _find_processflow_block(old)
    if not found:
        return PatchResult(
            ok=False,
            message="未定位到 processFlow(ProjectModifyReq) 方法",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )
    start, end, block = found

    # 幂等检测：如果已经有 delayType 变量与判空，认为已修复
    if "Integer delayType" in block and "projectNodeFlowDO == null" in block:
        return PatchResult(
            ok=True,
            message="目标方法看起来已包含空值保护，跳过生成补丁",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )

    # 在 block 内做有界替换：仅替换最核心的 if/else 结构的头部两处引用
    # 目标：把 projectModifyReq.getDelayType() 多次解引用改为 delayType，并在 >=1 分支增加 projectNodeFlowDO 判空/补 projectId
    # 采用模式替换，尽量减少对其他逻辑的扰动。
    new_block = block

    # 1) 注入 delayType 局部变量 + 判空（插在第一行 projectNodeDOList 后）
    anchor = "List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());"
    if anchor not in new_block:
        return PatchResult(
            ok=False,
            message="未找到预期锚点行，无法安全生成补丁（源码可能与预期版本不同）",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )
    injection = (
        anchor
        + "\n"
        + "        Integer delayType = projectModifyReq.getDelayType();\n"
        + "        if (delayType == null) {\n"
        + "            log.warn(\"delayType 为空，跳过流程处理，projectId={}\", projectModifyReq.getId());\n"
        + "            return;\n"
        + "        }\n"
    )
    new_block = new_block.replace(anchor, injection, 1)

    # 2) 替换 if 条件使用 delayType
    new_block = new_block.replace("if (projectModifyReq.getDelayType().compareTo(1) >= 0) {", "if (delayType.compareTo(1) >= 0) {", 1)

    # 3) 在 >=1 分支中，projectNodeFlowDO 后插入判空与 projectId 兜底
    flow_line = "ProjectNodeFlowDO projectNodeFlowDO = ProjectNodeFlowCopier.INSTANCE.convert(projectModifyReq.getProjectNodeFlow());"
    if flow_line not in new_block:
        return PatchResult(
            ok=False,
            message="未找到 ProjectNodeFlowDO 转换行，无法安全生成补丁",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )
    flow_injection = (
        flow_line
        + "\n"
        + "            if (projectNodeFlowDO == null) {\n"
        + "                throw new BaseBizRuntimeException(\"请填写流程表单数据后重新发起\");\n"
        + "            }\n"
        + "            if (projectNodeFlowDO.getProjectId() == null) {\n"
        + "                projectNodeFlowDO.setProjectId(projectModifyReq.getId());\n"
        + "            }\n"
    )
    new_block = new_block.replace(flow_line, flow_injection, 1)

    # 4) else-if 对 delayType=0 的判断用 delayType
    new_block = new_block.replace("} else if (Integer.valueOf(0).equals(projectModifyReq.getDelayType())) {", "} else if (Integer.valueOf(0).equals(delayType)) {", 1)

    new = old[:start] + new_block + old[end:]
    diff = _unified_diff(old, new, file_path=str(target))
    if not diff.strip():
        return PatchResult(
            ok=False,
            message="补丁生成后 diff 为空，已中止（避免无意义写入）",
            target_file=str(target),
            unified_diff="",
            applied=False,
        )

    if apply:
        _write_text(target, new)
        return PatchResult(
            ok=True,
            message="已应用补丁到目标文件",
            target_file=str(target),
            unified_diff=diff,
            applied=True,
        )

    return PatchResult(
        ok=True,
        message="已生成补丁（未应用）",
        target_file=str(target),
        unified_diff=diff,
        applied=False,
    )


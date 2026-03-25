"""
Bugfix Agent：让模型通过 tool calling 主动调用 list_uploaded_files、read_uploaded_file、query_code_graph，
从而自己决定读哪些文件（如 CSV 第 19 行 ContentRaw），而不是预填整段上下文。
"""
from __future__ import annotations

import json
import logging
from typing import Any, AsyncIterator, Dict, List, Optional

from api.tools import (
    list_uploaded_files,
    query_code_graph,
    read_uploaded_file_for_llm,
    search_code,
    read_source_file,
    get_project_dependencies,
    get_bug_detail,
    get_bug_attachments,
    cleanup_bug_attachments,
    cleanup_session,
    clone_git_repo,
    resolve_imports,
    _files_from_names,
)

logger = logging.getLogger("api.agent")


class BugInfoUnavailableError(Exception):
    """Bug 信息无法获取时抛出，触发流程终止。"""
    pass


def _block_type(block: Any) -> Optional[str]:
    """兼容 SDK 返回对象：type 可能是属性或类名。"""
    t = getattr(block, "type", None)
    if t in ("text", "tool_use"):
        return t
    name = type(block).__name__
    if name == "TextBlock":
        return "text"
    if name == "ToolUseBlock":
        return "tool_use"
    return t


def _content_blocks_to_dicts(content: List[Any]) -> List[Dict[str, Any]]:
    """把 SDK 返回的 content 转成 API 要求的 dict 列表，便于下一轮请求被正确接受。"""
    out: List[Dict[str, Any]] = []
    for block in content:
        btype = _block_type(block)
        if btype == "text":
            out.append({"type": "text", "text": getattr(block, "text", "") or ""})
        elif btype == "tool_use":
            raw_input = getattr(block, "input", None)
            out.append({
                "type": "tool_use",
                "id": getattr(block, "id", "") or "",
                "name": getattr(block, "name", "") or "",
                "input": raw_input if isinstance(raw_input, dict) else {},
            })
    return out

# 允许调用的文件名集合；None 表示不限制（按 project/ref 列出的均可读）
AllowedFiles = Optional[List[str]]


def _normalize_analysis_chain_steps(raw: Any) -> List[Dict[str, Any]]:
    """把 AI 传入的 steps 规范为 [{type?, label, detail?, tool_kind?}, ...]，首步 type 设为 problem。"""
    out: List[Dict[str, Any]] = []
    if not isinstance(raw, list):
        return out
    for i, item in enumerate(raw):
        if not isinstance(item, dict):
            continue
        label = (item.get("label") or item.get("name") or "").strip()
        if not label:
            continue
        detail = item.get("detail")
        if detail is not None and not isinstance(detail, str):
            detail = str(detail)
        if isinstance(detail, str):
            detail = detail.strip() or None
        step: Dict[str, Any] = {"label": label, "detail": detail}
        if i == 0:
            step["type"] = "problem"
        else:
            step["type"] = item.get("type") or "step"
        tool_kind = (item.get("tool_kind") or "").strip()
        if tool_kind:
            step["tool_kind"] = tool_kind
        out.append(step)
    return out


def _execute_tool(
    name: str,
    arguments: Dict[str, Any],
    *,
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    allowed_file_names: AllowedFiles = None,
    session_id: Optional[str] = None,
) -> str:
    """执行单个工具，返回给模型看的字符串。"""
    try:
        if name == "list_uploaded_files":
            # 直接列出 session 目录下所有文件，不再用 allowed_file_names 白名单过滤
            files = list_uploaded_files(project_name=project_name, ref=ref, session_id=session_id)
            if not files:
                return "（当前会话目录无可用文件；若用户已上传，请确认本次会话 session_id 是否正确。）"
            lines = [f"  - {f['name']} （{f.get('size', 0)} bytes）" for f in files]
            return "当前会话可读取的文件列表（可直接用 read_uploaded_file 读取以下文件名）：\n" + "\n".join(lines)

        if name == "read_uploaded_file":
            filename = (arguments.get("filename") or "").strip()
            if not filename:
                return "[错误：缺少参数 filename]"
            # 只依赖 session_id 目录隔离，不再做白名单校验
            # read_uploaded_file_for_llm 内部已有路径穿越保护
            return read_uploaded_file_for_llm(filename, session_id=session_id)

        if name == "query_code_graph":
            proj = (arguments.get("project_name") or project_name or "").strip()
            if not proj:
                return "[错误：缺少 project_name 或当前无项目上下文]"
            cypher = (arguments.get("cypher") or "").strip() or None
            query_summary = (arguments.get("query_summary") or "").strip()
            result = query_code_graph(proj, cypher=cypher)
            # 把 cypher 语句和查询目的拼进结果，让前端能展示
            prefix_parts = []
            if query_summary:
                prefix_parts.append(f"🎯 **查询目的**\n\n{query_summary}")
            if cypher:
                prefix_parts.append(f"🔎 **Cypher**\n\n```cypher\n{cypher}\n```")
            if prefix_parts:
                result = "\n\n".join(prefix_parts) + f"\n\n📊 **查询结果**\n\n```json\n{result}\n```"
            return result

        if name == "search_code":
            proj = (arguments.get("project_name") or project_name or "").strip()
            if not proj:
                return "[错误：当前无项目上下文，无法搜索源码]"
            return search_code(
                project_name=proj,
                pattern=arguments.get("pattern", ""),
                file_glob=arguments.get("file_glob", "**/*.java"),
                max_results=int(arguments.get("max_results", 20)),
                context_lines=int(arguments.get("context_lines", 3)),
            )

        if name == "read_source_file":
            proj = (arguments.get("project_name") or project_name or "").strip()
            if not proj:
                return "[错误：当前无项目上下文，无法读取源码]"
            return read_source_file(
                project_name=proj,
                file_path=arguments.get("file_path", ""),
                start_line=int(arguments.get("start_line", 1)),
                end_line=int(arguments.get("end_line", 0)),
                max_lines=int(arguments.get("max_lines", 300)),
            )

        if name == "get_project_dependencies":
            proj = (arguments.get("project_name") or project_name or "").strip()
            if not proj:
                return "[错误：缺少 project_name]"
            return get_project_dependencies(proj)

        if name == "get_bug_detail":
            try:
                bid = int(arguments.get("bug_id", 0))
            except (TypeError, ValueError):
                return "[错误：bug_id 必须为整数]"
            if not bid:
                return "[错误：缺少参数 bug_id]"
            result = get_bug_detail(bid)
            # 获取失败时终止整个流程
            if result.startswith("[获取 Bug 详情失败") or result.startswith(f"[Bug #{bid} 不存在"):
                raise BugInfoUnavailableError(f"无法获取 Bug #{bid} 的详情：{result}")
            return result

        if name == "get_bug_attachments":
            try:
                bid = int(arguments.get("bug_id", 0))
            except (TypeError, ValueError):
                return "[错误：bug_id 必须为整数]"
            attachment_urls = arguments.get("attachment_urls", [])
            max_size = int(arguments.get("max_file_size", 2 * 1024 * 1024))
            return get_bug_attachments(bid, attachment_urls, session_id=session_id, max_file_size=max_size)

        if name == "cleanup_bug_attachments":
            try:
                bid = int(arguments.get("bug_id", 0))
            except (TypeError, ValueError):
                return "[错误：bug_id 必须为整数]"
            return cleanup_bug_attachments(bid, session_id=session_id)

        if name == "cleanup_session":
            sid = (arguments.get("session_id") or session_id or "").strip()
            if not sid:
                return "[错误：缺少 session_id]"
            return cleanup_session(sid)

        if name == "clone_git_repo":
            pname = (arguments.get("project_name") or project_name or "").strip()
            if not pname:
                return "[错误：缺少 project_name]"
            rurl = (arguments.get("repo_url") or "").strip()
            if not rurl:
                return "[错误：缺少 repo_url]"
            return clone_git_repo(
                project_name=pname,
                repo_url=rurl,
                branch=arguments.get("branch") or None,
                commit_id=arguments.get("commit_id") or None,
                timeout=int(arguments["timeout"]) if arguments.get("timeout") else None,
            )

        if name == "output_analysis_chain":
            return "已记录分析链路，请继续给出你的分析结论。"

        if name == "resolve_imports":
            fqns = arguments.get("fqns") or []
            if isinstance(fqns, str):
                fqns = [f.strip() for f in fqns.split(",") if f.strip()]
            return resolve_imports(fqns)

        return f"[未知工具: {name}]"
    except BugInfoUnavailableError:
        raise  # 透传，让上层终止流程
    except Exception as e:
        logger.exception("tool %s failed", name)
        return f"[执行失败: {e}]"


# --------------- Claude (Anthropic) 工具定义 ---------------
CLAUDE_TOOLS = [
    {
        "name": "list_uploaded_files",
        "description": "列出当前可用的用户上传文件（本次会话允许读取的文件）。返回文件名与大小，用于后续用 read_uploaded_file 按文件名读取内容。",
        "input_schema": {
            "type": "object",
            "properties": {},
        },
    },
    {
        "name": "read_uploaded_file",
        "description": "按文件名读取用户上传的某个文件的原始内容（文本）。文件格式可能多样（如 .log、.csv、.json、.txt 等），由你根据内容自行理解。务必先 list_uploaded_files 再按返回的文件名读取。",
        "input_schema": {
            "type": "object",
            "properties": {
                "filename": {
                    "type": "string",
                    "description": "文件名，如 xxx.log、xxx.csv",
                }
            },
            "required": ["filename"],
        },
    },
    {
        "name": "query_code_graph",
        "description": "查询代码图谱（Neo4j），获取项目中的类、方法等结构摘要，用于定位与 bug 相关的代码。支持自定义 Cypher 查询，可精确查找特定类/方法/字段及其调用关系。",
        "input_schema": {
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "项目名，如 forward",
                },
                "cypher": {
                    "type": "string",
                    "description": "可选：自定义只读 Cypher 查询语句。例如：MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod) WHERE c.name='ProjectServiceImpl' AND m.name='processFlow' RETURN m.raw_metadata, m.start_line, m.end_line。不填则返回项目节点摘要。",
                },
                "query_summary": {
                    "type": "string",
                    "description": "本次查询的目的说明，如「查找 processFlow 方法的完整实现」，会展示在思考过程中方便用户理解。",
                },
            },
            "required": ["project_name"],
        },
    },
    {
        "name": "get_project_dependencies",
        "description": "查询当前项目（或指定项目）依赖了哪些其他已导入项目（二方包）。返回依赖项目名列表，可用于后续用 query_code_graph / search_code / read_source_file 并传入对应 project_name 去查那个项目的图谱或源码。排查问题时若怀疑根因在二方包，应先调用本工具确认依赖关系。",
        "input_schema": {
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "要查询依赖的项目名，不填则用当前会话项目",
                },
            },
        },
    },
    {
        "name": "search_code",
        "description": "在本地项目源码中用正则搜索，返回匹配行及上下文。适合查找某个类名、方法名、字段名、关键字等在哪些文件中出现。可通过 project_name 参数指定搜索其他项目（如二方包项目）的源码，不填则搜索当前会话项目。",
        "input_schema": {
            "type": "object",
            "properties": {
                "pattern": {
                    "type": "string",
                    "description": "正则表达式，如 'processFlow|delayType'",
                },
                "project_name": {
                    "type": "string",
                    "description": "要搜索的项目名，可显式传入以搜索二方包等其他项目；不填则用当前会话项目",
                },
                "search_summary": {
                    "type": "string",
                    "description": "本次搜索的目的说明，会展示在思考过程中，如「查找 processFlow 的调用位置」",
                },
                "file_glob": {
                    "type": "string",
                    "description": "文件匹配模式，默认 **/*.java，也可用 **/*.xml 等",
                },
                "max_results": {
                    "type": "integer",
                    "description": "最多返回多少处匹配，默认 20",
                },
                "context_lines": {
                    "type": "integer",
                    "description": "每处匹配前后各保留几行，默认 3",
                },
            },
            "required": ["pattern"],
        },
    },
    {
        "name": "read_source_file",
        "description": "读取本地项目源码文件内容。可通过 project_name 参数指定读取其他项目（如二方包项目）的源码，不填则读取当前会话项目。file_path 优先使用 query_code_graph 从 File 节点查出的 full_path 绝对路径，也支持相对于仓库根目录的相对路径。建议先用图谱查 File 节点拿到 full_path，再传入此工具。",
        "input_schema": {
            "type": "object",
            "properties": {
                "file_path": {
                    "type": "string",
                    "description": "相对于仓库根目录的文件路径，如 src/main/java/com/example/Foo.java",
                },
                "project_name": {
                    "type": "string",
                    "description": "要读取的项目名，可显式传入以读取二方包等其他项目的源码；不填则用当前会话项目",
                },
                "start_line": {
                    "type": "integer",
                    "description": "起始行（1-based），默认 1",
                },
                "end_line": {
                    "type": "integer",
                    "description": "结束行（1-based，含），0 表示读到文件末尾，默认 0",
                },
                "max_lines": {
                    "type": "integer",
                    "description": "最多返回行数，默认 300",
                },
            },
            "required": ["file_path"],
        },
    },
    {
        "name": "get_bug_detail",
        "description": "从 Forward 产研系统获取指定 Bug 的详情。返回内容包括：标题(name)、状态(status/statusName)、描述(describe)、附件列表(files)。当用户已选择 Bug 且需要了解 Bug 具体内容时必须调用此工具。",
        "input_schema": {
            "type": "object",
            "properties": {
                "bug_id": {
                    "type": "integer",
                    "description": "Bug ID，如 4518",
                }
            },
            "required": ["bug_id"],
        },
    },
    {
        "name": "get_bug_attachments",
        "description": "下载 Bug 附件到当前会话目录，供后续用 read_uploaded_file 读取分析。当 get_bug_detail 返回的附件列表中有文件且有 downloadUrl 时，必须调用本工具下载附件，然后用返回的「文件路径」调用 read_uploaded_file 读取内容。不要直接用原始文件名调用 read_uploaded_file。",
        "input_schema": {
            "type": "object",
            "properties": {
                "bug_id": {
                    "type": "integer",
                    "description": "Bug ID",
                },
                "attachment_urls": {
                    "type": "array",
                    "description": "附件信息列表，每项包含 fileName 和 downloadUrl，直接从 get_bug_detail 返回的附件列表传入",
                    "items": {
                        "type": "object",
                        "properties": {
                            "fileName": {"type": "string"},
                            "downloadUrl": {"type": "string"},
                        },
                    },
                },
            },
            "required": ["bug_id", "attachment_urls"],
        },
    },
    {
        "name": "clone_git_repo",
        "description": "根据 repo_url + branch/commit_id 拉取 Git 仓库到本地缓存目录。若本地已存在则 fetch + checkout 更新；若不存在则 clone。仅拉取仓库，不做代码图谱构建。拉取完成后可用 search_code / read_source_file 读取源码。",
        "input_schema": {
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "应用名称，用作本地缓存目录名，通常与应用的 project_name 一致",
                },
                "repo_url": {
                    "type": "string",
                    "description": "Git 仓库地址，如 https://github.com/xxx/yyy.git",
                },
                "branch": {
                    "type": "string",
                    "description": "分支名，如 main、release/2.0.5（与 commit_id 二选一）",
                },
                "commit_id": {
                    "type": "string",
                    "description": "指定 commit hash（优先级高于 branch）",
                },
            },
            "required": ["project_name", "repo_url"],
        },
    },
    {
        "name": "resolve_imports",
        "description": (
            "给定一批 Java 类的全限定名（FQN），查询它们来自哪个 jar 坐标，并标注是否为二方包或三方包。"
            "当你读取一个 Java 文件后，若不确定某些 import 来自哪里（是本项目源码、二方包还是三方包），"
            "应调用本工具明确归属，再决定是否需要跳转到对应项目的图谱或源码继续排查。"
            "注意：read_source_file 在检测到二方包 import 时会自动附加分析，无需重复调用。"
        ),
        "input_schema": {
            "type": "object",
            "properties": {
                "fqns": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Java 类全限定名列表，如 [\"com.timevale.forward.dal.dao.BizDemandMapper\", \"com.google.guava.collect.Lists\"]",
                },
            },
            "required": ["fqns"],
        },
    },
    {
        "description": "在给出最终分析结论之前，你必须调用本工具一次，提交本次分析的完整链路（从「拿到问题」到查图、查文件、读源码等每一步），便于用户看到流程图。steps 为数组，每项 {label: 步骤名称, detail: 本步具体说明, tool_kind: 本步对应的工具名（可选）}。detail 必填且要写清本步做了什么、查了什么或得到什么结论，例如：查图步写「在图谱中查询 ProjectServiceImpl.processFlow 方法的定义与调用关系」；查文件步写「读取用户上传的 bugfix-context.csv，确认第19行报错内容」；读源码步写「查看 BugfixController.java 第12-34行实现」；结论步写「定位到 NPE 来自 processFlow 入参未校验」。tool_kind 填本步实际调用的工具名，如 query_code_graph、read_uploaded_file、search_code、read_source_file，没有对应工具的步骤（如「拿到问题」「结论」）不填。",
        "input_schema": {
            "type": "object",
            "properties": {
                "steps": {
                    "type": "array",
                    "description": "分析链路步骤列表，每项含 label（必填）、detail（必填）、tool_kind（可选，填本步调用的工具名）",
                    "items": {
                        "type": "object",
                        "properties": {
                            "label": {"type": "string", "description": "步骤名称，如 拿到问题、查图、查文件、读源码、结论"},
                            "detail": {"type": "string", "description": "本步具体说明：做了什么、查了什么、得到什么结论，一句话写清"},
                            "tool_kind": {"type": "string", "description": "本步对应的工具名，如 query_code_graph、read_uploaded_file、search_code、read_source_file，无工具的步骤不填"},
                        },
                        "required": ["label", "detail"],
                    },
                },
            },
            "required": ["steps"],
        },
    },
]

# --------------- OpenAI / DeepSeek 工具定义 ---------------
OPENAI_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "list_uploaded_files",
            "description": "列出当前可用的用户上传文件（本次会话允许读取的文件）。返回文件名与大小，用于后续用 read_uploaded_file 按文件名读取内容。",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "read_uploaded_file",
            "description": "按文件名读取用户上传的某个文件的原始内容（文本）。格式可能多样（.log、.csv、.json、.txt 等），由你根据内容自行理解。务必先 list 再按返回的文件名读取。",
            "parameters": {
                "type": "object",
                "properties": {
                    "filename": {
                        "type": "string",
                        "description": "文件名，如 xxx.log、xxx.csv",
                    }
                },
                "required": ["filename"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "query_code_graph",
            "description": "查询代码图谱（Neo4j），获取项目中的类、方法等结构摘要。",
            "parameters": {
                "type": "object",
                "properties": {
                    "project_name": {"type": "string", "description": "项目名，如 forward"},
                    "cypher": {"type": "string", "description": "可选：自定义只读 Cypher 查询语句"},
                    "query_summary": {"type": "string", "description": "本次查询的目的说明，会展示在思考过程中"},
                },
                "required": ["project_name"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_project_dependencies",
            "description": "查询当前项目（或指定项目）依赖了哪些其他已导入项目（二方包）。排查问题时若怀疑根因在二方包，应先调用本工具确认依赖关系，再用 query_code_graph / search_code / read_source_file 并传入对应 project_name 去查那个项目。",
            "parameters": {
                "type": "object",
                "properties": {
                    "project_name": {"type": "string", "description": "要查询依赖的项目名，不填则用当前会话项目"},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_code",
            "description": "在本地项目源码中用正则搜索，返回匹配行及上下文。可通过 project_name 参数指定搜索其他项目（如二方包项目）的源码，不填则搜索当前会话项目。",
            "parameters": {
                "type": "object",
                "properties": {
                    "pattern": {"type": "string", "description": "正则表达式"},
                    "project_name": {"type": "string", "description": "要搜索的项目名，可显式传入以搜索二方包等其他项目；不填则用当前会话项目"},
                    "search_summary": {"type": "string", "description": "本次搜索的目的说明，会展示在思考过程中"},
                    "file_glob": {"type": "string", "description": "文件匹配模式，默认 **/*.java"},
                    "max_results": {"type": "integer", "description": "最多返回多少处，默认 20"},
                    "context_lines": {"type": "integer", "description": "前后各保留几行，默认 3"},
                },
                "required": ["pattern"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "read_source_file",
            "description": "读取本地项目源码文件内容。可通过 project_name 参数指定读取其他项目（如二方包项目）的源码，不填则读取当前会话项目。file_path 优先使用 query_code_graph 从 File 节点查出的 full_path 绝对路径，也支持相对路径。",
            "parameters": {
                "type": "object",
                "properties": {
                    "file_path": {"type": "string", "description": "相对路径，如 src/main/java/com/example/Foo.java"},
                    "project_name": {"type": "string", "description": "要读取的项目名，可显式传入以读取二方包等其他项目的源码；不填则用当前会话项目"},
                    "start_line": {"type": "integer", "description": "起始行（1-based），默认 1"},
                    "end_line": {"type": "integer", "description": "结束行（1-based），0=到末尾，默认 0"},
                    "max_lines": {"type": "integer", "description": "最多返回行数，默认 300"},
                },
                "required": ["file_path"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_bug_detail",
            "description": "从 Forward 产研系统获取指定 Bug 的详情。返回内容包括：标题(name)、状态(status/statusName)、描述(describe)、附件列表(files)。当用户已选择 Bug 且需要了解 Bug 具体内容时必须调用此工具。",
            "parameters": {
                "type": "object",
                "properties": {
                    "bug_id": {
                        "type": "integer",
                        "description": "Bug ID，如 4518",
                    }
                },
                "required": ["bug_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_bug_attachments",
            "description": "下载 Bug 附件到当前会话目录，供后续用 read_uploaded_file 读取分析。当 get_bug_detail 返回的附件列表中有文件且有 downloadUrl 时，必须调用本工具下载，然后用返回的「文件路径」调用 read_uploaded_file 读取内容。不要直接用原始文件名调用 read_uploaded_file。",
            "parameters": {
                "type": "object",
                "properties": {
                    "bug_id": {
                        "type": "integer",
                        "description": "Bug ID",
                    },
                    "attachment_urls": {
                        "type": "array",
                        "description": "附件信息列表，每项包含 fileName 和 downloadUrl，直接从 get_bug_detail 返回的附件列表传入",
                        "items": {
                            "type": "object",
                            "properties": {
                                "fileName": {"type": "string"},
                                "downloadUrl": {"type": "string"},
                            },
                        },
                    },
                },
                "required": ["bug_id", "attachment_urls"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "clone_git_repo",
            "description": "根据 repo_url + branch/commit_id 拉取 Git 仓库到本地缓存目录。若本地已存在则 fetch + checkout 更新；若不存在则 clone。仅拉取仓库，不做代码图谱构建。拉取完成后可用 search_code / read_source_file 读取源码。",
            "parameters": {
                "type": "object",
                "properties": {
                    "project_name": {
                        "type": "string",
                        "description": "应用名称，用作本地缓存目录名",
                    },
                    "repo_url": {
                        "type": "string",
                        "description": "Git 仓库地址，如 https://github.com/xxx/yyy.git",
                    },
                    "branch": {
                        "type": "string",
                        "description": "分支名（与 commit_id 二选一）",
                    },
                    "commit_id": {
                        "type": "string",
                        "description": "指定 commit hash（优先级高于 branch）",
                    },
                },
                "required": ["project_name", "repo_url"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "resolve_imports",
            "description": (
                "给定一批 Java 类的全限定名（FQN），查询它们来自哪个 jar 坐标，并标注是否为二方包或三方包。"
                "当你读取一个 Java 文件后，若不确定某些 import 来自哪里，应调用本工具明确归属，"
                "再决定是否需要跳转到对应项目的图谱或源码继续排查。"
                "注意：read_source_file 在检测到二方包 import 时会自动附加分析，无需重复调用。"
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "fqns": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "Java 类全限定名列表",
                    },
                },
                "required": ["fqns"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "output_analysis_chain",
            "description": "在给出最终分析结论之前，你必须调用本工具一次，提交本次分析的完整链路。每步的 detail 必填且写清本步具体做了什么、查了什么或得到什么结论。tool_kind 填本步实际调用的工具名（query_code_graph、read_uploaded_file、search_code、read_source_file），无工具的步骤不填。",
            "parameters": {
                "type": "object",
                "properties": {
                    "steps": {
                        "type": "array",
                        "description": "分析链路步骤，每项 {label: 步骤名, detail: 本步具体说明, tool_kind: 工具名（可选）}",
                        "items": {
                            "type": "object",
                            "properties": {
                                "label": {"type": "string"},
                                "detail": {"type": "string", "description": "本步具体内容，一句话写清"},
                                "tool_kind": {"type": "string", "description": "本步对应工具名，无工具不填"},
                            },
                            "required": ["label", "detail"],
                        },
                    },
                },
                "required": ["steps"],
            },
        },
    },
]


async def run_bugfix_agent(
    system_prompt: str,
    user_content: str,
    *,
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    allowed_file_names: AllowedFiles = None,
    session_id: Optional[str] = None,
    provider: str = "claude",
    model: Optional[str] = None,
    max_turns: int = 12,
    history: Optional[List[Dict[str, Any]]] = None,
) -> AsyncIterator[tuple[str, Dict[str, Any]]]:
    """
    运行带工具的 Bugfix Agent。yield ("tool", {kind, title, content}) 或 ("text", {content})。
    history: 历史对话 [{"role": "user"|"assistant", "content": "..."}]
    """
    if provider == "claude":
        async for ev in _run_claude_agent(
            system_prompt=system_prompt,
            user_content=user_content,
            project_name=project_name,
            ref=ref,
            allowed_file_names=allowed_file_names,
            session_id=session_id,
            model=model,
            max_turns=max_turns,
            history=history or [],
        ):
            yield ev
    else:
        async for ev in _run_openai_agent(
            system_prompt=system_prompt,
            user_content=user_content,
            project_name=project_name,
            ref=ref,
            allowed_file_names=allowed_file_names,
            session_id=session_id,
            model=model,
            max_turns=max_turns,
            history=history or [],
        ):
            yield ev


async def _run_claude_agent(
    system_prompt: str,
    user_content: str,
    *,
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    allowed_file_names: AllowedFiles = None,
    session_id: Optional[str] = None,
    model: Optional[str] = None,
    max_turns: int = 12,
    history: Optional[List[Dict[str, Any]]] = None,
) -> AsyncIterator[tuple[str, Dict[str, Any]]]:
    import os
    try:
        from core.env_loader import load_env_vars
        load_env_vars({"ANTHROPIC_API_BASE", "ANTHROPIC_BASE_URL", "ANTHROPIC_API_KEY", "ANTHROPIC_MODEL", "DEEPSEEK_API_BASE", "DEEPSEEK_API_KEY", "OPENAI_API_KEY"})
    except Exception:
        pass
    from anthropic import AsyncAnthropic

    base = (os.environ.get("ANTHROPIC_API_BASE") or os.environ.get("ANTHROPIC_BASE_URL", "") or "").strip()
    if not base:
        deepseek_base = (os.environ.get("DEEPSEEK_API_BASE") or "").rstrip("/")
        if deepseek_base.endswith("/v1"):
            deepseek_base = deepseek_base[: -len("/v1")]
        base = deepseek_base or "https://api.anthropic.com"
    base = base.rstrip("/")
    key = os.environ.get("ANTHROPIC_API_KEY", "") or os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")
    if not key:
        yield ("text", {"content": "[错误：未配置 Claude/ANTHROPIC 或 DEEPSEEK API Key]"})
        return

    # 显式超时，避免长分析被 SDK 提前断开（Anthropic 报 Request timed out or interrupted）
    timeout_sec = float(os.environ.get("ANTHROPIC_TIMEOUT_SEC", "600") or "600")
    client = AsyncAnthropic(api_key=key, base_url=base, timeout=timeout_sec)
    m = (model or os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-20250514")).strip()

    system = (system_prompt or "").strip() + "\n\n【重要】你拥有上述工具。你必须先调用 list_uploaded_files 查看可用文件，再按需调用 read_uploaded_file 或 query_code_graph 获取内容与图谱，最后再给出分析结论。禁止只回复一句开场白而不调用任何工具。\n优先用 query_code_graph 从图谱获取信息；若图谱返回的数据已足够分析，则不必再调用 read_source_file，仅在图谱数据不足时再查源码。\n每次调用 query_code_graph 时，必须同时填写 cypher（具体的 Cypher 查询语句）和 query_summary（本次查询的目的说明），不得省略，否则用户无法看到你执行了哪些查询。调用 search_code 时请同时填写 search_summary（本次搜索的目的说明，如「查找 processFlow 的调用位置」），便于用户在思考过程中看到目的。\n【源码访问】调用 search_code 或 read_source_file 之前，必须先调用 clone_git_repo 确保本地有该项目的 git 仓库缓存（工具内部会自动判断：已有缓存则静默跳过，不存在才真正拉取）。project_name 填应用名称，repo_url 填该应用配置的 Git 地址，branch/commit_id 按当前分析上下文填写。\n【跨项目排查】若分析过程中怀疑问题根因在某个二方包项目，应先调用 get_project_dependencies 确认当前项目依赖了哪些已导入项目，再用 query_code_graph、search_code 或 read_source_file 并显式传入 project_name=<依赖项目名> 去查那个项目的图谱或源码。\n【Bug 附件】调用 get_bug_detail 后，若返回的附件列表中有文件（files 字段非空），必须立即调用 get_bug_attachments 下载附件（附件可能是日志、截图、配置文件等任意格式，统称附件），然后用返回结果中的「文件路径」调用 read_uploaded_file 读取内容。禁止直接用原始文件名（如 log.csv）调用 read_uploaded_file。\n在给出最终分析结论的那一轮，你必须先调用 output_analysis_chain 提交本次分析的完整链路（从「拿到问题」到查图、查文件等每一步的简要说明），再在回复中给出文字结论。"
    # 历史消息前置，Claude 要求 user/assistant 交替
    history_messages: List[Dict[str, Any]] = []
    for h in (history or []):
        role = h.get("role", "")
        content = h.get("content", "")
        if role in ("user", "assistant") and content:
            history_messages.append({"role": role, "content": content})
    messages: List[Dict[str, Any]] = history_messages + [{"role": "user", "content": user_content}]
    turn = 0

    while turn < max_turns:
        turn += 1
        # 用 stream=True 实现文字实时输出
        try:
            stream_ctx = client.messages.stream(
                model=m,
                max_tokens=8192,
                system=system,
                messages=messages,
                tools=CLAUDE_TOOLS,
            )
        except Exception as e:
            logger.exception("Claude messages.stream init failed (turn=%s)", turn)
            # 交给上层路由统一转成 SSE error + done，避免前端无限 loading
            raise

        # 收集流式输出：文本实时 yield，tool_use 收集完整后执行
        text_buf = ""
        tool_uses: List[Dict[str, Any]] = []  # [{id, name, input_str}]
        current_tool: Optional[Dict[str, Any]] = None
        stop_reason = None

        try:
            async with stream_ctx as stream:
                async for event in stream:
                    etype = getattr(event, "type", None)

                    if etype == "content_block_start":
                        block = getattr(event, "content_block", None)
                        btype = getattr(block, "type", None)
                        if btype == "tool_use":
                            current_tool = {
                                "id": getattr(block, "id", "") or "",
                                "name": getattr(block, "name", "") or "",
                                "input_str": "",
                            }
                        elif btype == "text":
                            current_tool = None

                    elif etype == "content_block_delta":
                        delta = getattr(event, "delta", None)
                        dtype = getattr(delta, "type", None)
                        if dtype == "text_delta":
                            chunk = getattr(delta, "text", "") or ""
                            if chunk:
                                text_buf += chunk
                                # 实时流式输出文字片段
                                yield ("text_chunk", {"content": chunk})
                        elif dtype == "input_json_delta":
                            if current_tool is not None:
                                current_tool["input_str"] += getattr(delta, "partial_json", "") or ""

                    elif etype == "content_block_stop":
                        if current_tool is not None:
                            tool_uses.append(current_tool)
                            current_tool = None

                    elif etype == "message_delta":
                        delta = getattr(event, "delta", None)
                        stop_reason = getattr(delta, "stop_reason", None) or stop_reason

                    elif etype == "message_stop":
                        pass

        except Exception as e:
            logger.exception("Claude stream read failed (turn=%s)", turn)
            # 交给上层路由统一转成 SSE error + done，避免前端无限 loading
            raise

        # 没有工具调用 → 本轮是最终回复，结束
        if not tool_uses:
            if not text_buf.strip() and turn == 1:
                yield ("text", {"content": "[模型未返回内容，请重试或检查配置]"})
            break

        # 有工具调用 → 执行工具，把结果喂回去继续下一轮
        # 先把本轮 text_buf（如果有）作为 assistant 开场白发出
        # （Claude 有时在 tool_use 前会先说一句话，不丢弃）
        assistant_content: List[Dict[str, Any]] = []
        if text_buf.strip():
            assistant_content.append({"type": "text", "text": text_buf})

        tool_results = []
        for t in tool_uses:
            tid = t["id"]
            name = t["name"]
            try:
                import json as _json
                args = _json.loads(t["input_str"]) if t["input_str"].strip() else {}
            except Exception:
                args = {}
            assistant_content.append({"type": "tool_use", "id": tid, "name": name, "input": args})
            result = _execute_tool(
                name,
                args,
                project_name=project_name,
                ref=ref,
                allowed_file_names=allowed_file_names,
                session_id=session_id,
            )
            tool_results.append({"type": "tool_result", "tool_use_id": tid, "content": result})
            # AI 通过 output_analysis_chain 提交的分析链路：只发 SSE，不展示为「工具步骤」
            if name == "output_analysis_chain":
                steps = _normalize_analysis_chain_steps(args.get("steps"))
                if steps:
                    yield ("analysis_chain", {"steps": steps})
                continue
            # clone_git_repo：已有缓存时静默，真正拉取时才发 SSE
            if name == "clone_git_repo":
                if result.startswith("[CACHED]"):
                    continue
                yield ("tool", {"kind": name, "title": f"拉取仓库：{args.get('project_name', '')}", "content": result})
                continue
            # 给前端展示用的 title / summary
            title = name
            summary = None
            if name == "read_uploaded_file":
                fn = (args.get("filename") or "").strip()
                if fn:
                    title = f"读取文件：{fn}"
            elif name == "get_bug_attachments":
                title = "下载附件再分析"
            elif name == "get_bug_detail":
                title = f"获取 Bug 详情：{args.get('bug_id', '')}"
            elif name == "search_code":
                title = "代码搜索"
                summary = (args.get("search_summary") or "").strip() or None
            payload = {"kind": name, "title": title, "content": result[:4000] + ("..." if len(result) > 4000 else "")}
            if summary is not None:
                payload["summary"] = summary
            yield ("tool", payload)

        messages.append({"role": "assistant", "content": assistant_content})
        messages.append({"role": "user", "content": tool_results})

    return


async def _run_openai_agent(
    system_prompt: str,
    user_content: str,
    *,
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    allowed_file_names: AllowedFiles = None,
    session_id: Optional[str] = None,
    model: Optional[str] = None,
    max_turns: int = 12,
    history: Optional[List[Dict[str, Any]]] = None,
) -> AsyncIterator[tuple[str, Dict[str, Any]]]:
    import os
    from openai import AsyncOpenAI

    base = (os.environ.get("DEEPSEEK_API_BASE") or "https://api.deepseek.com").strip().rstrip("/")
    key = os.environ.get("DEEPSEEK_API_KEY", "") or os.environ.get("OPENAI_API_KEY", "")
    if not key:
        yield ("text", {"content": "[错误：未配置 DEEPSEEK_API_KEY 或 OPENAI_API_KEY]"})
        return

    client = AsyncOpenAI(api_key=key, base_url=base)
    m = (model or os.environ.get("DEEPSEEK_MODEL", "deepseek-chat")).strip()

    # 历史消息前置
    history_messages: List[Dict[str, Any]] = []
    for h in (history or []):
        role = h.get("role", "")
        content = h.get("content", "")
        if role in ("user", "assistant") and content:
            history_messages.append({"role": role, "content": content})

    messages: List[Dict[str, Any]] = [
        {"role": "system", "content": (system_prompt or "").strip() + "\n\n【重要】你拥有上述工具。你必须先调用 list_uploaded_files 查看可用文件，再按需调用 read_uploaded_file 或 query_code_graph，最后再给出分析结论。禁止只回复一句开场白而不调用任何工具。\n优先用 query_code_graph 从图谱获取信息；若图谱返回的数据已足够分析，则不必再调用 read_source_file，仅在图谱数据不足时再查源码。\n每次调用 query_code_graph 时，必须同时填写 cypher（具体的 Cypher 查询语句）和 query_summary（本次查询的目的说明），不得省略，否则用户无法看到你执行了哪些查询。调用 search_code 时请同时填写 search_summary（本次搜索的目的说明），便于用户在思考过程中看到目的。\n【源码访问】调用 search_code 或 read_source_file 之前，必须先调用 clone_git_repo 确保本地有该项目的 git 仓库缓存（工具内部会自动判断：已有缓存则静默跳过，不存在才真正拉取）。project_name 填应用名称，repo_url 填该应用配置的 Git 地址，branch/commit_id 按当前分析上下文填写。\n【跨项目排查】若分析过程中怀疑问题根因在某个二方包项目，应先调用 get_project_dependencies 确认当前项目依赖了哪些已导入项目，再用 query_code_graph、search_code 或 read_source_file 并显式传入 project_name=<依赖项目名> 去查那个项目的图谱或源码。\n【Bug 附件】调用 get_bug_detail 后，若返回的附件列表中有文件（files 字段非空），必须立即调用 get_bug_attachments 下载附件（附件可能是日志、截图、配置文件等任意格式，统称附件），然后用返回结果中的「文件路径」调用 read_uploaded_file 读取内容。禁止直接用原始文件名（如 log.csv）调用 read_uploaded_file。\n在给出最终分析结论的那一轮，你必须先调用 output_analysis_chain 提交本次分析的完整链路（从「拿到问题」到查图、查文件等每一步），再在回复中给出文字结论。"},
        *history_messages,
        {"role": "user", "content": user_content},
    ]
    turn = 0

    while turn < max_turns:
        turn += 1
        resp = await client.chat.completions.create(
            model=m,
            messages=messages,
            tools=OPENAI_TOOLS,
            stream=False,
        )
        choice = resp.choices and resp.choices[0]
        if not choice or not choice.message:
            break

        msg = choice.message
        # DeepSeek-R1 思考过程在 reasoning_content 字段
        reasoning = (getattr(msg, "reasoning_content", None) or "").strip()
        if reasoning:
            yield ("tool", {"kind": "reasoning", "title": "思考过程", "content": reasoning})

        # 过滤掉 content 里可能残留的 <think>...</think> 标签（部分模型会重复输出）
        raw_content = (getattr(msg, "content", None) or "").strip()
        import re as _re
        clean_content = _re.sub(r"<think>.*?</think>", "", raw_content, flags=_re.DOTALL).strip()

        tool_calls = getattr(msg, "tool_calls", None) or []
        if not tool_calls:
            if clean_content:
                yield ("text", {"content": clean_content})
            break

        if clean_content:
            yield ("text", {"content": clean_content})

        messages.append({
            "role": "assistant",
            "content": msg.content or "",
            "tool_calls": [{"id": tc.id, "type": "function", "function": {"name": tc.function.name, "arguments": tc.function.arguments or "{}"}} for tc in tool_calls],
        })
        for tc in tool_calls:
            name = tc.function.name if hasattr(tc.function, "name") else ""
            try:
                args = json.loads(tc.function.arguments or "{}") if hasattr(tc.function, "arguments") else {}
            except Exception:
                args = {}
            result = _execute_tool(
                name,
                args,
                project_name=project_name,
                ref=ref,
                allowed_file_names=allowed_file_names,
                session_id=session_id,
            )
            messages.append({"role": "tool", "tool_call_id": tc.id, "content": result})
            if name == "output_analysis_chain":
                steps = _normalize_analysis_chain_steps(args.get("steps"))
                if steps:
                    yield ("analysis_chain", {"steps": steps})
                continue
            # clone_git_repo：已有缓存时静默，真正拉取时才发 SSE
            if name == "clone_git_repo":
                if result.startswith("[CACHED]"):
                    continue
                yield ("tool", {"kind": name, "title": f"拉取仓库：{args.get('project_name', '')}", "content": result})
                continue
            title = name
            summary = None
            if name == "read_uploaded_file":
                fn = (args.get("filename") or "").strip()
                if fn:
                    title = f"读取文件：{fn}"
            elif name == "get_bug_attachments":
                title = "下载附件再分析"
            elif name == "get_bug_detail":
                title = f"获取 Bug 详情：{args.get('bug_id', '')}"
            elif name == "search_code":
                title = "代码搜索"
                summary = (args.get("search_summary") or "").strip() or None
            payload: Dict[str, Any] = {"kind": name, "title": title, "content": result[:4000] + ("..." if len(result) > 4000 else "")}
            if summary is not None:
                payload["summary"] = summary
            yield ("tool", payload)

    return

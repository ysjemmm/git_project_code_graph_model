"""
Bugfix 专家可用的自定义工具：图数据库查询、用户上传文件列表/读取。
"""
from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any, Dict, List, Optional

from api.config import STATIC_DIR


def _safe_ref_for_filter(ref: Optional[str]) -> str:
    """与上传时命名一致：用于匹配文件名中的 ref 部分（如 release/2.0.5 -> release_2_0_5）。"""
    return re.sub(r"[^\w\-.]", "_", (ref or "").strip()) if ref else ""


def list_uploaded_files(
    session_id: Optional[str] = None,
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    limit: int = 50,
) -> List[Dict[str, Any]]:
    """
    列出指定会话目录下的文件。如果不指定 session_id，则列出 static 根目录下的文件。
    按项目名、ref 过滤（ref 会做与上传时一致的安全化再匹配）。
    返回 [{"name": "xxx", "path": "static/xxx", "size": 123}, ...]
    """
    # 确定搜索目录
    if session_id:
        search_dir = STATIC_DIR / "sessions" / session_id
    else:
        search_dir = STATIC_DIR
    
    if not search_dir.exists():
        return []
    
    ref_safe = _safe_ref_for_filter(ref)
    out: List[Dict[str, Any]] = []
    
    # 递归遍历目录
    for f in search_dir.rglob("*"):
        if not f.is_file():
            continue
            
        # 获取相对于搜索目录的路径
        relative_path = f.relative_to(search_dir)
        name = str(relative_path).replace("\\", "/")  # 统一使用正斜杠
        
        if project_name and not name.startswith(project_name + "-"):
            continue
        if ref_safe and ref_safe not in name:
            continue
            
        try:
            size = f.stat().st_size
        except OSError:
            size = 0
            
        # 构造返回的路径（相对于 static 目录）
        if session_id:
            full_path = f"static/sessions/{session_id}/{name}"
        else:
            full_path = f"static/{name}"
            
        out.append({"name": name, "path": full_path, "size": size})
        if len(out) >= limit:
            break
            
    return sorted(out, key=lambda x: x["name"])


def read_uploaded_file(
    filename: str, 
    session_id: Optional[str] = None,
    max_bytes: int = 512 * 1024
) -> Optional[str]:
    """
    读取上传文件内容（仅文本，限制大小）。
    如果指定 session_id，则只在该会话目录下查找文件；否则在 static 根目录查找。
    文件名需在指定目录内，禁止路径穿越。
    任何异常都捕获并返回 None，避免调用方报错。
    """
    try:
        if not filename or ".." in filename:
            return None
            
        # 确定根目录
        if session_id:
            root = (STATIC_DIR / "sessions" / session_id).resolve()
        else:
            root = STATIC_DIR.resolve()
            
        # 处理文件名中的路径分隔符
        clean_filename = filename.replace("/", os.sep).replace("\\", os.sep)
        path = (root / clean_filename).resolve()
        
        # 安全校验：path 必须在 root 下（Windows 兼容）
        try:
            root_str = str(root)
            path_str = str(path)
            if not path_str.startswith(root_str.rstrip(os.sep) + os.sep) and path_str != root_str:
                return None
        except Exception:
            return None
            
        if not path.is_file():
            return None
            
        raw = path.read_bytes()
        if len(raw) > max_bytes:
            raw = raw[:max_bytes]
        return raw.decode("utf-8", errors="replace")
    except Exception:
        return None


def read_uploaded_file_for_llm(
    filename: str, 
    session_id: Optional[str] = None,
    max_bytes: int = 512 * 1024
) -> str:
    """
    供 LLM/Agent 调用的读文件：在指定会话目录内、禁止路径穿越。
    直接返回原始文本（任意格式：log、csv、json 等由模型自行理解），截断到 max_bytes。
    失败返回错误说明字符串（不抛异常）。
    """
    raw = read_uploaded_file(filename, session_id=session_id, max_bytes=max_bytes)
    if raw is None:
        if session_id:
            return f"[读取失败：会话 {session_id} 中文件不存在或不可读：{filename}]"
        else:
            return f"[读取失败：文件不存在或不可读：{filename}]"
    return raw


def query_code_graph(
    project_name: str,
    query_summary: Optional[str] = None,
    cypher: Optional[str] = None,
    limit: int = 100,
) -> str:
    """
    查询代码图谱（Neo4j）。若提供 cypher 则执行（只读）；否则用 project_name 查 Project 及周边节点摘要。
    返回可读文本，供 LLM 上下文使用。
    """
    try:
        from storage.neo4j.session_builder import get_project_neo_dao
        dao = get_project_neo_dao()
        if cypher:
            from neomodel import db as neo4j_db
            results, meta = neo4j_db.cypher_query(cypher, {"limit": limit})
            cols = list(meta)
            rows = [dict(zip(cols, row)) for row in results]
        else:
            row = dao.get_project_summary(project_name)
            rows = [row] if row else []
        if not rows:
            return f"[项目 {project_name} 在图库中无匹配节点或无权限]"
        import json as _json
        formatted = []
        for r in rows:
            try:
                row_dict = dict(r)
                for k, v in row_dict.items():
                    if isinstance(v, str):
                        row_dict[k] = v.replace("\\n", "\n").replace("\\t", "\t")
                formatted.append(_json.dumps(row_dict, ensure_ascii=False, indent=2))
            except Exception:
                formatted.append(str(r))
        return "\n---\n".join(formatted[:30])
    except Exception as e:
        return f"[图库查询异常: {e}]"


def gather_tool_context(
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    use_graph: bool = True,
    use_uploads: bool = True,
    uploaded_file_names: Optional[List[str]] = None,
) -> str:
    """兼容旧接口：仅返回上下文字符串。"""
    events, context_str = gather_tool_context_with_events(
        project_name=project_name,
        ref=ref,
        use_graph=use_graph,
        use_uploads=use_uploads,
        uploaded_file_names=uploaded_file_names,
    )
    return context_str


def _files_from_names(names: List[str]) -> List[Dict[str, Any]]:
    """仅根据文件名列表从 static 中取存在的文件信息（不按 project/ref 过滤）。"""
    if not STATIC_DIR.exists() or not names:
        return []
    out: List[Dict[str, Any]] = []
    for name in names:
        if not name or ".." in name or "/" in name or "\\" in name:
            continue
        path = STATIC_DIR / name
        if not path.is_file():
            continue
        try:
            size = path.stat().st_size
        except OSError:
            size = 0
        out.append({"name": name, "path": f"static/{name}", "size": size})
    return out


def gather_tool_context_with_events(
    project_name: Optional[str] = None,
    ref: Optional[str] = None,
    use_graph: bool = True,
    use_uploads: bool = True,
    uploaded_file_names: Optional[List[str]] = None,
) -> tuple[List[Dict[str, Any]], str]:
    """
    汇总工具上下文，并返回 (用于 SSE 的 tool 事件列表, 上下文字符串)。
    若传入 uploaded_file_names，则仅将这批文件纳入可读列表并读取；否则按 project_name+ref 从 static 列出。
    """
    events: List[Dict[str, Any]] = []
    parts: List[str] = []

    if use_graph and project_name:
        graph_text = query_code_graph(project_name)
        events.append({
            "kind": "graph_query",
            "title": "代码图谱查询",
            "content": graph_text,
        })
        parts.append("## 代码图谱摘要\n" + graph_text)

    if use_uploads:
        scoped_to_session_uploads = uploaded_file_names is not None
        if scoped_to_session_uploads:
            # 显式指定“本次上传范围”（哪怕为空）时，不允许读取历史文件
            files = _files_from_names(uploaded_file_names or [])
        else:
            files = list_uploaded_files(project_name=project_name, ref=ref)
        if not files:
            events.append({
                "kind": "file_list",
                "title": "用户上传文件列表",
                "content": "（本次未上传文件 / 或上传文件不在允许范围内）" if scoped_to_session_uploads else "（当前项目/版本下暂无用户上传文件）",
            })
            parts.append(
                "## 用户上传文件（本次可用）\n"
                + (
                    "（本次未上传文件，因此不会读取任何历史文件；如需读取，请先在左侧上传并重新发送。）"
                    if scoped_to_session_uploads
                    else "（当前项目/版本下暂无用户上传文件；若用户已上传，请提示其确认项目名称与分支/提交是否与本次选择一致。）"
                )
            )
        else:
            lines = [
                "以下是你本次可查看的用户上传文件列表，回答时可直接引用文件名。",
                "列表中部分文件下方会附带「内容摘要」，未附带内容的文件请根据文件名推断用途或请用户补充说明。",
                "",
                "**文件列表：**",
            ]
            for f in files:
                lines.append(f"  - 文件名: `{f['name']}` （大小: {f.get('size', 0)} bytes）")
            file_list_content = "\n".join(lines)
            events.append({
                "kind": "file_list",
                "title": "用户上传文件列表",
                "content": file_list_content,
            })
            parts.append("## 用户上传文件（本次可用）\n" + file_list_content)
            for f in files[:5]:
                try:
                    content = read_uploaded_file(f["name"], max_bytes=8192)
                except Exception as e:
                    content = None
                    events.append({
                        "kind": "file_read",
                        "title": f"查看文件：{f['name']}",
                        "content": f"[读取失败: {e}]",
                    })
                if content:
                    snippet = content[:4000]
                    events.append({
                        "kind": "file_read",
                        "title": f"查看文件：{f['name']}",
                        "content": snippet,
                    })
                    parts.append(f"### 文件 `{f['name']}` 内容摘要\n" + snippet)
    context_str = "\n\n".join(parts) if parts else ""
    return events, context_str


# --------------- 本地源码搜索 / 读取 ---------------

def _repo_dir(project_name: str) -> Optional[Path]:
    """根据 project_name 推导本地 git 仓库目录。"""
    from api.config import GIT_CACHE_DIR
    if not project_name:
        return None
    p = GIT_CACHE_DIR / project_name
    return p if p.is_dir() else None


def get_project_dependencies(project_name: str) -> str:
    """
    查询某项目依赖了哪些其他已导入项目（通过手动关联的 DEPENDS_ON 边）。
    返回可读文本，供 AI 决定下一步去哪个项目查源码。
    """
    try:
        from storage.neo4j.session_builder import get_project_neo_dao
        rows = get_project_neo_dao().get_depends_on(project_name)
    except Exception as e:
        return f"[查询依赖关系失败: {e}]"

    if not rows:
        return f"[项目 {project_name!r} 在图谱中没有 DEPENDS_ON 关系，请先在「应用详情」页手动关联二方包依赖]"
    lines = [f"项目 {project_name!r} 依赖以下已导入项目：\n"]
    for d in rows:
        lines.append(
            f"  - {d.get('dep_project')}  "
            f"({d.get('group_id')}:{d.get('artifact_id')})  "
            f"[{d.get('project_type', 'Application')}]"
        )
    lines.append("\n可用 query_code_graph(project_name=<dep_project>) 查询对应项目的图谱，")
    lines.append("或用 search_code / read_source_file 并传入 project_name=<dep_project> 读取其源码。")
    return "\n".join(lines)


def search_code(
    project_name: str,
    pattern: str,
    file_glob: str = "**/*.java",
    max_results: int = 20,
    context_lines: int = 3,
) -> str:
    """
    在本地项目源码中用正则搜索，返回匹配行及上下文（纯 Python 实现，跨平台）。
    project_name: 项目名，用于定位本地 git 仓库目录（可显式传入，不填则用会话上下文项目）
    pattern: 正则表达式
    file_glob: 文件匹配模式，默认 **/*.java
    max_results: 最多返回多少处匹配
    context_lines: 每处匹配前后各保留几行
    """
    repo = _repo_dir(project_name)
    if repo is None:
        return f"[找不到项目 {project_name!r} 的本地仓库，请确认已克隆到 .cache/git_repos/{project_name}]"

    try:
        regex = re.compile(pattern)
    except re.error as e:
        return f"[正则表达式错误: {e}]"

    results: List[str] = []
    count = 0
    for filepath in sorted(repo.rglob(file_glob)):
        if not filepath.is_file():
            continue
        try:
            lines = filepath.read_text(encoding="utf-8", errors="replace").splitlines()
        except Exception:
            continue
        rel = filepath.relative_to(repo)
        for i, line in enumerate(lines):
            if regex.search(line):
                start = max(0, i - context_lines)
                end = min(len(lines), i + context_lines + 1)
                snippet_lines = []
                for j in range(start, end):
                    prefix = ">>>" if j == i else "   "
                    snippet_lines.append(f"{prefix} {j + 1:4d}: {lines[j]}")
                results.append(f"# {rel}:{i + 1}\n" + "\n".join(snippet_lines))
                count += 1
                if count >= max_results:
                    break
        if count >= max_results:
            break

    if not results:
        return f"[未找到匹配 {pattern!r} 的代码（glob={file_glob}）]"
    header = f"共找到 {count} 处匹配（最多 {max_results}）：\n\n"
    return header + "\n\n".join(f"```java\n{r}\n```" for r in results)


# --------------- Forward 系统 Bug 详情 ---------------

# 无需 AI 修复的终态（已关闭/已完成/已转需求/修复中/等待上线等）
_BUG_TERMINAL_STATUSES: dict[int, str] = {
    2: "关闭",
    4: "问题修复（已有人在修复中）",
    5: "QA修复确认（修复已提交）",
    6: "待上线（修复已完成）",
    8: "完成",
    9: "已转需求",
    11: "待验收（修复已提交等待验收）",
}


def get_bug_detail(bug_id: int) -> str:
    """
    从 Forward 产研系统获取线上 Bug 详情。
    只返回 AI 分析必要的字段：name、status、describe、files。
    返回格式化文本，供 LLM 上下文使用。
    """
    import os as _os
    import json as _json
    try:
        from api.clients.forward_client import ForwardClient
    except ImportError:
        return "[无法导入 ForwardClient，请检查项目依赖]"

    try:
        with ForwardClient.from_env() as c:
            raw = c.get_bug(bug_id)
    except Exception as e:
        return f"[获取 Bug 详情失败: {e}]"

    if not raw:
        return f"[Bug #{bug_id} 不存在或返回为空]"

    # 只提取 AI 需要的字段
    name        = raw.get("name") or ""
    status      = raw.get("status")
    status_name = raw.get("statusName") or ""
    describe    = raw.get("describe") or "无描述信息"
    # 附件字段：接口返回 "files" 列表（非 "attaches"）
    attaches    = raw.get("files") or raw.get("attaches") or []

    # 检查是否处于无需修复的终态
    if status in _BUG_TERMINAL_STATUSES:
        reason = _BUG_TERMINAL_STATUSES[status]
        return (
            f"## Bug #{bug_id} 无需 AI 修复\n"
            f"**标题**: {name}\n"
            f"**状态**: {status_name}(状态码={status})\n"
            f"**原因**: {reason}，该 Bug 已处于无需 AI 介入的状态，请告知用户无需进行修复分析。"
        )

    lines = [
        f"## Bug #{bug_id} 详情",
        f"**标题**: {name}",
        f"**状态**: {status_name}(状态码={status})",
        f"**描述**:\n{describe}",
    ]

    # 附件列表：无 downloadUrl 的附件直接寻过
    valid_files: list[str] = []
    skipped_files: list[str] = []
    for f in (attaches if isinstance(attaches, list) else []):
        if not isinstance(f, dict):
            continue
        fname = f.get("fileName") or ""
        furl  = (f.get("downloadUrl") or "").strip()
        if not furl:
            skipped_files.append(fname or "(未知文件)")
            continue
        valid_files.append(f"  - {fname}\n    下载地址: {furl}")

    if valid_files:
        lines.append("**附件列表**:")
        lines.extend(valid_files)
    else:
        lines.append("**附件列表**: 无可用附件")

    result = "\n".join(lines)

    # 被跳过的附件通过 SSE hint 提示（返回给调用方，由上层在工具结果中展示）
    if skipped_files:
        skipped_hint = "\n\n> ℹ️ 以下附件因无下载地址已忽略：" + "、".join(skipped_files)
        result += skipped_hint

    return result


def get_bug_attachments(
    bug_id: int, 
    attachment_urls: List[Dict[str, str]], 
    session_id: Optional[str] = None,
    max_file_size: int = 2 * 1024 * 1024
) -> str:
    """
    下载 Bug 的附件到指定会话目录，供 AI 分析使用。
    
    参数:
        bug_id: Bug ID
        attachment_urls: 附件信息列表，每个元素包含 {"fileName": "文件名", "downloadUrl": "下载地址"}
        session_id: 会话ID，如果指定则下载到 sessions/{session_id}/ 目录下
        max_file_size: 单个文件最大大小（字节），默认 2MB
    
    返回:
        下载成功的文件列表信息，格式化文本供 LLM 使用
        如果下载失败会返回错误信息
    """
    import os as _os
    from pathlib import Path as _Path
    import urllib.request as _request
    from urllib.parse import urlparse as _urlparse
    
    if not isinstance(attachment_urls, list) or not attachment_urls:
        return "[无附件需要下载]"
    
    # 确定下载目录
    if session_id:
        temp_dir = STATIC_DIR / "sessions" / session_id / f"bug_{bug_id}_attachments"
    else:
        temp_dir = STATIC_DIR / f"bug_{bug_id}_attachments"
        
    temp_dir.mkdir(parents=True, exist_ok=True)
    
    downloaded_files = []
    failed_files = []
    
    for i, attachment in enumerate(attachment_urls):
        if not isinstance(attachment, dict):
            continue
            
        file_name = attachment.get("fileName") or f"attachment_{i}"
        download_url = (attachment.get("downloadUrl") or "").strip()
        
        if not download_url:
            failed_files.append(f"{file_name} (无下载链接)")
            continue
        
        try:
            # 解析文件扩展名
            parsed_url = _urlparse(download_url)
            url_path = parsed_url.path
            ext = _Path(url_path).suffix or ".bin"
            
            # 生成安全的本地文件名
            safe_name = f"bug_{bug_id}_{i:03d}_{file_name}"
            if not safe_name.endswith(ext):
                safe_name += ext
                
            local_path = temp_dir / safe_name
            
            # 下载文件
            req = _request.Request(
                download_url,
                headers={
                    'User-Agent': 'Bugfix-Agent/1.0',
                    # 如果需要认证头部，可以从环境变量获取
                }
            )
            
            with _request.urlopen(req, timeout=30) as response:
                # 检查文件大小
                content_length = response.headers.get('content-length')
                if content_length:
                    size = int(content_length)
                    if size > max_file_size:
                        failed_files.append(f"{file_name} (文件过大: {size//1024}KB)")
                        continue
                
                # 读取并保存文件
                content = response.read(max_file_size + 1)
                if len(content) > max_file_size:
                    failed_files.append(f"{file_name} (文件过大)")
                    continue
                    
                local_path.write_bytes(content)
                
                # 计算相对于会话目录的路径
                if session_id:
                    relative_path = local_path.relative_to(STATIC_DIR / "sessions" / session_id)
                    file_path_for_llm = str(relative_path).replace("\\", "/")
                else:
                    file_path_for_llm = str(local_path.relative_to(STATIC_DIR)).replace("\\", "/")
                    
                downloaded_files.append({
                    "name": file_name,
                    "local_path": file_path_for_llm,
                    "size": len(content),
                    "url": download_url
                })
                
        except Exception as e:
            failed_files.append(f"{file_name} ({str(e)})")
    
    # 生成返回结果
    lines = [f"## Bug #{bug_id} 附件下载结果"]
    
    if downloaded_files:
        lines.append(f"**成功下载 {len(downloaded_files)} 个文件**:")
        for f in downloaded_files:
            size_kb = f['size'] // 1024
            lines.append(f"  - {f['name']} ({size_kb}KB)")
            lines.append(f"    文件路径: {f['local_path']}")
    
    if failed_files:
        lines.append(f"**下载失败 {len(failed_files)} 个文件**:")
        for fail in failed_files:
            lines.append(f"  - {fail}")
    
    # 提示 AI 可以使用 read_uploaded_file 工具读取这些文件
    if downloaded_files:
        file_names = [f["local_path"] for f in downloaded_files]
        lines.append("")
        lines.append("> 💡 提示：可以使用 read_uploaded_file 工具读取这些文件进行分析")
        if session_id:
            lines.append(f"> 会话ID: {session_id}")
        lines.append(f"> 可读取的文件名: {', '.join(file_names)}")
    
    return "\n".join(lines)


def cleanup_bug_attachments(bug_id: int, session_id: Optional[str] = None) -> str:
    """
    清理指定 Bug 的下载附件。
    
    参数:
        bug_id: Bug ID
        session_id: 会话ID，如果指定则清理 sessions/{session_id}/ 目录下的附件
    
    返回:
        清理结果信息
    """
    import shutil as _shutil
    
    # 确定清理目录
    if session_id:
        temp_dir = STATIC_DIR / "sessions" / session_id / f"bug_{bug_id}_attachments"
    else:
        temp_dir = STATIC_DIR / f"bug_{bug_id}_attachments"
    
    if not temp_dir.exists():
        if session_id:
            return f"[无需清理：会话 {session_id} 中 Bug #{bug_id} 的附件目录不存在]"
        else:
            return f"[无需清理：Bug #{bug_id} 的附件目录不存在]"
    
    try:
        _shutil.rmtree(temp_dir)
        if session_id:
            return f"[已清理会话 {session_id} 中 Bug #{bug_id} 的附件目录]"
        else:
            return f"[已清理 Bug #{bug_id} 的附件目录]"
    except Exception as e:
        return f"[清理失败: {e}]"


def cleanup_session(session_id: str) -> str:
    """
    清理指定会话的所有文件（包括上传文件和下载的附件）。
    
    参数:
        session_id: 会话ID
    
    返回:
        清理结果信息
    """
    import shutil as _shutil
    
    session_dir = STATIC_DIR / "sessions" / session_id
    
    if not session_dir.exists():
        return f"[无需清理：会话 {session_id} 不存在]"
    
    try:
        _shutil.rmtree(session_dir)
        return f"[已清理会话 {session_id} 的所有文件]"
    except Exception as e:
        return f"[清理失败: {e}]"


def _append_import_analysis(source_lines: List[str]) -> str:
    """
    从源码行中提取 import 语句，分析每个 import 的归属。
    - 精确 import（import a.b.C）：直接按 FQN 查
    - 通配符 import（import a.b.*）：扫描代码正文中出现的大写开头标识符，
      按 simple_name 查 DB，再过滤包名匹配通配符前缀的结果，还原出 FQN
    仅当存在二方包时才附加分析结果（避免三方包噪音）。
    """
    exact_fqns: List[str] = []
    wildcard_prefixes: List[str] = []  # 通配符包前缀，如 com.timevale.forward.dal.dao
    in_import_block = True

    for line in source_lines:
        stripped = line.strip()
        if not in_import_block:
            break
        if stripped.startswith("import ") and stripped.endswith(";"):
            fqn = stripped.removeprefix("import static ").removeprefix("import ").rstrip(";").strip()
            if not fqn:
                continue
            if fqn.endswith(".*"):
                wildcard_prefixes.append(fqn[:-2])  # 去掉 .*
            else:
                exact_fqns.append(fqn)
        elif stripped and not stripped.startswith("//") and not stripped.startswith("package"):
            if not stripped.startswith("import") and not stripped.startswith("@") \
                    and not stripped.startswith("/*") and not stripped.startswith("*"):
                in_import_block = False

    # 通配符处理：从代码正文提取大写开头的标识符（潜在类名），反向查 DB
    resolved_from_wildcard: List[str] = []
    if wildcard_prefixes:
        try:
            from storage.sqlite.jar_class_db import get_jar_class_db
            db = get_jar_class_db()
            # 收集正文中所有大写开头的单词（类名候选）
            body_text = "\n".join(source_lines)
            candidates = set(re.findall(r'\b([A-Z][A-Za-z0-9_$]*)\b', body_text))
            for simple_name in candidates:
                matches = db.query_by_simple_name(simple_name, include_anonymous=False)
                for m in matches:
                    # 只保留包名匹配某个通配符前缀的结果
                    if any(m.package_name == prefix or m.package_name.startswith(prefix + ".")
                           for prefix in wildcard_prefixes):
                        resolved_from_wildcard.append(m.fqn)
        except Exception:
            pass

    all_fqns = exact_fqns + list(dict.fromkeys(resolved_from_wildcard))  # 去重保序
    if not all_fqns:
        return ""

    analysis = resolve_imports(all_fqns)
    if "【二方包】" not in analysis:
        return ""
    return "\n\n---\n" + analysis


def read_source_file(
    project_name: str,
    file_path: str,
    start_line: int = 1,
    end_line: int = 0,
    max_lines: int = 300,
) -> str:
    """
    读取本地文件内容（源码/配置等文本）。
    project_name: 项目名，可显式传入以读取其他项目（如二方包项目）的源码，不填则用会话上下文项目。
    file_path 支持两种格式：
      - 相对于仓库根目录的相对路径，如 service/src/main/java/com/timevale/forward/service/impl/ProjectServiceImpl.java
      - 图谱 File 节点的 full_path 绝对路径，会自动转换为相对路径
    start_line/end_line: 1-based，end_line=0 表示读到文件末尾。
    max_lines: 最多返回行数，防止超长。
    """
    import os as _os

    # 清洗路径字符串：Neo4j 里可能带引号/多余空白
    file_path = (file_path or "").strip().strip('"').strip("'")

    fp = Path(file_path)
    if fp.is_absolute():
        # 用户要求不做安全限制：绝对路径直接读取（full_path 只要存在就能读到）
        try:
            target_abs = fp.resolve()
        except Exception:
            target_abs = fp
        if not target_abs.is_file():
            return f"[文件不存在: {file_path}]"
        try:
            lines = target_abs.read_text(encoding="utf-8", errors="replace").splitlines()
        except Exception as e:
            return f"[读取失败: {e}]"

        total = len(lines)
        s = max(1, start_line) - 1          # 转 0-based
        e_ = (end_line if end_line > 0 else total)
        e_ = min(e_, s + max_lines, total)  # 不超过 max_lines 且不越界
        chunk = lines[s:e_]
        numbered = [f"{s + i + 1:4d}: {l}" for i, l in enumerate(chunk)]
        header = f"{str(target_abs)}  (lines {s + 1}–{s + len(chunk)} / {total})\n"
        code_block = "```java\n" + header + "\n".join(numbered) + "\n```"
        return code_block + _append_import_analysis(lines)

    repo = _repo_dir(project_name)
    if repo is None:
        return f"[找不到项目 {project_name!r} 的本地仓库]"

    repo_resolved = repo.resolve()

    # 安全校验：禁止路径穿越
    try:
        target = (repo_resolved / file_path).resolve()
        if not str(target).startswith(str(repo_resolved)):
            return "[错误：路径穿越，拒绝访问]"
    except Exception as e:
        return f"[路径解析失败: {e}]"

    if not target.is_file():
        # 尝试在子模块中模糊匹配，给出候选路径提示
        filename = Path(file_path).name
        candidates = [str(p.relative_to(repo)) for p in repo.rglob(filename) if p.is_file()][:5]
        if candidates:
            # 自动选第一个候选继续读取，避免前端看到“文件不存在”噪音
            file_path = candidates[0]
            try:
                target = (repo_resolved / file_path).resolve()
            except Exception as e:
                return f"[路径解析失败: {e}]"
        else:
            return f"[文件不存在: {file_path}（未找到同名文件）]"

    try:
        lines = target.read_text(encoding="utf-8", errors="replace").splitlines()
    except Exception as e:
        return f"[读取失败: {e}]"

    total = len(lines)
    s = max(1, start_line) - 1          # 转 0-based
    e_ = (end_line if end_line > 0 else total)
    e_ = min(e_, s + max_lines, total)  # 不超过 max_lines 且不越界
    chunk = lines[s:e_]
    numbered = [f"{s + i + 1:4d}: {l}" for i, l in enumerate(chunk)]
    header = f"{file_path}  (lines {s + 1}–{s + len(chunk)} / {total})\n"
    code_block = "```java\n" + header + "\n".join(numbered) + "\n```"
    return code_block + _append_import_analysis(lines)


def clone_git_repo(
    project_name: str,
    repo_url: str,
    branch: Optional[str] = None,
    commit_id: Optional[str] = None,
    timeout: Optional[int] = None,
) -> str:
    """
    根据 repo_url + branch/commit_id 拉取 Git 仓库到本地缓存目录（.cache/git_repos/<project_name>）。
    若本地已存在有效 git 仓库则静默返回（前缀 [CACHED]），不做任何操作；
    若不存在则执行 clone（前缀 [CLONED]）。
    仅拉取仓库，不做任何代码图谱构建操作。
    """
    try:
        from git.manager import GitManager
        from tools.constants import CACHE_GIT_REPOS_PATH

        repo_name = project_name.strip()
        if not repo_name:
            return "[错误：project_name 不能为空]"
        if not repo_url or not repo_url.strip():
            return "[错误：repo_url 不能为空]"

        repo_cache_dir = str(CACHE_GIT_REPOS_PATH / repo_name)
        git_manager = GitManager(repo_cache_dir)

        # 本地已有有效仓库 → 静默返回，不拉取
        if git_manager.is_repo_exists():
            return f"[CACHED] 本地已存在仓库缓存，无需重新拉取（{repo_cache_dir}）"

        # 本地不存在 → 执行 clone
        git_config = {"core.safecrlf": "false", "core.autocrlf": "false"}
        shallow = commit_id is None
        clone_timeout = timeout or GitManager.calculate_dynamic_timeout(repo_url)
        target_branch = branch or "master"
        ok, msg = git_manager.clone(
            repo_url, target_branch, shallow=shallow,
            timeout=clone_timeout, git_config=git_config,
        )
        if not ok:
            return f"[克隆失败：{msg}]"

        if commit_id:
            ok, msg = git_manager.checkout(commit_id)
            if not ok:
                return f"[CLONED] 克隆成功，但切换到 commit {commit_id} 失败：{msg}"
            return f"[CLONED] 成功克隆仓库并切换到 commit {commit_id}，缓存目录：{repo_cache_dir}"

        return f"[CLONED] 成功克隆仓库（branch={target_branch}），缓存目录：{repo_cache_dir}"

    except Exception as e:
        return f"[clone_git_repo 执行异常：{e}]"


# --------------- Import 归属解析 ---------------

def _load_second_party_rules() -> List[Dict[str, Any]]:
    """加载二方包判断规则（group_id_regex + artifact_id_regex）。"""
    try:
        from storage.sqlite.business.business_db import get_business_db
        from storage.sqlite.business.second_party_rules_repo import SecondPartyRulesRepo
        db = get_business_db()
        repo = SecondPartyRulesRepo(db)
        return [r for r in repo.list_rules() if r.get("enabled")]
    except Exception:
        return []


def _is_second_party(group_id: str, artifact_id: str, rules: List[Dict[str, Any]]) -> bool:
    """按规则列表判断坐标是否属于二方包。"""
    for rule in rules:
        try:
            if re.fullmatch(rule["group_id_regex"], group_id or "") and \
               re.fullmatch(rule["artifact_id_regex"], artifact_id or ""):
                return True
        except re.error:
            continue
    return False


def resolve_imports(fqns: List[str]) -> str:
    """
    给定一批 Java 类的全限定名，查询它们来自哪个 jar/坐标，
    并标注是否为二方包（根据 second_party_rules 规则匹配）。
    用于帮助 AI 判断某个 import 是本项目源码、二方包还是三方包，
    从而决定是否需要跳转到对应项目的图谱或源码继续排查。
    """
    if not fqns:
        return "[未提供任何 FQN]"

    try:
        from storage.sqlite.jar_class_db import get_jar_class_db
    except ImportError:
        return "[无法导入 JARClassDB，请确认项目依赖]"

    db = get_jar_class_db()
    rules = _load_second_party_rules()
    lines: List[str] = ["## Import 归属分析\n"]

    for fqn in fqns:
        fqn = fqn.strip()
        if not fqn:
            continue
        info = db.query_by_fqn(fqn)
        if info is None:
            lines.append(
                f"- `{fqn}`\n"
                f"  → 未在 jar_classes.db 中找到（可能是本项目源码或未扫描的依赖）\n"
                f"  → 建议：直接用 query_code_graph 或 search_code 在当前项目中查找\n"
            )
            continue

        g = info.artifact_group_id or info.parent_group_id or ""
        a = info.artifact_id or info.parent_artifact_id or ""
        v = info.artifact_version or info.parent_version or ""
        coord = f"{g}:{a}:{v}" if g or a else info.jar_name

        if _is_second_party(g, a, rules):
            kind = "【二方包】"
            hint = (
                f"  → 建议：先用 get_project_dependencies 确认是否已导入该项目，\n"
                f"    再用 query_code_graph(project_name=\"{a}\") 查图谱，\n"
                f"    或 search_code / read_source_file 并传入 project_name=\"{a}\" 读取源码。"
            )
        else:
            kind = "三方包（通常无需深入排查）"
            hint = "  → 建议：三方包一般不需要查源码，关注调用方的使用姿势即可。"

        lines.append(
            f"- `{fqn}`\n"
            f"  → 坐标: `{coord}`\n"
            f"  → 类型: {kind}\n"
            f"{hint}\n"
        )

    return "\n".join(lines)


# --------------- Java 反编译 ---------------

def decompile_class(fqn: str, session_id: Optional[str] = None) -> str:
    """
    根据 Java 类的全限定名（FQN），从 jar_classes.db 找到对应 jar，
    提取 .class 文件并反编译，返回可读的类结构或源码。

    优先使用 CFR（完整源码），若未配置则回退到 javap（方法签名）。
    CFR jar 放置路径：.cache/cfr.jar（或通过环境变量 CFR_JAR 指定）。
    提取的 .class 文件放在 static/sessions/<session_id>/decompile/ 下。

    适用场景：
    - 排查三方包行为（如 Spring、MyBatis 内部逻辑）
    - 图谱中不存在的二方包类
    - 任何只有 jar 没有源码的依赖
    """
    import zipfile
    import subprocess
    import shutil

    fqn = (fqn or "").strip()
    if not fqn:
        return "[错误：FQN 不能为空]"

    # 1. 从 jar_classes.db 查找 jar 路径和 class 文件路径
    try:
        from storage.sqlite.jar_class_db import get_jar_class_db
    except ImportError:
        return "[无法导入 JARClassDB]"

    db = get_jar_class_db()
    info = db.query_by_fqn(fqn)
    if info is None:
        simple = fqn.rsplit(".", 1)[-1]
        candidates = db.query_by_simple_name(simple, include_anonymous=False)
        if candidates:
            hints = "\n".join(f"  - {c.fqn}  ({c.jar_name})" for c in candidates[:5])
            return f"[未找到 FQN `{fqn}`，同名类候选：\n{hints}\n请确认 FQN 是否正确]"
        return f"[未找到 FQN `{fqn}`，该类可能未被索引，请先重建 JAR 索引]"

    jar_path = info.jar_path
    class_entry = info.file_path
    if not class_entry:
        class_entry = fqn.replace(".", "/") + ".class"

    if not Path(jar_path).exists():
        return f"[jar 文件不存在：{jar_path}]"

    # 2. 确定工作目录：session 目录下的 decompile/ 子目录
    from api.config import STATIC_DIR
    if session_id:
        work_dir = STATIC_DIR / "sessions" / session_id / "decompile"
    else:
        work_dir = STATIC_DIR / "decompile"
    work_dir.mkdir(parents=True, exist_ok=True)

    # 3. 从 jar 中提取 .class 到工作目录
    try:
        with zipfile.ZipFile(jar_path, "r") as zf:
            names = zf.namelist()
            matched = class_entry if class_entry in names else next(
                (n for n in names if n.endswith("/" + class_entry.split("/")[-1]) and
                 fqn.replace(".", "/") in n), None
            )
            if not matched:
                return f"[在 jar 中未找到 class 文件：{class_entry}（jar: {Path(jar_path).name}）]"
            zf.extract(matched, work_dir)
            class_file = str(work_dir / matched)
    except zipfile.BadZipFile:
        return f"[jar 文件损坏：{jar_path}]"

    # 4. 优先尝试 CFR
    from tools.constants import CACHE_ROOT_PATH
    cfr_env = os.environ.get("CFR_JAR") or ""
    cfr_path = next(
        (p for p in [cfr_env, str(CACHE_ROOT_PATH / "cfr.jar")] if p and Path(p).exists()),
        None,
    )

    coord = (
        f"{info.artifact_group_id or info.parent_group_id or '?'}:"
        f"{info.artifact_id or info.parent_artifact_id or '?'}:"
        f"{info.artifact_version or info.parent_version or '?'}"
    )

    if cfr_path:
        try:
            r = subprocess.run(
                ["java", "-jar", cfr_path, class_file, "--silent", "true"],
                capture_output=True, text=True, timeout=30,
                encoding="utf-8", errors="replace",
            )
            output = (r.stdout or "").strip()
            if output and "Exception" not in output[:50]:
                return f"// 反编译来源: {Path(jar_path).name}  坐标: {coord}\n// 工具: CFR\n\n{output}"
        except Exception:
            pass

    # 5. 回退 javap
    javap = shutil.which("javap") or "javap"
    try:
        r = subprocess.run(
            [javap, "-p", "-c", class_file],
            capture_output=True, text=True, timeout=15,
            encoding="utf-8", errors="replace",
        )
        output = (r.stdout or r.stderr or "").strip()
        if not output:
            return f"[javap 无输出，class 文件可能损坏：{class_file}]"
        note = (
            "// ⚠️  当前使用 javap 输出方法签名（无方法体）。\n"
            "// 如需完整源码，请将 CFR jar 放到 .cache/cfr.jar 或设置环境变量 CFR_JAR。\n"
            "// 下载地址：https://github.com/leibnitz27/cfr/releases\n\n"
        )
        return f"// 反编译来源: {Path(jar_path).name}  坐标: {coord}\n// 工具: javap\n{note}{output}"
    except FileNotFoundError:
        return "[javap 未找到，请确认 JDK 已安装并加入 PATH]"
    except subprocess.TimeoutExpired:
        return "[反编译超时]"

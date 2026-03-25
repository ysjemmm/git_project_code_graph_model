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
    uri = os.environ.get("NEO4J_URI", "")
    user = os.environ.get("NEO4J_USER", "neo4j")
    password = os.environ.get("NEO4J_PASSWORD", "")
    database = os.environ.get("NEO4J_DATABASE", "neo4j")
    if not uri or not password:
        return "[图数据库未配置：缺少 NEO4J_URI / NEO4J_PASSWORD]"

    try:
        from storage.neo4j import Neo4jConnector
    except ImportError:
        return "[无法导入 Neo4j 连接器，请确认项目依赖]"

    conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
    if not conn.connect():
        return "[图数据库连接失败]"

    try:
        if cypher:
            rows = conn.execute_query(cypher, {"limit": limit})
        else:
            from storage.neo4j.queries import Neo4jQueries
            rows = conn.execute_query(Neo4jQueries.project_summary(), {"project_name": project_name})
        if not rows:
            return f"[项目 {project_name} 在图库中无匹配节点或无权限]"
        # 转成格式化 JSON，每行一个结果
        import json as _json
        formatted = []
        for r in rows:
            try:
                row_dict = dict(r)
                # raw_metadata 里的 \n 是真实换行，还原它
                for k, v in row_dict.items():
                    if isinstance(v, str):
                        row_dict[k] = v.replace("\\n", "\n").replace("\\t", "\t")
                formatted.append(_json.dumps(row_dict, ensure_ascii=False, indent=2))
            except Exception:
                formatted.append(str(dict(r)))
        return "\n---\n".join(formatted[:30])
    except Exception as e:
        return f"[图库查询异常: {e}]"
    finally:
        conn.disconnect()


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
    uri = os.environ.get("NEO4J_URI", "")
    user = os.environ.get("NEO4J_USER", "neo4j")
    password = os.environ.get("NEO4J_PASSWORD", "")
    database = os.environ.get("NEO4J_DATABASE", "neo4j")
    if not uri or not password:
        return "[图数据库未配置：缺少 NEO4J_URI / NEO4J_PASSWORD]"

    try:
        from storage.neo4j import Neo4jConnector
    except ImportError:
        return "[无法导入 Neo4j 连接器]"

    conn = Neo4jConnector(uri=uri, username=user, password=password, database=database)
    if not conn.connect():
        return "[图数据库连接失败]"

    try:
        from storage.neo4j.queries import Neo4jQueries
        rows = conn.execute_query(Neo4jQueries.get_project_depends_on(), {"name": project_name})
        if not rows:
            return f"[项目 {project_name!r} 在图谱中没有 DEPENDS_ON 关系，请先在「应用详情」页手动关联二方包依赖]"
        lines = [f"项目 {project_name!r} 依赖以下已导入项目：\n"]
        for r in rows:
            d = dict(r)
            lines.append(
                f"  - {d.get('dep_project')}  "
                f"({d.get('group_id')}:{d.get('artifact_id')})  "
                f"[{d.get('project_type', 'Application')}]"
            )
        lines.append("\n可用 query_code_graph(project_name=<dep_project>) 查询对应项目的图谱，")
        lines.append("或用 search_code / read_source_file 并传入 project_name=<dep_project> 读取其源码。")
        return "\n".join(lines)
    except Exception as e:
        return f"[查询依赖关系失败: {e}]"
    finally:
        conn.disconnect()


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
        return "```java\n" + header + "\n".join(numbered) + "\n```"

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
    return "```java\n" + header + "\n".join(numbered) + "\n```"

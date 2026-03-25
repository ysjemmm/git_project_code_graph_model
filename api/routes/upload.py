"""
用户文件上传接口。
"""
from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, Form, UploadFile

from api.config import STATIC_DIR
from api.utils import safe_filename

router = APIRouter(prefix="/api", tags=["upload"])


@router.post("/upload")
async def upload_files(
    project_name: str = Form(..., description="项目名（用于文件名前缀）"),
    ref: str = Form(..., description="branch 或 commitId"),
    files: list[UploadFile] = File(...),
    session_id: Optional[str] = Form(None, description="会话ID，如果指定则存入 sessions/{session_id}/ 目录"),
):
    """
    上传文件到 static/sessions/{session_id}/ 或 static/，命名：项目名-ref-时间戳[-序号].扩展名
    """
    # 确定上传目录
    if session_id:
        upload_dir = STATIC_DIR / "sessions" / safe_filename(session_id)
    else:
        upload_dir = STATIC_DIR
    upload_dir.mkdir(parents=True, exist_ok=True)

    safe_project = safe_filename(project_name)
    safe_ref = safe_filename(ref)
    ts = datetime.now().strftime("%Y%m%d%H%M%S")
    saved = []
    for i, f in enumerate(files):
        if not f.filename:
            continue
        ext = Path(f.filename).suffix or ""
        name = f"{safe_project}-{safe_ref}-{ts}{ext}" if len(files) == 1 else f"{safe_project}-{safe_ref}-{ts}-{i}{ext}"
        path = upload_dir / name
        content = await f.read()
        path.write_bytes(content)
        saved.append({"filename": name, "path": str(path.relative_to(STATIC_DIR)).replace("\\", "/")})
    return {"ok": True, "saved": saved, "session_id": session_id}

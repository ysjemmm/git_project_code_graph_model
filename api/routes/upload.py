"""
用户文件上传接口。
"""
from __future__ import annotations

from datetime import datetime
from pathlib import Path

from fastapi import APIRouter, File, Form, UploadFile

from api.config import STATIC_DIR
from api.utils import safe_filename

router = APIRouter(prefix="/api", tags=["upload"])


@router.post("/upload")
async def upload_files(
    project_name: str = Form(..., description="项目名（用于文件名前缀）"),
    ref: str = Form(..., description="branch 或 commitId"),
    files: list[UploadFile] = File(...),
):
    """
    上传文件到 static/，命名：项目名-ref-时间戳[-序号].扩展名
    """
    STATIC_DIR.mkdir(parents=True, exist_ok=True)
    safe_project = safe_filename(project_name)
    safe_ref = safe_filename(ref)
    ts = datetime.now().strftime("%Y%m%d%H%M%S")
    saved = []
    for i, f in enumerate(files):
        if not f.filename:
            continue
        ext = Path(f.filename).suffix or ""
        name = f"{safe_project}-{safe_ref}-{ts}{ext}" if len(files) == 1 else f"{safe_project}-{safe_ref}-{ts}-{i}{ext}"
        path = STATIC_DIR / name
        content = await f.read()
        path.write_bytes(content)
        saved.append({"filename": name, "path": f"static/{name}"})
    return {"ok": True, "saved": saved}

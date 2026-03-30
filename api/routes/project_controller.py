"""
项目相关新 API，旧 API 目前兼容存放在 cache.py
"""
from __future__ import annotations

import logging
from typing import Optional

from fastapi import APIRouter, Query, Depends

from api.request import PageRequest
from api.response import PageResponse
from api.schemas import CacheProjectItem, GraphProjectItem, GraphProjectVersionItem
from storage.neo4j.dao.project_neo_dao import ProjectNeoDao
from storage.neo4j.session_builder import get_project_neo_dao
from storage.sqlite.dao.project_dao import ProjectDAO
from storage.sqlite.session_builder import get_project_dao
from core.project_ids import parse_version_from_project_key

router = APIRouter(prefix="/api/project", tags=["project"])


@router.get("/list-for-import", response_model=PageResponse[CacheProjectItem])
def list_projects_for_import(
    project_name: Optional[str] = Query(None, description="项目名称过滤"),
    page_req: PageRequest = Depends(),
    _project_dao: ProjectDAO = Depends(get_project_dao),
):
    try:
        projects, total, cursor, _ = _project_dao.list_by_conditions(project_name, page_req)
        return PageResponse.success_response(
            page_size=page_req.page_size,
            current_cursor=page_req.cursor,
            next_cursor=cursor,
            total=total,
            data=[CacheProjectItem.model_validate(item) for item in projects]
        )
    except Exception as e:
        logging.error(f"[list-for-import]Error listing projects: {e}")
        return PageResponse.error_response(error=f"失败啦：{e}")


@router.get("/list-for-overview", response_model=PageResponse[GraphProjectItem])
def list_projects_for_overview(
    project_name: Optional[str] = Query(None, description="项目名称过滤"),
    page_req: PageRequest = Depends(),
    _project_neo_dao: ProjectNeoDao = Depends(get_project_neo_dao),
    _project_dao: ProjectDAO = Depends(get_project_dao),
):
    try:
        projects, total, cursor, _ = _project_dao.list_by_conditions(project_name, page_req)

        project_keys = [p.project_key for p in projects if p.project_key]

        # 批量查各项目所有版本
        names = [p.project_name for p in projects if p.project_name]
        versions_map = _project_neo_dao.list_versions_by_names(names)

        # 收集所有版本的 project_key 用于批量统计节点数
        all_version_keys = [
            v["project_key"]
            for vers in versions_map.values()
            for v in vers
            if v.get("project_key")
        ]
        counts = _project_neo_dao.count_all(all_version_keys)

        items = []
        for p in projects:
            vers = versions_map.get(p.project_name, [])

            # 取 active 版本的 project_key，兜底取最后一个版本
            active_ver = next((v for v in vers if v.get("is_active")), vers[-1] if vers else None)
            project_key = (active_ver or {}).get("project_key") or p.project_key

            # node_count 按所有版本的 project_key 汇总
            all_keys = [v["project_key"] for v in vers if v.get("project_key")]
            node_count = sum(
                sum(counts.get(pk, {}).values()) for pk in all_keys
            ) or None

            version = parse_version_from_project_key(project_key)

            version_items = [
                GraphProjectVersionItem(
                    version=parse_version_from_project_key(v["project_key"]),
                    project_key=v["project_key"],
                    branch=v["branch"],
                    commit_hash=v["commit_hash"],
                    node_count=sum(counts.get(v["project_key"], {}).values()) or None,
                )
                for v in vers
            ]

            items.append(GraphProjectItem(
                project_name=p.project_name,
                project_key=project_key,
                project_type=p.project_type,
                branch=p.head_branch,
                commit_hash=p.head_commit,
                repo_url=p.repo_url,
                last_update_time=p.last_update_time,
                version=version,
                versions=version_items,
            ))

        return PageResponse.success_response(
            page_size=page_req.page_size,
            current_cursor=page_req.cursor,
            next_cursor=cursor,
            total=total,
            data=items,
        )
    except Exception as e:
        logging.error(f"[list-for-overview]Error listing projects: {e}")
        return PageResponse.error_response(error=f"失败啦：{e}")


@router.get("/{project_name}/versions")
def list_project_versions(
    project_name: str,
    _project_neo_dao: ProjectNeoDao = Depends(get_project_neo_dao),
):
    """轻量接口：只返回某项目在图谱中已导入的版本列表，不含节点统计。"""
    try:
        versions = _project_neo_dao.list_project_versions(project_name)
        return {"ok": True, "items": versions}
    except Exception as e:
        logging.error(f"[list-project-versions] {e}")
        return {"ok": False, "items": [], "error": str(e)}

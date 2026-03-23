from __future__ import annotations

from typing import Any, Dict, List

from api.schemas import CacheProjectItem


def attach_link_stats_to_projects(db: Any, items: List[CacheProjectItem]) -> List[CacheProjectItem]:
    """
    为应用列表补充关联统计：
    - linked_by_count: 被多少条关联指向（入向）
    - linked_to_count: 指向多少条关联（出向）
    - linked_by_preview: 入向来源预览（项目名(次数)）
    """
    if not items:
        return items

    cur = db.conn.cursor()
    in_rows = cur.execute(
        "SELECT linked_app_id AS app_id, COUNT(*) AS c FROM app_dependency_links GROUP BY linked_app_id"
    ).fetchall()
    out_rows = cur.execute(
        "SELECT app_id, COUNT(*) AS c FROM app_dependency_links GROUP BY app_id"
    ).fetchall()
    in_preview_rows = cur.execute(
        """
        SELECT
          l.linked_app_id AS app_id,
          COALESCE(NULLIF(TRIM(s.project_name), ''), s.repo_name) AS source_name,
          COUNT(*) AS c
        FROM app_dependency_links l
        JOIN application_projects_cache s ON s.id = l.app_id
        GROUP BY l.linked_app_id, source_name
        ORDER BY l.linked_app_id, c DESC, source_name ASC
        """
    ).fetchall()

    in_map = {int(r["app_id"]): int(r["c"]) for r in in_rows if r["app_id"] is not None}
    out_map = {int(r["app_id"]): int(r["c"]) for r in out_rows if r["app_id"] is not None}
    preview_map: Dict[int, List[str]] = {}
    for r in in_preview_rows:
        aid = int(r["app_id"] or 0)
        if aid <= 0:
            continue
        name = str(r["source_name"] or "").strip() or "-"
        cnt = int(r["c"] or 0)
        preview_map.setdefault(aid, []).append(f"{name}({cnt})")

    return [
        it.model_copy(update={
            "linked_by_count": in_map.get(int(it.id or 0), 0),
            "linked_to_count": out_map.get(int(it.id or 0), 0),
            "linked_by_preview": preview_map.get(int(it.id or 0), [])[:6],
        })
        for it in items
    ]


def list_linked_by_items(db: Any, app_id: int) -> List[Dict[str, Any]]:
    """
    查询“哪些项目关联到了当前项目”（入向关联聚合）。
    """
    cur = db.conn.cursor()
    rows = cur.execute(
        """
        SELECT
          s.id AS source_app_id,
          s.repo_name AS source_repo_name,
          s.project_name AS source_project_name,
          COUNT(*) AS link_count,
          MAX(l.created_at) AS last_linked_at
        FROM app_dependency_links l
        JOIN application_projects_cache s ON s.id = l.app_id
        WHERE l.linked_app_id = ?
        GROUP BY s.id, s.repo_name, s.project_name
        ORDER BY link_count DESC, source_project_name ASC, source_repo_name ASC
        """,
        (app_id,),
    ).fetchall()

    return [
        {
            "source_app_id": int(r["source_app_id"]),
            "source_repo_name": str(r["source_repo_name"] or ""),
            "source_project_name": str(r["source_project_name"] or "") or str(r["source_repo_name"] or ""),
            "link_count": int(r["link_count"] or 0),
            "last_linked_at": str(r["last_linked_at"] or ""),
        }
        for r in rows
    ]

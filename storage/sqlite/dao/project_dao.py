from __future__ import annotations
from typing import Optional, List

from sqlalchemy.orm import Session

from api.request import PageRequest
from storage.sqlite.orm.project_orm import ProjectORM


class ProjectDAO:
    def __init__(self, session: Session):
        self.session = session

    def list_by_conditions(
            self,
            project_name: Optional[str] = None,
            page_request: Optional[PageRequest] = None
    ) -> tuple[list[ProjectORM], int, str | None, bool]:
        query = self.session.query(ProjectORM)

        if project_name:
            query = query.filter(
                ProjectORM.project_name.like(f"%{project_name}%")
            )

        total = query.count()

        next_cursor = None
        has_next = False
        items: List[ProjectORM] = []
        if page_request:
            if page_request.is_cursor_pagination():
                query = (query.filter(ProjectORM.id > int(page_request.cursor))
                         .order_by(ProjectORM.id.asc()))

                items = query.limit(page_request.page_size + 1).all()
                has_next = len(items) > page_request.page_size
                items = items[:page_request.page_size]

                # 计算下一页游标
                if has_next and items:
                    last_item = items[-1]
                    next_cursor = str(last_item.id)

                return items, total, next_cursor, has_next

            else:
                # 传统分页
                items = query.order_by(ProjectORM.id.asc()) \
                    .offset(page_request.offset) \
                    .limit(page_request.page_size) \
                    .all()
                return items, total, str(items[-1].id) if len(items) > 0 else None, False

        # 无分页
        items = query.order_by(ProjectORM.id.asc()).all()
        return items, total, None, False
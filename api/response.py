from pydantic import BaseModel, Field
from typing import Generic, TypeVar, List, Optional, Any

T = TypeVar('T')

class PageResponse(BaseModel, Generic[T]):
    """通用分页响应"""
    # 分页信息
    page: int = Field(1, description="当前页码")
    page_size: int = Field(20, description="每页数量")
    total: int = Field(0, description="总条数")
    total_pages: int = Field(0, description="总页数")
    
    # 游标信息（用于游标分页）
    current_cursor: Optional[str] = Field(None, description="当前页游标")
    next_cursor: Optional[str] = Field(None, description="下一页游标")
    
    # 数据
    data: List[T] = Field(default_factory=list, description="数据列表")
    
    # 状态
    success: bool = Field(True, description="是否成功")
    error: Optional[str] = Field(None, description="错误信息")
    
    @classmethod
    def success_response(
        cls,
        data: List[T],
        total: int,
        page: int = 1,
        page_size: int = 20,
        current_cursor: Optional[str] = None,
        next_cursor: Optional[str] = None
    ):
        """成功响应"""
        total_pages = (total + page_size - 1) // page_size if page_size > 0 else 0
        return cls(
            page=page,
            page_size=page_size,
            total=total,
            total_pages=total_pages,
            current_cursor=current_cursor,
            next_cursor=next_cursor,
            data=data,
            success=True
        )
    
    @classmethod
    def error_response(cls, error: str):
        """错误响应"""
        return cls(
            success=False,
            error=error,
            data=[]
        )
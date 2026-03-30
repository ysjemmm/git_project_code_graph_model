from pydantic import BaseModel, Field
from typing import Optional
from fastapi import Query

class PageRequest(BaseModel):
    """通用分页请求"""
    # 传统分页参数
    page: int = Field(1, ge=1, description="当前页码")
    page_size: int = Field(10, ge=1, description="每页数量")
    
    # 游标分页参数
    cursor: Optional[str] = Field(None, description="游标（用于游标分页）")
    
    @property
    def offset(self) -> int:
        """计算偏移量（用于传统分页）"""
        return (self.page - 1) * self.page_size
    
    @property
    def limit(self) -> int:
        """获取每页数量"""
        return self.page_size
    
    def is_cursor_pagination(self) -> bool:
        """判断是否使用游标分页"""
        return self.cursor is not None and self.cursor != ""
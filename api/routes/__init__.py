"""
路由模块：按领域拆分 LLM、Git、上传、Bugfix，便于维护。
"""
from api.routes.llm import router as llm_router
from api.routes.git import router as git_router
from api.routes.upload import router as upload_router
from api.routes.bugfix import router as bugfix_router
from api.routes.graph import router as graph_router
from api.routes.cache import router as cache_router
from api.routes.import_tasks import router as import_router
from api.routes.second_party_rules import router as second_party_rules_router
from api.routes.invoke import router as invoke_router
from api.routes.bugfix_history import router as bugfix_history_router

__all__ = [
    "llm_router",
    "git_router",
    "upload_router",
    "bugfix_router",
    "graph_router",
    "cache_router",
    "import_router",
    "second_party_rules_router",
    "invoke_router",
    "bugfix_history_router",
]

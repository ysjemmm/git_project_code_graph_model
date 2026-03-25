"""
Bugfix API 入口：加载环境、创建 FastAPI 应用并挂载各领域路由。
"""
from __future__ import annotations

from fastapi import FastAPI

from api.config import load_env
from api.routes import llm_router, git_router, upload_router, bugfix_router, graph_router, cache_router, import_router, second_party_rules_router, invoke_router, bugfix_history_router

# 在 import 其他 api 模块前加载 .env，确保 llm/agent 能读到正确环境变量
load_env()

app = FastAPI(title="Bugfix API", version="0.1.0")

app.include_router(llm_router)
app.include_router(git_router)
app.include_router(upload_router)
app.include_router(bugfix_router)
app.include_router(graph_router)
app.include_router(cache_router)
app.include_router(import_router)
app.include_router(second_party_rules_router)
app.include_router(invoke_router)
app.include_router(bugfix_history_router)

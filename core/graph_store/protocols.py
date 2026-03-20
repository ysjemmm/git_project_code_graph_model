from __future__ import annotations

from typing import Any, Dict, Protocol, runtime_checkable

from core.import_context import ProjectImportContext
from core.graph_ir import GraphBatch


@runtime_checkable
class GraphStore(Protocol):
    """
    图存储抽象层（Storage Adapter）。

    设计目标：
    - importer / language adapter 不直接依赖 Neo4j 细节
    - 允许未来替换存储（Neo4j / 其他图数据库 / 文件化等）而不改核心流程
    - 先以“导出 AST 数据”为最小闭包接口，后续可演进为语言无关 IR
    """

    def delete_project_subgraph(self, ctx: ProjectImportContext) -> int: ...

    def delete_file_subgraph(self, ctx: ProjectImportContext, file_path: str) -> int: ...

    def export_ast(
        self,
        ctx: ProjectImportContext,
        ast_data_list: list,
        symbol_table: Any,
        clear_database: bool,
    ) -> Dict: ...

    def write_batch(self, ctx: ProjectImportContext, batch: GraphBatch) -> Dict: ...


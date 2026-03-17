from __future__ import annotations

from typing import Any, List, Optional, Protocol, runtime_checkable

from core.import_context import ProjectImportContext
from core.graph_store.protocols import GraphStore


@runtime_checkable
class ParseResult(Protocol):
    """
    parse_files 的通用返回结构。
    importer/exporter 只依赖这三个字段，不强绑定具体 AST/符号表实现。
    """

    project_root_symbol_id: str
    ast_data_list: list
    symbol_table: Any


@runtime_checkable
class LanguageAdapter(Protocol):
    """
    语言适配器最小协议：GitToNeo4jImporter 仅依赖这些方法。

    约束目标：
    - 让后续新增语言（Kotlin/Python/Go 等）有明确的实现边界
    - 不强绑定具体 AST/符号表类型（用 Any 保持最小耦合）
    """

    source_extensions: List[str]

    def find_source_dirs(self, repo_path: str, source_dir: Optional[str] = None) -> List[str]: ...

    def find_source_files(self, repo_path: str, source_dirs: List[str]) -> List[str]: ...

    def is_source_file(self, file_path: str) -> bool: ...

    def parse_files(
        self,
        repo_path: str,
        project_name: str,
        source_files: List[str],
        project_root_symbol_id: Optional[str] = None,
    ) -> ParseResult: ...

    def export_to_neo4j(
        self,
        connector: Any,
        project_name: str,
        project_root_symbol_id: str,
        repo_path: str,
        ast_data_list: list,
        symbol_table: Any,
        clear_database: bool,
    ) -> dict: ...


@runtime_checkable
class ContextualLanguageAdapter(LanguageAdapter, Protocol):
    """
    可选的增强协议：用 ProjectImportContext 承载项目/仓库/隔离键等信息。

    设计目标：
    - 不破坏现有 LanguageAdapter（旧实现无需立刻改造）
    - importer 优先使用该协议以减少散落传参和漏传风险（例如 project_key）
    """

    def parse_files_with_context(self, ctx: ProjectImportContext, source_files: List[str]) -> ParseResult: ...

    def export_to_neo4j_with_context(
        self,
        ctx: ProjectImportContext,
        connector: Any,
        ast_data_list: list,
        symbol_table: Any,
        clear_database: bool,
    ) -> dict: ...

    def export_to_store_with_context(
        self,
        ctx: ProjectImportContext,
        store: GraphStore,
        ast_data_list: list,
        symbol_table: Any,
        clear_database: bool,
    ) -> dict: ...


import os
from dataclasses import dataclass
from typing import List, Optional

from loraxmod import Parser

from parser.common.symbol_table import SymbolTable
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from storage.neo4j.external_linker import ExternalClassLinker
from parser.languages.java.java_graph_batch_builder import build_java_graph_batch

from core.import_context import ProjectImportContext
from core.language_adapters.protocols import ContextualLanguageAdapter, LanguageAdapter, ParseResult
from core.import_result import ImportResult
from core.graph_store.protocols import GraphStore
from core.graph_store.neo4j_store import Neo4jGraphStore
from core.project_ids import generate_project_symbol_id


@dataclass
class JavaParseResult:
    project_root_symbol_id: str
    ast_data_list: list
    symbol_table: SymbolTable


class JavaLanguageAdapter(LanguageAdapter):
    """
    Java 语言适配器：负责
    - 发现 Java 源码目录/文件
    - 解析 Java AST（loraxmod + JavaFileAnalyzer）
    - 导出到 Neo4j（通过通用 GraphStore / GraphBatch 写入；兼容层类名仍保留）
    """

    source_extensions = [".java"]

    def __init__(self):
        # Parser 创建成本较高，复用同一个实例
        self._parser = Parser("java")

    def find_source_dirs(self, repo_path: str, source_dir: Optional[str] = None) -> List[str]:
        """
        查找 Java 源码目录（返回 repo_path 的相对路径列表）
        - 若传入 java_source_dir，则直接使用
        - 否则自动查找 Maven 常见结构以及简单 java 目录
        """
        if source_dir:
            return [source_dir]

        found_dirs = set()
        for root, dirs, _files in os.walk(repo_path):
            # Maven 结构: */src/(main|test)/java
            if "src" in dirs:
                src_path = os.path.join(root, "src")
                for src_subdir in os.listdir(src_path):
                    src_subdir_path = os.path.join(src_path, src_subdir)
                    if os.path.isdir(src_subdir_path) and "java" in os.listdir(src_subdir_path):
                        java_path = os.path.join(src_subdir_path, "java")
                        if self._dir_has_java_files(java_path):
                            found_dirs.add(os.path.relpath(java_path, repo_path))
            # 简单结构: */java
            elif "java" in dirs:
                java_path = os.path.join(root, "java")
                if self._dir_has_java_files(java_path):
                    found_dirs.add(os.path.relpath(java_path, repo_path))

        return sorted(found_dirs)

    def find_source_files(self, repo_path: str, source_dirs: List[str]) -> List[str]:
        """
        在给定源码目录列表下找到所有 Java 文件（返回绝对路径）
        """
        all_files: list[str] = []
        for source_dir in source_dirs:
            abs_dir = os.path.join(repo_path, source_dir.replace("/", os.sep))
            all_files.extend(self._find_java_files(abs_dir))
        return sorted(all_files)

    def parse_files(
        self,
        repo_path: str,
        project_name: str,
        source_files: List[str],
        project_root_symbol_id: Optional[str] = None,
    ) -> ParseResult:
        symbol_table = SymbolTable()
        if not project_root_symbol_id:
            project_root_symbol_id = generate_project_symbol_id(project_name, project_type="Application")

        context = AnalyzerContext(
            project_name=project_name,
            project_path=repo_path,
            root_project_symbol_id=project_root_symbol_id,
            parser=self._parser,
        )

        ast_data_list = []
        for file_path in source_files:
            ast_data = self._parse_java_file(file_path, context, symbol_table)
            if ast_data is not None:
                ast_data_list.append(ast_data)

        return JavaParseResult(
            project_root_symbol_id=project_root_symbol_id,
            ast_data_list=ast_data_list,
            symbol_table=symbol_table,
        )

    def export_to_neo4j(
        self,
        connector,
        project_name: str,
        project_root_symbol_id: str,
        repo_path: str,
        ast_data_list: list,
        symbol_table: SymbolTable,
        clear_database: bool,
    ) -> dict:
        # 统一走 GraphBatch -> GraphStore.write_batch 的通用写入路径，避免 exporter.py 的语言耦合。
        ctx = ProjectImportContext(
            project_name=project_name,
            project_key=project_root_symbol_id,
            repo_cache_dir=repo_path,
            language="java",
            languages=["java"],
            clear_database=clear_database,
        )
        store = Neo4jGraphStore(connector)
        return self.export_to_store_with_context(
            ctx=ctx,
            store=store,
            ast_data_list=ast_data_list,
            symbol_table=symbol_table,
            clear_database=clear_database,
        )

    # ------------------------------
    # ContextualLanguageAdapter (可选增强协议)
    # ------------------------------

    def parse_files_with_context(self, ctx: ProjectImportContext, source_files: List[str]) -> ParseResult:
        return self.parse_files(
            repo_path=ctx.repo_cache_dir,
            project_name=ctx.project_name,
            source_files=source_files,
            project_root_symbol_id=ctx.project_key,
        )

    def export_to_neo4j_with_context(
        self,
        ctx: ProjectImportContext,
        connector,
        ast_data_list: list,
        symbol_table: SymbolTable,
        clear_database: bool,
    ) -> dict:
        return self.export_to_neo4j(
            connector=connector,
            project_name=ctx.project_name,
            project_root_symbol_id=ctx.project_key,
            repo_path=ctx.repo_cache_dir,
            ast_data_list=ast_data_list,
            symbol_table=symbol_table,
            clear_database=clear_database,
        )

    def export_to_store_with_context(
        self,
        ctx: ProjectImportContext,
        store: GraphStore,
        ast_data_list: list,
        symbol_table: SymbolTable,
        clear_database: bool,
    ) -> dict:
        if clear_database:
            store.delete_project_subgraph(ctx)

        batch = build_java_graph_batch(
            project_name=ctx.project_name,
            project_key=ctx.project_key,
            project_path=ctx.repo_cache_dir,
            ast_data_list=ast_data_list,
            include_comment_nodes=ctx.include_comment_nodes,
        )
        result = store.write_batch(ctx, batch)

        # 外部类链接统一在落库后执行（Java 专用增强，不作为 GraphStore 协议要求）
        connector = getattr(store, "connector", None)
        if connector is not None:
            try:
                linker = ExternalClassLinker(connector)
                _ = linker.link_by_project(ctx.project_name, project_key=ctx.project_key, dry_run=False)
            except Exception:
                pass

        # store.write_batch 已返回兼容 dict，这里补一个 ImportResult 封装，便于上层强类型使用
        ir = ImportResult(
            success=bool(result.get("success", True)),
            status="imported" if result.get("success") else "failed",
            message=str(result.get("message", "")),
            project_name=ctx.project_name,
            project_key=ctx.project_key,
            attempted_nodes=int(result.get("attempted_nodes", 0) or 0),
            attempted_relationships=int(result.get("attempted_relationships", 0) or 0),
            created_nodes=int(result.get("created_nodes", 0) or 0),
            created_relationships=int(result.get("created_relationships", 0) or 0),
        )
        # 对现有调用方保持 dict 语义不变
        return ir.to_dict()

    def is_source_file(self, file_path: str) -> bool:
        return bool(file_path) and any(file_path.endswith(ext) for ext in self.source_extensions)

    @staticmethod
    def _dir_has_java_files(java_path: str) -> bool:
        for _r, _d, files in os.walk(java_path):
            if any(f.endswith(".java") for f in files):
                return True
        return False

    @staticmethod
    def _find_java_files(directory_path: str) -> List[str]:
        java_files = []
        for root, _dirs, files in os.walk(directory_path):
            for file in files:
                if file.endswith(".java"):
                    java_files.append(os.path.join(root, file))
        return sorted(java_files)

    def _parse_java_file(self, java_file_path: str, context: AnalyzerContext, symbol_table: SymbolTable):
        try:
            # 设置当前文件路径到 context（用于生成 symbol_id）
            try:
                relative_path = os.path.relpath(java_file_path, context.project_path)
                context.file_path = relative_path.replace(os.sep, "/")
            except ValueError:
                context.file_path = os.path.basename(java_file_path)

            analyzer = JavaFileAnalyzer(
                context=context,
                symbol_table=symbol_table,
                auto_resolve_types=True,
                file_path=java_file_path,
            )
            return analyzer.analyze_file()
        except Exception:
            return None


from __future__ import annotations

from pathlib import Path

from parser.languages.java.core.ast_node_types import JavaFileStructure
from storage.neo4j.graph_schema import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
)
from storage.neo4j.node_types import FileNode, ProjectNode


class JavaProjectAndFileCollectionMixin:
    """
    Java 项目节点/文件节点收集（从旧 Java exporter 的 mixin 迁移出来）。

    约束：
    - 宿主类需提供：
      - self.project_name / self.project_id / self.project_path
      - self.nodes_to_create / self.created_nodes / self.relationships_to_create
      - self.symbol_manager
      - self._collect_* 子方法（class/interface/enum/annotation/record 等）
    """

    def _prepare_project_node(self) -> ProjectNode:
        project_node: ProjectNode = {
            "symbol_id": self.project_id,
            "name": self.project_name,
            "belong_project": self.project_name,
            "project_key": self.project_id,
        }
        self.nodes_to_create[JavaNeo4jNodeType.Project].append(project_node)
        self.created_nodes.add(self.project_id)
        return project_node

    def _collect_ast_file_nodes(self, ast_data: JavaFileStructure | None, parent_node: ProjectNode) -> None:
        if ast_data is None:
            return

        try:
            file_path = Path(ast_data.file_path)
            project_path = Path(self.project_path)
            relative_path_str = str(file_path.relative_to(project_path)).replace("\\", "/")
        except ValueError:
            relative_path_str = ast_data.file_name

        java_file_node: FileNode = {
            "symbol_id": ast_data.symbol_id,
            "name": ast_data.file_name,
            "file_path": relative_path_str,
            "full_path": ast_data.file_path,
            "package_name": ast_data.package_info.name,
            "file_type": "Java",
            "imports": [imp.import_path for imp in ast_data.import_details],
            "start_line": ast_data.location.start_line,
            "end_line": ast_data.location.end_line,
            "start_column": ast_data.location.start_column,
            "end_column": ast_data.location.end_column,
            "belong_project": self.project_name,
            "project_key": parent_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.File].append(java_file_node)
        self.created_nodes.add(java_file_node["symbol_id"])

        self.relationships_to_create.append(
            (self.project_id, java_file_node["symbol_id"], JavaGraphEdgeType.HAVE.value)
        )

        for class_data in ast_data.classes:
            self._collect_class_nodes(class_data, java_file_node)

        for interface_data in ast_data.interfaces:
            self._collect_interface_nodes(interface_data, java_file_node)

        for enum_data in ast_data.enums:
            self._collect_enum_nodes(enum_data, java_file_node)

        for annotation_data in ast_data.annotations:
            self._collect_annotation_nodes(annotation_data, java_file_node)

        for record_data in ast_data.records:
            self._collect_record_nodes(record_data, java_file_node)

        self.symbol_manager.collect_from_java_file(
            project_name=self.project_name,
            java_file_structure=ast_data,
        )

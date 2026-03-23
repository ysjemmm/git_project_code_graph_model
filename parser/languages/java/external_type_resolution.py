from __future__ import annotations

import os
import re
from typing import Any

from parser.languages.java.symbol.symbol_commons import ClassLocation, ClassLocationType
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from storage.neo4j.java_modules import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
    JavaObjectNodeGraphNode,
    ObjectFromType,
    ObjectType,
    ProjectGraphNode,
)
from tools.ast_tool import AstTool


# 版本号正则表达式模式
VERSION_PATTERN = r"[-_](\d+(?:\.\d+)*(?:[-._]\w+)?)$"

# 已知 BOM / 通用 parent POM：这些不作为 Lib 名，避免把依赖归属到 BOM；多模块二方包的 parent 不在此列表则用 parent，便于显示为「epaas-gateway」等
_PARENT_AS_BOM_BLOCKLIST = frozenset({
    "spring-boot-starter-parent",
    "spring-boot-dependencies",
    "mandarin-pure-bom",
    "platform-bom",
})


def _is_parent_likely_bom(parent_artifact_id: str) -> bool:
    if not (parent_artifact_id or "").strip():
        return True
    pid = (parent_artifact_id or "").strip().lower()
    if pid in _PARENT_AS_BOM_BLOCKLIST:
        return True
    if pid.endswith("-bom") or pid.endswith("-parent"):
        return True
    return False


class JavaExternalTypeResolutionMixin:
    """
    Java 外部类/JDK/UNKNOWN 解析与依赖 Project(Lib) 节点创建（从旧 Java exporter 的 mixin 迁移出来）。

    约束：
    - 宿主类需提供：
      - self.project_name / self.project_id
      - self.nodes_to_create / self.relationships_to_create / self.created_nodes
      - self._node_exists_in_list(...)
    """

    def _create_external_java_object(
        self, location: ClassLocation, object_type: ObjectType
    ) -> JavaObjectNodeGraphNode:
        java_object = JavaObjectNodeGraphNode()
        java_object.symbol_id = (
            AstTool.get_str(location.jar_path, "UNKNOWN")
            + "<path>"
            + AstTool.get_str(location.fqn, "UNKNOWN")
        )
        java_object.qualified_name = AstTool.get_str(location.fqn, "UNKNOWN")
        java_object.name = java_object.qualified_name.rsplit(".", 1)[-1]
        java_object.object_type = object_type.value
        java_object.belong_file = location.file_path

        # Lib 归属：多模块二方包（如 epaas-gateway 的 core、plugin-api）希望显示为同一 Lib「epaas-gateway」；
        # 优先 parent_artifact_id（非 BOM）；若无则从同 jar 的其它类借 parent（同一 jar 的 pom 相同）；再 artifact_id / jar 名。
        belong = None
        if location.parent_artifact_id and not _is_parent_likely_bom(location.parent_artifact_id):
            belong = location.parent_artifact_id
        if belong is None and location.jar_path:
            try:
                sm = getattr(self, "symbol_manager", None)
                jdb = getattr(sm, "jar_db", None) if sm else None
                if jdb:
                    same_jar = jdb.query_by_jar(location.jar_path, include_anonymous=False)
                    for c in (same_jar or [])[:30]:
                        pid = getattr(c, "parent_artifact_id", None)
                        if pid and not _is_parent_likely_bom(pid):
                            belong = pid
                            break
            except Exception:
                pass
        if belong is None and location.artifact_id:
            belong = location.artifact_id
        if belong is None and location.parent_artifact_id:
            belong = location.parent_artifact_id
        if belong is None and location.jar_path:
            jar_filename = os.path.basename(location.jar_path)
            if jar_filename.endswith(".jar"):
                jar_filename = jar_filename[:-4]
            project_name = re.sub(VERSION_PATTERN, "", jar_filename)
            belong = project_name if project_name else jar_filename
        java_object.belong_project = belong if belong is not None else "UNKNOWN"

        if location.parent_version:
            java_object.version = location.parent_version
        elif location.artifact_version:
            java_object.version = location.artifact_version
        elif location.jar_path:
            jar_filename = os.path.basename(location.jar_path)
            if jar_filename.endswith(".jar"):
                jar_filename = jar_filename[:-4]
            match = re.search(VERSION_PATTERN, jar_filename)
            java_object.version = match.group(1) if match else ""
        else:
            java_object.version = ""

        java_object.from_type = ObjectFromType.EXTERNAL_DEFINITION.value
        return java_object

    # 仅对 import 列表中的类型建外部节点，避免 extends/泛型/同包等把整库都算进 Lib
    _IMPORT_ONLY_EXTERNAL_METHODS = frozenset({"explicit_import_external", "wildcard_import_external"})

    def _parse_class_location_to_node(
        self,
        location: ClassLocation,
        class_symbol_id: str,
        extend_type: str,
        object_type: ObjectType,
    ):
        if location is None:
            return

        if location.type == ClassLocationType.EXTERNAL:
            if getattr(location, "resolution_method", "") not in self._IMPORT_ONLY_EXTERNAL_METHODS:
                return

        java_object = JavaObjectNodeGraphNode()

        if location.type == ClassLocationType.EXTERNAL:
            java_object = self._create_external_java_object(location, object_type)
        elif location.type == ClassLocationType.JDK:
            jar_path = AstTool.get_str(location.jar_path, "")
            fqn = AstTool.get_str(location.fqn, "UNKNOWN")
            java_object.symbol_id = f"{jar_path}<path>{fqn}" if jar_path else f"JDK<path>{fqn}"
            java_object.qualified_name = fqn
            java_object.name = fqn.rsplit(".", 1)[-1]
            java_object.object_type = object_type.value
            java_object.belong_project = "__JDK__"
            java_object.version = "1.8"
            java_object.from_type = ObjectFromType.JDK_DEFINITION.value
            self.created_nodes.add(java_object.symbol_id)
        elif location.type == ClassLocationType.UNKNOWN:
            jar_path = AstTool.get_str(location.jar_path, "UNKNOWN")
            fqn = AstTool.get_str(location.fqn, "UNKNOWN")
            java_object.symbol_id = f"{jar_path}<path>{fqn}"
            java_object.belong_project = "__UNKNOWN__"
            java_object.qualified_name = fqn
            java_object.name = fqn.rsplit(".", 1)[-1] if fqn != "UNKNOWN" else "UNKNOWN"
            java_object.object_type = object_type.value
            java_object.from_type = ObjectFromType.UNKNOWN_DEFINITION.value

        if location.type == ClassLocationType.INTERNAL:
            self.relationships_to_create.append((class_symbol_id, location.symbol_id, extend_type))
        else:
            self.created_nodes.add(java_object.symbol_id)
            self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object)
            self.relationships_to_create.append((class_symbol_id, java_object.symbol_id, extend_type))

        # 仅对「能解析到归属」的外部依赖创建 Project(Lib) 与 CONTAINS_LIB，避免 __UNKNOWN__ 撑大 Lib 列表
        if (
            getattr(self, 'include_lib_nodes', True)
            and java_object.belong_project is not None
            and java_object.belong_project != self.project_name
            and java_object.belong_project != "__UNKNOWN__"
        ):
            dep_project_symbol_id = AnalyzerHelper.generate_symbol_id_for_project(
                java_object.belong_project,
                project_type="Lib",
                version=java_object.version,
            )

            project_data = {"symbol_id": dep_project_symbol_id}
            if not self._node_exists_in_list(JavaNeo4jNodeType.Project, project_data):
                self.created_nodes.add(dep_project_symbol_id)
                pn = ProjectGraphNode(
                    name=java_object.belong_project,
                    qualified_name=dep_project_symbol_id,
                    symbol_id=dep_project_symbol_id,
                    belong_project=java_object.belong_project,
                    project_type="Lib",
                )
                pn.version = java_object.version
                self.nodes_to_create[JavaNeo4jNodeType.Project].append(pn)

            self.relationships_to_create.append(
                (dep_project_symbol_id, java_object.symbol_id, JavaGraphEdgeType.CONTAINS_LIB.value)
            )


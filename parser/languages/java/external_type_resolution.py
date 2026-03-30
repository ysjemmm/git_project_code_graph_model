from __future__ import annotations

import os
import re
from typing import Any

from parser.common.symbol_table import SymbolIdGenerator
from parser.languages.java.symbol.symbol_commons import ClassLocation, ClassLocationType
from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
from storage.neo4j.graph_schema import JavaGraphEdgeType, JavaNeo4jNodeType
from parser.languages.java.java_constants import ObjectFromType, ObjectType
from storage.neo4j.node_types import JavaObjectNode, ProjectNode
from tools.ast_tool import AstTool


VERSION_PATTERN = r"[-_](\d+(?:\.\d+)*(?:[-._]\w+)?)$"

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
    Java 外部类/JDK/UNKNOWN 解析与依赖 Project(Lib) 节点创建。
    """

    def _create_external_java_object(
        self, location: ClassLocation, object_type: ObjectType
    ) -> JavaObjectNode:
        symbol_id = SymbolIdGenerator.for_external_class(
            AstTool.get_str(location.jar_path, "UNKNOWN"),
            AstTool.get_str(location.fqn, "UNKNOWN"),
        )
        qualified_name = AstTool.get_str(location.fqn, "UNKNOWN")
        name = qualified_name.rsplit(".", 1)[-1]

        # Lib 归属解析
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

        # 版本解析
        if location.parent_version:
            version = location.parent_version
        elif location.artifact_version:
            version = location.artifact_version
        elif location.jar_path:
            jar_filename = os.path.basename(location.jar_path)
            if jar_filename.endswith(".jar"):
                jar_filename = jar_filename[:-4]
            match = re.search(VERSION_PATTERN, jar_filename)
            version = match.group(1) if match else ""
        else:
            version = ""

        java_object: JavaObjectNode = {
            "symbol_id": symbol_id,
            "name": name,
            "qualified_name": qualified_name,
            "object_type": object_type.value,
            "from_type": ObjectFromType.EXTERNAL_DEFINITION.value,
            "belong_file": location.file_path,
            "belong_project": belong if belong is not None else "UNKNOWN",
        }
        # version 不在 JavaObjectNode TypedDict 里（已从 ORM 移除），暂存为额外字段供 Lib 节点使用
        java_object["_version"] = version  # type: ignore[typeddict-unknown-key]
        return java_object

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

        java_object: JavaObjectNode

        if location.type == ClassLocationType.EXTERNAL:
            java_object = self._create_external_java_object(location, object_type)
        elif location.type == ClassLocationType.JDK:
            jar_path = AstTool.get_str(location.jar_path, "")
            fqn = AstTool.get_str(location.fqn, "UNKNOWN")
            java_object = {
                "symbol_id": SymbolIdGenerator.for_jdk_class(jar_path, fqn),
                "name": fqn.rsplit(".", 1)[-1],
                "qualified_name": fqn,
                "object_type": object_type.value,
                "from_type": ObjectFromType.JDK_DEFINITION.value,
                "belong_project": "__JDK__",
            }
            java_object["_version"] = "1.8"  # type: ignore[typeddict-unknown-key]
            self.created_nodes.add(java_object["symbol_id"])
        elif location.type == ClassLocationType.UNKNOWN:
            jar_path = AstTool.get_str(location.jar_path, "UNKNOWN")
            fqn = AstTool.get_str(location.fqn, "UNKNOWN")
            java_object = {
                "symbol_id": SymbolIdGenerator.for_external_class(jar_path, fqn),
                "name": fqn.rsplit(".", 1)[-1] if fqn != "UNKNOWN" else "UNKNOWN",
                "qualified_name": fqn,
                "object_type": object_type.value,
                "from_type": ObjectFromType.UNKNOWN_DEFINITION.value,
                "belong_project": "__UNKNOWN__",
            }
        else:
            java_object = {"symbol_id": ""}  # type: ignore[typeddict-item]

        if location.type == ClassLocationType.INTERNAL:
            self.relationships_to_create.append((class_symbol_id, location.symbol_id, extend_type))
        else:
            self.created_nodes.add(java_object["symbol_id"])
            self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object)
            self.relationships_to_create.append((class_symbol_id, java_object["symbol_id"], extend_type))

        belong_project = java_object.get("belong_project")
        version = java_object.pop("_version", "")  # type: ignore[misc]

        if (
            getattr(self, "include_lib_nodes", True)
            and belong_project is not None
            and belong_project != self.project_name
            and belong_project != "__UNKNOWN__"
        ):
            dep_project_symbol_id = AnalyzerHelper.generate_symbol_id_for_project(
                belong_project,
                project_type="Lib",
                version=version,
            )

            project_data = {"symbol_id": dep_project_symbol_id}
            if not self._node_exists_in_list(JavaNeo4jNodeType.Project, project_data):
                self.created_nodes.add(dep_project_symbol_id)
                pn: ProjectNode = {
                    "symbol_id": dep_project_symbol_id,
                    "name": belong_project,
                    "project_key": dep_project_symbol_id,
                    "project_type": "Lib",
                    "belong_project": belong_project,
                    "version": version,
                }
                self.nodes_to_create[JavaNeo4jNodeType.Project].append(pn)

            self.relationships_to_create.append(
                (dep_project_symbol_id, java_object["symbol_id"], JavaGraphEdgeType.CONTAINS_LIB.value)
            )

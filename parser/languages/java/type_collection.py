from __future__ import annotations

from parser.languages.java.java_constants import ObjectFromType, ObjectType
from storage.neo4j.graph_schema import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
)
from storage.neo4j.node_types import JavaObjectNode


def _make_java_object_node(
    symbol_id: str,
    name: str,
    qualified_name: str,
    parent_symbol_id: str,
    object_type: str,
    from_type: str,
    data,
    parent_node: dict,
    belong_file: str = "",
) -> JavaObjectNode:
    return {
        "symbol_id": symbol_id,
        "name": name,
        "qualified_name": qualified_name,
        "parent_symbol_id": parent_symbol_id,
        "object_type": object_type,
        "from_type": from_type,
        "raw_metadata": "empty now",
        "type_parameters": getattr(data, "type_parameters", []) or [],
        "super_class": getattr(data, "super_class", None),
        "super_interfaces": getattr(data, "super_interfaces", None) or getattr(data, "extends_interfaces", None) or [],
        "annotations": [ann.name for ann in (getattr(data, "annotations", None) or [])],
        "request_uri": getattr(data, "mapping_uri", None),
        "belong_file": belong_file,
        "start_line": data.location.start_line,
        "end_line": data.location.end_line,
        "start_column": data.location.start_column,
        "end_column": data.location.end_column,
        "belong_project": parent_node.get("belong_project"),
        "project_key": parent_node.get("project_key"),
    }


class JavaTopLevelTypeCollectionMixin:
    """
    Java 顶层类型（class/interface/enum/annotation/record）节点收集。
    """

    def _collect_class_nodes(self, class_data, java_file_node):
        if class_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=class_data.symbol_id,
            name=class_data.class_name,
            qualified_name=java_file_node.get("package_name", "") + "." + class_data.class_name,
            parent_symbol_id=class_data.parent_symbol_id,
            object_type=ObjectType.CLASS_TYPE.value,
            from_type=ObjectFromType.INNER_DEFINITION.value,
            data=class_data,
            parent_node=java_file_node,
            belong_file=java_file_node.get("file_path", ""),
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (java_file_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(class_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for cb in class_data.code_blocks:
            self._collect_code_block_nodes(cb, java_object_node)
        for m in class_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for f in class_data.fields:
            self._collect_field_nodes(f, java_object_node)
        for c in class_data.constructors:
            self._collect_constructor_nodes(c, java_object_node)
        for nc in class_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node)
        for ni in class_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node)
        for ne in class_data.nested_enums:
            self._collect_nested_enum_nodes(ne, java_object_node)
        for na in class_data.nested_annotations:
            self._collect_nested_annotation_nodes(na, java_object_node)
        for nr in class_data.nested_records:
            self._collect_nested_record_nodes(nr, java_object_node)

    def _collect_interface_nodes(self, interface_data, java_file_node):
        if interface_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=interface_data.symbol_id,
            name=interface_data.interface_name,
            qualified_name=java_file_node.get("package_name", "") + "." + interface_data.interface_name,
            parent_symbol_id=interface_data.parent_symbol_id,
            object_type=ObjectType.INTERFACE_TYPE.value,
            from_type=ObjectFromType.INNER_DEFINITION.value,
            data=interface_data,
            parent_node=java_file_node,
            belong_file=java_file_node.get("file_path", ""),
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (java_file_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(interface_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for m in interface_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for nc in interface_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node)
        for ni in interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node)

    def _collect_enum_nodes(self, enum_data, java_file_node):
        if enum_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=enum_data.symbol_id,
            name=enum_data.enum_name,
            qualified_name=java_file_node.get("package_name", "") + "." + enum_data.enum_name,
            parent_symbol_id=enum_data.parent_symbol_id,
            object_type=ObjectType.ENUM_TYPE.value,
            from_type=ObjectFromType.INNER_DEFINITION.value,
            data=enum_data,
            parent_node=java_file_node,
            belong_file=java_file_node.get("file_path", ""),
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (java_file_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(enum_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for cb in enum_data.code_blocks:
            self._collect_code_block_nodes(cb, java_object_node)
        for m in enum_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for f in enum_data.fields:
            self._collect_field_nodes(f, java_object_node)
        for c in enum_data.enum_constants:
            self._collect_enum_constant_nodes(c, java_object_node)
        for nc in enum_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node)
        for ni in enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node)

    def _collect_annotation_nodes(self, annotation_data, java_file_node):
        if annotation_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=annotation_data.symbol_id,
            name=annotation_data.annotation_name,
            qualified_name=java_file_node.get("package_name", "") + "." + annotation_data.annotation_name,
            parent_symbol_id=annotation_data.parent_symbol_id,
            object_type=ObjectType.ANNOTATION_TYPE.value,
            from_type=ObjectFromType.INNER_DEFINITION.value,
            data=annotation_data,
            parent_node=java_file_node,
            belong_file=java_file_node.get("file_path", ""),
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (java_file_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(annotation_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for f in annotation_data.elements:
            self._collect_field_nodes(f, java_object_node)

    def _collect_record_nodes(self, record_data, java_file_node):
        if record_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=record_data.symbol_id,
            name=record_data.record_name,
            qualified_name=java_file_node.get("package_name", "") + "." + record_data.record_name,
            parent_symbol_id=record_data.parent_symbol_id,
            object_type=ObjectType.RECORD_TYPE.value,
            from_type=ObjectFromType.INNER_DEFINITION.value,
            data=record_data,
            parent_node=java_file_node,
            belong_file=java_file_node.get("file_path", ""),
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (java_file_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(record_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for cb in record_data.code_blocks:
            self._collect_code_block_nodes(cb, java_object_node)
        for m in record_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for c in record_data.constructors:
            self._collect_constructor_nodes(c, java_object_node)
        for p in record_data.components:
            self._collect_record_component_nodes(p, java_object_node)

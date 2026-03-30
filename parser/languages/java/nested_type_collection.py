from __future__ import annotations

from parser.languages.java.java_constants import ObjectFromType, ObjectType
from storage.neo4j.graph_schema import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
)
from storage.neo4j.node_types import JavaObjectNode
from parser.languages.java.type_collection import _make_java_object_node


class JavaNestedTypeCollectionMixin:
    """
    Java 嵌套类型节点收集（nested class/interface/enum/annotation/record）。
    """

    def _collect_nested_class_nodes(self, nested_class_data, parent_object_node, depth: int = 0):
        if depth > 50 or nested_class_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=nested_class_data.symbol_id,
            name=nested_class_data.class_name,
            qualified_name=parent_object_node.get("qualified_name", "") + "." + nested_class_data.class_name,
            parent_symbol_id=nested_class_data.parent_symbol_id,
            object_type=ObjectType.CLASS_TYPE.value,
            from_type=ObjectFromType.NESTED_DEFINITION.value,
            data=nested_class_data,
            parent_node=parent_object_node,
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (parent_object_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(nested_class_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for m in nested_class_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for f in nested_class_data.fields:
            self._collect_field_nodes(f, java_object_node)
        for c in nested_class_data.constructors:
            self._collect_constructor_nodes(c, java_object_node)
        for nc in nested_class_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node, depth + 1)
        for ni in nested_class_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node, depth + 1)
        for ne in nested_class_data.nested_enums:
            self._collect_nested_enum_nodes(ne, java_object_node, depth + 1)
        for na in nested_class_data.nested_annotations:
            self._collect_nested_annotation_nodes(na, java_object_node, depth + 1)
        for nr in nested_class_data.nested_records:
            self._collect_nested_record_nodes(nr, java_object_node, depth + 1)

    def _collect_nested_interface_nodes(self, nested_interface_data, parent_object_node, depth: int = 0):
        if depth > 50 or nested_interface_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=nested_interface_data.symbol_id,
            name=nested_interface_data.interface_name,
            qualified_name=parent_object_node.get("qualified_name", "") + "." + nested_interface_data.interface_name,
            parent_symbol_id=nested_interface_data.parent_symbol_id,
            object_type=ObjectType.INTERFACE_TYPE.value,
            from_type=ObjectFromType.NESTED_DEFINITION.value,
            data=nested_interface_data,
            parent_node=parent_object_node,
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (parent_object_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(nested_interface_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for m in nested_interface_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for f in getattr(nested_interface_data, "fields", []):
            self._collect_field_nodes(f, java_object_node)
        for nc in nested_interface_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node, depth + 1)
        for ni in nested_interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node, depth + 1)

    def _collect_nested_enum_nodes(self, nested_enum_data, parent_object_node, depth: int = 0):
        if depth > 50 or nested_enum_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=nested_enum_data.symbol_id,
            name=nested_enum_data.enum_name,
            qualified_name=parent_object_node.get("qualified_name", "") + "." + nested_enum_data.enum_name,
            parent_symbol_id=nested_enum_data.parent_symbol_id,
            object_type=ObjectType.ENUM_TYPE.value,
            from_type=ObjectFromType.NESTED_DEFINITION.value,
            data=nested_enum_data,
            parent_node=parent_object_node,
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (parent_object_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(nested_enum_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for m in nested_enum_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for f in nested_enum_data.fields:
            self._collect_field_nodes(f, java_object_node)
        for c in nested_enum_data.constructors:
            self._collect_constructor_nodes(c, java_object_node)
        for ec in nested_enum_data.enum_constants:
            self._collect_enum_constant_nodes(ec, java_object_node)
        for nc in nested_enum_data.nested_classes:
            self._collect_nested_class_nodes(nc, java_object_node, depth + 1)
        for ni in nested_enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(ni, java_object_node, depth + 1)

    def _collect_nested_annotation_nodes(self, nested_annotation_data, parent_object_node, depth: int = 0):
        if depth > 50 or nested_annotation_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=nested_annotation_data.symbol_id,
            name=nested_annotation_data.annotation_name,
            qualified_name=parent_object_node.get("qualified_name", "") + "." + nested_annotation_data.annotation_name,
            parent_symbol_id=nested_annotation_data.parent_symbol_id,
            object_type=ObjectType.ANNOTATION_TYPE.value,
            from_type=ObjectFromType.NESTED_DEFINITION.value,
            data=nested_annotation_data,
            parent_node=parent_object_node,
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (parent_object_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(nested_annotation_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for f in nested_annotation_data.elements:
            self._collect_field_nodes(f, java_object_node)

    def _collect_nested_record_nodes(self, nested_record_data, parent_object_node, depth: int = 0):
        if depth > 50 or nested_record_data is None:
            return

        java_object_node: JavaObjectNode = _make_java_object_node(
            symbol_id=nested_record_data.symbol_id,
            name=nested_record_data.record_name,
            qualified_name=parent_object_node.get("qualified_name", "") + "." + nested_record_data.record_name,
            parent_symbol_id=nested_record_data.parent_symbol_id,
            object_type=ObjectType.RECORD_TYPE.value,
            from_type=ObjectFromType.NESTED_DEFINITION.value,
            data=nested_record_data,
            parent_node=parent_object_node,
        )

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node["symbol_id"])
        self.relationships_to_create.append(
            (parent_object_node["symbol_id"], java_object_node["symbol_id"], JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(nested_record_data.comments, java_object_node["symbol_id"], java_object_node, java_object_node.get("belong_project"))

        for m in nested_record_data.methods:
            self._collect_method_nodes(m, java_object_node)
        for c in nested_record_data.constructors:
            self._collect_constructor_nodes(c, java_object_node)
        for p in nested_record_data.components:
            self._collect_record_component_nodes(p, java_object_node)

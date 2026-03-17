from __future__ import annotations

from storage.neo4j.java_modules import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
    JavaObjectNodeGraphNode,
    ObjectFromType,
    ObjectType,
)


class JavaNestedTypeCollectionMixin:
    """
    Java 嵌套类型节点收集（nested class/interface/enum/annotation/record）。
    """

    def _collect_nested_class_nodes(self, nested_class_data, parent_object_node, depth: int = 0):
        if depth > 50:
            return
        if nested_class_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_class_data.class_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_class_data.class_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_class_data.symbol_id
        java_object_node.parent_symbol_id = nested_class_data.parent_symbol_id
        java_object_node.type_parameters = nested_class_data.type_parameters
        java_object_node.start_line = nested_class_data.location.start_line
        java_object_node.end_line = nested_class_data.location.end_line
        java_object_node.start_column = nested_class_data.location.start_column
        java_object_node.end_column = nested_class_data.location.end_column
        java_object_node.object_type = ObjectType.CLASS_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.request_uri = nested_class_data.mapping_uri
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_class_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            nested_class_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for method_data in nested_class_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        for field_data in nested_class_data.fields:
            self._collect_field_nodes(field_data, java_object_node)
        for constructor_data in nested_class_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)

        for nested_class in nested_class_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)
        for nested_interface in nested_class_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)
        for nested_enum in nested_class_data.nested_enums:
            self._collect_nested_enum_nodes(nested_enum, java_object_node, depth + 1)
        for nested_annotation in nested_class_data.nested_annotations:
            self._collect_nested_annotation_nodes(nested_annotation, java_object_node, depth + 1)
        for nested_record in nested_class_data.nested_records:
            self._collect_nested_record_nodes(nested_record, java_object_node, depth + 1)

    def _collect_nested_interface_nodes(self, nested_interface_data, parent_object_node, depth: int = 0):
        if depth > 50:
            return
        if nested_interface_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_interface_data.interface_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_interface_data.interface_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_interface_data.symbol_id
        java_object_node.parent_symbol_id = nested_interface_data.parent_symbol_id
        java_object_node.start_line = nested_interface_data.location.start_line
        java_object_node.end_line = nested_interface_data.location.end_line
        java_object_node.start_column = nested_interface_data.location.start_column
        java_object_node.end_column = nested_interface_data.location.end_column
        java_object_node.object_type = ObjectType.INTERFACE_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_interface_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            nested_interface_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for method_data in nested_interface_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        for field_data in nested_interface_data.fields:
            self._collect_field_nodes(field_data, java_object_node)

        for nested_class in nested_interface_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)
        for nested_interface in nested_interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)

    def _collect_nested_enum_nodes(self, nested_enum_data, parent_object_node, depth: int = 0):
        if depth > 50:
            return
        if nested_enum_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_enum_data.enum_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_enum_data.enum_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_enum_data.symbol_id
        java_object_node.parent_symbol_id = nested_enum_data.parent_symbol_id
        java_object_node.start_line = nested_enum_data.location.start_line
        java_object_node.end_line = nested_enum_data.location.end_line
        java_object_node.start_column = nested_enum_data.location.start_column
        java_object_node.end_column = nested_enum_data.location.end_column
        java_object_node.object_type = ObjectType.ENUM_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_enum_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            nested_enum_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for method_data in nested_enum_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        for field_data in nested_enum_data.fields:
            self._collect_field_nodes(field_data, java_object_node)
        for constructor_data in nested_enum_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)
        for constant_data in nested_enum_data.enum_constants:
            self._collect_enum_constant_nodes(constant_data, java_object_node)

        for nested_class in nested_enum_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node, depth + 1)
        for nested_interface in nested_enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node, depth + 1)

    def _collect_nested_annotation_nodes(self, nested_annotation_data, parent_object_node, depth: int = 0):
        if depth > 50:
            return
        if nested_annotation_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_annotation_data.annotation_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_annotation_data.annotation_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_annotation_data.symbol_id
        java_object_node.parent_symbol_id = nested_annotation_data.parent_symbol_id
        java_object_node.start_line = nested_annotation_data.location.start_line
        java_object_node.end_line = nested_annotation_data.location.end_line
        java_object_node.start_column = nested_annotation_data.location.start_column
        java_object_node.end_column = nested_annotation_data.location.end_column
        java_object_node.object_type = ObjectType.ANNOTATION_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_annotation_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            nested_annotation_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for element_data in nested_annotation_data.elements:
            self._collect_field_nodes(element_data, java_object_node)

    def _collect_nested_record_nodes(self, nested_record_data, parent_object_node, depth: int = 0):
        if depth > 50:
            return
        if nested_record_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = nested_record_data.record_name
        java_object_node.qualified_name = parent_object_node.qualified_name + "." + nested_record_data.record_name
        java_object_node.belong_project = parent_object_node.belong_project
        java_object_node.symbol_id = nested_record_data.symbol_id
        java_object_node.parent_symbol_id = nested_record_data.parent_symbol_id
        java_object_node.type_parameters = nested_record_data.type_parameters
        java_object_node.start_line = nested_record_data.location.start_line
        java_object_node.end_line = nested_record_data.location.end_line
        java_object_node.start_column = nested_record_data.location.start_column
        java_object_node.end_column = nested_record_data.location.end_column
        java_object_node.object_type = ObjectType.RECORD_TYPE.value
        java_object_node.from_type = ObjectFromType.NESTED_DEFINITION.value
        java_object_node.raw_metadata = "empty now"
        java_object_node.annotations = [ann.name for ann in nested_record_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (parent_object_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            nested_record_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for method_data in nested_record_data.methods:
            self._collect_method_nodes(method_data, java_object_node)
        for constructor_data in nested_record_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)
        for component_data in nested_record_data.components:
            self._collect_record_component_nodes(component_data, java_object_node)


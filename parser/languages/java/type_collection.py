from __future__ import annotations

from storage.neo4j.java_modules import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
    JavaObjectNodeGraphNode,
    ObjectFromType,
    ObjectType,
)


class JavaTopLevelTypeCollectionMixin:
    """
    Java 顶层类型（class/interface/enum/annotation/record）节点收集（从旧 Java exporter 的 mixin 迁移出来）。

    约束：
    - 宿主类需提供：
      - self.nodes_to_create / self.created_nodes / self.relationships_to_create
      - self._collect_comment_nodes / _collect_* 成员方法（method/field/constructor/param/enum_constant/code_block/nested_* 等）
    """

    def _collect_class_nodes(self, class_data, java_file_node):
        if class_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = class_data.class_name
        java_object_node.qualified_name = java_file_node.package_name + "." + class_data.class_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.project_key = java_file_node.project_key
        java_object_node.belong_file = java_file_node.file_path
        java_object_node.symbol_id = class_data.symbol_id
        java_object_node.parent_symbol_id = class_data.parent_symbol_id
        java_object_node.type_parameters = class_data.type_parameters
        java_object_node.start_line = class_data.location.start_line
        java_object_node.end_line = class_data.location.end_line
        java_object_node.start_column = class_data.location.start_column
        java_object_node.end_column = class_data.location.end_column
        java_object_node.object_type = ObjectType.CLASS_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.request_uri = class_data.mapping_uri
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_class = class_data.super_class
        java_object_node.super_interfaces = class_data.super_interfaces

        java_object_node.annotations = [ann.name for ann in class_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            class_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for code_blocks in class_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in class_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for field_data in class_data.fields:
            self._collect_field_nodes(field_data, java_object_node)

        for constructor_data in class_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)

        for nested_class in class_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)

        for nested_interface in class_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)

        for nested_enum in class_data.nested_enums:
            self._collect_nested_enum_nodes(nested_enum, java_object_node)

        for nested_annotation in class_data.nested_annotations:
            self._collect_nested_annotation_nodes(nested_annotation, java_object_node)

        for nested_record in class_data.nested_records:
            self._collect_nested_record_nodes(nested_record, java_object_node)

    def _collect_interface_nodes(self, interface_data, java_file_node) -> None:
        if interface_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = interface_data.interface_name
        java_object_node.qualified_name = java_file_node.package_name + "." + interface_data.interface_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.belong_file = java_file_node.file_path
        java_object_node.project_key = java_file_node.project_key
        java_object_node.symbol_id = interface_data.symbol_id
        java_object_node.parent_symbol_id = interface_data.parent_symbol_id
        java_object_node.start_line = interface_data.location.start_line
        java_object_node.end_line = interface_data.location.end_line
        java_object_node.start_column = interface_data.location.start_column
        java_object_node.end_column = interface_data.location.end_column
        java_object_node.object_type = ObjectType.INTERFACE_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = interface_data.extends_interfaces
        java_object_node.annotations = [ann.name for ann in interface_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            interface_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for method_data in interface_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for nested_class in interface_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)

        for nested_interface in interface_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)

    def _collect_enum_nodes(self, enum_data, java_file_node):
        if enum_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = enum_data.enum_name
        java_object_node.qualified_name = java_file_node.package_name + "." + enum_data.enum_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.project_key = java_file_node.project_key
        java_object_node.belong_file = java_file_node.file_path
        java_object_node.symbol_id = enum_data.symbol_id
        java_object_node.parent_symbol_id = enum_data.parent_symbol_id
        java_object_node.start_line = enum_data.location.start_line
        java_object_node.end_line = enum_data.location.end_line
        java_object_node.start_column = enum_data.location.start_column
        java_object_node.end_column = enum_data.location.end_column
        java_object_node.object_type = ObjectType.ENUM_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = enum_data.super_interfaces
        java_object_node.annotations = [ann.name for ann in enum_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            enum_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for code_blocks in enum_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in enum_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for field_data in enum_data.fields:
            self._collect_field_nodes(field_data, java_object_node)

        for constant_data in enum_data.enum_constants:
            self._collect_enum_constant_nodes(constant_data, java_object_node)

        for nested_class in enum_data.nested_classes:
            self._collect_nested_class_nodes(nested_class, java_object_node)

        for nested_interface in enum_data.nested_interfaces:
            self._collect_nested_interface_nodes(nested_interface, java_object_node)

    def _collect_annotation_nodes(self, annotation_data, java_file_node):
        if annotation_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = annotation_data.annotation_name
        java_object_node.qualified_name = java_file_node.package_name + "." + annotation_data.annotation_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.belong_file = java_file_node.file_path
        java_object_node.project_key = java_file_node.project_key
        java_object_node.symbol_id = annotation_data.symbol_id
        java_object_node.parent_symbol_id = annotation_data.parent_symbol_id
        java_object_node.start_line = annotation_data.location.start_line
        java_object_node.end_line = annotation_data.location.end_line
        java_object_node.start_column = annotation_data.location.start_column
        java_object_node.end_column = annotation_data.location.end_column
        java_object_node.object_type = ObjectType.ANNOTATION_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.annotations = [ann.name for ann in annotation_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            annotation_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for field_data in annotation_data.elements:
            self._collect_field_nodes(field_data, java_object_node)

    def _collect_record_nodes(self, record_data, java_file_node):
        if record_data is None:
            return

        java_object_node = JavaObjectNodeGraphNode()
        java_object_node.name = record_data.record_name
        java_object_node.qualified_name = java_file_node.package_name + "." + record_data.record_name
        java_object_node.belong_project = java_file_node.belong_project
        java_object_node.project_key = java_file_node.project_key
        java_object_node.belong_file = java_file_node.file_path
        java_object_node.project_key = java_file_node.project_key
        java_object_node.symbol_id = record_data.symbol_id
        java_object_node.parent_symbol_id = record_data.parent_symbol_id
        java_object_node.type_parameters = record_data.type_parameters
        java_object_node.start_line = record_data.location.start_line
        java_object_node.end_line = record_data.location.end_line
        java_object_node.start_column = record_data.location.start_column
        java_object_node.end_column = record_data.location.end_column
        java_object_node.object_type = ObjectType.RECORD_TYPE.value
        java_object_node.from_type = ObjectFromType.INNER_DEFINITION.value
        java_object_node.raw_metadata = "empty now"

        java_object_node.super_interfaces = record_data.super_interfaces
        java_object_node.annotations = [ann.name for ann in record_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaObject].append(java_object_node)
        self.created_nodes.add(java_object_node.symbol_id)
        self.relationships_to_create.append(
            (java_file_node.symbol_id, java_object_node.symbol_id, JavaGraphEdgeType.CONTAINS.value)
        )

        self._collect_comment_nodes(
            record_data.comments,
            java_object_node.symbol_id,
            java_object_node,
            java_object_node.belong_project,
        )

        for code_blocks in record_data.code_blocks:
            self._collect_code_block_nodes(code_blocks, java_object_node)

        for method_data in record_data.methods:
            self._collect_method_nodes(method_data, java_object_node)

        for constructor_data in record_data.constructors:
            self._collect_constructor_nodes(constructor_data, java_object_node)

        for param_data in record_data.components:
            self._collect_record_component_nodes(param_data, java_object_node)


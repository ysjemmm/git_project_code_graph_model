from __future__ import annotations

from storage.neo4j.java_modules import (
    JavaCodeBlockNodeGraphNode,
    JavaEnumConstantNodeGraphNode,
    JavaFieldNodeGraphNode,
    JavaGraphEdgeType,
    JavaMethodNodeGraphNode,
    JavaNeo4jNodeType,
    JavaParameterNodeGraphNode,
)


class JavaMemberCollectionMixin:
    """
    Java 成员节点收集（method/constructor/field/parameter/enum_constant/record_component/code_block）。
    """

    def _collect_method_nodes(self, method_data, java_object_node):
        if method_data is None:
            return

        java_method_node = JavaMethodNodeGraphNode()
        java_method_node.is_constructor = False
        java_method_node.name = method_data.method_name
        java_method_node.belong_project = java_object_node.belong_project
        java_method_node.project_key = java_object_node.project_key

        if method_data.is_static:
            java_method_node.is_static = True
            java_method_node.symbol_id = method_data.symbol_id
        else:
            java_method_node.symbol_id = method_data.symbol_id

        java_method_node.parent_symbol_id = method_data.parent_symbol_id
        java_method_node.start_line = method_data.location.start_line
        java_method_node.end_line = method_data.location.end_line
        java_method_node.start_column = method_data.location.start_column
        java_method_node.end_column = method_data.location.end_column
        java_method_node.raw_metadata = method_data.raw_method
        java_method_node.mapping_method_type = ""
        java_method_node.mapping_uri = method_data.base_mapping_uri
        java_method_node.throws_exceptions = method_data.exceptions
        java_method_node.type_parameters = method_data.type_parameters
        java_method_node.return_type = method_data.return_type
        java_method_node.mapping_method_type = (
            ",".join(method_data.mapping_method_types)
            if method_data.mapping_method_types
            else ""
        )
        java_method_node.base_uri = method_data.base_mapping_uri
        java_method_node.full_uri = method_data.full_mapping_uri
        java_object_node.annotations = [ann.name for ann in method_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_method_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

        self._collect_comment_nodes(
            method_data.comments,
            java_method_node.symbol_id,
            java_method_node,
            java_method_node.belong_project,
        )

        for param_data in method_data.parameters:
            self._collect_parameter_nodes(param_data, java_method_node)

    def _collect_code_block_nodes(self, code_block_data, java_object_node):
        if code_block_data is None:
            return

        symbol_id = getattr(code_block_data, "symbol_id", None)
        parent_symbol_id = getattr(code_block_data, "parent_symbol_id", None)
        loc = getattr(code_block_data, "location", None)
        start_line = getattr(loc, "start_line", None) if loc else None
        start_column = getattr(loc, "start_column", None) if loc else None
        end_line = getattr(loc, "end_line", None) if loc else None
        end_column = getattr(loc, "end_column", None) if loc else None

        if not symbol_id:
            # 保证 symbol_id 稳定可重复（用于 MERGE/约束/增量）
            symbol_id = (
                f"{parent_symbol_id or java_object_node.symbol_id}"
                f"<code_block>{start_line}:{start_column}:{end_line}:{end_column}"
            )

        java_cb_node = JavaCodeBlockNodeGraphNode()
        java_cb_node.name = "__CodeBlock__"
        java_cb_node.is_static = code_block_data.is_static
        java_cb_node.belong_project = java_object_node.belong_project
        java_cb_node.project_key = java_object_node.project_key

        java_cb_node.symbol_id = symbol_id
        java_cb_node.parent_symbol_id = parent_symbol_id
        java_cb_node.start_line = start_line
        java_cb_node.end_line = end_line
        java_cb_node.start_column = start_column
        java_cb_node.end_column = end_column
        java_cb_node.raw_metadata = code_block_data.raw_method

        self.nodes_to_create[JavaNeo4jNodeType.JavaCodeBlock].append(java_cb_node)
        self.created_nodes.add(java_cb_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_cb_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_constructor_nodes(self, constructor_data, java_object_node):
        if constructor_data is None:
            return

        java_method_node = JavaMethodNodeGraphNode()
        java_method_node.name = constructor_data.constructor_name
        java_method_node.is_constructor = True
        java_method_node.belong_project = java_object_node.belong_project
        java_method_node.project_key = java_object_node.project_key

        java_method_node.symbol_id = constructor_data.symbol_id
        java_method_node.parent_symbol_id = constructor_data.parent_symbol_id
        java_method_node.start_line = constructor_data.location.start_line
        java_method_node.end_line = constructor_data.location.end_line
        java_method_node.start_column = constructor_data.location.start_column
        java_method_node.end_column = constructor_data.location.end_column
        java_method_node.raw_metadata = constructor_data.raw_method
        java_method_node.throws_exceptions = constructor_data.exceptions
        java_method_node.annotations = [ann.name for ann in constructor_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_method_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

        for param_data in constructor_data.parameters:
            self._collect_parameter_nodes(param_data, java_method_node)

    def _collect_field_nodes(self, field_data, java_object_node):
        if field_data is None:
            return

        java_field_node = JavaFieldNodeGraphNode()
        java_field_node.name = field_data.field_name
        java_field_node.is_static = field_data.is_static
        java_field_node.is_final = field_data.is_final
        java_field_node.has_default_value = field_data.has_initial_value
        java_field_node.default_value = field_data.initial_value
        java_field_node.belong_project = java_object_node.belong_project
        java_field_node.project_key = java_object_node.project_key

        java_field_node.parent_symbol_id = field_data.parent_symbol_id
        java_field_node.symbol_id = field_data.symbol_id
        java_field_node.start_line = field_data.location.start_line
        java_field_node.end_line = field_data.location.end_line
        java_field_node.start_column = field_data.location.start_column
        java_field_node.end_column = field_data.location.end_column
        java_field_node.raw_metadata = field_data.raw_field
        java_field_node.annotations = [ann.name for ann in field_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaField].append(java_field_node)
        self.created_nodes.add(java_field_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_field_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

        self._collect_comment_nodes(
            field_data.comments,
            java_field_node.symbol_id,
            java_field_node,
            java_field_node.belong_project,
        )

    def _collect_parameter_nodes(self, param_data, java_method_node):
        if param_data is None:
            return

        java_param_node = JavaParameterNodeGraphNode()
        java_param_node.is_constructor = False
        java_param_node.name = param_data.parameter_name
        java_param_node.type_name = param_data.parameter_type
        java_param_node.belong_project = java_method_node.belong_project
        java_param_node.symbol_id = param_data.symbol_id
        java_param_node.parent_symbol_id = java_method_node.symbol_id
        java_param_node.start_line = param_data.location.start_line
        java_param_node.end_line = param_data.location.end_line
        java_param_node.start_column = param_data.location.start_column
        java_param_node.end_column = param_data.location.end_column
        java_param_node.raw_metadata = param_data.raw_parameter
        java_param_node.annotations = [ann.name for ann in param_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethodParameter].append(java_param_node)
        self.created_nodes.add(java_param_node.symbol_id)
        self.relationships_to_create.append(
            (java_method_node.symbol_id, java_param_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_enum_constant_nodes(self, constant_data, java_object_node):
        if constant_data is None:
            return

        java_enum_constant_node = JavaEnumConstantNodeGraphNode()
        java_enum_constant_node.name = constant_data.constant_name
        java_enum_constant_node.belong_project = java_object_node.belong_project
        java_enum_constant_node.project_key = java_object_node.project_key
        java_enum_constant_node.parent_symbol_id = constant_data.parent_symbol_id
        java_enum_constant_node.symbol_id = constant_data.symbol_id

        java_enum_constant_node.start_line = constant_data.location.start_line
        java_enum_constant_node.end_line = constant_data.location.end_line
        java_enum_constant_node.start_column = constant_data.location.start_column
        java_enum_constant_node.end_column = constant_data.location.end_column
        java_enum_constant_node.raw_metadata = constant_data.raw_constant
        java_enum_constant_node.arguments = constant_data.arguments
        java_enum_constant_node.annotations = [ann.name for ann in constant_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaEnumConstant].append(java_enum_constant_node)
        self.created_nodes.add(java_enum_constant_node.symbol_id)
        self.relationships_to_create.append(
            (java_object_node.symbol_id, java_enum_constant_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_record_component_nodes(self, param_data, record_node):
        if param_data is None:
            return

        java_param_node = JavaParameterNodeGraphNode()
        java_param_node.is_constructor = False
        java_param_node.name = param_data.parameter_name
        java_param_node.type_name = param_data.parameter_type
        java_param_node.belong_project = record_node.belong_project
        java_param_node.project_key = record_node.project_key
        java_param_node.symbol_id = param_data.symbol_id
        java_param_node.parent_symbol_id = param_data.symbol_id
        java_param_node.start_line = param_data.location.start_line
        java_param_node.end_line = param_data.location.end_line
        java_param_node.start_column = param_data.location.start_column
        java_param_node.end_column = param_data.location.end_column
        java_param_node.raw_metadata = param_data.raw_parameter
        java_param_node.annotations = [ann.name for ann in param_data.annotations]

        self.nodes_to_create[JavaNeo4jNodeType.JavaRecordComponent].append(java_param_node)
        self.created_nodes.add(java_param_node.symbol_id)
        self.relationships_to_create.append(
            (record_node.symbol_id, java_param_node.symbol_id, JavaGraphEdgeType.MEMBER_OF.value)
        )


from __future__ import annotations

from parser.common.symbol_table import SymbolIdGenerator
from storage.neo4j.graph_schema import (
    JavaGraphEdgeType,
    JavaNeo4jNodeType,
)
from storage.neo4j.node_types import (
    JavaCodeBlockNode,
    JavaEnumConstantNode,
    JavaFieldNode,
    JavaMethodNode,
    JavaMethodParameterNode,
    JavaRecordComponentNode,
)


class JavaMemberCollectionMixin:
    """
    Java 成员节点收集（method/constructor/field/parameter/enum_constant/record_component/code_block）。
    """

    def _collect_method_nodes(self, method_data, java_object_node):
        if method_data is None:
            return

        java_method_node: JavaMethodNode = {
            "symbol_id": method_data.symbol_id,
            "name": method_data.method_name,
            "parent_symbol_id": method_data.parent_symbol_id,
            "is_constructor": False,
            "is_static": method_data.is_static,
            "return_type": method_data.return_type,
            "raw_metadata": method_data.raw_method,
            "base_uri": method_data.base_mapping_uri,
            "full_uri": method_data.full_mapping_uri,
            "mapping_method_type": ",".join(method_data.mapping_method_types) if method_data.mapping_method_types else "",
            "throws_exceptions": method_data.exceptions,
            "type_parameters": method_data.type_parameters,
            "annotations": [ann.name for ann in method_data.annotations],
            "start_line": method_data.location.start_line,
            "end_line": method_data.location.end_line,
            "start_column": method_data.location.start_column,
            "end_column": method_data.location.end_column,
            "belong_project": java_object_node.get("belong_project"),
            "project_key": java_object_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node["symbol_id"])
        self.relationships_to_create.append(
            (java_object_node["symbol_id"], java_method_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

        self._collect_comment_nodes(
            method_data.comments,
            java_method_node["symbol_id"],
            java_method_node,
            java_method_node.get("belong_project"),
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
            symbol_id = SymbolIdGenerator.for_code_block(
                parent_symbol_id or java_object_node["symbol_id"],
                start_line, start_column, end_line, end_column,
            )

        java_cb_node: JavaCodeBlockNode = {
            "symbol_id": symbol_id,
            "parent_symbol_id": parent_symbol_id,
            "is_static": code_block_data.is_static,
            "raw_metadata": code_block_data.raw_method,
            "start_line": start_line,
            "end_line": end_line,
            "start_column": start_column,
            "end_column": end_column,
            "belong_project": java_object_node.get("belong_project"),
            "project_key": java_object_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaCodeBlock].append(java_cb_node)
        self.created_nodes.add(java_cb_node["symbol_id"])
        self.relationships_to_create.append(
            (java_object_node["symbol_id"], java_cb_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_constructor_nodes(self, constructor_data, java_object_node):
        if constructor_data is None:
            return

        java_method_node: JavaMethodNode = {
            "symbol_id": constructor_data.symbol_id,
            "name": constructor_data.constructor_name,
            "parent_symbol_id": constructor_data.parent_symbol_id,
            "is_constructor": True,
            "is_static": False,
            "raw_metadata": constructor_data.raw_method,
            "throws_exceptions": constructor_data.exceptions,
            "annotations": [ann.name for ann in constructor_data.annotations],
            "start_line": constructor_data.location.start_line,
            "end_line": constructor_data.location.end_line,
            "start_column": constructor_data.location.start_column,
            "end_column": constructor_data.location.end_column,
            "belong_project": java_object_node.get("belong_project"),
            "project_key": java_object_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethod].append(java_method_node)
        self.created_nodes.add(java_method_node["symbol_id"])
        self.relationships_to_create.append(
            (java_object_node["symbol_id"], java_method_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

        for param_data in constructor_data.parameters:
            self._collect_parameter_nodes(param_data, java_method_node)

    def _collect_field_nodes(self, field_data, java_object_node):
        if field_data is None:
            return

        java_field_node: JavaFieldNode = {
            "symbol_id": field_data.symbol_id,
            "name": field_data.field_name,
            "parent_symbol_id": field_data.parent_symbol_id,
            "type_name": field_data.field_type,
            "is_static": field_data.is_static,
            "is_final": field_data.is_final,
            "has_default_value": field_data.has_initial_value,
            "default_value": field_data.initial_value,
            "raw_metadata": field_data.raw_field,
            "annotations": [ann.name for ann in field_data.annotations],
            "start_line": field_data.location.start_line,
            "end_line": field_data.location.end_line,
            "start_column": field_data.location.start_column,
            "end_column": field_data.location.end_column,
            "belong_project": java_object_node.get("belong_project"),
            "project_key": java_object_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaField].append(java_field_node)
        self.created_nodes.add(java_field_node["symbol_id"])
        self.relationships_to_create.append(
            (java_object_node["symbol_id"], java_field_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

        self._collect_comment_nodes(
            field_data.comments,
            java_field_node["symbol_id"],
            java_field_node,
            java_field_node.get("belong_project"),
        )

    def _collect_parameter_nodes(self, param_data, java_method_node):
        if param_data is None:
            return

        java_param_node: JavaMethodParameterNode = {
            "symbol_id": param_data.symbol_id,
            "name": param_data.parameter_name,
            "parent_symbol_id": java_method_node["symbol_id"],
            "type_name": param_data.parameter_type,
            "annotations": [ann.name for ann in param_data.annotations],
            "start_line": param_data.location.start_line,
            "end_line": param_data.location.end_line,
            "start_column": param_data.location.start_column,
            "end_column": param_data.location.end_column,
            "belong_project": java_method_node.get("belong_project"),
            "project_key": java_method_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaMethodParameter].append(java_param_node)
        self.created_nodes.add(java_param_node["symbol_id"])
        self.relationships_to_create.append(
            (java_method_node["symbol_id"], java_param_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_enum_constant_nodes(self, constant_data, java_object_node):
        if constant_data is None:
            return

        java_enum_constant_node: JavaEnumConstantNode = {
            "symbol_id": constant_data.symbol_id,
            "name": constant_data.constant_name,
            "parent_symbol_id": constant_data.parent_symbol_id,
            "arguments": constant_data.arguments,
            "annotations": [ann.name for ann in constant_data.annotations],
            "start_line": constant_data.location.start_line,
            "end_line": constant_data.location.end_line,
            "start_column": constant_data.location.start_column,
            "end_column": constant_data.location.end_column,
            "belong_project": java_object_node.get("belong_project"),
            "project_key": java_object_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaEnumConstant].append(java_enum_constant_node)
        self.created_nodes.add(java_enum_constant_node["symbol_id"])
        self.relationships_to_create.append(
            (java_object_node["symbol_id"], java_enum_constant_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

    def _collect_record_component_nodes(self, param_data, record_node):
        if param_data is None:
            return

        java_record_component_node: JavaRecordComponentNode = {
            "symbol_id": param_data.symbol_id,
            "name": param_data.parameter_name,
            "parent_symbol_id": param_data.symbol_id,
            "type_name": param_data.parameter_type,
            "annotations": [ann.name for ann in param_data.annotations],
            "start_line": param_data.location.start_line,
            "end_line": param_data.location.end_line,
            "start_column": param_data.location.start_column,
            "end_column": param_data.location.end_column,
            "belong_project": record_node.get("belong_project"),
            "project_key": record_node.get("project_key"),
        }

        self.nodes_to_create[JavaNeo4jNodeType.JavaRecordComponent].append(java_record_component_node)
        self.created_nodes.add(java_record_component_node["symbol_id"])
        self.relationships_to_create.append(
            (record_node["symbol_id"], java_record_component_node["symbol_id"], JavaGraphEdgeType.MEMBER_OF.value)
        )

from __future__ import annotations

from storage.neo4j.java_modules import JavaGraphEdgeType, ObjectType


class JavaTypeRelationshipCollectionMixin:
    """
    Java 类型之间关系收集（extends/implements）。

    约束：
    - 宿主类需提供：
      - self.symbol_manager
      - self.project_name
      - self._parse_class_location_to_node(...)
    """

    def _parse_extend_impl_relationships(self, ast_data_list):
        for ast_data in ast_data_list:
            for c in ast_data.classes:
                if c.super_class is not None and c.super_class.strip() != "":
                    location = self.symbol_manager.parse_java_object_where(
                        c.super_class.split("<")[0],
                        ast_data,
                        project_name=self.project_name,
                    )
                    self._parse_class_location_to_node(
                        location,
                        c.symbol_id,
                        JavaGraphEdgeType.EXTENDS.value,
                        ObjectType.CLASS_TYPE,
                    )

                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(
                        interface.split("<")[0],
                        ast_data,
                        project_name=self.project_name,
                    )
                    self._parse_class_location_to_node(
                        location,
                        c.symbol_id,
                        JavaGraphEdgeType.IMPLEMENTS.value,
                        ObjectType.INTERFACE_TYPE,
                    )

            for c in ast_data.interfaces:
                for interface in c.extends_interfaces:
                    location = self.symbol_manager.parse_java_object_where(
                        interface.split("<")[0],
                        ast_data,
                        project_name=self.project_name,
                    )
                    self._parse_class_location_to_node(
                        location,
                        c.symbol_id,
                        JavaGraphEdgeType.EXTENDS.value,
                        ObjectType.INTERFACE_TYPE,
                    )

            for c in ast_data.enums:
                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(
                        interface.split("<")[0],
                        ast_data,
                        project_name=self.project_name,
                    )
                    self._parse_class_location_to_node(
                        location,
                        c.symbol_id,
                        JavaGraphEdgeType.IMPLEMENTS.value,
                        ObjectType.INTERFACE_TYPE,
                    )

            for c in ast_data.records:
                for interface in c.super_interfaces:
                    location = self.symbol_manager.parse_java_object_where(
                        interface.split("<")[0],
                        ast_data,
                        project_name=self.project_name,
                    )
                    self._parse_class_location_to_node(
                        location,
                        c.symbol_id,
                        JavaGraphEdgeType.IMPLEMENTS.value,
                        ObjectType.INTERFACE_TYPE,
                    )


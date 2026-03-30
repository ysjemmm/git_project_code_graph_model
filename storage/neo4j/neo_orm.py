from typing import Union

from neomodel import (
    StructuredNode,
    StringProperty,
    RelationshipTo,
    RelationshipFrom,
    IntegerProperty,
    BooleanProperty,
    JSONProperty,
)


# ==================== 节点类定义 ====================
# 字段名与写入侧（node_types.py TypedDict）保持一致，
# 以图数据库中实际存储的属性名为准。

class Project(StructuredNode):
    """项目节点（Application 或 Lib）"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty(required=True)          # 写入侧用 name，对应 project_name 语义
    project_key = StringProperty()
    project_type = StringProperty()               # Application | Lib
    belong_project = StringProperty()
    repo_url = StringProperty()
    repo_branch = StringProperty()
    repo_commit = StringProperty()
    version = StringProperty()                    # 导入版本号

    # 关系定义
    has_file = RelationshipTo('File', 'HAVE')
    contains_lib = RelationshipTo('JavaObject', 'CONTAINS_LIB')
    depends_on = RelationshipTo('Project', 'DEPENDS_ON')
    same_artifact = RelationshipTo('Project', 'SAME_ARTIFACT')


class File(StructuredNode):
    """文件节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 文件名
    file_path = StringProperty(required=True)
    full_path = StringProperty()
    package_name = StringProperty()
    file_type = StringProperty()                  # Java
    imports = JSONProperty(default=list)
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    contained_by = RelationshipFrom('Project', 'HAVE')
    contains_object = RelationshipTo('JavaObject', 'CONTAINS')


class JavaObject(StructuredNode):
    """Java 对象节点（类、接口、枚举、注解、记录）"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 简单类名
    qualified_name = StringProperty()
    parent_symbol_id = StringProperty()
    object_type = StringProperty()                # classType | interfaceType | enumType | annotationType | recordType
    from_type = StringProperty()                  # InnerDefinition | NestedDefinition | ExternalDefinition | JdkDefinition
    raw_metadata = StringProperty()
    super_class = StringProperty()
    super_interfaces = JSONProperty(default=list)
    type_parameters = JSONProperty(default=list)
    annotations = JSONProperty(default=list)
    simple_comment = StringProperty()
    has_detailed_comment = BooleanProperty(default=False)
    request_uri = StringProperty()                # REST API 路径
    belong_file = StringProperty()
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    contained_by = RelationshipFrom('File', 'CONTAINS')
    contained_by_object = RelationshipFrom('JavaObject', 'CONTAINS')
    extends = RelationshipTo('JavaObject', 'EXTENDS')
    implements = RelationshipTo('JavaObject', 'IMPLEMENTS')
    has_method = RelationshipTo('JavaMethod', 'MEMBER_OF')
    has_field = RelationshipTo('JavaField', 'MEMBER_OF')
    has_enum_constant = RelationshipTo('JavaEnumConstant', 'MEMBER_OF')
    has_record_component = RelationshipTo('JavaRecordComponent', 'MEMBER_OF')
    has_code_block = RelationshipTo('JavaCodeBlock', 'MEMBER_OF')
    has_comment = RelationshipTo('Comment', 'HAS_COMMENT')
    lib_link = RelationshipTo('JavaObject', 'LIB_LINK')


class JavaMethod(StructuredNode):
    """Java 方法节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 方法名
    parent_symbol_id = StringProperty()
    raw_signature = StringProperty()
    return_type = StringProperty()
    is_static = BooleanProperty(default=False)
    is_constructor = BooleanProperty(default=False)
    base_uri = StringProperty()
    full_uri = StringProperty()
    mapping_method_type = StringProperty()
    type_parameters = JSONProperty(default=list)
    annotations = JSONProperty(default=list)
    throws_exceptions = JSONProperty(default=list)
    simple_comment = StringProperty()
    has_detailed_comment = BooleanProperty(default=False)
    raw_metadata = StringProperty()
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaObject', 'MEMBER_OF')
    has_parameter = RelationshipTo('JavaMethodParameter', 'MEMBER_OF')
    has_code_block = RelationshipTo('JavaCodeBlock', 'MEMBER_OF')
    calls = RelationshipTo('JavaMethod', 'CALLS')
    accesses = RelationshipTo('JavaField', 'ACCESSES')
    has_comment = RelationshipTo('Comment', 'HAS_COMMENT')


class JavaField(StructuredNode):
    """Java 字段节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 字段名
    parent_symbol_id = StringProperty()
    type_name = StringProperty()
    is_static = BooleanProperty(default=False)
    is_final = BooleanProperty(default=False)
    has_default_value = BooleanProperty(default=False)
    default_value = StringProperty()
    annotations = JSONProperty(default=list)
    simple_comment = StringProperty()
    has_detailed_comment = BooleanProperty(default=False)
    raw_metadata = StringProperty()
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaObject', 'MEMBER_OF')
    accessed_by = RelationshipFrom('JavaMethod', 'ACCESSES')
    has_comment = RelationshipTo('Comment', 'HAS_COMMENT')


class JavaMethodParameter(StructuredNode):
    """Java 方法参数节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 参数名
    parent_symbol_id = StringProperty()
    type_name = StringProperty()
    annotations = JSONProperty(default=list)
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaMethod', 'MEMBER_OF')


class JavaCodeBlock(StructuredNode):
    """Java 代码块节点（静态块、实例块等）"""
    symbol_id = StringProperty(unique=True, required=True)
    parent_symbol_id = StringProperty()
    is_static = BooleanProperty(default=False)
    raw_metadata = StringProperty()
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaObject', 'MEMBER_OF')


class JavaEnumConstant(StructuredNode):
    """Java 枚举常量节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 常量名
    parent_symbol_id = StringProperty()
    annotations = JSONProperty(default=list)
    arguments = JSONProperty(default=list)
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaObject', 'MEMBER_OF')


class JavaRecordComponent(StructuredNode):
    """Java 记录组件节点"""
    symbol_id = StringProperty(unique=True, required=True)
    name = StringProperty()                       # 组件名
    parent_symbol_id = StringProperty()
    type_name = StringProperty()
    annotations = JSONProperty(default=list)
    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    member_of = RelationshipFrom('JavaObject', 'MEMBER_OF')


class Comment(StructuredNode):
    """注释节点"""
    symbol_id = StringProperty(unique=True, required=True)
    parent_symbol_id = StringProperty()
    content = StringProperty()
    comment_type = StringProperty()  # javadoc | long_comment | block_comment | line_comment
    char_count = IntegerProperty()
    line_count = IntegerProperty()

    # Javadoc 特有字段
    javadoc_summary = StringProperty()
    javadoc_params = JSONProperty(default=list)
    javadoc_return = StringProperty()
    javadoc_throws = JSONProperty(default=list)
    javadoc_author = StringProperty()
    javadoc_version = StringProperty()
    javadoc_since = StringProperty()
    javadoc_deprecated = StringProperty()
    javadoc_see = JSONProperty(default=list)

    start_line = IntegerProperty()
    end_line = IntegerProperty()
    start_column = IntegerProperty()
    end_column = IntegerProperty()
    belong_project = StringProperty()
    project_key = StringProperty()

    # 关系定义
    commented_by = RelationshipFrom('JavaObject', 'HAS_COMMENT')


# ==================== 关系属性类定义 ====================

class CallRelationshipProperties:
    """方法调用关系属性"""
    call_count = IntegerProperty(default=1)
    last_call_line = IntegerProperty()


class AccessRelationshipProperties:
    """字段访问关系属性"""
    access_count = IntegerProperty(default=1)
    access_type = StringProperty()  # READ | WRITE | READ_WRITE
    last_access_line = IntegerProperty()


class DependsOnRelationshipProperties:
    """项目依赖关系属性"""
    group_id = StringProperty()
    artifact_id = StringProperty()
    version = StringProperty()
    scope = StringProperty()

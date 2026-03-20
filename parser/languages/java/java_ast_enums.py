"""Java AST Node Types - Enumerations for tree-sitter-java"""

from enum import Enum
from typing import Optional


class JavaAstNodeType(Enum):
    """Java AST node types from tree-sitter-java"""
    
    # Declarations
    CLASS_DECLARATION = "class_declaration"
    INTERFACE_DECLARATION = "interface_declaration"
    ENUM_DECLARATION = "enum_declaration"
    ANNOTATION_TYPE_DECLARATION = "annotation_type_declaration"
    RECORD_DECLARATION = "record_declaration"

    # Members
    METHOD_DECLARATION = "method_declaration"
    CONSTRUCTOR_DECLARATION = "constructor_declaration"
    FIELD_DECLARATION = "field_declaration"
    CONSTANT_DECLARATION = "constant_declaration"
    ENUM_CONSTANT = "enum_constant"
    ANNOTATION = "annotation"
    MARKER_ANNOTATION = "marker_annotation"
    SINGLE_ELEMENT_ANNOTATION = "single_element_annotation"
    NORMAL_ANNOTATION = "normal_annotation"
    
    # Statements
    BLOCK = "block"
    CLASS_BODY = "class_body"
    INTERFACE_BODY = "interface_body"
    ANNOTATION_TYPE_BODY = 'annotation_type_body'
    IF_STATEMENT = "if_statement"
    FOR_STATEMENT = "for_statement"
    ENHANCED_FOR_STATEMENT = "enhanced_for_statement"
    WHILE_STATEMENT = "while_statement"
    DO_STATEMENT = "do_statement"
    SWITCH_STATEMENT = "switch_statement"
    TRY_STATEMENT = "try_statement"
    TRY_WITH_RESOURCES_STATEMENT = "try_with_resources_statement"
    CATCH_CLAUSE = "catch_clause"
    FINALLY_CLAUSE = "finally_clause"
    RETURN_STATEMENT = "return_statement"
    BREAK_STATEMENT = "break_statement"
    CONTINUE_STATEMENT = "continue_statement"
    THROW_STATEMENT = "throw_statement"
    SYNCHRONIZED_STATEMENT = "synchronized_statement"
    ASSERT_STATEMENT = "assert_statement"
    EXPRESSION_STATEMENT = "expression_statement"
    LABELED_STATEMENT = "labeled_statement"
    EMPTY_STATEMENT = "empty_statement"
    
    # Expressions
    IDENTIFIER = "identifier"
    METHOD_INVOCATION = "method_invocation"
    FIELD_ACCESS = "field_access"
    ARRAY_ACCESS = "array_access"
    BINARY_EXPRESSION = "binary_expression"
    UNARY_EXPRESSION = "unary_expression"
    UPDATE_EXPRESSION = "update_expression"
    CAST_EXPRESSION = "cast_expression"
    INSTANCEOF_EXPRESSION = "instanceof_expression"
    LAMBDA_EXPRESSION = "lambda_expression"
    PARENTHESIZED_EXPRESSION = "parenthesized_expression"
    OBJECT_CREATION_EXPRESSION = "object_creation_expression"
    ARRAY_CREATION_EXPRESSION = "array_creation_expression"
    CLASS_LITERAL = "class_literal"
    THIS = "this"
    SUPER = "super"
    TERNARY_EXPRESSION = "ternary_expression"
    METHOD_REFERENCE = "method_reference"
    ASSIGNMENT_EXPRESSION = "assignment_expression"
    SWITCH_EXPRESSION = "switch_expression"
    TEMPLATE_EXPRESSION = "template_expression"
    
    # Literals
    STRING_LITERAL = "string_literal"
    DECIMAL_INTEGER_LITERAL = "decimal_integer_literal"
    HEX_INTEGER_LITERAL = "hex_integer_literal"
    OCTAL_INTEGER_LITERAL = "octal_integer_literal"
    BINARY_INTEGER_LITERAL = "binary_integer_literal"
    DECIMAL_FLOATING_POINT_LITERAL = "decimal_floating_point_literal"
    HEX_FLOATING_POINT_LITERAL = "hex_floating_point_literal"
    CHARACTER_LITERAL = "character_literal"
    TRUE = "true"
    FALSE = "false"
    NULL_LITERAL = "null_literal"
    
    # Types
    TYPE = "type"
    THROWS = "throws"
    TYPE_PARAMETERS = "type_parameters"
    SPREAD_PARAMETERS = "spread_parameter"
    PRIMITIVE_TYPE = "primitive_type"
    ARRAY_TYPE = "array_type"
    PARAMETERIZED_TYPE = "parameterized_type"
    UNION_TYPE = "union_type"
    INTERSECTION_TYPE = "intersection_type"
    WILDCARD = "wildcard"
    GENERIC_TYPE = "generic_type"
    TYPE_IDENTIFIER = "type_identifier"
    TYPE_LIST = "type_list"
    
    # Other
    PACKAGE_DECLARATION = "package_declaration"
    IMPORT_DECLARATION = "import_declaration"
    ANNOTATION_TYPE_ELEMENT_DECLARATION = "annotation_type_element_declaration"
    MODIFIERS = "modifiers"
    FORMAL_PARAMETERS = "formal_parameters"
    FORMAL_PARAMETER = "formal_parameter"
    LOCAL_VARIABLE_DECLARATION = "local_variable_declaration"
    VARIABLE_DECLARATOR = "variable_declarator"
    ENUM_BODY = "enum_body"
    ENUM_BODY_DECLARATIONS = "enum_body_declarations"
    STATIC_INITIALIZER = "static_initializer"
    INSTANCE_INITIALIZER = "instance_initializer"
    SUPER_INTERFACES = "super_interfaces"
    TYPE_PARAMETER = "type_parameter"
    ANNOTATION_ELEMENT_DECLARATION = "annotation_element_declaration"
    RECORD_COMPONENT = "record_component"
    RECORD_COMPONENT_LIST = "record_component_list"
    SWITCH_BLOCK = "switch_block"
    SWITCH_BLOCK_STATEMENT_GROUP = "switch_block_statement_group"
    SWITCH_LABEL = "switch_label"
    RESOURCE = "resource"
    RESOURCE_SPECIFICATION = "resource_specification"
    DIMENSION = "dimension"
    DIMENSIONS = "dimensions"
    INFERRED_PARAMETERS = "inferred_parameters"
    EXPLICIT_CONSTRUCTOR_INVOCATION = "explicit_constructor_invocation"
    ARRAY_INITIALIZER = "array_initializer"
    ANNOTATION_ARGUMENT_LIST = "annotation_argument_list"
    ELEMENT_VALUE_PAIR = "element_value_pair"
    ELEMENT_VALUE_ARRAY_INITIALIZER = "element_value_array_initializer"
    ELEMENT_VALUE = "element_value"
    ARGUMENT_LIST = "argument_list"
    EXTENDS_INTERFACES = "extends_interfaces"
    SUPER_CLASS = "superclass"
    IMPLEMENTS_INTERFACES = "implements_interfaces"
    PERMITS_CLAUSE = "permits_clause"
    SEALED_MODIFIER = "sealed_modifier"
    NON_SEALED_MODIFIER = "non_sealed_modifier"
    LINE_COMMENT = "line_comment"
    BLOCK_COMMENT = "block_comment"

    # Extractions
    EX_IDENTIFIER = 'identifier'
    EX_PARAMETERS = 'parameters'
    EX_ENUM_BODY = "body"
    EX_ENUM_CONSTANT_PARAM = "parameters"
    EX_ENUM_CONSTANT_BODY = "body"
    EX_INTERFACE_BODY = "body"
    EX_ANNOTATION_BODY = "body"
    EX_RECORD_BODY = "body"
    EX_FIELD_TYPE = "type"
    EX_FIELD_NAME = "identifier"
    EX_FIELD_VALUE = "value"
    EX_METHOD_BODY = "body"
    EX_METHOD_RETURN_TYPE = "type"
    EX_METHOD_PARAM_TYPE = "type"

    UNKNOWN = "unknown"
    
    def get_value(self):
        """Return the string value of the enum"""
        return self._value_

    @staticmethod
    def from_value(value: str) -> Optional['JavaAstNodeType']:
        """从字符串值获取枚举成员
        
        Args:
            value: 枚举的字符串值
        
        Returns:
            对应的枚举成员，如果不存在则返回 None
        """
        for member in JavaAstNodeType:
            if member.value == value:
                return member
        return JavaAstNodeType.UNKNOWN


# Node type groupings
DECLARATION_NODES = [
    JavaAstNodeType.CLASS_DECLARATION, JavaAstNodeType.METHOD_DECLARATION,
    JavaAstNodeType.CONSTRUCTOR_DECLARATION, JavaAstNodeType.FIELD_DECLARATION,
    JavaAstNodeType.LOCAL_VARIABLE_DECLARATION, JavaAstNodeType.FORMAL_PARAMETER,
    JavaAstNodeType.INTERFACE_DECLARATION, JavaAstNodeType.ENUM_DECLARATION,
    JavaAstNodeType.ANNOTATION_TYPE_DECLARATION, JavaAstNodeType.RECORD_DECLARATION,
    JavaAstNodeType.PACKAGE_DECLARATION, JavaAstNodeType.IMPORT_DECLARATION
]


CONTROL_FLOW_NODES = [
    JavaAstNodeType.IF_STATEMENT, JavaAstNodeType.FOR_STATEMENT,
    JavaAstNodeType.ENHANCED_FOR_STATEMENT, JavaAstNodeType.WHILE_STATEMENT,
    JavaAstNodeType.DO_STATEMENT, JavaAstNodeType.SWITCH_STATEMENT,
    JavaAstNodeType.TRY_STATEMENT, JavaAstNodeType.TRY_WITH_RESOURCES_STATEMENT,
    JavaAstNodeType.CATCH_CLAUSE, JavaAstNodeType.FINALLY_CLAUSE
]


EXPRESSION_NODES = [
    JavaAstNodeType.IDENTIFIER, JavaAstNodeType.METHOD_INVOCATION,
    JavaAstNodeType.FIELD_ACCESS, JavaAstNodeType.ARRAY_ACCESS,
    JavaAstNodeType.BINARY_EXPRESSION, JavaAstNodeType.UNARY_EXPRESSION,
    JavaAstNodeType.UPDATE_EXPRESSION, JavaAstNodeType.CAST_EXPRESSION,
    JavaAstNodeType.INSTANCEOF_EXPRESSION, JavaAstNodeType.LAMBDA_EXPRESSION,
    JavaAstNodeType.PARENTHESIZED_EXPRESSION, JavaAstNodeType.OBJECT_CREATION_EXPRESSION,
    JavaAstNodeType.ARRAY_CREATION_EXPRESSION, JavaAstNodeType.CLASS_LITERAL,
    JavaAstNodeType.THIS, JavaAstNodeType.SUPER, JavaAstNodeType.TERNARY_EXPRESSION,
    JavaAstNodeType.METHOD_REFERENCE, JavaAstNodeType.ASSIGNMENT_EXPRESSION,
    JavaAstNodeType.SWITCH_EXPRESSION, JavaAstNodeType.TEMPLATE_EXPRESSION
]


LITERAL_NODES = [
    JavaAstNodeType.STRING_LITERAL, JavaAstNodeType.DECIMAL_INTEGER_LITERAL,
    JavaAstNodeType.HEX_INTEGER_LITERAL, JavaAstNodeType.OCTAL_INTEGER_LITERAL,
    JavaAstNodeType.BINARY_INTEGER_LITERAL, JavaAstNodeType.DECIMAL_FLOATING_POINT_LITERAL,
    JavaAstNodeType.HEX_FLOATING_POINT_LITERAL, JavaAstNodeType.CHARACTER_LITERAL,
    JavaAstNodeType.TRUE, JavaAstNodeType.FALSE, JavaAstNodeType.NULL_LITERAL
]

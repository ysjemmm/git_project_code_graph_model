
from dataclasses import dataclass
from typing import Optional, List

from parser.common.external_type_manager import (
    get_external_type_manager, ExternalTypeReference
)
from parser.common.symbol_table import (
    SymbolTable, Symbol, SymbolType, SymbolIdGenerator, InheritanceEdge
)
from parser.languages.java.core.ast_node_types import (
    JavaFileStructure, ClassInfo, InterfaceInfo, EnumInfo,
    RecordInfo, AnnotationTypeInfo, MethodInfo, ConstructorInfo, FieldInfo
)
from parser.languages.java.core.ast_node_types import LocationRange


@dataclass
class SymbolTableBuilder:
    
    symbol_table: SymbolTable
    current_file: str = ""                    # 当前解析的文件路径
    current_class_stack: List[str] = None     # 当前类作用域栈(支持嵌套类)
    current_method: Optional[str] = None      # 当前方法符号ID
    current_package_symbol_id: Optional[str] = None  # 当前包符号ID
    current_package_name: Optional[str] = None  # 当前包名(用于生symbol_id)
    
    def __post_init__(self):
        if self.current_class_stack is None:
            self.current_class_stack = []
    
    def _get_package_name(self) -> Optional[str]:
        
        if self.current_package_symbol_id and self.current_package_symbol_id.startswith("package:"):
            return self.current_package_symbol_id[8:]  # 移除 "package:" 前缀
        return None
    
    # ========== 入口方法 ==========
    
    def build_from_java_file(self, java_file: JavaFileStructure, file_path: str):
        """JavaFileStructure 构建符号"""
        self.current_file = file_path
        
        # 创建 JAVAFILE 节点
        self._register_javafile(file_path)
        
        # 处理包声
        if java_file.package_info and java_file.package_info.name:
            self._register_package(java_file.package_info.name)
            # 存储包符号ID供后续使
            self.current_package_symbol_id = f"package:{java_file.package_info.name}"
        
        # 处理导入声明
        for import_info in java_file.import_details:
            self._register_import(import_info)
        
        # 处理所有类型声
        for class_info in java_file.classes:
            self._register_class(class_info)
        
        for interface_info in java_file.interfaces:
            self._register_interface(interface_info)
        
        for enum_info in java_file.enums:
            self._register_enum(enum_info)
        
        for record_info in java_file.records:
            self._register_record(record_info)
        
        for annotation_info in java_file.annotations:
            self._register_annotation(annotation_info)
        
        # 第二阶段:解析所有类型(在所有符号注册完成后
        self._resolve_all_types()
        
        # 第三阶段:解析所有继承关系边(将类型名转换为符号ID
        self._resolve_all_inheritance_edges()
        
        # 第四阶段:注册方法调用关系边
        self.register_all_method_calls(java_file)
        
        # 第五阶段:注册字段访问关系边
        self.register_all_field_accesses(java_file)
    
    # ========== 类型声明注册 ==========
    
    def _register_class(self, class_info: ClassInfo):
        """注册类及其成员符号"""
        symbol_id = SymbolIdGenerator.for_class(self.current_file, class_info.type_name)
        
        # 回填 symbol_id ClassInfo
        class_info.symbol_id = symbol_id
        
        # 构建全限定名
        qualified_name = self._build_qualified_name(class_info.type_name)
        
        # 提取注解名称列表
        annotation_names = [ann.name for ann in class_info.annotations] if class_info.annotations else []
        
        # 创建类符
        class_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.CLASS,
            name=class_info.type_name,
            qualified_name=qualified_name,
            declaring_symbol=self.current_package_symbol_id,  # 链接到包
            location=class_info.position,
            file_path=self.current_file,
            type_name=None,  # 类本身不是某个类型的实例,所type_name 应该None
            is_public=self._extract_is_public(class_info.raw_metadata),
            is_abstract='abstract' in class_info.raw_metadata,
            is_final='final' in class_info.raw_metadata,
            metadata={"annotations": annotation_names, "raw_metadata": class_info.raw_metadata}
        )
        
        self.symbol_table.register_symbol(class_symbol)
        
        # 进入类作用域
        self.current_class_stack.append(symbol_id)
        
        # 注册继承关系
        self._extract_class_inheritance(class_info, symbol_id)
        
        # 注册类成
        self._register_class_members(class_info, symbol_id)
        
        # 退出类作用
        self.current_class_stack.pop()
    
    def _register_interface(self, interface_info: InterfaceInfo):
        
        symbol_id = SymbolIdGenerator.for_class(self.current_file, interface_info.type_name)
        
        # 回填 symbol_id
        interface_info.symbol_id = symbol_id
        
        qualified_name = self._build_qualified_name(interface_info.type_name)
        
        # 提取注解名称列表
        annotation_names = [ann.name for ann in interface_info.annotations] if interface_info.annotations else []
        
        interface_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.INTERFACE,
            name=interface_info.type_name,
            qualified_name=qualified_name,
            declaring_symbol=self.current_package_symbol_id,  # 链接到包
            location=interface_info.position,
            file_path=self.current_file,
            type_name=None,  # 接口本身不是某个类型的实例,所type_name 应该None
            is_public=self._extract_is_public(interface_info.raw_metadata),
            is_abstract=True,  # 接口默认是抽象的
            metadata={"annotations": annotation_names, "raw_metadata": interface_info.raw_metadata}
        )
        
        self.symbol_table.register_symbol(interface_symbol)
        self.current_class_stack.append(symbol_id)
        
        # 注册接口继承关系(extends 其他接口
        self._extract_interface_inheritance(interface_info, symbol_id)
        
        self._register_interface_members(interface_info, symbol_id)
        self.current_class_stack.pop()
    
    def _register_enum(self, enum_info: EnumInfo):
        
        symbol_id = SymbolIdGenerator.for_class(self.current_file, enum_info.type_name)
        
        # 回填 symbol_id
        enum_info.symbol_id = symbol_id
        
        qualified_name = self._build_qualified_name(enum_info.type_name)
        
        # 提取注解名称列表
        annotation_names = [ann.name for ann in enum_info.annotations] if enum_info.annotations else []
        
        enum_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.ENUM,
            name=enum_info.type_name,
            qualified_name=qualified_name,
            declaring_symbol=self.current_package_symbol_id,  # 链接到包
            location=enum_info.position,
            file_path=self.current_file,
            type_name=None,  # 枚举本身不是某个类型的实例,所type_name 应该None
            is_public=self._extract_is_public(enum_info.raw_metadata),
            metadata={"annotations": annotation_names, "raw_metadata": enum_info.raw_metadata}
        )
        
        self.symbol_table.register_symbol(enum_symbol)
        self.current_class_stack.append(symbol_id)
        
        # 注册枚举实现的接口
        self._extract_enum_inheritance(enum_info, symbol_id)
        
        # 注册枚举成员
        self._register_enum_members(enum_info, symbol_id)
        
        self.current_class_stack.pop()
    def _register_record(self, record_info: RecordInfo):
        
        symbol_id = SymbolIdGenerator.for_class(self.current_file, record_info.type_name)
        
        # 回填 symbol_id
        record_info.symbol_id = symbol_id
        
        qualified_name = self._build_qualified_name(record_info.type_name)
        
        # 提取注解名称列表
        annotation_names = [ann.name for ann in record_info.annotations] if record_info.annotations else []
        
        record_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.RECORD,
            name=record_info.type_name,
            qualified_name=qualified_name,
            declaring_symbol=self.current_package_symbol_id,  # 链接到包
            location=record_info.position,
            file_path=self.current_file,
            type_name=None,  # record 本身不是某个类型的实例,所type_name 应该None
            is_public=self._extract_is_public(record_info.raw_metadata),
            is_final=True,  # record 默认final 
            metadata={"annotations": annotation_names, "raw_metadata": record_info.raw_metadata}
        )
        
        self.symbol_table.register_symbol(record_symbol)
        self.current_class_stack.append(symbol_id)
        
        # 注册 record 实现的接口
        self._extract_record_inheritance(record_info, symbol_id)
        
        # 注册 record 成员
        self._register_record_members(record_info, symbol_id)
        
        self.current_class_stack.pop()
    
    def _register_annotation(self, annotation_info: AnnotationTypeInfo):
        
        symbol_id = SymbolIdGenerator.for_class(self.current_file, annotation_info.type_name)
        
        # 回填 symbol_id
        annotation_info.symbol_id = symbol_id
        
        qualified_name = self._build_qualified_name(annotation_info.type_name)
        
        # 提取注解名称列表(注解定义本身可能也有注解)
        annotation_names = [ann.name for ann in annotation_info.annotations] if annotation_info.annotations else []
        
        annotation_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.ANNOTATION,
            name=annotation_info.type_name,
            qualified_name=qualified_name,
            declaring_symbol=self.current_package_symbol_id,  # 链接到包
            location=annotation_info.position,

            file_path=self.current_file,
            type_name=None,  # 注解本身不是某个类型的实例,所type_name 应该None
            is_public=self._extract_is_public(annotation_info.raw_metadata),
            metadata={"annotations": annotation_names, "raw_metadata": annotation_info.raw_metadata}
        )
        
        self.symbol_table.register_symbol(annotation_symbol)
        self.current_class_stack.append(symbol_id)
        
        # 注册注解成员
        self._register_annotation_members(annotation_info, symbol_id)
        
        self.current_class_stack.pop()
    
    # ========== 成员注册 ==========
    
    def _register_class_members(self, class_info: ClassInfo, class_symbol_id: str):
        
        # 注册字段
        for field in class_info.fields:
            self._register_field(field, class_symbol_id)
        
        # 注册方法
        for method in class_info.methods:
            self._register_method(method, class_symbol_id, is_static=False)
        
        # 注册构造器
        for constructor in class_info.constructors:
            self._register_constructor(constructor, class_symbol_id)
        
        # 注册嵌套类型
        self._register_nested_types(class_info, class_symbol_id)
    
    def _register_interface_members(self, interface_info: InterfaceInfo, interface_symbol_id: str):
        
        # 注册常量字段
        for constant in interface_info.constants:
            self._register_field(constant, interface_symbol_id, is_static=True, is_final=True)
        
        # 注册方法
        for method in interface_info.methods:
            self._register_method(method, interface_symbol_id)
        
        # 注册嵌套类型
        self._register_nested_types(interface_info, interface_symbol_id)
    
    def _register_enum_members(self, enum_info: EnumInfo, enum_symbol_id: str):
        
        # 注册枚举常量
        for constant in enum_info.enum_constants:
            self._register_enum_constant(constant, enum_symbol_id)
        
        # 注册字段
        for field in enum_info.fields:
            self._register_field(field, enum_symbol_id)
        
        # 注册方法
        for method in enum_info.methods:
            self._register_method(method, enum_symbol_id)
        
        # 注册构造器
        for constructor in enum_info.constructors:
            self._register_constructor(constructor, enum_symbol_id)
        
        # 注册嵌套类型
        self._register_nested_types(enum_info, enum_symbol_id)
    
    def _register_record_members(self, record_info: RecordInfo, record_symbol_id: str):
        
        # 注册 record 组件
        for component in record_info.components:
            self._register_record_component(component, record_symbol_id)
        
        # 注册方法
        for method in record_info.methods:
            self._register_method(method, record_symbol_id)
        
        # 注册构造器
        for constructor in record_info.constructors:
            self._register_constructor(constructor, record_symbol_id)
        
        # 注册嵌套类型
        self._register_nested_types(record_info, record_symbol_id)
    
    def _register_annotation_members(self, annotation_info: AnnotationTypeInfo, annotation_symbol_id: str):
        
        # 注册注解元素
        for element in annotation_info.elements:
            self._register_annotation_element(element, annotation_symbol_id)
        
        # 注册常量字段
        for constant in annotation_info.constants:
            self._register_field(constant, annotation_symbol_id, is_static=True, is_final=True)
        
        # 注册嵌套类型
        self._register_nested_types(annotation_info, annotation_symbol_id)
    
    def _register_nested_types(self, type_info, parent_symbol_id: str):
        
        # 嵌套
        if hasattr(type_info, 'nested_classes'):
            for nested_class in type_info.nested_classes:
                self._register_nested_class(nested_class, parent_symbol_id)
        
        # 嵌套接口
        if hasattr(type_info, 'nested_interfaces'):
            for nested_interface in type_info.nested_interfaces:
                self._register_nested_interface(nested_interface, parent_symbol_id)
        
        # 嵌套枚举
        if hasattr(type_info, 'nested_enums'):
            for nested_enum in type_info.nested_enums:
                self._register_nested_enum(nested_enum, parent_symbol_id)
        
        # 嵌套记录
        if hasattr(type_info, 'nested_records'):
            for nested_record in type_info.nested_records:
                self._register_nested_record(nested_record, parent_symbol_id)
        
        # 嵌套注解
        if hasattr(type_info, 'nested_annotations'):
            for nested_annotation in type_info.nested_annotations:
                self._register_nested_annotation(nested_annotation, parent_symbol_id)
    
    def _register_field(self, field: FieldInfo, class_symbol_id: str, 
                       is_static: bool = False, is_final: bool = False):
        
        # 根据是否静态选择不同symbol_id 生成方法
        is_static_field = is_static or 'static' in field.raw_field
        if is_static_field:
            symbol_id = SymbolIdGenerator.for_static_field(class_symbol_id, field.field_name)
        else:
            symbol_id = SymbolIdGenerator.for_field(class_symbol_id, field.field_name)
        
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.{field.field_name}"
        
        # 解析字段类型
        type_symbol = self._resolve_type_symbol(field.field_type)
        
        # 提取注解名称列表
        annotation_names = [ann.name for ann in field.annotations] if field.annotations else []
        
        field_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.FIELD,
            name=field.field_name,
            qualified_name=qualified_name,
            declaring_symbol=class_symbol_id,
            location=field.position,
            file_path=self.current_file,
            type_name=field.field_type,
            type_symbol=type_symbol,  # 添加类型符号
            is_static=is_static_field,
            is_final=is_final or 'final' in field.raw_field,
            is_private='private' in field.raw_field,
            is_protected='protected' in field.raw_field,
            is_public='public' in field.raw_field,
            metadata={
                'raw_field': field.raw_field,
                'annotations': annotation_names,
                'has_default_value': field.has_default_value,
                'default_value': field.default_value
            }
        )
        
        self.symbol_table.register_symbol(field_symbol)
        
        # 添加属于边:字段 -> 
        from parser.common.symbol_table import MembershipEdge, TypeEdge
        membership_edge = MembershipEdge(
            source_symbol=symbol_id,
            target_symbol=class_symbol_id,
            edge_type="member_of"
        )
        self.symbol_table.add_membership_edge(membership_edge)
        
        # 添加类型边:字段 -> 字段类型(如果类型符号存在)
        if type_symbol:
            type_edge = TypeEdge(
                source_symbol=symbol_id,
                target_symbol=type_symbol,
                edge_type="has_type"
            )
            self.symbol_table.add_type_edge(type_edge)
        
        # 保存字段类型信息SymbolTable 用于调用边解
        if class_symbol_id not in self.symbol_table.field_types:
            self.symbol_table.field_types[class_symbol_id] = {}
        self.symbol_table.field_types[class_symbol_id][field.field_name] = field.field_type
    
    def _register_method(self, method: MethodInfo, class_symbol_id: str, is_static: bool = False):
        
        # 提取参数类型用于重载区分
        param_types = [p.type_name for p in method.parameters]
        
        # 根据是否静态选择不同symbol_id 生成方法
        is_static_method = is_static or 'static' in method.raw_signature
        if is_static_method:
            symbol_id = SymbolIdGenerator.for_static_method(class_symbol_id, method.method_name, param_types)
        else:
            symbol_id = SymbolIdGenerator.for_method(class_symbol_id, method.method_name, param_types)
        
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.{method.method_name}"
        
        # 解析返回类型
        return_type_symbol = self._resolve_type_symbol(method.return_type)
        
        # 确定访问修饰
        # 如果显式指定了访问修饰符,使用显式的
        # 否则根据所属类型的默认
        is_public = 'public' in method.raw_signature
        is_private = 'private' in method.raw_signature
        is_protected = 'protected' in method.raw_signature
        
        # 如果没有显式指定任何访问修饰符,使用默认
        if not (is_public or is_private or is_protected):
            # 获取所属类的类
            declaring_class = self.symbol_table.lookup_by_id(class_symbol_id)
            if declaring_class:
                # 接口和注解的方法默认是public
                if declaring_class.symbol_type.value in ['interface', 'annotation']:
                    is_public = True
                # 类的方法:检查是否有HTTP方法注解或其他表示public的注
                elif declaring_class.symbol_type.value == 'class':
                    # 检查方法是否有HTTP方法注解(@GetMapping, @PostMapping等)或@RequestMapping
                    method_annotations = method.annotations if hasattr(method, 'annotations') else []
                    http_annotations = {'GetMapping', 'PostMapping', 'PutMapping', 'DeleteMapping', 'PatchMapping', 'RequestMapping'}
                    has_http_annotation = any(ann.name in http_annotations for ann in method_annotations if hasattr(ann, 'name'))
                    
                    if has_http_annotation:
                        is_public = True
                    # 否则默认是package-private(不设置任何标志)
        
        method_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.METHOD,
            name=method.method_name,
            qualified_name=qualified_name,
            declaring_symbol=class_symbol_id,
            location=method.position,
            file_path=self.current_file,
            type_name=method.return_type,
            type_symbol=return_type_symbol,  # 添加返回类型符号
            is_static=is_static_method,
            is_abstract='abstract' in method.raw_signature,
            is_final='final' in method.raw_signature,
            is_private=is_private,
            is_protected=is_protected,
            is_public=is_public,
            metadata={
                'throws': method.exceptions,
                'param_types': param_types,
                'raw_signature': method.raw_method,
                'annotations': [ann.name if hasattr(ann, 'name') else str(ann) for ann in method.annotations] if hasattr(method, 'annotations') else [],
                'raw_method': method.raw_method if hasattr(method, 'raw_method') else '',
                'referenced_types': method.referenced_types if hasattr(method, 'referenced_types') else {},
                'field_accesses': method.field_accesses if hasattr(method, 'field_accesses') else [],
                'method_calls': method.method_calls if hasattr(method, 'method_calls') else []
            }
        )
        
        self.symbol_table.register_symbol(method_symbol)
        
        # 添加属于边:方法 -> 
        from parser.common.symbol_table import MembershipEdge, TypeEdge
        membership_edge = MembershipEdge(
            source_symbol=symbol_id,
            target_symbol=class_symbol_id,
            edge_type="member_of"
        )
        self.symbol_table.add_membership_edge(membership_edge)
        
        # 添加返回类型边:方法 -> 返回类型(如果类型符号存在)
        if return_type_symbol and method.return_type != 'void':
            type_edge = TypeEdge(
                source_symbol=symbol_id,
                target_symbol=return_type_symbol,
                edge_type="returns"
            )
            self.symbol_table.add_type_edge(type_edge)
        
        # 进入方法作用
        self.current_method = symbol_id
        
        # 注册方法参数
        for idx, param in enumerate(method.parameters):
            self._register_parameter(param, symbol_id, idx)
        
        # TODO: 分析方法体中的局部变量(需AST 节点
        
        # 退出方法作用域
        self.current_method = None
    
    def _register_constructor(self, constructor: ConstructorInfo, class_symbol_id: str):
        
        param_types = [p.type_name for p in constructor.parameters]
        symbol_id = SymbolIdGenerator.for_constructor(class_symbol_id, param_types)
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.<init>"
        
        # 解析构造器返回类型(构造器返回类型是类本身
        return_type_symbol = self._resolve_type_symbol(class_name)
        
        constructor_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.CONSTRUCTOR,
            name="<init>",
            qualified_name=qualified_name,
            declaring_symbol=class_symbol_id,
            location=constructor.position,
            file_path=self.current_file,
            type_name=class_name,  # 构造器返回类型是类本身
            type_symbol=return_type_symbol,  # 添加返回类型符号
            is_private='private' in constructor.raw_signature,
            is_protected='protected' in constructor.raw_signature,
            is_public='public' in constructor.raw_signature,
            metadata={
                'throws': constructor.throws_exceptions,
                'param_types': param_types
            }
        )
        
        self.symbol_table.register_symbol(constructor_symbol)
        
        # 添加属于边:构造器 -> 
        from parser.common.symbol_table import MembershipEdge
        membership_edge = MembershipEdge(
            source_symbol=symbol_id,
            target_symbol=class_symbol_id,
            edge_type="member_of"
        )
        self.symbol_table.add_membership_edge(membership_edge)
        
        # 注册参数
        for idx, param in enumerate(constructor.parameters):
            self._register_parameter(param, symbol_id, idx)
    
    def _register_parameter(self, param, method_symbol_id: str, index: int):
        
        symbol_id = SymbolIdGenerator.for_parameter(method_symbol_id, param.name, index)
        class_name = self._get_class_name_from_stack()
        method_name = self._get_method_name_from_id(method_symbol_id)
        
        # 解析参数类型
        param_type_symbol = self._resolve_type_symbol(param.type_name)
        
        param_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.PARAMETER,
            name=param.name,
            qualified_name=f"{method_symbol_id}.${param.name}",
            declaring_symbol=method_symbol_id,
            location=param.position,
            file_path=self.current_file,
            type_name=param.type_name,
            type_symbol=param_type_symbol  # 添加参数类型符号
        )
        
        self.symbol_table.register_symbol(param_symbol)
        
        # 添加参数类型边:参数 -> 参数类型(如果类型符号存在)
        if param_type_symbol:
            from parser.common.symbol_table import TypeEdge
            type_edge = TypeEdge(
                source_symbol=symbol_id,
                target_symbol=param_type_symbol,
                edge_type="has_type"
            )
            self.symbol_table.add_type_edge(type_edge)
    
    def _register_enum_constant(self, constant, enum_symbol_id: str):
        
        # 枚举常量是静态的,使for_static_field
        symbol_id = SymbolIdGenerator.for_static_field(enum_symbol_id, constant.constant_name)
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.{constant.constant_name}"

        # 解析枚举常量类型(枚举常量的类型是枚举本身)
        constant_type_symbol = self._resolve_type_symbol(class_name)

        constant_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.FIELD,
            name=constant.constant_name,
            qualified_name=qualified_name,
            declaring_symbol=enum_symbol_id,
            location=constant.position,
            file_path=self.current_file,
            type_name=class_name,
            type_symbol=constant_type_symbol,  # 添加类型符号
            is_static=True,
            is_final=True,
            metadata={
                "raw_constant": constant.raw_constant,
                "is_enum_constant": True,
            },
        )

        self.symbol_table.register_symbol(constant_symbol)

    def _register_record_component(self, component, record_symbol_id: str):
        
        symbol_id = SymbolIdGenerator.for_field(record_symbol_id, component.name)
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.{component.name}"

        # 解析 record 组件类型
        component_type_symbol = self._resolve_type_symbol(component.type_name)

        component_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.FIELD,
            name=component.name,
            qualified_name=qualified_name,
            declaring_symbol=record_symbol_id,
            location=component.position,
            file_path=self.current_file,
            type_name=component.type_name,
            type_symbol=component_type_symbol,  # 添加类型符号
            is_final=True,
            is_private=True,
            metadata={
                "raw_parameter": component.raw_parameter,
                "is_record_component": True,
            },
        )

        self.symbol_table.register_symbol(component_symbol)

    def _register_annotation_element(self, element, annotation_symbol_id: str):
        
        # 注解元素是常量,使用 for_static_method
        symbol_id = SymbolIdGenerator.for_static_method(annotation_symbol_id, element.element_name, [])
        class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(class_name)}.{element.element_name}"

        # 解析注解元素类型
        element_type_symbol = self._resolve_type_symbol(element.element_type)

        element_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.METHOD,
            name=element.element_name,
            qualified_name=qualified_name,
            declaring_symbol=annotation_symbol_id,
            location=element.position,
            file_path=self.current_file,
            type_name=element.element_type,
            type_symbol=element_type_symbol,  # 添加类型符号
            is_public=True,
            is_abstract=True,
            is_static=True,
            metadata={
                "default_value": element.default_value,
                "is_annotation_element": True,
            },
        )

        self.symbol_table.register_symbol(element_symbol)
    
    # ========== 嵌套类型注册 ==========
    
    def _register_nested_class(self, nested_class: ClassInfo, parent_symbol_id: str):
        """注册嵌套类"""
        # 嵌套类的 symbol_id 基于父类symbol_id
        symbol_id = SymbolIdGenerator.for_class(parent_symbol_id, nested_class.type_name)
        
        # 回填 symbol_id
        nested_class.symbol_id = symbol_id
        
        parent_class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(parent_class_name)}.{nested_class.type_name}"
        
        nested_class_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.CLASS,
            name=nested_class.type_name,
            qualified_name=qualified_name,
            declaring_symbol=parent_symbol_id,
            location=nested_class.position,
            file_path=self.current_file,
            type_name=nested_class.type_name,
            is_public=self._extract_is_public(nested_class.raw_metadata),
            is_static='static' in nested_class.raw_metadata
        )
        
        self.symbol_table.register_symbol(nested_class_symbol)
        self.current_class_stack.append(symbol_id)
        self._register_class_members(nested_class, symbol_id)
        self.current_class_stack.pop()
    
    def _register_nested_interface(self, nested_interface: InterfaceInfo, parent_symbol_id: str):
        
        # 嵌套接口symbol_id 基于父类symbol_id
        symbol_id = SymbolIdGenerator.for_class(parent_symbol_id, nested_interface.type_name)
        
        nested_interface.symbol_id = symbol_id
        
        parent_class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(parent_class_name)}.{nested_interface.type_name}"
        
        nested_interface_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.INTERFACE,
            name=nested_interface.type_name,
            qualified_name=qualified_name,
            declaring_symbol=parent_symbol_id,
            location=nested_interface.position,
            file_path=self.current_file,
            type_name=nested_interface.type_name,
            is_public=self._extract_is_public(nested_interface.raw_metadata),
            is_static='static' in nested_interface.raw_metadata
        )
        
        self.symbol_table.register_symbol(nested_interface_symbol)
        self.current_class_stack.append(symbol_id)
        self._register_interface_members(nested_interface, symbol_id)
        self.current_class_stack.pop()
    
    def _register_nested_enum(self, nested_enum: EnumInfo, parent_symbol_id: str):
        
        # 嵌套枚举symbol_id 基于父类symbol_id
        symbol_id = SymbolIdGenerator.for_class(parent_symbol_id, nested_enum.type_name)
        
        nested_enum.symbol_id = symbol_id
        
        parent_class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(parent_class_name)}.{nested_enum.type_name}"
        
        nested_enum_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.ENUM,
            name=nested_enum.type_name,
            qualified_name=qualified_name,
            declaring_symbol=parent_symbol_id,
            location=nested_enum.position,
            file_path=self.current_file,
            type_name=nested_enum.type_name,
            is_public=self._extract_is_public(nested_enum.raw_metadata),
            is_static='static' in nested_enum.raw_metadata
        )
        
        self.symbol_table.register_symbol(nested_enum_symbol)
        self.current_class_stack.append(symbol_id)
        self._register_enum_members(nested_enum, symbol_id)
        self.current_class_stack.pop()
    
    def _register_nested_record(self, nested_record: RecordInfo, parent_symbol_id: str):
        
        # 嵌套记录symbol_id 基于父类symbol_id
        symbol_id = SymbolIdGenerator.for_class(parent_symbol_id, nested_record.type_name)
        
        nested_record.symbol_id = symbol_id
        
        parent_class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(parent_class_name)}.{nested_record.type_name}"
        
        nested_record_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.RECORD,
            name=nested_record.type_name,
            qualified_name=qualified_name,
            declaring_symbol=parent_symbol_id,
            location=nested_record.position,
            file_path=self.current_file,
            type_name=nested_record.type_name,
            is_public=self._extract_is_public(nested_record.raw_metadata),
            is_static='static' in nested_record.raw_metadata
        )
        
        self.symbol_table.register_symbol(nested_record_symbol)
        self.current_class_stack.append(symbol_id)
        self._register_record_members(nested_record, symbol_id)
        self.current_class_stack.pop()
    
    def _register_nested_annotation(self, nested_annotation: AnnotationTypeInfo, parent_symbol_id: str):
        
        # 嵌套注解symbol_id 基于父类symbol_id
        symbol_id = SymbolIdGenerator.for_class(parent_symbol_id, nested_annotation.type_name)
        
        nested_annotation.symbol_id = symbol_id
        
        parent_class_name = self._get_class_name_from_stack()
        qualified_name = f"{self._build_qualified_name(parent_class_name)}.{nested_annotation.type_name}"
        
        nested_annotation_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.ANNOTATION,
            name=nested_annotation.type_name,
            qualified_name=qualified_name,
            declaring_symbol=parent_symbol_id,
            location=nested_annotation.position,
            file_path=self.current_file,
            type_name=nested_annotation.type_name,
            is_public=self._extract_is_public(nested_annotation.raw_metadata),
            is_static='static' in nested_annotation.raw_metadata
        )
        
        self.symbol_table.register_symbol(nested_annotation_symbol)
        self.current_class_stack.append(symbol_id)
        self._register_annotation_members(nested_annotation, symbol_id)
        self.current_class_stack.pop()
    
    def _register_package(self, package_name: str):
        """注册包符号"""
        symbol_id = f"package:{package_name}"

        package_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.PACKAGE,
            name=package_name.split('.')[-1],
            qualified_name=package_name,
            location=LocationRange(),
            file_path=self.current_file
        )
        
        self.symbol_table.register_symbol(package_symbol)
    
    def _register_javafile(self, file_path: str):
        
        symbol_id = f"file#{file_path}"
        
        # 提取文件名(不含路径
        file_name = file_path.split('/')[-1] if '/' in file_path else file_path.split('\\')[-1]
        
        java_file_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.JAVAFILE,
            name=file_name,
            qualified_name=file_path,
            location=LocationRange(),
            file_path=file_path
        )
        
        self.symbol_table.register_symbol(java_file_symbol)
    
    def _register_import(self, import_info):
        """注册导入符号"""
        # 生成导入符号ID
        symbol_id = SymbolIdGenerator.for_import(self.current_file, import_info.import_path)
        
        # 提取导入的类名(最后一个点后面的部分,或* 如果是通配符)
        import_name = import_info.import_path.split('.')[-1]
        
        import_symbol = Symbol(
            symbol_id=symbol_id,
            symbol_type=SymbolType.IMPORT,
            name=import_name,
            qualified_name=import_info.import_path,
            location=import_info.position,
            file_path=self.current_file,
            metadata={
                'import_path': import_info.import_path,
                'is_static': import_info.is_static,
                'is_wildcard': import_info.is_wildcard
            }
        )
        
        self.symbol_table.register_symbol(import_symbol)
    
    # ========== 辅助方法 ==========
    
    def _build_qualified_name(self, type_name: str) -> str:
        
        # TODO: package_info 获取包名
        if len(self.current_class_stack) > 1:
            # 嵌套
            parent_class = self.current_class_stack[-1].split('#')[-1]
            return f"{parent_class}.{type_name}"
        return type_name
    
    def _get_class_name_from_stack(self) -> str:
        """从类栈获取当前类"""
        if self.current_class_stack:
            return self.current_class_stack[-1].split('#')[-1]
        return ""
    
    def _get_method_name_from_id(self, method_symbol_id: str) -> str:
        """从方法符号ID提取方法"""
        # 格式: file_path#ClassName#methodName(params)
        parts = method_symbol_id.split('#')
        if len(parts) >= 3:
            return parts[2].split('(')[0]
        return ""
    
    def _extract_is_public(self, raw_metadata: str) -> bool:
        
        return 'public' in raw_metadata
    
    # ========== 类型解析 ==========
    
    def _resolve_all_types(self):
        
        for symbol_id, symbol in self.symbol_table.symbols.items():
            if symbol.type_name and not symbol.type_symbol:
                # 尝试解析该符号的类型
                type_symbol = self._resolve_type_symbol_in_context(symbol.type_name, symbol.declaring_symbol)
                if type_symbol:
                    symbol.type_symbol = type_symbol
                    
                    # 创建缺失TypeEdge(第一阶段可能因为类型未注册而没有创建)
                    from parser.common.symbol_table import TypeEdge
                    type_edge = TypeEdge(
                        source_symbol=symbol_id,
                        target_symbol=type_symbol,
                        edge_type="has_type"
                    )
                    self.symbol_table.add_type_edge(type_edge)
    
    def _resolve_all_inheritance_edges(self):
        """第三阶段:解析所有继承关系边(将类型名转换为符号ID)"""
        external_type_manager = get_external_type_manager()
        
        for edge in self.symbol_table.inheritance_edges:
            # 如果 target_symbol 还是类型名(不是符号ID),需要解
            if not edge.target_symbol.startswith(self.current_file):
                # target_symbol 是类型名,需要转换为符号ID
                type_name = edge.target_symbol
                resolved_symbol_id = self._resolve_type_symbol(type_name)
                
                if resolved_symbol_id:
                    # 更新边的目标符号
                    edge.target_symbol = resolved_symbol_id
                else:
                    # 如果无法解析,尝试在所有文件中查找
                    resolved_symbol_id = self._find_type_in_all_files(type_name)
                    if resolved_symbol_id:
                        edge.target_symbol = resolved_symbol_id
                    else:
                        # 尝试解析为外部类
                        external_type = external_type_manager.resolve_type_to_external(type_name)
                        if external_type:
                            # 创建外部类型的虚拟符号ID
                            external_symbol_id = f"external:{external_type.qualified_name}"
                            edge.target_symbol = external_symbol_id
                            
                            # 注册外部类型引用
                            reference = ExternalTypeReference(
                                source_symbol_id=edge.source_symbol,
                                target_type_name=type_name,
                                target_qualified_name=external_type.qualified_name,
                                reference_type=edge.edge_type
                            )
                            external_type_manager.add_external_reference(reference)
                            
                            # 如果外部类型还没有在符号表中,创建一个虚拟符
                            if external_symbol_id not in self.symbol_table.symbols:
                                external_symbol = Symbol(
                                    symbol_id=external_symbol_id,
                                    symbol_type=SymbolType.INTERFACE if external_type.is_interface else SymbolType.CLASS,
                                    name=external_type.type_name,
                                    qualified_name=external_type.qualified_name,
                                    file_path=f"external:{external_type.package_name}",
                                    is_public=True,
                                    metadata={
                                        "is_external": True,
                                        "source": external_type.source.value,
                                        "package": external_type.package_name,
                                    }
                                )
                                self.symbol_table.register_symbol(external_symbol)
    
    def _find_type_in_all_files(self, type_name: str) -> Optional[str]:
        
        for symbol_id, symbol in self.symbol_table.symbols.items():
            if symbol.name == type_name and symbol.symbol_type in [
                SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.ENUM,
                SymbolType.RECORD, SymbolType.ANNOTATION
            ]:
                return symbol_id
        return None
    
    def _resolve_type_symbol_in_context(self, type_name: str, declaring_symbol: Optional[str]) -> Optional[str]:
        """在给定上下文中解析类"""
        if not type_name:
            return None
        
        # 1. 检查是否是基本类型
        basic_types = {'int', 'long', 'float', 'double', 'boolean', 'byte', 'short', 'char', 'void', 'String'}
        if type_name in basic_types:
            return None
        
        # 2. 检查是否是泛型类型(如 List<String>
        if '<' in type_name:
            base_type = type_name.split('<')[0].strip()
            return self._resolve_type_symbol_in_context(base_type, declaring_symbol)
        
        # 3. 检查是否是数组类型(如 String[]
        if type_name.endswith('[]'):
            base_type = type_name[:-2].strip()
            return self._resolve_type_symbol_in_context(base_type, declaring_symbol)
        
        # 4. 检查是否是已注册的接口/枚举/记录/注解(精确匹配)
        for symbol_id, symbol in self.symbol_table.symbols.items():
            if symbol.name == type_name and symbol.symbol_type in [
                SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.ENUM,
                SymbolType.RECORD, SymbolType.ANNOTATION
            ]:
                return symbol_id
        
        # 5. 检查是否是嵌套类型(如 TestClass.Status
        if '.' in type_name:
            # 尝试直接查找完整的嵌套类型名
            for symbol_id, symbol in self.symbol_table.symbols.items():
                if symbol.qualified_name == type_name and symbol.symbol_type in [
                    SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.ENUM,
                    SymbolType.RECORD, SymbolType.ANNOTATION
                ]:
                    return symbol_id
        
        # 6. 检查是否是嵌套类型(如 Status TestClass 中)
        if declaring_symbol:
            # 获取声明符号的类
            declaring_class = self.symbol_table.lookup_by_id(declaring_symbol)
            if declaring_class:
                # 如果声明符号本身是嵌套类型,获取其父
                if declaring_class.declaring_symbol:
                    parent_class = self.symbol_table.lookup_by_id(declaring_class.declaring_symbol)
                    if parent_class:
                        # 尝试查找嵌套类型
                        for symbol_id, symbol in self.symbol_table.symbols.items():
                            if symbol.name == type_name and symbol.declaring_symbol == declaring_class.declaring_symbol:
                                return symbol_id
                
                # 尝试查找嵌套类型
                for symbol_id, symbol in self.symbol_table.symbols.items():
                    if symbol.name == type_name and symbol.declaring_symbol == declaring_symbol:
                        return symbol_id
        
        # 7. 如果没有找到,返None(可能是外部类型
        return None
    
    def _resolve_type_symbol(self, type_name: str) -> Optional[str]:
        
        if not type_name:
            return None
        
        # 1. 检查是否是基本类型
        basic_types = {'int', 'long', 'float', 'double', 'boolean', 'byte', 'short', 'char', 'void', 'String'}
        if type_name in basic_types:
            return None
        
        # 2. 检查是否是泛型类型(如 List<String>
        if '<' in type_name:
            base_type = type_name.split('<')[0].strip()
            return self._resolve_type_symbol(base_type)
        
        # 3. 检查是否是数组类型(如 String[]
        if type_name.endswith('[]'):
            base_type = type_name[:-2].strip()
            return self._resolve_type_symbol(base_type)
        
        # 4. 检查是否是已注册的接口/枚举/记录/注解
        for symbol_id, symbol in self.symbol_table.symbols.items():
            if symbol.name == type_name and symbol.symbol_type in [
                SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.ENUM,
                SymbolType.RECORD, SymbolType.ANNOTATION
            ]:
                return symbol_id
        
        # 5. 检查是否是嵌套类型(如 TestClass.Status
        # 如果当前在类中,尝试查找嵌套类型
        if self.current_class_stack:
            current_class_id = self.current_class_stack[0]  # 获取顶级
            current_class_name = current_class_id.split('#')[-1]
            nested_type_name = f"{current_class_name}.{type_name}"
            
            for symbol_id, symbol in self.symbol_table.symbols.items():
                if symbol.name == type_name and symbol.qualified_name.endswith(nested_type_name):
                    return symbol_id
        
        # 6. 如果没有找到,返None(可能是外部类型
        return None
    
    # ========== 继承关系提取 ==========
    
    def _extract_class_inheritance(self, class_info: ClassInfo, class_symbol_id: str):
        """提取类的继承关系(extends implements)"""
        raw_metadata = class_info.raw_metadata
        
        # 提取 extends(父类)
        # 格式示例: "public class User extends BaseUser implements Serializable"
        extends_type = self._extract_extends_type(raw_metadata)
        if extends_type:
            # 创建继承
            edge = InheritanceEdge(
                source_symbol=class_symbol_id,
                target_symbol=extends_type,  # 暂时存类型名,后续解析为符号ID
                edge_type="extends",
                is_extension=True,
                is_implementation=False
            )
            self.symbol_table.add_inheritance_edge(edge)
            # metadata 中标记需要后续解
            symbol = self.symbol_table.lookup_by_id(class_symbol_id)
            if symbol:
                symbol.metadata['extends'] = extends_type
        
        # 提取 implements(实现的接口
        implements_types = self._extract_implements_types(raw_metadata)
        for interface_type in implements_types:
            edge = InheritanceEdge(
                source_symbol=class_symbol_id,
                target_symbol=interface_type,
                edge_type="implements",
                is_extension=False,
                is_implementation=True
            )
            self.symbol_table.add_inheritance_edge(edge)
        
        if implements_types:
            symbol = self.symbol_table.lookup_by_id(class_symbol_id)
            if symbol:
                symbol.metadata['implements'] = implements_types
    
    def _extract_interface_inheritance(self, interface_info: InterfaceInfo, interface_symbol_id: str):
        """提取接口的继承关系(extends 其他接口)"""
        raw_metadata = interface_info.raw_metadata
        
        # 接口可以 extends 多个其他接口
        extends_types = self._extract_extends_types_for_interface(raw_metadata)
        for parent_interface in extends_types:
            edge = InheritanceEdge(
                source_symbol=interface_symbol_id,
                target_symbol=parent_interface,
                edge_type="extends",
                is_extension=True,
                is_implementation=False
            )
            self.symbol_table.add_inheritance_edge(edge)
        
        if extends_types:
            symbol = self.symbol_table.lookup_by_id(interface_symbol_id)
            if symbol:
                symbol.metadata['extends'] = extends_types
    
    def _extract_extends_type(self, raw_metadata: str) -> str:
        """从元数据提取 extends 的父类(单继承)
        
        处理泛型:如 "extends Repository<User>" -> "Repository"
        """
        import re
        # 匹配 "extends TypeName" "extends TypeName<...>"
        # 支持嵌套泛型Repository<User>
        match = re.search(r'extends\s+([\w<>,\s]+)(:\s+implements|{|\s*$)', raw_metadata)
        if match:
            type_str = match.group(1).strip()
            # 如果是泛型,提取基础类型
            if '<' in type_str:
                base_type = type_str.split('<')[0].strip()
                return base_type
            return type_str
        return ""
    
    def _extract_implements_types(self, raw_metadata: str) -> List[str]:
        """从元数据提取 implements 的接口列
        
        处理泛型:如 "implements Repository<User>, Auditable" -> ["Repository", "Auditable"]
        处理嵌套泛型:如 "implements Repository<V>, Map<K, V>" -> ["Repository", "Map"]
        """
        import re
        # 匹配 "implements Interface1, Interface2" "implements Interface1<T>, Interface2"
        match = re.search(r'implements\s+(.*)(:{|\s*$)', raw_metadata)
        if match:
            interfaces_str = match.group(1)
            # 分割多个接口,需要正确处理泛型中的逗号
            interfaces = self._split_generic_types(interfaces_str)
            result = []
            for i in interfaces:
                i = i.strip()
                if i:
                    # 如果是泛型,提取基础类型
                    if '<' in i:
                        base_type = i.split('<')[0].strip()
                        result.append(base_type)
                    else:
                        result.append(i)
            return result
        return []
    
    def _extract_extends_types_for_interface(self, raw_metadata: str) -> List[str]:
        """从元数据提取接口 extends 的父接口列表
        
        处理泛型:如 "extends Repository<T>, Auditable" -> ["Repository", "Auditable"]
        """
        import re
        # 匹配 "extends Interface1, Interface2" "extends Interface1<T>, Interface2"
        match = re.search(r'extends\s+(.*)(:{|\s*$)', raw_metadata)
        if match:
            interfaces_str = match.group(1)
            interfaces = self._split_generic_types(interfaces_str)
            result = []
            for i in interfaces:
                i = i.strip()
                if i:
                    # 如果是泛型,提取基础类型
                    if '<' in i:
                        base_type = i.split('<')[0].strip()
                        result.append(base_type)
                    else:
                        result.append(i)
            return result
        return []
    
    def _split_generic_types(self, types_str: str) -> List[str]:
        """分割泛型类型列表,正确处理嵌套泛型中的逗号
        
        例如
        - "Repository<User>, Auditable" -> ["Repository<User>", "Auditable"]
        - "Repository<V>, Map<K, V>" -> ["Repository<V>", "Map<K, V>"]
        - "Repository<List<User>>" -> ["Repository<List<User>>"]
        """
        result = []
        current = ""
        depth = 0  # 泛型嵌套深度
        
        for char in types_str:
            if char == '<':
                depth += 1
                current += char
            elif char == '>':
                depth -= 1
                current += char
            elif char == ',' and depth == 0:
                # 只在泛型外的逗号处分
                if current.strip():
                    result.append(current.strip())
                current = ""
            else:
                current += char
        
        # 添加最后一个类
        if current.strip():
            result.append(current.strip())
        
        return result
    
    def _extract_record_inheritance(self, record_info: RecordInfo, record_symbol_id: str):
        
        raw_metadata = record_info.raw_metadata
        
        # 提取 implements(实现的接口
        implements_types = self._extract_implements_types(raw_metadata)
        for interface_type in implements_types:
            edge = InheritanceEdge(
                source_symbol=record_symbol_id,
                target_symbol=interface_type,
                edge_type="implements",
                is_extension=False,
                is_implementation=True
            )
            self.symbol_table.add_inheritance_edge(edge)
        
        if implements_types:
            symbol = self.symbol_table.lookup_by_id(record_symbol_id)
            if symbol:
                symbol.metadata['implements'] = implements_types
    
    def _extract_enum_inheritance(self, enum_info: EnumInfo, enum_symbol_id: str):
        
        raw_metadata = enum_info.raw_metadata
        
        # 提取 implements(实现的接口
        implements_types = self._extract_implements_types(raw_metadata)
        for interface_type in implements_types:
            edge = InheritanceEdge(
                source_symbol=enum_symbol_id,
                target_symbol=interface_type,
                edge_type="implements",
                is_extension=False,
                is_implementation=True
            )
            self.symbol_table.add_inheritance_edge(edge)
        
        if implements_types:
            symbol = self.symbol_table.lookup_by_id(enum_symbol_id)
            if symbol:
                symbol.metadata['implements'] = implements_types
    
    # ========== 方法调用关系==========
    
    def register_all_method_calls(self, java_file: JavaFileStructure):
        
        # 遍历所有类的方
        for cls in java_file.classes:
            self._register_class_method_calls(cls)
        
        # 遍历所有接口的方法
        for interface in java_file.interfaces:
            self._register_interface_method_calls(interface)
        
        # 遍历所有枚举的方法
        for enum in java_file.enums:
            self._register_enum_method_calls(enum)
        
        # 遍历所record 的方
        for record in java_file.records:
            self._register_record_method_calls(record)
    
    def _register_class_method_calls(self, class_info: ClassInfo):
        """注册类中的方法调用"""
        class_name = class_info.type_name
        
        # 处理实例方法
        for method in class_info.methods:
            self._register_method_calls_in_method(method, class_name)
        
        # 处理构造器
        for constructor in class_info.constructors:
            self._register_method_calls_in_constructor(constructor, class_name)
    
    def _register_interface_method_calls(self, interface_info: InterfaceInfo):
        
        interface_name = interface_info.type_name
        
        # 处理接口方法
        for method in interface_info.methods:
            self._register_method_calls_in_method(method, interface_name)
    
    def _register_enum_method_calls(self, enum_info: EnumInfo):
        
        enum_name = enum_info.type_name
        
        # 处理方法
        for method in enum_info.methods:
            self._register_method_calls_in_method(method, enum_name)
        
        # 处理构造器
        for constructor in enum_info.constructors:
            self._register_method_calls_in_constructor(constructor, enum_name)
    
    def _register_record_method_calls(self, record_info: RecordInfo):
        
        record_name = record_info.type_name
        
        # 处理方法
        for method in record_info.methods:
            self._register_method_calls_in_method(method, record_name)
        
        # 处理构造器
        for constructor in record_info.constructors:
            self._register_method_calls_in_constructor(constructor, record_name)
    
    def _register_method_calls_in_method(self, method: MethodInfo, class_name: str):
        
        if not method.method_calls:
            return
        
        # 获取调用者方法的符号ID
        param_types = [p.type_name for p in method.parameters]
        class_symbol_id = SymbolIdGenerator.for_class(self.current_file, class_name)
        caller_symbol_id = SymbolIdGenerator.for_method(class_symbol_id, method.method_name, param_types)
        
        # 临时设置 current_class_stack 用于调用边解
        old_class_stack = self.current_class_stack.copy()
        self.current_class_stack = [class_symbol_id]
        
        # 处理每个方法调用
        for call_info in method.method_calls:
            self._create_call_edge(caller_symbol_id, call_info)
        
        # 恢复 current_class_stack
        self.current_class_stack = old_class_stack
    
    def _register_method_calls_in_constructor(self, constructor: ConstructorInfo, class_name: str):
        
        if not constructor.method_calls:
            return
        
        # 获取调用者构造器的符号ID
        param_types = [p.type_name for p in constructor.parameters]
        class_symbol_id = SymbolIdGenerator.for_class(self.current_file, class_name)
        caller_symbol_id = SymbolIdGenerator.for_constructor(class_symbol_id, param_types)
        
        # 临时设置 current_class_stack 用于调用边解
        old_class_stack = self.current_class_stack.copy()
        self.current_class_stack = [class_symbol_id]
        
        # 处理每个方法调用
        for call_info in constructor.method_calls:
            self._create_call_edge(caller_symbol_id, call_info)
        
        # 恢复 current_class_stack
        self.current_class_stack = old_class_stack
    
    def _create_call_edge(self, caller_symbol_id: str, call_info):
        """创建方法调用边"""
        # call_info 是字典,包含:method_name, receiver_expr, receiver_kind, is_static, argument_count, qualified_call
        method_name = call_info.get('method_name', '')
        receiver_expr = call_info.get('receiver_expr', '')
        receiver_kind = call_info.get('receiver_kind', '')
        is_static = call_info.get('is_static', False)
        
        # 尝试解析被调用方法的符号ID
        # 这是一个简化的实现,实际可能需要更复杂的解
        callee_symbol_id = self._resolve_method_call(method_name, receiver_expr, receiver_kind)
        
        # 避免创建自循环边(除非明确是递归调用
        # 自循环边通常表示方法调用自己,这在大多数情况下是不准确的
        if callee_symbol_id and callee_symbol_id != caller_symbol_id:
            from parser.common.symbol_table import CallEdge
            edge = CallEdge(
                source_symbol=caller_symbol_id,
                target_symbol=callee_symbol_id,
                edge_type="call",
                is_static_call=is_static,
                is_virtual_call=not is_static
            )
            self.symbol_table.add_call_edge(edge)
    
    def _resolve_method_call(self, method_name: str, receiver_expr: str, receiver_kind: str) -> Optional[str]:
        
        if not method_name:
            return None
        
        # 根据接收者类型来解析
        if receiver_kind == 'this':
            # this.method() - 在当前类中查
            if self.current_class_stack:
                current_class_id = self.current_class_stack[-1]
                current_class_symbol = self.symbol_table.lookup_by_id(current_class_id)
                if current_class_symbol:
                    # 查找该类中的方法
                    children = self.symbol_table.lookup_children(current_class_id)
                    for child in children:
                        if child.symbol_type == SymbolType.METHOD and child.name == method_name:
                            return child.symbol_id
        
        elif receiver_kind == 'type':
            # ClassName.method() - 静态方法调用或类方法调
            # 尝试在所有类中查
            for symbol_id, symbol in self.symbol_table.symbols.items():
                if symbol.symbol_type == SymbolType.CLASS and symbol.name == receiver_expr:
                    # 查找该类中的静态方
                    children = self.symbol_table.lookup_children(symbol_id)
                    for child in children:
                        if child.symbol_type == SymbolType.METHOD and child.name == method_name and child.is_static:
                            return child.symbol_id
            
            # 如果是标准库类型(如 Integer、String 等),返None
            return None
        
        elif receiver_kind == 'param' or receiver_kind == 'local' or receiver_kind == 'other':
            # 参数/局部变其他调用
            # 首先尝试在当前类中查
            if self.current_class_stack:
                current_class_id = self.current_class_stack[-1]
                current_class_symbol = self.symbol_table.lookup_by_id(current_class_id)
                if current_class_symbol:
                    # 查找该类中的方法
                    children = self.symbol_table.lookup_children(current_class_id)
                    for child in children:
                        if child.symbol_type == SymbolType.METHOD and child.name == method_name:
                            return child.symbol_id
                
                # 如果在当前类中没有找到,尝试根据字段类型查找
                if receiver_kind == 'other' and current_class_id in self.symbol_table.field_types:
                    field_type = self.symbol_table.field_types[current_class_id].get(receiver_expr)
                    if field_type:
                        # 根据字段类型查找方法
                        for symbol_id, symbol in self.symbol_table.symbols.items():
                            if symbol.symbol_type == SymbolType.CLASS and symbol.name == field_type:
                                # 在该类中查找方法
                                children = self.symbol_table.lookup_children(symbol_id)
                                for child in children:
                                    if child.symbol_type == SymbolType.METHOD and child.name == method_name:
                                        return child.symbol_id
            
            # 如果都没有找到,返回 None
            return None
        
        elif receiver_kind == 'field':
            # 字段调用 - 例如: this.name.compareTo()
            # 虽然我们知道 name 是字段,但无法确定其具体类型
            # 返回 None 以避免创建错误的调用
            return None
        
        # 如果是未知的接收者类型,也返None
        return None
    
    # ========== 字段访问关系==========
    
    def register_all_field_accesses(self, java_file: JavaFileStructure):
        
        # 遍历所有类的方
        for cls in java_file.classes:
            self._register_class_field_accesses(cls)
        
        # 遍历所有接口的方法
        for interface in java_file.interfaces:
            self._register_interface_field_accesses(interface)
        
        # 遍历所有枚举的方法
        for enum in java_file.enums:
            self._register_enum_field_accesses(enum)
        
        # 遍历所record 的方
        for record in java_file.records:
            self._register_record_field_accesses(record)
    
    def _register_class_field_accesses(self, class_info: ClassInfo):
        """注册类中的字段访问"""
        class_name = class_info.type_name
        
        # 处理实例方法
        for method in class_info.methods:
            self._register_field_accesses_in_method(method, class_name)
        
        # 处理静态方
        for method in class_info.static_methods:
            self._register_field_accesses_in_method(method, class_name)
        
        # 处理构造器
        for constructor in class_info.constructors:
            self._register_field_accesses_in_constructor(constructor, class_name)
    
    def _register_interface_field_accesses(self, interface_info: InterfaceInfo):
        
        interface_name = interface_info.type_name
        
        # 处理接口方法
        for method in interface_info.methods:
            self._register_field_accesses_in_method(method, interface_name)
    
    def _register_enum_field_accesses(self, enum_info: EnumInfo):
        
        enum_name = enum_info.type_name
        
        # 处理方法
        for method in enum_info.methods:
            self._register_field_accesses_in_method(method, enum_name)
        
        # 处理构造器
        for constructor in enum_info.constructors:
            self._register_field_accesses_in_constructor(constructor, enum_name)
    
    def _register_record_field_accesses(self, record_info: RecordInfo):
        
        record_name = record_info.type_name
        
        # 处理方法
        for method in record_info.methods:
            self._register_field_accesses_in_method(method, record_name)
        
        # 处理构造器
        for constructor in record_info.constructors:
            self._register_field_accesses_in_constructor(constructor, record_name)
    
    def _register_field_accesses_in_method(self, method: MethodInfo, class_name: str):
        
        if not method.field_accesses:
            return
        
        # 获取访问者方法的符号ID
        param_types = [p.type_name for p in method.parameters]
        class_symbol_id = SymbolIdGenerator.for_class(self.current_file, class_name)
        accessor_symbol_id = SymbolIdGenerator.for_method(class_symbol_id, method.method_name, param_types)
        
        # 处理每个字段访问
        for access_info in method.field_accesses:
            self._create_access_edge(accessor_symbol_id, access_info)
    
    def _register_field_accesses_in_constructor(self, constructor: ConstructorInfo, class_name: str):
        
        if not constructor.field_accesses:
            return
        
        # 获取访问者构造器的符号ID
        param_types = [p.type_name for p in constructor.parameters]
        class_symbol_id = SymbolIdGenerator.for_class(self.current_file, class_name)
        accessor_symbol_id = SymbolIdGenerator.for_constructor(class_symbol_id, param_types)
        
        # 处理每个字段访问
        for access_info in constructor.field_accesses:
            self._create_access_edge(accessor_symbol_id, access_info)
    
    def _create_access_edge(self, accessor_symbol_id: str, access_info):
        """创建字段访问"""
        # access_info FieldAccessInfo 对象
        field_name = access_info.field_name if hasattr(access_info, 'field_name') else ''
        operation = access_info.operation if hasattr(access_info, 'operation') else 'read'
        
        # 尝试解析被访问字段的符号ID
        field_symbol_id = self._resolve_field_access(field_name)
        
        if field_symbol_id:
            from parser.common.symbol_table import AccessEdge
            is_write = operation in ['write', 'read_write']
            is_read = operation in ['read', 'read_write']
            
            edge = AccessEdge(
                source_symbol=accessor_symbol_id,
                target_symbol=field_symbol_id,
                edge_type="access",
                is_write=is_write,
                is_read=is_read
            )
            self.symbol_table.add_access_edge(edge)
    
    def _resolve_field_access(self, field_name: str) -> Optional[str]:
        
        if not field_name:
            return None
        
        # 在当前类中查找字
        if self.current_class_stack:
            current_class_id = self.current_class_stack[-1]
            children = self.symbol_table.lookup_children(current_class_id)
            for child in children:
                if child.symbol_type == SymbolType.FIELD and child.name == field_name:
                    return child.symbol_id
        
        # 如果在当前类中没找到,尝试在所有类中查
        for symbol_id, symbol in self.symbol_table.symbols.items():
            if symbol.symbol_type == SymbolType.FIELD and symbol.name == field_name:
                return symbol_id
        
        return None
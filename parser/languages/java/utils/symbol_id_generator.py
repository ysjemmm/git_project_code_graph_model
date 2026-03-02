"""Symbol ID Generator for Java AST nodes"""


class SymbolIdGenerator:
    """Generates unique symbol IDs for Java AST nodes"""
    
    @staticmethod
    def generate_class_symbol_id(file_path: str, class_name: str, package_name: str = "") -> str:
        """Generate symbol ID for a class"""
        if package_name:
            return f"{package_name}.{class_name}#{file_path}"
        return f"{class_name}#{file_path}"
    
    @staticmethod
    def generate_interface_symbol_id(file_path: str, interface_name: str, package_name: str = "") -> str:
        """Generate symbol ID for an interface"""
        if package_name:
            return f"{package_name}.{interface_name}#{file_path}"
        return f"{interface_name}#{file_path}"
    
    @staticmethod
    def generate_enum_symbol_id(file_path: str, enum_name: str, package_name: str = "") -> str:
        """Generate symbol ID for an enum"""
        if package_name:
            return f"{package_name}.{enum_name}#{file_path}"
        return f"{enum_name}#{file_path}"
    
    @staticmethod
    def generate_annotation_symbol_id(file_path: str, annotation_name: str, package_name: str = "") -> str:
        """Generate symbol ID for an annotation"""
        if package_name:
            return f"{package_name}.{annotation_name}#{file_path}"
        return f"{annotation_name}#{file_path}"
    
    @staticmethod
    def generate_record_symbol_id(file_path: str, record_name: str, package_name: str = "") -> str:
        """Generate symbol ID for a record"""
        if package_name:
            return f"{package_name}.{record_name}#{file_path}"
        return f"{record_name}#{file_path}"
    
    @staticmethod
    def generate_method_symbol_id(class_symbol_id: str, method_name: str, param_types: list = None) -> str:
        """Generate symbol ID for a method"""
        if param_types:
            param_str = ",".join(param_types)
            return f"{class_symbol_id}#{method_name}({param_str})"
        return f"{class_symbol_id}#{method_name}()"
    
    @staticmethod
    def generate_field_symbol_id(class_symbol_id: str, field_name: str) -> str:
        """Generate symbol ID for a field"""
        return f"{class_symbol_id}#{field_name}"
    
    @staticmethod
    def generate_constructor_symbol_id(class_symbol_id: str, param_types: list = None) -> str:
        """Generate symbol ID for a constructor"""
        if param_types:
            param_str = ",".join(param_types)
            return f"{class_symbol_id}#<init>({param_str})"
        return f"{class_symbol_id}#<init>()"
    
    @staticmethod
    def generate_parameter_symbol_id(method_symbol_id: str, param_name: str, param_index: int) -> str:
        """Generate symbol ID for a parameter"""
        return f"{method_symbol_id}@{param_index}:{param_name}"
    
    @staticmethod
    def generate_enum_constant_symbol_id(enum_symbol_id: str, constant_name: str) -> str:
        """Generate symbol ID for an enum constant"""
        return f"{enum_symbol_id}#{constant_name}"
    
    @staticmethod
    def generate_annotation_element_symbol_id(annotation_symbol_id: str, element_name: str) -> str:
        """Generate symbol ID for an annotation element"""
        return f"{annotation_symbol_id}#{element_name}"
    
    @staticmethod
    def generate_record_component_symbol_id(record_symbol_id: str, component_name: str) -> str:
        """Generate symbol ID for a record component"""
        return f"{record_symbol_id}#{component_name}"

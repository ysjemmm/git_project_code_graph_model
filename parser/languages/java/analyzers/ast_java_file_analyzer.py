"""Java file analyzer for AST"""
from pathlib import Path
from typing import List, Dict

from loraxmod import ExtractedNode

from parser.languages.java import JavaAstNodeType
from parser.languages.java.core.ast_node_types import JavaFileStructure, PackageInfo, ImportInfo, ClassInfo, EnumInfo, \
    InterfaceInfo, AnnotationTypeInfo, RecordInfo
from parser.languages.java.core.base_analyzer import BaseAnalyzer
from parser.languages.java.utils.analyzer_cache import AnalyzerCache
from parser.languages.java.utils.analyzer_context import AnalyzerContext


class JavaFileAnalyzer(BaseAnalyzer):
    """Analyzes Java source files"""
    
    def __init__(self, context: AnalyzerContext, symbol_table=None, auto_resolve_types=False, 
                 file_path: str = None, lazy_parse: bool = True):
        super().__init__()
        self.context = context
        self.symbol_table = symbol_table
        self.auto_resolve_types = auto_resolve_types
        self.current_file = file_path or ""
        self.relative_file = str(Path(self.current_file).relative_to(context.project_path))
        self.type2node: Dict[JavaAstNodeType, List[ExtractedNode]] = {}

        self.ast_node = None
        self.java_file_structure = None
        if not lazy_parse:
            self._ast_node()

    def _ast_node(self):
        if self.ast_node is None:
            if self.context.parser is None:
                return None
            try:
                parsed = self.context.parser.parse_file(self.current_file)
                if hasattr(self.context.parser, 'extract_all'):
                    self.ast_node = self.context.parser.extract_all(parsed, True)
                else:
                    self.ast_node = parsed.root_node
            except Exception:
                return None
        
        if self.ast_node is not None and len(self.type2node) == 0:
            if hasattr(self.ast_node, 'children'):
                for n in self.ast_node.children:
                    node_type_str = getattr(n, 'node_type', getattr(n, 'type', ''))
                    enum_type = JavaAstNodeType.from_value(node_type_str)
                    if enum_type:
                        self.type2node.setdefault(enum_type, []).append(n)
        return self.ast_node

    def analyze_file(self, file_path: str = None) -> JavaFileStructure:
        """Analyze a Java file"""
        if file_path is not None:
            self.current_file = file_path
        java_file_structure = JavaFileStructure(file_path=self.current_file)

        self.java_file_structure = java_file_structure
        self.java_file_structure.file_name = Path(self.current_file).name
        self.java_file_structure.relative_path = self.relative_file if self.relative_file else ""
        self._ast_node()

        self.java_file_structure.set_pos_from_node(self.ast_node)

        # Generate symbol_id for the Java file
        from parser.languages.java.utils.analyzer_helper import AnalyzerHelper
        self.java_file_structure.symbol_id = AnalyzerHelper.generate_symbol_id_for_file(
            self.context.root_project_symbol_id, self.relative_file
        )

        self.java_file_structure.package_info = self._extract_package_info()
        self.java_file_structure.import_details = self._extract_import_infos()
        self.java_file_structure.comments = self._extract_comments()

        self.java_file_structure.classes = self._extract_class_info(self.java_file_structure.symbol_id)
        self.java_file_structure.enums = self._extract_enum_info(self.java_file_structure.symbol_id)
        self.java_file_structure.interfaces = self._extract_interface_info(self.java_file_structure.symbol_id)
        self.java_file_structure.annotations = self._extract_annotation_info(self.java_file_structure.symbol_id)
        self.java_file_structure.records = self._extract_record_info(self.java_file_structure.symbol_id)

        return java_file_structure

    def _extract_package_info(self) -> PackageInfo:
        """Extract package info"""
        analyzer = AnalyzerCache.get_package_analyzer(self.context.project_name)
        node = self.type2node.get(JavaAstNodeType.PACKAGE_DECLARATION, [None])[0]
        if node is None:
            return PackageInfo()
        return analyzer.handle_package_declaration(node)

    def _extract_import_infos(self) -> List[ImportInfo]:
        """Extract import info"""
        analyzer = AnalyzerCache.get_import_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.IMPORT_DECLARATION, [])

        imps = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_import_declaration(n)
                if result is not None:
                    imps.append(result)
        return imps

    def _extract_comments(self) -> list:
        """Extract comments"""
        analyzer = AnalyzerCache.get_comment_analyzer(self.context.project_name)
        
        # 获取注释节点
        line_comments = self.type2node.get(JavaAstNodeType.LINE_COMMENT, []) if hasattr(JavaAstNodeType, 'LINE_COMMENT') else []
        block_comments = self.type2node.get(JavaAstNodeType.BLOCK_COMMENT, []) if hasattr(JavaAstNodeType, 'BLOCK_COMMENT') else []
        nodes = line_comments + block_comments

        comments = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_comment(n)
                if result is not None:
                    comments.append(result)
        return comments

    def _extract_class_info(self, parent_symbol_id: str) -> list[ClassInfo]:
        """Extract class info"""
        analyzer = AnalyzerCache.get_class_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.CLASS_DECLARATION, [])

        objs = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_class_declaration(n, self.context, parent_symbol_id)
                if result is not None:
                    objs.append(result)
        return objs

    def _extract_enum_info(self, parent_symbol_id: str) -> list[EnumInfo]:
        """Extract enum info"""
        analyzer = AnalyzerCache.get_enum_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.ENUM_DECLARATION, [])

        objs = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_enum_declaration(n, self.context, parent_symbol_id)
                if result is not None:
                    objs.append(result)
        return objs

    def _extract_interface_info(self, parent_symbol_id: str) -> list[InterfaceInfo]:
        """Extract interface info"""
        analyzer = AnalyzerCache.get_interface_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.INTERFACE_DECLARATION, [])

        objs = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_interface_declaration(n, self.context, parent_symbol_id)
                if result is not None:
                    objs.append(result)
        return objs

    def _extract_annotation_info(self, parent_symbol_id: str) -> list[AnnotationTypeInfo]:
        """Extract annotation info"""
        analyzer = AnalyzerCache.get_annotation_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.ANNOTATION_TYPE_DECLARATION, [])

        objs = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_annotation_declaration(n, self.context, parent_symbol_id)
                if result is not None:
                    objs.append(result)
        return objs

    def _extract_record_info(self, parent_symbol_id: str) -> list[RecordInfo]:
        """Extract record info"""
        analyzer = AnalyzerCache.get_record_analyzer(self.context.project_name)
        nodes = self.type2node.get(JavaAstNodeType.RECORD_DECLARATION, [])

        objs = []
        for n in nodes:
            if n is not None:
                result = analyzer.handle_record_declaration(n, self.context, parent_symbol_id)
                if result is not None:
                    objs.append(result)
        return objs

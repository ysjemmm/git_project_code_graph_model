"""Analyzer cache for Java AST analysis with project-level isolation"""

from threading import Lock
from typing import Dict, Optional, TYPE_CHECKING

if TYPE_CHECKING:
    from parser.languages.java.analyzers.class_analyzer import ClassAnalyzer
    from parser.languages.java.analyzers.class_body_analyzer import ClassBodyAnalyzer
    from parser.languages.java.analyzers.import_analyzer import ImportAnalyzer
    from parser.languages.java.analyzers.interface_analyzer import InterfaceAnalyzer
    from parser.languages.java.analyzers.package_analyzer import PackageAnalyzer
    from parser.languages.java.analyzers.record_analyzer import RecordAnalyzer
    from parser.languages.java.analyzers.comment_analyzer import CommentAnalyzer
    from parser.languages.java.analyzers.annotation_analyzer import AnnotationAnalyzer
    from parser.languages.java.analyzers.constructor_analyzer import ConstructorAnalyzer
    from parser.languages.java.analyzers.field_analyzer import FieldAnalyzer
    from parser.languages.java.analyzers.method_analyzer import MethodAnalyzer
    from parser.languages.java.analyzers.interface_body_analyzer import InterfaceBodyAnalyzer
    from parser.languages.java.analyzers.annotation_body_analyzer import AnnotationBodyAnalyzer
    from parser.languages.java.analyzers.record_body_analyzer import RecordBodyAnalyzer


class AnalyzerCache:
    """Project-level cache for analyzer instances to prevent blocking across multiple projects"""
    
    _project_caches: Dict[str, Dict[str, object]] = {}
    _lock = Lock()
    _current_project: Optional[str] = None
    
    @classmethod
    def set_project(cls, project_id: str) -> None:
        """Set the current project context for cache operations.
        
        Args:
            project_id: Unique identifier for the project (e.g., project path, name, or custom key)
        """
        cls._current_project = project_id
        if project_id not in cls._project_caches:
            with cls._lock:
                if project_id not in cls._project_caches:
                    cls._project_caches[project_id] = {}
                    cls._init_analyzers_for_project(project_id)
    
    @classmethod
    def _init_analyzers_for_project(cls, project_id: str) -> None:
        """Initialize analyzer instances for a specific project."""
        from parser.languages.java.analyzers.class_analyzer import ClassAnalyzer
        from parser.languages.java.analyzers.class_body_analyzer import ClassBodyAnalyzer
        from parser.languages.java.analyzers.import_analyzer import ImportAnalyzer
        from parser.languages.java.analyzers.interface_analyzer import InterfaceAnalyzer
        from parser.languages.java.analyzers.package_analyzer import PackageAnalyzer
        from parser.languages.java.analyzers.record_analyzer import RecordAnalyzer
        from parser.languages.java.analyzers.comment_analyzer import CommentAnalyzer
        from parser.languages.java.analyzers.annotation_analyzer import AnnotationAnalyzer
        from parser.languages.java.analyzers.constructor_analyzer import ConstructorAnalyzer
        from parser.languages.java.analyzers.field_analyzer import FieldAnalyzer
        from parser.languages.java.analyzers.method_analyzer import MethodAnalyzer
        from parser.languages.java.analyzers.code_block_analyzer import CodeBlockAnalyzer
        from parser.languages.java.analyzers.enum_analyzer import EnumAnalyzer
        from parser.languages.java.analyzers.enum_body_analyzer import EnumBodyAnalyzer
        from parser.languages.java.analyzers.interface_body_analyzer import InterfaceBodyAnalyzer
        from parser.languages.java.analyzers.annotation_body_analyzer import AnnotationBodyAnalyzer
        from parser.languages.java.analyzers.record_body_analyzer import RecordBodyAnalyzer

        cls._project_caches[project_id] = {
            'package': PackageAnalyzer(),
            'import': ImportAnalyzer(),
            'comment': CommentAnalyzer(),
            'class': ClassAnalyzer(),
            'enum': EnumAnalyzer(),
            'interface': InterfaceAnalyzer(),
            'annotation': AnnotationAnalyzer(),
            'record': RecordAnalyzer(),
            'code_block': CodeBlockAnalyzer(),
            'method': MethodAnalyzer(),
            'field': FieldAnalyzer(),
            'constructor': ConstructorAnalyzer(),
            'class_body': ClassBodyAnalyzer(),
            'enum_body': EnumBodyAnalyzer(),
            'interface_body': InterfaceBodyAnalyzer(),
            'annotation_body': AnnotationBodyAnalyzer(),
            'record_body': RecordBodyAnalyzer(),
        }
    
    @classmethod
    def _get_cache(cls, project_id: Optional[str] = None) -> Dict[str, object]:
        """Get the cache for a project. Uses current project if not specified."""
        pid = project_id or cls._current_project
        if pid is None:
            raise RuntimeError("No project context set. Provide project_id or call set_project() first.")
        if pid not in cls._project_caches:
            cls.set_project(pid)
        return cls._project_caches[pid]

    @classmethod
    def get_package_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['package']

    @classmethod
    def get_import_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['import']

    @classmethod
    def get_comment_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['comment']

    @classmethod
    def get_annotation_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['annotation']

    @classmethod
    def get_class_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['class']

    @classmethod
    def get_enum_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['enum']

    @classmethod
    def get_interface_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['interface']

    @classmethod
    def get_record_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['record']

    @classmethod
    def get_code_block_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['code_block']
    
    @classmethod
    def get_method_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['method']
    
    @classmethod
    def get_field_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['field']
    
    @classmethod
    def get_constructor_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['constructor']

    @classmethod
    def get_class_body_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['class_body']

    @classmethod
    def get_enum_body_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['enum_body']

    @classmethod
    def get_interface_body_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['interface_body']

    @classmethod
    def get_annotation_body_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['annotation_body']

    @classmethod
    def get_record_body_analyzer(cls, project_id: Optional[str] = None):
        return cls._get_cache(project_id)['record_body']
    
    @classmethod
    def clear_project(cls, project_id: str) -> None:
        """Clear cache for a specific project.
        
        Args:
            project_id: The project identifier to clear
        """
        with cls._lock:
            if project_id in cls._project_caches:
                cls._project_caches[project_id].clear()
                del cls._project_caches[project_id]
    
    @classmethod
    def clear_all(cls) -> None:
        """Clear all project caches."""
        with cls._lock:
            for cache in cls._project_caches.values():
                cache.clear()
            cls._project_caches.clear()
            cls._current_project = None

"""
Neo4j 存储模块（语言无关）。

兼容说明：
- 历史上 `Neo4jExporterAST` 也在这里导出，但它属于 Java 侧兼容外观类
- 为避免 storage 层在 import 时引入语言实现，这里对 `Neo4jExporterAST` 采用懒加载导出
"""

from __future__ import annotations

from .connector import Neo4jConnector
from .external_linker import ExternalClassLinker
from .queries import Neo4jQueries, QueryBuilder

__all__ = [
    "Neo4jConnector",
    "Neo4jExporterAST",
    "ExternalClassLinker",
    "Neo4jQueries",
    "QueryBuilder",
]


def __getattr__(name: str):
    # PEP 562: module attribute access hook (py>=3.7)
    if name == "Neo4jExporterAST":
        from .exporter import Neo4jExporterAST as _Neo4jExporterAST

        return _Neo4jExporterAST
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")


def __dir__():
    return sorted(set(globals().keys()) | {"Neo4jExporterAST"})

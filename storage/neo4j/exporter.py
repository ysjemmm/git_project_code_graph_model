"""
兼容模块：保留历史导入路径 `storage.neo4j.exporter.Neo4jExporterAST`。

注意：`Neo4jExporterAST` 的实际实现已经迁移到 `parser.languages.java.neo4j_exporter_ast`（Java 专用），以避免存储层携带语言逻辑。
"""

from parser.languages.java.neo4j_exporter_ast import Neo4jExporterAST

__all__ = ["Neo4jExporterAST"]
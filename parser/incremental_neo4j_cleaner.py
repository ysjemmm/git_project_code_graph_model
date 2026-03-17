#!/usr/bin/env python3

from pathlib import Path
from typing import List, Dict

from parser.cgm_exporter import CGMExporter

from parser.incremental_analyzer import IncrementalAnalyzer
from storage.neo4j.connector import Neo4jConnector
from tools.constants import PROJECT_ROOT_PATH


class IncrementalNeo4jCleaner:
    """增量 Neo4j 清理器"""
    
    def __init__(self, neo4j_connector: Neo4jConnector):
        
        self.connector = neo4j_connector
    
    def cleanup_deleted_files(self, deleted_files: List[str]) -> Dict:
        
        if not deleted_files:
            return {
                'deleted_nodes': 0,
                'deleted_relationships': 0,
                'deleted_files': [],
                'affected_symbols': []
            }
        
        total_deleted_nodes = 0
        total_deleted_relationships = 0
        affected_symbols = []
        
        for file_path in deleted_files:
            print(f"[清理] 删除文件相关的节 {file_path}")
            
            # 1. 查找该文件中的所有符
            symbols = self._find_symbols_by_file(file_path)
            affected_symbols.extend(symbols)
            
            # 2. 删除该文件中的所有节点及其关
            deleted_nodes, deleted_rels = self._delete_symbols_and_relationships(symbols)
            total_deleted_nodes += deleted_nodes
            total_deleted_relationships += deleted_rels
            
            # 3. 删除 JAVAFILE 节点
            deleted_nodes, deleted_rels = self._delete_javafile_node(file_path)
            total_deleted_nodes += deleted_nodes
            total_deleted_relationships += deleted_rels
        
        return {
            'deleted_nodes': total_deleted_nodes,
            'deleted_relationships': total_deleted_relationships,
            'deleted_files': deleted_files,
            'affected_symbols': affected_symbols
        }
    
    def cleanup_modified_files(self, modified_files: List[str]) -> Dict:
        
        if not modified_files:
            return {
                'deleted_nodes': 0,
                'deleted_relationships': 0,
                'modified_files': [],
                'affected_symbols': []
            }
        
        total_deleted_nodes = 0
        total_deleted_relationships = 0
        affected_symbols = []
        
        for file_path in modified_files:
            print(f"[清理] 删除文件的旧数据: {file_path}")
            
            # 1. 查找该文件中的所有符
            symbols = self._find_symbols_by_file(file_path)
            affected_symbols.extend(symbols)
            
            # 2. 删除该文件中的所有节点及其关
            deleted_nodes, deleted_rels = self._delete_symbols_and_relationships(symbols)
            total_deleted_nodes += deleted_nodes
            total_deleted_relationships += deleted_rels
        
        return {
            'deleted_nodes': total_deleted_nodes,
            'deleted_relationships': total_deleted_relationships,
            'modified_files': modified_files,
            'affected_symbols': affected_symbols
        }
    
    def _find_symbols_by_file(self, file_path: str) -> List[str]:
        """查找文件中的所有符"""
        result = self.connector.execute_query(
            """
            MATCH (n)
            WHERE n.file_path = $file_path
            RETURN n.symbol_id as symbol_id
            """,
            {"file_path": file_path},
        )
        
        return [record['symbol_id'] for record in result if record['symbol_id']]
    
    def _delete_symbols_and_relationships(self, symbol_ids: List[str]) -> tuple:
        
        if not symbol_ids:
            return 0, 0
        
        # 批量删除关系（UNWIND）
        rel_result = self.connector.execute_write_query(
            """
            UNWIND $ids AS id
            MATCH (n {symbol_id: id})-[r]-()
            DELETE r
            RETURN count(r) as count
            """,
            {"ids": symbol_ids},
        )
        deleted_relationships = rel_result[0]["count"] if rel_result else 0

        # 批量删除节点（UNWIND）
        node_result = self.connector.execute_write_query(
            """
            UNWIND $ids AS id
            MATCH (n {symbol_id: id})
            DELETE n
            RETURN count(n) as count
            """,
            {"ids": symbol_ids},
        )
        deleted_nodes = node_result[0]["count"] if node_result else 0

        return deleted_nodes, deleted_relationships
    
    def _delete_javafile_node(self, file_path: str) -> tuple:
        
        java_file_id = f"file#{file_path}"
        
        # 删除JAVAFILE 节点相关的所有关
        result = self.connector.execute_write_query(
            """
            MATCH (n {symbol_id: $sid})-[r]-()
            DELETE r
            RETURN count(r) as count
            """,
            {"sid": java_file_id},
        )
        
        deleted_relationships = result[0]['count'] if result else 0
        
        # 删除 JAVAFILE 节点
        result = self.connector.execute_write_query(
            """
            MATCH (n {symbol_id: $sid})
            DELETE n
            RETURN count(n) as count
            """,
            {"sid": java_file_id},
        )
        
        deleted_nodes = result[0]['count'] if result else 0
        
        return deleted_nodes, deleted_relationships
    
    def cleanup_orphaned_relationships(self) -> Dict:
        
        print("[清理] 删除孤立的关..")
        
        # 查找指向不存在节点的关系
        result = self.connector.execute_write_query(
            """
            // Neo4j 中关系必须有起点/终点，理论上不会存在“孤立关系”。
            // 这里保留兼容清理逻辑，但用 Neo4j 5 更推荐的 OPTIONAL MATCH + IS NULL 写法。
            OPTIONAL MATCH ()-[r]->(b)
            WITH r, b
            WHERE r IS NOT NULL AND b IS NULL
            DELETE r
            RETURN count(r) as count
            """
        )
        
        deleted_count = result[0]['count'] if result else 0
        
        # 查找来自不存在节点的关系
        result = self.connector.execute_write_query(
            """
            OPTIONAL MATCH (a)-[r]->()
            WITH a, r
            WHERE r IS NOT NULL AND a IS NULL
            DELETE r
            RETURN count(r) as count
            """
        )
        
        deleted_count += result[0]['count'] if result else 0
        
        return {
            'deleted_relationships': deleted_count
        }
    
    def get_database_statistics(self) -> Dict:
        """获取数据库统计信息"""
        # 获取节点统计
        result = self.connector.execute_query('''
        MATCH (n)
        RETURN labels(n) as labels, count(*) as count
        ORDER BY count DESC
        ''')
        
        node_stats = {}
        for record in result:
            label = record['labels'][0] if record['labels'] else 'Unknown'
            node_stats[label] = record['count']
        
        # 获取关系统计
        result = self.connector.execute_query('''
        MATCH ()-[r]->()
        RETURN type(r) as rel_type, count(*) as count
        ORDER BY count DESC
        ''')
        
        rel_stats = {}
        for record in result:
            rel_stats[record['rel_type']] = record['count']
        
        # 获取总数
        total_nodes = sum(node_stats.values())
        total_relationships = sum(rel_stats.values())
        
        return {
            'total_nodes': total_nodes,
            'total_relationships': total_relationships,
            'node_stats': node_stats,
            'rel_stats': rel_stats
        }

class IncrementalExportManager:
    """
    增量导出：只对变更/新增文件重新分析并写回 Neo4j。
    要求传入 project_key，写入的节点统一带上该属性，与主流程一致。
    cache_dir 默认使用项目根目录下的 .cache/incremental（与 core/importer 的 .cache 约定一致）。
    """

    def __init__(
        self,
        neo4j_connector: Neo4jConnector,
        code_graph_builder,
        project_key: str,
        cache_dir: str = None,
    ):
        self.connector = neo4j_connector
        self.builder = code_graph_builder
        self.project_key = project_key
        # 默认使用项目根目录下的 .cache/incremental，与 core/importer 的 .cache 约定一致
        if cache_dir is None:
            cache_dir = str(PROJECT_ROOT_PATH / ".cache" / "incremental")
        self.incremental_analyzer = IncrementalAnalyzer(cache_dir)
        self.neo4j_cleaner = IncrementalNeo4jCleaner(neo4j_connector)
    
    def sync_with_git_changes(self, directory_path: str) -> Dict:
        
        try:
            print("=" * 80)
            print("同步 Git 变化Neo4j")
            print("=" * 80)
            
            # 1. 检测文件变
            print("\n[步骤 1] 检测文件变..")
            changes = self.incremental_analyzer.analyze_changes(directory_path)
            
            print(f"  修改的文 {len(changes['modified'])}")
            print(f"  新增的文 {len(changes['new'])}")
            print(f"  删除的文 {len(changes['deleted'])}")
            print(f"  未变化的文件: {len(changes['unchanged'])}")
            
            # 2. 清理已删除文件的数据
            print("\n[步骤 2] 清理已删除文件的数据...")
            cleanup_result = self.neo4j_cleaner.cleanup_deleted_files(changes['deleted'])
            print(f"  删除的节 {cleanup_result['deleted_nodes']}")
            print(f"  删除的关 {cleanup_result['deleted_relationships']}")
            
            # 3. 清理已修改文件的旧数
            print("\n[步骤 3] 清理已修改文件的旧数..")
            cleanup_result_modified = self.neo4j_cleaner.cleanup_modified_files(changes['modified'])
            print(f"  删除的节 {cleanup_result_modified['deleted_nodes']}")
            print(f"  删除的关 {cleanup_result_modified['deleted_relationships']}")
            
            # 4. 重新分析修改和新增的文件
            print("\n[步骤 4] 重新分析修改和新增的文件...")
            files_to_analyze = changes['modified'] + changes['new']
            if files_to_analyze:
                self.builder.build_from_files(files_to_analyze)
                print(f"  分析{len(files_to_analyze)} 个文件")
            
            # 5. 导出新数据到 Neo4j
            print("\n[步骤 5] 导出新数据到 Neo4j...")
            exporter = CGMExporter(
                self.connector.uri,
                self.connector.user,
                self.connector.password,
                self.connector.database
            )
            exporter.connector = self.connector  # 使用现有连接
            
            # 只导出修改和新增的文件中的符号（统一注入 project_key）
            export_result = self._export_incremental_symbols(files_to_analyze, self.project_key)
            print(f"  导出的节 {export_result['nodes_count']}")
            print(f"  导出的关 {export_result['relationships_count']}")
            
            # 6. 获取最终统
            print("\n[步骤 6] 获取最终统..")
            statistics = self.neo4j_cleaner.get_database_statistics()
            print(f"  总节点数: {statistics['total_nodes']}")
            print(f"  总关系数: {statistics['total_relationships']}")
            
            return {
                'status': 'success',
                'changes': changes,
                'cleanup_result': {
                    'deleted': cleanup_result,
                    'modified': cleanup_result_modified
                },
                'export_result': export_result,
                'statistics': statistics
            }
        
        except Exception as e:
            print(f"[错误] 同步失败: {e}")
            return {
                'status': 'error',
                'error': str(e)
            }
    
    def _export_incremental_symbols(self, file_paths: List[str], project_key: str) -> Dict:
        """
        导出增量符号节点与关系；每个节点统一写入 project_key，与主流程一致。
        """
        from parser.common.symbol_table import SymbolType

        nodes_count = 0
        relationships_count = 0

        def _batch_create_nodes(label: str, nodes: List[dict], batch_size: int = 5000) -> int:
            if not nodes:
                return 0
            for n in nodes:
                if isinstance(n, dict) and "project_key" not in n:
                    n["project_key"] = project_key
            created = 0
            query = f"""
            UNWIND $nodes AS node
            MERGE (n:{label} {{symbol_id: node.symbol_id}})
            SET n += node
            RETURN count(n) as created_count
            """
            for i in range(0, len(nodes), batch_size):
                batch = nodes[i:i + batch_size]
                self.connector.execute_write_query(query, {"nodes": batch})
                created += len(batch)
            return created

        def _batch_create_relationships(rel_type: str, rels: List[dict], batch_size: int = 5000) -> int:
            if not rels:
                return 0
            created = 0
            query = f"""
            UNWIND $rels AS rel
            MATCH (a {{symbol_id: rel.s}})
            MATCH (b {{symbol_id: rel.t}})
            MERGE (a)-[:{rel_type}]->(b)
            RETURN count(*) as created_count
            """
            for i in range(0, len(rels), batch_size):
                batch = rels[i:i + batch_size]
                self.connector.execute_write_query(query, {"rels": batch})
                created += len(batch)
            return created
        
        # 1. 导出符号节点
        print("  [导出] 创建符号节点...")
        nodes_by_label: Dict[str, List[dict]] = {}
        for symbol_id, symbol in self.builder.symbol_table.symbols.items():
            if symbol.file_path not in file_paths:
                continue
            
            # 跳过 PACKAGE IMPORT 节点
            if symbol.symbol_type in (SymbolType.PACKAGE, SymbolType.IMPORT):
                continue
            
            label = symbol.symbol_type.value.upper()
            props = {
                "symbol_id": symbol_id,
                "name": symbol.name,
                "qualified_name": symbol.qualified_name,
                "file_path": symbol.file_path,
                "start_line": symbol.location.start_pos[0],
                "start_col": symbol.location.start_pos[1],
                "end_line": symbol.location.end_pos[0],
                "end_col": symbol.location.end_pos[1],
            }
            
            # 添加类型特定的属
            type_declaration_types = {SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.RECORD, SymbolType.ENUM, SymbolType.ANNOTATION}
            
            if symbol.symbol_type in type_declaration_types:
                raw_metadata = symbol.metadata.get('raw_metadata', '')
                if raw_metadata:
                    props["raw_metadata"] = raw_metadata
            elif symbol.symbol_type in (SymbolType.METHOD, SymbolType.CONSTRUCTOR):
                raw_method = symbol.metadata.get('raw_method', '')
                if raw_method:
                    props["raw_method"] = raw_method
                props["method_type"] = "internal"
            elif symbol.symbol_type == SymbolType.FIELD:
                raw_field = symbol.metadata.get('raw_field', '')
                if raw_field:
                    props["raw_field"] = raw_field
                if symbol.type_name:
                    props["type_name"] = symbol.type_name
                props["initialization_status"] = symbol.initialization_status
                if symbol.initialization_methods:
                    props["initialization_methods"] = ",".join(symbol.initialization_methods)
                if symbol.default_value:
                    props["default_value"] = symbol.default_value
            elif symbol.symbol_type == SymbolType.PARAMETER:
                if symbol.type_name:
                    props["type_name"] = symbol.type_name
            nodes_by_label.setdefault(label, []).append(props)

        for label, nodes in nodes_by_label.items():
            try:
                nodes_count += _batch_create_nodes(label, nodes)
            except Exception as e:
                print(f"    警告: 批量创建节点失败 {label}: {e}")
        
        # 2. 导出 JAVAFILE 节点
        print("  [导出] 创建 JAVAFILE 节点...")
        javafile_nodes: List[dict] = []
        for file_path in file_paths:
            java_file_id = f"file#{file_path}"
            
            # 收集文件信息
            package_name = ""
            imports = []
            types = []
            
            for symbol_id, symbol in self.builder.symbol_table.symbols.items():
                if symbol.file_path != file_path:
                    continue
                
                if symbol.symbol_type == SymbolType.PACKAGE:
                    package_name = symbol.name
                elif symbol.symbol_type == SymbolType.IMPORT:
                    imports.append(symbol.name)
                elif symbol.symbol_type in {SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.RECORD, SymbolType.ENUM, SymbolType.ANNOTATION}:
                    types.append(symbol.name)
            
            # 创建 JAVAFILE 节点
            javafile_nodes.append(
                {
                    "symbol_id": java_file_id,
                    "file_path": file_path,
                    "package_name": package_name,
                    "imports": ",".join(imports) if imports else "",
                    "defined_types": ",".join(types) if types else "",
                    "start_line": 1,
                    "start_col": 0,
                    "end_line": 0,
                    "end_col": 0,
                }
            )

        try:
            nodes_count += _batch_create_nodes("JAVAFILE", javafile_nodes)
        except Exception as e:
            print(f"    警告: 批量创建 JAVAFILE 节点失败: {e}")
        
        # 3. 导出关系
        print("  [导出] 创建关系...")

        rels_by_type: Dict[str, List[dict]] = {}

        # 成员属于关系
        for edge in self.builder.symbol_table.membership_edges:
            source_symbol = self.builder.symbol_table.lookup_by_id(edge.source_symbol)
            target_symbol = self.builder.symbol_table.lookup_by_id(edge.target_symbol)
            
            if not source_symbol or not target_symbol:
                continue
            
            if source_symbol.file_path not in file_paths and target_symbol.file_path not in file_paths:
                continue
            rels_by_type.setdefault("MEMBER_OF", []).append({"s": edge.source_symbol, "t": edge.target_symbol})
        
        # JAVAFILE 包含关系
        for symbol_id, symbol in self.builder.symbol_table.symbols.items():
            if symbol.file_path not in file_paths:
                continue
            
            if symbol.symbol_type in {SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.RECORD, SymbolType.ENUM, SymbolType.ANNOTATION}:
                java_file_id = f"file#{symbol.file_path}"
                rels_by_type.setdefault("CONTAINS", []).append({"s": java_file_id, "t": symbol_id})
        
        # 继承关系
        for edge in self.builder.symbol_table.inheritance_edges:
            source_symbol = self.builder.symbol_table.lookup_by_id(edge.source_symbol)
            target_symbol = self.builder.symbol_table.lookup_by_id(edge.target_symbol)
            
            if not source_symbol or not target_symbol:
                continue
            
            if source_symbol.file_path not in file_paths and target_symbol.file_path not in file_paths:
                continue
            
            rel_type = "EXTENDS" if edge.is_extension else "IMPLEMENTS"
            rels_by_type.setdefault(rel_type, []).append({"s": edge.source_symbol, "t": edge.target_symbol})
        
        # 方法调用关系
        for edge in self.builder.symbol_table.call_edges:
            source_symbol = self.builder.symbol_table.lookup_by_id(edge.source_symbol)
            target_symbol = self.builder.symbol_table.lookup_by_id(edge.target_symbol)
            
            if not source_symbol or not target_symbol:
                continue
            
            if source_symbol.file_path not in file_paths and target_symbol.file_path not in file_paths:
                continue
            rels_by_type.setdefault("CALLS", []).append({"s": edge.source_symbol, "t": edge.target_symbol})
        
        # 字段访问关系
        for edge in self.builder.symbol_table.access_edges:
            source_symbol = self.builder.symbol_table.lookup_by_id(edge.source_symbol)
            target_symbol = self.builder.symbol_table.lookup_by_id(edge.target_symbol)
            
            if not source_symbol or not target_symbol:
                continue
            
            if source_symbol.file_path not in file_paths and target_symbol.file_path not in file_paths:
                continue
            
            rel_type = "ACCESSES_WRITE" if edge.is_write else "ACCESSES_READ"
            rels_by_type.setdefault(rel_type, []).append({"s": edge.source_symbol, "t": edge.target_symbol})

        for rel_type, rels in rels_by_type.items():
            try:
                relationships_count += _batch_create_relationships(rel_type, rels)
            except Exception as e:
                print(f"    警告: 批量创建关系失败 {rel_type}: {e}")
        
        return {
            'nodes_count': nodes_count,
            'relationships_count': relationships_count
        }
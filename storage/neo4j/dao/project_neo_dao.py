from typing import Optional, Dict, List, Any

from neomodel import db

from storage.neo4j.graph_schema import JavaNeo4jNodeType


class ProjectNeoDao:

    # ==================== 统计 ====================

    def count_all(self, project_key_list: List[str]) -> Dict[str, Dict[str, int]]:
        """批量查询各项目各节点类型数量"""
        if not project_key_list:
            return {}

        all_labels = [t.value for t in JavaNeo4jNodeType]
        query = """
        UNWIND $project_keys AS project_key
        MATCH (n)
        WHERE n.project_key = project_key
          AND labels(n)[0] IN $labels
        WITH project_key, labels(n)[0] AS label
        RETURN project_key, label, COUNT(*) AS count
        """
        results, _ = db.cypher_query(query, {"project_keys": project_key_list, "labels": all_labels})

        result_dict: Dict[str, Dict[str, int]] = {}
        for row in results:
            pk, label, count = row[0], row[1], row[2]
            result_dict.setdefault(pk, {})[label] = count
        return result_dict

    # ==================== 项目列表 ====================

    def list_projects(self, include_counts: bool = False) -> List[Dict[str, Any]]:
        """
        列出所有 Application 类型的 Project 节点。
        include_counts=True 时附带节点/关系统计（较慢）。
        """
        if include_counts:
            query = """
            MATCH (p:Project)
            WHERE p.name IS NOT NULL AND p.name <> "" AND p.project_type = "Application"
            WITH p,
                 p.name AS project_name,
                 coalesce(p.symbol_id, '') AS project_key,
                 coalesce(p.project_type, '') AS project_type,
                 coalesce(p.branch, '') AS branch,
                 coalesce(p.commit_hash, '') AS commit_hash
            RETURN project_name, project_key, project_type, branch, commit_hash,
                   count { MATCH (n) WHERE n.belong_project = project_name } AS node_count,
                   count { MATCH (n)-[r]-() WHERE n.belong_project = project_name } AS relationship_count
            ORDER BY project_name
            """
        else:
            query = """
            MATCH (p:Project)
            WHERE (p.is_active = true OR p.is_active IS NULL)
              AND p.name IS NOT NULL
              AND p.name <> ""
              AND p.project_type = "Application"
            RETURN
              p.name AS project_name,
              coalesce(p.symbol_id, '') AS project_key,
              coalesce(p.project_type, '') AS project_type,
              coalesce(p.branch, '') AS branch,
              coalesce(p.commit_hash, '') AS commit_hash
            ORDER BY project_name
            """
        results, meta = db.cypher_query(query)
        cols = [c for c in meta]
        return [dict(zip(cols, row)) for row in results]

    def list_project_versions(self, project_name: str) -> List[Dict[str, Any]]:
        """查询某项目在图谱中所有已导入的版本列表"""
        query = """
        MATCH (p:Project {name: $name, project_type: 'Application'})
        RETURN
          coalesce(p.version, '') AS version,
          coalesce(p.symbol_id, '') AS project_key,
          coalesce(p.branch, '') AS branch,
          coalesce(p.commit_hash, '') AS commit_hash
        ORDER BY p.version
        """
        results, meta = db.cypher_query(query, {"name": project_name})
        cols = [c for c in meta]
        return [dict(zip(cols, row)) for row in results]

    def list_versions_by_names(self, project_names: List[str]) -> Dict[str, List[Dict[str, Any]]]:
        """批量查询多个项目的所有版本，返回 {project_name: [version_info, ...]}"""
        if not project_names:
            return {}
        query = """
        MATCH (p:Project {project_type: 'Application'})
        WHERE p.name IN $names
        RETURN
          p.name AS project_name,
          coalesce(p.version, '') AS version,
          coalesce(p.symbol_id, '') AS project_key,
          coalesce(p.branch, '') AS branch,
          coalesce(p.commit_hash, '') AS commit_hash
        ORDER BY p.name, p.version
        """
        results, meta = db.cypher_query(query, {"names": project_names})
        cols = list(meta)
        out: Dict[str, List[Dict[str, Any]]] = {}
        for row in results:
            d = dict(zip(cols, row))
            out.setdefault(d["project_name"], []).append({
                "version": d["version"],
                "project_key": d["project_key"],
                "branch": d["branch"],
                "commit_hash": d["commit_hash"],
            })
        return out

    # ==================== 依赖关系 ====================

    def get_depends_on(self, project_name: str) -> List[Dict[str, Any]]:
        """查询某项目通过 DEPENDS_ON 边依赖的其他已导入项目"""
        query = """
        MATCH (a:Project {name: $name})-[r:DEPENDS_ON]->(b:Project)
        RETURN b.name AS dep_project,
               r.group_id AS group_id,
               r.artifact_id AS artifact_id,
               r.dep_version AS dep_version,
               b.project_type AS project_type
        ORDER BY r.group_id, r.artifact_id
        """
        results, meta = db.cypher_query(query, {"name": project_name})
        cols = [c for c in meta]
        return [dict(zip(cols, row)) for row in results]

    def get_external_jar_names(self, project_name: str) -> List[str]:
        """查询某项目引用的所有外部 jar 名称（ExternalDefinition 节点）"""
        query = """
        MATCH (p:Project {project_type: 'Application'})
        WHERE p.name = $project_name
        MATCH (p)-[:CONTAINS_LIB]->(ext:JavaObject)
        WHERE ext.from_type = 'ExternalDefinition'
          AND ext.jar_name IS NOT NULL AND ext.jar_name <> ''
        RETURN DISTINCT ext.jar_name AS jar_name
        ORDER BY jar_name
        """
        results, _ = db.cypher_query(query, {"project_name": project_name})
        return [row[0] for row in results if row[0]]

    # ==================== 项目摘要（供 LLM 工具使用）====================

    def get_project_summary(self, project_name: str) -> Optional[Dict[str, Any]]:
        """查询某 Application 项目的节点摘要（周边 2 跳内的节点采样）"""
        query = """
        MATCH (p:Project {project_type: 'Application'})
        WHERE p.name = $project_name AND (coalesce(p.is_active,false) = true OR p.is_active IS NULL)
        OPTIONAL MATCH (p)-[:CONTAINS*0..2]->(m)
        WHERE m.symbol_id IS NOT NULL
        WITH p, collect(DISTINCT { label: labels(m)[0], id: m.symbol_id, name: m.name })[0..20] AS sample
        RETURN p.name AS projectName, sample
        LIMIT 1
        """
        results, meta = db.cypher_query(query, {"project_name": project_name})
        if not results:
            return None
        cols = [c for c in meta]
        return dict(zip(cols, results[0]))

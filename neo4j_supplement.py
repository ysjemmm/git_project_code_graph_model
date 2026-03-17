from neo4j import GraphDatabase
from datetime import datetime

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
AUTH = ("neo4j", "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4")

driver = GraphDatabase.driver(URI, auth=AUTH)

print(f"【模式二补充分析 - 开始时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}】")
print()

def run_query(session, cypher, title):
    print(f"{'='*60}")
    print(f"### {title}")
    print(f"{'='*60}")
    try:
        result = session.run(cypher)
        records = list(result)
        if not records:
            print("（无结果）")
        else:
            for i, r in enumerate(records):
                d = dict(r)
                for k, v in d.items():
                    if v and len(str(v)) > 300:
                        print(f"  [{k}]: {str(v)[:300]}...(截断)")
                    else:
                        print(f"  [{k}]: {v}")
                print()
        print(f"共 {len(records)} 条记录")
    except Exception as e:
        print(f"查询出错: {e}")
    print()

with driver.session(database="neo4j") as session:

    # Query 1: Plugin/Filter/Runner/Chain classes
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.name CONTAINS 'Plugin' OR n.name CONTAINS 'Filter' OR n.name CONTAINS 'Runner' OR n.name CONTAINS 'Chain'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 30
""", "查询1：Plugin/Filter/Runner/Chain 相关类")

    # Query 2: PluginRunner / FilterRunner methods
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginRunner' OR m.parent_symbol_id CONTAINS 'FilterRunner'
RETURN m.name, m.raw_metadata
""", "查询2：PluginRunner/FilterRunner 方法体")

    # Query 3: GatewayPlugin / AsyncGatewayPlugin interface
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.name = 'GatewayPlugin' OR n.name = 'AsyncGatewayPlugin'
RETURN n.name, n.qualified_name, n.raw_metadata
""", "查询3：GatewayPlugin/AsyncGatewayPlugin 接口定义")

    # Query 4: filterRequest / filterResponse method nodes
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.name = 'filterResponse' OR m.name = 'filterRequest'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 20
""", "查询4：所有 filterRequest/filterResponse 方法节点")

    # Query 5: CALLS involving filterResponse
    run_query(session, """
MATCH (caller:JavaMethod)-[r:CALLS]->(callee:JavaMethod)
WHERE callee.name = 'filterResponse' OR caller.name CONTAINS 'run' OR caller.name CONTAINS 'execute'
RETURN caller.name, caller.parent_symbol_id, callee.name, callee.parent_symbol_id
LIMIT 20
""", "查询5：CALLS 关系中涉及 filterResponse 的调用")

    # Query 6: CustomRoutingPlugin all relations
    run_query(session, """
MATCH (n:JavaObject {name: 'CustomRoutingPlugin'})-[r]-(m)
RETURN type(r), m.name, m.qualified_name
LIMIT 20
""", "查询6：CustomRoutingPlugin 所有关系")

    # Bonus: PluginInboundAsyncFilter methods (from previous queries)
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginInboundAsyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "补充查询A：PluginInboundAsyncFilter 方法体")

    # Bonus: any class that calls filterResponse
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.raw_metadata CONTAINS 'filterResponse'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 20
""", "补充查询B：raw_metadata 中包含 filterResponse 的方法")

    # Bonus: epaas-gateway presence
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.file_path CONTAINS 'epaas-gateway' OR n.qualified_name CONTAINS 'epaas'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 20
""", "补充查询C：epaas-gateway 是否在图谱中")

print(f"【模式二补充分析 - 结束时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}】")
driver.close()

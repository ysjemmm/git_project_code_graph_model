from neo4j import GraphDatabase
from datetime import datetime

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
AUTH = ("neo4j", "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4")

driver = GraphDatabase.driver(URI, auth=AUTH)

print(f"开始时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

def run_query(session, cypher, title):
    print(f"\n### {title}")
    try:
        result = session.run(cypher)
        records = list(result)
        if not records:
            print("（无结果）")
        else:
            for r in records:
                print(dict(r))
        print(f"共 {len(records)} 条记录")
    except Exception as e:
        print(f"查询出错: {e}")

with driver.session(database="neo4j") as session:

    # Query 1 - Filter/Plugin/Chain/Runner classes
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.name CONTAINS 'Filter' OR n.name CONTAINS 'Plugin' OR n.name CONTAINS 'Chain' OR n.name CONTAINS 'Runner'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 50
""", "查询1：框架核心类")

    # Query 2 - GatewayPlugin implementations
    run_query(session, """
MATCH (c:JavaObject)-[:IMPLEMENTS]->(p:JavaObject)
WHERE p.name = 'GatewayPlugin' OR p.name CONTAINS 'GatewayPlugin'
RETURN c.name, c.qualified_name, p.name
""", "查询2：GatewayPlugin 实现类")

    # Query 6 - epaas-gateway in graph
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.file_path CONTAINS 'epaas-gateway'
RETURN n.name, n.file_path
LIMIT 20
""", "查询6：epaas-gateway 是否在图谱中")

    # Additional: PluginInboundAsyncFilter full detail
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginInboundAsyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "补充：PluginInboundAsyncFilter 方法体")

    # Additional: PluginAsFilterLoader
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginAsFilterLoader'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 10
""", "补充：PluginAsFilterLoader 方法体")

    # Additional: CustomRoutingPlugin filterRequest/filterResponse
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN m.name, m.raw_metadata
""", "补充：CustomRoutingPlugin 所有方法")

print(f"\n结束时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
driver.close()

from neo4j import GraphDatabase
from datetime import datetime

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
AUTH = ("neo4j", "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4")

driver = GraphDatabase.driver(URI, auth=AUTH)

print(f"【模式二补充分析 - 开始时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}】")
print()

def run_query(session, cypher, title, limit=None):
    print(f"### {title}")
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
    print()

with driver.session(database="neo4j") as session:

    # Query 1
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.name CONTAINS 'Filter' OR n.name CONTAINS 'Plugin' OR n.name CONTAINS 'Chain' OR n.name CONTAINS 'Runner'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 50
""", "查询1结果：框架核心类")

    # Query 2
    run_query(session, """
MATCH (c:JavaObject)-[:IMPLEMENTS]->(p:JavaObject)
WHERE p.name = 'GatewayPlugin' OR p.name CONTAINS 'GatewayPlugin'
RETURN c.name, c.qualified_name, p.name
""", "查询2结果：GatewayPlugin 实现类")

    # Query 3
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.name = 'filterRequest' OR m.name = 'filterResponse' OR m.raw_metadata CONTAINS 'filterRequest' OR m.raw_metadata CONTAINS 'filterResponse'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 30
""", "查询3结果：filterRequest/filterResponse 调用方")

    # Query 4
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'epaas' OR m.parent_symbol_id CONTAINS 'gateway.core' OR m.parent_symbol_id CONTAINS 'FilterRunner' OR m.parent_symbol_id CONTAINS 'PluginChain'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 30
""", "查询4结果：框架执行类方法体")

    # Query 5
    run_query(session, """
MATCH (f:JavaField)
WHERE f.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN f.name, f.raw_metadata, f.modifiers
""", "查询5结果：parallelTotalCount 字段信息")

    # Query 6
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.file_path CONTAINS 'epaas-gateway'
RETURN n.name, n.file_path
LIMIT 20
""", "查询6结果：epaas-gateway 是否在图谱中")

    # Query 7
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'CustomAsyncHttpsPlugin'
RETURN m.name, m.raw_metadata
""", "查询7结果：CustomAsyncHttpsPlugin 完整方法体")

    # Query 8
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.raw_metadata CONTAINS 'future' OR m.raw_metadata CONTAINS 'CompletableFuture'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 20
""", "查询8结果：future 相关方法")

print(f"【模式二补充分析 - 结束时间：{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}】")

driver.close()

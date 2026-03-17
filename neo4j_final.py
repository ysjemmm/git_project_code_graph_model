from neo4j import GraphDatabase

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
AUTH = ("neo4j", "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4")
driver = GraphDatabase.driver(URI, auth=AUTH)

def run_query(session, cypher, title):
    print(f"\n{'='*70}")
    print(f"### {title}")
    print(f"{'='*70}")
    try:
        result = session.run(cypher)
        records = list(result)
        if not records:
            print("（无结果）")
        else:
            for r in records:
                d = dict(r)
                for k, v in d.items():
                    val = str(v) if v is not None else "None"
                    # 不截断，完整输出
                    print(f"  [{k}]:\n{val}")
                print("  ---")
        print(f"共 {len(records)} 条记录")
    except Exception as e:
        print(f"查询出错: {e}")

with driver.session(database="neo4j") as session:

    # wrapPluginAsFilter 完整内容（不截断）
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginAsFilterLoader' AND m.name = 'wrapPluginAsFilter'
RETURN m.name, m.raw_metadata
""", "wrapPluginAsFilter 完整内容（关键：GatewayPlugin 如何被包装）")

    # PluginOutboundSyncFilter.shouldFilter 逻辑
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginOutboundSyncFilter' OR m.parent_symbol_id CONTAINS 'PluginBaseFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginOutboundSyncFilter/PluginBaseFilter shouldFilter 逻辑")

    # FilterUtil.shouldFilter 完整内容
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'FilterUtil' AND m.name = 'shouldFilter'
RETURN m.name, m.raw_metadata
""", "FilterUtil.shouldFilter 完整内容（决定 outbound filter 是否执行）")

    # PluginEndpointFilter 方法体
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginEndpointFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginEndpointFilter 方法体（endpoint filter 如何处理 filterRequest 返回非null）")

    # PluginBaseFilter shouldFilter
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginBaseFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginBaseFilter 所有方法（基类 shouldFilter 逻辑）")

driver.close()

from neo4j import GraphDatabase
from datetime import datetime

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
                    if len(val) > 2000:
                        print(f"  [{k}]:\n{val[:2000]}\n...(截断，共{len(val)}字符)")
                    else:
                        print(f"  [{k}]:\n{val}")
                print("  ---")
        print(f"共 {len(records)} 条记录")
    except Exception as e:
        print(f"查询出错: {e}")

with driver.session(database="neo4j") as session:

    # FilterUtil 完整方法体
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'FilterUtil'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "FilterUtil 所有方法体（关键：executeFilterResponse 逻辑）")

    # PluginOutboundWrappedSyncToAsyncFilter 完整方法体
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginOutboundWrappedSyncToAsyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginOutboundWrappedSyncToAsyncFilter 完整方法体")

    # PluginOutboundAsyncFilter 完整方法体
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginOutboundAsyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginOutboundAsyncFilter 完整方法体")

    # PluginInboundSyncFilter 方法体（同步插件的 inbound filter）
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginInboundSyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginInboundSyncFilter 完整方法体")

    # PluginOutboundSyncFilter 方法体
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginOutboundSyncFilter'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
""", "PluginOutboundSyncFilter 完整方法体")

    # ExecuteConfig / FilterRequestConfig 结构
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'ExecuteConfig'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 10
""", "ExecuteConfig 方法体（了解 filterRequest 返回非null时的配置）")

    # 查找 PluginAsFilterLoader 或 PluginLoader
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginAsFilterLoader' OR m.parent_symbol_id CONTAINS 'PluginLoader'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 10
""", "PluginAsFilterLoader/PluginLoader 方法体")

    # 查找 bootstrap-common 下所有 Filter 类
    run_query(session, """
MATCH (n:JavaObject)
WHERE n.qualified_name CONTAINS 'bootstrap' AND (n.name CONTAINS 'Filter' OR n.name CONTAINS 'Plugin')
RETURN n.name, n.qualified_name
""", "bootstrap-common 下所有 Filter/Plugin 类")

    # 查找 PluginInboundAsyncFilter.convertToObservable 完整内容（不截断）
    run_query(session, """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginInboundAsyncFilter' AND m.name = 'convertToObservable'
RETURN m.name, m.raw_metadata
""", "PluginInboundAsyncFilter.convertToObservable 完整内容")

driver.close()

#!/usr/bin/env python3
"""Neo4j 图谱 - 查询字段节点"""

from neo4j import GraphDatabase

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
USER = "neo4j"
PASSWORD = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"

driver = GraphDatabase.driver(URI, auth=(USER, PASSWORD))

def run_query(query, params=None):
    with driver.session(database="neo4j") as session:
        result = session.run(query, params or {})
        return [record.data() for record in result]

print("=" * 80)
print("Neo4j 图谱 - 字段节点查询")
print("=" * 80)

# 1. 查看 CustomRoutingPlugin 的字段
print("\n【1】查看 CustomRoutingPlugin 的字段")
q1 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(f:JavaField)
WHERE c.name = 'CustomRoutingPlugin'
RETURN f.name, f.symbol_id, f.raw_field, f.is_static, f.type_name
LIMIT 20
"""
r1 = run_query(q1)
print(f"  找到 {len(r1)} 个字段")
for r in r1:
    raw_field = r.get('f.raw_field') or ''
    print(f"  field: {r.get('f.name','?')} | static={r.get('f.is_static','?')} | type={r.get('f.type_name','?')}")
    if raw_field:
        print(f"    raw_field: {raw_field[:300]}")

# 2. 查看 FilterUtil 的方法（epaas-gateway 项目）
print("\n【2】查看 epaas-gateway 项目中的 FilterUtil 方法")
q2 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'FilterUtil' AND c.belong_project = 'epaas-gateway'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r2 = run_query(q2)
print(f"  找到 {len(r2)} 个方法")
for r in r2:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 3. 查看 PluginInboundAsyncFilter 的方法（epaas-gateway 项目）
print("\n【3】查看 epaas-gateway 项目中的 PluginInboundAsyncFilter 方法")
q3 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'PluginInboundAsyncFilter' AND c.belong_project = 'epaas-gateway'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r3 = run_query(q3)
print(f"  找到 {len(r3)} 个方法")
for r in r3:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 4. 查看所有 epaas-gateway 项目的 JavaObject 节点
print("\n【4】查看 epaas-gateway 项目的 JavaObject 节点（bootstrap-common 相关）")
q4 = """
MATCH (n:JavaObject)
WHERE n.belong_project = 'epaas-gateway' AND (
    n.name CONTAINS 'Filter' OR n.name CONTAINS 'Plugin'
)
RETURN n.name, n.qualified_name, n.belong_project
ORDER BY n.name
LIMIT 40
"""
r4 = run_query(q4)
print(f"  找到 {len(r4)} 个节点")
for r in r4:
    print(f"  {r.get('n.name','?')} | {r.get('n.qualified_name','?')}")

# 5. 查看 PluginInboundAsyncFilter 节点（不限项目）
print("\n【5】查看所有 PluginInboundAsyncFilter 节点")
q5 = """
MATCH (n:JavaObject)
WHERE n.name = 'PluginInboundAsyncFilter'
RETURN n.name, n.qualified_name, n.belong_project, n.symbol_id
"""
r5 = run_query(q5)
print(f"  找到 {len(r5)} 个节点")
for r in r5:
    print(f"  {r.get('n.name','?')} | {r.get('n.qualified_name','?')} | project={r.get('n.belong_project','?')}")

# 6. 查看 PluginInboundAsyncFilter 的方法（通过 symbol_id）
print("\n【6】查看 PluginInboundAsyncFilter 的方法（通过 parent_symbol_id）")
q6 = """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginInboundAsyncFilter'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r6 = run_query(q6)
print(f"  找到 {len(r6)} 个方法")
for r in r6:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 7. 查看 FilterUtil 的方法（通过 parent_symbol_id）
print("\n【7】查看 FilterUtil 的方法（通过 parent_symbol_id）")
q7 = """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'FilterUtil'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r7 = run_query(q7)
print(f"  找到 {len(r7)} 个方法")
for r in r7:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 8. 查看 GatewayPlugin 的方法（通过 parent_symbol_id）
print("\n【8】查看 GatewayPlugin 的方法（通过 parent_symbol_id）")
q8 = """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'GatewayPlugin' OR m.parent_symbol_id CONTAINS 'AsyncGatewayPlugin'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r8 = run_query(q8)
print(f"  找到 {len(r8)} 个方法")
for r in r8:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:400]}")

# 9. 查看 PluginOutboundAsyncFilter 的方法（通过 parent_symbol_id）
print("\n【9】查看 PluginOutboundAsyncFilter 的方法（通过 parent_symbol_id）")
q9 = """
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginOutboundAsyncFilter'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r9 = run_query(q9)
print(f"  找到 {len(r9)} 个方法")
for r in r9:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 10. 查看 CustomRoutingPlugin 的字段（通过 parent_symbol_id）
print("\n【10】查看 CustomRoutingPlugin 的字段（通过 parent_symbol_id）")
q10 = """
MATCH (f:JavaField)
WHERE f.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN f.name, f.symbol_id, f.raw_field, f.is_static, f.type_name
LIMIT 20
"""
r10 = run_query(q10)
print(f"  找到 {len(r10)} 个字段")
for r in r10:
    raw_field = r.get('f.raw_field') or ''
    print(f"  field: {r.get('f.name','?')} | static={r.get('f.is_static','?')} | type={r.get('f.type_name','?')}")
    if raw_field:
        print(f"    raw_field: {raw_field[:300]}")

driver.close()
print("\n查询完成")

#!/usr/bin/env python3
"""Neo4j 图谱 - 查询方法节点（关系方向修正）"""

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
print("Neo4j 图谱 - 方法节点查询（修正关系方向）")
print("=" * 80)

# 关系方向是 JavaObject -MEMBER_OF-> JavaMethod（从上面看到的）
# 实际上是 JavaMethod -MEMBER_OF-> JavaObject 还是 JavaObject -MEMBER_OF-> JavaMethod?
# 从【3】看到：CustomAsyncHttpsPlugin -MEMBER_OF-> filterRequestAsync
# 所以是 JavaObject -MEMBER_OF-> JavaMethod（类 -> 方法）

# 1. 查看 CustomAsyncHttpsPlugin 的方法（正确方向）
print("\n【1】查看 CustomAsyncHttpsPlugin 的方法（JavaObject -MEMBER_OF-> JavaMethod）")
q1 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'CustomAsyncHttpsPlugin'
RETURN m.name, m.symbol_id, m.raw_metadata, m.method_calls, m.start_line, m.end_line
LIMIT 20
"""
r1 = run_query(q1)
print(f"  找到 {len(r1)} 个方法")
for r in r1:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:800]}")
    if r.get('m.method_calls'):
        print(f"    method_calls: {r.get('m.method_calls','')[:400]}")

# 2. 查看 CustomRoutingPlugin 的方法
print("\n【2】查看 CustomRoutingPlugin 的方法")
q2 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'CustomRoutingPlugin'
RETURN m.name, m.symbol_id, m.raw_metadata, m.method_calls, m.start_line, m.end_line
LIMIT 20
"""
r2 = run_query(q2)
print(f"  找到 {len(r2)} 个方法")
for r in r2:
    body = r.get('m.raw_metadata','') or ''
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if 'parallelTotalCount' in body or 'permit' in body.lower() or 'Permit' in body:
        print(f"    [含限流逻辑] raw_metadata: {body[:800]}")
    elif body:
        print(f"    raw_metadata: {body[:200]}")

# 3. 查看 FilterUtil 的方法
print("\n【3】查看 FilterUtil 的方法")
q3 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'FilterUtil'
RETURN m.name, m.symbol_id, m.raw_metadata, m.method_calls, m.start_line, m.end_line
LIMIT 20
"""
r3 = run_query(q3)
print(f"  找到 {len(r3)} 个方法")
for r in r3:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 4. 查看 PluginInboundAsyncFilter 的方法
print("\n【4】查看 PluginInboundAsyncFilter 的方法")
q4 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'PluginInboundAsyncFilter'
RETURN m.name, m.symbol_id, m.raw_metadata, m.method_calls, m.start_line, m.end_line
LIMIT 20
"""
r4 = run_query(q4)
print(f"  找到 {len(r4)} 个方法")
for r in r4:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 5. 查看 HttpResponseMessageFutureCallback 的方法
print("\n【5】查看 HttpResponseMessageFutureCallback 的方法")
q5 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'HttpResponseMessageFutureCallback'
RETURN m.name, m.symbol_id, m.raw_metadata, m.method_calls, m.start_line, m.end_line
LIMIT 20
"""
r5 = run_query(q5)
print(f"  找到 {len(r5)} 个方法")
for r in r5:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 6. 查看 GatewayPlugin 的方法
print("\n【6】查看 GatewayPlugin 的方法")
q6 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN c.name, m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r6 = run_query(q6)
print(f"  找到 {len(r6)} 个方法")
for r in r6:
    print(f"  {r.get('c.name','?')}.{r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:400]}")

# 7. 查看 CustomRoutingPlugin 的字段
print("\n【7】查看 CustomRoutingPlugin 的字段")
q7 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(f:JavaField)
WHERE c.name = 'CustomRoutingPlugin'
RETURN f.name, f.symbol_id, f.raw_field, f.is_static, f.type_name
LIMIT 20
"""
r7 = run_query(q7)
print(f"  找到 {len(r7)} 个字段")
for r in r7:
    print(f"  field: {r.get('f.name','?')} | static={r.get('f.is_static','?')} | type={r.get('f.type_name','?')} | {r.get('f.raw_field','')[:200]}")

# 8. 查看 PluginBaseFilter 的方法
print("\n【8】查看 PluginBaseFilter 的方法")
q8 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'PluginBaseFilter'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r8 = run_query(q8)
print(f"  找到 {len(r8)} 个方法")
for r in r8:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:400]}")

# 9. 查看 PluginOutboundAsyncFilter 的方法
print("\n【9】查看 PluginOutboundAsyncFilter 的方法")
q9 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'PluginOutboundAsyncFilter'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r9 = run_query(q9)
print(f"  找到 {len(r9)} 个方法")
for r in r9:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:600]}")

# 10. 查看 PluginAsFilterLoader 的方法
print("\n【10】查看 PluginAsFilterLoader 的方法")
q10 = """
MATCH (c:JavaObject)-[:MEMBER_OF]->(m:JavaMethod)
WHERE c.name = 'PluginAsFilterLoader'
RETURN m.name, m.symbol_id, m.raw_metadata, m.start_line, m.end_line
LIMIT 20
"""
r10 = run_query(q10)
print(f"  找到 {len(r10)} 个方法")
for r in r10:
    print(f"  method: {r.get('m.name','?')} | lines: {r.get('m.start_line','?')}-{r.get('m.end_line','?')}")
    if r.get('m.raw_metadata'):
        print(f"    raw_metadata: {r.get('m.raw_metadata','')[:400]}")

driver.close()
print("\n查询完成")

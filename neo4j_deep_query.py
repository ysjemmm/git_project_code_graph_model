#!/usr/bin/env python3
"""Neo4j 图谱深度查询 - 探索 JavaMethod 节点和关系"""

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
print("Neo4j 图谱深度查询")
print("=" * 80)

# 1. 查看 JavaMethod 节点的属性
print("\n【1】查看 JavaMethod 节点属性示例")
q1 = """
MATCH (m:JavaMethod)
RETURN m
LIMIT 3
"""
r1 = run_query(q1)
for r in r1:
    print(f"  {r}")

# 2. 查看 JavaObject 节点的属性
print("\n【2】查看 JavaObject 节点属性示例（CustomAsyncHttpsPlugin）")
q2 = """
MATCH (n:JavaObject)
WHERE n.name = 'CustomAsyncHttpsPlugin'
RETURN n
"""
r2 = run_query(q2)
for r in r2:
    print(f"  {r}")

# 3. 查看 CustomAsyncHttpsPlugin 的所有关系
print("\n【3】查看 CustomAsyncHttpsPlugin 的所有关系")
q3 = """
MATCH (n:JavaObject)-[r]->(m)
WHERE n.name = 'CustomAsyncHttpsPlugin'
RETURN type(r) as rel_type, m.name, m.qualified_name, labels(m) as labels
LIMIT 30
"""
r3 = run_query(q3)
print(f"  找到 {len(r3)} 个关系")
for r in r3:
    print(f"  -{r.get('rel_type','?')}-> {r.get('m.name','?')} [{r.get('labels',[])}]")

# 4. 查看 CustomAsyncHttpsPlugin 的反向关系
print("\n【4】查看指向 CustomAsyncHttpsPlugin 的关系")
q4 = """
MATCH (m)-[r]->(n:JavaObject)
WHERE n.name = 'CustomAsyncHttpsPlugin'
RETURN type(r) as rel_type, m.name, m.qualified_name, labels(m) as labels
LIMIT 30
"""
r4 = run_query(q4)
print(f"  找到 {len(r4)} 个关系")
for r in r4:
    print(f"  {r.get('m.name','?')} [{r.get('labels',[])}] -{r.get('rel_type','?')}->")

# 5. 查看 JavaMethod 节点的 MEMBER_OF 关系
print("\n【5】查看 JavaMethod 节点的 MEMBER_OF 关系（CustomAsyncHttpsPlugin）")
q5 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'CustomAsyncHttpsPlugin'
RETURN m.name, m.qualified_name, m.raw_method_body, m.raw_method, m.method_calls, m.body
LIMIT 20
"""
r5 = run_query(q5)
print(f"  找到 {len(r5)} 个方法")
for r in r5:
    print(f"  method: {r.get('m.name','?')} | {r.get('m.qualified_name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    raw_method_body: {r.get('m.raw_method_body','')[:600]}")
    if r.get('m.raw_method'):
        print(f"    raw_method: {r.get('m.raw_method','')[:600]}")
    if r.get('m.method_calls'):
        print(f"    method_calls: {r.get('m.method_calls','')[:400]}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:400]}")

# 6. 查看 CustomRoutingPlugin 的 MEMBER_OF 关系
print("\n【6】查看 CustomRoutingPlugin 的方法（MEMBER_OF）")
q6 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'CustomRoutingPlugin'
RETURN m.name, m.qualified_name, m.raw_method_body, m.method_calls
LIMIT 20
"""
r6 = run_query(q6)
print(f"  找到 {len(r6)} 个方法")
for r in r6:
    body = r.get('m.raw_method_body','') or ''
    print(f"  method: {r.get('m.name','?')}")
    if 'parallelTotalCount' in body or 'permit' in body.lower():
        print(f"    [含限流逻辑] body: {body[:800]}")
    elif body:
        print(f"    body: {body[:200]}")

# 7. 查看 FilterUtil 的方法（MEMBER_OF）
print("\n【7】查看 FilterUtil 的方法（MEMBER_OF）")
q7 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'FilterUtil'
RETURN m.name, m.qualified_name, m.raw_method_body, m.method_calls
LIMIT 20
"""
r7 = run_query(q7)
print(f"  找到 {len(r7)} 个方法")
for r in r7:
    print(f"  method: {r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:600]}")

# 8. 查看 PluginInboundAsyncFilter 的方法（MEMBER_OF）
print("\n【8】查看 PluginInboundAsyncFilter 的方法（MEMBER_OF）")
q8 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'PluginInboundAsyncFilter'
RETURN m.name, m.qualified_name, m.raw_method_body, m.method_calls
LIMIT 20
"""
r8 = run_query(q8)
print(f"  找到 {len(r8)} 个方法")
for r in r8:
    print(f"  method: {r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:600]}")

# 9. 查看 HttpResponseMessageFutureCallback 的方法（MEMBER_OF）
print("\n【9】查看 HttpResponseMessageFutureCallback 的方法（MEMBER_OF）")
q9 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'HttpResponseMessageFutureCallback'
RETURN m.name, m.qualified_name, m.raw_method_body, m.method_calls
LIMIT 20
"""
r9 = run_query(q9)
print(f"  找到 {len(r9)} 个方法")
for r in r9:
    print(f"  method: {r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:600]}")

# 10. 查看 GatewayPlugin 的方法（MEMBER_OF）
print("\n【10】查看 GatewayPlugin 的方法（MEMBER_OF）")
q10 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN c.name, m.name, m.qualified_name, m.raw_method_body, m.method_calls
LIMIT 20
"""
r10 = run_query(q10)
print(f"  找到 {len(r10)} 个方法")
for r in r10:
    print(f"  {r.get('c.name','?')}.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:400]}")

# 11. 查看 JavaField 节点（CustomRoutingPlugin）
print("\n【11】查看 CustomRoutingPlugin 的字段（JavaField MEMBER_OF）")
q11 = """
MATCH (f:JavaField)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'CustomRoutingPlugin'
RETURN f.name, f.qualified_name, f.raw_field, f.is_static, f.type_name
LIMIT 20
"""
r11 = run_query(q11)
print(f"  找到 {len(r11)} 个字段")
for r in r11:
    print(f"  field: {r.get('f.name','?')} | static={r.get('f.is_static','?')} | type={r.get('f.type_name','?')} | {r.get('f.raw_field','')[:200]}")

# 12. 查看 PluginBaseFilter 的方法（MEMBER_OF）
print("\n【12】查看 PluginBaseFilter 的方法（MEMBER_OF）")
q12 = """
MATCH (m:JavaMethod)-[:MEMBER_OF]->(c:JavaObject)
WHERE c.name = 'PluginBaseFilter'
RETURN m.name, m.qualified_name, m.raw_method_body
LIMIT 20
"""
r12 = run_query(q12)
print(f"  找到 {len(r12)} 个方法")
for r in r12:
    print(f"  method: {r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:400]}")

driver.close()
print("\n查询完成")

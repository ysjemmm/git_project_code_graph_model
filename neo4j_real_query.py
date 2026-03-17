#!/usr/bin/env python3
"""Neo4j 图谱查询 - 使用正确的属性名"""

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
print("Neo4j 图谱查询 - epaas-gateway 框架分析（正确属性名）")
print("=" * 80)

# 1. 查找 epaas-gateway 框架在图谱中的节点
print("\n【1】查找 epaas-gateway 框架节点")
q1 = """
MATCH (n)
WHERE n.qualified_name CONTAINS 'timevale.middleware.gateway'
   OR n.qualified_name CONTAINS 'timevale.egress.gateway'
RETURN n.qualified_name, n.name, labels(n) as labels, n.file_path
ORDER BY n.qualified_name
LIMIT 60
"""
r1 = run_query(q1)
print(f"  找到 {len(r1)} 个节点")
for r in r1:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualified_name','?')} | name: {r.get('n.name','?')}")

# 2. 查找 GatewayPlugin、AsyncGatewayPlugin 接口节点
print("\n【2】查找 GatewayPlugin、AsyncGatewayPlugin 接口节点")
q2 = """
MATCH (n)
WHERE n.name IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN n.qualified_name, n.name, labels(n) as labels, n.file_path, n.raw_code
"""
r2 = run_query(q2)
print(f"  找到 {len(r2)} 个节点")
for r in r2:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualified_name','?')}")
    if r.get('n.raw_code'):
        print(f"    raw_code: {r.get('n.raw_code','')[:300]}")

# 3. 查找 GatewayPlugin 的方法节点（CONTAINS 关系）
print("\n【3】查找 GatewayPlugin 的方法节点")
q3 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body, m.raw_method
"""
r3 = run_query(q3)
print(f"  找到 {len(r3)} 个方法")
for r in r3:
    print(f"  {r.get('c.name','?')}.{r.get('m.name','?')} | {r.get('m.qualified_name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:400]}")
    elif r.get('m.raw_method'):
        print(f"    method: {r.get('m.raw_method','')[:400]}")

# 4. 查找 FilterUtil 节点和方法
print("\n【4】查找 FilterUtil 节点和方法")
q4 = """
MATCH (n)
WHERE n.name = 'FilterUtil'
RETURN n.qualified_name, n.name, labels(n) as labels, n.file_path
"""
r4 = run_query(q4)
print(f"  找到 {len(r4)} 个节点")
for r in r4:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualified_name','?')}")

q4b = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'FilterUtil'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body, m.raw_method
"""
r4b = run_query(q4b)
print(f"  FilterUtil 方法数: {len(r4b)}")
for r in r4b:
    print(f"  FilterUtil.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:600]}")

# 5. 查找 PluginInboundAsyncFilter 节点和方法
print("\n【5】查找 PluginInboundAsyncFilter 节点和方法")
q5 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'PluginInboundAsyncFilter'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body, m.raw_method
"""
r5 = run_query(q5)
print(f"  找到 {len(r5)} 个方法")
for r in r5:
    print(f"  PluginInboundAsyncFilter.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:600]}")

# 6. 查找 CustomAsyncHttpsPlugin 节点
print("\n【6】查找 CustomAsyncHttpsPlugin 节点")
q6 = """
MATCH (n)
WHERE n.name = 'CustomAsyncHttpsPlugin'
RETURN n.qualified_name, n.name, labels(n) as labels, n.file_path
"""
r6 = run_query(q6)
print(f"  找到 {len(r6)} 个节点")
for r in r6:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualified_name','?')}")

q6b = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'CustomAsyncHttpsPlugin'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body, m.raw_method
"""
r6b = run_query(q6b)
print(f"  CustomAsyncHttpsPlugin 方法数: {len(r6b)}")
for r in r6b:
    print(f"  CustomAsyncHttpsPlugin.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:800]}")

# 7. 查找 CustomAsyncHttpsPlugin 的 HAVE 关系（方法调用）
print("\n【7】查找 CustomAsyncHttpsPlugin 的 HAVE 关系")
q7 = """
MATCH (c)-[:HAVE]->(m)
WHERE c.name = 'CustomAsyncHttpsPlugin'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.method_calls
LIMIT 30
"""
r7 = run_query(q7)
print(f"  找到 {len(r7)} 个 HAVE 关系")
for r in r7:
    print(f"  {r.get('c.name','?')} -> {r.get('m.name','?')} | {r.get('m.qualified_name','?')}")
    if r.get('m.method_calls'):
        print(f"    method_calls: {r.get('m.method_calls','')[:400]}")

# 8. 查找 CustomRoutingPlugin 节点和方法（parallelTotalCount 相关）
print("\n【8】查找 CustomRoutingPlugin 节点和方法")
q8 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'CustomRoutingPlugin'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body
"""
r8 = run_query(q8)
print(f"  CustomRoutingPlugin 方法数: {len(r8)}")
for r in r8:
    body = r.get('m.raw_method_body','') or ''
    if 'parallelTotalCount' in body or 'permit' in body.lower() or 'Permit' in body:
        print(f"  [含限流逻辑] CustomRoutingPlugin.{r.get('m.name','?')}")
        print(f"    body: {body[:800]}")
    else:
        print(f"  CustomRoutingPlugin.{r.get('m.name','?')}")

# 9. 查找 CustomRoutingPlugin 的字段
print("\n【9】查找 CustomRoutingPlugin 的字段")
q9 = """
MATCH (c)-[:CONTAINS]->(f:JavaField)
WHERE c.name = 'CustomRoutingPlugin'
RETURN c.name, f.name, f.qualified_name, f.raw_field, f.is_static
"""
r9 = run_query(q9)
print(f"  CustomRoutingPlugin 字段数: {len(r9)}")
for r in r9:
    print(f"  {r.get('c.name','?')}.{r.get('f.name','?')} | static={r.get('f.is_static','?')} | {r.get('f.raw_field','')[:200]}")

# 10. 查找 PluginBaseFilter 节点和方法
print("\n【10】查找 PluginBaseFilter 节点和方法")
q10 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'PluginBaseFilter'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body
"""
r10 = run_query(q10)
print(f"  PluginBaseFilter 方法数: {len(r10)}")
for r in r10:
    print(f"  PluginBaseFilter.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:400]}")

# 11. 查找 HttpResponseMessageFutureCallback 节点和方法
print("\n【11】查找 HttpResponseMessageFutureCallback 节点和方法")
q11 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.name = 'HttpResponseMessageFutureCallback'
RETURN c.name, m.name, m.qualified_name, labels(m) as labels, m.raw_method_body
"""
r11 = run_query(q11)
print(f"  HttpResponseMessageFutureCallback 方法数: {len(r11)}")
for r in r11:
    print(f"  HttpResponseMessageFutureCallback.{r.get('m.name','?')}")
    if r.get('m.raw_method_body'):
        print(f"    body: {r.get('m.raw_method_body','')[:400]}")

# 12. 查找 PluginAsFilterLoader 节点
print("\n【12】查找 PluginAsFilterLoader 节点")
q12 = """
MATCH (n)
WHERE n.name = 'PluginAsFilterLoader'
RETURN n.qualified_name, n.name, labels(n) as labels, n.file_path
"""
r12 = run_query(q12)
print(f"  找到 {len(r12)} 个节点")
for r in r12:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualified_name','?')}")

# 13. 查找 epaas-gateway 项目节点
print("\n【13】查找 epaas-gateway 项目节点")
q13 = """
MATCH (n:Project)
WHERE n.name CONTAINS 'epaas' OR n.name CONTAINS 'egress' OR n.name CONTAINS 'gateway'
RETURN n.qualified_name, n.name, n.project_type, n.version
"""
r13 = run_query(q13)
print(f"  找到 {len(r13)} 个项目节点")
for r in r13:
    print(f"  {r.get('n.name','?')} | type={r.get('n.project_type','?')} | version={r.get('n.version','?')}")

driver.close()
print("\n查询完成")

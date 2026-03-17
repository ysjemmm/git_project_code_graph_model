#!/usr/bin/env python3
"""Neo4j 图谱查询脚本 - 补充分析报告"""

from neo4j import GraphDatabase
import json

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
USER = "neo4j"
PASSWORD = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"

driver = GraphDatabase.driver(URI, auth=(USER, PASSWORD))

def run_query(query, params=None):
    with driver.session(database="neo4j") as session:
        result = session.run(query, params or {})
        return [record.data() for record in result]

print("=" * 80)
print("Neo4j 图谱查询 - epaas-gateway 框架分析")
print("=" * 80)

# 1. 查找 epaas-gateway 框架在图谱中的节点（plugin-api、core 模块）
print("\n【1】查找 epaas-gateway 框架节点（plugin-api、core 模块）")
q1 = """
MATCH (n) 
WHERE n.qualifiedName CONTAINS 'timevale.middleware.gateway' 
   OR n.qualifiedName CONTAINS 'timevale.middleware.gateway.pluginapi'
   OR n.qualifiedName CONTAINS 'timevale.middleware.gateway.core'
RETURN n.qualifiedName, n.simpleName, labels(n) as labels, n.filePath
ORDER BY n.qualifiedName
LIMIT 50
"""
r1 = run_query(q1)
print(f"  找到 {len(r1)} 个节点")
for r in r1:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualifiedName','?')} | file: {r.get('n.filePath','?')}")

# 2. 查找 GatewayPlugin、AsyncGatewayPlugin 接口的图谱节点和方法
print("\n【2】查找 GatewayPlugin、AsyncGatewayPlugin 接口节点和方法")
q2 = """
MATCH (n)
WHERE n.simpleName IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN n.qualifiedName, n.simpleName, labels(n) as labels, n.filePath, n.body
"""
r2 = run_query(q2)
print(f"  找到 {len(r2)} 个节点")
for r in r2:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualifiedName','?')}")
    if r.get('n.body'):
        print(f"    body: {r.get('n.body','')[:200]}")

# 3. 查找 GatewayPlugin 的方法节点
print("\n【3】查找 GatewayPlugin 的方法节点（CONTAINS 关系）")
q3 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName IN ['GatewayPlugin', 'AsyncGatewayPlugin']
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r3 = run_query(q3)
print(f"  找到 {len(r3)} 个方法")
for r in r3:
    print(f"  {r.get('c.simpleName','?')}.{r.get('m.simpleName','?')} | {r.get('m.qualifiedName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:300]}")

# 4. 查找 FilterUtil 的方法体
print("\n【4】查找 FilterUtil 节点和方法")
q4 = """
MATCH (n)
WHERE n.simpleName = 'FilterUtil'
RETURN n.qualifiedName, n.simpleName, labels(n) as labels, n.filePath
"""
r4 = run_query(q4)
print(f"  找到 {len(r4)} 个节点")
for r in r4:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualifiedName','?')}")

q4b = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'FilterUtil'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r4b = run_query(q4b)
print(f"  FilterUtil 方法数: {len(r4b)}")
for r in r4b:
    print(f"  FilterUtil.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:500]}")

# 5. 查找 PluginInboundAsyncFilter 节点和方法
print("\n【5】查找 PluginInboundAsyncFilter 节点和方法")
q5 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'PluginInboundAsyncFilter'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r5 = run_query(q5)
print(f"  找到 {len(r5)} 个方法")
for r in r5:
    print(f"  PluginInboundAsyncFilter.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:500]}")

# 6. 查找 CustomAsyncHttpsPlugin 的完整方法体和 CALLS 关系
print("\n【6】查找 CustomAsyncHttpsPlugin 节点")
q6 = """
MATCH (n)
WHERE n.simpleName = 'CustomAsyncHttpsPlugin'
RETURN n.qualifiedName, n.simpleName, labels(n) as labels, n.filePath
"""
r6 = run_query(q6)
print(f"  找到 {len(r6)} 个节点")
for r in r6:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualifiedName','?')}")

q6b = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'CustomAsyncHttpsPlugin'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r6b = run_query(q6b)
print(f"  CustomAsyncHttpsPlugin 方法数: {len(r6b)}")
for r in r6b:
    print(f"  CustomAsyncHttpsPlugin.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:600]}")

# 7. 查找 CustomAsyncHttpsPlugin 的 CALLS 关系
print("\n【7】查找 CustomAsyncHttpsPlugin 的 CALLS 关系")
q7 = """
MATCH (caller)-[:CALLS]->(callee)
WHERE caller.qualifiedName CONTAINS 'CustomAsyncHttpsPlugin'
RETURN caller.qualifiedName, callee.qualifiedName, callee.simpleName
LIMIT 30
"""
r7 = run_query(q7)
print(f"  找到 {len(r7)} 个 CALLS 关系")
for r in r7:
    print(f"  {r.get('caller.qualifiedName','?')} -> {r.get('callee.qualifiedName','?')}")

# 8. 查找 CustomRoutingPlugin 节点和方法
print("\n【8】查找 CustomRoutingPlugin 节点和方法（parallelTotalCount 相关）")
q8 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'CustomRoutingPlugin'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r8 = run_query(q8)
print(f"  CustomRoutingPlugin 方法数: {len(r8)}")
for r in r8:
    print(f"  CustomRoutingPlugin.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        body = r.get('m.body','')
        if 'parallelTotalCount' in body or 'permit' in body.lower():
            print(f"    [含限流逻辑] body: {body[:600]}")

# 9. 查找 PluginBaseFilter.shouldFilter 逻辑
print("\n【9】查找 PluginBaseFilter 节点和方法")
q9 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'PluginBaseFilter'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r9 = run_query(q9)
print(f"  PluginBaseFilter 方法数: {len(r9)}")
for r in r9:
    print(f"  PluginBaseFilter.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:400]}")

# 10. 查找 PluginOutboundAsyncFilter 节点和方法
print("\n【10】查找 PluginOutboundAsyncFilter 节点和方法")
q10 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'PluginOutboundAsyncFilter'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r10 = run_query(q10)
print(f"  PluginOutboundAsyncFilter 方法数: {len(r10)}")
for r in r10:
    print(f"  PluginOutboundAsyncFilter.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:500]}")

# 11. 查找 HttpResponseMessageFutureCallback 节点
print("\n【11】查找 HttpResponseMessageFutureCallback 节点和方法")
q11 = """
MATCH (c)-[:CONTAINS]->(m)
WHERE c.simpleName = 'HttpResponseMessageFutureCallback'
RETURN c.simpleName, m.simpleName, m.qualifiedName, labels(m) as labels, m.body
"""
r11 = run_query(q11)
print(f"  HttpResponseMessageFutureCallback 方法数: {len(r11)}")
for r in r11:
    print(f"  HttpResponseMessageFutureCallback.{r.get('m.simpleName','?')}")
    if r.get('m.body'):
        print(f"    body: {r.get('m.body','')[:400]}")

# 12. 查找 PluginAsFilterLoader 节点
print("\n【12】查找 PluginAsFilterLoader 节点")
q12 = """
MATCH (n)
WHERE n.simpleName = 'PluginAsFilterLoader'
RETURN n.qualifiedName, n.simpleName, labels(n) as labels, n.filePath
"""
r12 = run_query(q12)
print(f"  找到 {len(r12)} 个节点")
for r in r12:
    print(f"  [{r.get('labels',[])}] {r.get('n.qualifiedName','?')}")

driver.close()
print("\n查询完成")

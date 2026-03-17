#!/usr/bin/env python3
"""探索 Neo4j 图谱 schema"""

from neo4j import GraphDatabase

URI = "neo4j+s://26fa83e0.databases.neo4j.io"
USER = "neo4j"
PASSWORD = "kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4"

driver = GraphDatabase.driver(URI, auth=(USER, PASSWORD))

def run_query(query, params=None):
    with driver.session(database="neo4j") as session:
        result = session.run(query, params or {})
        return [record.data() for record in result]

print("=== 探索图谱 Schema ===")

# 1. 查看所有节点标签
print("\n【1】所有节点标签")
r = run_query("CALL db.labels()")
for x in r:
    print(f"  {x}")

# 2. 查看所有关系类型
print("\n【2】所有关系类型")
r = run_query("CALL db.relationshipTypes()")
for x in r:
    print(f"  {x}")

# 3. 查看节点属性键
print("\n【3】所有属性键")
r = run_query("CALL db.propertyKeys()")
for x in r:
    print(f"  {x}")

# 4. 随机取一个节点看看属性
print("\n【4】随机取5个节点看属性")
r = run_query("MATCH (n) RETURN n LIMIT 5")
for x in r:
    print(f"  {x}")

# 5. 查看节点数量
print("\n【5】节点总数")
r = run_query("MATCH (n) RETURN count(n) as cnt")
for x in r:
    print(f"  {x}")

# 6. 查看关系总数
print("\n【6】关系总数")
r = run_query("MATCH ()-[r]->() RETURN count(r) as cnt")
for x in r:
    print(f"  {x}")

driver.close()
print("\n完成")

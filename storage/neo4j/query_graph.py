#!/usr/bin/env python3

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent.parent))


def query_relationships(connector):
    """查询所有关系类型及其数""
    print("\n" + "=" * 70)
    print("关系类型分布")
    print("=" * 70)
    
    result = connector.execute_query('''
    MATCH ()-[r]->()
    RETURN type(r) as relationship_type, count(*) as count
    ORDER BY count DESC
    ''')
    
    for record in result:
        print(f"  {record['relationship_type']}: {record['count']}")

def query_method_calls(connector):
    
    print("\n" + "=" * 70)
    print("方法调用关系 (CALLS)")
    print("=" * 70)
    
    result = connector.execute_query('''
    MATCH (m1:Method)-[r:CALLS]->(m2:Method)
    RETURN m1.name as caller, m2.name as callee
    ORDER BY m1.name
    ''')
    
    if result:
        for record in result:
            print(f"  {record['caller']} -> {record['callee']}")
    else:
        print("  (无数")

def query_field_access(connector):
    
    print("\n" + "=" * 70)
    print("字段访问关系 (ACCESSES)")
    print("=" * 70)
    
    result = connector.execute_query('''
    MATCH (m:Method)-[r:ACCESSES]->(f:Field)
    RETURN m.name as method, f.name as field
    ORDER BY m.name
    ''')
    
    if result:
        for record in result:
            print(f"  {record['method']} -> {record['field']}")
    else:
        print("  (无数")

def query_api_endpoints(connector):
    
    print("\n" + "=" * 70)
    print("API 端点")
    print("=" * 70)
    
    result = connector.execute_query('''
    MATCH (c:JavaObject)-[:MEMBER_OF]-(m:Method)
    WHERE m.request_mapping_path IS NOT NULL
    RETURN c.name as controller, m.name as method, 
           m.request_mapping_path as path, m.request_methods as methods
    ORDER BY c.name, m.name
    ''')
    
    if result:
        for record in result:
            methods = record['methods'] if record['methods'] else ['GET']
            print(f"  {record['controller']}.{record['method']}")
            print(f"    Path: {record['path']}")
            print(f"    Methods: {', '.join(methods)}")
    else:
        print("  (无数")

def main():
    ""
    print("=" * 70)
    print("Neo4j 代码图谱查询工具")
    print("=" * 70)
    
    connector = Neo4jConnector(
        'neo4j+s://26fa83e0.databases.neo4j.io',
        'neo4j',
        'kJ0iZG0ys9euMz_6rQle5f6-ibVqHtLDzLCgr42wZe4',
        'neo4j'
    )
    
    if not connector.connect():
        print("[ERROR] 无法连接Neo4j")
        return 1
    
    try:
        query_relationships(connector)
        query_method_calls(connector)
        query_field_access(connector)
        query_api_endpoints(connector)
        
        print("\n" + "=" * 70)
        print("[OK] 查询完成")
        print("=" * 70)
        return 0
    finally:
        connector.disconnect()

if __name__ == "__main__":
    sys.exit(main())
"""
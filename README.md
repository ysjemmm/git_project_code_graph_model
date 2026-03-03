# Java AST Parser & Neo4j Exporter

一个强大的 Java 代码分析工具，能够解析 Java 源代码的 AST（抽象语法树），并将其导出到 Neo4j 图数据库进行深度分析。

## 功能特性

- 🔍 **完整的 Java AST 解析** - 支持类、接口、枚举、注解、记录等所有 Java 类型
- 📊 **嵌套类型支持** - 完整支持嵌套类、嵌套接口、嵌套枚举等
- 💬 **智能注释存储** - 混合策略存储注释（属性 + 独立节点），支持 Javadoc 解析
- 🔗 **关系分析** - 支持继承、实现、方法调用、字段访问等关系
- 📈 **Neo4j 导出** - 将代码结构导出为图数据库，支持复杂查询
- 🚀 **增量分析** - 支持基于 Merkle Tree 的 Git 增量分析，提高大型项目的处理效率
- 🔄 **多项目支持** - 支持同时分析多个项目，项目级别隔离

## 项目结构

```
.
├── core/                          # 核心导入和处理逻辑
│   ├── importer.py               # 主导入器
│   ├── task_queue.py             # 任务队列
│   └── __init__.py
├── parser/                        # 解析器模块
│   ├── languages/
│   │   └── java/                 # Java 语言支持
│   │       ├── analyzers/        # 各类型分析器
│   │       ├── core/             # 核心类型定义
│   │       └── utils/            # 工具类
│   ├── common/                   # 通用类型
│   ├── symbol_table_builder.py   # 符号表构建
│   └── __init__.py
├── storage/                       # 存储模块
│   ├── neo4j/                    # Neo4j 相关
│   │   ├── connector.py          # Neo4j 连接器
│   │   ├── exporter.py           # AST 导出器
│   │   ├── java_modules.py       # Java 节点定义
│   │   └── merge_builder.py      # MERGE 查询构建
│   └── cache/                    # 缓存模块
├── git/                           # Git 相关
│   ├── manager.py                # Git 管理器
│   └── incremental_analyzer.py   # 增量分析
├── tools/                         # 工具
│   └── ast_tool.py               # AST 工具
├── tests/                         # 测试
│   └── fixtures/                 # 测试数据
├── scripts/                       # 脚本
│   └── simple_import.py          # 简单导入示例
└── README.md
```

## 快速开始

### 安装依赖

```bash
pip install -r requirements.txt
```

### 基本使用

### 仅获取 AST 数据
```bash
python tests/fixtures/test_java_file.py
```

### AST -> Neo4j
```bash
python scripts/simple_import.py
```

## 核心概念

### AnalyzerContext

统一的参数传递载体，包含：
- `project_name`: 项目名称
- `project_path`: 项目路径
- `parser`: 解析器对象
- `before_uri_path`: REST 映射前缀

### 注释存储策略

- **<200字符**: 存为节点属性 `simple_comment`
- **包含Javadoc**: 创建独立 Comment 节点，解析 Javadoc 标签
- **>200字符或多条**: 创建长注释节点

### 嵌套类型支持

完整支持嵌套类型的递归处理，包括：
- 嵌套类
- 嵌套接口
- 嵌套枚举
- 嵌套注解
- 嵌套记录

深度限制为 50 层，防止栈溢出。

## Neo4j 图结构

### 节点类型

- `Project`: 项目节点
- `File`: Java 文件节点
- `JavaObject`: 类/接口/枚举/注解/记录节点
- `JavaMethod`: 方法节点
- `JavaField`: 字段节点
- `JavaMethodParameter`: 参数节点
- `JavaEnumConstant`: 枚举常量节点
- `Comment`: 注释节点

### 关系类型

- `HAVE`: 项目包含文件
- `CONTAINS`: 文件包含类型定义
- `MEMBER_OF`: 类型包含成员
- `EXTENDS`: 继承关系
- `IMPLEMENTS`: 实现关系
- `CALLS`: 方法调用关系
- `ACCESSES`: 字段访问关系
- `HAS_COMMENT`: 代码元素关联注释

## 查询示例

### 查询所有 Javadoc

```cypher
MATCH (c:Comment {comment_type: 'javadoc'})
RETURN c.javadoc_summary, c.javadoc_params
```

### 查询类的所有方法

```cypher
MATCH (obj:JavaObject {name: 'UserService'})-[:MEMBER_OF]->(m:JavaMethod)
RETURN m.name, m.return_type
```

### 查询方法调用链

```cypher
MATCH path = (m1:JavaMethod)-[:CALLS*]->(m2:JavaMethod)
WHERE m1.name = 'main'
RETURN path
```

## 配置

### 注释存储配置

在 `storage/neo4j/exporter.py` 中修改 `CommentStorageConfig`:

```python
class CommentStorageConfig:
    SHORT_COMMENT_THRESHOLD = 200      # 短注释阈值
    MULTIPLE_COMMENT_THRESHOLD = 3     # 多条注释阈值
    MULTIPLE_COMMENT_MIN_LENGTH = 100  # 多条注释最小总长度
```

## 开发

### 代码风格

- 使用 Enum 代替魔法值
- 使用 dataclass 代替 dict
- 类型注解完整
- 清晰的方法命名

## 性能优化

- **项目级别隔离**: AnalyzerCache 支持多项目并发分析
- **增量分析**: 支持 Git 增量分析，只处理变更文件
- **批量操作**: Neo4j 导出使用 UNWIND 批量创建节点和关系
- **深度限制**: 嵌套类型处理有 50 层深度限制

## 已知限制

- 暂不支持 Java 泛型的完整分析
- 方法调用分析仅支持直接调用，不支持反射
- 字段访问分析基于静态分析

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

MIT License

## 作者

mayYoung

## 更新日志

### v1.0.0 (2026-03-02)
- ✅ 完整的 Java AST 解析
- ✅ 嵌套类型支持
- ✅ 智能注释存储（方案A）
- ✅ Neo4j 导出
- ✅ 增量分析支持

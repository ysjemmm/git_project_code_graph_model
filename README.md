# Java AST Parser & Neo4j Exporter

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Python 3.8+](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![Neo4j](https://img.shields.io/badge/Neo4j-5.x-green.svg)](https://neo4j.com/)

一个强大的 Java 代码分析工具，能够解析 Java 源代码的 AST（抽象语法树），并将其导出到 Neo4j 图数据库进行深度分析。

支持完整的符号解析、类型推断和依赖分析。

[功能特性](#功能特性) • [快速开始](#快速开始) • [使用示例](#使用示例) • [文档](#相关文档)

</div>

---

## ✨ 亮点

- 🎯 **智能符号解析** - 自动识别类来源（项目内部/外部JAR/JDK标准库）
- 📦 **依赖分析** - 扫描并索引 Maven 依赖和 JDK 标准库
- 🔍 **完整 AST 解析** - 支持所有 Java 类型和嵌套结构
- 📊 **图数据库导出** - 将代码结构导出到 Neo4j，支持复杂查询
- 🚀 **增量分析** - 基于 Git 和文件修改时间的增量更新
- 🎭 **单例模式** - 每个项目独立的符号管理器实例

<img width="2672" height="1282" alt="Neo4j Graph Visualization" src="https://github.com/user-attachments/assets/828467ca-a932-4c12-952b-3b02216d892a" />


## 📋 功能特性

### 核心功能
- 🔍 **完整的 Java AST 解析** - 支持类、接口、枚举、注解、记录等所有 Java 类型
- 📊 **嵌套类型支持** - 完整支持嵌套类、嵌套接口、嵌套枚举等（支持任意深度，如 A.B.C）
- 💬 **智能注释存储** - 混合策略存储注释（属性 + 独立节点），支持 Javadoc 解析
- 🔗 **关系分析** - 支持继承、实现、方法调用、字段访问等关系
- 📈 **Neo4j 导出** - 将代码结构导出为图数据库，支持复杂查询
- 🚀 **增量分析** - 支持基于 Merkle Tree 的 Git 增量分析
- 🔄 **多仓库支持** - 支持同时分析多个项目，项目级别隔离

### 符号解析系统
- 🎯 **智能符号解析** - 自动解析类的来源（项目内部 / 外部 JAR / JDK 标准库）
- 📦 **依赖索引** - 扫描并索引 Maven 依赖和 JDK 标准库
- 🗄️ **项目索引** - 扫描并索引项目源代码的所有类
- 🔍 **完全限定名解析** - 支持包名、import、嵌套类等多种解析策略
- 🏷️ **Symbol ID 追踪** - 为每个类、方法、字段生成唯一的符号 ID
- 🔄 **增量更新** - 基于文件修改时间的增量扫描和 UPSERT 操作

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
│   │       ├── symbol/           # 符号解析系统（新增）
│   │       │   ├── symbol_manager.py      # 符号管理器（单例）
│   │       │   └── symbol_commons.py      # 符号通用类型
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
│   ├── sqlite/                   # SQLite 索引数据库（新增）
│   │   ├── jar_class_db.py       # JAR 类数据库
│   │   ├── jar_scanner.py        # JAR 扫描器
│   │   ├── project_class_db.py   # 项目类数据库
│   │   ├── project_scanner.py    # 项目扫描器
│   │   ├── class_name_parser.py  # 类名解析器
│   │   └── scan_maven_jars.py    # Maven JAR 扫描脚本
│   └── cache/                    # 缓存模块
├── git/                           # Git 相关
│   ├── manager.py                # Git 管理器
│   └── incremental_analyzer.py   # 增量分析
├── tools/                         # 工具
│   ├── ast_tool.py               # AST 工具
│   └── constants.py              # 常量定义
├── docs/                          # 文档（新增）
│   ├── symbol_manager_data_flow.md        # 符号管理器数据流
│   ├── database_upsert_behavior.md        # 数据库 UPSERT 行为
│   └── parse_java_object_where_api_update.md  # 符号解析 API 更新
├── tests/                         # 测试
│   └── fixtures/                 # 测试数据
├── scripts/                       # 脚本
│   ├── simple_import.py          # 简单导入示例
│   ├── scan_esign_project.py     # 项目扫描脚本
│   └── query_project_db.py       # 数据库查询脚本
└── README.md
```

## 🚀 快速开始

### 前置要求

- Python 3.8+
- Neo4j 5.x（可选，用于图数据库导出）
- Java 8+（用于分析 Java 项目）

### 安装

```bash
# 克隆仓库
git clone https://github.com/yourusername/java-ast-parser.git
cd java-ast-parser

# 安装依赖
pip install -r requirements.txt
```

### 基本使用

#### 1. 构建依赖索引

```bash
# 扫描 Maven 依赖
python scripts/scan_maven_jars.py

# 构建 JDK 索引（可选，但推荐）
python scripts/build_jdk_index.py --auto-detect
```

#### 2. 分析项目

```bash
# 扫描项目源代码
python scripts/scan_esign_project.py

# 查询类信息
python scripts/query_project_db.py
```

#### 3. 导出到 Neo4j

```bash
# 导出 AST 到 Neo4j
python scripts/simple_import.py
```

## 📚 项目结构

## 💡 核心概念

统一的参数传递载体，包含：
- `project_name`: 项目名称
- `project_path`: 项目路径
- `parser`: 解析器对象
- `before_uri_path`: REST 映射前缀

### 符号解析系统

#### SymbolManager（符号管理器）

每个项目的单例实例，负责解析类的来源：

```python
# 获取项目的 SymbolManager 实例
manager = SymbolManager.get_instance("my-project")

# 解析类的来源
location = manager.parse_java_object_where(
    identifier="User",              # 类名
    java_file_structure=structure,  # 文件结构
    project_name="my-project"       # 项目名称
)

# 返回 ClassLocation 对象
# - type: INTERNAL（项目内部）/ EXTERNAL（外部 JAR）/ UNKNOWN（未知）
# - fqn: 完全限定名（例如 com.example.User）
# - jar_path: JAR 路径（仅外部类）
# - file_path: 文件路径（仅内部类）
# - symbol_id: 符号 ID（仅内部类）
# - resolution_method: 解析方法
```

#### Symbol ID 生成规则

项目的 Symbol ID 包含项目类型和版本信息，确保不同类型和版本的项目节点唯一：

```python
# Application 类型项目
project#my-project@Application

# Lib 类型项目（带版本）
project#spring-core@Lib@5.3.0

# Lib 类型项目（无版本）
project#commons-lang@Lib
```

**格式规则**：
- 基础格式：`project#{project_name}@{project_type}`
- 带版本：`project#{project_name}@{project_type}@{version}`
- `project_type` 可以是 `Application` 或 `Lib`

**节点关系**：
- **Application 项目** → `HAVE` → 文件节点 → `CONTAINS` → 内部定义的类
- **Lib 项目** → `CONTAINS_LIB` → 外部定义的类
- 同名项目的 Application 和 Lib 节点可以共存，通过不同的 Symbol ID 区分

#### 解析优先级

按照 Java/IDE 规范的解析顺序：

1. **同包类**（项目内部）
2. **显式 import**（项目内部）
3. **同包类**（外部 JAR）
4. **显式 import**（外部 JAR）
5. **通配符 import**（项目内部 → 外部 JAR）
6. **java.lang 包**（外部 JAR）
7. **未知**

#### 嵌套类解析

支持任意深度的嵌套类解析：

```java
// 支持的格式
class A extends B.C { }              // 嵌套类
class A extends B.C.D { }            // 多层嵌套
class A extends com.example.B.C { }  // 完全限定名 + 嵌套
class A extends A.B { }              // 自引用嵌套
```

解析策略：
1. 检测是否包含 `.`
2. 判断第一部分是否为包名（小写开头）
3. 如果是类名，递归解析外层类
4. 拼接嵌套路径并查询数据库

### SQLite 索引数据库

#### JAR 类数据库（jar_classes.db）

存储所有外部依赖的类信息：

```sql
CREATE TABLE jar_classes (
    id INTEGER PRIMARY KEY,
    fqn TEXT UNIQUE NOT NULL,           -- 完全限定名
    simple_name TEXT NOT NULL,          -- 简单名称
    package_name TEXT NOT NULL,         -- 包名
    jar_path TEXT NOT NULL,             -- JAR 路径
    is_anonymous BOOLEAN NOT NULL,      -- 是否匿名类
    is_nested BOOLEAN NOT NULL,         -- 是否嵌套类
    parent_class TEXT,                  -- 父类 FQN
    insert_time TEXT NOT NULL,          -- 插入时间
    last_scan_time TEXT NOT NULL        -- 最后扫描时间
);
```

#### 项目类数据库（project_classes.db）

存储项目源代码的类信息：

```sql
CREATE TABLE project_classes (
    id INTEGER PRIMARY KEY,
    fqn TEXT NOT NULL,                  -- 完全限定名
    simple_name TEXT NOT NULL,          -- 简单名称
    package_name TEXT NOT NULL,         -- 包名
    project_name TEXT NOT NULL,         -- 项目名称
    file_path TEXT NOT NULL,            -- 源文件路径
    relative_path TEXT NOT NULL,        -- 相对路径
    is_anonymous BOOLEAN NOT NULL,      -- 是否匿名类
    is_nested BOOLEAN NOT NULL,         -- 是否嵌套类
    parent_class TEXT,                  -- 父类 FQN
    symbol_id TEXT NOT NULL,            -- 符号 ID
    parent_symbol_id TEXT,              -- 父符号 ID
    insert_time TEXT NOT NULL,          -- 插入时间
    last_modified TEXT NOT NULL,        -- 文件修改时间
    UNIQUE(project_name, fqn)           -- 项目 + FQN 唯一
);
```

#### Symbol ID 层级结构

- **文件**: 有 `symbol_id`
- **顶层类**: `parent_symbol_id` 指向文件的 `symbol_id`
- **嵌套类**: `parent_symbol_id` 指向父类的 `symbol_id`

#### UPSERT 行为

两个数据库都使用 `INSERT OR REPLACE` 实现 UPSERT：

- **jar_classes**: `UNIQUE(fqn)` 约束
- **project_classes**: `UNIQUE(project_name, fqn)` 约束

增量更新基于文件修改时间，只更新变更的文件。

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

## 🗂️ Neo4j 图结构

### 节点类型

- `Project`: 项目节点（区分 Application 和 Lib 类型）
- `File`: Java 文件节点
- `JavaObject`: 类/接口/枚举/注解/记录节点（区分内部定义和外部定义）
- `JavaMethod`: 方法节点
- `JavaField`: 字段节点
- `JavaMethodParameter`: 参数节点
- `JavaEnumConstant`: 枚举常量节点
- `Comment`: 注释节点

**JavaObject 的 from_type 属性**：
- `InnerDefinition`: 项目内部定义的类
- `ExternalDefinition`: 外部 JAR 中的类
- `JdkDefinition`: JDK 标准库中的类
- `NestedDefinition`: 嵌套类
- `UnknownDefinition`: 未知来源的类

### 关系类型

- `HAVE`: 项目包含文件（仅 Application 类型项目）
- `CONTAINS`: 文件包含类型定义
- `CONTAINS_LIB`: Lib 项目包含外部定义的类
- `MEMBER_OF`: 类型包含成员
- `EXTENDS`: 继承关系
- `IMPLEMENTS`: 实现关系
- `CALLS`: 方法调用关系
- `ACCESSES`: 字段访问关系
- `HAS_COMMENT`: 代码元素关联注释
- `LIB_LINK`: 外部定义链接到内部实现（JAR 中的类链接到源码中的类）

## 📖 使用示例

### 1. 扫描 Maven JAR 依赖

```python
from storage.sqlite import get_jar_class_db, JARScanner
from tools.constants import PROJECT_ROOT_PATH

# 初始化数据库
jar_db = get_jar_class_db()
jar_db.initialize_schema()

# 创建扫描器
scanner = JARScanner(jar_db)

# 扫描 Maven 缓存目录
maven_cache = PROJECT_ROOT_PATH / ".cache" / "maven"
result = scanner.scan_directory(str(maven_cache))

print(f"扫描了 {result.jars_scanned} 个 JAR")
print(f"提取了 {result.total_classes} 个类")
```

### 2. 扫描项目源代码

```python
from storage.sqlite import get_project_class_db, ProjectScanner
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer

# 初始化数据库
project_db = get_project_class_db()
project_db.initialize_schema()
scanner = ProjectScanner(project_db)

# 分析 Java 文件
analyzer = JavaFileAnalyzer(context=context, file_path=file_path)
java_file_structure = analyzer.analyze_file()

# 扫描到数据库
count = scanner.scan_file(
    project_name="my-project",
    java_file_structure=java_file_structure
)
```

### 3. 解析类的来源

```python
from parser.languages.java.symbol.symbol_manager import SymbolManager

# 获取 SymbolManager 实例（单例）
manager = SymbolManager.get_instance("my-project")

# 解析类的来源
location = manager.parse_java_object_where(
    identifier="ArrayList",
    java_file_structure=structure,
    project_name="my-project"
)

# 检查结果
if location.type == ClassLocationType.EXTERNAL:
    print(f"外部类: {location.fqn}")
    print(f"来自 JAR: {location.jar_path}")
elif location.type == ClassLocationType.INTERNAL:
    print(f"内部类: {location.fqn}")
    print(f"文件路径: {location.file_path}")
    print(f"Symbol ID: {location.symbol_id}")
else:
    print(f"未知类: {location.fqn}")
```

### 4. 查询数据库

```python
from storage.sqlite import get_project_class_db

db = get_project_class_db()

# 按 FQN 查询
cls = db.query_by_fqn("com.example.User", "my-project")
print(f"类名: {cls.simple_name}")
print(f"Symbol ID: {cls.symbol_id}")
print(f"父 Symbol ID: {cls.parent_symbol_id}")

# 查询嵌套类
nested = db.query_nested_classes("com.example.User", "my-project")
for n in nested:
    print(f"嵌套类: {n.fqn}")
```

## 🔍 Neo4j 查询示例

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

### 查询类的继承关系

```cypher
MATCH (child:JavaObject)-[:EXTENDS]->(parent:JavaObject)
WHERE child.name = 'UserService'
RETURN parent.qualified_name, parent.from_type
```

### 查询外部依赖

```cypher
MATCH (obj:JavaObject {from_type: 'ExternalDefinition'})
RETURN obj.qualified_name, obj.belong_project
ORDER BY obj.belong_project
```

### 查询项目节点

```cypher
// 查询所有 Application 类型项目
MATCH (p:Project {project_type: 'Application'})
RETURN p.name, p.symbol_id, p.version

// 查询所有 Lib 类型项目
MATCH (p:Project {project_type: 'Lib'})
RETURN p.name, p.symbol_id, p.version
ORDER BY p.name

// 查询同名项目的不同类型节点
MATCH (p:Project)
WHERE p.name = 'epaas-gateway'
RETURN p.project_type, p.version, p.symbol_id
```

### 查询外部类链接

```cypher
// 查询外部定义链接到内部实现的类
MATCH (external:JavaObject {from_type: 'ExternalDefinition'})-[:LIB_LINK]->(internal:JavaObject {from_type: 'InnerDefinition'})
RETURN external.qualified_name, external.belong_project, internal.belong_file
LIMIT 10
```

## ⚙️ 配置

### 注释存储配置

在 `storage/neo4j/exporter.py` 中修改 `CommentStorageConfig`:

```python
class CommentStorageConfig:
    SHORT_COMMENT_THRESHOLD = 200      # 短注释阈值
    MULTIPLE_COMMENT_THRESHOLD = 3     # 多条注释阈值
    MULTIPLE_COMMENT_MIN_LENGTH = 100  # 多条注释最小总长度
```

## 🛠️ 开发

### 代码风格

- 使用 Enum 代替魔法值
- 使用 dataclass 代替 dict
- 类型注解完整
- 清晰的方法命名

## ⚡ 性能优化

- **项目级别隔离**: AnalyzerCache 支持多项目并发分析
- **增量分析**: 支持 Git 增量分析，只处理变更文件
- **批量操作**: Neo4j 导出使用 UNWIND 批量创建节点和关系
- **深度限制**: 嵌套类型处理有 50 层深度限制
- **单例模式**: SymbolManager 每个项目单例，避免重复初始化
- **SQLite 索引**: 使用 SQLite 数据库索引类信息，快速查询
- **增量扫描**: 基于文件修改时间的增量更新，跳过未修改文件
- **WAL 模式**: SQLite 启用 WAL 模式，支持并发读写
- **批量插入**: 使用 executemany 批量插入数据，提高性能

## ⚠️ 已知限制

- 暂不支持 Java 泛型的完整分析
- 方法调用分析仅支持直接调用，不支持反射
- 字段访问分析基于静态分析
- JDK 标准库类需要手动扫描 rt.jar 或 modules 文件才能识别
- 匿名类默认不包含在索引中（可通过 `include_anonymous=True` 启用）

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👤 作者

**mayYoung**

- GitHub: [@mayYoung](https://github.com/mayYoung)

## 🙏 致谢

感谢所有为本项目做出贡献的开发者！

## 📝 更新日志

### v2.1.0 (2026-03-09)
- ✅ **项目节点 Symbol ID 优化** - Symbol ID 包含项目类型和版本信息
- ✅ **项目类型区分** - Application 和 Lib 类型项目完全独立
- ✅ **关系优化** - Application 项目使用 HAVE 关系，Lib 项目使用 CONTAINS_LIB 关系
- ✅ **外部类链接** - 支持外部定义链接到内部实现（LIB_LINK 关系）
- ✅ **批量链接优化** - 使用批量查询优化外部类链接性能

### v2.0.0 (2026-03-05)
- ✅ **符号解析系统** - 完整的类来源解析（内部/外部/未知）
- ✅ **SQLite 索引数据库** - JAR 类和项目类的快速索引
- ✅ **嵌套类解析** - 支持任意深度的嵌套类（A.B.C 格式）
- ✅ **Symbol ID 追踪** - 完整的符号 ID 层级结构
- ✅ **单例模式** - SymbolManager 每个项目单例
- ✅ **增量扫描** - 基于文件修改时间的增量更新
- ✅ **UPSERT 支持** - 使用 INSERT OR REPLACE 实现数据更新
- ✅ **完整文档** - 添加数据流、UPSERT 行为等文档

### v1.0.0 (2026-03-02)
- ✅ 完整的 Java AST 解析
- ✅ 嵌套类型支持
- ✅ 智能注释存储（方案A）
- ✅ Neo4j 导出
- ✅ 增量分析支持

## 📚 相关文档

- [符号管理器数据流](docs/symbol_manager_data_flow.md) - SymbolManager 的工作原理和使用方式
- [数据库 UPSERT 行为](docs/database_upsert_behavior.md) - SQLite 数据库的 UPSERT 实现
- [JDK 索引构建指南](docs/jdk_index_guide.md) - 如何构建和使用 JDK 标准库索引

---

<div align="center">

**如果这个项目对你有帮助，请给它一个 ⭐️**

Made with ❤️ by mayYoung

</div>

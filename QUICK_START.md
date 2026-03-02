# 快速开始指南

## 🚀 5分钟快速上手

### 1. 克隆仓库

```bash
git clone https://github.com/YOUR_USERNAME/java-ast-parser.git
cd java-ast-parser
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置 Neo4j

```bash
# 启动 Neo4j（使用 Docker）
docker run -d \
  --name neo4j \
  -p 7687:7687 \
  -p 7474:7474 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:latest
```

### 4. 运行示例

```bash
# 简单导入示例
python scripts/simple_import.py

# 或运行测试
python tests/fixtures/test_java_file.py
```

## 📚 常用命令

### 分析单个 Java 文件

```python
from parser.languages.java.utils.analyzer_context import AnalyzerContext
from parser.languages.java.analyzers.ast_java_file_analyzer import JavaFileAnalyzer
from parser.utils.optional_imports import Parser

# 创建上下文
context = AnalyzerContext(
    project_name="MyProject",
    project_path="/path/to/project",
    parser=Parser("java")
)

# 分析文件
analyzer = JavaFileAnalyzer(
    context=context,
    file_path="/path/to/MyFile.java"
)

result = analyzer.analyze_file()
print(result)
```

### 导入 Git 仓库到 Neo4j

```python
from core.importer import Importer
from storage.neo4j.connector import Neo4jConnector

# 连接 Neo4j
connector = Neo4jConnector(
    uri="bolt://localhost:7687",
    user="neo4j",
    password="password"
)

# 导入仓库
importer = Importer(connector)
result = importer.import_from_git(
    repo_url="https://github.com/user/repo.git",
    project_name="MyProject"
)

print(f"导入完成: {result}")
```

## 🔍 Neo4j 查询示例

### 查询所有类

```cypher
MATCH (obj:JavaObject {object_type: 'classType'})
RETURN obj.name, obj.simple_comment
LIMIT 10
```

### 查询类的所有方法

```cypher
MATCH (obj:JavaObject {name: 'UserService'})-[:MEMBER_OF]->(m:JavaMethod)
RETURN m.name, m.return_type, m.simple_comment
```

### 查询 Javadoc

```cypher
MATCH (c:Comment {comment_type: 'javadoc'})
RETURN c.javadoc_summary, c.javadoc_params
LIMIT 5
```

### 查询方法调用关系

```cypher
MATCH (m1:JavaMethod)-[:CALLS]->(m2:JavaMethod)
RETURN m1.name, m2.name
LIMIT 20
```

## 🛠️ 开发

### 项目结构

```
java-ast-parser/
├── core/              # 核心导入逻辑
├── parser/            # AST 解析器
├── storage/           # Neo4j 存储
├── git/               # Git 操作
├── tools/             # 工具类
├── tests/             # 测试
├── scripts/           # 脚本
├── README.md          # 项目文档
├── requirements.txt   # 依赖
└── QUICK_START.md     # 本文件
```

### 添加新的分析器

1. 在 `parser/languages/java/analyzers/` 创建新文件
2. 继承 `BaseAnalyzer`
3. 实现 `handle_*` 方法
4. 在 `AnalyzerCache` 中注册

### 运行测试

```bash
# 运行所有测试
pytest tests/

# 运行特定测试
pytest tests/fixtures/test_java_file.py

# 显示覆盖率
pytest --cov=parser tests/
```

## 📖 文档

- [README.md](README.md) - 项目概述
- [GITHUB_SETUP.md](GITHUB_SETUP.md) - GitHub 设置指南
- [TODO.md](TODO.md) - 待办事项

## 🐛 常见问题

### Q: 如何修改 Neo4j 连接信息？

A: 在代码中创建 `Neo4jConnector` 时修改参数：

```python
connector = Neo4jConnector(
    uri="bolt://your-host:7687",
    user="your-user",
    password="your-password"
)
```

### Q: 如何分析多个项目？

A: 使用不同的 `project_name` 创建多个 `AnalyzerContext`：

```python
for project in projects:
    context = AnalyzerContext(
        project_name=project['name'],
        project_path=project['path'],
        parser=Parser("java")
    )
    # 分析...
```

### Q: 如何处理大型项目？

A: 使用增量分析：

```python
from parser.incremental_analyzer import IncrementalAnalyzer

analyzer = IncrementalAnalyzer(connector)
result = analyzer.analyze_incremental(
    repo_url="...",
    project_name="..."
)
```

## 📞 获取帮助

- 查看 [Issues](https://github.com/YOUR_USERNAME/java-ast-parser/issues)
- 提交 [Pull Request](https://github.com/YOUR_USERNAME/java-ast-parser/pulls)
- 阅读 [文档](README.md)

## 📝 许可证

MIT License - 详见 [LICENSE](LICENSE)

---

**祝你使用愉快！** 🎉

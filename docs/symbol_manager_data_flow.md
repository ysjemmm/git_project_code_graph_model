# 符号管理器数据流说明

## 单例模式（重要更新）

**每个项目的 SymbolManager 都是唯一实例！**

### 使用方式

```python
# 方式 1: 使用构造函数（推荐）
manager = SymbolManager(project_name="my-project")

# 方式 2: 使用 get_instance 方法
manager = SymbolManager.get_instance("my-project")

# 同一个项目多次创建，返回同一个实例
manager1 = SymbolManager(project_name="my-project")
manager2 = SymbolManager(project_name="my-project")
assert manager1 is manager2  # True

# 不同项目返回不同实例
manager_a = SymbolManager(project_name="project-a")
manager_b = SymbolManager(project_name="project-b")
assert manager_a is not manager_b  # True
```

### 清除实例（测试或重置时使用）

```python
# 清除单个项目的实例
SymbolManager.clear_instance("my-project")

# 清除所有实例
SymbolManager.clear_all_instances()
```

## 当前设计

### 初始化
```python
manager = SymbolManager(
    project_name="my-project",
    use_global_db=True,      # 是否使用全局数据库
    auto_sync_db=True        # 是否启用自动同步（初始化 ProjectScanner）
)
```

### 数据库支持

SymbolManager 自动加载三个数据库：

1. **项目类数据库** (`project_classes.db`) - 项目源代码的类
2. **JAR 类数据库** (`jar_classes.db`) - Maven 依赖的类
3. **JDK 类数据库** (`jdk{version}_classes.db`) - JDK 标准库的类（可选）

### 数据插入时机

**重要：`auto_sync_db=True` 不会自动插入数据！**

它只是：
1. 初始化数据库 schema
2. 创建 `ProjectScanner` 实例
3. 准备好同步的基础设施

**实际插入数据需要手动调用：**

```python
# 1. 分析 Java 文件
java_file_structure = analyzer.analyze_file()

# 2. 手动调用 collect_from_java_file 插入数据
manager.collect_from_java_file(project_name, java_file_structure)
```

### 完整流程

```python
# 步骤 1: 创建 SymbolManager
manager = SymbolManager(project_name="my-project", auto_sync_db=True)

# 步骤 2: 分析 Java 文件
analyzer = JavaFileAnalyzer(context=context, file_path=file_path)
java_file_structure = analyzer.analyze_file()

# 步骤 3: 插入数据到数据库（手动调用）
manager.collect_from_java_file("my-project", java_file_structure)

# 步骤 4: 查询符号位置
location = manager.parse_java_object_where("SomeClass", java_file_structure, "my-project")
```

## 数据流图

```
JavaFileAnalyzer.analyze_file()
        ↓
JavaFileStructure (内存对象)
        ↓
manager.collect_from_java_file()  ← 手动调用
        ↓
ProjectScanner.scan_file()
        ↓
提取类信息 (ProjectClassInfo)
        ↓
ProjectClassDB.batch_insert_classes()
        ↓
SQLite 数据库 (持久化)
```

## 查询流程

```
manager.parse_java_object_where("ClassName", java_file_structure)
        ↓
解析类名（处理嵌套类、包名等）
        ↓
查询数据库（按优先级）
  ├─ project_db.query_by_fqn()  (项目内部类)
  ├─ jar_db.query_by_fqn()      (外部 JAR 类)
  └─ jdk_db.query_by_fqn()      (JDK 标准库类)
        ↓
返回 ClassLocation (类型、FQN、来源)
```

## ClassLocation 类型

```python
class ClassLocationType(Enum):
    INTERNAL = "INTERNAL"  # 项目内部类
    EXTERNAL = "EXTERNAL"  # 外部 JAR 类
    JDK = "JDK"           # JDK 标准库类
    UNKNOWN = "UNKNOWN"    # 未知类
```

## 解析优先级

1. 项目内部类（同包）
2. 项目内部类（显式 import）
3. 外部 JAR 类（同包）
4. 外部 JAR 类（显式 import）
5. 外部 JAR 类（通配符 import）
6. **JDK 类（同包）**
7. **JDK 类（显式 import）**
8. **JDK 类（通配符 import）**
9. **JDK 类（java.lang 包）**
10. 未知

## 使用场景

### 场景 1: 批量扫描项目（推荐）
使用 `ProjectScanner` 直接扫描，不需要 SymbolManager：

```python
from storage.sqlite import get_project_class_db, ProjectScanner

db = get_project_class_db()
scanner = ProjectScanner(db)

# 扫描文件
scanner.scan_file(project_name, java_file_structure)
```

### 场景 2: 分析 + 查询（使用 SymbolManager）
需要同时插入数据和查询符号位置：

```python
manager = SymbolManager(project_name="my-project", auto_sync_db=True)

# 分析文件
java_file_structure = analyzer.analyze_file()

# 插入数据
manager.collect_from_java_file(project_name, java_file_structure)

# 查询符号
location = manager.parse_java_object_where("ClassName", java_file_structure)

# 检查类型
if location.type == ClassLocationType.JDK:
    print(f"JDK 类: {location.fqn}")
elif location.type == ClassLocationType.EXTERNAL:
    print(f"外部 JAR 类: {location.fqn}")
elif location.type == ClassLocationType.INTERNAL:
    print(f"项目内部类: {location.fqn}")
```

### 场景 3: 只查询（不插入）
如果数据库已经有数据，只需要查询：

```python
manager = SymbolManager(project_name="my-project", auto_sync_db=False)

# 直接查询（不会插入数据）
location = manager.parse_java_object_where("ClassName", java_file_structure)
```

## 参数说明

### `auto_sync_db` 参数

- `True`: 初始化 `ProjectScanner`，准备好同步基础设施
- `False`: 不初始化 `ProjectScanner`，只能查询，不能插入

**注意：** 即使 `auto_sync_db=True`，也需要手动调用 `collect_from_java_file()` 才会插入数据！

### `use_global_db` 参数

- `True`: 使用全局单例数据库（推荐）
- `False`: 不使用数据库，只能做内存操作

## JDK 索引支持

### 自动加载

如果存在 JDK 索引数据库，SymbolManager 会自动加载：

```python
manager = SymbolManager.get_instance("my-project")

# 查询 JDK 类
location = manager.parse_java_object_where("String", java_file_structure)
# location.type == ClassLocationType.JDK
# location.fqn == "java.lang.String"
```

### 构建 JDK 索引

```bash
# 自动检测 JDK 路径并构建索引
python scripts/build_jdk_index.py --auto-detect

# 或指定 JDK 路径
python scripts/build_jdk_index.py --jdk-home /path/to/jdk
```

详见：[JDK 索引构建指南](jdk_index_guide.md)

## 总结

- ✅ `auto_sync_db=True` 只是准备基础设施
- ✅ 实际插入需要手动调用 `collect_from_java_file()`
- ✅ 支持三种类型：INTERNAL、EXTERNAL、JDK
- ✅ JDK 索引可选，自动加载
- ✅ 这样设计给用户更多控制权

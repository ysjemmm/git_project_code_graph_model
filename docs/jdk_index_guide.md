# JDK 索引构建指南

## 概述

JDK 索引用于解析 Java 标准库类（如 `String`、`ArrayList`、`HashMap` 等），避免这些常用类被标记为 UNKNOWN。

## 快速开始

### 1. 自动检测并构建

```bash
python scripts/build_jdk_index.py --auto-detect
```

这会自动检测系统的 JDK 路径并构建索引。

### 2. 指定 JDK 路径

```bash
python scripts/build_jdk_index.py --jdk-home /path/to/jdk
```

### 3. 指定 JDK 版本

```bash
python scripts/build_jdk_index.py --jdk-home /path/to/jdk --jdk-version 17
```

这会生成 `jdk17_classes.db` 文件。

## 支持的 JDK 版本

### JDK 8
- 扫描 `$JAVA_HOME/jre/lib/rt.jar`
- 扫描 `$JAVA_HOME/jre/lib/*.jar`
- 扫描 `$JAVA_HOME/jre/lib/ext/*.jar`

### JDK 9+
- 扫描 `$JAVA_HOME/jmods/*.jmod`

## 输出文件

默认输出到 `.cache/` 目录：

- `jdk8_classes.db` - JDK 8 索引
- `jdk11_classes.db` - JDK 11 索引
- `jdk17_classes.db` - JDK 17 索引
- `jdk21_classes.db` - JDK 21 索引

## 使用索引

### 自动加载

`SymbolManager` 会自动查找并加载 JDK 索引：

```python
from parser.languages.java.symbol.symbol_manager import SymbolManager

# 自动加载 JDK 索引（如果存在）
manager = SymbolManager.get_instance("my-project")

# 解析 JDK 类
location = manager.parse_java_object_where("String", java_file_structure)

# 检查结果
if location.type == ClassLocationType.JDK:
    print(f"JDK 类: {location.fqn}")  # java.lang.String
```

### 检查是否有 JDK 索引

```python
from storage.sqlite import has_jdk_index, get_jdk_class_db

# 检查是否存在 JDK 索引
if has_jdk_index():
    print("JDK 索引已存在")
    
    # 获取 JDK 数据库
    jdk_db = get_jdk_class_db()
    
    # 查询类
    cls = jdk_db.query_by_fqn("java.lang.String")
    print(f"类名: {cls.simple_name}")
else:
    print("JDK 索引不存在，请运行构建脚本")
```

### 查看可用版本

```python
from storage.sqlite.jdk_class_db import JDKClassDB

versions = JDKClassDB.get_available_versions()
print(f"可用的 JDK 版本: {versions}")  # [21, 17, 11, 8]
```

## 解析优先级

添加 JDK 索引后，解析优先级变为：

1. 项目内部类（同包）
2. 项目内部类（显式 import）
3. 外部 JAR 类（同包）
4. 外部 JAR 类（显式 import）
5. 外部 JAR 类（通配符 import）
6. **JDK 类（同包）** ← 新增
7. **JDK 类（显式 import）** ← 新增
8. **JDK 类（通配符 import）** ← 新增
9. **JDK 类（java.lang 包）** ← 新增
10. 未知

## ClassLocation 类型

添加了新的类型 `JDK`：

```python
class ClassLocationType(Enum):
    INTERNAL = "INTERNAL"  # 项目内部类
    EXTERNAL = "EXTERNAL"  # 外部 JAR 类
    JDK = "JDK"           # JDK 标准库类 ← 新增
    UNKNOWN = "UNKNOWN"    # 未知类
```

## 性能数据

### JDK 8
- 文件数: ~20 个 JAR
- 类数: ~20,000 个
- 数据库大小: ~15 MB
- 构建时间: ~5 秒

### JDK 17
- 文件数: ~70 个 JMOD
- 类数: ~6,000 个
- 数据库大小: ~5 MB
- 构建时间: ~3 秒

## 常见问题

### Q: 为什么需要 JDK 索引？

A: 没有 JDK 索引时，所有 JDK 标准库类（如 `String`、`ArrayList`）都会被标记为 UNKNOWN，影响依赖分析的准确性。

### Q: 必须构建 JDK 索引吗？

A: 不是必须的。如果不构建，系统仍然可以正常工作，只是 JDK 类会被标记为 UNKNOWN。

### Q: 可以同时使用多个 JDK 版本的索引吗？

A: 目前 `SymbolManager` 只会加载一个 JDK 索引（优先级：21 > 17 > 11 > 8）。如果需要特定版本，可以删除其他版本的数据库文件。

### Q: JDK 索引需要更新吗？

A: 通常不需要。JDK 标准库的类很少变化。只有在升级 JDK 主版本时才需要重新构建。

### Q: 构建失败怎么办？

A: 检查以下几点：
1. JDK 路径是否正确
2. 是否有读取权限
3. 磁盘空间是否足够
4. Python 依赖是否完整

## 示例

### 完整工作流

```bash
# 1. 构建 JDK 索引
python scripts/build_jdk_index.py --auto-detect

# 2. 扫描 Maven 依赖
python scripts/scan_maven_jars.py

# 3. 扫描项目
python scripts/scan_esign_project.py

# 4. 查询类来源
python scripts/query_project_db.py
```

### 验证 JDK 索引

```python
from storage.sqlite import get_jdk_class_db

jdk_db = get_jdk_class_db()

# 测试常用类
test_classes = [
    "java.lang.String",
    "java.lang.Object",
    "java.util.ArrayList",
    "java.util.HashMap",
    "java.io.File",
    "java.time.LocalDate",
]

for fqn in test_classes:
    cls = jdk_db.query_by_fqn(fqn)
    if cls:
        print(f"✓ {fqn}")
    else:
        print(f"✗ {fqn} 未找到")
```

## 高级用法

### 自定义输出路径

```bash
python scripts/build_jdk_index.py \
    --jdk-home /path/to/jdk \
    --output /custom/path/jdk_classes.db
```

### 强制重新扫描

```bash
python scripts/build_jdk_index.py --auto-detect --force
```

### 只扫描特定 JAR

```python
from storage.sqlite import get_jdk_class_db, JARScanner

jdk_db = get_jdk_class_db()
scanner = JARScanner(jdk_db)

# 只扫描 rt.jar
scanner.scan_jar("/path/to/jdk/jre/lib/rt.jar")
```

## 总结

- ✅ 预先构建 JDK 索引，性能最优
- ✅ 支持 JDK 8 和 JDK 9+ 的不同结构
- ✅ 自动检测和加载
- ✅ 新增 `ClassLocationType.JDK` 类型
- ✅ 完整的解析优先级支持

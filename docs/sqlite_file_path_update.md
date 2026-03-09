# SQLite 数据库更新：添加 file_path 字段

## 概述

为 SQLite 数据库中的 `jar_classes` 表添加了 `file_path` 字段，用于存储类在 JAR 文件中的完整路径。

## 修改内容

### 1. ClassInfo 数据类更新

**文件**: `storage/sqlite/jar_class_db.py`

在 `ClassInfo` 数据类中添加了 `file_path` 字段：

```python
@dataclass
class ClassInfo:
    fqn: str                    # 完全限定名
    simple_name: str            # 简单名称
    package_name: str           # 包名
    jar_name: str               # JAR文件名
    jar_path: str               # JAR文件完整路径
    is_anonymous: bool          # 是否为匿名类
    insert_time: str            # 插入时间
    file_path: Optional[str] = None  # 类在 JAR 中的文件路径 ✨ 新增
    # ... 其他字段
```

### 2. 数据库表结构更新

**文件**: `storage/sqlite/jar_class_db.py`

在 `jar_classes` 表中添加了 `file_path` 列：

```sql
CREATE TABLE IF NOT EXISTS jar_classes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fqn TEXT NOT NULL UNIQUE,
    simple_name TEXT NOT NULL,
    package_name TEXT NOT NULL,
    jar_name TEXT NOT NULL,
    jar_path TEXT NOT NULL,
    is_anonymous BOOLEAN NOT NULL,
    insert_time TEXT NOT NULL,
    file_path TEXT,  -- ✨ 新增列
    parent_artifact_id TEXT,
    parent_group_id TEXT,
    parent_version TEXT,
    artifact_id TEXT,
    artifact_group_id TEXT,
    artifact_version TEXT
)
```

### 3. 批量插入方法更新

**文件**: `storage/sqlite/jar_class_db.py`

更新了 `batch_insert_classes` 方法以包含 `file_path` 字段：

```python
cursor.executemany("""
    INSERT OR REPLACE INTO jar_classes 
    (fqn, simple_name, package_name, jar_name, jar_path, is_anonymous, 
     insert_time, file_path, parent_artifact_id, parent_group_id, 
     parent_version, artifact_id, artifact_group_id, artifact_version)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""", [...])
```

### 4. 查询方法更新

**文件**: `storage/sqlite/jar_class_db.py`

- 更新了 `_row_to_classinfo` 方法以读取 `file_path` 字段
- 更新了 `_get_select_fields` 方法以包含 `file_path` 在可选字段列表中

### 5. JAR 扫描器更新

**文件**: `storage/sqlite/jar_scanner.py`

在扫描 JAR 文件时保存类的文件路径：

```python
class_info = ClassInfo(
    fqn=fqn,
    simple_name=simple_name,
    package_name=package_name,
    jar_name=jar_name,
    jar_path=jar_path,
    is_anonymous=is_anonymous,
    insert_time=current_time,
    file_path=file_info,  # ✨ 保存文件路径
    # ... 其他字段
)
```

## file_path 字段说明

### 用途

`file_path` 字段存储类在 JAR 文件中的完整路径，例如：

- `com/example/User.class`
- `com/example/model/Order.class`
- `org/springframework/boot/SpringApplication.class`

### 与 ClassLocation 的关系

`file_path` 字段对应 `ClassLocation` 类中的 `file_path` 属性：

```python
@dataclass
class ClassLocation:
    type: ClassLocationType     # 类型：内部/外部/未知
    fqn: Optional[str]          # 完全限定名
    jar_path: Optional[str]     # JAR 路径（仅外部类）
    file_path: Optional[str]    # 文件路径 ✨
    resolution_method: str      # 解析方法
    symbol_id: Optional[str]    # 符号 ID
    # ... Maven 信息
```

### 使用场景

1. **精确定位类文件**: 知道类在 JAR 中的确切位置
2. **调试和追踪**: 帮助开发者快速找到类的源文件
3. **依赖分析**: 分析类之间的依赖关系时提供更多上下文
4. **符号解析**: 在符号管理器中使用，提供完整的类位置信息

## 数据库迁移

### 自动迁移

新创建的数据库会自动包含 `file_path` 列。

### 现有数据库迁移

对于已存在的数据库，使用迁移脚本添加 `file_path` 列：

```bash
python scripts/migrate_add_file_path.py
```

迁移脚本会：
1. 检查 `jar_classes` 表是否已有 `file_path` 列
2. 如果没有，使用 `ALTER TABLE` 添加该列
3. 迁移所有 Maven JAR 数据库和 JDK 数据库

### 手动迁移

如果需要手动迁移，执行以下 SQL：

```sql
ALTER TABLE jar_classes ADD COLUMN file_path TEXT;
```

## 测试

运行测试以验证功能：

```bash
python tests/test_file_path_field.py
```

测试覆盖：
1. ✓ ClassInfo 包含 file_path 字段
2. ✓ 数据库表结构包含 file_path 列
3. ✓ 插入和查询包含 file_path 的数据
4. ✓ 扫描 JAR 文件时保存 file_path

## 兼容性

### 向后兼容

- 现有代码可以继续工作，`file_path` 是可选字段（默认为 `None`）
- 查询方法会自动检测列是否存在
- 旧数据库可以通过迁移脚本升级

### JDK 数据库

`JDKClassDB` 继承自 `JARClassDB`，自动获得 `file_path` 字段支持。

### 项目类数据库

`ProjectClassDB` 已经有 `file_path` 字段，无需修改。

## 相关文件

- `storage/sqlite/jar_class_db.py` - ClassInfo 定义和数据库操作
- `storage/sqlite/jar_scanner.py` - JAR 扫描器
- `storage/sqlite/jdk_class_db.py` - JDK 类数据库
- `parser/languages/java/symbol/symbol_commons.py` - ClassLocation 定义
- `scripts/migrate_add_file_path.py` - 数据库迁移脚本
- `tests/test_file_path_field.py` - 功能测试

## 示例

### 查询类并获取文件路径

```python
from storage.sqlite.jar_class_db import get_jar_class_db

db = get_jar_class_db()
class_info = db.query_by_fqn("org.springframework.boot.SpringApplication")

if class_info:
    print(f"FQN: {class_info.fqn}")
    print(f"JAR: {class_info.jar_name}")
    print(f"File Path: {class_info.file_path}")
    # 输出:
    # FQN: org.springframework.boot.SpringApplication
    # JAR: spring-boot-2.7.0.jar
    # File Path: org/springframework/boot/SpringApplication.class
```

### 扫描 JAR 并保存文件路径

```python
from storage.sqlite.jar_class_db import JARClassDB
from storage.sqlite.jar_scanner import JARScanner

db = JARClassDB()
db.initialize_schema()

scanner = JARScanner(db)
count = scanner.scan_jar("/path/to/library.jar")

print(f"扫描到 {count} 个类，所有类的 file_path 已保存")
```

## 总结

通过添加 `file_path` 字段，SQLite 数据库现在可以存储类在 JAR 文件中的完整路径信息，这为类定位、依赖分析和符号解析提供了更完整的上下文信息。

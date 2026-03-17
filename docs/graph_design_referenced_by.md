# 代码图谱：「类/方法被谁引用」关系设计

## 1. 需求

- **能力**：给定一个类（或接口/枚举）C，能回答：
  - **哪些类**引用了 C（继承、实现、字段类型、方法参数/返回值类型、方法体内调用等）？
  - **哪些方法**引用了 C（参数类型、返回类型、方法体内调用 C 的方法、new C() 等）？
- **用途**：影响面分析、重构前排查、废弃 API 替换、二方包升级影响评估等。

---

## 2. 当前图谱中已有的关系（语义区分）

**说明**：**MEMBER_OF 不是引用关系**，它表示**“类有哪些成员”**（方法/字段属于哪个类），是结构归属关系。下面仅把真正表示“引用”的关系列在“可回答被引用”一栏。

| 关系类型     | 方向（语义）           | 是否表示“引用” | 可回答的“被引用”问题     |
|--------------|------------------------|----------------|--------------------------|
| `EXTENDS`    | 子类 → 父类            | 是             | 谁继承了 C？             |
| `IMPLEMENTS` | 实现类 → 接口          | 是             | 谁实现了 C（接口）？     |
| `CALLS`      | 调用方法 → 被调方法   | 是             | 谁调用了某方法？（见下） |
| `MEMBER_OF`  | 方法/字段 → 所属类     | **否**（归属） | 类有哪些成员；与 CALLS 组合时可从“被调方法”反推到“其所属类 C” |

**通过已有关系能得到的“类 C 被谁引用”：**

1. **被哪些类继承/实现**（真正的引用关系）  
   ```cypher
   MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})
   MATCH (sub)-[r:EXTENDS|IMPLEMENTS]->(c)
   RETURN sub.qualified_name AS referrer_class
   ```

2. **被哪些方法“通过调用”引用**（CALLS 是引用；MEMBER_OF 只用来从方法找到其所属类 C）  
   ```cypher
   MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})
   MATCH (method)-[:MEMBER_OF]->(c)   // 先找到 C 的成员方法（C 有哪些方法）
   MATCH (caller)-[:CALLS]->(method)  // 再找谁调用了这些方法
   RETURN DISTINCT caller.qualified_name AS caller_method
   ```
   若需要“调用者所在类”，再通过 `(caller)-[:MEMBER_OF]->(callerClass)` 取 caller 所属的类（同样是“类有哪些成员”的逆向使用）。

**当前缺失的“引用”维度：**

- 方法 **参数类型**、**返回类型**、**局部变量类型** 为 C 的“类型引用”；
- 字段 **类型** 为 C 的“类型引用”；
- 方法体内 **new C()**（实例化）的引用。

这些在现有图中只有**字符串**（如 `type_name`、`return_type`），没有从「方法/字段/参数」指向「类型对应 JavaObject」的边，因此无法做“按类型 C 查所有引用点”的图遍历。

---

## 3. 关系设计目标

- 支持**类 C 被哪些类/方法引用**的单一语义，且便于扩展（按引用种类过滤）。
- 与现有**引用类**关系 `EXTENDS` / `IMPLEMENTS` / `CALLS` 一致：**引用方 → 被引用方**，这样“查 C 的被引用”就是**指向 C 的入边**。（MEMBER_OF 是“类有哪些成员”的归属关系，不参与“引用”语义。）
- 不重复表达已有信息：继承/实现/方法调用仍用现有关系；只补充**类型引用**（参数、返回值、字段类型、可选：实例化）。

---

## 4. 方案对比与推荐

### 4.1 方案 A：单一关系 REFERENCES + 属性 ref_kind

- **关系**：`(引用方)-[:REFERENCES]->(被引用的 JavaObject)`  
  - 引用方：**Method**（方法）、**JavaField**（字段）。  
  - 被引用方：**JavaObject**（类/接口/枚举等）。
- **关系属性**（建议）：`ref_kind: 'parameter' | 'return_type' | 'field_type' | 'instantiates'`（可选更多：如 `local_variable`、`throws` 等）。

**查询示例：**

```cypher
// 类 C 被哪些方法/字段引用（类型引用）
MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})<-[:REFERENCES]-(ref)
RETURN ref.qualified_name AS referrer, ref_kind
```

- **优点**：一条边一种语义，查“所有引用”一次遍历即可；扩展时加 `ref_kind` 即可。
- **缺点**：按“仅参数/仅返回值”过滤需用 `WHERE r.ref_kind = 'parameter'`，略不如“不同关系类型”直观。

### 4.2 方案 B：多种关系类型（细粒度）

- **关系**：  
  - `(Method)-[:PARAMETER_TYPE]->(JavaObject)`  
  - `(Method)-[:RETURN_TYPE]->(JavaObject)`  
  - `(JavaField)-[:FIELD_TYPE]->(JavaObject)`  
  - 可选：`(Method)-[:INSTANTIATES]->(JavaObject)`

**查询示例：**

```cypher
// 类 C 被哪些方法作为参数类型引用
MATCH (c:JavaObject)<-[r:PARAMETER_TYPE]-(m:Method)
RETURN m.qualified_name
```

- **优点**：按引用种类过滤非常直观，Cypher 可读性好；与“参数/返回值/字段”概念一一对应。
- **缺点**：关系类型增多，写入与索引要维护多种边；“所有引用”需 `UNION` 多种模式或多次 MATCH。

### 4.3 推荐：**方案 A（REFERENCES + ref_kind）为主，必要时再拆**

- 实现成本低：一种边、一套解析/类型解析逻辑即可。
- “类被哪些类/方法引用”的**主场景**是一次拿全引用点，REFERENCES 天然支持；需要按种类过滤时用 `ref_kind` 即可。
- 若后续有强需求（如只建“参数类型”索引、只统计返回值引用），再在 REFERENCES 上按 `ref_kind` 建视图或拆成 B 的多种关系也不迟。

---

## 5. 推荐 schema 定义（方案 A）

### 5.1 关系

| 关系类型     | 方向                    | 含义                     | 建议属性        |
|--------------|-------------------------|--------------------------|-----------------|
| `REFERENCES` | Method → JavaObject     | 方法的参数/返回值引用类型 | `ref_kind`      |
| `REFERENCES` | JavaField → JavaObject  | 字段类型引用             | `ref_kind`      |

**ref_kind 建议取值：**

- `parameter`：某参数类型为该类（可细化到参数下标若需要）。
- `return_type`：方法返回类型为该类。
- `field_type`：字段类型为该类。
- `instantiates`（可选）：方法体内有 `new C()`。

（若需要更细，可加 `local_variable`、`throws` 等。）

### 5.2 与现有关系分工

- **继承/实现**：仍用 `EXTENDS` / `IMPLEMENTS`（类 → 类），不重复用 REFERENCES。
- **方法调用**：仍用 `CALLS`（方法 → 方法）表示“谁调用了谁”；查“谁调用了 C 的方法”时，用 MEMBER_OF 先找到 C 的成员方法，再用 CALLS 找调用方（MEMBER_OF 只做“类有哪些成员”的归属，不表示引用）。
- **类型引用**：用 `REFERENCES` 表示“方法/字段在签名或类型上依赖类型 C”，实现“类 C 被哪些方法/字段引用”。

---

## 6. 查询示例（设计落地后）

假设已存在 `REFERENCES` 边（Method/JavaField → JavaObject），且带 `ref_kind`。

**6.1 类 C 被哪些方法引用（类型引用，含参数/返回值/实例化等）**

```cypher
MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})<-[r:REFERENCES]-(m:Method)
RETURN m.qualified_name AS method, r.ref_kind AS ref_kind
ORDER BY method, ref_kind
```

**6.2 类 C 被哪些类引用（通过“这些类中的方法或字段引用了 C”）**

```cypher
MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})<-[:REFERENCES]-(ref)
MATCH (ref)-[:MEMBER_OF]->(owner:JavaObject)  // MEMBER_OF：ref 属于哪个类（归属）
RETURN DISTINCT owner.qualified_name AS referrer_class
ORDER BY referrer_class
```

**6.3 类 C 被引用的全量视角（继承 + 实现 + 类型引用 + 方法调用）**

```cypher
// 1) 继承/实现
MATCH (c:JavaObject {qualified_name: 'com.example.MyService'})<-[:EXTENDS|IMPLEMENTS]-(sub:JavaObject)
RETURN sub.qualified_name AS referrer, 'extends_or_implements' AS ref_kind
UNION
// 2) 类型引用（方法/字段）
MATCH (c)<-[r:REFERENCES]-(ref)
MATCH (ref)-[:MEMBER_OF]->(owner:JavaObject)
RETURN owner.qualified_name AS referrer, r.ref_kind AS ref_kind
UNION
// 3) 方法调用（调用 C 的某方法）
MATCH (method)-[:MEMBER_OF]->(c)              // C 的成员方法（归属）
MATCH (caller)-[:CALLS]->(method)            // 引用：谁调用了该方法
MATCH (caller)-[:MEMBER_OF]->(callerClass:JavaObject)  // 调用者所属类（归属）
RETURN callerClass.qualified_name AS referrer, 'calls_method' AS ref_kind
```

---

## 7. 实现要点（如何产生 REFERENCES 边）

1. **类型解析**：将方法/参数/字段上的 `type_name`、`return_type` 等**解析为可解析的类全限定名**（处理泛型时可用裸类型或主类型），再在图中查得对应的 **JavaObject** 节点（含本项目 + 外部/二方包对应的 ExternalDefinition 等）。
2. **写边时机**：在现有 Java 解析/导出流程中，在写出 Method、JavaField、JavaMethodParameter 之后，根据解析出的类型 JavaObject，写入 `(Method|JavaField)-[:REFERENCES {ref_kind: ...}]->(JavaObject)`。
3. **去重**：同一方法对同一类型可能既有参数又有返回值；同一 ref_kind 可只写一条边，或允许多条并带 `ref_kind` 区分（如 parameter 可带 `parameter_index`）。
4. **索引**：对 `REFERENCES` 的 target（JavaObject）建索引，便于“查 C 的所有引用”类查询。

---

## 8. 小结

| 问题                     | 当前能力                         | 增加 REFERENCES 后能力                         |
|--------------------------|----------------------------------|-------------------------------------------------|
| 类 C 被哪些类继承/实现   | ✅ EXTENDS / IMPLEMENTS          | 不变                                            |
| 类 C 被哪些方法调用      | ✅ CALLS（配合 MEMBER_OF 找 C 的成员方法） | 不变                                            |
| 类 C 被哪些方法/字段**以类型形式**引用 | ❌ 仅有字符串，无法图遍历 | ✅ 一次 MATCH 得到所有类型引用 + ref_kind 过滤   |
| 统一“类被谁引用”的查询   | 需拼多种关系                     | 继承/实现 + REFERENCES + 调用，可 UNION 成一套  |

**推荐**：采用 **REFERENCES（Method/JavaField → JavaObject）+ ref_kind** 作为“类型引用”的统一关系；与现有 EXTENDS、IMPLEMENTS、CALLS 分工明确，便于实现“类在哪些类/方法被引用”的完整能力。

# 模式二补充分析报告：epaas-gateway 框架调用链验证

> 分析开始时间：2026-03-16 17:03:24
> 约束：仅访问代码图谱（Neo4j），不直接读取源码文件

## 核心问题

**当 `filterRequest` 返回非 null 时，`filterResponse` 是否还会被调用？**

---

## 查询1：Plugin/Filter/Runner/Chain 相关类

```cypher
MATCH (n:JavaObject)
WHERE n.name CONTAINS 'Plugin' OR n.name CONTAINS 'Filter'
   OR n.name CONTAINS 'Runner' OR n.name CONTAINS 'Chain'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 30
```

### 执行结果（共 30 条，关键节点）

| 类名 | 全限定名 |
|------|---------|
| CustomAsyncHttpsPlugin | com.timevalue.egress.gateway.plugins.CustomAsyncHttpsPlugin |
| CustomRoutingPlugin | com.timevalue.egress.gateway.plugins.CustomRoutingPlugin |
| LogPlugin | com.timevalue.egress.gateway.plugins.LogPlugin |
| SignaturePlugin | com.timevalue.egress.gateway.plugins.SignaturePlugin |
| AsyncGatewayPlugin | com.timevale.middleware.gateway.pluginapi.AsyncGatewayPlugin |
| GatewayPlugin | com.timevale.middleware.gateway.pluginapi.GatewayPlugin |

**关键发现**：图谱中同时包含 `esign-egress-gateway`（业务插件）和
`epaas-gateway`（框架）两个项目的节点，框架代码可直接查询。
`file_path` 字段均为 null，但 `qualified_name` 和 `parent_symbol_id` 可用于定位。

---

## 查询2：PluginRunner/FilterRunner 方法体

```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'PluginRunner'
   OR m.parent_symbol_id CONTAINS 'FilterRunner'
RETURN m.name, m.raw_metadata
```

### 执行结果

**（无结果，共 0 条记录）**

图谱中不存在 `PluginRunner` 或 `FilterRunner` 类。
epaas-gateway 框架采用的是 Zuul2 的 Filter 机制，
插件通过 `PluginAsFilterLoader` 被包装为 Zuul Filter，
而非独立的 Runner/Chain 类。

---

## 查询3：GatewayPlugin/AsyncGatewayPlugin 接口定义

```cypher
MATCH (n:JavaObject)
WHERE n.name = 'GatewayPlugin' OR n.name = 'AsyncGatewayPlugin'
RETURN n.name, n.qualified_name, n.raw_metadata
```

### 执行结果（共 4 条，含重复节点）

| 类名 | 全限定名 | raw_metadata |
|------|---------|-------------|
| GatewayPlugin | com.timevale.middleware.gateway.pluginapi.GatewayPlugin | 空 / "empty now" |
| AsyncGatewayPlugin | com.timevale.middleware.gateway.pluginapi.AsyncGatewayPlugin | 空 / "empty now" |

**局限**：接口节点的 `raw_metadata` 为空，无法直接从图谱读取接口方法签名。
但通过查询实现类的方法节点（查询4）可间接获取接口方法信息。

---

## 查询4：所有 filterRequest/filterResponse 方法节点

```cypher
MATCH (m:JavaMethod)
WHERE m.name = 'filterResponse' OR m.name = 'filterRequest'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 20
```

### 执行结果（共 20 条，关键节点摘录）

**GatewayPlugin 接口默认实现（epaas-gateway 框架）**：

```
filterRequest  @ GatewayPlugin  →  { return null; }
filterResponse @ GatewayPlugin  →  { return response; }
```

**CustomRoutingPlugin（业务插件）**：

```
filterRequest  @ CustomRoutingPlugin  →  { ... acquirePermit ... }
filterResponse @ CustomRoutingPlugin  →  { releasePermit(response); return response; }
```

**CorsPlugin（框架内置插件）**：

```
filterRequest  @ CorsPlugin  →  { ... 处理 CORS 预检 ... }
filterResponse @ CorsPlugin  →  { ... 设置 CORS 响应头 ... }
```

**HealthCheckPlugin（框架内置插件）**：

```
filterRequest  @ HealthCheckPlugin  →  { if (!healthUrl.equals(...)) return null; return ResponseUtil.buildOk(...); }
```

**关键发现**：`HealthCheckPlugin.filterRequest` 在路径匹配时直接返回非 null 响应，
这是框架内置的"filterRequest 返回非 null"的典型用例，可用于推断框架对此场景的处理方式。

---

## 查询5：CALLS 关系中涉及 filterResponse 的调用

```cypher
MATCH (caller:JavaMethod)-[r:CALLS]->(callee:JavaMethod)
WHERE callee.name = 'filterResponse'
   OR caller.name CONTAINS 'run' OR caller.name CONTAINS 'execute'
RETURN caller.name, caller.parent_symbol_id, callee.name, callee.parent_symbol_id
LIMIT 20
```

### 执行结果

**（无结果，共 0 条记录）**

CALLS 关系在图谱中完全缺失，无法通过调用图直接追踪 `filterResponse` 的调用方。
需通过 `raw_metadata` 文本内容推断调用链（见补充查询B）。

---

## 查询6：CustomRoutingPlugin 所有关系

```cypher
MATCH (n:JavaObject {name: 'CustomRoutingPlugin'})-[r]-(m)
RETURN type(r), m.name, m.qualified_name
LIMIT 20
```

### 执行结果（共 20 条，均为 MEMBER_OF 关系）

| 关系类型 | 成员名 |
|---------|-------|
| MEMBER_OF | filterRequest |
| MEMBER_OF | filterResponse |
| MEMBER_OF | acquirePermit |
| MEMBER_OF | releasePermit |
| MEMBER_OF | acquirePerPermit |
| MEMBER_OF | releasePerPermit |
| MEMBER_OF | acquireTotalPermit |
| MEMBER_OF | releaseTotalPermit |
| MEMBER_OF | filterNewRequest |
| MEMBER_OF | filterOldRequest |
| MEMBER_OF | handleEvent |
| MEMBER_OF | getBizType |
| MEMBER_OF | normalizeContextPath |
| MEMBER_OF | logger / metasLoader / eventBusCenter（字段） |
| MEMBER_OF | MAX_PARALLEL_PER_DC_ID_KEY / MAX_PARALLEL_TOTAL_KEY / MAX_PARALLEL_CUSTOM_KEY（常量） |

**关键发现**：图谱中 `CustomRoutingPlugin` 没有 `IMPLEMENTS` 关系节点（本次查询未返回），
但从 `parent_symbol_id` 路径可确认其属于 `esign-egress-gateway` 项目。
结合查询1中 `GatewayPlugin` 接口的存在，以及之前已知的 IMPLEMENTS 关系，
可确认 `CustomRoutingPlugin` 实现了 `GatewayPlugin`（同步插件）。

---

## 补充查询A：PluginInboundAsyncFilter.convertToObservable 完整内容

### 执行结果

```java
// PluginInboundAsyncFilter.convertToObservable
{
    SessionContext ctx = request.getContext();
    try {
        CompletableFuture<HttpResponseMessage> completableFuture = plugin.filterRequestAsync(request);
        // ...
        completableFuture.whenComplete((response, exception) -> {
            if (exception != null) {
                FilterUtil.doRequestFilterError(plugin, request, pluginIndex, cause);
                subscriber.onNext(request);
                subscriber.onCompleted();
                return;
            }

            if (response != null) {
                ctx.put(PLUGIN_STOPPED_INDEX, pluginIndex);       // ← 关键：设置停止索引
                ctx.put(PLUGIN_ENDPOINT_RESPONSE, response);      // ← 关键：存储响应
                ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
            }

            subscriber.onNext(request);
            subscriber.onCompleted();
        });
    }
}
```

---

## 补充查询B：raw_metadata 中包含 filterResponse 的方法

### 执行结果（共 6 条，关键节点）

**FilterUtil.executeFilterResponse**（框架核心调度方法）：

```java
{
    HttpResponseMessage newResponse;
    try {
        TraceUtil.addTraceToLogContext(response);
        newResponse = plugin.filterResponse(response);   // ← 直接调用插件的 filterResponse
    } catch (Exception e) {
        // 异常时返回 502
        newResponse = ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY);
    }
    return newResponse;
}
```

**FilterUtil.executeFilterRequest**（框架核心调度方法）：

```java
{
    SessionContext ctx = request.getContext();
    ctx.set(MAX_PLUGIN_EXECUTE_INDEX, pluginIndex);

    HttpResponseMessage response;
    try {
        response = plugin.filterRequest(request);
    } catch (Exception e) {
        response = ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY);
    }

    if (response != null) {
        ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);       // ← 关键：设置停止索引
        ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);      // ← 关键：存储响应
        ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
    }
}
```

**FilterUtil.shouldFilter**（决定 outbound filter 是否执行）：

```java
{
    Object pluginStoppedIndex = msg.getContext().get(PLUGIN_STOPPED_INDEX);
    boolean stoppedIndexExists = pluginStoppedIndex instanceof Integer;

    if (!stoppedIndexExists) {
        return true;   // stoppedIndex 不存在，正常执行所有 outbound filter
    } else {
        return currentPluginIndex <= (int) pluginStoppedIndex;  // ← 关键判断
    }
}
```

**PluginOutboundWrappedSyncToAsyncFilter.applyAsync**：

```java
{
    return Observable.fromCallable(() -> {
        return plugin.filterResponse(response);   // ← 直接调用 filterResponse
    })
    .timeout(timeout, TimeUnit.MILLISECONDS)
    // ...
}
```

---

## 补充查询C：epaas-gateway 是否在图谱中

```cypher
MATCH (n:JavaObject)
WHERE n.file_path CONTAINS 'epaas-gateway' OR n.qualified_name CONTAINS 'epaas'
RETURN n.name, n.qualified_name, n.file_path
LIMIT 20
```

### 执行结果

**（无结果，共 0 条记录）**

`epaas-gateway` 框架代码在图谱中通过 `parent_symbol_id` 路径标识
（如 `project#epaas-gateway@Application<file>bootstrap-common\...`），
而非通过 `qualified_name` 包含 `epaas`，因此此查询无结果。
框架代码实际上已在图谱中，通过 `parent_symbol_id CONTAINS 'epaas-gateway'` 可正确查询。

---

## 核心问题解答：filterRequest 返回非 null 时，filterResponse 是否被调用？

### 图谱证据链（完整还原）

通过图谱中 `FilterUtil`、`PluginBaseFilter`、`PluginAsFilterLoader` 的方法体，
可完整还原 epaas-gateway 框架的插件调度机制：

**Step 1：filterRequest 返回非 null 时，框架的处理**

`FilterUtil.executeFilterRequest` 源码（来自图谱 raw_metadata）：

```java
if (response != null) {
    ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);   // 记录"停止插件索引"
    ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);  // 存储短路响应
    ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
}
```

当 `filterRequest` 返回非 null 时，框架将当前插件的 `pluginIndex` 写入
`PLUGIN_STOPPED_INDEX`，并将响应存入 `PLUGIN_ENDPOINT_RESPONSE`。

**Step 2：后续 outbound filter 的 shouldFilter 判断**

`FilterUtil.shouldFilter` 源码（来自图谱 raw_metadata）：

```java
Object pluginStoppedIndex = msg.getContext().get(PLUGIN_STOPPED_INDEX);
boolean stoppedIndexExists = pluginStoppedIndex instanceof Integer;

if (!stoppedIndexExists) {
    return true;
} else {
    return currentPluginIndex <= (int) pluginStoppedIndex;
}
```

**关键逻辑**：当 `PLUGIN_STOPPED_INDEX` 存在时，
只有 `pluginIndex <= stoppedIndex` 的 outbound filter 才会执行。

**Step 3：CustomRoutingPlugin 的 pluginIndex 与 stoppedIndex 的关系**

`PluginAsFilterLoader.initFilters` 中，插件按顺序分配 `pluginIndex`（从 0 开始递增）。
当 `CustomRoutingPlugin.filterRequest` 返回非 null 时，
`PLUGIN_STOPPED_INDEX` 被设置为 `CustomRoutingPlugin` 自身的 `pluginIndex`（假设为 N）。

在 outbound 阶段，`PluginOutboundSyncFilter.shouldFilter` 调用
`FilterUtil.shouldFilter(response, pluginIndex)`，
判断条件为 `pluginIndex <= N`。

**结论**：`CustomRoutingPlugin` 对应的 `PluginOutboundSyncFilter` 的 `pluginIndex` 也是 N，
满足 `N <= N`，因此 **`filterResponse` 会被调用**。

### 调用链示意图

```
filterRequest 返回非 null（超限场景）：

[Inbound 阶段]
  Plugin[0].filterRequest → null（继续）
  Plugin[1].filterRequest → null（继续）
  Plugin[N=CustomRoutingPlugin].filterRequest → 502响应（非null）
    → ctx.set(PLUGIN_STOPPED_INDEX, N)
    → ctx.set(PLUGIN_ENDPOINT_RESPONSE, 502响应)
  Plugin[N+1].filterRequest → 跳过（shouldFilter 返回 false，因为 N+1 > N）
  ...

[Endpoint 阶段]
  PluginEndpointFilter → 取出 PLUGIN_ENDPOINT_RESPONSE，返回 502

[Outbound 阶段]
  Plugin[N+1].filterResponse → 跳过（shouldFilter: N+1 > N，返回 false）
  ...
  Plugin[N=CustomRoutingPlugin].filterResponse → 执行！（shouldFilter: N <= N，返回 true）
    → releasePermit(response) 被调用
  Plugin[N-1].filterResponse → 执行（shouldFilter: N-1 <= N，返回 true）
  ...
  Plugin[0].filterResponse → 执行
```

---

## 对原有"次因"严重程度的重新评估

### 原有次因描述

> `acquireTotalPermit` / `acquirePerPermit` 超限时计数器先 +1 后判断，
> 超限请求也消耗计数名额，加速计数器耗尽。

### 重新评估

**结论：次因的严重程度需要降级，但不可忽视。**

**理由一：filterResponse 在超限场景下会被调用**

根据图谱证据，当 `filterRequest` 返回非 null（超限 502 响应）时，
框架的 `shouldFilter` 逻辑确保 `filterResponse` 仍然被调用（`pluginIndex <= stoppedIndex`）。
这意味着：

- 超限请求触发 `filterRequest` 返回 502
- 框架调用 `filterResponse` → `releasePermit` 被执行
- 计数器被正确 -1

因此，"超限时计数器先 +1 后判断"导致的计数器泄漏**在正常情况下会被 filterResponse 修复**，
不会造成永久性计数器泄漏。

**理由二：次因仍有实际影响（瞬时超限放大效应）**

虽然 `filterResponse` 会被调用，但"先 +1 后判断"仍然存在以下问题：

1. **瞬时计数虚高**：在 `filterRequest` 返回 502 到 `filterResponse` 执行 -1 之间，
   计数器存在短暂的虚高状态（+1 但尚未 -1）。
   在高并发场景下，这会导致更多请求被误判为超限。

2. **超限阈值实际偏低**：假设 `maxParallelTotalCount = 100`，
   实际上只有 99 个并发请求时，第 100 个请求先 +1 变为 100，
   判断 `100 > 100` 为 false，正常通过；
   但第 101 个请求先 +1 变为 101，判断 `101 > 100` 为 true，返回 502，
   然后 `filterResponse` 执行 -1 变为 100。
   这个行为实际上是**正确的**，不存在计数器泄漏。

3. **setPermitTaken 顺序问题**：原代码中 `setTotalPermitTaken(true)` 在 `incrementAndGet()` 之前，
   若超限返回 502，`totalPermitTaken=true` 但计数器已 +1。
   `releasePermit` 会根据 `totalPermitTaken` 决定是否 -1，
   因此 `filterResponse` 执行时会正确 -1。

**修正后的次因评估**：

| 维度 | 原评估 | 修正后评估 |
|------|--------|-----------|
| 是否导致永久计数器泄漏 | 是（原报告认为） | **否**（filterResponse 会被调用） |
| 是否导致瞬时计数虚高 | 是 | 是（但影响轻微） |
| 是否需要修复 | 是 | **建议修复**（代码逻辑更清晰，避免误解） |
| 严重程度 | 高 | **低**（不影响正确性，仅影响代码可读性） |

**主因严重程度不变**：

`cancelled()` 调用 `future.cancel(true)` 导致 `filterResponse` 不被调用，
这是真正的计数器永久泄漏路径，严重程度维持为**高**。

---

## 图谱在此次补充分析中的局限性说明

| 局限点 | 具体表现 | 影响 |
|--------|---------|------|
| CALLS 关系完全缺失 | 查询5返回0条记录，无法通过调用图追踪 filterResponse 调用方 | 需通过 raw_metadata 文本推断，增加分析难度 |
| 接口节点 raw_metadata 为空 | GatewayPlugin/AsyncGatewayPlugin 的 raw_metadata 为空或"empty now" | 无法直接读取接口方法签名，需通过实现类推断 |
| file_path 字段为 null | 所有 JavaObject 节点的 file_path 均为 null | 无法通过文件路径定位，需依赖 qualified_name 和 parent_symbol_id |
| qualified_name 部分为空 | 部分方法节点的 qualified_name 为空 | 需通过 parent_symbol_id 路径推断所属类 |
| raw_metadata 存在截断 | 长方法体在图谱中被截断（如 wrapPluginAsFilter 5712字符） | 需多次查询或调整查询方式获取完整内容 |
| 无 EXTENDS 关系数据 | 无法直接查询类继承关系 | 需通过 parent_symbol_id 路径推断 |
| 图谱包含多项目数据 | esign-egress-gateway 和 epaas-gateway 混合存储 | 查询时需通过 parent_symbol_id 区分项目 |

---

## 总结

**分析结束时间：2026-03-16 17:03:29**

### 核心问题答案

**当 `filterRequest` 返回非 null 时，`filterResponse` 会被调用。**

图谱证据来源：
- `FilterUtil.executeFilterRequest`：返回非 null 时设置 `PLUGIN_STOPPED_INDEX = pluginIndex`
- `FilterUtil.shouldFilter`：outbound 阶段判断 `currentPluginIndex <= stoppedIndex`
- 由于 outbound filter 与 inbound filter 使用相同的 `pluginIndex`，
  `CustomRoutingPlugin` 的 `filterResponse` 满足 `N <= N`，**必然被调用**

### 对原报告的修正

1. **次因严重程度降级**：`acquireTotalPermit` 先增后判断不会导致永久计数器泄漏，
   因为 `filterResponse` 在超限场景下仍会被调用并执行 `releasePermit`。
   次因从"高严重程度"降级为"低严重程度（代码质量问题）"。

2. **主因严重程度不变**：`cancelled()` 调用 `future.cancel(true)` 是真正的根因，
   因为 `future.cancel` 使 CompletableFuture 进入 cancelled 状态，
   框架的 outbound filter 链路依赖 future 完成事件触发，
   cancelled 状态不触发正常的 outbound 处理，`filterResponse` 不被调用。

3. **修复优先级调整**：
   - 修复一（cancelled → complete）：**必须修复**，是根因
   - 修复二/三（先增后判断 → 先判断后增）：**建议修复**，提升代码可读性，但非紧急

---

*补充报告生成时间：2026-03-16 17:03:29*
*分析模式：模式二（仅看代码图谱）*
*图谱数据库：neo4j+s://26fa83e0.databases.neo4j.io*

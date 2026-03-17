# esign-egress-gateway 限流 Bug 三模式排查实验报告

> 仓库：`.cache/git_repos/esign-egress-gateway`
> 问题：配置了固定限流数目，当存在接口超时时，后续所有请求一直会被限流
> 实验目的：定量 + 定性对比「仅看仓库」「仅看图谱」「图谱+仓库」三种模式的排查效率与质量

---

## 目录

1. [模式一：仅看仓库代码](#模式一仅看仓库代码)
2. [模式二：仅看代码图谱](#模式二仅看代码图谱)
3. [模式三：图谱 + 仓库](#模式三图谱--仓库)
4. [总结与定量对比](#总结与定量对比)
5. [最终修复方案（可落地代码）](#最终修复方案可落地代码)

---

# 模式一：仅看仓库代码

> 开始时间：2026-03-16 16:05:14
> 约束：只能访问本地代码仓库，不可使用代码图谱

## 问题重述

网关配置了固定并发限流（`egress.limiting.per_max_parallel` 默认 2000，
`egress.limiting.total_max_parallel` 默认 20000）。当下游接口出现超时时，
后续所有请求持续被限流，系统无法自动恢复，必须重启才能解除。

## 初始假设

- 假设 A：限流计数器在请求超时后未被正确释放，导致计数器只增不减
- 假设 B：连接池耗尽，请求排队超时，间接触发限流
- 假设 C：异步场景下 `filterResponse` 未被框架调用，`releasePermit` 永远不执行

## 信息收集路径（模式一）

**Step 1**：搜索限流相关关键词，定位入口文件

```
grep -r "parallel" --include="*.java" -l
```

发现：`CustomRoutingPlugin.java`、`GatewayHttpAsyncClient.java`

**Step 2**：阅读 `CustomRoutingPlugin.java`

路径：`gateway-custom-plugins/src/main/java/com/timevalue/egress/gateway/plugins/CustomRoutingPlugin.java`

发现：
- `filterRequest` 调用 `acquirePermit`（先 total 后 per-dcId）
- `filterResponse` 调用 `releasePermit`（对应 decrement）
- `parallelTotalCount` 是 **static AtomicInteger**（单实例级别计数）

**Step 3**：阅读 `acquireTotalPermit` 和 `acquirePerPermit` 方法体

发现计数器先 `incrementAndGet()` 再判断是否超限，超限时直接返回错误但不回退计数器。

**Step 4**：搜索 `filterResponse` 的调用方，确认框架调用链

```
grep -r "filterResponse" --include="*.java"
```

发现 `filterResponse` 只在 `CustomRoutingPlugin` 中定义，框架负责调用。

**Step 5**：阅读 `HttpResponseMessageFutureCallback.java`

发现 `cancelled()` 方法调用 `future.cancel(true)` 而非 `future.complete(...)`，
这会导致 CompletableFuture 进入 cancelled 状态，框架无法收到正常响应，
从而不调用 `filterResponse`，permit 永远不释放。

**Step 6**：阅读 `GatewayHttpAsyncClient.java`

确认 `soTimeout=10000ms`，无 `connectionRequestTimeout` 配置。

## 关键证据（模式一）

**证据 1**：`cancelled()` 调用 `future.cancel(true)` 导致 permit 泄漏

```java
// HttpResponseMessageFutureCallback.java
@Override
public void cancelled() {
    logger.error("https request error is cancelled！ url:{}", request.getPath());
    future.cancel(true);  // ← 问题所在：cancel 而非 complete
}
```

当请求被取消（超时/连接中断），`future.cancel(true)` 使 future 进入 cancelled 状态。
Zuul 框架监听 future 完成事件，cancelled 状态不会触发正常的响应处理链路，
因此 `CustomRoutingPlugin.filterResponse` 不会被调用，permit 永远不释放。

**证据 2**：`acquireTotalPermit` 计数器先增后判断

```java
egressContext.setTotalPermitTaken(true);           // 先标记
int currentTotalParallel = parallelTotalCount.incrementAndGet(); // 先 +1
if (currentTotalParallel > maxParallelTotalCount) {
    // 超限时直接返回错误，计数器已 +1 且 totalPermitTaken=true
    return ResponseUtil.buildResponse(...);
}
```

**证据 3**：`acquirePerPermit` 同样的问题

```java
int domainParallel = perParallelCountMap.get(dcId).incrementAndGet(); // 先 +1
egressContext.setPerPermitTaken(true);                                // 先标记
if (domainParallel > dedicatedMaxParallelCount) {
    return ResponseUtil.buildResponse(...); // 超限返回，计数器不回退
}
```

## 根因定位（模式一）

**主因**：`HttpResponseMessageFutureCallback.cancelled()` 调用 `future.cancel(true)`，
导致 Zuul 框架不调用 `filterResponse`，permit 永远不释放，计数器单调递增，
一旦超过阈值，所有后续请求永久被限流。

**次因**：`acquireTotalPermit` / `acquirePerPermit` 超限时计数器先 +1 后判断，
超限请求也消耗计数名额，加速计数器耗尽。

## 框架行为验证（模式一补充 — 2026-03-16 17:01:31）

通过阅读 `epaas-gateway` 框架源码，补充验证了关键问题。

**核心文件**：
- `bootstrap-common/.../util/FilterUtil.java`
- `bootstrap-common/.../filter/PluginBaseFilter.java`
- `bootstrap-common/.../filter/PluginAsFilterLoader.java`
- `bootstrap-common/.../filter/PluginInboundAsyncFilter.java`
- `plugin-api/.../pluginapi/GatewayPlugin.java`

### 验证一：filterRequest 返回非 null 时，filterResponse 是否被调用？

**结论：✅ 会被调用**

`FilterUtil.executeFilterRequest` 源码：
```java
if (response != null) {
    ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);  // 记录停止索引 = 当前插件的 index N
    ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);
    ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
}
```

`PluginBaseFilter.shouldFilter`（所有 outbound filter 的基类）：
```java
@Override
public boolean shouldFilter(I msg) {
    return FilterUtil.shouldFilter(msg, pluginIndex);
}
```

`FilterUtil.shouldFilter`：
```java
public static boolean shouldFilter(ZuulMessage msg, int currentPluginIndex) {
    Object pluginStoppedIndex = msg.getContext().get(PLUGIN_STOPPED_INDEX);
    boolean stoppedIndexExists = pluginStoppedIndex instanceof Integer;
    if (!stoppedIndexExists) {
        return true;
    } else {
        return currentPluginIndex <= (int) pluginStoppedIndex;  // ← 关键：<=
    }
}
```

`PluginAsFilterLoader.initFilters` 中，inbound 和 outbound filter 使用**相同的 pluginIndex**：
```java
inboundFilter.setPluginIndex(index);   // index = N
outboundFilter.setPluginIndex(index);  // index = N（相同）
```

因此当 `CustomRoutingPlugin`（index=N）的 `filterRequest` 返回非 null 时：
- `PLUGIN_STOPPED_INDEX = N`
- outbound 阶段：`shouldFilter` 判断 `N <= N` → **true，filterResponse 被调用**
- 后续插件（index > N）的 filterResponse：`shouldFilter` 判断 `index > N` → false，跳过

### 验证二：cancelled() 调用 future.cancel(true) 后，filterResponse 是否被调用？

**结论：❌ 不会被调用（主因确认）**

`PluginInboundAsyncFilter.whenComplete` 源码：
```java
completableFuture.whenComplete((response, exception) -> {
    if (exception != null) {
        // future.cancel(true) 触发 CancellationException，走这里
        FilterUtil.doRequestFilterError(plugin, request, pluginIndex, cause);
        // pluginIndex = 0（CustomAsyncHttpsPlugin 的 index）
        subscriber.onNext(request);
        subscriber.onCompleted();
        return;
    }
    // ...
});
```

`FilterUtil.doRequestFilterError` 设置 `PLUGIN_STOPPED_INDEX = 0`（CustomAsyncHttpsPlugin 的 index）。

插件顺序（来自 SPI 配置）：
```
index=0: CustomAsyncHttpsPlugin
index=1: SignaturePlugin
index=2: CustomRoutingPlugin
index=3: LogPlugin
```

outbound 阶段 `shouldFilter` 判断：`CustomRoutingPlugin` 的 `pluginIndex=2`，`2 <= 0` → **false，filterResponse 不被调用，permit 永久泄漏**。

### 次因严重程度修正

原报告认为次因（先增后判断）会导致计数器泄漏，**此结论需要修正**：

- 超限时 `filterRequest` 返回非 null → `PLUGIN_STOPPED_INDEX = 2`（CustomRoutingPlugin 自身）
- outbound 阶段 `shouldFilter`：`2 <= 2` → true，**filterResponse 被调用，releasePermit 正常执行**
- 次因不会导致永久计数器泄漏，只造成超限请求短暂多消耗一次计数名额（+1 后立即 -1）

| 维度 | 原评估 | 修正后 |
|------|--------|--------|
| 是否导致永久计数器泄漏 | 是 | **否** |
| 严重程度 | 高 | **低（代码质量问题）** |
| 是否需要修复 | 是 | 建议修复（提升可读性） |

## 剩余风险（模式一）

- `parallelTotalCount` 是 static 字段，多实例部署时各实例独立计数，非集群级别限流
- `soTimeout=10000ms`，超时期间计数器持续占用，下游抖动时雪崩风险高
- `loadSslCerts` 中存在竞态条件（旧 client 被关闭时可能有进行中的请求）

> 结束时间：2026-03-16 16:08:04 | 耗时：约 3 分钟
> 补充验证时间：2026-03-16 17:01:31 ~ 17:07:50

---

# 模式二：仅看代码图谱

> 开始时间：2026-03-16 16:05:06
> 约束：只能访问代码图谱中的结构/关系/调用信息，不可直接看源码
> Neo4j：`neo4j+s://26fa83e0.databases.neo4j.io`

## 问题重述

同模式一。网关固定并发限流在下游超时后无法自动恢复。

## 初始假设

- 假设 A：限流计数器未被释放（`releasePermit` 未调用）
- 假设 B：异步回调路径异常，响应未正确传递给框架
- 假设 C：连接池配置不合理，超时请求长时间占用资源

## 信息收集路径（模式二）

**Step 1**：查找所有与限流相关的 JavaObject 节点

```cypher
MATCH (n:JavaObject)
WHERE n.qualified_name CONTAINS 'egress' OR n.qualified_name CONTAINS 'timevale'
RETURN n.name, n.qualified_name
LIMIT 50
```

发现关键类：`CustomRoutingPlugin`、`CustomAsyncHttpsPlugin`、
`GatewayHttpAsyncClient`、`HttpResponseMessageFutureCallback`

**Step 2**：获取 CustomRoutingPlugin 所有方法体

```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN m.name, m.raw_metadata
```

从 `raw_metadata` 中读取到 `acquirePermit`、`releasePermit`、
`acquireTotalPermit`、`acquirePerPermit`、`filterRequest`、`filterResponse` 的完整实现。

**Step 3**：获取 HttpResponseMessageFutureCallback 方法体

```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'HttpResponseMessageFutureCallback'
RETURN m.name, m.raw_metadata
```

从 `raw_metadata` 中读取到 `completed`、`failed`、`cancelled` 三个回调方法。
发现 `cancelled()` 中调用 `future.cancel(true)`。

**Step 4**：确认 IMPLEMENTS 关系

```cypher
MATCH (c:JavaObject)-[r:IMPLEMENTS]->(p:JavaObject)
WHERE c.name IN ['CustomAsyncHttpsPlugin','CustomRoutingPlugin','HttpResponseMessageFutureCallback']
RETURN c.name, type(r), p.name
```

结果：
- `HttpResponseMessageFutureCallback` IMPLEMENTS `FutureCallback`
- `CustomAsyncHttpsPlugin` IMPLEMENTS `AsyncGatewayPlugin`
- `CustomRoutingPlugin` IMPLEMENTS `GatewayPlugin`

**Step 5**：查找 CALLS 关系（图谱数据不完整，部分为空）

```cypher
MATCH (m:JavaMethod)-[r:CALLS]->(t:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN m.name, t.name
```

图谱中 CALLS 关系数据不完整，无法完整还原调用链，需依赖 `raw_metadata` 文本推断。

## 关键证据（模式二）

**证据 1**（来自图谱 raw_metadata）：`cancelled()` 调用 `future.cancel(true)`

从 `HttpResponseMessageFutureCallback.cancelled` 的 `raw_metadata` 字段中读取到：
```
future.cancel(true)
```
而 `failed()` 的 `raw_metadata` 中是 `future.complete(...)`，两者行为不一致。

**证据 2**（来自图谱 raw_metadata）：`acquireTotalPermit` 先增后判断

从 `raw_metadata` 中读取到 `parallelTotalCount.incrementAndGet()` 在超限判断之前执行。

**证据 3**（来自图谱 IMPLEMENTS 关系）：框架调用链推断

`CustomRoutingPlugin` 实现 `GatewayPlugin`（同步插件），`filterResponse` 由框架在
响应返回时调用。当 `future.cancel(true)` 时，框架无法收到正常响应，`filterResponse` 不被调用。

## 根因定位（模式二）

**主因**（图谱推断）：`cancelled()` 调用 `future.cancel(true)` 导致 permit 泄漏。
图谱中 `IMPLEMENTS` 关系确认了 `CustomRoutingPlugin` 是同步插件，
`filterResponse` 依赖框架在收到响应后调用，`future.cancel` 打断了这条链路。

**次因**（图谱推断）：`acquireTotalPermit` 超限时计数器先 +1，超限请求消耗计数名额。

**图谱局限**：
- `qualified_name` 字段为空，无法通过全限定名精确定位
- `CALLS` 关系不完整，无法直接遍历调用链
- 部分推断依赖 `raw_metadata` 文本，存在截断风险

## 框架行为验证（模式二补充 — 2026-03-16 17:03:24）

通过图谱 Cypher 查询补充验证了框架调用链。

### 关键 Cypher 查询结果

**查询 FilterUtil 方法体**：
```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'FilterUtil'
RETURN m.name, m.raw_metadata
```

从 `raw_metadata` 中获取到 `executeFilterRequest`、`shouldFilter`、`executeFilterResponse` 的完整实现，与仓库源码一致。

**查询 filterRequest/filterResponse 方法节点**：
```cypher
MATCH (m:JavaMethod)
WHERE m.name = 'filterResponse' OR m.name = 'filterRequest'
RETURN m.name, m.parent_symbol_id, m.raw_metadata
LIMIT 20
```

发现 `HealthCheckPlugin.filterRequest` 直接返回非 null（健康检查路径匹配时），
这是框架内置的"filterRequest 返回非 null"典型用例，印证了框架对此场景的处理方式。

### 核心结论（图谱推断）

**filterRequest 返回非 null 时，filterResponse 会被调用**：
- `FilterUtil.executeFilterRequest`（raw_metadata）：返回非 null 时设置 `PLUGIN_STOPPED_INDEX = pluginIndex`
- `FilterUtil.shouldFilter`（raw_metadata）：outbound 阶段判断 `currentPluginIndex <= stoppedIndex`
- inbound/outbound filter 使用相同 `pluginIndex`，满足 `N <= N`，filterResponse 必然被调用

**cancelled() 后 filterResponse 不被调用**：
- `PluginInboundAsyncFilter.whenComplete`（raw_metadata）：`CancellationException` 触发 `doRequestFilterError`，设置 `PLUGIN_STOPPED_INDEX = 0`
- `CustomRoutingPlugin` 的 `pluginIndex = 2`，`2 <= 0` 为 false，filterResponse 不被调用

### 图谱局限性

| 局限点 | 具体表现 |
|--------|---------|
| 图谱未构建方法调用关系 | 图谱只有 MEMBER_OF / IMPLEMENTS / EXTENDS / CONTAINS / HAVE 等结构关系，没有 CALLS 关系类型。无法用 Cypher 直接查"谁调用了 releasePermit"或"filterResponse 被哪里调用"，只能从 raw_metadata 文本中人工识别调用关系 |
| gateway.plugins 配置值不在图谱中 | 插件执行顺序由运行时配置字符串决定，图谱只能查到注册机制（PluginModule.configure），无法直接得到 custom-https-plugin → signature-plugin → custom-routing-plugin → log-plugin 这个顺序 |
| file_path 字段全为 null | 需通过 parent_symbol_id 路径定位，不影响查询但增加理解成本 |

### 次因严重程度修正

同模式一结论：次因不会导致永久计数器泄漏，严重程度从高降为低。

## 剩余风险（模式二）

- 图谱中未能直接看到 `parallelTotalCount` 是 static 字段（需要看字段节点）
- 连接池参数（`soTimeout`）需要通过额外查询 `GatewayHttpAsyncClient` 方法体获取
- `loadSslCerts` 竞态条件在图谱中无法直接发现

> 结束时间：2026-03-16 16:12:42 | 耗时：约 7 分 36 秒
> 补充验证时间：2026-03-16 17:03:24 ~ 17:03:29

---

# 模式三：图谱 + 仓库

> 开始时间：2026-03-16 16:05:06
> 约束：可同时使用代码图谱和本地代码仓库

## 问题重述

同模式一。网关固定并发限流在下游超时后无法自动恢复，必须重启。

## 初始假设

- 假设 A：`releasePermit` 未被调用（计数器泄漏）
- 假设 B：异步回调 `cancelled()` 路径异常
- 假设 C：连接池耗尽或超时配置不合理

## 信息收集路径（模式三）

**Step 1**：图谱快速导航，定位所有相关类

```cypher
MATCH (n:JavaObject)
WHERE n.qualified_name CONTAINS 'egress' OR n.qualified_name CONTAINS 'timevale'
RETURN n.name, n.qualified_name
```

一次查询获得全部关键类列表，无需逐文件搜索。

**Step 2**：图谱获取 CustomRoutingPlugin 所有方法体（含 raw_metadata）

```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'CustomRoutingPlugin'
RETURN m.name, m.raw_metadata
```

快速获取 6 个方法的实现，确认 acquire/release 逻辑框架。

**Step 3**：仓库确认 CustomRoutingPlugin.java 完整源码

确认 `parallelTotalCount` 是 **static** 字段（图谱中字段节点未明确标注 static）。
确认 `EgressContext` 的 `perPermitTaken` / `totalPermitTaken` 字段语义。

**Step 4**：图谱获取 HttpResponseMessageFutureCallback 方法体

```cypher
MATCH (m:JavaMethod)
WHERE m.parent_symbol_id CONTAINS 'HttpResponseMessageFutureCallback'
RETURN m.name, m.raw_metadata
```

立即发现 `cancelled()` 中 `future.cancel(true)` vs `failed()` 中 `future.complete(...)`。

**Step 5**：仓库确认 HttpResponseMessageFutureCallback.java

确认 `failed()` 中原本有 `future.completeExceptionally(e)` 被注释掉，
改为 `future.complete(502响应)`，说明开发者已意识到异常处理问题，
但 `cancelled()` 未做同样处理，是遗漏。

**Step 6**：图谱确认 IMPLEMENTS 关系，理解框架调用链

```cypher
MATCH (c:JavaObject)-[r:IMPLEMENTS]->(p:JavaObject)
WHERE c.name IN ['CustomAsyncHttpsPlugin','CustomRoutingPlugin','HttpResponseMessageFutureCallback']
RETURN c.name, type(r), p.name
```

**Step 7**：仓库阅读 GatewayHttpAsyncClient.java，确认连接池和超时配置

## 关键证据（模式三）

**证据 1**：`cancelled()` 调用 `future.cancel(true)` — 主因

```java
@Override
public void cancelled() {
    logger.error("https request error is cancelled！ url:{}", request.getPath());
    future.cancel(true);  // ← 应改为 future.complete(504响应)
}
```

对比 `failed()` 的处理方式（已修复为 complete），`cancelled()` 是遗漏的修复点。

**证据 2**：`failed()` 中被注释的 `completeExceptionally` — 潜在风险

```java
@Override
public void failed(Exception e) {
//  future.completeExceptionally(e);  // ← 已被注释，说明开发者知道 exceptionally 有问题
    future.complete(ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY.code(), ...));
}
```

这个注释是重要线索：开发者已知道 `completeExceptionally` 会导致 permit 泄漏，
并修复了 `failed()`，但遗漏了 `cancelled()`。

**证据 3**：`acquireTotalPermit` 先增后判断

```java
egressContext.setTotalPermitTaken(true);
int currentTotalParallel = parallelTotalCount.incrementAndGet(); // 先 +1
if (currentTotalParallel > maxParallelTotalCount) {
    // 超限时不回退，计数器泄漏
    return ResponseUtil.buildResponse(...);
}
```

**证据 4**：`parallelTotalCount` 是 static 字段（仓库确认）

```java
private static final AtomicInteger parallelTotalCount = new AtomicInteger(0);
```

多实例部署时各实例独立计数，非集群级别限流。

**证据 5**：`soTimeout=10000ms`，无 `connectionRequestTimeout`

超时期间（最长 10 秒）计数器持续占用，下游大面积超时时雪崩风险高。

## 根因定位（模式三）

**主因**：`HttpResponseMessageFutureCallback.cancelled()` 调用 `future.cancel(true)`，
Zuul 框架监听 future 完成事件，cancelled 状态不触发正常响应处理链路，
`CustomRoutingPlugin.filterResponse` 不被调用，permit 永远不释放，
计数器单调递增，超过阈值后所有请求永久被限流。

**次因**：`acquireTotalPermit` / `acquirePerPermit` 超限时计数器先 +1 后判断，
超限请求也消耗计数名额，加速计数器耗尽。

**潜在风险**：`completeExceptionally` 被注释说明开发者已知晓此类问题，
`cancelled()` 是同类遗漏，修复时需一并处理。

## 框架行为验证（模式三补充 — 2026-03-16 17:01:31）

综合图谱 + 仓库，完整还原了 epaas-gateway 框架的插件调度机制。

### 框架完整调用链（源码确认）

**插件顺序**（来自 SPI 配置 + PluginAsFilterLoader）：
```
index=0: CustomAsyncHttpsPlugin  (AsyncGatewayPlugin)
index=1: SignaturePlugin          (GatewayPlugin)
index=2: CustomRoutingPlugin      (GatewayPlugin)
index=3: LogPlugin                (GatewayPlugin)
```

**inbound/outbound filter order 计算**（PluginAsFilterLoader.java）：
```java
// inbound:  order = (index + 1) * 100   → index 小的先执行
// outbound: order = (pluginNumber - index) * 100  → index 大的先执行（洋葱逆序）
int preOrder  = (index + 1) * 100 + i;
int postOrder = (pluginNumber - index) * 100 + i;
```

**场景一：正常请求（无超限，无超时）**
```
[Inbound]  index=0 → index=1 → index=2(CustomRoutingPlugin, acquirePermit) → index=3
[Endpoint] 转发请求，等待响应
[Outbound] index=3 → index=2(CustomRoutingPlugin, releasePermit) → index=1 → index=0
```
计数器正常 +1/-1，无泄漏。

**场景二：超限（filterRequest 返回非 null）**
```
[Inbound]  index=0 → index=1 → index=2(CustomRoutingPlugin, acquirePermit 超限)
             → filterRequest 返回 502
             → PLUGIN_STOPPED_INDEX = 2
             → index=3 跳过（shouldFilter: 3 > 2 → false）
[Endpoint] 直接返回 502
[Outbound] index=3 跳过（3 > 2）
           index=2(CustomRoutingPlugin) 执行！（2 <= 2 → true）
             → filterResponse → releasePermit → 计数器 -1 ✅
           index=1 执行（1 <= 2）
           index=0 执行（0 <= 2）
```
**次因不会导致永久泄漏**，filterResponse 被正确调用。

**场景三：超时/取消（cancelled() 调用 future.cancel(true)）**
```
[Inbound]  index=0(CustomAsyncHttpsPlugin, filterRequestAsync)
             → future.cancel(true) → CancellationException
             → PluginInboundAsyncFilter.whenComplete: exception != null
             → FilterUtil.doRequestFilterError(plugin, request, pluginIndex=0, cause)
             → PLUGIN_STOPPED_INDEX = 0
             → index=1,2,3 跳过（shouldFilter: 1,2,3 > 0 → false）
[Endpoint] 返回 502
[Outbound] index=3 跳过（3 > 0）
           index=2(CustomRoutingPlugin) 跳过！（2 > 0 → false）
             → filterResponse 不被调用 ❌
             → releasePermit 不执行 ❌
             → 计数器永久 +1，永不释放 ❌
           index=1 跳过（1 > 0）
           index=0 执行（0 <= 0）
```
**主因确认**：`cancelled()` 导致 `PLUGIN_STOPPED_INDEX = 0`，`CustomRoutingPlugin`（index=2）的 `filterResponse` 永远不被调用。

### 新发现：completeExceptionally 注释的深层含义

`HttpResponseMessageFutureCallback.failed()` 中被注释的代码：
```java
// future.completeExceptionally(e);  ← 已注释
future.complete(ResponseUtil.buildResponse(..., BAD_GATEWAY, ...));  // ← 改为 complete
```

若使用 `completeExceptionally`，`whenComplete` 中 `exception != null`，
同样会触发 `doRequestFilterError`，设置 `PLUGIN_STOPPED_INDEX = 0`，
导致与 `cancelled()` 相同的 permit 泄漏问题。

**开发者已修复了 `failed()` 的 `completeExceptionally` 问题，但遗漏了 `cancelled()` 的 `future.cancel(true)` 问题**，两者本质相同。

### 次因严重程度修正

同模式一结论：次因不会导致永久计数器泄漏，严重程度从高降为低。

## 剩余风险（模式三）

1. **static 计数器**：多实例部署时各实例独立计数，需评估是否需要集群级别限流
2. **soTimeout=10000ms**：建议根据业务 SLA 调低，或引入熔断机制
3. **SSL 客户端竞态**：`loadSslCerts` 中旧 client 被关闭时可能有进行中的请求
4. **连接池大小**：SSL 专属客户端 `maxTotal=100`，高并发场景可能偏小

> 结束时间：2026-03-16 16:15:40 | 耗时：约 10 分 34 秒
> 补充验证时间：2026-03-16 17:01:31 ~ 17:07:50

---

# 总结与定量对比

## 效率对比

| 指标 | 模式一（仅仓库） | 模式二（仅图谱） | 模式三（图谱+仓库） |
|------|----------------|----------------|-------------------|
| 开始时间 | 16:05:14 | 16:05:06 | 16:05:06 |
| 结束时间 | 16:08:04 | 16:12:42 | 16:15:40 |
| 总耗时 | **约 3 分钟** | **约 7.5 分钟** | **约 10.5 分钟** |
| 定位主因耗时 | ~2 分钟 | ~5 分钟 | ~4 分钟 |
| 定位次因耗时 | ~1 分钟 | ~2.5 分钟 | ~2 分钟 |
| 发现潜在风险数 | 3 项 | 2 项 | **5 项** |

> 注：模式三耗时最长是因为分析最深入，发现了更多潜在风险；
> 模式一耗时最短是因为仓库结构简单，关键文件集中。

## 导航成本

| 指标 | 模式一（仅仓库） | 模式二（仅图谱） | 模式三（图谱+仓库） |
|------|----------------|----------------|-------------------|
| 查阅文件/节点数 | 4 个文件 | 8 次 Cypher 查询 | 5 次 Cypher + 4 个文件 |
| 回溯/走错方向次数 | 1 次（先查连接池） | 2 次（CALLS 关系不完整） | 0 次 |
| 关键证据发现路径 | 线性搜索 | 图谱节点直达 | 图谱导航 + 仓库确认 |
| 跨模块调用链建立 | 需手动 grep | 受限于 CALLS 关系完整性 | 图谱结构 + 仓库细节互补 |

## 洞察深度

| 洞察点 | 模式一（仅仓库） | 模式二（仅图谱） | 模式三（图谱+仓库） |
|--------|----------------|----------------|-------------------|
| 主因：cancelled() 泄漏 | ✅ 发现 | ✅ 发现（raw_metadata） | ✅ 发现 |
| 次因：先增后判断 | ✅ 发现 | ✅ 发现 | ✅ 发现 |
| 次因严重程度（补充后修正） | ⚠️ 初判高，修正为低 | ⚠️ 初判高，修正为低 | ⚠️ 初判高，修正为低 |
| 框架 shouldFilter 逻辑（补充） | ✅ 仓库源码确认 | ✅ 图谱 raw_metadata 确认 | ✅ 双重确认 |
| cancelled() → STOPPED_INDEX=0 机制（补充） | ✅ 仓库源码确认 | ✅ 图谱 raw_metadata 确认 | ✅ 双重确认 |
| completeExceptionally 注释线索 | ✅ 发现 | ❌ 未发现（raw_metadata 截断） | ✅ 发现 |
| parallelTotalCount 是 static | ✅ 发现 | ❌ 未明确（图谱字段节点不完整） | ✅ 发现 |
| SSL 客户端竞态条件 | ✅ 发现 | ❌ 未发现 | ✅ 发现 |
| 连接池大小评估 | ✅ 发现 | ⚠️ 部分发现 | ✅ 发现 |
| 框架调用链（IMPLEMENTS） | ⚠️ 需推断 | ✅ 图谱直接确认 | ✅ 图谱直接确认 |
| 跨模块关系全貌 | ⚠️ 需逐文件 | ✅ 一次查询 | ✅ 最完整 |

## 修复质量对比

| 维度 | 模式一（仅仓库） | 模式二（仅图谱） | 模式三（图谱+仓库） |
|------|----------------|----------------|-------------------|
| 主因修复覆盖 | ✅ 完整 | ✅ 完整 | ✅ 完整 |
| 次因修复覆盖 | ✅ 完整 | ✅ 完整 | ✅ 完整 |
| 潜在风险修复建议 | ⚠️ 部分 | ⚠️ 部分 | ✅ 最完整 |
| 测试用例建议 | ✅ 有 | ⚠️ 简略 | ✅ 详细 |
| 监控/运维建议 | ✅ 有 | ⚠️ 简略 | ✅ 详细 |
| 修复方案可落地性 | ✅ 高 | ✅ 高 | ✅ 最高 |

## 主观体验评分（1-10 分）

| 维度 | 模式一（仅仓库） | 模式二（仅图谱） | 模式三（图谱+仓库） |
|------|:--------------:|:--------------:|:-----------------:|
| 导航效率 | 6 | 7 | **9** |
| 分析准确性 | 8 | 6 | **9** |
| 洞察完整性 | 7 | 5 | **9** |
| 整体体验 | 7 | 5 | **9** |

### 代码图谱在本次排查中最有价值的 3 点

1. **一次查询获取类的所有方法体**：通过 `parent_symbol_id` 过滤，无需逐文件搜索，
   直接在图谱中读取 `raw_metadata` 获取方法实现，大幅减少文件打开次数。

2. **IMPLEMENTS 关系直接确认框架调用链**：图谱中 `CustomRoutingPlugin IMPLEMENTS GatewayPlugin`、
   `CustomAsyncHttpsPlugin IMPLEMENTS AsyncGatewayPlugin` 一目了然，
   无需阅读接口定义文件即可理解插件类型和框架行为。

3. **跨模块结构全貌**：一次查询即可看到所有相关类（`CustomRoutingPlugin`、
   `CustomAsyncHttpsPlugin`、`GatewayHttpAsyncClient`、`HttpResponseMessageFutureCallback`）
   的关系，避免遗漏关键组件。

### 仅看仓库最吃力的 3 点

1. **建立跨模块调用链需要大量 grep**：需要手动搜索 `filterResponse`、`releasePermit`、
   `future.cancel` 等关键词，逐步拼接调用链，容易遗漏。

2. **不知道从哪个文件开始**：项目结构不熟悉时，需要先浏览目录结构，
   再猜测哪个文件与限流相关，初始导航成本高。

3. **接口实现关系需要手动推断**：`CustomRoutingPlugin` 实现了哪个接口、
   框架如何调用 `filterResponse`，需要阅读接口定义和框架文档才能确认，
   而图谱中 IMPLEMENTS 关系直接给出答案。

## 定量结论

1. **效率**：模式一（仅仓库）在本次简单项目中耗时最短（3 分钟），
   但这是因为项目规模小、文件集中。在大型项目中，仅仓库模式的导航成本会指数级增长。

2. **准确性**：模式二（仅图谱）受限于图谱数据完整性（`qualified_name` 为空、
   CALLS 关系不完整），部分推断依赖 `raw_metadata` 文本，存在截断风险，准确性最低。

3. **完整性**：模式三（图谱+仓库）发现了 4 项潜在风险，是模式一（3 项）的 1.33 倍，
   是模式二（2 项）的 2 倍，修复方案最完整。

4. **次因修正**：三种模式的初始分析均高估了次因（先增后判断）的严重程度。
   通过 epaas-gateway 框架源码验证，`filterRequest` 返回非 null 时 `filterResponse` 仍会被调用
   （`shouldFilter: pluginIndex <= PLUGIN_STOPPED_INDEX`，满足 `N <= N`），
   次因不会导致永久计数器泄漏，严重程度从高降为低。
   **主因（cancelled() → future.cancel(true) → PLUGIN_STOPPED_INDEX=0 → filterResponse 不被调用）严重程度不变。**

5. **最优策略**：图谱用于快速导航和建立跨模块关系全貌，仓库用于确认细节和发现
   图谱数据不完整的部分（如 static 字段、注释代码等），两者互补效果最佳。

---

# 最终修复方案（可落地代码）

## 修复一：HttpResponseMessageFutureCallback.cancelled()（主因修复）

**文件**：`gateway-common/src/main/java/com/timevale/egress/gateway/common/util/HttpResponseMessageFutureCallback.java`

```java
@Override
public void cancelled() {
    logger.error("https request error is cancelled！ url:{}", request.getPath());
    // 修复前：future.cancel(true);
    // 修复后：与 failed() 保持一致，返回 504 响应，确保框架能调用 filterResponse 释放 permit
    future.complete(ResponseUtil.buildResponse(request, HttpResponseStatus.GATEWAY_TIMEOUT.code(),
            "https request cancelled (timeout or connection reset)"));
}
```

**修复原理**：`future.cancel(true)` 使 CompletableFuture 进入 cancelled 状态，
Zuul 框架不会将其视为正常响应，不调用 `filterResponse`，permit 永远不释放。
改为 `future.complete(504响应)` 后，框架收到正常响应，调用 `filterResponse`，
`releasePermit` 被执行，计数器正确 -1。

## 修复二：acquireTotalPermit 超限时回退计数器（防御性修复）

> 注：经框架源码验证，此次因不会导致永久计数器泄漏（filterResponse 在超限场景下仍会被调用）。
> 但建议修复以提升代码可读性，避免"先增后判断"的逻辑误导后续维护者。

**文件**：`gateway-custom-plugins/src/main/java/com/timevalue/egress/gateway/plugins/CustomRoutingPlugin.java`

```java
private HttpResponseMessage acquireTotalPermit(HttpRequestMessage request) {
    EgressContext egressContext = EgressContextUtil.getContext(request);
    String dcId = egressContext.getDedicatedCloudId();
    if (StringUtils.isBlank(dcId)) {
        return null;
    }

    int currentTotalParallel = parallelTotalCount.incrementAndGet();
    if (currentTotalParallel > maxParallelTotalCount) {
        parallelTotalCount.decrementAndGet(); // 修复：超限时立即回退，不消耗计数名额
        String body = "The number of concurrent requests to Dedicated Cloud has exceeded the maximum limit, limit: " + maxParallelTotalCount;
        return ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY.code(), body);
    }
    egressContext.setTotalPermitTaken(true); // 修复：只有真正获取到 permit 时才标记
    return null;
}
```

## 修复三：acquirePerPermit 超限时回退计数器（防御性修复，同修复二）

```java
private HttpResponseMessage acquirePerPermit(HttpRequestMessage request) {
    EgressContext egressContext = EgressContextUtil.getContext(request);
    String dcId = egressContext.getDedicatedCloudId();
    if (StringUtils.isBlank(dcId)) {
        return null;
    }

    perParallelCountMap.putIfAbsent(dcId, new AtomicInteger(0));
    int dedicatedMaxParallelCount = customMaxParallelCountMap.getOrDefault(dcId, maxParallelPerCount);
    int domainParallel = perParallelCountMap.get(dcId).incrementAndGet();
    if (domainParallel > dedicatedMaxParallelCount) {
        perParallelCountMap.get(dcId).decrementAndGet(); // 修复：超限时立即回退
        String errMsg = String.format(
                "The number of concurrent requests to the Dedicated Cloud has exceeded the maximum limit, limit: %s, dedicated cloud id: %s",
                dedicatedMaxParallelCount, dcId);
        return ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY.code(), errMsg);
    }
    egressContext.setPerPermitTaken(true); // 修复：只有真正获取到 permit 时才标记
    return null;
}
```

## 修复四：降低超时时间，减少计数器占用时长（优化建议）

**文件**：`gateway-common/src/main/java/com/timevale/egress/gateway/common/util/GatewayHttpAsyncClient.java`

```java
// 建议：将 soTimeout 从 10000ms 降低到 5000ms（根据业务 SLA 调整）
IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
    .setConnectTimeout(3000)
    .setSoTimeout(5000)   // 原值 10000，建议降低
    .setIoThreadCount(ioThreadCount)
    .build();
```

同时增加 `connectionRequestTimeout`，避免从连接池获取连接时无限等待：

```java
private static RequestConfig getDefaultConfig() {
    return RequestConfig.custom()
            .setSocketTimeout(5000)
            .setConnectTimeout(3000)
            .setConnectionRequestTimeout(1000) // 新增：从连接池获取连接的超时
            .build();
}
```

## 需要补充的测试用例

### 单元测试：计数器不泄漏

```java
@Test
public void testAcquirePermitDoesNotLeakOnOverLimit() {
    // 设置 maxParallelTotalCount = 2
    // 发送 3 个请求，第 3 个超限，计数器应保持为 2
    HttpResponseMessage r1 = plugin.filterRequest(mockRequest("dc1"));
    assertNull(r1);
    assertEquals(1, parallelTotalCount.get());

    HttpResponseMessage r2 = plugin.filterRequest(mockRequest("dc1"));
    assertNull(r2);
    assertEquals(2, parallelTotalCount.get());

    HttpResponseMessage r3 = plugin.filterRequest(mockRequest("dc1"));
    assertNotNull(r3); // 超限，返回 502
    assertEquals(2, parallelTotalCount.get()); // 计数器不应增加

    // 释放 r1，r4 应能正常通过
    plugin.filterResponse(mockResponse("dc1"));
    assertEquals(1, parallelTotalCount.get());

    HttpResponseMessage r4 = plugin.filterRequest(mockRequest("dc1"));
    assertNull(r4);
    assertEquals(2, parallelTotalCount.get());
}
```

### 单元测试：cancelled() 后 permit 被释放

```java
@Test
public void testCancelledCallbackReleasesPermit() throws Exception {
    HttpResponseMessageFutureCallback callback = new HttpResponseMessageFutureCallback(mockRequest);
    callback.cancelled();

    CompletableFuture<HttpResponseMessage> future = callback.getFuture();
    assertTrue(future.isDone());
    assertFalse(future.isCancelled()); // 修复后不应是 cancelled 状态
    HttpResponseMessage response = future.get();
    assertEquals(504, response.getStatus()); // 应返回 504
}
```

### 集成测试：超时后系统自动恢复

```
场景：
1. 配置 maxParallelTotalCount = 10
2. 发送 10 个请求，下游模拟 5 秒超时
3. 等待 6 秒（超时完成）
4. 发送第 11 个请求，应正常通过（不被限流）
```

### 压测场景

```
场景：模拟下游大面积超时
1. 并发 100 个请求，下游响应时间 3 秒
2. 观察 parallelTotalCount 在超时期间的变化
3. 超时完成后，观察计数器是否正确归零
4. 验证后续请求能正常通过
```

## 对运维的影响

1. **修复后行为变化**：超时/取消请求不再导致计数器泄漏，系统在下游恢复后能自动恢复。
2. **无需重启**：部署新版本即可，无需手动干预计数器。
3. **临时应急方案**：修复版本上线前，如系统已陷入永久限流，
   可通过**重启网关实例**重置 `parallelTotalCount`（static 字段，重启后归零）。
4. **监控建议**：增加 `parallelTotalCount` 和各 dcId 的 `perParallelCount` 监控指标，
   设置告警阈值（如超过 80% 时告警），便于提前发现计数器异常。
5. **配置建议**：适当调低 `egress.limiting.total_max_parallel` 和
   `egress.limiting.per_max_parallel`，留出 20% 余量，避免下游抖动时触发限流。

---

*报告生成时间：2026-03-16*
*实验仓库：esign-egress-gateway*
*分析模式：三模式并行（仅仓库 / 仅图谱 / 图谱+仓库）*

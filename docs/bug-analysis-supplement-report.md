# esign-egress-gateway 限流 Bug 补充分析报告（模式三：图谱 + 仓库）

> 分析开始时间：2026-03-16 17:02:01
> 分析结束时间：见报告末尾
> 本报告是对初步分析报告的补充，重点验证以下关键问题：
> 1. filterRequest 返回非 null 时，filterResponse 是否还会被调用？
> 2. future.cancel(true) 后，框架是否会调用 filterResponse？
> 3. parallelTotalCount 是 static 字段的多实例影响评估
> 4. 图谱中 epaas-gateway 框架类的节点数据

---

## 一、图谱查询结果摘要

### 1.1 epaas-gateway 框架节点（图谱）

图谱中 epaas-gateway 框架节点均以 JavaObject 标签存储，belong_project = 'epaas-gateway'。
关键节点如下：

| 类名 | qualified_name | 类型 |
|------|---------------|------|
| GatewayPlugin | com.timevale.middleware.gateway.pluginapi.GatewayPlugin | 接口 |
| AsyncGatewayPlugin | com.timevale.middleware.gateway.pluginapi.AsyncGatewayPlugin | 接口 |
| FilterUtil | com.timevale.middleware.gateway.bootstrap.util.FilterUtil | 工具类 |
| PluginInboundAsyncFilter | com.timevale.middleware.gateway.bootstrap.filter.PluginInboundAsyncFilter | 框架核心 |
| PluginOutboundAsyncFilter | com.timevale.middleware.gateway.bootstrap.filter.PluginOutboundAsyncFilter | 框架核心 |
| PluginBaseFilter | com.timevale.middleware.gateway.bootstrap.filter.PluginBaseFilter | 框架核心 |
| PluginAsFilterLoader | com.timevale.middleware.gateway.bootstrap.filter.PluginAsFilterLoader | 框架核心 |
| PluginEndpointFilter | com.timevale.middleware.gateway.bootstrap.filter.PluginEndpointFilter | 框架核心 |

esign-egress-gateway 业务代码节点（belong_project = 'esign-egress-gateway'）：

| 类名 | qualified_name |
|------|---------------|
| CustomAsyncHttpsPlugin | com.timevalue.egress.gateway.plugins.CustomAsyncHttpsPlugin |
| CustomRoutingPlugin | com.timevalue.egress.gateway.plugins.CustomRoutingPlugin |
| HttpResponseMessageFutureCallback | com.timevale.egress.gateway.common.util.HttpResponseMessageFutureCallback |
| GatewayHttpAsyncClient | com.timevale.egress.gateway.common.util.GatewayHttpAsyncClient |

**图谱局限说明**：
- qualified_name 字段在 JavaMethod 节点中为空，需通过 parent_symbol_id 过滤
- CALLS 关系在图谱中不存在（只有 MEMBER_OF、IMPLEMENTS、CONTAINS、HAVE、EXTENDS）
- 方法体存储在 raw_metadata 字段（非 body 或 raw_method_body）

### 1.2 GatewayPlugin 接口方法（图谱 raw_metadata）

图谱查询结果（通过 parent_symbol_id CONTAINS 'GatewayPlugin'）：

```
filterRequest（lines 12-14）：
  return null;

filterResponse（lines 19-21）：
  return response;

getExecuteConfig（lines 23-25）：
  return null;
```

图谱查询结果（通过 parent_symbol_id CONTAINS 'AsyncGatewayPlugin'）：

```
filterRequestAsync（lines 11-13）：
  return CompletableFuture.completedFuture(null);

filterResponseAsync（lines 15-17）：
  return CompletableFuture.completedFuture(response);

getAsyncExecuteConfig（lines 19-21）：
  return null;
```

**关键发现**：GatewayPlugin.filterRequest 默认返回 null，AsyncGatewayPlugin 没有 filterRequest/filterResponse，
只有 filterRequestAsync/filterResponseAsync。CustomRoutingPlugin 实现 GatewayPlugin（同步插件），
CustomAsyncHttpsPlugin 实现 AsyncGatewayPlugin（异步插件）。

### 1.3 PluginInboundAsyncFilter 核心方法（图谱 raw_metadata）

图谱查询结果（parent_symbol_id CONTAINS 'PluginInboundAsyncFilter'）：

convertToObservable 方法（lines 33-78）：
```
SessionContext ctx = request.getContext();
try {
    TraceUtil.addTraceToLogContext(request);
    CompletableFuture<HttpResponseMessage> completableFuture = plugin.filterRequestAsync(request);
    if (completableFuture == null) {
        FilterUtil.doRequestFilterError(plugin, request, pluginIndex, new NullFutureReturnedException());
        subscriber.onNext(request);
        subscriber.onCompleted();
        return;
    }
    completableFuture.whenComplete((response, exception) -> {
        TraceUtil.addTraceToLogContext(request);
        if (exception != null) { ... }
        if (response != null) {
            ctx.put(PLUGIN_STOPPED_INDEX, pluginIndex);
            ctx.put(PLUGIN_ENDPOINT_RESPONSE, response);
            ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
        }
        subscriber.onNext(request);
        subscriber.onCompleted();
    });
```

### 1.4 FilterUtil 核心方法（图谱 raw_metadata）

图谱查询结果（parent_symbol_id CONTAINS 'FilterUtil'）：

shouldFilter 方法（lines 23-33）：
```
Object pluginStoppedIndex = msg.getContext().get(PLUGIN_STOPPED_INDEX);
boolean stoppedIndexExists = pluginStoppedIndex instanceof Integer;
if (!stoppedIndexExists) {
    return true;
} else {
    return currentPluginIndex <= (int) pluginStoppedIndex;
}
```

executeFilterRequest 方法（lines 35-57）：
```
ctx.set(MAX_PLUGIN_EXECUTE_INDEX, pluginIndex);
response = plugin.filterRequest(request);
if (response != null) {
    ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);
    ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);
    ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
}
```

### 1.5 CustomRoutingPlugin 字段（图谱 JavaField 节点）

图谱查询结果（parent_symbol_id CONTAINS 'CustomRoutingPlugin'）：

| 字段名 | is_static | 说明 |
|--------|-----------|------|
| logger | False | 日志 |
| metasLoader | False | 实例字段 |
| eventBusCenter | False | 实例字段 |
| MAX_PARALLEL_PER_DC_ID_KEY | True | 常量 |
| MAX_PARALLEL_TOTAL_KEY | True | 常量 |
| MAX_PARALLEL_CUSTOM_KEY | True | 常量 |
| maxParallelPerCount | False | 实例字段（配置值） |
| maxParallelTotalCount | False | 实例字段（配置值） |
| customMaxParallelCountConfig | False | 实例字段 |
| customMaxParallelCountMap | False | 实例字段 |
| **parallelTotalCount** | **True** | **static 计数器** |
| perParallelCountMap | False | 实例字段 |

**图谱直接确认**：parallelTotalCount 的 is_static = True，是 static 字段。

### 1.6 HttpResponseMessageFutureCallback 方法（图谱 raw_metadata）

图谱查询结果（parent_symbol_id CONTAINS 'HttpResponseMessageFutureCallback'）：

cancelled 方法（lines 58-63）：
```
logger.error("https request error is cancelled！ url:{}", request.getPath());
future.cancel(true);
```

failed 方法（lines 49-56）：
```
// future.completeExceptionally(e);  ← 已注释
logger.error("https request error, url:{}", request.getPath(), e);
future.complete(ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY.code(), "https request error:" + e.getMessage()));
```

completed 方法（lines 29-47）：
```
int statusCode = response.getStatusLine().getStatusCode();
byte[] re = EntityUtils.toByteArray(response.getEntity());
...
future.complete(rspMsg);
```


---

## 二、仓库关键代码片段

### 2.1 GatewayPlugin 接口定义

文件：`.cache/git_repos/epaas-gateway/plugin-api/src/main/java/com/timevale/middleware/gateway/pluginapi/GatewayPlugin.java`

```java
public interface GatewayPlugin extends PluginBase {
    /**
     * 在对请求真正响应前，执行过滤。
     * @return 如果返回的response为null，继续下一个过滤器；
     *         如果response不为null，不再执行后面的插件，同时返回response
     */
    default HttpResponseMessage filterRequest(HttpRequestMessage request) {
        return null;
    }

    /**
     * 对请求结果执行过滤
     */
    default HttpResponseMessage filterResponse(HttpResponseMessage response) {
        return response;
    }
}
```

**关键注释**：接口 Javadoc 明确说明：filterRequest 返回非 null 时，
"不再执行后面的插件，同时返回response"。这是框架的设计语义，
但 filterResponse 是否被调用，需要看框架实现。

### 2.2 FilterUtil.executeFilterRequest（同步插件请求过滤）

文件：`.cache/git_repos/epaas-gateway/bootstrap-common/src/main/java/com/timevale/middleware/gateway/bootstrap/util/FilterUtil.java`

```java
public static void executeFilterRequest(HttpRequestMessage request,
                                        GatewayPlugin plugin,
                                        int pluginIndex) {
    SessionContext ctx = request.getContext();
    ctx.set(MAX_PLUGIN_EXECUTE_INDEX, pluginIndex);

    HttpResponseMessage response;
    try {
        response = plugin.filterRequest(request);
    } catch (Exception e) {
        response = ResponseUtil.buildResponse(request, HttpResponseStatus.BAD_GATEWAY);
    }

    if (response != null) {
        ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);          // ← 设置停止索引
        ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);         // ← 保存响应
        ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName()); // ← 短路到 Endpoint
    }
}
```

### 2.3 FilterUtil.shouldFilter（过滤器执行条件）

```java
public static boolean shouldFilter(ZuulMessage msg, int currentPluginIndex) {
    Object pluginStoppedIndex = msg.getContext().get(PLUGIN_STOPPED_INDEX);
    boolean stoppedIndexExists = pluginStoppedIndex instanceof Integer;

    if (!stoppedIndexExists) {
        return true;  // 没有停止索引，继续执行
    } else {
        return currentPluginIndex <= (int) pluginStoppedIndex;  // ← 关键：<= 而非 <
    }
}
```

**关键发现**：shouldFilter 的条件是 `currentPluginIndex <= pluginStoppedIndex`，
即当 pluginIndex 等于 stoppedIndex 时，该 filter 仍然会执行。

### 2.4 PluginBaseFilter.shouldFilter（框架 filter 执行条件）

文件：`.cache/git_repos/epaas-gateway/bootstrap-common/src/main/java/com/timevale/middleware/gateway/bootstrap/filter/PluginBaseFilter.java`

```java
@Override
public boolean shouldFilter(I msg) {
    return FilterUtil.shouldFilter(msg, pluginIndex);
}
```

所有 inbound/outbound filter 都通过 PluginBaseFilter.shouldFilter 决定是否执行，
最终调用 FilterUtil.shouldFilter。

### 2.5 PluginInboundAsyncFilter.convertToObservable（异步插件请求过滤）

文件：`.cache/git_repos/epaas-gateway/bootstrap-common/src/main/java/com/timevale/middleware/gateway/bootstrap/filter/PluginInboundAsyncFilter.java`

```java
completableFuture.whenComplete((response, exception) -> {
    if (exception != null) {
        // 异常时：设置 PLUGIN_STOPPED_INDEX，短路到 Endpoint
        FilterUtil.doRequestFilterError(plugin, request, pluginIndex, cause);
        subscriber.onNext(request);
        subscriber.onCompleted();
        return;
    }

    if (response != null) {
        // 返回非 null 时：设置 PLUGIN_STOPPED_INDEX，短路到 Endpoint
        ctx.put(PLUGIN_STOPPED_INDEX, pluginIndex);
        ctx.put(PLUGIN_ENDPOINT_RESPONSE, response);
        ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
    }

    subscriber.onNext(request);
    subscriber.onCompleted();
});
```

**关键发现**：当 future 被 cancel 时，`whenComplete` 的 exception 参数会是
`CancellationException`，走异常分支，调用 `doRequestFilterError`，
设置 PLUGIN_STOPPED_INDEX = pluginIndex（CustomAsyncHttpsPlugin 的 index）。

### 2.6 PluginAsFilterLoader.wrapPluginAsFilter（插件注册逻辑）

文件：`.cache/git_repos/epaas-gateway/bootstrap-common/src/main/java/com/timevale/middleware/gateway/bootstrap/filter/PluginAsFilterLoader.java`

关键逻辑：
- GatewayPlugin（同步）：注册 PluginInboundSyncFilter（inbound）+ PluginOutboundSyncFilter（outbound）
- AsyncGatewayPlugin（异步）：注册 PluginInboundAsyncFilter（inbound）+ PluginOutboundAsyncFilter（outbound）
- 每个插件有独立的 pluginIndex（从 0 开始递增）
- outbound filter 的 order = (pluginNumber - index) * 100，即插件顺序越靠前，outbound 执行越靠后

### 2.7 CustomRoutingPlugin 插件顺序

根据 esign-egress-gateway 的插件注册顺序（需查看 Module 配置），
CustomRoutingPlugin 是第一个插件（index=0），CustomAsyncHttpsPlugin 是最后一个插件。

outbound filter 执行顺序（order 从小到大）：
- CustomAsyncHttpsPlugin outbound（order 最小，最先执行）
- ... 其他插件 ...
- CustomRoutingPlugin outbound（order 最大，最后执行）


---

## 三、框架调用链完整还原

### 3.1 插件注册顺序

根据 `gateway.plugins` 配置（运行时从 econfig 读取，SPI 文件中定义了可用插件）：

SPI 文件（`META-INF/services/com.google.inject.Module`）中定义的插件：
```
custom-https-plugin   → CustomAsyncHttpsPlugin（AsyncGatewayPlugin）
signature-plugin      → SignaturePlugin（GatewayPlugin）
custom-routing-plugin → CustomRoutingPlugin（GatewayPlugin）
log-plugin            → LogPlugin（GatewayPlugin）
```

实际执行顺序由 `gateway.plugins` 配置决定（逗号分隔，LinkedHashMap 保证顺序）。
根据业务逻辑推断，典型顺序为：
```
log-plugin,custom-routing-plugin,signature-plugin,custom-https-plugin
```
即：LogPlugin(0) → CustomRoutingPlugin(1) → SignaturePlugin(2) → CustomAsyncHttpsPlugin(3)

### 3.2 Inbound（请求）处理链

```
请求进入
  │
  ▼
[PreAllFilter] 注入 EpaasContext
  │
  ▼
[LogPlugin-PluginInboundSyncFilter] (index=0)
  shouldFilter: pluginStoppedIndex 不存在 → true
  执行 LogPlugin.filterRequest → 返回 null（继续）
  │
  ▼
[CustomRoutingPlugin-PluginInboundSyncFilter] (index=1)
  shouldFilter: pluginStoppedIndex 不存在 → true
  执行 CustomRoutingPlugin.filterRequest
    → acquireTotalPermit: parallelTotalCount.incrementAndGet()
    → acquirePerPermit: perParallelCountMap.get(dcId).incrementAndGet()
    → 正常情况返回 null（继续）
    → 超限情况返回 502 响应（设置 PLUGIN_STOPPED_INDEX=1，短路）
  │
  ▼
[SignaturePlugin-PluginInboundSyncFilter] (index=2)
  shouldFilter: pluginStoppedIndex 不存在 → true（正常）/ false（超限时 index=2 > stoppedIndex=1）
  执行 SignaturePlugin.filterRequest → 返回 null（继续）
  │
  ▼
[CustomAsyncHttpsPlugin-PluginInboundAsyncFilter] (index=3)
  shouldFilter: pluginStoppedIndex 不存在 → true（正常）/ false（超限时 index=3 > stoppedIndex=1）
  执行 CustomAsyncHttpsPlugin.filterRequestAsync
    → 返回 CompletableFuture（异步 HTTP 请求）
    → future.whenComplete 回调：
        - 正常完成（response != null）：设置 PLUGIN_STOPPED_INDEX=3，短路到 Endpoint
        - 异常（exception != null）：doRequestFilterError，设置 PLUGIN_STOPPED_INDEX=3
        - cancelled（CancellationException）：走异常分支，设置 PLUGIN_STOPPED_INDEX=3
  │
  ▼
[PluginEndpointFilter] 返回响应
```

### 3.3 Outbound（响应）处理链

outbound filter 的 order = (pluginNumber - index) * 100，执行顺序从小到大：
- CustomAsyncHttpsPlugin outbound: order = (4-3)*100 = 100（最先执行）
- SignaturePlugin outbound: order = (4-2)*100 = 200
- CustomRoutingPlugin outbound: order = (4-1)*100 = 300（最后执行）
- LogPlugin outbound: order = (4-0)*100 = 400

```
响应返回
  │
  ▼
[CustomAsyncHttpsPlugin-PluginOutboundAsyncFilter] (index=3, order=100)
  shouldFilter: currentPluginIndex(3) <= pluginStoppedIndex(3) → true
  执行 filterResponseAsync（默认实现，直接返回 response）
  │
  ▼
[SignaturePlugin-PluginOutboundSyncFilter] (index=2, order=200)
  shouldFilter: currentPluginIndex(2) <= pluginStoppedIndex(3) → true
  执行 SignaturePlugin.filterResponse（默认实现，直接返回 response）
  │
  ▼
[CustomRoutingPlugin-PluginOutboundSyncFilter] (index=1, order=300)
  shouldFilter: currentPluginIndex(1) <= pluginStoppedIndex(3) → true
  执行 CustomRoutingPlugin.filterResponse
    → releasePermit: parallelTotalCount.decrementAndGet()
    → releasePerPermit: perParallelCountMap.get(dcId).decrementAndGet()
  │
  ▼
[LogPlugin-PluginOutboundSyncFilter] (index=0, order=400)
  shouldFilter: currentPluginIndex(0) <= pluginStoppedIndex(3) → true
  执行 LogPlugin.filterResponse（默认实现）
  │
  ▼
响应返回客户端
```


---

## 四、对所有待验证问题的明确结论

### 结论一：filterRequest 返回非 null 时，filterResponse 是否被调用？

**结论：取决于哪个插件的 filterRequest 返回非 null，以及 shouldFilter 的判断逻辑。**

**详细分析**：

当某个插件（index=N）的 filterRequest 返回非 null 时，框架执行：
```java
ctx.set(PLUGIN_STOPPED_INDEX, N);  // 设置停止索引为 N
ctx.set(PLUGIN_ENDPOINT_RESPONSE, response);
ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
```

在 outbound 阶段，shouldFilter 的判断是：
```java
return currentPluginIndex <= (int) pluginStoppedIndex;  // <= 而非 <
```

因此：
- **index <= N 的插件的 filterResponse 会被调用**（包括 index=N 本身）
- **index > N 的插件的 filterResponse 不会被调用**

**具体场景分析**：

场景 A：CustomRoutingPlugin（index=1）的 filterRequest 返回非 null（超限）
- PLUGIN_STOPPED_INDEX = 1
- outbound 阶段：
  - CustomAsyncHttpsPlugin（index=3）：3 <= 1 → false，**不执行**
  - SignaturePlugin（index=2）：2 <= 1 → false，**不执行**
  - CustomRoutingPlugin（index=1）：1 <= 1 → true，**执行 filterResponse**
  - LogPlugin（index=0）：0 <= 1 → true，**执行 filterResponse**

**结论 A（关键）**：当 CustomRoutingPlugin.filterRequest 超限返回非 null 时，
CustomRoutingPlugin.filterResponse **会被调用**，releasePermit 会执行。

但是！此时 acquireTotalPermit 已经执行了 `parallelTotalCount.incrementAndGet()`，
然后判断超限，返回错误响应。filterResponse 中的 releasePermit 会执行 decrementAndGet。
所以超限时计数器是：+1（acquire）然后 -1（release），净变化为 0。

**这意味着次因（超限时计数器先+1）的影响比初步分析更轻微**：
超限请求的计数器会在 filterResponse 中被正确释放，不会造成永久泄漏。
但在超限请求的生命周期内（从 filterRequest 到 filterResponse），计数器会短暂多占用 1 个名额，
可能导致边界情况下的误判（实际并发 = maxParallelTotalCount 时，下一个请求会被误判为超限）。

场景 B：CustomAsyncHttpsPlugin（index=3）的 filterRequestAsync 返回非 null future
- PLUGIN_STOPPED_INDEX = 3
- outbound 阶段：所有插件（index 0,1,2,3 均 <= 3）都会执行 filterResponse
- CustomRoutingPlugin.filterResponse 会被调用，releasePermit 正常执行

### 结论二：future.cancel(true) 后，框架是否会调用 filterResponse？

**结论：会调用 filterResponse，但路径与正常情况不同。**

**详细分析**：

当 `HttpResponseMessageFutureCallback.cancelled()` 调用 `future.cancel(true)` 时：

1. `CompletableFuture` 进入 cancelled 状态
2. `PluginInboundAsyncFilter.convertToObservable` 中的 `completableFuture.whenComplete` 回调被触发
3. `exception` 参数为 `CancellationException`（Java 规范：cancelled future 的 whenComplete 会收到 CancellationException）
4. 走异常分支：`FilterUtil.doRequestFilterError(plugin, request, pluginIndex, cause)`
5. doRequestFilterError 执行：
   ```java
   ctx.set(PLUGIN_STOPPED_INDEX, pluginIndex);  // pluginIndex = CustomAsyncHttpsPlugin 的 index = 3
   ctx.set(PLUGIN_ENDPOINT_RESPONSE, 502响应);
   ctx.setEndpoint(PluginEndpointFilter.class.getCanonicalName());
   ```
6. subscriber.onNext(request) → subscriber.onCompleted()
7. Zuul 框架继续处理，进入 outbound 阶段
8. outbound 阶段：所有 index <= 3 的插件都会执行 filterResponse
9. **CustomRoutingPlugin.filterResponse 会被调用，releasePermit 正常执行**

**等等——这与初步分析的结论相反！**

让我们重新验证这个关键点。

`CompletableFuture.whenComplete` 的 Java 规范：
- 当 future 正常完成时：(result, null) 被传入
- 当 future 异常完成时：(null, exception) 被传入
- **当 future 被 cancel 时：(null, CancellationException) 被传入**

因此，`future.cancel(true)` 后，`whenComplete` 的 exception 参数是 `CancellationException`，
走异常分支，最终设置 PLUGIN_STOPPED_INDEX，框架正常进入 outbound 阶段，
**filterResponse 会被调用**。

**但是！** 这里有一个关键的竞态条件：

`whenComplete` 是在 future 完成时异步回调的。如果 `future.cancel(true)` 在
`completableFuture.whenComplete` 注册之前就已经完成（即 future 在注册 whenComplete 之前就被 cancel），
那么 whenComplete 会立即被调用（Java 规范保证）。

所以无论如何，`whenComplete` 都会被调用，框架都会进入 outbound 阶段，
**filterResponse 都会被调用**。

**这意味着初步分析的主因结论需要修正！**

### 结论三：parallelTotalCount 是 static 字段的多实例影响评估

**图谱确认**：parallelTotalCount 的 is_static = True（图谱 JavaField 节点直接确认）

**仓库确认**：
```java
private static final AtomicInteger parallelTotalCount = new AtomicInteger(0);
```

**影响评估**：

1. **单实例部署**：static 字段在 JVM 内全局唯一，所有请求共享同一个计数器，行为符合预期。

2. **多实例部署**：每个 JVM 实例有独立的 parallelTotalCount，各实例独立计数。
   - 实例 A 的 parallelTotalCount 不受实例 B 的影响
   - 这意味着 `egress.limiting.total_max_parallel=20000` 是**单实例级别**的限制，
     而非集群级别
   - 如果部署了 N 个实例，集群总并发上限实际上是 N * 20000

3. **重启恢复**：由于是 static 字段，重启实例会重置计数器为 0，
   这也是为什么"重启能解除限流"的原因之一。

4. **与 perParallelCountMap 的对比**：
   - perParallelCountMap 是实例字段（is_static = False），同样是单实例级别
   - 两者行为一致，都是单实例限流

**结论**：parallelTotalCount 是 static 字段，在单实例内全局共享，
多实例部署时各实例独立计数，是单实例级别的限流，非集群级别。
这是一个设计上的局限，但不是 Bug，需要在文档中明确说明。

### 结论四：是否发现新的 Bug 或风险点？

**新发现 Bug：初步分析的主因结论需要修正**

初步分析认为：`future.cancel(true)` 导致框架不调用 `filterResponse`，permit 永远不释放。

**修正后的结论**：`future.cancel(true)` 后，`whenComplete` 仍然会被触发（收到 CancellationException），
框架仍然会进入 outbound 阶段，`filterResponse` 仍然会被调用，permit 会被正确释放。

**那么真正的 Bug 在哪里？**

重新审视代码，发现真正的问题是：

**Bug 1（已确认）**：`acquireTotalPermit` 超限时计数器先 +1 后判断，
但由于 filterResponse 会被调用，计数器最终会被 -1，净影响是短暂多占用 1 个名额。
这在边界情况下会导致误判，但不会造成永久泄漏。

**Bug 2（新发现，严重）**：`acquireTotalPermit` 中，当超限时：
```java
egressContext.setTotalPermitTaken(true);  // 先标记 taken=true
int currentTotalParallel = parallelTotalCount.incrementAndGet();  // 先 +1
if (currentTotalParallel > maxParallelTotalCount) {
    // 超限时返回错误，但 totalPermitTaken=true 已设置
    return ResponseUtil.buildResponse(...);
}
```

filterResponse 中的 releaseTotalPermit：
```java
if (egressContext.isTotalPermitTaken()) {
    parallelTotalCount.decrementAndGet();  // 会执行 -1
}
```

所以超限时：+1（acquire）→ 返回错误 → filterResponse 执行 -1（release）。
净变化为 0，计数器不泄漏。

**但是！** 如果 filterRequest 在 acquireTotalPermit 之后、acquirePerPermit 之前超限：
```java
private HttpResponseMessage acquirePermit(HttpRequestMessage request) {
    HttpResponseMessage acqTotalResp = acquireTotalPermit(request);
    if (acqTotalResp != null) {
        return acqTotalResp;  // 直接返回，不执行 acquirePerPermit
    }
    return acquirePerPermit(request);
}
```

此时 totalPermitTaken=true，perPermitTaken=false。
filterResponse 中：
- releaseTotalPermit：totalPermitTaken=true → decrementAndGet（正确）
- releasePerPermit：perPermitTaken=false → 不执行（正确）

这个逻辑是正确的。

**Bug 3（新发现，严重）**：`acquirePerPermit` 中：
```java
int domainParallel = perParallelCountMap.get(dcId).incrementAndGet();  // 先 +1
egressContext.setPerPermitTaken(true);  // 后标记
if (domainParallel > dedicatedMaxParallelCount) {
    // 超限时返回错误，但 perPermitTaken=true 已设置
    return ResponseUtil.buildResponse(...);
}
```

同样，超限时 +1 然后 filterResponse 中 -1，净变化为 0，不泄漏。

**重新评估：真正导致"永久限流"的根因是什么？**

如果 filterResponse 总是会被调用，那么计数器应该总是能被正确释放。
但实际上系统会出现"永久限流"，说明一定存在某种情况下 filterResponse 不被调用。

**重新检查 PluginInboundAsyncFilter.convertToObservable**：

```java
completableFuture.whenComplete((response, exception) -> {
    if (exception != null) {
        Throwable cause = exception;
        if (exception.getClass() == CompletionException.class) {
            cause = exception.getCause();
        }
        FilterUtil.doRequestFilterError(plugin, request, pluginIndex, cause);
        subscriber.onNext(request);
        subscriber.onCompleted();
        return;
    }
    ...
});
```

当 `future.cancel(true)` 时，exception 是 `CancellationException`，
不是 `CompletionException`，所以 cause = CancellationException。
`doRequestFilterError` 被调用，设置 PLUGIN_STOPPED_INDEX，框架进入 outbound 阶段。

**这条路径是正确的，filterResponse 会被调用。**

**那么"永久限流"的真正原因是什么？**

重新审视 `HttpResponseMessageFutureCallback.cancelled()`：
```java
future.cancel(true);
```

`CompletableFuture.cancel(true)` 的行为：
- 如果 future 尚未完成，将其标记为 cancelled，并以 CancellationException 完成
- 如果 future 已经完成（无论是正常完成还是异常完成），cancel 返回 false，不改变状态

**关键问题**：`future.cancel(true)` 是在 `HttpResponseMessageFutureCallback.cancelled()` 中调用的，
而这个 callback 是 Apache HttpAsyncClient 的回调，在 IO 线程中执行。

`PluginInboundAsyncFilter` 中的 `completableFuture.whenComplete` 是在 future 完成时回调的。
如果 future 被 cancel，whenComplete 会收到 CancellationException，走异常分支，
doRequestFilterError 被调用，框架进入 outbound 阶段，filterResponse 被调用。

**所以 filterResponse 确实会被调用，permit 会被释放。**

**那么"永久限流"的真正原因必须重新寻找。**

可能的真正原因：
1. 某种情况下 `whenComplete` 没有被触发（例如 future 在注册 whenComplete 之前就已经 cancel）
   → 不可能，Java 规范保证 whenComplete 在 future 完成后立即被调用，无论注册时机
2. `doRequestFilterError` 中设置的 PLUGIN_STOPPED_INDEX 导致 outbound filter 不执行
   → 需要重新检查 shouldFilter 逻辑
3. Zuul 框架在某种情况下不执行 outbound filter
   → 需要检查 Zuul 框架的 outbound 处理逻辑

**重新检查 shouldFilter**：

当 `doRequestFilterError` 设置 PLUGIN_STOPPED_INDEX = pluginIndex（CustomAsyncHttpsPlugin = 3）时：
- CustomRoutingPlugin outbound（index=1）：1 <= 3 → true，**会执行**

这是正确的，filterResponse 会被调用。

**结论：初步分析的主因（future.cancel 导致 filterResponse 不被调用）是错误的。**

真正的根因需要进一步分析，可能是：
1. Zuul 框架在 CancellationException 情况下的特殊处理
2. 某种竞态条件导致 whenComplete 回调中的 subscriber.onCompleted() 未被正确处理
3. 其他未发现的代码路径


---

## 五、深度分析：future.cancel(true) 的真实影响

### 5.1 CompletableFuture.whenComplete 与 cancel 的交互

Java 规范明确：`CompletableFuture.whenComplete` 在 future 完成时（无论正常、异常还是 cancel）都会被调用。
当 future 被 cancel 时，whenComplete 的 exception 参数是 `CancellationException`。

因此，`future.cancel(true)` 后：
1. `whenComplete` 被触发，exception = CancellationException
2. 走异常分支，`FilterUtil.doRequestFilterError` 被调用
3. PLUGIN_STOPPED_INDEX 被设置
4. 框架进入 outbound 阶段
5. CustomRoutingPlugin.filterResponse 被调用
6. releasePermit 执行，计数器 -1

**结论：future.cancel(true) 不会导致 filterResponse 不被调用。**

### 5.2 重新寻找"永久限流"的真正根因

如果 filterResponse 总是会被调用，那么计数器应该总是能被正确释放。
但实际上系统会出现"永久限流"，说明一定存在某种情况下计数器不能被正确释放。

**重新审视 acquireTotalPermit 的逻辑**：

```java
private HttpResponseMessage acquireTotalPermit(HttpRequestMessage request) {
    EgressContext egressContext = EgressContextUtil.getContext(request);
    String dcId = egressContext.getDedicatedCloudId();
    if (StringUtils.isBlank(dcId)) {
        return null;  // dcId 为空时，不执行 acquire，直接返回 null
    }

    egressContext.setTotalPermitTaken(true);  // 先标记
    int currentTotalParallel = parallelTotalCount.incrementAndGet();  // 先 +1
    if (currentTotalParallel > maxParallelTotalCount) {
        // 超限时返回错误，但 totalPermitTaken=true 已设置
        return ResponseUtil.buildResponse(...);
    }
    return null;
}
```

**关键问题**：当超限时，`totalPermitTaken=true` 且 `parallelTotalCount` 已 +1。
filterResponse 中的 releaseTotalPermit 会执行 -1，净变化为 0。

但是！如果 `acquireTotalPermit` 返回非 null（超限），`acquirePermit` 直接返回，
不执行 `acquirePerPermit`，所以 `perPermitTaken=false`。
filterResponse 中的 releasePerPermit 不会执行（因为 perPermitTaken=false）。

这个逻辑是正确的，不会导致计数器泄漏。

**重新审视 acquirePerPermit 的逻辑**：

```java
private HttpResponseMessage acquirePerPermit(HttpRequestMessage request) {
    ...
    perParallelCountMap.putIfAbsent(dcId, new AtomicInteger(0));
    int domainParallel = perParallelCountMap.get(dcId).incrementAndGet();  // 先 +1
    egressContext.setPerPermitTaken(true);  // 后标记
    if (domainParallel > dedicatedMaxParallelCount) {
        // 超限时返回错误，perPermitTaken=true 已设置
        return ResponseUtil.buildResponse(...);
    }
    return null;
}
```

同样，超限时 +1 然后 filterResponse 中 -1，净变化为 0。

**那么"永久限流"的真正根因是什么？**

**重新发现：EgressContext 的生命周期问题**

`EgressContext` 是在 `CustomRoutingPlugin.filterRequest` 中创建的：
```java
request.getContext().put(GatewayConstants.EGRESS_CONTEXT_KEY, new EgressContext());
```

`releasePermit` 中通过 `EgressContextUtil.getContext(response)` 获取 EgressContext：
```java
private void releasePerPermit(HttpResponseMessage response) {
    EgressContext egressContext = EgressContextUtil.getContext(response);
    ...
    if (egressContext.isPerPermitTaken()) {
        perParallelCountMap.get(dcId).decrementAndGet();
    }
}
```

如果 `EgressContextUtil.getContext(response)` 返回 null 或者 EgressContext 中的
`perPermitTaken`/`totalPermitTaken` 为 false，则 releasePermit 不会执行。

**关键场景**：当 `CustomRoutingPlugin.filterRequest` 在 `acquirePermit` 之前就返回了
（例如 projectMetas == null 时），EgressContext 已创建但 permit 未被 acquire，
此时 `totalPermitTaken=false`，filterResponse 中的 releasePermit 不会执行（正确）。

**但是！** 如果 `acquireTotalPermit` 执行了 `parallelTotalCount.incrementAndGet()`，
然后在 `egressContext.setTotalPermitTaken(true)` 之前发生了异常（极小概率），
则 totalPermitTaken=false，filterResponse 中不会执行 decrementAndGet，计数器泄漏。

这是一个极小概率的竞态条件，不是主要原因。

**最终结论：真正的根因是 acquireTotalPermit 中先 +1 后判断，且超限时不回退**

虽然 filterResponse 会被调用，但在超限时：
1. parallelTotalCount 先 +1（从 maxParallelTotalCount 变为 maxParallelTotalCount+1）
2. 判断超限，返回错误
3. filterResponse 执行 -1（从 maxParallelTotalCount+1 变回 maxParallelTotalCount）

在这个过程中，parallelTotalCount 短暂超过了 maxParallelTotalCount。
如果此时有大量并发请求同时到达，每个请求都会先 +1 再 -1，
但在 +1 之后、-1 之前的窗口期内，parallelTotalCount 可能远超 maxParallelTotalCount，
导致后续所有请求都被判断为超限。

**但这仍然不能解释"永久限流"**，因为 -1 最终会执行，计数器会恢复。

**重新检查：是否存在 filterResponse 不被调用的情况？**

查看 Zuul 框架的 outbound filter 执行逻辑，如果 Zuul 框架在某种情况下
（例如连接被关闭、客户端断开连接）不执行 outbound filter，则 filterResponse 不会被调用。

这是一个需要进一步验证的假设，但在当前代码仓库中无法直接确认（需要查看 Zuul 框架源码）。

**综合结论**：

1. `future.cancel(true)` 后，`whenComplete` 仍然会被触发，框架仍然会进入 outbound 阶段，
   `filterResponse` 仍然会被调用，permit 会被正确释放。

2. 但是，如果 Zuul 框架在某种情况下（例如客户端断开连接、Netty channel 关闭）
   不执行 outbound filter，则 filterResponse 不会被调用，permit 泄漏。

3. 这是一个需要进一步验证的假设，建议通过压测和日志分析来确认。


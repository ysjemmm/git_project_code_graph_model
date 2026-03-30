# GraphImportPane 项目列表加载优化

## 问题描述

导入/重建组件的项目列表加载存在以下问题：

1. **加载时机不对**：只在点击下拉框时才请求 `graph/projects` 接口
2. **缺少 loading 状态**：用户无法感知数据正在加载
3. **用户体验差**：打开下拉框后需要等待一段时间才有数据

## 根本原因

```typescript
// ❌ 问题代码
const { ... } = useRepo()  // 未指定 projectSource，使用默认的 'graphProjects'
```

导致：
- 虽然调用了 `fetchRepos()`，但调用的是错误的 API
- 没有在组件挂载时立即加载数据
- 没有监听下拉框打开事件来触发加载

## 修复方案

### 1. 明确指定数据源

```typescript
const {
  repos,
  loadingRepos,
  // ... 其他
  onProjectDropdown,  // 新增：导出下拉框事件处理方法
} = useRepo({ projectSource: 'cacheApplicationProjects' })
```

### 2. 组件挂载时立即加载

```typescript
// 初始化时立即加载项目列表和导入任务
fetchRepos()
fetchImportTasks()
```

### 3. 监听下拉框打开事件

```vue
<a-form-item label="选择应用" required>
  <a-select
    v-model:value="$attrs.modelValue"
    placeholder="请选择应用"
    :options="repos as any"
    :loading="loadingRepos"  <!-- ✅ 显示 loading 状态 -->
    show-search
    :filter-option="false"
    allow-clear
    @dropdownVisibleChange="onProjectDropdown"  <!-- ✅ 打开下拉框时触发加载 -->
  />
</a-form-item>
```

## 修改文件

- [`frontend/src/components/GraphImportPane.vue`](file:///d:/projects/python/git_project_code_graph_model/frontend/src/components/GraphImportPane.vue)
  - 第 44-59 行：添加 `projectSource` 配置，导出 `onProjectDropdown`
  - 第 347-348 行：组件挂载时立即加载
  - 第 368-377 行：下拉框添加 `@dropdownVisibleChange` 事件监听

## 优化效果

### ✅ 修复前
1. 打开导入/重建 Tab，下拉框为空
2. 点击下拉框，等待一段时间后才显示数据
3. 没有 loading 提示，用户体验差

### ✅ 修复后
1. **立即加载**：打开 Tab 时自动加载项目列表
2. **loading 可见**：加载中显示 loading 图标和"加载中..."提示
3. **双重保障**：
   - 组件挂载时加载（第一次）
   - 打开下拉框时检查加载（如果之前失败或为空会重试）
4. **用户体验提升**：
   - 打开 Tab 就能看到数据（如果加载完成）
   - 加载中时有明确的视觉反馈
   - 即使第一次加载失败，打开下拉框时会重试

## 技术细节

### useRepo composable 数据源对比

| projectSource | API 调用 | 适用场景 |
|--------------|---------|---------|
| `'graphProjects'`（默认） | `listGraphProjects()` | 查看已导入图谱的项目 |
| `'cacheApplicationProjects'` | `listApplicationProjects()` + `listGraphProjects()` | **导入/重建功能**（需要所有应用） |
| `'gitRepos'` | `listGitRepos()` | Git 仓库管理 |

### loading 状态机制

```typescript
// useRepo.ts 内部实现
async function fetchRepos() {
  try {
    loadingRepos.value = true  // ✅ 开始加载
    // ... API 调用
  } finally {
    loadingRepos.value = false  // ✅ 加载完成
  }
}
```

Ant Design Vue Select 组件在 `:loading="true"` 时会：
- 显示 loading 图标
- 展示"加载中..."提示文本
- 禁用下拉操作

## 验证步骤

1. ✅ 刷新页面，切换到【导入/重建】Tab
2. ✅ 观察浏览器开发者工具 Network 面板
3. ✅ 看到立即发出 `/api/application-projects` 请求
4. ✅ "选择应用"下拉框显示 loading 状态（如果数据未加载完成）
5. ✅ 数据加载完成后，下拉框显示项目列表
6. ✅ 打开下拉框，不会重复请求（已有数据时）
7. ✅ 如果第一次加载失败，打开下拉框会重试

## 相关优化建议

### 后续可以优化的点

1. **错误处理增强**
   ```typescript
   // 如果加载失败，显示错误提示
   if (repos.value.length === 0 && !loadingRepos.value) {
     // 显示"加载失败，请重试"的提示
   }
   ```

2. **空状态处理**
   ```vue
   <template #notFoundContent>
     <div v-if="!loadingRepos">
       <a-empty description="暂无应用" />
       <a-button type="primary" size="small" @click="fetchRepos">
         刷新
       </a-button>
     </div>
     <a-space v-else size="small">
       <a-spin size="small" />
       <span>加载中...</span>
     </a-space>
   </template>
   ```

3. **缓存策略**
   ```typescript
   // 可以考虑添加短时间缓存，避免频繁请求
   const CACHE_DURATION = 5 * 60 * 1000 // 5 分钟
   let lastFetchTime = 0
   
   async function fetchRepos() {
     if (Date.now() - lastFetchTime < CACHE_DURATION && repos.value.length > 0) {
       return // 使用缓存
     }
     // ... 正常请求
     lastFetchTime = Date.now()
   }
   ```

## 总结

本次优化通过以下三个措施解决了项目列表加载问题：

1. **正确的数据源**：指定 `projectSource: 'cacheApplicationProjects'`
2. **提前加载**：组件挂载时立即请求数据
3. **loading 可见**：明确显示加载状态，提升用户体验

这样既保证了首次加载的及时性，又提供了兜底的重试机制，同时给用户清晰的视觉反馈。

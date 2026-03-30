# SkeletonModal 通用骨架屏弹窗组件

## 📖 组件说明

`SkeletonModal` 是一个通用的带骨架屏加载状态的 Modal 组件，适用于所有需要异步加载数据的弹窗场景。

## ✨ 特性

- ✅ **开箱即用** - 内置骨架屏加载状态
- ✅ **简洁 API** - 只需控制 `loading` 属性
- ✅ **自动切换** - 加载时显示骨架屏，完成后显示内容
- ✅ **高度复用** - 适用于各种数据加载场景
- ✅ **优雅降级** - 无数据时自动隐藏

## 🚀 基础用法

### 基本示例

```vue
<script setup lang="ts">
import { ref } from 'vue'
import SkeletonModal from '@/components/SkeletonModal.vue'
import { fetchData } from '@/api'

const modalOpen = ref(false)
const loading = ref(false)
const data = ref(null)

async function openModal() {
  // 先打开弹窗
  modalOpen.value = true
  loading.value = true
  
  try {
    // 请求数据
    data.value = await fetchData()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-button @click="openModal">打开弹窗</a-button>
  
  <SkeletonModal
    v-model:open="modalOpen"
    title="数据详情"
    :loading="loading"
    width="800px"
  >
    <!-- 实际内容 -->
    <div v-if="data">
      <p>{{ data.content }}</p>
    </div>
  </SkeletonModal>
</template>
```

## 📋 Props 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `open` | `boolean` | `false` | 弹窗是否打开（支持 v-model） |
| `title` | `string` | `''` | 弹窗标题 |
| `width` | `string` | `'800px'` | 弹窗宽度 |
| `loading` | `boolean` | `false` | 是否显示骨架屏加载状态 |
| `footer` | `any` | `undefined` | 底部内容（设为 null 隐藏） |

## 🎯 Events 事件

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `update:open` | `(value: boolean)` | 弹窗开关状态变化时触发 |
| `cancel` | `-` | 点击取消按钮时触发 |

## 💡 使用场景

### 1️⃣ 发布历史记录（当前用例）

```vue
<SkeletonModal
  v-model:open="publishRecordModalOpen"
  title="发布历史记录"
  :loading="publishRecordLoading"
  :footer="null"
  width="1100px"
>
  <div v-if="publishRecordHistory.length">
    <!-- Tabs + 详情 + 分页器 -->
  </div>
</SkeletonModal>
```

### 2️⃣ 用户详情弹窗

```vue
<SkeletonModal
  v-model:open="userDetailOpen"
  title="用户详情"
  :loading="userLoading"
  width="600px"
>
  <a-descriptions v-if="userData" bordered :column="1">
    <a-descriptions-item label="姓名">{{ userData.name }}</a-descriptions-item>
    <a-descriptions-item label="邮箱">{{ userData.email }}</a-descriptions-item>
  </a-descriptions>
</SkeletonModal>
```

### 3️⃣ 数据统计弹窗

```vue
<SkeletonModal
  v-model:open="statsModalOpen"
  title="统计数据"
  :loading="statsLoading"
  width="900px"
>
  <div v-if="statsData" class="stats-content">
    <a-row :gutter="16">
      <a-col :span="8">
        <a-statistic title="总数" :value="statsData.total" />
      </a-col>
      <a-col :span="8">
        <a-statistic title="成功" :value="statsData.success" />
      </a-col>
      <a-col :span="8">
        <a-statistic title="失败" :value="statsData.failed" />
      </a-col>
    </a-row>
  </div>
</SkeletonModal>
```

### 4️⃣ 表单编辑弹窗

```vue
<SkeletonModal
  v-model:open="editModalOpen"
  title="编辑信息"
  :loading="formLoading"
  :footer="null"
  width="700px"
>
  <a-form v-if="!formLoading && formData" :model="formData" layout="vertical">
    <a-form-item label="名称">
      <a-input v-model:value="formData.name" />
    </a-form-item>
    
    <template #footer>
      <a-space>
        <a-button @click="editModalOpen = false">取消</a-button>
        <a-button type="primary" @click="handleSubmit">提交</a-button>
      </a-space>
    </template>
  </a-form>
</SkeletonModal>
```

## 🎨 自定义骨架屏

如果需要自定义骨架屏样式，可以使用插槽：

```vue
<SkeletonModal
  v-model:open="customOpen"
  title="自定义骨架屏"
  :loading="customLoading"
>
  <template #skeleton>
    <a-skeleton :paragraph="{ rows: 10 }" active />
  </template>
  
  <div class="custom-content">
    <!-- 实际内容 -->
  </div>
</SkeletonModal>
```

## ⚠️ 注意事项

### 1. Loading 状态管理

```typescript
// ✅ 推荐：先打开弹窗，再设置 loading
async function openModal() {
  modalOpen.value = true
  loading.value = true
  
  try {
    data.value = await fetchData()
  } finally {
    loading.value = false
  }
}

// ❌ 不推荐：等数据加载完再打开弹窗（会卡顿）
async function openModal() {
  data.value = await fetchData() // 用户感觉卡顿
  modalOpen.value = true
}
```

### 2. 空数据处理

```vue
<SkeletonModal v-model:open="modalOpen" :loading="loading">
  <a-empty v-if="!data || data.length === 0" description="暂无数据" />
  <div v-else>
    <!-- 数据内容 -->
  </div>
</SkeletonModal>
```

### 3. 错误处理

```typescript
async function openModal() {
  modalOpen.value = true
  loading.value = true
  
  try {
    data.value = await fetchData()
  } catch (error) {
    message.error('加载失败')
    modalOpen.value = false // 关闭弹窗
  } finally {
    loading.value = false
  }
}
```

## 🔧 源码位置

`frontend/src/components/SkeletonModal.vue`

## 📝 更新日志

- **v1.0.0** - 初始版本，支持基础骨架屏加载功能

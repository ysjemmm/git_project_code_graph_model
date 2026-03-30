<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  open?: boolean
  title?: string
  width?: string
  loading?: boolean
  footer?: any
  bodyStyle?: any
}

const props = withDefaults(defineProps<Props>(), {
  open: false,
  title: '',
  width: '800px',
  loading: false,
  footer: undefined,
  bodyStyle: undefined,
})

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'cancel'): void
}>()

// 计算属性：是否显示骨架屏
const showSkeleton = computed(() => props.loading)

// 关闭弹窗
const handleClose = () => {
  emit('update:open', false)
  emit('cancel')
}
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    :width="width"
    :footer="footer"
    @cancel="handleClose"
    :body-style="bodyStyle || { padding: '16px', maxHeight: '75vh', overflow: 'hidden' }"
  >
    <!-- 骨架屏加载状态 -->
    <div v-if="showSkeleton" class="skeleton-container">
      <a-skeleton :paragraph="{ rows: 6 }" active />
    </div>
    
    <!-- 实际内容 -->
    <div v-else class="skeleton-modal-content">
      <slot></slot>
    </div>
  </a-modal>
</template>

<style scoped>
.skeleton-container {
  padding: 24px;
  min-height: 400px; /* 增加最小高度，避免切换时跳动 */
}

/* 内容容器也设置相同的最小高度 */
:deep(.skeleton-modal-content) {
  min-height: 400px;
}
</style>

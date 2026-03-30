<script setup lang="ts">
import { ref, watch } from 'vue'
import GraphOverviewPane from '../components/GraphOverviewPane.vue'
import GraphQueryPane from '../components/GraphQueryPane.vue'
import GraphImportPane from '../components/GraphImportPane.vue'
import GraphDiagnosticsPane from '../components/GraphDiagnosticsPane.vue'

const active = ref<'overview' | 'query' | 'import' | 'diagnostics'>('overview')

// 监听 tab 切换实现按需加载
watch(
  () => active.value,
  (newActive) => {
    if (newActive === 'diagnostics') {
      console.log('切换到诊断 tab，将加载数据')
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="graph-page">
    <a-card title="代码图谱" class="panel">
      <a-tabs v-model:activeKey="active">
        <a-tab-pane key="overview" tab="概览">
          <GraphOverviewPane />
        </a-tab-pane>

        <a-tab-pane key="query" tab="查询">
          <GraphQueryPane />
        </a-tab-pane>

        <a-tab-pane key="import" tab="导入/重建">
          <GraphImportPane />
        </a-tab-pane>

        <a-tab-pane key="diagnostics" tab="诊断">
          <GraphDiagnosticsPane />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped>
.graph-page {
  height: 100%;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.graph-page > .panel > :deep(.ant-card-body) {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0px 24px 12px 24px;
}

.graph-page > .panel > :deep(.ant-card-body > .ant-tabs){
  height: 100%;
  display: flex;
  flex-direction: column;
}

.graph-page > .panel > :deep(.ant-card-body > .ant-tabs > .ant-tabs-content-holder > .ant-tabs-content) {
  min-height: calc(100vh - 56px - 46px - 16px);
  max-height: calc(100vh - 56px - 46px - 16px);
}

.graph-page > .panel > :deep(.ant-card-body > .ant-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) {
  min-height: 100%;
  max-height: 100%;
}

.graph-page > .panel {
  border-radius: 4px;
  flex: 1;
  min-height: 0;
}
</style>
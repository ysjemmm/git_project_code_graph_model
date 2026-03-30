<script setup lang="ts">
import { ref } from 'vue'
import { getGraphDiagnosticsSummary, clearGraphDatabase } from '../api'
import { message } from 'ant-design-vue'

interface Diagnostics {
  score: number
  alerts: {
    error: number
    warning: number
    ok: number
  }
  stats: {
    project_count: number
    application_project_count: number
    node_count: number
    relationship_count: number
  }
  schema: {
    index_total: number
    index_online: number
    index_failed: number
    indexes: any[]
    constraints: any[]
  }
  ops: {
    summary: any
    recent: any[]
  }
  generated_at: string
}

const diagnostics = ref<Diagnostics | null>(null)
const diagnosticsLoading = ref(false)
const diagnosticsError = ref<string | null>(null)
const includeCounts = ref(false)

async function fetchDiagnostics() {
  diagnosticsLoading.value = true
  diagnosticsError.value = null
  try {
    const data = await getGraphDiagnosticsSummary()
    diagnostics.value = data as Diagnostics
  } catch (e: any) {
    diagnosticsError.value = e?.message ?? String(e)
    diagnostics.value = null
  } finally {
    diagnosticsLoading.value = false
  }
}

async function clearGraphWithConfirm() {
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '确认清理整个图谱？',
    content: '该操作会删除 Neo4j 中所有节点和关系，且不可恢复。',
    okText: '确认清理',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const data = await clearGraphDatabase('CLEAR_ALL')
        if (!data?.ok) {
          message.error(data?.message || '图谱清理失败')
          return
        }
        message.success(`图谱已清理`)
        await fetchDiagnostics()
      } catch (e: any) {
        message.error(e?.message ?? String(e))
      }
    },
  })
}

// 初始化加载
fetchDiagnostics()

defineExpose({
  fetchDiagnostics,
  diagnostics,
  diagnosticsLoading,
})
</script>

<template>
  <div class="diagnostics-pane">
    <div class="diagnostics-top">
      <a-space>
        <a-button :loading="diagnosticsLoading" @click="fetchDiagnostics">刷新诊断</a-button>
        <a-button danger :loading="diagnosticsLoading" @click="clearGraphWithConfirm">清理图谱</a-button>
      </a-space>
      <span v-if="diagnostics?.generated_at" class="mono diagnostics-time">
        最近诊断：{{ String(diagnostics.generated_at).slice(0, 19).replace('T', ' ') }}
      </span>
    </div>

    <a-alert
      v-if="diagnosticsError"
      type="error"
      show-icon
      :message="diagnosticsError"
      style="margin-bottom: 12px;"
    />

    <template v-if="diagnostics">
      <a-row :gutter="12" style="margin-bottom: 12px;">
        <a-col :span="6"><a-statistic title="健康分" :value="diagnostics.score" suffix="/ 100" /></a-col>
        <a-col :span="6"><a-statistic title="失败项" :value="diagnostics.alerts.error" /></a-col>
        <a-col :span="6"><a-statistic title="警告项" :value="diagnostics.alerts.warning" /></a-col>
        <a-col :span="6"><a-statistic title="通过项" :value="diagnostics.alerts.ok" /></a-col>
      </a-row>

      <a-row :gutter="12" style="margin-bottom: 12px;">
        <a-col :span="6"><a-statistic title="项目节点" :value="diagnostics.stats.project_count" /></a-col>
        <a-col :span="6"><a-statistic title="Application 项目" :value="diagnostics.stats.application_project_count" /></a-col>
        <a-col :span="6"><a-statistic title="总节点数" :value="diagnostics.stats.node_count" /></a-col>
        <a-col :span="6"><a-statistic title="总关系数" :value="diagnostics.stats.relationship_count" /></a-col>
      </a-row>

      <a-card size="small" title="索引与约束" style="margin-bottom: 12px;">
        <a-table
          :data-source="diagnostics.schema.indexes as any"
          :pagination="{ pageSize: 8 }"
          size="small"
          bordered
          row-key="name"
          :scroll="{ x: 980 }"
          style="margin-bottom: 12px;"
        >
          <a-table-column title="索引名" data-index="name" key="name" :width="220" ellipsis />
          <a-table-column title="状态" key="state" :width="110">
            <template #default="{ record }">
              <a-tag :color="String(record.state || '').toUpperCase() === 'ONLINE' ? 'green' : 'orange'">
                {{ record.state || '-' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="类型" data-index="type" key="type" :width="140" ellipsis />
          <a-table-column title="标签/关系" key="labels_or_types" :width="180">
            <template #default="{ record }">
              <span class="mono">{{ Array.isArray(record.labels_or_types) ? record.labels_or_types.join(', ') : '-' }}</span>
            </template>
          </a-table-column>
        </a-table>

        <a-table
          :data-source="diagnostics.schema.constraints as any"
          :pagination="{ pageSize: 6 }"
          size="small"
          bordered
          row-key="name"
          :scroll="{ x: 880 }"
        >
          <a-table-column title="约束名" data-index="name" key="name" :width="220" ellipsis />
          <a-table-column title="类型" data-index="type" key="type" :width="160" ellipsis />
          <a-table-column title="标签/关系" key="labels_or_types">
            <template #default="{ record }">
              <span class="mono">{{ Array.isArray(record.labels_or_types) ? record.labels_or_types.join(', ') : '-' }}</span>
            </template>
          </a-table-column>
        </a-table>
      </a-card>
    </template>

    <a-empty v-else-if="!diagnosticsLoading" description="暂无诊断数据" />
  </div>
</template>

<style scoped>
.diagnostics-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.diagnostics-top {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.diagnostics-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>

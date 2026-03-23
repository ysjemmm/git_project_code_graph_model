<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import { LinkOutlined } from '@ant-design/icons-vue'
import { useRepo } from '../composables/useRepo'
import { message } from 'ant-design-vue'
import {
  listImportTasks,
  cancelImportTask as apiCancelImportTask,
  createImportTask,
  getImportSettings,
  getImportTaskLog,
  listGraphProjects,
  runGraphQuery,
  getGraphDiagnosticsSummary,
} from '../api'

const active = ref<'overview' | 'query' | 'import' | 'diagnostics'>('overview')

type QueryTemplate = {
  key: string
  name: string
  cypher: string
  desc: string
}

const queryTemplates: QueryTemplate[] = [
  {
    key: 'project-overview',
    name: '项目节点概览',
    desc: '查看每个项目节点数量（Top 30）',
    cypher: `MATCH (n)
WHERE coalesce(n.belong_project, '') <> ''
RETURN n.belong_project AS project, count(*) AS node_count
ORDER BY node_count DESC
LIMIT coalesce($limit, 30)`,
  },
  {
    key: 'depends-on-top',
    name: '依赖关联 Top',
    desc: '查看 DEPENDS_ON 关联最密集的项目',
    cypher: `MATCH (a:Project)-[r:DEPENDS_ON]->(b:Project)
RETURN a.name AS source_project, b.name AS target_project, count(r) AS rel_count
ORDER BY rel_count DESC
LIMIT coalesce($limit, 50)`,
  },
  {
    key: 'javaobject-type',
    name: '对象类型分布',
    desc: '统计 JavaObject 的 from_type 分布',
    cypher: `MATCH (n:JavaObject)
RETURN coalesce(n.from_type, 'UNKNOWN') AS from_type, count(*) AS cnt
ORDER BY cnt DESC
LIMIT coalesce($limit, 30)`,
  },
]

const selectedTemplateKey = ref(queryTemplates[0]?.key || '')
const queryText = ref(queryTemplates[0]?.cypher || '')
const queryLimit = ref(200)
const queryLoading = ref(false)
const queryError = ref<string | null>(null)
const queryResult = ref<{ columns: string[]; rows: Array<Record<string, any>> }>({ columns: [], rows: [] })

function applyQueryTemplate(key: string) {
  const t = queryTemplates.find((x) => x.key === key)
  if (!t) return
  selectedTemplateKey.value = key
  queryText.value = t.cypher
}

function formatCell(v: any): string {
  if (v == null) return '-'
  if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') return String(v)
  try {
    return JSON.stringify(v)
  } catch {
    return String(v)
  }
}

const queryTableColumns = computed(() =>
  queryResult.value.columns.map((c) => ({ title: c, dataIndex: c, key: c, ellipsis: true })),
)

const queryTableData = computed(() =>
  queryResult.value.rows.map((r, i) => ({ __rowKey: `row-${i}`, ...r })),
)

const rowCount = computed(() => queryResult.value.rows.length)

const visualMetric = computed(() => {
  const rows = queryResult.value.rows
  const columns = queryResult.value.columns
  if (!rows.length || !columns.length) return null
  const numberCol = columns.find((col) => rows.every((x) => x[col] == null || typeof x[col] === 'number'))
  if (!numberCol) return null
  const dimCol = columns.find((col) => col !== numberCol && rows.some((x) => typeof x[col] === 'string'))
  if (!dimCol) return null
  const items = rows
    .slice(0, 12)
    .map((x) => ({ name: String(x[dimCol] ?? '-'), value: Number(x[numberCol] ?? 0) }))
  const max = Math.max(1, ...items.map((x) => x.value))
  return { dimCol, numberCol, items, max }
})

async function runQuery() {
  const cypher = String(queryText.value || '').trim()
  if (!cypher) {
    queryError.value = '请先输入 Cypher 查询语句'
    return
  }
  queryLoading.value = true
  queryError.value = null
  try {
    const data = await runGraphQuery({
      cypher,
      limit: Number(queryLimit.value || 200),
      params: { limit: Number(queryLimit.value || 200) },
    })
    if (!data?.ok) {
      queryError.value = data?.message || '查询失败'
      queryResult.value = { columns: [], rows: [] }
      return
    }
    queryResult.value = {
      columns: Array.isArray(data.columns) ? data.columns : [],
      rows: Array.isArray(data.rows) ? data.rows : [],
    }
  } catch (e: any) {
    queryError.value = e?.message ?? String(e)
    queryResult.value = { columns: [], rows: [] }
  } finally {
    queryLoading.value = false
  }
}

function statusTag(status: any) {
  const s = String(status || '').toLowerCase()
  if (s === 'success' || s === 'succeeded' || s === 'done') return { color: 'green', text: 'success' }
  if (s === 'running') return { color: 'processing', text: 'running' }
  if (s === 'pending' || s === 'queued') return { color: 'default', text: 'pending' }
  if (s === 'failed' || s === 'error') return { color: 'red', text: 'failed' }
  if (s === 'cancelled' || s === 'canceled') return { color: 'orange', text: 'cancelled' }
  return { color: 'default', text: s || '-' }
}

function openAura() {
  window.open(NEO4J_AURA_QUERY_URL, '_blank', 'noopener,noreferrer')
}

type GraphProjectItem = {
  project_name: string
  project_key?: string | null
  project_type?: string | null
  branch?: string | null
  commit_hash?: string | null
  repo_url?: string | null
  last_update_time?: string | null
  node_count?: number | null
  relationship_count?: number | null
}

const loading = ref(false)
const error = ref<string | null>(null)
const items = ref<GraphProjectItem[]>([])
const includeCounts = ref(true)

// ---- Diagnostics ----
type DiagnosticsSummary = {
  generated_at: string
  score: number
  stats: {
    project_count: number
    application_project_count: number
    node_count: number
    relationship_count: number
    unknown_project_node_count: number
  }
  schema: {
    indexes: Array<{
      name: string
      state: string
      type: string
      entity_type: string
      labels_or_types: string[]
      properties: string[]
      population_percent: number
    }>
    constraints: Array<{
      name: string
      type: string
      entity_type: string
      labels_or_types: string[]
      properties: string[]
    }>
    index_total: number
    index_online: number
    index_failed: number
  }
  ops: {
    summary: {
      window_minutes: number
      total: number
      error_count: number
      slow_count: number
    }
    recent: Array<{
      ts: string
      op_type: string
      ok: boolean
      is_slow: boolean
      elapsed_ms: number
      slow_threshold_ms: number
      row_count: number
      query: string
      query_hash: string
      params: Record<string, any>
      error: string
    }>
  }
  checks: Array<{ id: string; title: string; status: 'ok' | 'warning' | 'error'; message: string; suggestion?: string }>
  alerts: { error: number; warning: number; ok: number }
}

const diagnosticsLoading = ref(false)
const diagnosticsError = ref<string | null>(null)
const diagnostics = ref<DiagnosticsSummary | null>(null)

function diagnosticsTag(status: 'ok' | 'warning' | 'error') {
  if (status === 'ok') return { color: 'green', text: '通过' }
  if (status === 'warning') return { color: 'orange', text: '警告' }
  return { color: 'red', text: '失败' }
}

async function fetchDiagnostics() {
  diagnosticsLoading.value = true
  diagnosticsError.value = null
  try {
    const data = await getGraphDiagnosticsSummary()
    diagnostics.value = data as DiagnosticsSummary
  } catch (e: any) {
    diagnosticsError.value = e?.message ?? String(e)
    diagnostics.value = null
  } finally {
    diagnosticsLoading.value = false
  }
}

// ---- Graph project selection (used only for overview highlight) ----
const selectedGraphProject = ref<GraphProjectItem | null>(null)

function getProjectRowKey(x: GraphProjectItem) {
  return `${x.project_name}::${x.branch ?? ''}::${x.commit_hash ?? ''}`
}

// ---- Import tasks ----
type ImportTask = any
const importForm = ref({
  maven_scan_enabled: true,
  force_maven: false,
  clear_database: false,
  auto_link_external: true,
})
const importSubmitting = ref(false)
const importTasks = ref<ImportTask[]>([])
const importStats = ref<any>(null)
const selectedTaskId = ref<string | null>(null)

const logModalOpen = ref(false)
const logModalTaskId = ref<string | null>(null)
const logOffset = ref(0)
const logLines = ref<string[]>([])
const logLoading = ref(false)
const logHasMore = ref(true)
const logStickToBottom = ref(true)
const logViewEl = ref<HTMLElement | null>(null)
let importPollTimer: number | undefined

let lastImportStatsText = ''

watch(importStats, (v) => {
  if (!v) {
    lastImportStatsText = ''
    return
  }
  const running = Number(v.running ?? 0)
  const pending = Number(v.pending ?? 0)
  // 噪音控制：并发监控只在有“运行中/排队”时提示
  if (running <= 0 && pending <= 0) return

  const text = `并发=${v.max_workers}，运行中=${v.running}，排队=${v.pending}`
  if (text === lastImportStatsText) return
  lastImportStatsText = text
  message.info({ content: text, duration: 3 })
})

// ---- Import: 只需要选择 Application（repo_url/branch/commit_id 自动从缓存落盘信息读取）----
const repoState = useRepo({
  lazyRefFetch: true,
  projectSource: 'cacheApplicationProjects',
  autoFillRefFromMeta: false,
})
const {
  repoOptions,
  selectedProjectName,
  readonlyUrl,
  branch,
  commitId,
  canRun,
  canSelectRef,
  loadingRepos,
  loadingBranches,
  loadingCommits,
  branchOptions,
  commitOptions,
  onBranchSearch,
  onCommitSearch,
  onBranchDropdown,
  onCommitDropdown,
  fetchRepos,
} = repoState

const selectedMeta = repoState.selectedMeta

const refType = ref<'branch' | 'commit'>('branch')

watch(
  () => selectedProjectName.value,
  () => {
    // 导入/重建默认走 Branch，不根据历史缓存自动切到 Commit
    refType.value = 'branch'
  },
)

const importSubmitEnabled = computed(() => {
  if (!canRun.value) return false
  if (refType.value === 'commit') return Boolean(String(commitId.value || '').trim())
  return Boolean(String(branch.value || '').trim())
})

const selectedProjectRowKey = computed(() =>
  selectedGraphProject.value ? getProjectRowKey(selectedGraphProject.value) : '',
)

async function selectGraphProject(project: GraphProjectItem) {
  selectedGraphProject.value = project
}

async function fetchImportTasks() {
  const data = await listImportTasks()
  if (data?.ok) {
    importTasks.value = Array.isArray(data.items) ? data.items : []
    importStats.value = (data as any).stats
    if (!selectedTaskId.value && importTasks.value.length) {
      selectedTaskId.value = importTasks.value[0].task_id
    }
    syncImportFormFromSelectedApp()
  }
}

async function syncImportFormFromSelectedApp() {
  const appId = selectedMeta.value?.id
  if (!appId) return
  try {
    const data = await getImportSettings(appId)
    if (data?.ok && data.settings) {
      importForm.value.maven_scan_enabled = Boolean(data.settings.maven_scan_enabled ?? true)
      importForm.value.force_maven = Boolean(data.settings.force_maven ?? false)
      importForm.value.clear_database = Boolean(data.settings.clear_database ?? false)
      importForm.value.auto_link_external = Boolean(data.settings.auto_link_external ?? true)
    }
  } catch {
    // 读取失败保持默认值
  }
}

function isCancellableTaskStatus(status: any) {
  const s = String(status || '').toLowerCase()
  return s === 'pending' || s === 'running'
}

async function cancelImportTask(taskId: string) {
  if (!taskId) return
  const t = (importTasks.value as any[]).find((x) => x?.task_id === taskId)
  if (t && !isCancellableTaskStatus(t.status)) return
  if (!window.confirm('确认终止该导入任务？正在执行中的任务会在可中断点尽快停止。')) return
  try {
    await apiCancelImportTask(taskId)
    await fetchImportTasks()
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  }
}

async function submitImport() {
  if (!canRun.value) {
    message.error({ content: '请先选择应用', duration: 4 })
    return
  }

  const repo_url = String(readonlyUrl.value || '').trim()
  if (!repo_url) {
    message.error({ content: '应用的 Git 地址为空', duration: 4 })
    return
  }

  if (refType.value === 'commit') {
    if (!commitId.value) {
      message.error({ content: '请选择或输入 CommitId', duration: 4 })
      return
    }
  } else {
    if (!branch.value) {
      message.error({ content: '请选择或输入 Branch', duration: 4 })
      return
    }
  }

  importSubmitting.value = true
  try {
    const payload: any = {
      repo_url,
      maven_scan_enabled: Boolean(importForm.value.maven_scan_enabled),
      force_maven: Boolean(importForm.value.force_maven),
      clear_database: Boolean(importForm.value.clear_database),
      auto_link_external: Boolean(importForm.value.auto_link_external),
    }

    if (refType.value === 'commit') payload.commit_id = String(commitId.value || '').trim()
    else payload.branch = String(branch.value || '').trim()

    const data = await createImportTask(payload)
    if (!data?.ok) throw new Error('提交失败')
    await fetchImportTasks()
    selectedTaskId.value = (data as any).task_id
    logOffset.value = 0
    logLines.value = []
    await fetchLogs()
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  } finally {
    importSubmitting.value = false
  }
}

async function fetchLogs() {
  const tid = logModalTaskId.value || selectedTaskId.value
  if (!tid) return
  if (!logHasMore.value) return
  if (logLoading.value) return
  logLoading.value = true
  try {
    const data = await getImportTaskLog(tid, logOffset.value, 2000)
    if (data?.ok) {
      const lines = Array.isArray(data.lines) ? data.lines : []
      if (lines.length) {
        logLines.value.push(...lines)
      } else if (logModalTaskDone.value) {
        logHasMore.value = false
      }
      logOffset.value = Number(data.next_offset ?? logOffset.value)
    }
  } finally {
    logLoading.value = false
    if (logStickToBottom.value) {
      await nextTick()
      const el = logViewEl.value
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}

function selectTask(id: string) {
  selectedTaskId.value = id
}

async function openLogModal(taskId: string) {
  logModalTaskId.value = taskId
  logModalOpen.value = true  // 先开弹窗，让动画先跑
  logOffset.value = 0
  logLines.value = []
  logHasMore.value = true
  logStickToBottom.value = true
  // 等弹窗动画完成后再请求（ant-design modal 动画约 300ms）
  setTimeout(() => {
    fetchLogs().then(() => drainLogsIfDone())
  }, 320)
}

function closeLogModal() {
  logModalOpen.value = false
}

async function reloadLogs() {
  logOffset.value = 0
  logLines.value = []
  logHasMore.value = true
  logStickToBottom.value = true
  await fetchLogs()
  await drainLogsIfDone()
}

const logModalTask = computed(() => {
  const tid = logModalTaskId.value
  if (!tid) return null
  return (importTasks.value || []).find((t: any) => t?.task_id === tid) || null
})

const logModalTaskDone = computed(() => {
  const s = String(logModalTask.value?.status || '').toLowerCase()
  return s === 'success' || s === 'failed' || s === 'cancelled' || s === 'canceled'
})

async function drainLogsIfDone() {
  if (!logModalTaskDone.value) return
  for (let i = 0; i < 30; i++) {
    if (!logHasMore.value) break
    const before = logOffset.value
    await fetchLogs()
    if (logOffset.value === before) break
  }
}

function onLogScroll() {
  const el = logViewEl.value
  if (!el) return
  const threshold = 80
  const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - threshold
  logStickToBottom.value = atBottom
  if (atBottom && !logLoading.value) void fetchLogs()
}

async function fetchProjects() {
  loading.value = true
  error.value = null
  try {
    const data = await listGraphProjects(includeCounts.value)
    items.value = Array.isArray(data?.items) ? data.items : []
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchProjects()
  void fetchDiagnostics()
  void fetchImportTasks()
  void fetchRepos()
  importPollTimer = window.setInterval(async () => {
    await fetchImportTasks()
    // 任务列表刷新后，尝试把导入参数同步成“应用最近一次配置”
    syncImportFormFromSelectedApp()
    // 自动拉最新日志（只在弹窗打开时）
    if (logModalOpen.value) {
      if (!logModalTaskDone.value) {
        logHasMore.value = true  // 任务未完成时强制允许继续拉取
        await fetchLogs()
      } else {
        // 任务已完成，尝试排干剩余日志
        await drainLogsIfDone()
      }
    }
  }, 5000)
})

// 选中应用变化后，立即同步导入参数为“应用最近一次导入任务配置”
watch(
  readonlyUrl,
  () => {
    syncImportFormFromSelectedApp()
  },
  { immediate: true },
)

// 组件卸载时清理 timer
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (importPollTimer) window.clearInterval(importPollTimer)
  importPollTimer = undefined
})

watch(includeCounts, () => {
  void fetchProjects()
})

const tableData = computed(() =>
  items.value.map((x) => ({
    key: getProjectRowKey(x),
    ...x,
  })),
)

const columns = [
  { title: '项目', dataIndex: 'project_name', key: 'project_name', width: 220, ellipsis: true },
  { title: '类型', dataIndex: 'project_type', key: 'project_type', width: 110 },
  { title: '节点数', dataIndex: 'node_count', key: 'node_count', width: 110 },
  { title: '关系数', dataIndex: 'relationship_count', key: 'relationship_count', width: 110 },
  { title: '更新时间', dataIndex: 'last_update_time', key: 'last_update_time', width: 210, ellipsis: true },
] as const
</script>

<template>
  <div class="graph-page">
    <a-card title="管理代码图谱" class="panel">
      <a-tabs v-model:activeKey="active">
        <a-tab-pane key="overview" tab="概览">
          <div class="overview-pane">
            <div class="overview-top">
              <a-space>
                <a-button @click="openAura">
                  <template #icon><LinkOutlined /></template>
                  打开 Neo4j Aura Query
                </a-button>
                <a-switch v-model:checked="includeCounts" checked-children="含统计" un-checked-children="不含统计" />
                <a-button :loading="loading" @click="fetchProjects">刷新项目列表</a-button>
              </a-space>
              <!-- 错误由 watch(error) 使用 message 提示 -->
            </div>

            <div class="overview-table">
              <a-table
                class="project-table"
                :loading="loading"
                :columns="columns as any"
                :data-source="tableData as any"
                :pagination="{ pageSize: 15, showSizeChanger: true }"
                size="small"
                bordered
                row-key="key"
                :row-class-name="(r: any) => (String(r?.key) === String(selectedProjectRowKey) ? 'row-selected' : '')"
                :custom-row="(record: any) => ({ onClick: () => selectGraphProject(record) })"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'project_type'">
                    <a-tag :color="String(record.project_type || '').toLowerCase() === 'application' ? 'blue' : 'gold'">
                      {{ record.project_type || '-' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'node_count' || column.key === 'relationship_count'">
                    <span class="mono">{{ record[column.dataIndex] ?? '-' }}</span>
                  </template>
                </template>
              </a-table>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="query" tab="查询">
          <div class="query-pane">
            <div class="query-left">
              <a-card size="small" title="查询模板与编辑器">
                <a-alert
                  type="warning"
                  show-icon
                  style="margin-bottom: 10px;"
                  message="仅支持 Cypher（只读）。禁止 CREATE/MERGE/DELETE/SET 等写操作。"
                />
                <a-form layout="vertical" size="small">
                  <a-form-item label="模板">
                    <a-select
                      v-model:value="selectedTemplateKey"
                      :options="queryTemplates.map(t => ({ value: t.key, label: t.name }))"
                      @change="(v: string) => applyQueryTemplate(v)"
                    />
                    <div class="query-template-desc">
                      {{ queryTemplates.find(t => t.key === selectedTemplateKey)?.desc || '-' }}
                    </div>
                  </a-form-item>
                  <a-form-item label="最大返回行数">
                    <a-input-number v-model:value="queryLimit" :min="1" :max="2000" style="width: 180px;" />
                  </a-form-item>
                  <a-form-item label="Cypher">
                    <a-textarea v-model:value="queryText" :rows="16" class="mono query-editor" />
                  </a-form-item>
                  <a-space>
                    <a-button type="primary" :loading="queryLoading" @click="runQuery">执行查询</a-button>
                    <a-button @click="openAura">
                      <template #icon><LinkOutlined /></template>
                      在 Aura 中打开
                    </a-button>
                  </a-space>
                </a-form>
              </a-card>
            </div>
            <div class="query-right">
              <a-card size="small" title="查询结果与可视化">
                <a-alert
                  v-if="queryError"
                  type="error"
                  show-icon
                  :message="queryError"
                  style="margin-bottom: 10px;"
                />
                <div class="query-result-summary">
                  <a-statistic title="返回行数" :value="rowCount" />
                </div>
                <div v-if="visualMetric" class="query-chart">
                  <div class="query-chart-title">
                    可视化（{{ visualMetric.numberCol }} by {{ visualMetric.dimCol }}，Top {{ visualMetric.items.length }}）
                  </div>
                  <div v-for="it in visualMetric.items" :key="it.name" class="query-chart-row">
                    <div class="query-chart-name mono">{{ it.name }}</div>
                    <div class="query-chart-bar-wrap">
                      <div class="query-chart-bar" :style="{ width: `${Math.max(2, Math.round((it.value / visualMetric.max) * 100))}%` }" />
                    </div>
                    <div class="query-chart-value mono">{{ it.value }}</div>
                  </div>
                </div>
                <a-table
                  :loading="queryLoading"
                  :columns="queryTableColumns as any"
                  :data-source="queryTableData as any"
                  :pagination="{ pageSize: 10, showSizeChanger: true }"
                  size="small"
                  bordered
                  row-key="__rowKey"
                  :scroll="{ x: true }"
                >
                  <template #bodyCell="{ column, record }">
                    <a-tooltip :title="formatCell(record[column.key])" placement="topLeft">
                      <span class="mono cell-ellipsis">{{ formatCell(record[column.key]) }}</span>
                    </a-tooltip>
                  </template>
                </a-table>
              </a-card>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="import" tab="导入/重建">
          <a-row :gutter="16" style="height:100%">
            <a-col :xs="24" :lg="10" style="height:100%; display:flex; flex-direction:column;">
              <div class="import-card">
                <div class="import-form-scroll">
                  <a-form layout="vertical">
                  <a-form-item label="选择应用" required>
                    <a-select
                      v-model:value="selectedProjectName"
                      placeholder="请选择应用"
                      :options="repoOptions"
                      :loading="loadingRepos"
                      show-search
                      :filter-option="false"
                      allow-clear
                    />
                  </a-form-item>

                  <div
                    class="import-meta"
                    style="margin-bottom: 12px; padding: 10px 12px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: rgba(0,0,0,.02);"
                  >
                    <div class="import-meta-row">
                      <span class="import-meta-label" style="display:inline-block; min-width:64px; color: rgba(0,0,0,.65); font-weight:600; margin-right:6px;">Git：</span>
                      <span class="mono">{{ readonlyUrl || '-' }}</span>
                    </div>
                    <div class="import-meta-row">
                      <span class="import-meta-label" style="display:inline-block; min-width:64px; color: rgba(0,0,0,.65); font-weight:600; margin-right:6px;">Ref：</span>
                      <span class="mono">{{ refType === 'commit' ? String(commitId || '-') : String(branch || '-') }}</span>
                    </div>
                  </div>

                  <a-form-item label="Ref（Branch 或 CommitId）" required>
                    <a-radio-group v-model:value="refType" button-style="solid">
                      <a-radio-button value="branch">Branch</a-radio-button>
                      <a-radio-button value="commit">CommitId</a-radio-button>
                    </a-radio-group>
                  </a-form-item>

                  <a-form-item v-if="refType === 'branch'" label="Branch（下拉选择）" required>
                    <a-select
                      v-model:value="branch"
                      placeholder="请选择/搜索 Branch"
                      :options="branchOptions"
                      :disabled="!canSelectRef"
                      :loading="loadingBranches"
                      show-search
                      :filter-option="false"
                      @search="onBranchSearch"
                      @dropdownVisibleChange="onBranchDropdown"
                      allow-clear
                    >
                      <template #notFoundContent>
                        <a-space v-if="loadingBranches" size="small"><a-spin size="small" /><span>加载中...</span></a-space>
                        <span v-else>暂无数据</span>
                      </template>
                    </a-select>
                  </a-form-item>

                  <a-form-item v-if="refType === 'commit'" label="CommitId（下拉选择）" required>
                    <a-select
                      v-model:value="commitId"
                      placeholder="请选择/搜索 CommitId"
                      :options="commitOptions"
                      :disabled="!canSelectRef"
                      :loading="loadingCommits"
                      show-search
                      :filter-option="false"
                      @search="onCommitSearch"
                      @dropdownVisibleChange="onCommitDropdown"
                      allow-clear
                    >
                      <template #notFoundContent>
                        <a-space v-if="loadingCommits" size="small"><a-spin size="small" /><span>加载中...</span></a-space>
                        <span v-else>暂无数据</span>
                      </template>
                    </a-select>
                  </a-form-item>

                  <div class="import-desc-line">
                    <span class="import-desc-label">解析 pom 外部依赖（Maven 扫描）</span>
                    <span class="mono">{{ importForm.maven_scan_enabled ? '启用' : '关闭' }}</span>
                  </div>
                  <div class="import-desc-line">
                    <span class="import-desc-label">强制重新解析 Maven（可能较慢）</span>
                    <span class="mono">{{ importForm.force_maven ? '强制' : '关闭' }}</span>
                  </div>
                  <div class="import-desc-line">
                    <span class="import-desc-label">导入前清理旧图谱（clear_database，重建）</span>
                    <span class="mono">{{ importForm.clear_database ? '清理' : '不清理' }}</span>
                  </div>
                  <div class="import-desc-line">
                    <span class="import-desc-label">自动关联外部类（ExternalClassLinker）</span>
                    <span class="mono">{{ importForm.auto_link_external ? '启用' : '关闭' }}</span>
                  </div>
                  </a-form>
                </div>

                <div class="import-form-footer">
                  <a-button type="primary" :loading="importSubmitting" :disabled="!importSubmitEnabled" @click="submitImport">
                    提交导入（并发=1，自动排队）
                  </a-button>
                </div>
              </div>
            </a-col>

            <a-col :xs="24" :lg="14">
              <a-card size="small" title="任务队列与日志">
                <!-- 并发信息由 watch(importStats) 使用 message 提示 -->

                <div class="import-tasks-table-wrap">
                <a-table
                  :data-source="importTasks as any"
                  :pagination="{ pageSize: 6 }"
                  size="small"
                  bordered
                  :scroll="{ x: 800 }"
                  :row-class-name="(r: any) => (r.task_id === selectedTaskId ? 'row-selected' : '')"
                  :custom-row="(record: any) => ({ onClick: () => selectTask(record.task_id) })"
                >
                  <a-table-column title="task_id" data-index="task_id" key="task_id" :width="100" ellipsis />
                  <a-table-column title="状态" data-index="status" key="status" :width="90">
                    <template #default="{ record }">
                      <a-tag :color="statusTag(record.status).color">
                        {{ statusTag(record.status).text }}
                      </a-tag>
                    </template>
                  </a-table-column>
                  <a-table-column title="repo" data-index="repo_url" key="repo_url" :width="180" ellipsis />
                  <a-table-column title="ref" key="ref" :width="280">
                    <template #default="{ record }">
                      <span class="mono">{{ record.commit_id ? (record.commit_id || '') : record.branch }}</span>
                    </template>
                  </a-table-column>
                  <a-table-column title="创建时间" key="created_at" :width="160">
                    <template #default="{ record }">
                      <span class="mono" style="font-size:12px">{{ (record.created_at || '').slice(0, 19).replace('T', ' ') }}</span>
                    </template>
                  </a-table-column>
                  <a-table-column title="操作" key="ops" :width="80" fixed="right" class="ops-col">
                    <template #default="{ record }">
                      <a-space size="small">
                        <a-button type="link" size="small" style="padding: 0; min-width: 0;" @click.stop="openLogModal(record.task_id)">日志</a-button>
                        <a-button
                          v-if="isCancellableTaskStatus(record.status)"
                          danger
                          type="link"
                          size="small"
                          style="padding: 0; min-width: 0;"
                          @click.stop="cancelImportTask(record.task_id)"
                        >
                          终止
                        </a-button>
                      </a-space>
                    </template>
                  </a-table-column>
                </a-table>
                </div>

                <div style="height: 10px" />
                <a-modal
                  v-model:open="logModalOpen"
                  :title="`任务日志：${logModalTaskId || '-'}`"
                  width="1180px"
                  :footer="null"
                  :body-style="{ padding: '12px', height: 'calc(60vh + 60px)', overflow: 'hidden' }"
                  @cancel="closeLogModal"
                >
                  <div class="logbox">
                    <div class="logbox-hd">
                      <div class="mono">日志</div>
                      <a-space>
                        <a-button size="small" :loading="logLoading" @click="fetchLogs">拉取更多</a-button>
                        <a-button size="small" @click="reloadLogs">重载</a-button>
                      </a-space>
                    </div>
                    <div ref="logViewEl" class="log" @scroll="onLogScroll">
                      <div v-if="logLoading && logLines.length === 0" class="log-loading">
                        <a-spin size="large" />
                        <div style="margin-top: 12px; color: rgba(0,0,0,.45); font-size: 13px;">正在加载日志…</div>
                      </div>
                      <pre v-else class="log-pre">{{ logLines.join('\n') }}</pre>
                    </div>
                  </div>
                </a-modal>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="diagnostics" tab="诊断">
          <div class="diagnostics-pane">
            <div class="diagnostics-top">
              <a-space>
                <a-button :loading="diagnosticsLoading" @click="fetchDiagnostics">刷新诊断</a-button>
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

              <a-row :gutter="12" style="margin-bottom: 12px;">
                <a-col :span="8"><a-statistic title="索引总数" :value="diagnostics.schema.index_total" /></a-col>
                <a-col :span="8"><a-statistic title="ONLINE 索引" :value="diagnostics.schema.index_online" /></a-col>
                <a-col :span="8"><a-statistic title="异常/构建中索引" :value="diagnostics.schema.index_failed" /></a-col>
              </a-row>

              <a-card size="small" title="索引与约束" style="margin-bottom: 12px;">
                <div class="diagnostics-schema-table">
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
                    <a-table-column title="实体类型" data-index="entity_type" key="entity_type" :width="120" ellipsis />
                    <a-table-column title="标签/关系" key="labels_or_types" :width="180">
                      <template #default="{ record }">
                        <span class="mono">{{ Array.isArray(record.labels_or_types) ? record.labels_or_types.join(', ') : '-' }}</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="属性" key="properties" :width="180">
                      <template #default="{ record }">
                        <span class="mono">{{ Array.isArray(record.properties) ? record.properties.join(', ') : '-' }}</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="构建进度%" key="population_percent" :width="110">
                      <template #default="{ record }">
                        <span class="mono">{{ Number(record.population_percent || 0).toFixed(1) }}</span>
                      </template>
                    </a-table-column>
                  </a-table>
                </div>

                <div class="diagnostics-schema-table">
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
                    <a-table-column title="实体类型" data-index="entity_type" key="entity_type" :width="120" ellipsis />
                    <a-table-column title="标签/关系" key="labels_or_types">
                      <template #default="{ record }">
                        <span class="mono">{{ Array.isArray(record.labels_or_types) ? record.labels_or_types.join(', ') : '-' }}</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="属性" key="properties">
                      <template #default="{ record }">
                        <span class="mono">{{ Array.isArray(record.properties) ? record.properties.join(', ') : '-' }}</span>
                      </template>
                    </a-table-column>
                  </a-table>
                </div>
              </a-card>

              <a-card size="small" title="图数据库操作日志（近 60 分钟）" style="margin-bottom: 12px;">
                <a-row :gutter="12" style="margin-bottom: 10px;">
                  <a-col :span="8"><a-statistic title="记录条数" :value="diagnostics.ops.summary.total" /></a-col>
                  <a-col :span="8"><a-statistic title="错误数" :value="diagnostics.ops.summary.error_count" /></a-col>
                  <a-col :span="8"><a-statistic title="慢查询/慢写入" :value="diagnostics.ops.summary.slow_count" /></a-col>
                </a-row>
                <div class="diagnostics-schema-table">
                  <a-table
                    :data-source="diagnostics.ops.recent as any"
                    :pagination="{ pageSize: 8 }"
                    size="small"
                    bordered
                    :row-key="(r: any) => `${r.ts || ''}-${r.query_hash || ''}-${r.op_type || ''}`"
                    :scroll="{ x: 1180 }"
                  >
                    <a-table-column title="时间" key="ts" :width="170">
                      <template #default="{ record }">
                        <span class="mono">{{ String(record.ts || '').slice(0, 19).replace('T', ' ') }}</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="类型" data-index="op_type" key="op_type" :width="120" ellipsis />
                    <a-table-column title="状态" key="ok" :width="90">
                      <template #default="{ record }">
                        <a-tag :color="record.ok ? (record.is_slow ? 'orange' : 'green') : 'red'">
                          {{ record.ok ? (record.is_slow ? '慢' : '正常') : '错误' }}
                        </a-tag>
                      </template>
                    </a-table-column>
                    <a-table-column title="耗时(ms)" key="elapsed_ms" :width="110">
                      <template #default="{ record }">
                        <span class="mono">{{ Number(record.elapsed_ms || 0).toFixed(2) }}</span>
                      </template>
                    </a-table-column>
                    <a-table-column title="返回行数" data-index="row_count" key="row_count" :width="90" />
                    <a-table-column title="Cypher" data-index="query" key="query" ellipsis />
                    <a-table-column title="错误信息" data-index="error" key="error" :width="220" ellipsis />
                  </a-table>
                </div>
              </a-card>

              <a-card size="small" title="检查项">
                <a-table
                  :data-source="diagnostics.checks as any"
                  :pagination="false"
                  size="small"
                  bordered
                  row-key="id"
                >
                  <a-table-column title="检查项" data-index="title" key="title" :width="180" />
                  <a-table-column title="状态" key="status" :width="90">
                    <template #default="{ record }">
                      <a-tag :color="diagnosticsTag(record.status).color">
                        {{ diagnosticsTag(record.status).text }}
                      </a-tag>
                    </template>
                  </a-table-column>
                  <a-table-column title="结果" data-index="message" key="message" />
                  <a-table-column title="建议" data-index="suggestion" key="suggestion" />
                </a-table>
              </a-card>
            </template>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<style scoped>
.graph-page {
  height: 100%;
}
.panel :deep(.ant-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.panel :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}
.panel :deep(.ant-tabs-content) {
  height: 100%;
}
.panel :deep(.ant-tabs-tabpane) {
  height: 100%;
}
.overview-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.overview-top {
  flex: 0 0 auto;
  margin-bottom: 12px;
}
.overview-table {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
.project-table :deep(.mono) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

:deep(.ant-table-cell-fix-right) {
  background: #fff !important;
}
:deep(.ant-table-row-selected .ant-table-cell-fix-right),
:deep(.row-selected .ant-table-cell-fix-right) {
  background: #e6f4ff !important;
}
:deep(.ant-table-cell-fix-right-first::after) {
  box-shadow: none !important;
}
:deep(th.ant-table-cell-fix-right-first),
:deep(td.ant-table-cell-fix-right-first) {
  border-left: 1px solid rgba(0, 0, 0, 0.1) !important;
  box-shadow: -4px 0 6px rgba(0, 0, 0, 0.06);
}
.logbox {
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 8px;
  overflow: hidden;
}
.logbox-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: rgba(0,0,0,.02);
  border-bottom: 1px solid rgba(0,0,0,.08);
}
.log {
  margin: 0;
  padding: 10px 12px;
  height: 60vh;
  min-height: 200px;
  overflow: auto;
  background: rgba(0,0,0,.03);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.log-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}
:deep(.row-selected td) { background: rgba(22,119,255,.08) !important; }

/* 导入任务表 */
.import-tasks-table-wrap { width: 100%; }

/* 导入表单：中间滚动 + 底部按钮固定 */
.import-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}
.import-form-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 8px;
}
.import-form-footer {
  flex-shrink: 0;
  padding: 10px 16px;
  border-top: 1px solid rgba(0,0,0,.08);
  background: #fff;
}

.import-desc-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 0;
  padding: 8px 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.015);
}

.import-desc-label {
  color: rgba(0, 0, 0, 0.65);
  font-weight: 600;
}

.diagnostics-pane {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}
.diagnostics-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.diagnostics-time {
  color: rgba(0, 0, 0, 0.45);
}
.diagnostics-schema-table {
  width: 100%;
  overflow-x: auto;
}

.query-pane {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(360px, 42%) minmax(420px, 58%);
  gap: 12px;
}
.query-left,
.query-right {
  min-height: 0;
  overflow: auto;
}
.query-template-desc {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.5);
  font-size: 12px;
}
.query-editor {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
.query-result-summary {
  margin-bottom: 10px;
}
.query-chart {
  margin-bottom: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 10px;
  background: rgba(22, 119, 255, 0.02);
}
.query-chart-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.query-chart-row {
  display: grid;
  grid-template-columns: minmax(120px, 38%) 1fr 80px;
  align-items: center;
  gap: 8px;
  margin: 6px 0;
}
.query-chart-name {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.query-chart-bar-wrap {
  height: 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  overflow: hidden;
}
.query-chart-bar {
  height: 100%;
  background: linear-gradient(90deg, #1677ff 0%, #69b1ff 100%);
}
.query-chart-value {
  text-align: right;
}

@media (max-width: 1320px) {
  .query-pane {
    grid-template-columns: 1fr;
  }
}

</style>


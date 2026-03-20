<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import { LinkOutlined } from '@ant-design/icons-vue'
import { useRepo } from '../composables/useRepo'
import { message } from 'ant-design-vue'

const active = ref<'overview' | 'query' | 'import' | 'diagnostics'>('overview')

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

// ---- Graph project selection (metadata -> import form) ----
type ImportMode = 'metadata' | 'custom'
const importMode = ref<ImportMode>('custom')
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
})
const importErrors = ref<{ repoUrl?: string; ref?: string }>({})
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

// ---- Import repo/ref (same UX as Bugfix page) ----
const repoState = useRepo({ lazyRefFetch: true })
const {
  repoUrl,
  branch,
  commitId,
  loadingBranches,
  loadingCommits,
  branchOptions,
  commitOptions,
  canSelectRef,
  switchRepoMode,
  onBranchSearch,
  onCommitSearch,
  onBranchDropdown,
  onCommitDropdown,
} = repoState

// 导入任务为「自定义」时只填 URL，需让 useRepo 按 URL 算 repo，否则 Branch 会一直 disabled
switchRepoMode('custom')

const refType = ref<'branch' | 'commit'>('branch')
watch(
  refType,
  (t) => {
    if (t === 'branch') commitId.value = undefined
    else branch.value = undefined
  },
  { flush: 'post' },
)

const selectedProjectRowKey = computed(() =>
  selectedGraphProject.value ? getProjectRowKey(selectedGraphProject.value) : '',
)

async function selectGraphProject(project: GraphProjectItem) {
  // 选中元数据后，导入表单会自动填充 repo_url/ref（URL/branch/commitId）
  selectedGraphProject.value = project
  importMode.value = 'metadata'

  const repo = String(project.repo_url || '').trim()
  const commit = project.commit_hash ? String(project.commit_hash).trim() : ''
  const br = project.branch ? String(project.branch).trim() : ''

  if (!repo) {
    switchToCustom()
    return
  }

  // 注意：useRepo 会在 repoUrl 变化时清空 branch/commitId，所以要在 nextTick 后再写回
  repoUrl.value = repo
  await nextTick()

  if (commit) {
    refType.value = 'commit'
    commitId.value = commit
    branch.value = undefined
    return
  }
  if (br) {
    refType.value = 'branch'
    branch.value = br
    commitId.value = undefined
    return
  }

  // 缺少 branch/commit_hash 的元数据，回退为自定义输入模式
  switchToCustom()
}

function switchToCustom() {
  importMode.value = 'custom'
  selectedGraphProject.value = null
}

async function fetchImportTasks() {
  const r = await fetch('/api/import/tasks')
  const data = await r.json()
  if (data?.ok) {
    importTasks.value = Array.isArray(data.items) ? data.items : []
    importStats.value = data.stats
    if (!selectedTaskId.value && importTasks.value.length) {
      selectedTaskId.value = importTasks.value[0].task_id
    }
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
    const r = await fetch(`/api/import/tasks/${taskId}/cancel`, { method: 'POST' })
    const data = await r.json().catch(() => ({}))
    if (!r.ok || !data?.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    await fetchImportTasks()
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  }
}

async function submitImport() {
  // 前端校验
  importErrors.value = {}
  const repo_url = (repoUrl.value || '').trim()
  const refVal = refType.value === 'branch' ? String(branch.value || '').trim() : String(commitId.value || '').trim()
  if (!repo_url) importErrors.value.repoUrl = 'Git 仓库地址不能为空'
  if (!refVal) importErrors.value.ref = refType.value === 'branch' ? '请选择或输入 Branch' : '请选择或输入 CommitId'
  if (Object.keys(importErrors.value).length) return

  importSubmitting.value = true
  try {
    const payload: any = {
      repo_url,
      maven_scan_enabled: Boolean(importForm.value.maven_scan_enabled),
      force_maven: Boolean(importForm.value.force_maven),
      clear_database: Boolean(importForm.value.clear_database),
    }
    if (refType.value === 'branch') payload.branch = refVal
    else payload.commit_id = refVal

    const r = await fetch('/api/import/tasks', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const data = await r.json().catch(() => ({}))
    if (!r.ok || !data?.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    await fetchImportTasks()
    selectedTaskId.value = data.task_id
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
    const url = new URL(`/api/import/tasks/${tid}/logs`, window.location.origin)
    url.searchParams.set('offset', String(logOffset.value))
    url.searchParams.set('limit', '2000')
    const r = await fetch(url.toString())
    const data = await r.json()
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
    const url = new URL('/api/graph/projects', window.location.origin)
    url.searchParams.set('include_counts', includeCounts.value ? 'true' : 'false')
    const r = await fetch(url.toString())
    const data = await r.json()
    if (!r.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    items.value = Array.isArray(data?.items) ? data.items : []
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchProjects()
  void fetchImportTasks()
  importPollTimer = window.setInterval(async () => {
    await fetchImportTasks()
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
    commit_short: (x.commit_hash ?? '').slice(0, 8),
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

        <a-tab-pane key="query" tab="查询模板">
          <a-empty description="待接入：常用 Cypher 模板 / 一键复制 / 结果渲染" />
        </a-tab-pane>

        <a-tab-pane key="import" tab="导入/重建">
          <a-row :gutter="16">
            <a-col :xs="24" :lg="10">
              <a-card size="small" class="import-card">
                <a-form layout="vertical" class="import-form">
                  <div class="import-form-scroll">
                  <div
                    v-if="importMode === 'metadata' && selectedGraphProject"
                    class="import-meta"
                    style="margin-bottom: 12px; padding: 10px 12px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: rgba(0,0,0,.02);"
                  >
                    <div class="import-meta-row">
                      <span class="import-meta-label" style="display:inline-block; min-width:64px; color: rgba(0,0,0,.65); font-weight:600; margin-right:6px;">来源项目：</span>
                      <span>{{ selectedGraphProject.project_name }}</span>
                    </div>
                    <div class="import-meta-row">
                      <span class="import-meta-label" style="display:inline-block; min-width:64px; color: rgba(0,0,0,.65); font-weight:600; margin-right:6px;">Git：</span>
                      <span class="mono">{{ selectedGraphProject.repo_url || '-' }}</span>
                    </div>
                    <div class="import-meta-row">
                      <span class="import-meta-label" style="display:inline-block; min-width:64px; color: rgba(0,0,0,.65); font-weight:600; margin-right:6px;">Ref：</span>
                      <span class="mono">
                        {{
                          selectedGraphProject.commit_hash
                            ? `Commit ${String(selectedGraphProject.commit_hash).slice(0, 8)}`
                            : String(selectedGraphProject.branch || '-')
                        }}
                      </span>
                    </div>
                    <a-button size="small" @click="switchToCustom">
                      切换为自定义
                    </a-button>
                  </div>

                  <a-form-item
                    v-if="importMode === 'custom'"
                    label="Git 仓库地址"
                    required
                    :validate-status="importErrors.repoUrl ? 'error' : ''"
                    :help="importErrors.repoUrl"
                  >
                    <a-input v-model:value="repoUrl" placeholder="http(s)://.../*.git" allow-clear @change="importErrors.repoUrl = undefined" />
                  </a-form-item>

                  <a-form-item v-if="importMode === 'custom'" label="Ref（Branch / Commit 二选一）" required>
                    <a-radio-group v-model:value="refType" button-style="solid">
                      <a-radio-button value="branch">Branch</a-radio-button>
                      <a-radio-button value="commit">CommitId</a-radio-button>
                    </a-radio-group>
                  </a-form-item>

                  <a-form-item
                    v-if="importMode === 'custom' && refType === 'branch'"
                    label="Branch"
                    required
                    :validate-status="importErrors.ref ? 'error' : ''"
                    :help="importErrors.ref"
                  >
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
                      @change="importErrors.ref = undefined"
                      allow-clear
                    >
                      <template #notFoundContent>
                        <a-space v-if="loadingBranches" size="small">
                          <a-spin size="small" />
                          <span>加载中...</span>
                        </a-space>
                        <span v-else>暂无数据</span>
                      </template>
                    </a-select>
                  </a-form-item>

                  <a-form-item
                    v-if="importMode === 'custom' && refType === 'commit'"
                    label="CommitId"
                    required
                    :validate-status="importErrors.ref ? 'error' : ''"
                    :help="importErrors.ref"
                  >
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
                      @change="importErrors.ref = undefined"
                      allow-clear
                    >
                      <template #notFoundContent>
                        <a-space v-if="loadingCommits" size="small">
                          <a-spin size="small" />
                          <span>加载中...</span>
                        </a-space>
                        <span v-else>暂无数据</span>
                      </template>
                    </a-select>
                  </a-form-item>

                  <a-form-item label="解析 pom 外部依赖（Maven 扫描）">
                    <a-switch v-model:checked="importForm.maven_scan_enabled" checked-children="启用" un-checked-children="关闭" />
                  </a-form-item>

                  <a-form-item label="强制重新解析 Maven（force_maven）">
                    <a-switch v-model:checked="importForm.force_maven" checked-children="强制" un-checked-children="默认" />
                  </a-form-item>

                  <a-form-item label="导入前清理旧图谱（clear_database）">
                    <a-switch v-model:checked="importForm.clear_database" checked-children="清理" un-checked-children="不清理" />
                  </a-form-item>
                  </div>

                  <div class="import-form-footer">
                    <a-button type="primary" :loading="importSubmitting" @click="submitImport">
                      提交导入（并发=1，自动排队）
                    </a-button>
                  </div>
                </a-form>
              </a-card>
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
                  <a-table-column title="ref" key="ref" :width="120">
                    <template #default="{ record }">
                      <span class="mono">{{ record.commit_id ? (record.commit_id || '').slice(0,8) : record.branch }}</span>
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
          <a-empty description="待接入：约束/索引、节点/关系数量、热点查询、连通性检查" />
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
  max-height: 66vh;
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
  height: calc(100vh - 56px - 32px - 48px - 16px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.import-card :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 0;
}
.import-form {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.import-form-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
}
.import-form-footer {
  flex-shrink: 0;
  padding-top: 10px;
  padding-bottom: 10px;
  border-top: 1px solid rgba(0,0,0,.08);
  background: #fff;
}
</style>


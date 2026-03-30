<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import {
  listImportTasks,
  cancelImportTask as apiCancelImportTask,
  getImportTaskLog,
  type ImportTask,
} from '../api'

const emit = defineEmits<{
  (e: 'open-log', taskId: string): void
  (e: 'open-acceptance-detail', task: ImportTask): void
}>()

const importTasksLoading = ref(false)
const importTasks = ref<ImportTask[]>([])
const importStats = ref<any>(null)
const selectedTaskId = ref<string | null>(null)

// 有 running/pending 任务时自动轮询
let pollTimer: number | undefined
const hasActiveTask = computed(() =>
  importTasks.value.some(t => {
    const s = String(t.status || '').toLowerCase()
    return s === 'running' || s === 'pending' || s === 'queued'
  })
)

function startPolling() {
  if (pollTimer) return
  pollTimer = window.setInterval(async () => {
    await fetchImportTasks()
    if (!hasActiveTask.value) stopPolling()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) { window.clearInterval(pollTimer); pollTimer = undefined }
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

function taskTypeTag(task: any) {
  const t = String(task?.task_type || 'auto').toLowerCase()
  if (t === 'full') return { color: 'orange', text: '全量 (full)' }
  if (t === 'incremental') return { color: 'green', text: '增量 (incremental)' }
  return { color: 'blue', text: '自动 (auto)' }
}

function effectiveModeTag(task: any) {
  const mode = String(task?.result?.extra?.effective_mode || '').toLowerCase()
  if (mode === 'incremental') return { color: 'green', text: '实际：增量' }
  if (mode === 'full') return { color: 'orange', text: '实际：全量' }
  return null
}

function taskAcceptance(task: any): any | null {
  const v = task?.result?.extra?.acceptance
  return v && typeof v === 'object' ? v : null
}

function acceptanceTag(task: any): { color: string; text: string } {
  const acceptance = taskAcceptance(task)
  if (!acceptance?.enabled) return { color: 'default', text: '未启用' }
  const s = String(acceptance?.status || '').toLowerCase()
  if (s === 'passed') return { color: 'green', text: '通过' }
  if (s === 'failed') return { color: 'red', text: '失败' }
  if (s === 'skipped') return { color: 'orange', text: '跳过' }
  return { color: 'default', text: '-' }
}

function acceptanceDeltaText(task: any): string {
  const detail = task?.result?.extra?.delta_detail
  const snapshot = task?.result?.extra?.snapshot_delta
  const addedNodes = Number((detail as any)?.added_nodes?.total ?? 0)
  const deletedNodes = Number((detail as any)?.deleted_nodes?.total ?? 0)
  const addedRels = Number((detail as any)?.added_relationships?.total ?? 0)
  const deletedRels = Number((detail as any)?.deleted_relationships?.total ?? 0)

  if (
    Number.isFinite(addedNodes) &&
    Number.isFinite(deletedNodes) &&
    Number.isFinite(addedRels) &&
    Number.isFinite(deletedRels) &&
    (addedNodes > 0 || deletedNodes > 0 || addedRels > 0 || deletedRels > 0)
  ) {
    return `节点：+${addedNodes} -${deletedNodes} / 边：+${addedRels} -${deletedRels}`
  }

  const nodeDelta = Number((snapshot as any)?.node_count || 0)
  const relDelta = Number((snapshot as any)?.relationship_count || 0)
  const nodeAdd = nodeDelta > 0 ? nodeDelta : 0
  const nodeDel = nodeDelta < 0 ? Math.abs(nodeDelta) : 0
  const relAdd = relDelta > 0 ? relDelta : 0
  const relDel = relDelta < 0 ? Math.abs(relDelta) : 0
  return `节点：+${nodeAdd} -${nodeDel} / 边：+${relAdd} -${relDel}`
}

function isCancellableTaskStatus(status: any) {
  const s = String(status || '').toLowerCase()
  return s === 'pending' || s === 'running'
}

async function fetchImportTasks() {
  importTasksLoading.value = true
  try {
    const data = await listImportTasks()
    if (data?.ok) {
      importTasks.value = Array.isArray(data.items) ? data.items : []
      importStats.value = (data as any).stats
      if (!selectedTaskId.value && importTasks.value.length) {
        selectedTaskId.value = importTasks.value[0].task_id
      }
    }
  } finally {
    importTasksLoading.value = false
    if (hasActiveTask.value) startPolling()
    else stopPolling()
  }
}

async function cancelImportTask(taskId: string) {
  if (!taskId) return
  const t = importTasks.value.find((x) => x?.task_id === taskId)
  if (t && !isCancellableTaskStatus(t.status)) return
  if (!window.confirm('确认终止该导入任务？正在执行中的任务会在可中断点尽快停止。')) return
  try {
    await apiCancelImportTask(taskId)
    await fetchImportTasks()
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  }
}

function selectTask(id: string) {
  selectedTaskId.value = id
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
async function openAcceptanceDetail(task: ImportTask) {
  emit('open-acceptance-detail', task)
}

// ── Log Modal ─────────────────────────────────────────────────────────────────
const logModalOpen = ref(false)
const logModalTaskId = ref<string>('')
const logLines = ref<string[]>([])
const logOffset = ref(0)
const logHasMore = ref(true)
const logLoading = ref(false)
const logStickToBottom = ref(true)
const logWrap = ref(true)
const logViewEl = ref<HTMLElement | null>(null)
let _programmaticScroll = false
let _logPollTimer: number | undefined

const logModalTask = computed(() =>
  importTasks.value.find((t: any) => t?.task_id === logModalTaskId.value) || null
)
const logModalTaskDone = computed(() => {
  const s = String((logModalTask.value as any)?.status || '').toLowerCase()
  return s === 'success' || s === 'failed' || s === 'cancelled' || s === 'canceled'
})

function _startLogPolling() {
  if (_logPollTimer) return
  _logPollTimer = window.setInterval(async () => {
    if (!logModalOpen.value) { _stopLogPolling(); return }
    if (logModalTaskDone.value) {
      await drainLogsIfDone()
      _stopLogPolling()
      return
    }
    logHasMore.value = true  // 任务未完成时强制允许继续拉取
    await fetchLogs()
  }, 2000)
}

function _stopLogPolling() {
  if (_logPollTimer) { window.clearInterval(_logPollTimer); _logPollTimer = undefined }
}

async function fetchLogs() {
  if (!logModalTaskId.value || !logHasMore.value || logLoading.value) return
  logLoading.value = true
  try {
    const data = await getImportTaskLog(logModalTaskId.value, logOffset.value, 2000)
    if (data?.ok) {
      const lines = Array.isArray(data.lines) ? data.lines : []
      if (lines.length) logLines.value.push(...lines)
      logOffset.value = Number(data.next_offset ?? logOffset.value)
      if (!lines.length && logModalTaskDone.value) logHasMore.value = false
    }
  } finally {
    logLoading.value = false
    if (logStickToBottom.value) {
      _programmaticScroll = true
      await nextTick()
      if (logViewEl.value) logViewEl.value.scrollTop = logViewEl.value.scrollHeight
      setTimeout(() => { _programmaticScroll = false }, 50)
    }
  }
}

async function drainLogsIfDone() {
  if (!logModalTaskDone.value) return
  for (let i = 0; i < 30; i++) {
    if (!logHasMore.value) break
    const before = logOffset.value
    await fetchLogs()
    if (logOffset.value === before) break
  }
}

async function openLogModal(taskId: string) {
  logModalTaskId.value = taskId
  logModalOpen.value = true
  logOffset.value = 0
  logLines.value = []
  logHasMore.value = true
  logStickToBottom.value = true
  _stopLogPolling()
  setTimeout(() => {
    fetchLogs().then(() => {
      if (logModalTaskDone.value) drainLogsIfDone()
      else _startLogPolling()
    })
  }, 320)
}

function closeLogModal() {
  logModalOpen.value = false
  _stopLogPolling()
}

function reloadLogs() {
  logOffset.value = 0
  logLines.value = []
  logHasMore.value = true
  logStickToBottom.value = true
  fetchLogs().then(() => drainLogsIfDone())
}

function onLogScroll() {
  const el = logViewEl.value
  if (!el || _programmaticScroll) return
  const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 80
  logStickToBottom.value = atBottom
  if (atBottom && !logLoading.value) void fetchLogs()
}

// 暴露方法给父组件
defineExpose({
  fetchImportTasks,
  importTasksLoading,
  importTasks,
  importStats,
  selectedTaskId,
})

onMounted(() => {
  fetchImportTasks()
})

onUnmounted(() => {
  stopPolling()
  _stopLogPolling()
})
</script>

<template>
  <div class="import-task-list">
    <a-card size="small">
      <template #title>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>任务队列与日志</span>
          <ReloadOutlined
            :spin="importTasksLoading"
            style="cursor:pointer;font-size:15px;color:#595959"
            @click="fetchImportTasks"
          />
        </div>
      </template>
      <div class="import-tasks-table-wrap">
        <a-table
          :loading="importTasksLoading"
          :data-source="importTasks as any"
          :pagination="{ pageSize: 6 }"
          size="small"
          bordered
          :scroll="{ x: 980 }"
          :row-class-name="(r: any) => (r.task_id === selectedTaskId ? 'row-selected' : '')"
          :custom-row="(record: any) => ({ onClick: () => selectTask(record.task_id) })"
        >
          <a-table-column title="task_id" data-index="task_id" key="task_id" :width="120" ellipsis />
          <a-table-column title="状态" data-index="status" key="status" :width="90">
            <template #default="{ record }">
              <a-tag :color="statusTag(record.status).color">
                {{ statusTag(record.status).text }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="任务类型" key="task_type" :width="180">
            <template #default="{ record }">
              <div class="task-type-tags">
                <a-tag :color="taskTypeTag(record).color">{{ taskTypeTag(record).text }}</a-tag>
                <a-tag v-if="effectiveModeTag(record)" :color="effectiveModeTag(record)?.color">
                  {{ effectiveModeTag(record)?.text }}
                </a-tag>
              </div>
            </template>
          </a-table-column>
          <a-table-column title="验收" key="acceptance" :width="380">
            <template #default="{ record }">
              <a-space direction="vertical" :size="2">
                <a-tag
                  :color="acceptanceTag(record).color"
                  style="cursor: pointer;"
                  @click.stop="openAcceptanceDetail(record)"
                >
                  {{ acceptanceTag(record).text }}
                </a-tag>
                <span class="mono" style="font-size: 12px;">{{ acceptanceDeltaText(record) }}</span>
              </a-space>
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
    </a-card>

    <!-- 日志 Modal -->
    <a-modal
      v-model:open="logModalOpen"
      :title="`任务日志：${logModalTaskId || '-'}`"
      width="1180px"
      :footer="null"
      :body-style="{ padding: '0', height: 'calc(70vh)', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
      @cancel="closeLogModal"
    >
      <div class="logbox">
        <div class="logbox-hd">
          <span class="mono" style="font-size:12px;color:#666">{{ logModalTaskId }}</span>
          <a-space>
            <a-switch v-model:checked="logWrap" checked-children="换行" un-checked-children="不换行" size="small" />
            <a-button size="small" :loading="logLoading" @click="fetchLogs">拉取更多</a-button>
            <a-button size="small" @click="reloadLogs">重载</a-button>
          </a-space>
        </div>
        <div ref="logViewEl" class="log" :class="{ 'log--nowrap': !logWrap }" @scroll="onLogScroll">
          <div v-if="logLoading && logLines.length === 0" style="padding:40px;text-align:center">
            <a-spin size="large" />
            <div style="margin-top:12px;color:rgba(0,0,0,.45);font-size:13px">正在加载日志…</div>
          </div>
          <pre v-else class="log-pre">{{ logLines.join('\n') }}</pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.import-task-list {
  height: 100%;
}

.import-tasks-table-wrap {
  max-height: calc(70vh - 120px);
  overflow-y: auto;
}

.task-type-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.mono {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.logbox {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.logbox-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid rgba(0,0,0,0.08);
  flex-shrink: 0;
}
.log {
  flex: 1;
  overflow-y: auto;
  overflow-x: auto;
  background: #f8f8f8;
  padding: 12px 16px;
  border: 1px solid #e8e8e8;
}
.log--nowrap .log-pre {
  white-space: pre;
  word-break: normal;
}
.log-pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #1a1a1a;
  white-space: pre-wrap;
  word-break: break-all;
  line-height: 1.6;
}
</style>

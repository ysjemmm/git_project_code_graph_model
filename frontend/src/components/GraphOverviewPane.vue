<script setup lang="ts">
import { computed, ref } from 'vue'
import { LinkOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import { listProjectsForOverview, listImportTasks } from '../api'
import type { GraphProjectVersionItem } from '../api'
import SkeletonModal from './SkeletonModal.vue'

interface GraphProjectItem {
  project_name: string
  project_type?: string
  version?: string
  project_key?: string
  node_count?: number
  relationship_count?: number
  last_update_time?: string
  branch?: string
  commit_hash?: string
}

const emit = defineEmits<{
  (e: 'select-project', project: GraphProjectItem): void
}>()

// 内部管理的选中状态（不再依赖外部 props）
const selectedProjectName = ref<string | null>(null)

const loading = ref(false)
const items = ref<GraphProjectItem[]>([])
const total = ref(0)
const nextCursor = ref<string | undefined>(undefined)
const pageSize = 15

// tableData：把每个项目的 versions 展开成多行，支持多版本合并单元格
const tableData = computed(() => {
  const rows: any[] = []
  for (const p of items.value) {
    const versions: GraphProjectVersionItem[] = (p as any).versions || []
    if (versions.length === 0) {
      rows.push({
        key: `${p.project_name}::`,
        ...p,
        _groupSize: 1,
        _groupIdx: 0,
      })
    } else {
      versions.forEach((v, idx) => {
        rows.push({
          key: `${p.project_name}::${v.version ?? ''}::${v.branch ?? ''}`,
          project_name: p.project_name,
          project_type: p.project_type,
          last_update_time: p.last_update_time,
          version: v.version,
          project_key: v.project_key,
          branch: v.branch,
          commit_hash: v.commit_hash,
          node_count: v.node_count,
          relationship_count: v.relationship_count,
          _groupSize: versions.length,
          _groupIdx: idx,
        })
      })
    }
  }
  return rows
})
const publishRecordModalOpen = ref(false)
const publishRecordProjectName = ref('')
const publishRecordHistory = ref<any[]>([])
const publishRecordTask = ref<any | null>(null)
const publishRecordLoading = ref(false)

// 分页相关
const publishRecordPage = ref(1)
const publishRecordPageSize = ref(6)
const publishRecordTotal = ref(0)
const pageChanging = ref(false) // 标记是否正在切换页码

const columns = [
  { title: '项目', dataIndex: 'project_name', key: 'project_name', width: 200, ellipsis: true,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
  { title: '类型', dataIndex: 'project_type', key: 'project_type', width: 100,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
  { title: '版本', dataIndex: 'version', key: 'version', width: 120, ellipsis: true },
  { title: '节点数', dataIndex: 'node_count', key: 'node_count', width: 100 },
  { title: '关系数', dataIndex: 'relationship_count', key: 'relationship_count', width: 100 },
  { title: '更新时间', dataIndex: 'last_update_time', key: 'last_update_time', width: 190, ellipsis: true,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
  { title: '发布记录', key: 'release_record', width: 100,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
] as const

function openAura() {
  window.open(NEO4J_AURA_QUERY_URL, '_blank')
}

async function fetchProjects(cursor?: string) {
  loading.value = true
  try {
    const data = await listProjectsForOverview({ page_size: pageSize, cursor })
    items.value = Array.isArray(data.data) ? data.data : []
    total.value = data.total ?? 0
    nextCursor.value = data.next_cursor ?? undefined
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    loading.value = false
  }
}

function selectProject(record: GraphProjectItem) {
  emit('select-project', record)
}

async function handleRefresh() {
  nextCursor.value = undefined
  await fetchProjects()
}

async function viewRecord(record: GraphProjectItem) {
  const projectName = String(record?.project_name || '')
  
  publishRecordModalOpen.value = true
  publishRecordLoading.value = true
  publishRecordHistory.value = []
  
  try {
    const data = await listImportTasks({ 
      projectName, 
      page: 1, 
      page_size: 6 
    })
    const history = Array.isArray(data.items) ? data.items : []
    
    publishRecordProjectName.value = projectName
    publishRecordHistory.value = history
    publishRecordTotal.value = data.total ?? history.length
    publishRecordPage.value = 1
    
    if (!history.length) {
      publishRecordTask.value = null
    } else {
      publishRecordTask.value = history[0]
    }
  } catch (e: any) {
    message.error(e?.message ?? String(e))
    publishRecordModalOpen.value = false
  } finally {
    publishRecordLoading.value = false
  }
}

function selectPublishHistoryTask(record: any) {
  publishRecordTask.value = record || null
}

function handlePageChange(page: number, pageSize?: number) {
  publishRecordPage.value = page
  if (pageSize) {
    publishRecordPageSize.value = pageSize
  }
  
  // 加载新的一页数据
  void loadPublishRecordPage(page, publishRecordPageSize.value)
}

async function loadPublishRecordPage(page: number, pageSize: number) {
  const projectName = publishRecordProjectName.value
  if (!projectName) return
  
  // ✅ 设置切换状态（不隐藏内容）
  pageChanging.value = true
  
  try {
    const data = await listImportTasks({ 
      projectName, 
      page, 
      page_size: pageSize 
    })
    const history = Array.isArray(data.items) ? data.items : []
    
    publishRecordHistory.value = history
    publishRecordTotal.value = data.total ?? history.length
    
    // 切换到该页第一个任务
    if (history.length > 0) {
      publishRecordTask.value = history[0]
    }
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    // ✅ 延迟一点关闭，让过渡更平滑
    setTimeout(() => {
      pageChanging.value = false
    }, 200)
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

function taskFallbackReason(task: any): string {
  return String(task?.result?.extra?.fallback_reason || '').trim()
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

function acceptanceSummary(task: any): string {
  const acceptance = taskAcceptance(task)
  if (!acceptance) return '-'
  return String(acceptance.summary || '-')
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

function boolLabel(v: any): string {
  return Boolean(v) ? '启用' : '关闭'
}

// 组件加载时自动获取数据
fetchProjects()
</script>

<template>
  <div class="overview-pane">
    <div class="overview-top">
      <a-space>
        <a-button @click="openAura">
          <template #icon><LinkOutlined /></template>
          打开 Neo4j Aura Query
        </a-button>
        <a-button :loading="loading" @click="handleRefresh">刷新</a-button>
        <a-button v-if="nextCursor" :loading="loading" @click="fetchProjects(nextCursor)">加载更多</a-button>
        <span v-if="total" style="color: #999; font-size: 12px;">共 {{ total }} 个项目</span>
      </a-space>
    </div>

    <!-- 发布记录弹窗 -->
    <SkeletonModal
      v-model:open="publishRecordModalOpen"
      title="发布历史记录"
      :loading="publishRecordLoading"
      :footer="null"
      width="1100px"
      :body-style="{ padding: '16px', maxHeight: '75vh', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
    >
      <!-- 有数据时显示详情 -->
      <a-spin :spinning="pageChanging">
        <div class="publish-record-content" v-if="publishRecordHistory.length" style="display: flex; flex-direction: column; height: 100%;">   
            <!-- Tabs 导航 -->
            <div style="margin-bottom: 12px; flex-shrink: 0">
            <a-tabs
                :activeKey="publishRecordTask?.task_id"
                @update:activeKey="(key: any) => selectPublishHistoryTask(publishRecordHistory.find(t => t.task_id === key))"
                :style="{ marginBottom: '0' }"
            >
                <a-tab-pane 
                v-for="task in publishRecordHistory"
                :key="task.task_id"
                :tab="task.task_id" />
            </a-tabs>
            </div>
        
            <!-- 详情内容区域（可滚动） -->
            <div style="flex: 1; overflow-y: auto; overflow-x: hidden; margin-bottom: 12px; min-height: 0">
            <a-descriptions 
                v-if="publishRecordTask" 
                bordered 
                :column="{ xs: 1, sm: 2 }"
                size="small"
            >
                <a-descriptions-item label="任务 ID" :span="2">
                <span class="mono">{{ publishRecordTask.task_id || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="项目名称">
                <span class="mono">{{ publishRecordTask.project_name || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="状态">
                <a-tag :color="statusTag(publishRecordTask.status).color">{{ statusTag(publishRecordTask.status).text }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="任务类型">
                <div class="task-type-tags">
                    <a-tag :color="taskTypeTag(publishRecordTask).color">{{ taskTypeTag(publishRecordTask).text }}</a-tag>
                    <a-tag v-if="effectiveModeTag(publishRecordTask)" :color="effectiveModeTag(publishRecordTask)?.color">
                    {{ effectiveModeTag(publishRecordTask)?.text }}
                    </a-tag>
                </div>
                </a-descriptions-item>
                <a-descriptions-item label="降级原因" :span="2">
                <span class="mono">{{ taskFallbackReason(publishRecordTask) || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="repo_url" :span="2">
                <span class="mono" style="word-break: break-all">{{ publishRecordTask.repo_url || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="验收结论">
                <a-tag :color="acceptanceTag(publishRecordTask).color">{{ acceptanceTag(publishRecordTask).text }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="验收摘要">
                <span class="mono">{{ acceptanceSummary(publishRecordTask) || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="快照 Delta" :span="2">
                <span class="mono">{{ acceptanceDeltaText(publishRecordTask) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Ref 类型">
                <span class="mono">{{ publishRecordTask.commit_id ? 'CommitId' : 'Branch' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Ref">
                <span class="mono">{{ publishRecordTask.commit_id || publishRecordTask.branch || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Maven 扫描">
                <span class="mono">{{ boolLabel(publishRecordTask.maven_scan_enabled) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="强制 Maven">
                <span class="mono">{{ boolLabel(publishRecordTask.force_maven) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="清理旧图谱">
                <span class="mono">{{ boolLabel(publishRecordTask.clear_database) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="自动关联外部类">
                <span class="mono">{{ boolLabel(publishRecordTask.auto_link_external) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="创建时间">
                <span class="mono">{{ String(publishRecordTask.created_at || '').slice(0, 19).replace('T', ' ') || '-' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="完成时间">
                <span class="mono">{{ String(publishRecordTask.completed_at || '').slice(0, 19).replace('T', ' ') || '-' }}</span>
                </a-descriptions-item>
            </a-descriptions>
            
            <!-- 没有选中任务时显示空状态 -->
            <a-empty v-else description="暂无任务详情" />
            </div>

            <!-- 底部分页器 -->
            <div :style="{ marginTop: 'auto', paddingTop: '12px', borderTop: '1px solid #f0f0f0', textAlign: 'right' }">
            <a-pagination 
                v-model:current="publishRecordPage"
                v-model:page-size="publishRecordPageSize"
                :total="publishRecordTotal"
                :showSizeChanger="true"
                :showQuickJumper="true"
                :showTotal="(total: any) => `共 ${total} 条记录`"
                @change="handlePageChange"
                @showSizeChange="handlePageChange"
            />
            </div>
        </div>
        <!-- 有历史记录但当前页无数据 -->
        <div v-else-if="publishRecordTotal > 0 && !publishRecordHistory.length" style="padding: 80px; text-align: center">
            <a-empty description="当前页无数据" />
        </div>
        
        <!-- 完全没有历史记录 -->
        <div v-else style="padding: 80px; text-align: center">
            <a-empty description="暂无导入/发布记录" />
        </div>
     </a-spin>
    </SkeletonModal>

    <div class="overview-table">
      <a-table
        class="project-table"
        :loading="loading"
        :columns="columns as any"
        :data-source="tableData as any"
        :pagination="{ pageSize: pageSize, showSizeChanger: false, total: tableData.length }"
        size="small"
        bordered
        row-key="key"
        :row-class-name="(r: any) => (String(r?.key) === String(selectedProjectName) ? 'row-selected' : '')"
        :custom-row="(record: any) => ({ onClick: () => selectProject(record) })"
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
          <template v-else-if="column.key === 'release_record'">
            <a-button type="link" size="small" @click.stop="viewRecord(record)">
              <template #icon><EyeOutlined /></template>
              查看
            </a-button>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<style scoped>
.overview-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.overview-top {
  margin-bottom: 16px;
}

.overview-table {
  flex: 1;
  overflow: auto;
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

.row-selected {
  background-color: #e6f7ff !important;
}
</style>

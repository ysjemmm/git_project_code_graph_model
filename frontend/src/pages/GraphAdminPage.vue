<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import hljs from 'highlight.js/lib/common'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import { LinkOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { useRepo } from '../composables/useRepo'
import { message } from 'ant-design-vue'
import {
  listImportTasks,
  cancelImportTask as apiCancelImportTask,
  unblockAutoImportTasks,
  createImportTask,
  getImportSettings,
  getImportTaskAcceptanceDetail,
  getImportTaskLog,
  listGraphProjects,
  runGraphQuery,
  getGraphDiagnosticsSummary,
  clearGraphDatabase,
  getGitDiffSummary,
  getGitDiffFile,
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

function taskTypeTag(task: any) {
  const t = String(task?.task_type || 'auto').toLowerCase()
  if (t === 'full') return { color: 'orange', text: '全量(full)' }
  if (t === 'incremental') return { color: 'green', text: '增量(incremental)' }
  return { color: 'blue', text: '自动(auto)' }
}

function effectiveModeTag(task: any) {
  const mode = String(task?.result?.extra?.effective_mode || '').toLowerCase()
  if (mode === 'incremental') return { color: 'green', text: '实际:增量' }
  if (mode === 'full') return { color: 'orange', text: '实际:全量' }
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

function acceptanceDetail(task: any): any | null {
  const persisted = acceptanceDetailData.value?.delta_detail
  if (persisted && typeof persisted === 'object' && Object.keys(persisted).length > 0) return persisted
  const v = task?.result?.extra?.delta_detail
  return v && typeof v === 'object' ? v : null
}

function acceptanceFiles(): { added: string[]; changed: string[]; deleted: string[] } {
  const p = acceptanceDetailData.value
  if (p?.delta_detail?.files && typeof p.delta_detail.files === 'object') {
    return {
      added: Array.isArray(p.delta_detail.files.added) ? p.delta_detail.files.added : [],
      changed: Array.isArray(p.delta_detail.files.changed) ? p.delta_detail.files.changed : [],
      deleted: Array.isArray(p.delta_detail.files.deleted) ? p.delta_detail.files.deleted : [],
    }
  }
  return {
    added: Array.isArray(p?.added_files_list) ? p.added_files_list : [],
    changed: Array.isArray(p?.changed_files_list) ? p.changed_files_list : [],
    deleted: Array.isArray(p?.deleted_files_list) ? p.deleted_files_list : [],
  }
}

function hasEntries(v: any): boolean {
  return Boolean(v && typeof v === 'object' && Object.keys(v).length > 0)
}

function acceptanceTypeStats(task: any): {
  nodeByLabel: Record<string, number>
  relByType: Record<string, number>
  changedRelByType: Record<string, number>
} {
  const detail = acceptanceDetail(task)
  const snapshotDelta =
    acceptanceDetailData.value?.snapshot_delta ||
    task?.result?.extra?.snapshot_delta ||
    {}

  const nodeByLabelRaw = (detail as any)?.added_nodes?.by_label
  const relByTypeRaw = (detail as any)?.added_relationships?.by_type
  const changedRelByTypeRaw = (detail as any)?.changed_relationships?.by_type

  const snapshotNodeByLabel = (snapshotDelta as any)?.node_count_by_label
  const snapshotRelByType = (snapshotDelta as any)?.relationship_count_by_type
  // 优先展示前后快照真实 delta（可同时体现新增与删除）；缺失时再回退导出明细
  const nodeByLabel = hasEntries(snapshotNodeByLabel)
    ? snapshotNodeByLabel
    : (hasEntries(nodeByLabelRaw) ? nodeByLabelRaw : {})
  const relByType = hasEntries(snapshotRelByType)
    ? snapshotRelByType
    : (hasEntries(relByTypeRaw) ? relByTypeRaw : {})
  const changedRelByType = hasEntries(changedRelByTypeRaw) ? changedRelByTypeRaw : {}

  return { nodeByLabel, relByType, changedRelByType }
}

function typeDeltaRows(typeMap: Record<string, number>): Array<{
  key: string
  type: string
  added: number
  deleted: number
  delta: number
}> {
  return Object.entries(typeMap || {})
    .map(([k, v]) => {
      const delta = Number(v || 0)
      return {
        key: String(k || ''),
        type: String(k || ''),
        added: delta > 0 ? delta : 0,
        deleted: delta < 0 ? Math.abs(delta) : 0,
        delta,
      }
    })
    .filter((x) => x.type && Number.isFinite(x.delta) && (x.added > 0 || x.deleted > 0))
    .sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))
}

function topTypeDeltaRows(typeMap: Record<string, number>, limit = 10): Array<{
  key: string
  type: string
  added: number
  deleted: number
  delta: number
  total: number
  addPercent: number
  delPercent: number
}> {
  const rows = typeDeltaRows(typeMap).slice(0, limit)
  const maxTotal = Math.max(1, ...rows.map((r) => r.added + r.deleted))
  return rows.map((r) => {
    const total = r.added + r.deleted
    return {
      ...r,
      total,
      addPercent: total > 0 ? (r.added / maxTotal) * 100 : 0,
      delPercent: total > 0 ? (r.deleted / maxTotal) * 100 : 0,
    }
  })
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
  acceptance_enabled: true,
  acceptance_block_on_fail: false,
  acceptance_max_drop_ratio: 0.3,
})
const importSubmitting = ref(false)
const unblockingAutoTasks = ref(false)
const clearingGraph = ref(false)
const importTasks = ref<ImportTask[]>([])
const importStats = ref<any>(null)
const selectedTaskId = ref<string | null>(null)
const publishRecordModalOpen = ref(false)
const publishRecordProjectName = ref('')
const publishRecordHistory = ref<any[]>([])
const publishRecordTask = ref<any | null>(null)
const diffLoading = ref(false)
const diffError = ref<string | null>(null)
const diffSummary = ref<any | null>(null)
const diffSummaryModalOpen = ref(false)
const diffFileModalOpen = ref(false)
const diffFileLoading = ref(false)
const diffFilePatch = ref('')
const diffFilePath = ref('')
const acceptanceDetailModalOpen = ref(false)
const acceptanceDetailTask = ref<any | null>(null)
const acceptanceDetailData = ref<any | null>(null)
const acceptanceDetailLoading = ref(false)

function escapeHtml(s: string): string {
  return String(s || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

function detectCodeLanguageByFilePath(filePath: string): string {
  const p = String(filePath || '').toLowerCase()
  if (p.endsWith('.java')) return 'java'
  if (p.endsWith('.xml') || p.endsWith('.pom')) return 'xml'
  if (p.endsWith('.yml') || p.endsWith('.yaml')) return 'yaml'
  if (p.endsWith('.json')) return 'json'
  if (p.endsWith('.ts')) return 'typescript'
  if (p.endsWith('.tsx')) return 'typescript'
  if (p.endsWith('.js')) return 'javascript'
  if (p.endsWith('.jsx')) return 'javascript'
  if (p.endsWith('.py')) return 'python'
  if (p.endsWith('.sql') || p.endsWith('.cypher')) return 'sql'
  if (p.endsWith('.md')) return 'markdown'
  if (p.endsWith('.sh') || p.endsWith('.bash')) return 'bash'
  if (p.endsWith('.properties')) return 'properties'
  return ''
}

function highlightCodeLine(code: string, language: string): string {
  const text = String(code ?? '')
  if (!text) return '&nbsp;'
  try {
    if (language && hljs.getLanguage(language)) {
      return hljs.highlight(text, { language, ignoreIllegals: true }).value
    }
    return hljs.highlightAuto(text).value
  } catch {
    return escapeHtml(text)
  }
}

type DiffLineKind = 'meta' | 'hunk' | 'ctx' | 'add' | 'del'
type DiffRenderLine = {
  key: string
  kind: DiffLineKind
  oldLine: number | null
  newLine: number | null
  prefix: string
  html: string
}

const diffRenderLines = computed<DiffRenderLine[]>(() => {
  const patch = String(diffFilePatch.value || '')
  if (!patch) return []
  const lines = patch.split('\n')
  const language = detectCodeLanguageByFilePath(diffFilePath.value)
  let oldCursor = 0
  let newCursor = 0
  const out: DiffRenderLine[] = []
  for (let i = 0; i < lines.length; i += 1) {
    const raw = lines[i] ?? ''
    const mk = (kind: DiffLineKind, oldLine: number | null, newLine: number | null, prefix: string, text: string) => {
      out.push({
        key: `${i}-${kind}-${oldLine ?? ''}-${newLine ?? ''}`,
        kind,
        oldLine,
        newLine,
        prefix,
        html: highlightCodeLine(text, language),
      })
    }
    if (raw.startsWith('@@')) {
      const m = raw.match(/^@@\s*-(\d+)(?:,\d+)?\s+\+(\d+)(?:,\d+)?\s*@@(.*)$/)
      if (m) {
        oldCursor = Number(m[1] || 0)
        newCursor = Number(m[2] || 0)
      }
      mk('hunk', null, null, '@@', raw)
      continue
    }
    // 隐藏 git patch 文件头噪音，只保留 hunk 与增删改正文
    if (
      raw.startsWith('diff ') ||
      raw.startsWith('index ') ||
      raw.startsWith('--- ') ||
      raw.startsWith('+++ ') ||
      raw.startsWith('new file mode ') ||
      raw.startsWith('deleted file mode ')
    ) {
      continue
    }
    if (raw.startsWith('Binary files ')) {
      mk('meta', null, null, '', raw)
      continue
    }
    if (raw.startsWith('+')) {
      const lineNo = newCursor > 0 ? newCursor : null
      if (newCursor > 0) newCursor += 1
      mk('add', null, lineNo, '+', raw.slice(1))
      continue
    }
    if (raw.startsWith('-')) {
      const lineNo = oldCursor > 0 ? oldCursor : null
      if (oldCursor > 0) oldCursor += 1
      mk('del', lineNo, null, '-', raw.slice(1))
      continue
    }
    if (raw.startsWith(' ')) {
      const oldLine = oldCursor > 0 ? oldCursor : null
      const newLine = newCursor > 0 ? newCursor : null
      if (oldCursor > 0) oldCursor += 1
      if (newCursor > 0) newCursor += 1
      mk('ctx', oldLine, newLine, ' ', raw.slice(1))
      continue
    }
    mk('meta', null, null, '', raw)
  }
  return out
})

function normalizeProjectKey(name: string): string {
  return String(name || '').trim().toLowerCase()
}

function projectNameFromRepoUrl(repoUrl: string): string {
  const raw = String(repoUrl || '').trim()
  if (!raw) return ''
  const parts = raw.split('/').filter(Boolean)
  const last = parts[parts.length - 1] || ''
  return last.replace(/\.git$/i, '')
}

function getPublishHistoryForProject(projectName: string): any[] {
  const k = normalizeProjectKey(projectName)
  if (!k) return []
  const list = Array.isArray(importTasks.value) ? importTasks.value : []
  return list
    .filter((t) => {
      const p1 = normalizeProjectKey(String(t?.project_name || ''))
      const p2 = normalizeProjectKey(projectNameFromRepoUrl(String(t?.repo_url || '')))
      return p1 === k || p2 === k
    })
    .sort((a, b) => String(b?.created_at || '').localeCompare(String(a?.created_at || '')))
}

function isPublishSuccess(status: any): boolean {
  const s = String(status || '').toLowerCase()
  return s === 'success' || s === 'succeeded' || s === 'done'
}

function getLatestSuccessfulTaskForProject(projectName: string): any | null {
  const rows = getPublishHistoryForProject(projectName)
  const hit = rows.find((x) => isPublishSuccess(x?.status))
  return hit || (rows.length ? rows[0] : null)
}

// 发布差异预览：始终对比本地仓库 vs 用户选择的 ref
const selectedProjectBaseRef = computed(() => {
  // 上次发布：固定使用本地仓库的默认分支（main/master）
  return 'HEAD'
})

const selectedProjectBaseRefLabel = computed(() => {
  // 显示为"本地仓库最新"
  const repoName = String(selectedProjectName.value || '').trim()
  return repoName ? `本地仓库最新 (${repoName})` : '本地仓库最新'
})

const selectedProjectTargetRef = computed(() => {
  // 本次发布：用户选择的 branch 或 commitId
  const v = refType.value === 'commit' ? commitId.value : branch.value
  return String(v || '').trim()
})

const canLoadImportDiff = computed(() => {
  const repo = String(readonlyUrl.value || '').trim()
  const fromRef = selectedProjectBaseRef.value
  const toRef = selectedProjectTargetRef.value
  // 只需要本地仓库和本次发布的 ref 即可对比
  return Boolean(repo && toRef)
})

function selectPublishHistoryTask(record: any) {
  publishRecordTask.value = record || null
}

function openPublishRecord(record: any) {
  const projectName = String(record?.project_name || '')
  const history = getPublishHistoryForProject(projectName)
  if (!history.length) {
    message.info('该项目暂无导入/发布记录')
    return
  }
  publishRecordProjectName.value = projectName
  publishRecordHistory.value = history
  publishRecordTask.value = history[0]
  publishRecordModalOpen.value = true
}

function boolLabel(v: any): string {
  return Boolean(v) ? '启用' : '关闭'
}

function resetDiffPreview() {
  diffError.value = null
  diffSummary.value = null
  diffFilePatch.value = ''
  diffFilePath.value = ''
}

async function loadImportDiffPreview() {
  const repoUrl = String(readonlyUrl.value || '').trim()
  const fromRef = selectedProjectBaseRef.value
  const toRef = selectedProjectTargetRef.value
  diffSummaryModalOpen.value = true
  if (!repoUrl || !fromRef || !toRef) {
    diffError.value = '请先确保已选应用，且有"上次发布 Ref"和"本次 Ref"'
    return
  }
  // 注意：即使分支名相同（如都是 release/2.0.5），远程 commit 也可能已更新，所以仍需对比
  diffLoading.value = true
  diffError.value = null
  diffSummary.value = null
  try {
    const data = await getGitDiffSummary({
      repo_url: repoUrl,
      from_ref: fromRef,
      to_ref: toRef,
      max_files: 1200,
      project_name: String(selectedProjectName.value || '').trim() || undefined,
    })
    if (!data?.ok) {
      diffError.value = data?.message || '加载差异失败'
      return
    }
    diffSummary.value = data
  } catch (e: any) {
    diffError.value = e?.message ?? String(e)
  } finally {
    diffLoading.value = false
  }
}

async function openDiffFilePatch(fileRecord: any) {
  const repoUrl = String(readonlyUrl.value || '').trim()
  const fromRef = selectedProjectBaseRef.value
  const toRef = selectedProjectTargetRef.value
  const filePath = String(fileRecord?.path || '').trim()
  if (!repoUrl || !fromRef || !toRef || !filePath) return
  diffFileModalOpen.value = true
  diffFileLoading.value = true
  diffFilePath.value = filePath
  diffFilePatch.value = ''
  try {
    const data = await getGitDiffFile({
      repo_url: repoUrl,
      from_ref: fromRef,
      to_ref: toRef,
      file_path: filePath,
      context: 3,
      max_lines: 2000,
      project_name: String(selectedProjectName.value || '').trim() || undefined,
    })
    if (!data?.ok) {
      diffFilePatch.value = `读取文件差异失败：${data?.message || '-'}`
      return
    }
    const body = String(data.patch || '').trim()
    const tail = data.truncated ? '\n\n... [已截断，差异过长]' : ''
    diffFilePatch.value = body ? `${body}${tail}` : '(该文件无文本差异或为二进制文件)'
  } catch (e: any) {
    diffFilePatch.value = e?.message ?? String(e)
  } finally {
    diffFileLoading.value = false
  }
}

const logModalOpen = ref(false)
const logModalTaskId = ref<string | null>(null)
const logOffset = ref(0)
const logLines = ref<string[]>([])
const logLoading = ref(false)
const logHasMore = ref(true)
const logStickToBottom = ref(true)
let _programmaticScroll = false
const logViewEl = ref<HTMLElement | null>(null)
let importPollTimer: number | undefined

let lastImportStatsText = ''
let lastBlockedState = false

watch(importStats, (v) => {
  if (!v) {
    lastImportStatsText = ''
    lastBlockedState = false
    return
  }
  const blocked = Boolean(v.auto_submission_blocked)
  if (blocked && !lastBlockedState) {
    const reason = String(v.auto_submission_block_reason || '验收失败，已阻断后续 auto 任务')
    message.error({ content: reason, duration: 6 })
  }
  lastBlockedState = blocked

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

// 判断发布记录对应的应用是否为 Java 后端应用
function isPublishRecordJavaApp(task: any): boolean {
  const projectName = String(task?.project_name || '')
  const meta = repoState.repos.value.find((r: any) => r.name === projectName)
  if (!meta) return false
  return meta.appType === 'backend' && meta.language === 'java'
}

const refType = ref<'branch' | 'commit'>('branch')
const appVersion = ref<string>('')  // 版本号，如 2.0.5、release/2.0.5

watch(
  () => selectedProjectName.value,
  () => {
    // 导入/重建默认走 Branch，不根据历史缓存自动切到 Commit
    refType.value = 'branch'
    appVersion.value = ''
    resetDiffPreview()
  },
)

watch(
  [refType, branch, commitId, readonlyUrl],
  () => {
    resetDiffPreview()
  },
)

const importSubmitEnabled = computed(() => {
  if (!canRun.value) return false
  if (!String(appVersion.value || '').trim()) return false
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
      project_name: String(selectedProjectName.value || '').trim(),
      maven_scan_enabled: Boolean(importForm.value.maven_scan_enabled),
      force_maven: Boolean(importForm.value.force_maven),
      clear_database: Boolean(importForm.value.clear_database),
      auto_link_external: Boolean(importForm.value.auto_link_external),
      task_type: 'auto',
      acceptance_enabled: Boolean(importForm.value.acceptance_enabled),
      acceptance_block_on_fail: Boolean(importForm.value.acceptance_block_on_fail),
      acceptance_max_drop_ratio: Number(importForm.value.acceptance_max_drop_ratio || 0.3),
    }

    if (refType.value === 'commit') payload.commit_id = String(commitId.value || '').trim()
    else payload.branch = String(branch.value || '').trim()

    const v = String(appVersion.value || '').trim()
    if (!v) {
      message.error({ content: '版本号不能为空', duration: 4 })
      return
    }
    payload.app_version = v

    const data = await createImportTask(payload)
    if (!data?.ok) throw new Error(String((data as any)?.message || '提交失败'))
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

async function unblockAutoTasks() {
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '确认解除阻断？',
    content: '仅当你已确认“验收失败原因已处理”时再执行。确认后还需输入口令二次校验。',
    okText: '继续',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      const token = String(window.prompt('二次确认：请输入 UNBLOCK 以解除后续 auto 任务阻断', '') || '').trim().toUpperCase()
      if (token !== 'UNBLOCK') {
        message.warning('口令不正确，已取消解除阻断')
        return
      }
      unblockingAutoTasks.value = true
      try {
        const data = await unblockAutoImportTasks('manual-confirm')
        if (!data?.ok) throw new Error(String((data as any)?.message || '解除阻断失败'))
        message.success('已解除后续 auto 任务阻断，可继续提交导入任务')
        await fetchImportTasks()
      } catch (e: any) {
        message.error(e?.message ?? String(e))
      } finally {
        unblockingAutoTasks.value = false
      }
    },
  })
}

async function clearGraphWithConfirm() {
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '确认清理整个图谱？',
    content: '该操作会删除 Neo4j 中所有节点和关系，且不可恢复。通常用于图谱污染后重建。',
    okText: '确认清理',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      clearingGraph.value = true
      try {
        const data = await clearGraphDatabase('CLEAR_ALL')
        if (!data?.ok) {
          message.error(data?.message || '图谱清理失败')
          return
        }
        message.success(`图谱已清理（节点 ${data.before_nodes ?? 0}，关系 ${data.before_relationships ?? 0}）`)
        await fetchProjects()
        await fetchDiagnostics()
      } catch (e: any) {
        message.error(e?.message ?? String(e))
      } finally {
        clearingGraph.value = false
      }
    },
  })
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
      _programmaticScroll = true
      await nextTick()
      const el = logViewEl.value
      if (el) el.scrollTop = el.scrollHeight
      // 短暂延迟后重置标志，避免 scroll 事件误判
      setTimeout(() => { _programmaticScroll = false }, 50)
    }
  }
}

function selectTask(id: string) {
  selectedTaskId.value = id
}

async function openAcceptanceDetail(task: any) {
  acceptanceDetailTask.value = task || null
  acceptanceDetailData.value = null
  acceptanceDetailModalOpen.value = true
  const tid = String(task?.task_id || '').trim()
  if (!tid) return
  acceptanceDetailLoading.value = true
  try {
    const data = await getImportTaskAcceptanceDetail(tid)
    acceptanceDetailData.value = data?.detail || null
  } catch (e: any) {
    message.warning(e?.message ?? String(e))
  } finally {
    acceptanceDetailLoading.value = false
  }
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
  // 忽略程序触发的滚动，只响应用户手动滚动
  if (_programmaticScroll) return
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

const tableData = computed(() => {
  // 按 project_name 分组，每个版本一行，公共列（项目/类型/上次发布）做 rowspan
  const grouped = new Map<string, GraphProjectItem[]>()
  for (const x of items.value) {
    const key = `${x.project_name}||${x.project_type || ''}`
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key)!.push(x)
  }
  const rows: any[] = []
  for (const [, group] of grouped) {
    group.forEach((x, idx) => {
      rows.push({
        key: getProjectRowKey(x),
        ...x,
        _groupSize: group.length,
        _groupIdx: idx,
      })
    })
  }
  return rows
})

const columns = [
  { title: '项目', dataIndex: 'project_name', key: 'project_name', width: 200, ellipsis: true,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
  { title: '类型', dataIndex: 'project_type', key: 'project_type', width: 100,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
  { title: '版本', dataIndex: 'version', key: 'version', width: 120, ellipsis: true },
  { title: '节点数', dataIndex: 'node_count', key: 'node_count', width: 100 },
  { title: '关系数', dataIndex: 'relationship_count', key: 'relationship_count', width: 100 },
  { title: '更新时间', dataIndex: 'last_update_time', key: 'last_update_time', width: 190, ellipsis: true },
  { title: '上次发布记录', key: 'release_record', width: 130,
    customCell: (record: any) => ({ rowSpan: record._groupIdx === 0 ? record._groupSize : 0 }) },
] as const
</script>

<template>
  <div class="graph-page">
    <a-card title="代码图谱" class="panel">
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
                  <template v-else-if="column.key === 'release_record'">
                    <a-button type="link" size="small" @click.stop="openPublishRecord(record)">
                      <template #icon><EyeOutlined /></template>
                      查看
                    </a-button>
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

                  <a-form-item label="版本号" required>
                    <a-input
                      v-model:value="appVersion"
                      placeholder="如 2.0.5 或 release/2.0.5，用于图谱版本标记"
                      allow-clear
                    />
                  </a-form-item>

                  <div class="import-diff-card">
                    <div class="import-diff-head">
                      <div class="import-diff-title">发布差异预览</div>
                      <a-button type="primary" size="small" :loading="diffLoading" :disabled="!canLoadImportDiff" @click="loadImportDiffPreview">
                        查看前后代码差异
                      </a-button>
                    </div>
                    <div class="import-diff-meta">
                      <div class="import-diff-meta-row">
                        <span class="import-diff-meta-label">上次发布：</span>
                        <span class="mono">{{ selectedProjectBaseRefLabel }}</span>
                      </div>
                      <div class="import-diff-meta-row">
                        <span class="import-diff-meta-label">本次发布：</span>
                        <span class="mono">{{ refType === 'commit' ? `CommitId: ${selectedProjectTargetRef || '-'}` : `Branch: ${selectedProjectTargetRef || '-'}` }}</span>
                      </div>
                    </div>
                  </div>

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
                  <a-alert
                    type="info"
                    show-icon
                    style="margin: 8px 0 10px;"
                    message="新手提示：建议保持“导入验收闭环”开启。系统会自动对比导入前后指标，帮助你快速判断这次导入是否可信。"
                    description="防呆增强已启用：即使关闭验收判定，系统也会保留前后 snapshot 与 delta，便于事后回溯。开启后会额外给出通过/失败结论与阻断能力。"
                  />
                  <a-form-item
                    label="导入验收判定（前后 snapshot + delta）"
                    style="margin-top: 8px;"
                    extra="关闭时：仍会采集快照并展示 delta（仅不做通过/失败判定）；开启时：会输出验收结论，并可联动阻断后续 auto 任务。"
                  >
                    <a-switch v-model:checked="importForm.acceptance_enabled" />
                  </a-form-item>
                  <a-form-item
                    label="验收失败时阻断后续 auto 任务（可选）"
                    extra="开启后：若本次验收失败，系统会暂停后续自动任务，防止坏数据连续写入；可在右侧任务区手动解除。"
                  >
                    <a-switch
                      v-model:checked="importForm.acceptance_block_on_fail"
                      :disabled="!importForm.acceptance_enabled"
                    />
                  </a-form-item>
                  <a-form-item
                    label="最大允许降幅（非 clear_database 场景）"
                    extra="例如 30%：表示导入后节点/关系相比导入前最多允许下降 30%。下降过大通常意味着解析异常或数据丢失风险。"
                  >
                    <a-input-number
                      v-model:value="importForm.acceptance_max_drop_ratio"
                      :min="0"
                      :max="0.95"
                      :step="0.05"
                      :precision="2"
                      style="width: 180px;"
                      :disabled="!importForm.acceptance_enabled"
                    />
                    <span class="mono" style="margin-left: 8px;">{{ (Number(importForm.acceptance_max_drop_ratio || 0) * 100).toFixed(0) }}%</span>
                  </a-form-item>

                  </a-form>
                </div>

                <div class="import-form-footer">
                  <a-space>
                    <a-button danger :loading="clearingGraph" @click="clearGraphWithConfirm">
                      清理图谱（危险）
                    </a-button>
                    <a-button type="primary" :loading="importSubmitting" :disabled="!importSubmitEnabled" @click="submitImport">
                      提交导入（并发=1，自动排队）
                    </a-button>
                  </a-space>
                </div>
              </div>
            </a-col>

            <a-col :xs="24" :lg="14">
              <a-card size="small" title="任务队列与日志">
                <!-- 并发信息由 watch(importStats) 使用 message 提示 -->
                <a-alert
                  v-if="importStats?.auto_submission_blocked"
                  type="error"
                  show-icon
                  style="margin-bottom: 10px;"
                  :message="String(importStats?.auto_submission_block_reason || '验收失败，后续 auto 任务已阻断')"
                >
                  <template #description>
                    <a-space>
                      <span>系统已暂停后续自动任务，避免异常数据继续写入。确认问题已处理后，可手动解除阻断。</span>
                      <a-button size="small" :loading="unblockingAutoTasks" @click="unblockAutoTasks">手动解除阻断</a-button>
                    </a-space>
                  </template>
                </a-alert>

                <div class="import-tasks-table-wrap">
                <a-table
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

                <div style="height: 10px" />
                <a-modal
                  v-model:open="logModalOpen"
                  :title="`任务日志：${logModalTaskId || '-'}`"
                  width="1180px"
                  :footer="null"
                  :body-style="{ padding: '0', height: 'calc(70vh)', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
                  @cancel="closeLogModal"
                >
                  <div class="logbox logbox--modal">
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
                <a-modal
                  v-model:open="acceptanceDetailModalOpen"
                  :title="`验收详情：${acceptanceDetailTask?.task_id || '-'}`"
                  width="1180px"
                  :footer="null"
                  :body-style="{ maxHeight: '72vh', overflow: 'auto', padding: '12px' }"
                >
                  <a-alert
                    type="info"
                    show-icon
                    style="margin-bottom: 10px;"
                    :message="`点击验收可查看本次导入实体变化明细（新增/删除/变更）`"
                  />
                  <div v-if="acceptanceDetailLoading" style="padding: 36px 0; text-align: center;">
                    <a-spin />
                    <div style="margin-top: 8px; color: rgba(0,0,0,.45)">正在从 SQLite 加载验收明细...</div>
                  </div>
                  <template v-else-if="acceptanceDetail(acceptanceDetailTask)">
                    <a-row :gutter="10" style="margin-bottom: 12px;">
                      <a-col :span="6"><a-statistic title="新增节点" :value="acceptanceDetail(acceptanceDetailTask)?.added_nodes?.total || 0" /></a-col>
                      <a-col :span="6"><a-statistic title="新增关系" :value="acceptanceDetail(acceptanceDetailTask)?.added_relationships?.total || 0" /></a-col>
                      <a-col :span="6"><a-statistic title="删除节点" :value="acceptanceDetail(acceptanceDetailTask)?.deleted_nodes?.total || 0" /></a-col>
                      <a-col :span="6"><a-statistic title="删除关系" :value="acceptanceDetail(acceptanceDetailTask)?.deleted_relationships?.total || 0" /></a-col>
                    </a-row>
                    <a-card size="small" title="类型统计（你关心的重点）" style="margin-bottom: 12px;">
                      <div style="margin-bottom: 10px;">
                        <b>节点类型变化：</b>
                        <div class="accept-chart" style="margin-top: 6px;">
                          <div
                            v-for="row in topTypeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).nodeByLabel || {}, 10)"
                            :key="`node-chart-${row.key}`"
                            class="accept-chart-row"
                          >
                            <div class="accept-chart-name mono">{{ row.type }}</div>
                            <div class="accept-chart-track">
                              <div
                                v-if="row.added > 0"
                                class="accept-chart-segment accept-chart-segment--add"
                                :style="{ width: `${row.addPercent}%` }"
                              />
                              <div
                                v-if="row.deleted > 0"
                                class="accept-chart-segment accept-chart-segment--del"
                                :style="{ width: `${row.delPercent}%` }"
                              />
                            </div>
                            <div class="accept-chart-value mono">
                              <span class="accept-chart-plus">+{{ row.added }}</span>
                              <span style="margin: 0 6px; color: rgba(0,0,0,.35)">/</span>
                              <span class="accept-chart-minus">-{{ row.deleted }}</span>
                            </div>
                          </div>
                          <a-empty
                            v-if="topTypeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).nodeByLabel || {}, 10).length === 0"
                            description="暂无节点类型变化"
                          />
                        </div>
                      </div>
                      <div style="margin-bottom: 10px;">
                        <b>关系类型变化：</b>
                        <div class="accept-chart" style="margin-top: 6px;">
                          <div
                            v-for="row in topTypeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).relByType || {}, 10)"
                            :key="`rel-chart-${row.key}`"
                            class="accept-chart-row"
                          >
                            <div class="accept-chart-name mono">{{ row.type }}</div>
                            <div class="accept-chart-track">
                              <div
                                v-if="row.added > 0"
                                class="accept-chart-segment accept-chart-segment--add"
                                :style="{ width: `${row.addPercent}%` }"
                              />
                              <div
                                v-if="row.deleted > 0"
                                class="accept-chart-segment accept-chart-segment--del"
                                :style="{ width: `${row.delPercent}%` }"
                              />
                            </div>
                            <div class="accept-chart-value mono">
                              <span class="accept-chart-plus">+{{ row.added }}</span>
                              <span style="margin: 0 6px; color: rgba(0,0,0,.35)">/</span>
                              <span class="accept-chart-minus">-{{ row.deleted }}</span>
                            </div>
                          </div>
                          <a-empty
                            v-if="topTypeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).relByType || {}, 10).length === 0"
                            description="暂无关系类型变化"
                          />
                        </div>
                      </div>
                      <div style="margin-bottom: 10px;">
                        <a-collapse>
                          <a-collapse-panel key="type-table-details" header="查看类型变化明细表（可选）">
                            <a-row :gutter="10">
                              <a-col :span="12">
                                <a-table
                                  :data-source="typeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).nodeByLabel || {}) as any"
                                  :pagination="{ pageSize: 8 }"
                                  size="small"
                                  bordered
                                  :row-key="(r: any) => `node-${r.key}`"
                                >
                                  <a-table-column title="节点类型" data-index="type" key="type" />
                                  <a-table-column title="新增" key="added" :width="90">
                                    <template #default="{ record }">
                                      <a-tag v-if="record.added > 0" color="green">+{{ record.added }}</a-tag>
                                      <span v-else class="mono">0</span>
                                    </template>
                                  </a-table-column>
                                  <a-table-column title="删除" key="deleted" :width="90">
                                    <template #default="{ record }">
                                      <a-tag v-if="record.deleted > 0" color="red">-{{ record.deleted }}</a-tag>
                                      <span v-else class="mono">0</span>
                                    </template>
                                  </a-table-column>
                                </a-table>
                              </a-col>
                              <a-col :span="12">
                                <a-table
                                  :data-source="typeDeltaRows(acceptanceTypeStats(acceptanceDetailTask).relByType || {}) as any"
                                  :pagination="{ pageSize: 8 }"
                                  size="small"
                                  bordered
                                  :row-key="(r: any) => `rel-${r.key}`"
                                >
                                  <a-table-column title="关系类型" data-index="type" key="type" />
                                  <a-table-column title="新增" key="added" :width="90">
                                    <template #default="{ record }">
                                      <a-tag v-if="record.added > 0" color="green">+{{ record.added }}</a-tag>
                                      <span v-else class="mono">0</span>
                                    </template>
                                  </a-table-column>
                                  <a-table-column title="删除" key="deleted" :width="90">
                                    <template #default="{ record }">
                                      <a-tag v-if="record.deleted > 0" color="red">-{{ record.deleted }}</a-tag>
                                      <span v-else class="mono">0</span>
                                    </template>
                                  </a-table-column>
                                </a-table>
                              </a-col>
                            </a-row>
                          </a-collapse-panel>
                        </a-collapse>
                      </div>
                      <div>
                        <b>重建关系类型（changed 文件引起）：</b>
                        <span class="mono">{{ JSON.stringify(acceptanceTypeStats(acceptanceDetailTask).changedRelByType || {}, null, 2) }}</span>
                      </div>
                    </a-card>
                    <a-collapse style="margin-bottom: 12px;">
                      <a-collapse-panel key="file-changes">
                        <template #header>
                          <span>
                            文件变化
                            <span class="mono" style="margin-left: 8px; color: rgba(0,0,0,.55);">
                              (+{{ acceptanceFiles().added.length }} / ~{{ acceptanceFiles().changed.length }} / -{{ acceptanceFiles().deleted.length }})
                            </span>
                          </span>
                        </template>
                        <div style="margin-bottom:8px;">
                          <b>新增文件：</b>
                          <span class="mono">{{ acceptanceFiles().added.join(', ') || '-' }}</span>
                        </div>
                        <div style="margin-bottom:8px;">
                          <b>修改文件：</b>
                          <span class="mono">{{ acceptanceFiles().changed.join(', ') || '-' }}</span>
                        </div>
                        <div>
                          <b>删除文件：</b>
                          <span class="mono">{{ acceptanceFiles().deleted.join(', ') || '-' }}</span>
                        </div>
                      </a-collapse-panel>
                    </a-collapse>
                    <a-collapse style="margin-bottom: 12px;">
                      <a-collapse-panel key="detail-samples">
                        <template #header>
                          <span>
                            详细样本（可选）
                            <span class="mono" style="margin-left: 8px; color: rgba(0,0,0,.55);">
                              节点样本 {{ (acceptanceDetail(acceptanceDetailTask)?.added_nodes?.samples || []).length }} 条，
                              关系样本 {{ (acceptanceDetail(acceptanceDetailTask)?.added_relationships?.samples || []).length }} 条
                            </span>
                          </span>
                        </template>
                        <a-card size="small" title="新增节点（示例）" style="margin-bottom: 12px;">
                          <a-table
                            :data-source="(acceptanceDetail(acceptanceDetailTask)?.added_nodes?.samples || []) as any"
                            :pagination="{ pageSize: 8 }"
                            size="small"
                            bordered
                            :scroll="{ x: 980 }"
                            :row-key="(r: any, i: number) => `${r.symbol_id || ''}-${i}`"
                          >
                            <a-table-column title="类型" data-index="label" key="label" :width="140" ellipsis />
                            <a-table-column title="展示名" data-index="display" key="display" ellipsis />
                            <a-table-column title="symbol_id" data-index="symbol_id" key="symbol_id" :width="300" ellipsis />
                            <a-table-column title="文件" data-index="file_path" key="file_path" :width="220" ellipsis />
                          </a-table>
                        </a-card>
                        <a-card size="small" title="新增关系（示例）" style="margin-bottom: 12px;">
                          <a-table
                            :data-source="(acceptanceDetail(acceptanceDetailTask)?.added_relationships?.samples || []) as any"
                            :pagination="{ pageSize: 8 }"
                            size="small"
                            bordered
                            :scroll="{ x: 980 }"
                            :row-key="(r: any, i: number) => `${r.type || ''}-${r.source_id || ''}-${r.target_id || ''}-${i}`"
                          >
                            <a-table-column title="关系类型" data-index="type" key="type" :width="140" ellipsis />
                            <a-table-column title="source" data-index="source_id" key="source_id" :width="320" ellipsis />
                            <a-table-column title="target" data-index="target_id" key="target_id" :width="320" ellipsis />
                          </a-table>
                        </a-card>
                        <a-card size="small" title="删除明细（按文件）" style="margin-bottom: 12px;">
                          <a-table
                            :data-source="(acceptanceDetail(acceptanceDetailTask)?.deleted_nodes?.by_file || []) as any"
                            :pagination="{ pageSize: 8 }"
                            size="small"
                            bordered
                            :row-key="(r: any, i: number) => `${r.file || ''}-${r.reason || ''}-${i}`"
                          >
                            <a-table-column title="文件" data-index="file" key="file" ellipsis />
                            <a-table-column title="删除节点数" data-index="count" key="count" :width="120" />
                            <a-table-column title="原因" data-index="reason" key="reason" :width="140" ellipsis />
                          </a-table>
                        </a-card>
                      </a-collapse-panel>
                    </a-collapse>
                  </template>
                  <a-empty v-else description="暂无验收详情" />
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
    <a-modal
      v-model:open="publishRecordModalOpen"
      title="发布历史记录"
      :footer="null"
      width="980px"
      @cancel="publishRecordModalOpen = false"
    >
      <template v-if="publishRecordHistory.length">
        <a-alert
          type="info"
          show-icon
          style="margin-bottom: 12px;"
          :message="`项目：${publishRecordProjectName || '-'}，共 ${publishRecordHistory.length} 条发布记录`"
        />
        <a-table
          :data-source="publishRecordHistory as any"
          :pagination="{ pageSize: 6, showSizeChanger: true }"
          size="small"
          bordered
          row-key="task_id"
          :row-class-name="(r: any) => (r === publishRecordTask ? 'row-selected' : '')"
          :custom-row="(record: any) => ({ onClick: () => selectPublishHistoryTask(record) })"
          :scroll="{ x: 980 }"
          style="margin-bottom: 12px;"
        >
          <a-table-column title="task_id" data-index="task_id" key="task_id" :width="110" ellipsis />
          <a-table-column title="状态" key="status" :width="90">
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
          <a-table-column title="Ref 类型" key="ref_type" :width="90">
            <template #default="{ record }">
              <span class="mono">{{ record.commit_id ? 'CommitId' : 'Branch' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="Ref" key="ref" :width="240" ellipsis>
            <template #default="{ record }">
              <span class="mono">{{ record.commit_id || record.branch || '-' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" key="created_at" :width="170">
            <template #default="{ record }">
              <span class="mono">{{ String(record.created_at || '').slice(0, 19).replace('T', ' ') || '-' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="完成时间" key="completed_at" :width="170">
            <template #default="{ record }">
              <span class="mono">{{ String(record.completed_at || '').slice(0, 19).replace('T', ' ') || '-' }}</span>
            </template>
          </a-table-column>
        </a-table>

        <a-descriptions v-if="publishRecordTask" bordered size="small" :column="2">
          <a-descriptions-item label="选中 task_id">
            <span class="mono">{{ publishRecordTask.task_id || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="项目">
            <span class="mono">{{ publishRecordTask.project_name || projectNameFromRepoUrl(publishRecordTask.repo_url || '') || '-' }}</span>
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
          <a-descriptions-item label="验收结论">
            <a-tag :color="acceptanceTag(publishRecordTask).color">{{ acceptanceTag(publishRecordTask).text }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="验收摘要">
            <span class="mono">{{ acceptanceSummary(publishRecordTask) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="快照 Delta" :span="2">
            <span class="mono">{{ acceptanceDeltaText(publishRecordTask) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="repo_url" :span="2">
            <span class="mono">{{ publishRecordTask.repo_url || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="Ref 类型">
            <span class="mono">{{ publishRecordTask.commit_id ? 'CommitId' : 'Branch' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="Ref">
            <span class="mono">{{ publishRecordTask.commit_id || publishRecordTask.branch || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="isPublishRecordJavaApp(publishRecordTask)" label="Maven 扫描">
            <span class="mono">{{ boolLabel(publishRecordTask.maven_scan_enabled) }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="isPublishRecordJavaApp(publishRecordTask)" label="强制 Maven">
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
      </template>
    </a-modal>
    <a-modal
      v-model:open="diffSummaryModalOpen"
      title="发布代码差异"
      :footer="null"
      width="1120px"
      :body-style="{ maxHeight: '72vh', overflow: 'auto', padding: '12px' }"
      @cancel="diffSummaryModalOpen = false"
    >
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 12px;"
        :message="`上次发布：${selectedProjectBaseRefLabel}；本次发布：${refType === 'commit' ? `CommitId: ${selectedProjectTargetRef || '-'}` : `Branch: ${selectedProjectTargetRef || '-'}`}`"
        :description="diffSummary?.same_commit ? '两个 ref 指向相同的 commit，无代码差异' : undefined"
      />
      <a-alert
        v-if="diffError"
        type="error"
        show-icon
        :message="diffError"
        style="margin: 10px 0;"
      />
      <template v-if="diffSummary">
        <a-row :gutter="8" style="margin: 8px 0 10px;">
          <a-col :span="8">
            <a-statistic title="变更文件数" :value="diffSummary.stats?.files || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="新增行" :value="diffSummary.stats?.additions || 0" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="删除行" :value="diffSummary.stats?.deletions || 0" />
          </a-col>
        </a-row>
        <a-table
          :loading="diffLoading"
          :data-source="(diffSummary.files || []) as any"
          :pagination="{ pageSize: 10, showSizeChanger: true }"
          size="small"
          bordered
          row-key="path"
          :scroll="{ x: 980 }"
        >
          <a-table-column title="状态" key="status" :width="82">
            <template #default="{ record }">
              <a-tag :color="record.status === 'A' ? 'green' : (record.status === 'D' ? 'red' : (record.status === 'R' ? 'blue' : 'gold'))">
                {{ record.status || '-' }}
              </a-tag>
            </template>
          </a-table-column>
          <a-table-column title="文件" key="path" ellipsis>
            <template #default="{ record }">
              <a-tooltip :title="record.old_path ? `${record.old_path} -> ${record.path}` : record.path">
                <span class="mono">{{ record.old_path ? `${record.old_path} -> ${record.path}` : record.path }}</span>
              </a-tooltip>
            </template>
          </a-table-column>
          <a-table-column title="+行" key="additions" :width="82">
            <template #default="{ record }">
              <span class="mono" style="color:#237804;">{{ record.additions ?? 0 }}</span>
            </template>
          </a-table-column>
          <a-table-column title="-行" key="deletions" :width="82">
            <template #default="{ record }">
              <span class="mono" style="color:#cf1322;">{{ record.deletions ?? 0 }}</span>
            </template>
          </a-table-column>
          <a-table-column title="操作" key="ops" :width="90">
            <template #default="{ record }">
              <a-button type="link" size="small" @click.stop="openDiffFilePatch(record)">代码差异</a-button>
            </template>
          </a-table-column>
        </a-table>
      </template>
      <a-spin v-else-if="diffLoading" />
    </a-modal>
    <a-modal
      v-model:open="diffFileModalOpen"
      :title="`文件差异：${diffFilePath || '-'}`"
      :footer="null"
      width="1100px"
      :body-style="{ height: '68vh', overflow: 'hidden', padding: '12px' }"
      @cancel="diffFileModalOpen = false"
    >
      <div class="file-diff-box">
        <div v-if="diffFileLoading" class="log-loading">
          <a-spin size="large" />
          <div style="margin-top: 10px; color: rgba(0,0,0,.45);">正在加载文件差异...</div>
        </div>
        <template v-else>
          <div class="file-diff-legend">
            <a-space size="small">
              <a-tag color="green">+ 新增</a-tag>
              <a-tag color="red">- 删除</a-tag>
              <a-tag color="blue">@@ 区块</a-tag>
            </a-space>
          </div>
          <div class="file-diff-lines">
            <div
              v-for="row in diffRenderLines"
              :key="row.key"
              class="file-diff-line"
              :class="[
                row.kind === 'add' ? 'file-diff-line--add' : '',
                row.kind === 'del' ? 'file-diff-line--del' : '',
                row.kind === 'hunk' ? 'file-diff-line--hunk' : '',
                row.kind === 'meta' ? 'file-diff-line--meta' : '',
              ]"
            >
              <span class="file-diff-ln">{{ row.oldLine ?? '' }}</span>
              <span class="file-diff-ln">{{ row.newLine ?? '' }}</span>
              <span class="file-diff-prefix">{{ row.prefix }}</span>
              <code class="file-diff-code hljs" v-html="row.html" />
            </div>
          </div>
        </template>
      </div>
    </a-modal>
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
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 8px;
  overflow: hidden;
}
.logbox--modal {
  flex: 1;
  min-height: 0;
  margin: 12px;
}
.logbox-hd {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: rgba(0,0,0,.02);
  border-bottom: 1px solid rgba(0,0,0,.08);
}
.log {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.28) transparent;
  background: rgba(0,0,0,.03);
  font-size: 12px;
  line-height: 1.5;
}
.log::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.log::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: content-box;
}
.log::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.35);
  background-clip: content-box;
}
.log::-webkit-scrollbar-track {
  background: transparent;
}
.log-pre {
  margin: 0;
  padding: 0;
  white-space: pre;
  word-break: normal;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
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

.task-type-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
:deep(.task-type-tags .ant-tag) {
  margin-inline-end: 0;
}

.import-diff-card {
  margin: 12px 0 4px;
  padding: 10px;
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 8px;
  background: rgba(0,0,0,.012);
}
.import-diff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.import-diff-title {
  font-weight: 600;
  color: rgba(0,0,0,.85);
}
.import-diff-meta {
  margin-top: 8px;
}
.import-diff-meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 2px 0;
}
.import-diff-meta-label {
  min-width: 72px;
  color: rgba(0,0,0,.65);
}

.file-diff-box {
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 8px;
  height: calc(68vh - 8px);
  overflow: auto;
  background: #111;
}
.file-diff-legend {
  position: sticky;
  top: 0;
  z-index: 2;
  padding: 8px 10px;
  border-bottom: 1px solid rgba(255,255,255,.08);
  background: #151821;
}
.file-diff-lines {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}
.file-diff-line {
  display: grid;
  grid-template-columns: 64px 64px 24px 1fr;
  align-items: stretch;
}
.file-diff-line:hover {
  background: rgba(255,255,255,.04);
}
.file-diff-line--add {
  background: rgba(18, 133, 76, 0.24);
}
.file-diff-line--del {
  background: rgba(170, 46, 37, 0.28);
}
.file-diff-line--hunk {
  background: rgba(22, 119, 255, 0.24);
}
.file-diff-line--meta {
  background: rgba(255,255,255,.06);
}
.file-diff-ln {
  padding: 2px 8px;
  text-align: right;
  color: rgba(255,255,255,.58);
  border-right: 1px solid rgba(255,255,255,.08);
  user-select: none;
}
.file-diff-prefix {
  padding: 2px 6px;
  text-align: center;
  color: rgba(255,255,255,.72);
  border-right: 1px solid rgba(255,255,255,.08);
  user-select: none;
}
.file-diff-code {
  margin: 0;
  padding: 2px 10px;
  color: #e6edf3;
  white-space: pre;
  overflow-x: auto;
  background: transparent !important;
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

.accept-chart {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 8px 10px;
  background: rgba(0, 0, 0, 0.01);
}
.accept-chart-row {
  display: grid;
  grid-template-columns: minmax(140px, 34%) 1fr 130px;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}
.accept-chart-name {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.accept-chart-track {
  height: 12px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
}
.accept-chart-segment {
  height: 100%;
}
.accept-chart-segment--add {
  background: linear-gradient(90deg, #52c41a 0%, #95de64 100%);
}
.accept-chart-segment--del {
  background: linear-gradient(90deg, #ff4d4f 0%, #ff7875 100%);
}
.accept-chart-value {
  text-align: right;
}
.accept-chart-plus {
  color: #237804;
}
.accept-chart-minus {
  color: #a8071a;
}

@media (max-width: 1320px) {
  .query-pane {
    grid-template-columns: 1fr;
  }
}

</style>


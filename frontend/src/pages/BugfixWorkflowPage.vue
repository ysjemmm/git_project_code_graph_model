<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { BugOutlined, LoadingOutlined, MessageOutlined, ReloadOutlined, FullscreenOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { runForwardProcessflowNpeFixSse, type BugfixSseEvent, type ToolStep, listBugfixHistory, createBugfixHistory, updateBugfixHistoryStatus, type BugfixHistoryItem } from '../api'
import { gitApplyAndCommit } from '../api'
// import { runMockExecution } from '../api/mockExecution'  // 已废弃，使用真实接口
import { listOnlineBugs, searchOnlineBugs, type ForwardBugItem } from '../api/invoke'
import { parseModelChoice } from '../constants'
import type { ChatMsg } from '../types'
import ChatPanel from '../components/ChatPanel.vue'
import DiffViewer from '../components/DiffViewer.vue'
import { useRepo } from '../composables/useRepo'
import { useUpload } from '../composables/useUpload'
import { modelOptions } from '../constants'

marked.setOptions({ breaks: true, gfm: true })
marked.use({
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const langAlias: Record<string, string> = { vue: 'typescript', svelte: 'typescript', html: 'xml' }
      const resolved = langAlias[lang ?? ''] ?? lang
      const language = resolved && hljs.getLanguage(resolved) ? resolved : 'plaintext'
      const highlighted = hljs.highlight(text, { language }).value
      return `<pre class="hljs-block"><code class="hljs language-${language}">${highlighted}</code></pre>`
    },
  },
})

function renderMarkdown(text: string): string {
  try { return marked.parse(text) as string } catch { return text }
}

// ── 从 AI 回复中提取分析摘要 ──
function extractAnalysisSummary(messages: ChatMsg[]): {
  rootCause: string
  involvedFiles: string[]
  fixSuggestions: Array<{
    id: number
    file: string
    lineRange: string
    problem: string
    solution: string
    selected: boolean
  }>
} {
  const fullText = messages
    .filter(m => m.role === 'ai')
    .map(m => m.content)
    .join('\n')
  
  // 提取根本原因（查找"根本原因"、"root cause"、"原因"等关键词后的内容）
  let rootCause = '未识别到根本原因'
  const causePatterns = [
    /根本原因[：:]?\s*([^\n。.]+)/i,
    /root\s*cause[：:]?\s*([^\n。.]+)/i,
    /[原因因][：:]?\s*([^\n。.]+)/i,
    /问题在于[：:]?\s*([^\n。.]+)/i,
  ]
  for (const pattern of causePatterns) {
    const match = fullText.match(pattern)
    if (match?.[1]?.trim()) {
      rootCause = match[1].trim()
      break
    }
  }
  
  // 提取涉及文件（查找文件名模式 .java/.js/.py 等）
  const filePattern = /(?:\b|^)([\w/\\\-]+?\.(?:java|js|ts|py|go|cpp|c|h|xml|yaml|yml|json|sql))(?:\b|$)/gi
  const files = Array.from(new Set(
    Array.from(fullText.matchAll(filePattern)).map(m => m[1])
  )).slice(0, 5) // 最多取前5个
  
  // 提取修复建议（解析结构化格式：编号 | 文件 | 行号 | 问题 → 解决方案）
  const fixSuggestions: Array<{
    id: number
    file: string
    lineRange: string
    problem: string
    solution: string
    selected: boolean
  }> = []
  
  // 匹配格式：数字 | 文件名 | 行号 | 问题描述 → 解决方案
  const suggestionPattern = /(\d+)\s*\|\s*'([^']+?)'\s*\|\s*([\d\-–]+)\s*\|\s*([^→]+?)\s*→\s*(.+?)(?=\n\d+\s*\||$)/gs
  let match
  while ((match = suggestionPattern.exec(fullText)) !== null) {
    fixSuggestions.push({
      id: parseInt(match[1]),
      file: match[2].trim(),
      lineRange: match[3].trim(),
      problem: match[4].trim(),
      solution: match[5].trim(),
      selected: false  // 默认都不选中
    })
  }
  
  // 如果没匹配到结构化格式，尝试其他方式提取
  if (fixSuggestions.length === 0) {
    const fallbackPatterns = [
      /修复建议[：:]?\s*((?:(?!##|\n\s*\n).)*)/is,
      /解决方案[：:]?\s*((?:(?!##|\n\s*\n).)*)/is,
      /suggestions?[：:]?\s*((?:(?!##|\n\s*\n).)*)/is,
      /fix.*?[：:]?\s*((?:(?!##|\n\s*\n).)*)/is,
    ]
    const suggestionSections: string[] = []
    for (const pattern of fallbackPatterns) {
      const match = fullText.match(pattern)
      if (match?.[1]?.trim()) {
        suggestionSections.push(match[1].trim())
      }
    }
    
    // 转换为统一格式
    suggestionSections.slice(0, 3).forEach((text, idx) => {
      fixSuggestions.push({
        id: idx + 1,
        file: '未知文件',
        lineRange: '未知行号',
        problem: '未识别问题',
        solution: text,
        selected: false
      })
    })
  }
  
  // 如果还是空的，提供默认项
  if (fixSuggestions.length === 0) {
    fixSuggestions.push({
      id: 1,
      file: '未识别到相关文件',
      lineRange: '',
      problem: '未识别具体问题',
      solution: '未识别到具体修复建议',
      selected: false
    })
  }
  
  return {
    rootCause,
    involvedFiles: files.length > 0 ? files : ['未识别到相关文件'],
    fixSuggestions,
  }
}

// ─── 节点定义（响应式，支持拖拽改位置）───────────────────────────────────────
interface NodeDef {
  id: string
  title: string
  desc: string
  color: string
  x: number
  y: number
}

const nodes = ref<NodeDef[]>([
  { id: 'project',  title: '项目配置',  desc: '选择仓库 · 关联 Bug · 上传附件', color: '#4a9eff', x: 60,   y: 200 },
  { id: 'ai',       title: 'AI 分析',  desc: '配置模型与问题描述',               color: '#722ed1', x: 380,  y: 200 },
  { id: 'confirm',  title: '分析确认',  desc: '人工审查结果',                    color: '#fa8c16', x: 700,  y: 80  },
  { id: 'fix',      title: '修复执行',  desc: '建分支 · 提交 · CR',             color: '#f5222d', x: 1020, y: 80  },
  { id: 'error',    title: '异常结束',  desc: '分析失败或无法获取 Bug 信息',      color: '#8c8c8c', x: 700,  y: 340 },
])

const NODE_W = 220
const NODE_H = 120
const PIN_R  = 7

// ─── 画布变换：平移 + 缩放 ────────────────────────────────────────────────────
const canvasRef = ref<HTMLElement | null>(null)
const pageRef   = ref<HTMLElement | null>(null)
const panX  = ref(0)
const panY  = ref(0)
const scale = ref(1)

// 平移状态
let isPanning    = false
let panStartX    = 0
let panStartY    = 0
let panStartPanX = 0
let panStartPanY = 0
let canvasDragDist = 0  // 画布拖拽位移，用于区分点击与拖动

// 节点拖拽状态
let draggingId: string | null = null
let dragStartClientX = 0
let dragStartClientY = 0
let dragStartNodeX   = 0
let dragStartNodeY   = 0
let didDrag = false  // 区分拖拽和点击

// ── 滚轮缩放（以鼠标为中心）──
function onWheel(e: WheelEvent) {
  e.preventDefault()
  const factor   = e.deltaY < 0 ? 1.1 : 0.9
  const newScale = Math.min(Math.max(scale.value * factor, 0.15), 3)
  const rect     = canvasRef.value!.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  // 以鼠标位置为缩放中心重新计算平移
  panX.value = mx - (mx - panX.value) * (newScale / scale.value)
  panY.value = my - (my - panY.value) * (newScale / scale.value)
  scale.value = newScale
}

// ── 节点拖拽开始 ──
function onNodePointerDown(e: PointerEvent, id: string) {
  e.stopPropagation()
  draggingId       = id
  dragStartClientX = e.clientX
  dragStartClientY = e.clientY
  const n          = nodes.value.find(n => n.id === id)!
  dragStartNodeX   = n.x
  dragStartNodeY   = n.y
  didDrag          = false
  canvasRef.value?.setPointerCapture(e.pointerId)
}

// ── 画布空白区域拖拽平移 ──
function onCanvasPointerDown(e: PointerEvent) {
  if ((e.target as HTMLElement).closest('.bp-node')) return
  isPanning      = true
  canvasDragDist = 0
  panStartX      = e.clientX
  panStartY      = e.clientY
  panStartPanX   = panX.value
  panStartPanY   = panY.value
  canvasRef.value?.setPointerCapture(e.pointerId)
}

// ── 统一移动处理 ──
function onPointerMove(e: PointerEvent) {
  if (draggingId) {
    const dx = (e.clientX - dragStartClientX) / scale.value
    const dy = (e.clientY - dragStartClientY) / scale.value
    if (Math.sqrt(dx * dx + dy * dy) > 3) didDrag = true
    const n = nodes.value.find(n => n.id === draggingId)!
    n.x = dragStartNodeX + dx
    n.y = dragStartNodeY + dy
    return
  }
  if (isPanning) {
    const dx = e.clientX - panStartX
    const dy = e.clientY - panStartY
    canvasDragDist = Math.sqrt(dx * dx + dy * dy)
    panX.value = panStartPanX + dx
    panY.value = panStartPanY + dy
  }
}

function onPointerUp() {
  // 拖拽距离小，判定为点击——仅 done/active 节点可开启配置面板
  if (draggingId && !didDrag) {
    if (nodeState(draggingId) !== 'pending') {
      selectNode(draggingId)
    }
  }
  // 画布空白处点击（非拖动）→ 关闭抽屉
  if (!draggingId && isPanning && canvasDragDist < 5) {
    drawerOpen.value = false
  }
  draggingId = null
  isPanning  = false
}

// ─── 连线（贝塞尔曲线）────────────────────────────────────────────────────────
function outPin(n: NodeDef) { return { x: n.x + NODE_W, y: n.y + NODE_H / 2 } }
function inPin(n: NodeDef)  { return { x: n.x,           y: n.y + NODE_H / 2 } }

// 手动定义边：{ from, to, isError? }
// isError 边只在 AI 失败/无结构化数据时激活
interface EdgeDef { from: string; to: string; isError?: boolean }
const EDGES: EdgeDef[] = [
  { from: 'project', to: 'ai' },
  { from: 'ai',      to: 'confirm' },               // 正常路径
  { from: 'ai',      to: 'error',  isError: true },  // 异常路径
  { from: 'confirm', to: 'fix' },
]

// AI 完成后才显示后续节点/边，且只显示对应路径
const visibleNodeIds = computed(() => {
  const aiDone   = aiExecutedSuccessfully.value
  const aiFailed = execFailed.value
  const hasStructured = !!nodeIO.value.ai.output.structuredResult
  const base = ['project', 'ai']
  if (!aiDone && !aiFailed) return base
  // 异常路径
  if (aiFailed || !hasStructured) return [...base, 'error']
  // 正常路径
  return [...base, 'confirm', 'fix']
})

const visibleEdgeIndices = computed(() => {
  const ids = visibleNodeIds.value
  return EDGES.map((e, i) => ids.includes(e.from) && ids.includes(e.to) ? i : -1).filter(i => i >= 0)
})

const connections = computed(() =>
  EDGES.map(({ from, to }, i) => {
    if (!visibleEdgeIndices.value.includes(i)) return null
    const src = nodes.value.find(n => n.id === from)!
    const tgt = nodes.value.find(n => n.id === to)!
    const out = outPin(src)
    const inp = inPin(tgt)
    const dx  = Math.abs(inp.x - out.x) * 0.5
    return `M${out.x},${out.y} C${out.x+dx},${out.y} ${inp.x-dx},${inp.y} ${inp.x},${inp.y}`
  })
)

function wireColor(i: number) {
  const edge = EDGES[i]
  if (!edge) return '#c4cfe6'
  const src = nodes.value.find(n => n.id === edge.from)!
  const tgt = nodes.value.find(n => n.id === edge.to)!
  if (activeWireIdx.value === i) return src.color
  const srcState = nodeState(src.id)
  const tgtState = nodeState(tgt.id)
  if (edge.isError) {
    // 异常边：AI 失败时高亮红色，否则灰色
    return nodeState('error') === 'active' ? '#ff4d4f' : '#e2e8f4'
  }
  if (srcState === 'done') return src.color
  if (tgtState === 'pending') return '#e2e8f4'
  return '#c4cfe6'
}

// ─── 连线点击 ───────────────────────────────────────────────────────────────
const activeWireIdx = ref<number | null>(null)

function onWireClick(i: number) {
  activeWireIdx.value = activeWireIdx.value === i ? null : i
}

// ─── 节点状态 ───────────────────────────────────────────────────────────────
function nodeState(id: string): 'done' | 'active' | 'pending' {
  const projDone = canRun.value && !!selectedBug.value
  const aiDone   = aiExecutedSuccessfully.value
  const aiFailed = execFailed.value
  const hasStructured = !!nodeIO.value.ai.output.structuredResult
  const confDone = aiDone && confirmDone.value

  if (id === 'project') return projDone ? 'done' : 'active'
  if (id === 'ai')      return aiDone   ? 'done' : (projDone ? 'active' : 'pending')
  // 正常路径：AI 完成且有结构化数据
  if (id === 'confirm') return confDone ? 'done' : (aiDone && hasStructured ? 'active' : 'pending')
  if (id === 'fix')     return fixDone.value ? 'done' : (confDone ? 'active' : 'pending')
  // 异常路径：AI 执行失败 或 AI 完成但无结构化数据（执行中时不触发）
  if (id === 'error')   return (!isExecuting.value && (aiFailed || (aiDone && !hasStructured))) ? 'active' : 'pending'
  return 'pending'
}
function nodeStatusText(id: string) {
  const s = nodeState(id)
  if (id === 'ai') {
    if (s === 'done') return '✓ 已完成'
    if (isExecuting.value) return '执行中…'
    if (s === 'active') return '待执行'
    return '未就绪'
  }
  if (id === 'error') {
    if (s === 'active') return execFailed.value ? '⚠ 执行失败' : '⚠ 无结构化数据'
    return '未触发'
  }
  return s === 'done' ? '✓ 已完成' : s === 'active' ? '待配置' : '未就绪'
}

// ─── 配置面板 ────────────────────────────────────────────────────────────────
const selectedNodeId = ref<string | null>(null)
const activeNodeId   = ref<string | null>(null)
const drawerOpen     = ref(false)

// 顶部路径图辅助
function stepTagClass(id: string) {
  return [
    'bp-step-tag--' + nodeState(id),
    {
      'bp-step-tag--selected': selectedNodeId.value === id && drawerOpen.value,
      // spin 动画只给非 error 节点，且 AI 执行中时 error 不能 active
      'bp-step-tag--spin': id !== 'error' && nodeState(id) === 'active' && drawerOpen.value && selectedNodeId.value !== id,
    },
  ]
}
function stepDotStyle(id: string) {
  const n = nodes.value.find(n => n.id === id)!
  const s = nodeState(id)
  return { background: s === 'done' ? n.color : s === 'active' ? n.color : '#d9d9d9' }
}
function selectNode(id: string) { selectedNodeId.value = id; activeNodeId.value = id; drawerOpen.value = true }

function goToAiNode() {
  if (!projNextDisabled.value) {
    // 保存项目配置节点的输出（供 AI 节点参考）
    nodeIO.value.project.output = {
      repo: repo.value ? { name: repo.value.name, url: repo.value.url } : null,
      bug: selectedBug.value,
      attachments: uploadFileList.value.map(f => ({ name: f.name, size: f.size })),
    }
    // 保存到 localStorage
    saveWorkflowState()
    selectNode('ai')
  }
}

// ─── Step 0: 项目配置 ──────────────────────────────────────────────────────
const repoState = useRepo({ projectSource: 'cacheApplicationProjects' })
const {
  repoMode, repoUrl, repo, branch, commitId,
  loadingBranches, loadingCommits,
  repoOptions, branchOptions, commitOptions,
  branchDisabled, commitDisabled,
  canRun, uploadRef, canUpload,
  selectedProjectName, readonlyUrl, readonlyBranch, readonlyCommit, loadingRepos,
  onProjectDropdown, switchRepoMode,
  onBranchSearch, onCommitSearch, onBranchDropdown, onCommitDropdown,
  selectedAppType, selectedLanguage, selectedHasGraph,
} = repoState

// 是否需要手动选 branch/commit：非后端Java 或 没有代码图谱
const needManualRef = computed(() =>
  !!selectedProjectName.value && (
    selectedAppType.value !== 'backend' ||
    selectedLanguage.value !== 'java' ||
    !selectedHasGraph.value
  )
)

function getPopupContainer() { return document.body }

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// ─── Step 1: Bug 选择 ───────────────────────────────────────────────────────
type BugItem = { id: string; title: string }
const selectedBug      = ref<BugItem | null>(null)
const bugModalVisible  = ref(false)
const bugSearchKeyword = ref('')
const bugPageSize      = 8
const bugCurrentPage   = ref(1)

// 真实 Bug 列表状态
const bugListData   = ref<ForwardBugItem[]>([])
const bugListTotal  = ref(0)
const bugListLoading = ref(false)

async function fetchBugs(keyword: string, page: number) {
  bugListLoading.value = true
  try {
    const fn = keyword.trim() ? searchOnlineBugs : listOnlineBugs
    const res = keyword.trim()
      ? await searchOnlineBugs(keyword.trim(), page, bugPageSize)
      : await listOnlineBugs({ page, page_size: bugPageSize })
    if (res.ok && res.data) {
      bugListData.value  = res.data.items
      bugListTotal.value = res.data.total
    } else {
      bugListData.value  = []
      bugListTotal.value = 0
    }
  } catch {
    bugListData.value  = []
    bugListTotal.value = 0
  } finally {
    bugListLoading.value = false
  }
}

const pagedBugList = computed<BugItem[]>(() =>
  bugListData.value.map(b => ({ id: String(b.id), title: b.name }))
)

let bugSearchTimer: ReturnType<typeof setTimeout> | null = null
watch(bugSearchKeyword, (kw) => {
  bugCurrentPage.value = 1
  if (bugSearchTimer) clearTimeout(bugSearchTimer)
  bugSearchTimer = setTimeout(() => fetchBugs(kw, 1), 300)
})
watch(bugCurrentPage, (page) => { fetchBugs(bugSearchKeyword.value, page) })

function openBugModal() {
  bugSearchKeyword.value = ''
  bugCurrentPage.value = 1
  bugModalVisible.value = true
  fetchBugs('', 1)
}
function onSelectBug(bug: BugItem) { selectedBug.value = bug; bugModalVisible.value = false }

// ─── Step 1: 文件上传（只在前端收集，点"开始AI对话"时才真正上传） ────────────
const uploadState = useUpload(() => repo.value, () => uploadRef.value)
const { uploadFileList, uploading, batchUploadForSession } = uploadState

// before-upload 返回 false：阻止 Ant Design 自动上传，只收集文件到 uploadFileList
function beforeUpload() { return false }

// ─── Step 2: AI 分析 ────────────────────────────────────────────────────────
const aiModel  = ref<string | undefined>('claude::claude-sonnet-4-6')
const aiPrompt = ref('')
const aiModelOptions = modelOptions
const aiConfigDone = computed(() => !!aiModel.value && aiPrompt.value.trim().length > 0)
// AI 真正执行完成（配置成功 + 执行成功）
const aiExecutedSuccessfully = computed(() => aiConfigDone.value && execChatMsgs.value.length > 0 && !isExecuting.value)

// ─── Step 3: 分析确认 ──────────────────────────────────────────────────────
const confirmResult = ref<'pass' | 'rework' | null>(null)
const confirmNote   = ref('')
const confirmDone   = computed(() => confirmResult.value === 'pass')

function onConfirmPass() {
  confirmResult.value = 'pass'

  // 自动生成分支名和 commit 信息
  const plan = nodeIO.value.ai.output.structuredResult?.fix_plans?.find(
    (p: any) => p.id === selectedFixId.value
  )
  const bugId = selectedBug.value?.id ?? ''
  const now = new Date()
  const datePart = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const timePart = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`

  // 从方案标题提取语义片段：去掉标点、取前20字、转小写、空格换连字符
  const titleSlug = (plan?.title ?? '')
    .replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g, '')
    .trim()
    .slice(0, 20)
    .replace(/\s+/g, '-')
    .toLowerCase()

  const branchSuffix = [bugId && `bug${bugId}`, titleSlug, datePart, timePart]
    .filter(Boolean)
    .join('_')
  fixBranch.value = `aifix_${branchSuffix}`

  // commit 信息：fix(#bugId): 方案标题
  const commitTitle = plan?.title ?? '修复 Bug'
  fixCommitMsg.value = bugId
    ? `fix(#${bugId}): ${commitTitle}`
    : `fix: ${commitTitle}`

  saveWorkflowState()
  selectNode('fix')
}

function onConfirmRework() {
  Modal.confirm({
    title: '确认打回重改？',
    content: '将清空本次 AI 分析结果，返回 AI 分析节点重新执行。',
    okText: '确认打回',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => {
      confirmResult.value = 'rework'
      // 清空 AI 执行结果，回到 AI 分析节点
      execChatMsgs.value = []
      execFailed.value = false
      nodeIO.value.ai.output = {}
      selectedFixId.value = null
      confirmNote.value = ''
      saveWorkflowState()
      selectNode('ai')
    },
  })
}

// 选中的修复建议ID
const selectedFixId = ref<number | null>(null)

// diff 放大预览 Modal
const diffModalVisible = ref(false)
const diffModalPlan = ref<any>(null)

function openDiffModal(e: MouseEvent, plan: any) {
  e.stopPropagation()
  diffModalPlan.value = plan
  diffModalVisible.value = true
}

// bug 定位代码预览 Modal
const locModalVisible = ref(false)
const locModalActiveIdx = ref(0)

function openLocModal() {
  locModalActiveIdx.value = 0
  locModalVisible.value = true
}

const locationsWithSnippet = computed(() =>
  (nodeIO.value.ai.output.structuredResult?.bug_location ?? []).filter((l: any) => l.code_snippet?.trim())
)

// 选择修复建议的函数
function selectFixSuggestion(id: number) {
  selectedFixId.value = id
  // 更新 nodeIO 中的选中状态
  if (nodeIO.value.ai.output.analysisSummary?.fixSuggestions) {
    nodeIO.value.ai.output.analysisSummary.fixSuggestions.forEach((s: any) => {
      s.selected = s.id === id
    })
    saveWorkflowState()
  }
}

// ─── Step 4: 修复执行 ──────────────────────────────────────────────────────
const fixBranch    = ref('fix/')
const fixCommitMsg = ref('fix: ')
const fixReviewer  = ref('')
const fixDone      = computed(() => fixBranch.value.trim().startsWith('aifix_') && fixCommitMsg.value.trim().length > 5)

// 提交状态
const fixSubmitting = ref(false)
const fixSubmitResult = ref<{ ok: boolean; message: string; steps?: string[] } | null>(null)

function onSubmitFix() {
  const plan = nodeIO.value.ai.output.structuredResult?.fix_plans?.find(
    (p: any) => p.id === selectedFixId.value
  )
  Modal.confirm({
    title: '确认提交修复到 Git？',
    content: `将在仓库新建分支 "${fixBranch.value}"，应用 diff 并推送。此操作不可撤销，请确认 diff 内容无误。`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      fixSubmitting.value = true
      fixSubmitResult.value = null
      try {
        const res = await gitApplyAndCommit({
          git_url: repo.value?.url ?? '',
          project_name: selectedProjectName.value || undefined,
          branch: fixBranch.value,
          commit_msg: fixCommitMsg.value,
          diff: plan?.diff ?? '',
          reviewer: fixReviewer.value || undefined,
        })
        fixSubmitResult.value = res
        if (res.ok) saveWorkflowState()
      } catch (e: any) {
        fixSubmitResult.value = { ok: false, message: e?.message ?? String(e) }
      } finally {
        fixSubmitting.value = false
      }
    },
  })
}

// 节点输入输出数据（供后续节点参考）
const nodeIO = ref<Record<string, any>>({
  project: { input: {}, output: {} },
  ai:      { input: {}, output: {} },
  confirm: { input: {}, output: {} },
  fix:     { input: {}, output: {} },
  error:   null,
})

// ─── AI 执行进度对话面板 ─────────────────────────────────────────────────────
const chatDrawerOpen  = ref(false)
const isExecuting     = ref(false)
const execFailed      = ref(false)  // 执行是否失败
const showThinkingDetail = ref(false)  // 是否展示思考详情（工具调用步骤）
const execChatMsgs    = ref<ChatMsg[]>([])
const execChatInput   = ref('')
const execPanelRef    = ref<InstanceType<typeof ChatPanel> | null>(null)

let execStreamingIdx: number | null = null
let execStreamKind: 'text' | 'code' | null = null
let execCurrentToolSteps: ToolStep[] = []
let execAbort: AbortController | null = null

function execScrollBottom() {
  void execPanelRef.value?.scrollToBottom?.()
}

function execAppendStream(chunk: string, kind: 'text' | 'code' = 'text') {
  if (execStreamingIdx === null || execStreamKind !== kind) {
    const leadingTools = execCurrentToolSteps.length ? [...execCurrentToolSteps] : []
    execCurrentToolSteps = []
    const segments: import('../types/chat').ChatMsgSegment[] = []
    leadingTools.forEach(s => segments.push({ type: 'tool', steps: [s] }))
    segments.push({ type: 'text', content: chunk })
    execChatMsgs.value.push({
      role: 'ai',
      content: leadingTools.length ? '' : chunk,
      segments,
      streaming: true,
    })
    execStreamingIdx = execChatMsgs.value.length - 1
    execStreamKind = kind
  } else {
    const msg = execChatMsgs.value[execStreamingIdx]
    if (msg?.segments?.length) {
      const last = msg.segments[msg.segments.length - 1]
      if (last.type === 'text') last.content += chunk
      else msg.segments.push({ type: 'text', content: chunk })
      msg.content += chunk
    } else if (msg) {
      msg.content += chunk
    }
  }
  execScrollBottom()
}

async function startExecution() {
  if (!canStart.value) return
  isExecuting.value    = true
  execFailed.value     = false  // 重置失败状态
  chatDrawerOpen.value = true
  execChatMsgs.value   = []
  execStreamingIdx     = null
  execStreamKind       = null
  execCurrentToolSteps = []
  if (execAbort) { try { execAbort.abort() } catch {} }

  const onEvent = (ev: BugfixSseEvent) => {
    if (ev.type === 'status') return

    if (ev.type === 'tool') {
      if (ev.kind === 'file_list') return
      const step = { kind: ev.kind, title: ev.title, content: ev.content, summary: ev.summary }
      if (execStreamingIdx !== null) {
        const msg = execChatMsgs.value[execStreamingIdx]
        if (msg?.segments?.length) {
          const last = msg.segments[msg.segments.length - 1]
          if (last.type === 'tool') last.steps.push(step)
          else msg.segments.push({ type: 'tool', steps: [step] })
        } else if (msg) {
          msg.toolSteps = [...(msg.toolSteps ?? []), step]
        }
      } else {
        execCurrentToolSteps.push(step)
      }
      execScrollBottom()
      return
    }

    if (ev.type === 'text_chunk') {
      if (ev.content) execAppendStream(ev.content, 'text')
      return
    }

    if (ev.type === 'text') {
      if ((ev.content ?? '').trimEnd()) execAppendStream(ev.content!.trimEnd(), 'text')
      return
    }

    if (ev.type === 'diff') {
      const raw = String(ev.content ?? '')
      if (raw.trim()) { const wrapped = raw.includes('```') ? raw : '```diff\n' + raw + '\n```'; execAppendStream(wrapped, 'code') }
      return
    }

    if (ev.type === 'error') {
      execChatMsgs.value.push({ role: 'ai', content: `[错误] ${ev.content ?? '未知错误'}` })
      isExecuting.value = false
      execFailed.value = true  // 标记失败
      return
    }

    if (ev.type === 'done') {
      if (execStreamingIdx !== null) {
        const msg = execChatMsgs.value[execStreamingIdx]
        if (msg) msg.streaming = false
      }
      // ok: false 表示后端明确报错，标记失败
      if (ev.ok === false) {
        execFailed.value = true
      }
      isExecuting.value = false
      execScrollBottom()
      // 只有成功时才保存分析输出
      if (!execFailed.value) {
        nodeIO.value.ai.output = {
          ...nodeIO.value.ai.output,
          model: aiModel.value,
          prompt: aiPrompt.value,
          analysis: execChatMsgs.value.map(m => ({ role: m.role, content: m.content })),
        }
      }
      saveWorkflowState()
    }

    if (ev.type === 'structured_result') {
      nodeIO.value.ai.output = {
        ...nodeIO.value.ai.output,
        structuredResult: ev.data,
      }
      saveWorkflowState()
    }
  }

  const startTime = Date.now()
  let historyRecordId: number | null = null

  try {
    // 生成本次会话 ID
    const sessionId = `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

    // 在调 SSE 前，把项目配置中准备的文件批量上传到该会话目录
    let uploadedFileNames: string[] = []
    if (uploadFileList.value.length > 0) {
      uploadedFileNames = await batchUploadForSession(sessionId)
    }

    // 把 Bug ID 和 Bug 标题拼入消息，确保大模型知道需要分析哪个 Bug
    const bugPrefix = selectedBug.value
      ? `Bug ID：${selectedBug.value.id}\nBug 标题：${selectedBug.value.title}\n\n`
      : ''
    const fullMessage = `${bugPrefix}${aiPrompt.value}`

    // 使用真实接口调用 AI 分析
    const { provider, model } = parseModelChoice(aiModel.value ?? '')
    const ac = new AbortController()
    execAbort = ac

    // ── 执行开始前，插入 running 状态的历史记录 ──
    try {
      const res = await createBugfixHistory({
        project:    repo.value?.name ?? '',
        bug_id:     selectedBug.value?.id ?? '',
        bug_title:  selectedBug.value?.title ?? '',
        model:      aiModelOptions.find(m => m.value === aiModel.value)?.label ?? aiModel.value ?? '',
        status:     'running',
        session_id: sessionId,
        prompt:     aiPrompt.value,
        git_url:    repo.value?.url ?? undefined,
        git_branch: branch.value || undefined,
        git_commit: commitId.value || undefined,
      })
      if (res.ok) historyRecordId = res.id
    } catch (e) {
      console.warn('写入历史记录失败（不影响主流程）:', e)
    }

    await runForwardProcessflowNpeFixSse(
      {
        apply: false, // 不实际修改文件
        gitUrl: repo.value?.url ?? '',
        gitBranch: branch.value,
        gitCommit: commitId.value,
        message: fullMessage,
        provider,
        model: model || undefined,  // 转换为 string | undefined
        uploadedFileNames: uploadedFileNames.length > 0 ? uploadedFileNames : undefined,
        sessionId,
        history: execChatMsgs.value.map(m => ({ role: m.role, content: m.content })),
      },
      onEvent,
      { signal: ac.signal },
    )
  } catch (e: any) {
    if (e?.name !== 'AbortError') {
      execChatMsgs.value.push({ role: 'ai', content: `[错误] ${e?.message ?? String(e)}` })
      execFailed.value = true  // 标记失败
    }
  } finally {
    isExecuting.value = false
    execAbort = null
    execChatMsgs.value.forEach(m => { if (m.streaming) m.streaming = false })
    execStreamingIdx = null

    // ── 执行结束后，更新历史记录状态（含完整链路快照） ──
    if (historyRecordId !== null) {
      const durationMs = Date.now() - startTime
      const hasStructured = !!nodeIO.value.ai.output.structuredResult
      const finalStatus = execFailed.value ? 'failed' : 'success'
      const outcome = execFailed.value ? 'failed' : (hasStructured ? 'success' : 'error_end')
      const nodeIoJson = JSON.stringify(nodeIO.value)
      updateBugfixHistoryStatus(historyRecordId, finalStatus, durationMs, nodeIoJson, outcome).catch(() => {})
    }
  }
}

const canStart = computed(() =>
  canRun.value && !!selectedBug.value && aiConfigDone.value
)

const projNextDisabled = computed(() => {
  const hasRepo = canRun.value || !!selectedProjectName.value
  if (!hasRepo || !selectedBug.value) return true
  // 非后端Java或无图谱时，必须手动选 branch 或 commit
  if (needManualRef.value && !branch.value && !commitId.value) return true
  return false
})

// ─── 顶部操作栏 ────────────────────────────────────────────────────────────
const router = useRouter()
const historyVisible = ref(false)
type HistoryStatus = 'success' | 'failed' | 'running'

// 重置链路：清空所有状态，回到初始状态
function resetWorkflow() {
  // 如果正在执行中，禁止重置
  if (isExecuting.value) {
    return
  }

  // 二次确认
  Modal.confirm({
    title: '确定要重置链路吗？',
    content: '重置后将清空所有填写内容和执行记录，从头开始重新填写。',
    okText: '确定重置',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => {
      // 清空项目配置
      selectedProjectName.value = ''
      repoUrl.value = ''
      branch.value = ''
      commitId.value = ''

      // 清空 Bug
      selectedBug.value = null

      // 清空 AI 配置
      aiModel.value = 'claude::claude-sonnet-4-6'
      aiPrompt.value = ''

      // 清空上传文件
      uploadFileList.value = []

      // 清空分析确认
      confirmResult.value = null
      confirmNote.value = ''

      // 清空修复执行
      fixBranch.value = 'fix/'
      fixCommitMsg.value = 'fix: '
      fixReviewer.value = ''

      // 清空执行状态
      isExecuting.value = false
      execFailed.value = false
      execChatMsgs.value = []
      chatDrawerOpen.value = false

      // 清空节点 IO
      nodeIO.value = {
        project: { input: {}, output: {} },
        ai:      { input: {}, output: {} },
        confirm: { input: {}, output: {} },
        fix:     { input: {}, output: {} },
        error:   null,
      }

      // 关闭抽屉
      drawerOpen.value = false
      selectedNodeId.value = null
      activeNodeId.value = null

      // 清除 localStorage
      clearWorkflowState()
    },
  })
}

// ─── 节点输入输出 & 持久化 ────────────────────────────────────────────────
interface NodeIO {
  input: Record<string, any>
  output: Record<string, any>
}
const STORAGE_KEY = 'bugfix_workflow_state_v1'

// 保存状态到 localStorage
function saveWorkflowState() {
  const state = {
    timestamp: Date.now(),
    // 项目配置
    repoMode: repoMode.value,
    selectedProjectName: selectedProjectName.value,
    repoUrl: repoUrl.value,
    branch: branch.value,
    commitId: commitId.value,
    selectedBug: selectedBug.value,
    uploadFileList: uploadFileList.value.map(f => ({ name: f.name, size: f.size })),
    // AI 分析
    aiModel: aiModel.value,
    aiPrompt: aiPrompt.value,
    execChatMsgs: execChatMsgs.value,
    // 分析确认
    confirmResult: confirmResult.value,
    confirmNote: confirmNote.value,
    // 修复执行
    fixBranch: fixBranch.value,
    fixCommitMsg: fixCommitMsg.value,
    fixReviewer: fixReviewer.value,
    // 节点 IO
    nodeIO: nodeIO.value,
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

// 从 localStorage 加载状态
function loadWorkflowState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return false
    const state = JSON.parse(raw)
    // 检查是否过期（7 天）
    const expired = Date.now() - state.timestamp > 7 * 24 * 60 * 60 * 1000
    if (expired) {
      localStorage.removeItem(STORAGE_KEY)
      return false
    }
    // 恢复数据
    if (state.repoMode) repoMode.value = state.repoMode
    if (state.selectedProjectName) selectedProjectName.value = state.selectedProjectName
    if (state.repoUrl) repoUrl.value = state.repoUrl
    if (state.branch) branch.value = state.branch
    if (state.commitId) commitId.value = state.commitId
    if (state.selectedBug) selectedBug.value = state.selectedBug
    if (state.aiModel) aiModel.value = state.aiModel
    if (state.aiPrompt) aiPrompt.value = state.aiPrompt
    if (state.execChatMsgs && Array.isArray(state.execChatMsgs)) execChatMsgs.value = state.execChatMsgs
    if (state.confirmResult) confirmResult.value = state.confirmResult
    if (state.confirmNote) confirmNote.value = state.confirmNote
    if (state.fixBranch) fixBranch.value = state.fixBranch
    if (state.fixCommitMsg) fixCommitMsg.value = state.fixCommitMsg
    if (state.fixReviewer) fixReviewer.value = state.fixReviewer
    if (state.nodeIO) nodeIO.value = state.nodeIO
    
    // 重要：如果有项目名称，需要等待 useRepo 内部异步加载完成后才能生效
    // 这里延迟一小段时间让 repo 对象先初始化
    if (state.selectedProjectName) {
      setTimeout(() => {
        // 强制触发一次项目信息重新加载（模拟下拉框打开时的行为）
        onProjectDropdown(true)
        saveWorkflowState()
      }, 200)
    }
    
    return true
  } catch (e) {
    console.error('加载工作流状态失败:', e)
    return false
  }
}

// 清除保存的状态
function clearWorkflowState() {
  localStorage.removeItem(STORAGE_KEY)
}
const historyStatusMap: Record<HistoryStatus, { color: string; text: string }> = {
  success: { color: 'success', text: '成功' },
  failed:  { color: 'error',   text: '失败' },
  running: { color: 'processing', text: '进行中' },
}
const historyPageSize  = 10
const historyPage      = ref(1)
const historyTotal     = ref(0)
const historyList      = ref<BugfixHistoryItem[]>([])
const historyLoading   = ref(false)

async function fetchHistory(page = historyPage.value) {
  historyLoading.value = true
  try {
    const res = await listBugfixHistory({ page, page_size: historyPageSize })
    if (res.ok) {
      historyList.value  = res.items
      historyTotal.value = res.total
      historyPage.value  = page
    }
  } catch (e) {
    console.error('加载历史记录失败:', e)
  } finally {
    historyLoading.value = false
  }
}

// 打开历史弹窗时加载第一页
watch(historyVisible, (v) => { if (v) fetchHistory(1) })

// ─── 画布初始居中 ────────────────────────────────────────────────────────────
onMounted(() => {
  nextTick(() => {
    if (!canvasRef.value) return
    const cw = canvasRef.value.clientWidth
    const ch = canvasRef.value.clientHeight
    // 只用初始可见节点（project + ai）居中，避免隐藏节点撑大画布
    const initNodes = nodes.value.filter(n => ['project', 'ai'].includes(n.id))
    const allX = initNodes.map(n => [n.x, n.x + NODE_W]).flat()
    const allY = initNodes.map(n => [n.y, n.y + NODE_H]).flat()
    const minX = Math.min(...allX) - 80
    const minY = Math.min(...allY) - 80
    const maxX = Math.max(...allX) + 80
    const maxY = Math.max(...allY) + 80
    panX.value = (cw - (maxX - minX)) / 2 - minX
    panY.value = (ch - (maxY - minY)) / 2 - minY
    
    // 加载上次保存的工作流状态
    loadWorkflowState()
  })
})

// 监听状态变化并自动保存
watch(
  [
    repoMode,
    selectedProjectName,
    repoUrl,
    branch,
    commitId,
    selectedBug,
    aiModel,
    aiPrompt,
    execChatMsgs,
    confirmResult,
    confirmNote,
    fixBranch,
    fixCommitMsg,
    fixReviewer,
    nodeIO,
  ],
  () => { saveWorkflowState() },
  { deep: true }
)

// 监听异常结束节点激活，自动保存链路快照到 nodeIO
watch(
  () => nodeState('error'),
  (state) => {
    if (state !== 'active') return
    nodeIO.value.error = {
      triggeredAt: new Date().toISOString(),
      reason: execFailed.value ? 'ai_failed' : 'no_structured_result',
      snapshot: {
        project:    repo.value ? { name: repo.value.name, url: repo.value.url } : null,
        bug:        selectedBug.value,
        model:      aiModel.value,
        prompt:     aiPrompt.value,
        branch:     branch.value,
        commitId:   commitId.value,
        aiMessages: execChatMsgs.value.map(m => ({ role: m.role, content: m.content })),
        structuredResult: nodeIO.value.ai.output.structuredResult ?? null,
      },
    }
    saveWorkflowState()
  }
)
</script>

<template>
  <div ref="pageRef" class="bp-page">
    <!-- 顶部工具栏 -->
    <div class="bp-toolbar">
      <div class="bp-toolbar-left">
        <span class="bp-toolbar-title">Bugfix 流程编排</span>
        <a-divider type="vertical" style="height:20px;margin:0 12px" />

        <!-- 路径图：AI 完成前只显示前两节点，完成后展示完整分叉 -->
        <div class="bp-path-view">
          <div class="bp-path-main">
            <!-- 项目配置 -->
            <span class="bp-step-tag" :class="stepTagClass('project')" @click="nodeState('project') !== 'pending' && selectNode('project')">
              <span class="bp-step-dot" :style="stepDotStyle('project')" />项目配置
            </span>
            <span class="bp-step-arrow bp-step-arrow--active">-></span>
            <!-- AI 分析 -->
            <span class="bp-step-tag" :class="stepTagClass('ai')" @click="nodeState('ai') !== 'pending' && selectNode('ai')">
              <span class="bp-step-dot" :style="stepDotStyle('ai')" />
              <LoadingOutlined v-if="isExecuting" spin style="margin-right:4px;font-size:11px" />
              AI 分析
            </span>

            <!-- AI 完成后展示完整分叉 -->
            <template v-if="aiExecutedSuccessfully || execFailed">
              <span class="bp-fork-arrow">-></span>
              <!-- 分叉盒子：两条路径水平排列，竖线分隔 -->
              <div class="bp-path-fork">
                <!-- 正常路径 -->
                <div class="bp-path-branch" :class="{ 'bp-path-branch--dim': nodeState('error') === 'active' }">
                  <span class="bp-step-tag" :class="stepTagClass('confirm')" @click="nodeState('confirm') !== 'pending' && selectNode('confirm')">
                    <span class="bp-step-dot" :style="stepDotStyle('confirm')" />分析确认
                  </span>
                  <span class="bp-fork-arrow">-></span>
                  <span class="bp-step-tag" :class="stepTagClass('fix')" @click="nodeState('fix') !== 'pending' && selectNode('fix')">
                    <span class="bp-step-dot" :style="stepDotStyle('fix')" />修复执行
                  </span>
                </div>
                <!-- 分隔线 -->
                <span class="bp-fork-divider">|</span>
                <!-- 异常路径 -->
                <div class="bp-path-branch" :class="{ 'bp-path-branch--dim': nodeState('error') !== 'active' }">
                  <span
                    class="bp-step-tag"
                    :class="nodeState('error') === 'active' ? 'bp-step-tag--error-active' : 'bp-step-tag--pending'"
                    style="cursor:pointer"
                    @click="nodeState('error') === 'active' && selectNode('error')"
                  >
                    <span class="bp-step-dot" :style="{ background: nodeState('error') === 'active' ? '#ff4d4f' : '#d9d9d9' }" />异常结束
                  </span>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
      <div class="bp-toolbar-right">
        <a-button size="small" @click="historyVisible = true">历史记录</a-button>
        <a-tooltip :title="isExecuting ? 'AI 分析进行中…' : execFailed ? '执行失败，可重新执行' : aiExecutedSuccessfully ? '已执行完成' : !canStart ? '请先完成：项目配置、Bug 关联、AI 模型与提示词' : ''">
          <a-button
            type="primary"
            size="small"
            :disabled="!canStart || (isExecuting && !execFailed) || (aiExecutedSuccessfully && !execFailed)"
            @click="startExecution"
          >
            <template #icon>
              <LoadingOutlined v-if="isExecuting" spin />
              <ReloadOutlined v-else-if="execFailed" />
            </template>
            {{ isExecuting ? 'AI 分析中…' : execFailed ? '🔄 重新执行' : aiExecutedSuccessfully ? '✓ 已执行' : '▶ 开始执行' }}
          </a-button>
        </a-tooltip>
        <a-button type="dashed" danger size="small" :disabled="isExecuting" @click="resetWorkflow">重置链路</a-button>
      </div>
    </div>
    <!-- 画布 -->
    <div
      ref="canvasRef"
      class="bp-canvas"
      @wheel.prevent="onWheel"
      @pointerdown="onCanvasPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
    >
      <div
        class="bp-world"
        :style="{ transform: `translate(${panX}px, ${panY}px) scale(${scale})` }"
      >
        <!-- SVG 连线层：宽高设大覆盖整个世界坐标，overflow:visible 兜底 -->
        <svg class="bp-svg" style="overflow:visible" width="4000" height="2000">
          <defs>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="4" result="g"/>
              <feMerge><feMergeNode in="g"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>
            <!-- 箭头 marker：每条边单独定义 -->
            <marker v-for="(_, i) in EDGES" :key="'mk'+i"
              :id="'arrow-' + i"
              markerWidth="10" markerHeight="7"
              refX="8" refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" :fill="wireColor(i)" />
            </marker>
          </defs>

          <!-- 透明加粗命中区 -->
          <path
            v-for="(d, i) in connections"
            :key="'hit'+i"
            :d="d ?? ''"
            v-show="d !== null"
            fill="none"
            stroke="transparent"
            stroke-width="14"
            style="cursor:pointer"
            @click.stop="onWireClick(i)"
          />

          <!-- 可见连线 -->
          <path
            v-for="(d, i) in connections"
            :key="'w'+i"
            :d="d ?? ''"
            v-show="d !== null"
            fill="none"
            :stroke="wireColor(i)"
            :stroke-width="activeWireIdx === i ? 4 : 2.5"
            :marker-end="`url(#arrow-${i})`"
            :class="{
              'wire-active': activeWireIdx === i,
              'wire-error-anim': EDGES[i].isError && nodeState('error') === 'active',
              'wire-pending-anim': !EDGES[i].isError && nodeState(EDGES[i].to) === 'pending',
            }"
            style="pointer-events:none"
          />

          <!-- 已连通线流动光点（正常路径） -->
          <template v-for="(d, i) in connections" :key="'flow'+i">
            <path
              v-if="d !== null && !EDGES[i].isError && nodeState(EDGES[i].from) === 'done'"
              :d="d"
              fill="none"
              :stroke="nodes.find(n => n.id === EDGES[i].from)!.color"
              stroke-width="3"
              stroke-linecap="round"
              class="wire-flow-done"
              :style="{ animationDelay: `-${i * 0.45}s` }"
              style="pointer-events:none"
            />
          </template>

          <!-- 引脚圆点：只渲染可见节点的 -->
          <template v-for="n in nodes.filter(n => visibleNodeIds.includes(n.id))" :key="'p'+n.id">
            <circle
              v-if="EDGES.some(e => e.to === n.id && visibleEdgeIndices.includes(EDGES.indexOf(e)))"
              :cx="inPin(n).x" :cy="inPin(n).y" :r="PIN_R"
              :fill="EDGES.filter(e => e.to === n.id).some(e => nodeState(e.from) === 'done') ? nodes.find(nn => nn.id === EDGES.find(e => e.to === n.id)!.from)!.color : '#c0cce0'"
              stroke="#e8ecf2" stroke-width="2"
            />
            <circle
              v-if="EDGES.some(e => e.from === n.id && visibleEdgeIndices.includes(EDGES.indexOf(e)))"
              :cx="outPin(n).x" :cy="outPin(n).y" :r="PIN_R"
              :fill="nodeState(n.id) === 'done' ? n.color : '#c0cce0'"
              stroke="#e8ecf2" stroke-width="2"
            />
          </template>
        </svg>

        <!-- 节点卡片 -->
        <div
          v-for="n in nodes.filter(n => visibleNodeIds.includes(n.id))"
          :key="n.id"
          class="bp-node"
          :class="[
            'bp-node--' + nodeState(n.id),
            { 'bp-node--selected': selectedNodeId === n.id && drawerOpen },
            { 'bp-node--error-active': n.id === 'error' && nodeState('error') === 'active' },
          ]"
          :style="{ left: n.x+'px', top: n.y+'px', width: NODE_W+'px', height: NODE_H+'px' }"
          @pointerdown.stop="onNodePointerDown($event, n.id)"
        >
          <div class="bp-node-header" :style="{ background: n.color }">
            <span class="bp-node-title">{{ n.title }}</span>
            <span class="bp-node-status" :class="'bp-node-status--' + nodeState(n.id)">{{ nodeStatusText(n.id) }}</span>
          </div>
          <div class="bp-node-body">
            <div class="bp-node-desc">{{ n.desc }}</div>
            <div v-if="n.id==='project' && repo"         class="bp-node-info">{{ repo.name }}{{ selectedBug ? ' · #' + selectedBug.id : '' }}</div>
            <div v-if="n.id==='ai'      && aiModel"      class="bp-node-info">{{ aiModelOptions.find(m=>m.value===aiModel)?.label }}</div>
            <div v-if="n.id==='confirm' && confirmResult" class="bp-node-info" :style="{color: confirmResult==='pass'?'#52c41a':'#f5222d'}">
              {{ confirmResult === 'pass' ? '✓ 审查通过' : '↩ 打回重改' }}
            </div>
            <div v-if="n.id==='fix' && fixDone"          class="bp-node-info">{{ fixBranch }}</div>
            <div v-if="n.id==='error' && nodeState('error')==='active'" class="bp-node-info" style="color:#ff4d4f">
              {{ execFailed ? '执行失败' : '无结构化数据' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 左下角提示 -->
      <div class="bp-hint">
        滚轮缩放 · 拖拽画布/节点 · 点击节点配置 &nbsp;|&nbsp; {{ Math.round(scale * 100) }}%
      </div>
    </div>

    <!-- 右侧配置抽屉 -->
    <a-drawer
      :open="drawerOpen"
      :title="nodes.find(n => n.id === activeNodeId)?.title ?? '节点配置'"
      placement="right"
      :width="560"
      :mask="false"
      :destroy-on-close="true"
      :get-container="() => pageRef!"
      :header-style="{ background:'#fff', borderBottom:'1px solid #f0f0f0' }"
      :body-style="{ background:'#fafafa', padding:'20px', overflowX:'hidden' }"
      class="bp-drawer"
      @close="drawerOpen = false"
      @after-open-change="(open: boolean) => { if (!open) activeNodeId = null }"
    >
      <!-- 项目配置 -->
      <template v-if="activeNodeId === 'project'">
        <!-- 可滚动内容区 -->
        <div class="project-scroll-content">
          <a-form layout="vertical" class="bp-form">
            <a-form-item label="项目名称">
              <a-select
                v-model:value="selectedProjectName"
                placeholder="搜索或选择项目" :loading="loadingRepos"
                show-search allow-clear :list-height="228"
                :get-popup-container="getPopupContainer"
                :filter-option="(input: string, opt: any) => (opt?.label ?? '').toLowerCase().includes((input||'').toLowerCase())"
                @dropdownVisibleChange="(open: boolean) => onProjectDropdown(open)"
                :disabled="isExecuting || aiExecutedSuccessfully"
              >
                <a-select-option v-for="opt in repoOptions" :key="opt.value" :value="opt.value" :label="opt.label">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <template v-if="selectedProjectName">
              <a-form-item v-if="readonlyUrl" label="Git URL">
                <a-input :value="readonlyUrl" disabled />
              </a-form-item>

              <!-- 后端Java且有图谱：只读展示 branch/commit -->
              <template v-if="!needManualRef">
                <a-form-item v-if="readonlyBranch" label="Branch">
                  <a-input :value="readonlyBranch" disabled />
                </a-form-item>
                <a-form-item v-if="readonlyCommit" label="Commit">
                  <a-input :value="readonlyCommit" disabled />
                </a-form-item>
              </template>

              <!-- 非后端Java 或 无图谱：需手动选 branch/commit -->
              <template v-else>
                <a-alert
                  type="info" show-icon style="margin-bottom:12px"
                  :message="selectedHasGraph ? '该应用不是后端 Java，无代码图谱，请手动选择分支' : '该应用尚未构建代码图谱，请手动选择分支'"
                />
                <a-form-item label="Branch">
                  <a-select
                    v-model:value="branch"
                    placeholder="选择分支"
                    allow-clear show-search
                    :loading="loadingBranches"
                    :disabled="branchDisabled || isExecuting || aiExecutedSuccessfully"
                    :options="branchOptions"
                    :get-popup-container="getPopupContainer"
                    @search="onBranchSearch"
                    @dropdownVisibleChange="(open: boolean) => onBranchDropdown(open)"
                  />
                </a-form-item>
                <a-form-item label="Commit（可选）">
                  <a-select
                    v-model:value="commitId"
                    placeholder="选择 Commit"
                    allow-clear show-search
                    :loading="loadingCommits"
                    :disabled="commitDisabled || isExecuting || aiExecutedSuccessfully"
                    :options="commitOptions"
                    :get-popup-container="getPopupContainer"
                    @search="onCommitSearch"
                    @dropdownVisibleChange="(open: boolean) => onCommitDropdown(open)"
                  />
                </a-form-item>
              </template>
            </template>
          </a-form>

          <a-alert v-if="repo" type="success" show-icon style="margin-top:12px">
            <template #message><span style="color:#52c41a">{{ repo.name }}</span></template>
            <template #description><span style="font-size:12px;opacity:.7">{{ repo.url }}</span></template>
          </a-alert>

          <!-- 关联 Bug -->
          <a-divider style="margin:18px 0 14px" />
          <a-form layout="vertical" class="bp-form">
            <a-form-item label="关联 Bug">
              <div v-if="selectedBug" class="bp-bug-row">
                <a-tooltip :title="selectedBug.title" placement="topLeft">
                  <div class="bp-bug-display">
                    <BugOutlined style="flex-shrink:0;opacity:.6" />
                    <span class="bp-bug-id">{{ selectedBug.id }}</span>
                    <span class="bp-bug-title">{{ selectedBug.title }}</span>
                  </div>
                </a-tooltip>
                <a-button size="small" @click="openBugModal" :disabled="isExecuting || aiExecutedSuccessfully">更换</a-button>
                <a-button size="small" danger @click="selectedBug = null" :disabled="isExecuting || aiExecutedSuccessfully">移除</a-button>
              </div>
              <a-button v-else @click="openBugModal">
                <template #icon><BugOutlined /></template>选择 Bug
              </a-button>
            </a-form-item>

            <a-form-item label="上传附件">
              <a-upload
                v-model:file-list="uploadFileList"
                :before-upload="beforeUpload"
                :disabled="!canUpload || isExecuting || aiExecutedSuccessfully"
                :max-count="5"
                :show-upload-list="{ showRemoveIcon: !isExecuting && !aiExecutedSuccessfully }"
              >
                <a-button :loading="uploading" :disabled="!canUpload || isExecuting || aiExecutedSuccessfully">选择文件</a-button>
              </a-upload>
              <div v-if="!canUpload && !isExecuting && !aiExecutedSuccessfully" style="font-size:12px;color:rgba(0,0,0,.35);margin-top:4px">
                需先完成项目配置
              </div>
            </a-form-item>
          </a-form>
        </div>

        <!-- 底部固定按钮 -->
        <div class="project-footer">
          <a-tooltip :title="projNextDisabled ? '请先完成：项目配置、Bug 关联' : ''">
            <a-button type="primary" :disabled="projNextDisabled" @click="goToAiNode">下一步：AI 分析 ▶</a-button>
          </a-tooltip>
        </div>
      </template>

      <!-- AI 分析 -->
      <template v-else-if="activeNodeId === 'ai'">
        <a-form layout="vertical" class="bp-form">
          <a-form-item label="AI 模型">
            <a-select
              v-model:value="aiModel"
              placeholder="请选择模型"
              :options="modelOptions"
              :get-popup-container="getPopupContainer"
              allow-clear
              :disabled="isExecuting || aiExecutedSuccessfully"
            />
          </a-form-item>

          <a-form-item label="问题描述">
            <a-textarea
              v-model:value="aiPrompt"
              :rows="7"
              placeholder="请详细描述问题现象、复现步骤、可能原因等……例：调用 /api/order/pay 后常规返回 500，日志显示 NPE，堆栈在 PayServiceImpl.execute:142"
              allow-clear
              show-count
              :maxlength="2000"
              :disabled="isExecuting || aiExecutedSuccessfully"
            />
          </a-form-item>
        </a-form>
        <!-- AI 分析节点底部按鈕 -->
        <div style="margin-top:16px;display:flex;gap:8px;justify-content:flex-end">
          <a-button
            v-if="execChatMsgs.length > 0"
            @click="chatDrawerOpen = true"
          >
            <template #icon>
              <LoadingOutlined v-if="isExecuting" spin />
              <MessageOutlined v-else />
            </template>
            {{ isExecuting ? 'AI 分析中…' : '查看进度' }}
          </a-button>
          <a-tooltip :title="isExecuting ? 'AI 分析进行中…' : execFailed ? '执行失败，可重新执行' : aiExecutedSuccessfully ? '已执行完成，请到【分析确认】节点查看结果' : !canStart ? '请先完成：项目配置、Bug 关联、AI 模型与描述' : ''">
            <a-button
              type="primary"
              :disabled="!canStart || (isExecuting && !execFailed) || (aiExecutedSuccessfully && !execFailed)"
              @click="startExecution"
            >
              <template #icon>
                <LoadingOutlined v-if="isExecuting" spin />
                <ReloadOutlined v-else-if="execFailed" />
              </template>
              {{ isExecuting ? 'AI 分析中…' : execFailed ? '🔄 重新执行' : aiExecutedSuccessfully ? '✓ 已执行' : '▶ 开始执行' }}
            </a-button>
          </a-tooltip>
        </div>
      </template>

      <!-- 分析确认 -->
      <template v-else-if="activeNodeId === 'confirm'">
        <template v-if="nodeIO.ai.output.structuredResult">
          <!-- 结构化结果展示 -->
          <div class="bp-result-box">
            <div class="bp-result-box-title">🔍 Bug 根因</div>
            <div class="bp-result-box-body">
              <div class="bp-result-line">{{ nodeIO.ai.output.structuredResult.bug_cause }}</div>
            </div>
          </div>

          <div class="bp-result-box" style="margin-top:12px">
            <div class="bp-result-box-title">
              📍 Bug 定位
              <a-tooltip v-if="locationsWithSnippet.length" title="查看涉及代码">
                <span class="bp-fix-expand-btn" style="opacity:1;margin-left:6px" @click="openLocModal()">
                  <FullscreenOutlined />
                </span>
              </a-tooltip>
            </div>
            <div class="bp-result-box-body">
              <div
                v-for="(loc, idx) in nodeIO.ai.output.structuredResult.bug_location"
                :key="idx"
                class="bp-location-item"
              >
                <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap">
                  <span class="bp-fix-file">{{ loc.file }}</span>
                  <span v-if="loc.line_range" class="bp-fix-lines">行 {{ loc.line_range }}</span>
                </div>
                <div class="bp-result-line" style="margin-top:4px">{{ loc.description }}</div>
              </div>
            </div>
          </div>

          <div class="bp-result-box" style="margin-top:12px">
            <div class="bp-result-box-title">🛠 修复方案</div>
            <div class="bp-result-box-body" style="padding:0">
              <div
                v-for="plan in nodeIO.ai.output.structuredResult.fix_plans"
                :key="plan.id"
                class="bp-fix-item"
                :class="{
                  'bp-fix-item--selected': selectedFixId === plan.id,
                  'bp-fix-item--no-diff': !plan.diff,
                }"
                @click="!confirmDone && plan.diff ? (selectedFixId = plan.id) : undefined"
              >
                <a-radio :checked="selectedFixId === plan.id" :disabled="!plan.diff || confirmDone" @click.stop />
                <div class="bp-fix-content">
                  <div class="bp-fix-header">
                    <span style="font-weight:600;font-size:13px;flex:1">方案 {{ plan.id }}：{{ plan.title }}</span>
                    <a-tooltip v-if="!plan.diff" title="该方案缺少 diff，无法选择">
                      <span style="font-size:11px;color:#faad14;flex-shrink:0">⚠ 无 diff</span>
                    </a-tooltip>
                    <a-tooltip v-else title="查看 Diff 详情">
                      <span class="bp-fix-expand-btn" @click.stop="openDiffModal($event, plan)">
                        <FullscreenOutlined />
                      </span>
                    </a-tooltip>
                  </div>
                  <div class="bp-fix-problem">{{ plan.description }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 已审查通过：只读展示 -->
          <template v-if="confirmDone">
            <a-alert
              type="success"
              show-icon
              style="margin-top:16px"
              message="已审查通过，流程已进入修复执行阶段"
            >
              <template #description>
                <span v-if="confirmNote" style="font-size:12px;color:rgba(0,0,0,.55)">备注：{{ confirmNote }}</span>
              </template>
            </a-alert>
            <a-button style="margin-top:10px;width:100%" @click="selectNode('fix')">前往修复执行节点 →</a-button>
          </template>

          <!-- 待审查：展示操作区 -->
          <template v-else>
            <a-form layout="vertical" class="bp-form" style="margin-top:16px">
              <a-form-item label="审查结果">
                <div style="display:flex;gap:8px">
                  <a-button
                    type="primary"
                    :disabled="selectedFixId === null"
                    @click="onConfirmPass"
                  >✓ 审查通过，进入修复执行</a-button>
                  <a-button danger @click="onConfirmRework">↩ 打回重改</a-button>
                </div>
                <div v-if="selectedFixId === null" class="bp-tip">请先选择一个修复方案</div>
              </a-form-item>
              <a-form-item label="备注（可选）">
                <a-textarea v-model:value="confirmNote" :rows="3" placeholder="补充说明..." />
              </a-form-item>
            </a-form>
          </template>
        </template>
        <template v-else-if="aiConfigDone && execChatMsgs.length > 0">
          <!-- 结构化提取中或失败，显示等待提示 -->
          <a-alert v-if="isExecuting" type="info" show-icon message="AI 分析中，结构化结果将在完成后自动生成…" />
          <a-alert v-else type="warning" show-icon message="结构化提取未完成，请重新执行 AI 分析" />
        </template>
        <template v-else-if="aiConfigDone">
          <a-alert type="info" show-icon message="请先执行 AI 分析" />
        </template>
      </template>

      <!-- 修复执行 -->
      <template v-else-if="activeNodeId === 'fix'">
        <!-- 选中的修复方案摘要 -->
        <template v-if="nodeIO.ai.output.structuredResult && selectedFixId !== null">
          <div class="bp-result-box" style="margin-bottom:16px">
            <div class="bp-result-box-title">
              ✅ 已选方案：{{ nodeIO.ai.output.structuredResult.fix_plans.find((p: any) => p.id === selectedFixId)?.title }}
              <a-tooltip v-if="nodeIO.ai.output.structuredResult.fix_plans.find((p: any) => p.id === selectedFixId)?.diff" title="查看 Diff 详情">
                <span class="bp-fix-expand-btn" style="opacity:1;margin-left:6px" @click="openDiffModal($event, nodeIO.ai.output.structuredResult.fix_plans.find((p: any) => p.id === selectedFixId))">
                  <FullscreenOutlined />
                </span>
              </a-tooltip>
            </div>
            <div class="bp-result-box-body">
              <template v-for="plan in nodeIO.ai.output.structuredResult.fix_plans" :key="plan.id">
                <template v-if="plan.id === selectedFixId">
                  <div class="bp-result-line">{{ plan.description }}</div>
                  <div v-if="!plan.diff" style="font-size:12px;color:#faad14;margin-top:4px">⚠ 该方案暂无 diff，请参考 AI 对话中的修复说明</div>
                </template>
              </template>
            </div>
          </div>
        </template>
        <a-form layout="vertical" class="bp-form">
          <a-form-item label="分支名">
            <a-input v-model:value="fixBranch" placeholder="aifix_bug1001_xxx" allow-clear :disabled="!!fixSubmitResult?.ok">
              <template #prefix><span style="color:#bbb;font-size:12px">git checkout -b</span></template>
            </a-input>
          </a-form-item>
          <a-form-item label="Commit 信息">
            <a-input v-model:value="fixCommitMsg" placeholder="fix(#1001): 修复 NPE" allow-clear :disabled="!!fixSubmitResult?.ok" />
          </a-form-item>
          <a-form-item label="CR Reviewer（可选）">
            <a-input v-model:value="fixReviewer" placeholder="@username" allow-clear :disabled="!!fixSubmitResult?.ok" />
          </a-form-item>
        </a-form>

        <!-- 提交结果 -->
        <template v-if="fixSubmitResult">
          <a-alert
            :type="fixSubmitResult.ok ? 'success' : 'error'"
            show-icon
            :message="fixSubmitResult.ok ? '已成功提交并推送' : '提交失败'"
            style="margin-top:12px"
          >
            <template #description>
              <div style="white-space:pre-wrap;font-size:12px">{{ fixSubmitResult.message }}</div>
              <div v-if="(fixSubmitResult as any).missing_paths?.length" style="margin-top:8px">
                <div style="font-size:12px;color:rgba(0,0,0,.55);margin-bottom:4px">路径不存在（请检查 AI 生成的 diff 路径）：</div>
                <div
                  v-for="p in (fixSubmitResult as any).missing_paths"
                  :key="p"
                  style="font-family:monospace;font-size:11px;color:#cf1322"
                >{{ p }}</div>
              </div>
            </template>
          </a-alert>
          <div v-if="fixSubmitResult.steps?.length" style="margin-top:8px;font-size:12px;color:rgba(0,0,0,.45)">
            <div v-for="(s, i) in fixSubmitResult.steps" :key="i" style="font-family:monospace">$ {{ s }}</div>
          </div>
        </template>

        <!-- 提交按钮 -->
        <div v-if="!fixSubmitResult?.ok" style="margin-top:16px">
          <a-button
            type="primary"
            block
            :disabled="!fixDone"
            :loading="fixSubmitting"
            @click="onSubmitFix"
          >
            🚀 提交修复到 Git
          </a-button>
          <div v-if="!fixDone" style="font-size:12px;color:rgba(0,0,0,.35);margin-top:6px;text-align:center">
            请确保分支名以 aifix_ 开头且 commit 信息已填写
          </div>
        </div>
      </template>
      <!-- 异常结束 -->
      <template v-else-if="activeNodeId === 'error'">
        <a-result
          status="error"
          :title="execFailed ? 'AI 分析执行失败' : '未获取到结构化分析数据'"
          :sub-title="execFailed ? '请查看 AI 对话框中的错误信息，确认 Bug 信息是否可获取，然后重新执行分析。' : 'AI 分析已完成，但未能提取结构化结果。请重新执行 AI 分析，或检查 Bug 描述是否足够详细。'"
          style="padding:20px 0"
        >
          <template #extra>
            <a-button type="primary" @click="selectNode('ai')">返回 AI 分析节点</a-button>
          </template>
        </a-result>
        <div v-if="execChatMsgs.length > 0" style="margin-top:8px">
          <a-button block @click="chatDrawerOpen = true">
            <template #icon><MessageOutlined /></template>
            查看 AI 对话详情
          </a-button>
        </div>
        <!-- 链路快照 -->
        <div v-if="nodeIO.error?.triggeredAt" class="bp-result-box" style="margin-top:16px">
          <div class="bp-result-box-title">📋 链路快照</div>
          <div class="bp-result-box-body" style="gap:6px">
            <div class="bp-result-line">触发时间：<em>{{ new Date(nodeIO.error.triggeredAt).toLocaleString() }}</em></div>
            <div class="bp-result-line">失败原因：<em>{{ nodeIO.error.reason === 'ai_failed' ? 'AI 执行失败' : '无结构化数据' }}</em></div>
            <div v-if="nodeIO.error.snapshot?.bug" class="bp-result-line">关联 Bug：<em>#{{ nodeIO.error.snapshot.bug.id }} {{ nodeIO.error.snapshot.bug.title }}</em></div>
            <div v-if="nodeIO.error.snapshot?.project" class="bp-result-line">项目：<em>{{ nodeIO.error.snapshot.project.name }}</em></div>
            <div v-if="nodeIO.error.snapshot?.model" class="bp-result-line">模型：<em>{{ nodeIO.error.snapshot.model }}</em></div>
          </div>
        </div>
      </template>

      <template v-else>
        <div style="text-align:center;padding:60px 0;color:rgba(0,0,0,.25)">
          <div style="font-size:40px;margin-bottom:12px">🔧</div>
          <div>功能开发中…</div>
        </div>
      </template>
    </a-drawer>

    <!-- AI 执行进度对话抽屉 -->
    <a-drawer
      :open="chatDrawerOpen"
      placement="right"
      :width="900"
      :mask="true"
      :mask-closable="true"
      :get-container="() => pageRef!"
      :header-style="{ background:'#fff', borderBottom:'1px solid #f0f0f0', flexShrink: 0, display:'flex', alignItems:'center', justifyContent:'space-between' }"
      :body-style="{ padding:'0', height:'100%', display:'flex', flexDirection:'column', overflow:'hidden' }"
      @close="chatDrawerOpen = false"
    >
      <template #title>
        <div style="display:flex;alignItems:center;gap:12px;width:100%">
          <span style="font-size:16px;font-weight:600">AI 分析进度</span>
          <a-divider type="vertical" style="height:20px;margin:0 8px" />
          <a-switch v-model:checked="showThinkingDetail" size="small" />
          <span style="font-size:13px;color:rgba(0,0,0,.65)">展示思考详情</span>
        </div>
      </template>
      <div class="exec-layout">
        <!-- 左侧：任务信息 -->
        <div class="exec-info-panel">
          <div class="exec-info-title">本次任务</div>
          <div class="exec-info-block">
            <div class="exec-info-label">关联 Bug</div>
            <div class="exec-info-value">
              <span v-if="selectedBug" style="color:#1677ff;font-weight:600">#{{ selectedBug.id }}</span>
              <span v-else style="color:rgba(0,0,0,.35)">未选择</span>
            </div>
            <a-tooltip v-if="selectedBug" :title="selectedBug.title" placement="topLeft">
              <div class="exec-info-sub exec-bug-title">{{ selectedBug.title }}</div>
            </a-tooltip>
          </div>
          <div class="exec-info-block">
            <div class="exec-info-label">目标项目</div>
            <a-tooltip placement="topLeft">
              <template #title>
                <div style="font-size:12px;line-height:1.8">
                  <div><strong>项目名称：</strong>{{ repo?.name ?? '未配置' }}</div>
                  <div v-if="repo?.url"><strong>Git 仓库：</strong>{{ repo.url }}</div>
                  <div v-if="readonlyBranch"><strong>Branch：</strong>{{ readonlyBranch }}</div>
                  <div v-if="readonlyCommit"><strong>Commit：</strong>{{ readonlyCommit }}</div>
                  <div v-if="!repo?.url && !readonlyBranch && !readonlyCommit" style="color:rgba(0,0,0,.35)">暂无更多信息</div>
                </div>
              </template>
              <div class="exec-info-value" style="cursor:help">{{ repo?.name ?? '未配置' }}</div>
            </a-tooltip>
            <div v-if="fixBranch && fixBranch !== 'fix/'" class="exec-info-sub">分支：{{ fixBranch }}</div>
          </div>
          <div class="exec-info-block">
            <div class="exec-info-label">使用模型</div>
            <div class="exec-info-value">{{ aiModelOptions.find(m => m.value === aiModel)?.label ?? '未选择' }}</div>
          </div>
          <div class="exec-info-block">
            <div class="exec-info-label">问题描述</div>
            <div class="exec-info-desc">{{ aiPrompt || '未填写' }}</div>
          </div>
          <div class="exec-info-block">
            <div class="exec-info-label">上传附件</div>
            <div v-if="uploadFileList && uploadFileList.length > 0" class="exec-file-list">
              <div v-for="(f, idx) in uploadFileList" :key="idx" class="exec-file-item">
                <span class="exec-file-icon">📎</span>
                <span class="exec-file-name">{{ f.name }}</span>
                <span class="exec-file-size">({{ formatFileSize(f.size ?? 0) }})</span>
              </div>
            </div>
            <div v-else class="exec-info-empty">无附件</div>
          </div>
          <div class="exec-info-block" style="margin-top:auto;padding-top:12px;border-top:1px solid #f0f0f0">
            <div class="exec-info-label">执行状态</div>
            <div style="display:flex;align-items:center;gap:6px;margin-top:4px">
              <a-badge
                :status="isExecuting ? 'processing' : execFailed ? 'error' : (execChatMsgs.length ? 'success' : 'default')"
              />
              <span style="font-size:12px;color:rgba(0,0,0,.55)">
                {{ isExecuting ? 'AI 正在分析中…' : execFailed ? '执行失败' : (execChatMsgs.length ? '分析完成' : '等待执行') }}
              </span>
            </div>
            <a-button v-if="isExecuting" danger style="margin-top:8px;width:100%" @click="(execAbort as AbortController | null)?.abort();isExecuting=false">终止</a-button>
            <a-button v-else-if="execFailed" type="primary" style="margin-top:8px;width:100%" @click="startExecution">
              <template #icon><ReloadOutlined /></template>
              重新执行
            </a-button>
          </div>
        </div>
    
        <!-- 右侧：AI 对话流 -->
        <div class="exec-chat-panel">
          <ChatPanel
            ref="execPanelRef"
            :loading="isExecuting"
            :chat-messages="execChatMsgs"
            v-model:chat-input="execChatInput"
            :can-send="false"
            :render-markdown="renderMarkdown"
            :hide-input="true"
            :show-thinking-detail="showThinkingDetail"
            style="flex:1;min-height:0"
          />
        </div>
      </div>
    </a-drawer>

    <!-- 历史记录弹窗 -->
    <a-modal
      v-model:open="historyVisible"
      title="执行历史"
      :footer="null"
      :width="880"
      destroy-on-close
    >
      <!-- 固定内容区域，防止分页跳变高度 -->
      <div class="hist-body">
        <a-list :data-source="historyList" :split="true">
          <template #renderItem="{ item }">
            <a-list-item class="hist-item">
              <!-- 左：状态 + 时间 -->
              <div class="hist-col hist-col-status">
                <a-badge :status="historyStatusMap[(item as BugfixHistoryItem).status].color" />
                <span class="hist-status-text">{{ historyStatusMap[(item as BugfixHistoryItem).status].text }}</span>
                <span class="hist-time">{{ (item as BugfixHistoryItem).time }}</span>
              </div>
              <!-- 中：项目 / BugId / 标题 -->
              <div class="hist-col hist-col-main">
                <span class="hist-project">{{ (item as BugfixHistoryItem).project }}</span>
                <a-tag color="blue" class="hist-tag-bugid">{{ (item as BugfixHistoryItem).bugId }}</a-tag>
                <a-tooltip :title="(item as BugfixHistoryItem).bugTitle" placement="topLeft">
                  <span class="hist-bug-title">{{ (item as BugfixHistoryItem).bugTitle }}</span>
                </a-tooltip>
              </div>
              <!-- 右：模型 / 耗时 / 操作 -->
              <div class="hist-col hist-col-right">
                <a-tag class="hist-tag-sm">{{ (item as BugfixHistoryItem).model }}</a-tag>
                <span class="hist-duration">{{ (item as BugfixHistoryItem).duration ?? '—' }}</span>
                <a-button size="small" type="link" class="hist-btn" @click="() => { historyVisible = false; router.push(`/bugfix-workflow/bugfix-details/${(item as BugfixHistoryItem).id}`) }">详情</a-button>
                <a-button size="small" type="link" :disabled="(item as BugfixHistoryItem).status === 'running'" class="hist-btn">重运行</a-button>
              </div>
            </a-list-item>
          </template>
        </a-list>
      </div>
      <!-- 分页（固定在内容区域外） -->
      <div class="hist-footer">
        <a-pagination
          v-model:current="historyPage"
          :page-size="historyPageSize"
          :total="historyTotal"
          :show-size-changer="false"
          size="small"
        />
      </div>
    </a-modal>

    <!-- Diff 放大预览 Modal -->
    <a-modal
      v-model:open="diffModalVisible"
      :title="diffModalPlan ? `方案 ${diffModalPlan.id}：${diffModalPlan.title}` : 'Diff 详情'"
      :footer="null"
      :width="860"
      destroy-on-close
      :body-style="{ padding: '16px 20px', maxHeight: '70vh', overflowY: 'auto' }"
    >
      <template v-if="diffModalPlan">
        <div style="font-size:13px;color:#555;margin-bottom:12px;line-height:1.6">{{ diffModalPlan.description }}</div>
        <DiffViewer v-if="diffModalPlan.diff" :diff="diffModalPlan.diff" />
        <a-empty v-else description="该方案暂无 diff 内容" />
      </template>
    </a-modal>

    <!-- Bug 定位代码预览 Modal -->
    <a-modal
      v-model:open="locModalVisible"
      title="涉及代码"
      :footer="null"
      :width="860"
      destroy-on-close
      :body-style="{ padding: '0', maxHeight: '70vh', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
    >
      <template v-if="locationsWithSnippet.length">
        <!-- 文件标签页 -->
        <div class="loc-modal-tabs">
          <div
            v-for="(loc, i) in locationsWithSnippet"
            :key="i"
            class="loc-modal-tab"
            :class="{ 'loc-modal-tab--active': locModalActiveIdx === i }"
            @click="locModalActiveIdx = Number(i)"
          >
            <span class="loc-modal-tab-name" :title="loc.file">{{ loc.file.split('/').pop() }}</span>
            <span v-if="loc.line_range" class="loc-modal-tab-line">:{{ loc.line_range }}</span>
          </div>
        </div>
        <!-- 当前文件内容 -->
        <div class="loc-modal-body">
          <template v-for="(loc, i) in locationsWithSnippet" :key="i">
            <template v-if="locModalActiveIdx === i">
              <div class="loc-modal-meta">
                <span class="bp-fix-file" style="font-size:12px">{{ loc.file }}</span>
                <span v-if="loc.line_range" class="bp-fix-lines">行 {{ loc.line_range }}</span>
              </div>
              <div class="loc-modal-desc">{{ loc.description }}</div>
              <div class="bp-fix-diff" v-html="renderMarkdown('```java\n' + loc.code_snippet + '\n```')" style="margin:0" />
            </template>
          </template>
        </div>
      </template>
    </a-modal>

    <!-- Bug 选择模态框 -->
    <a-modal v-model:open="bugModalVisible" title="选择 Bug" :footer="null" :width="620" destroy-on-close>
      <a-input-search v-model:value="bugSearchKeyword" placeholder="搜索 Bug 标题…" allow-clear style="margin-bottom:12px" />
      <a-spin :spinning="bugListLoading">
        <div class="bug-list">
          <div v-for="bug in pagedBugList" :key="bug.id"
            class="bug-list-item" :class="{ 'bug-list-item-selected': selectedBug?.id === bug.id }"
            @click="onSelectBug(bug)"
          >
            <span class="bug-list-id">{{ bug.id }}</span>
            <a-tooltip :title="bug.title" placement="topLeft">
              <span class="bug-list-title">{{ bug.title }}</span>
            </a-tooltip>
          </div>
          <a-empty v-if="!bugListLoading && pagedBugList.length === 0" description="未找到匹配的 Bug" />
        </div>
      </a-spin>
      <div v-if="bugListTotal > bugPageSize" class="bug-pagination">
        <a-pagination size="small" :current="bugCurrentPage" :page-size="bugPageSize"
          :total="bugListTotal" :show-size-changer="false"
          @change="(p: number) => bugCurrentPage = p" />
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.bp-page { height: 100%; display: flex; flex-direction: column; overflow: hidden; position: relative; }

/* ── 顶部工具栏 ── */
.bp-toolbar {
  flex-shrink: 0;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  z-index: 10;
}
.bp-toolbar-left  { display: flex; align-items: center; gap: 4px; flex: 1; min-width: 0; overflow: hidden; }

/* 路径图容器 */
.bp-path-view { display: flex; align-items: center; min-width: 0; overflow: hidden; }
.bp-path-main { display: flex; align-items: center; gap: 4px; flex-wrap: nowrap; }

/* 分叉盒子：水平排列，带边框，视觉上是一个整体 */
.bp-path-fork {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border: 1px solid #e2e8f4;
  border-radius: 20px;
  background: #f8faff;
}
.bp-path-branch {
  display: flex;
  align-items: center;
  gap: 4px;
  transition: opacity 0.25s;
}
.bp-path-branch--dim { opacity: 0.3; }

/* 分叉内的箭头 */
.bp-fork-arrow {
  font-size: 13px;
  color: #1677ff;
  opacity: 0.5;
  flex-shrink: 0;
  user-select: none;
}
.bp-fork-arrow--error { color: #ff4d4f; opacity: 0.8; }

/* 分叉分隔线 */
.bp-fork-divider {
  color: #d0d7e6;
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
  user-select: none;
  margin: 0 2px;
}
.bp-toolbar-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.bp-toolbar-title { font-size: 14px; font-weight: 700; color: #1a1a2e; white-space: nowrap; }

/* 项目配置滚动容器 */
.project-scroll-content {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-right: 4px;
}
.project-footer {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  margin-top: 8px;
  background: linear-gradient(to bottom, transparent, #fafafa 60%);
}
.bp-step-tag {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 10px; border-radius: 100px;
  font-size: 12px; cursor: pointer; white-space: nowrap;
  border: 1px solid transparent; transition: all .15s;
  color: rgba(0,0,0,.45);
}
.bp-step-tag:hover { background: #f0f5ff; color: #1677ff; }
.bp-step-tag--done    { color: rgba(0,0,0,.65); }
.bp-step-tag--selected { border: 1.5px solid #1677ff !important; background: #f0f5ff !important; color: #1677ff !important; font-weight: 600; animation: none !important; }
.bp-step-tag--pending { color: rgba(0,0,0,.25) !important; cursor: not-allowed !important; }
.bp-step-tag--pending:hover { background: transparent !important; color: rgba(0,0,0,.25) !important; }
.bp-step-tag--active  { color: rgba(22,119,255,0.7); }

/* 旋转边框（active 未选中） */
@property --spin-angle {
  syntax: '<angle>';
  inherits: false;
  initial-value: 0deg;
}
@keyframes step-tag-spin { to { --spin-angle: 360deg; } }
.bp-step-tag--spin {
  border: 1.5px solid transparent !important;
  background:
    linear-gradient(#fff, #fff) padding-box,
    conic-gradient(from var(--spin-angle), rgba(22,119,255,0.15) 0deg, rgba(22,119,255,0.7) 80deg, rgba(22,119,255,0.15) 160deg, rgba(22,119,255,0.05) 360deg) border-box !important;
  color: rgba(22,119,255,0.75) !important;
  animation: step-tag-spin 2.5s linear infinite;
}
.bp-step-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }

/* 步骤标签间箭头 */
@keyframes arrow-pulse {
  0%, 100% { opacity: 0.25; transform: translateX(0); }
  50%       { opacity: 0.7;  transform: translateX(2px); }
}
.bp-step-arrow {
  font-size: 16px;
  line-height: 1;
  flex-shrink: 0;
  user-select: none;
  pointer-events: none;
  margin-bottom: 3px;
}
.bp-step-arrow--active  { color: #1677ff; opacity: 0.5; }
.bp-step-arrow--error   { color: #ff4d4f; opacity: 0.8; font-size: 12px; }
.bp-step-tag--error-active {
  color: #ff4d4f !important;
  border-color: #ff4d4f !important;
  background: #fff1f0 !important;
  cursor: pointer;
}
.bp-step-arrow--pending {
  color: #bfbfbf;
  animation: arrow-pulse 1.2s ease-in-out infinite;
}

.bp-canvas {
  flex: 1;
  position: relative;
  overflow: hidden;
  background-color: #f0f2f7;
  background-image: radial-gradient(circle, rgba(0,0,0,0.12) 1px, transparent 1px);
  background-size: 24px 24px;
  cursor: grab;
  user-select: none;
}
.bp-canvas:active { cursor: grabbing; }

.bp-world {
  position: absolute;
  top: 0; left: 0;
  transform-origin: 0 0;
}

.bp-svg {
  position: absolute;
  top: 0; left: 0;
}

.bp-hint {
  position: absolute;
  bottom: 12px; left: 16px;
  font-size: 12px;
  color: rgba(0,0,0,0.3);
  pointer-events: none;
}

/* ── 节点卡片 ── */
.bp-node {
  position: absolute;
  border-radius: 8px;
  overflow: hidden;
  cursor: grab;
  border: 2px solid rgba(0,0,0,0.08);
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12);
  transition: border-color 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.bp-node:active { cursor: grabbing; }
.bp-node:hover  { border-color: rgba(0,0,0,0.18); box-shadow: 0 6px 24px rgba(0,0,0,0.18); }
.bp-node--selected { border-color: #1677ff !important; box-shadow: 0 0 0 3px rgba(22,119,255,0.15), 0 6px 24px rgba(0,0,0,0.18) !important; }
.bp-node--done {
  border: 2.5px solid #52c41a;
  box-shadow: 0 0 0 3px rgba(82,196,26,0.12), 0 4px 16px rgba(0,0,0,0.1);
}
.bp-node--done .bp-node-body  { background: #f6ffed; }
.bp-node--active { border-color: #faad14; }
.bp-node--pending {
  opacity: 0.5;
  cursor: not-allowed;
}
.bp-node--pending:hover {
  border-color: rgba(0,0,0,0.08) !important;
  box-shadow: 0 2px 12px rgba(0,0,0,0.12) !important;
}
/* error 节点 active 时红色边框 */
.bp-node--error-active {
  border-color: #ff4d4f !important;
  box-shadow: 0 0 0 3px rgba(255,77,79,0.12), 0 4px 16px rgba(0,0,0,0.1) !important;
}

.bp-node-header {
  padding: 8px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.bp-node-title  { font-size: 13px; font-weight: 700; color: #fff; text-shadow: 0 1px 3px rgba(0,0,0,.4); }

/* 状态徽章 */
.bp-node-status { font-size: 11px; }
.bp-node-status--done {
  background: rgba(255,255,255,0.95);
  color: #389e0d;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  letter-spacing: 0.3px;
}
.bp-node-status--active  { color: rgba(255,255,255,.95); background: rgba(255,255,255,.18); padding: 2px 8px; border-radius: 10px; font-size: 11px; }
.bp-node-status--pending { color: rgba(255,255,255,.45); font-size: 11px; }

.bp-node-body   { flex:1; padding: 10px 14px; display: flex; flex-direction: column; gap: 4px; }
.bp-node-desc   { font-size: 12px; color: rgba(0,0,0,.4); }
.bp-node-info   {
  font-size: 12px; font-weight: 600; color: rgba(0,0,0,.65);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-top: auto; padding-top: 4px; border-top: 1px solid rgba(0,0,0,.06);
}

/* ── 连线动画 ── */
@keyframes wire-flow {
  from { stroke-dashoffset: 24; }
  to   { stroke-dashoffset: 0; }
}
.wire-active {
  stroke-dasharray: 10 6;
  animation: wire-flow 0.5s linear infinite;
}

/* 已连通线流动光点动画 */
@keyframes wire-dot-flow {
  from { stroke-dashoffset: 32; }
  to   { stroke-dashoffset: 0; }
}
.wire-flow-done {
  stroke-dasharray: 5 27;
  opacity: 0.85;
  animation: wire-dot-flow 1.4s linear infinite;
  filter: drop-shadow(0 0 3px currentColor);
}

/* 待配置目标节点的连线：流动虚线动画（箭头跟着跑） */
@keyframes wire-pending-flow {
  from { stroke-dashoffset: 24; }
  to   { stroke-dashoffset: 0; }
}
.wire-pending-anim {
  stroke-dasharray: 7 9;
  animation: wire-pending-flow 0.9s linear infinite;
  opacity: 0.7;
}

/* 异常路径连线：红色虚线闪烁 */
@keyframes wire-error-flow {
  from { stroke-dashoffset: 20; }
  to   { stroke-dashoffset: 0; }
}
.wire-error-anim {
  stroke-dasharray: 6 6;
  animation: wire-error-flow 0.6s linear infinite;
  opacity: 0.85;
}

/* ── Bug 行 ── */
.bp-bug-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.bp-bug-display {
  flex:1; min-width: 0; display: flex; align-items: center; gap: 6px;
  padding: 6px 10px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fafafa; color: rgba(0,0,0,.75); cursor: pointer; transition: border-color .2s;
}
.bp-bug-display:hover { border-color: #1677ff; }
.bp-bug-id    { flex-shrink:0; font-size:12px; font-weight:600; color:#1677ff; }
.bp-bug-title { flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:13px; }

/* ── 历史记录 ── */
.hist-body {
  height: 530px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}
.hist-footer {
  display: flex;
  justify-content: flex-end;
  padding: 10px 0 0;
}
.hist-item {
  padding: 10px 12px !important;
  align-items: center !important;
  gap: 0;
  min-height: 48px;
}
.hist-col  { display: flex; align-items: center; line-height: 1; }
.hist-col-status { flex: 0 0 186px; gap: 5px; }
.hist-col-main   { flex: 1; min-width: 0; gap: 0; overflow: hidden; align-items: baseline; }
.hist-col-right  { flex: 0 0 auto; gap: 6px; align-items: center; padding-left: 10px; }
.hist-project    { font-size: 12px; font-weight: 600; color: rgba(0,0,0,.75); white-space: nowrap; flex-shrink: 0; }
.hist-status-text { font-size: 11px; color: rgba(0,0,0,.55); white-space: nowrap; }
.hist-time       { font-size: 11px; color: rgba(0,0,0,.35); white-space: nowrap; }
.hist-duration   { font-size: 11px; color: rgba(0,0,0,.35); white-space: nowrap; min-width: 40px; text-align: right; }
.hist-tag-bugid  { margin: 0 6px; flex-shrink: 0; font-size: 11px; line-height: 18px; padding: 0 5px; }
.hist-tag-sm     { font-size: 11px; line-height: 18px; padding: 0 5px; margin: 0; flex-shrink: 0; }
.hist-btn        { padding: 0 4px; height: 22px; line-height: 22px; }
.hist-bug-title {
  font-size: 12px; color: rgba(0,0,0,.65);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  display: block; cursor: default;
}

/* ── AI 执行进度对话 ── */
.exec-watch-btn { color: #1677ff !important; border-color: #1677ff !important; }

/* 左右布局容器 */
.exec-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* 左侧信息面板 */
.exec-info-panel {
  flex: 0 0 260px;
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
  overflow-y: auto;
  gap: 2px;
}
.exec-info-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0,0,0,.55);
  text-transform: uppercase;
  letter-spacing: .04em;
  margin-bottom: 12px;
}
.exec-info-block {
  padding: 10px 0;
  border-bottom: 1px solid #e8e8e8;
}
.exec-info-block:last-child { border-bottom: none; }
.exec-info-label {
  font-size: 12px;
  color: rgba(0,0,0,.45);
  margin-bottom: 5px;
  font-weight: 500;
}
.exec-info-value {
  font-size: 14px;
  color: rgba(0,0,0,.85);
  font-weight: 500;
  word-break: break-all;
  line-height: 1.5;
}
.exec-info-sub {
  font-size: 12px;
  color: rgba(0,0,0,.55);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.exec-bug-title {
  max-width: 100%;
}
.exec-info-desc {
  font-size: 13px;
  color: rgba(0,0,0,.65);
  line-height: 1.6;
  word-break: break-all;
  overflow-y: auto;
  max-height: 180px;
  margin-top: 3px;
}
.exec-info-empty {
  font-size: 12px;
  color: rgba(0,0,0,.35);
  font-style: italic;
}

/* 附件列表 */
.exec-file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
}
.exec-file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(0,0,0,.65);
  padding: 4px 0;
}
.exec-file-icon {
  font-size: 14px;
  flex-shrink: 0;
}
.exec-file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(0,0,0,.75);
}
.exec-file-size {
  flex-shrink: 0;
  font-size: 11px;
  color: rgba(0,0,0,.45);
}

/* 右侧对话面板 */
.exec-chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── AI 结果框 ── */
.bp-result-box {
  border: 1px solid #e8e8e8; border-radius: 8px; overflow: hidden;
}
.bp-result-box-title {
  padding: 8px 14px; font-size: 13px; font-weight: 600;
  background: #fafafa; border-bottom: 1px solid #f0f0f0; color: #333;
  display: flex; align-items: center;
}

/* Bug 定位代码 Modal */
.loc-modal-tabs {
  display: flex;
  overflow-x: auto;
  scrollbar-width: none;
  border-bottom: 1px solid #e8e8e8;
  background: #f5f5f5;
  flex-shrink: 0;
}
.loc-modal-tabs::-webkit-scrollbar { display: none; }
.loc-modal-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  cursor: pointer;
  white-space: nowrap;
  font-size: 12px;
  color: rgba(0,0,0,.55);
  border-bottom: 2px solid transparent;
  transition: all 0.12s;
  font-family: ui-monospace, monospace;
}
.loc-modal-tab:hover { background: #ebebeb; color: rgba(0,0,0,.85); }
.loc-modal-tab--active { background: #fff; border-bottom-color: #1677ff; color: #1677ff; font-weight: 600; }
.loc-modal-tab-name { max-width: 180px; overflow: hidden; text-overflow: ellipsis; }
.loc-modal-tab-line { color: rgba(0,0,0,.35); font-size: 11px; }
.loc-modal-tab--active .loc-modal-tab-line { color: #91caff; }
.loc-modal-body { flex: 1; overflow-y: auto; padding: 12px 16px; }
.loc-modal-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.loc-modal-desc { font-size: 13px; color: rgba(0,0,0,.55); margin-bottom: 10px; line-height: 1.5; }
.bp-result-box-body { padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; overflow: hidden; }
.bp-result-line { font-size: 13px; color: rgba(0,0,0,.65); line-height: 1.6; word-break: break-all; }
.bp-result-line em { font-style: normal; color: #1677ff; font-weight: 500; }

/* 修复建议列表样式 */
.bp-fix-suggestions {
  margin-top: 12px;
}
.bp-location-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}
.bp-location-item:last-child { border-bottom: none; }
.bp-fix-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  margin-top: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.bp-fix-item:hover {
  border-color: #1677ff;
  background-color: #f0f5ff;
}
.bp-fix-item--selected {
  border-color: #1677ff;
  background-color: #e6f4ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.1);
}
.bp-fix-item--no-diff {
  opacity: 0.5;
  cursor: not-allowed;
}
.bp-fix-item--no-diff:hover {
  border-color: #e8e8e8 !important;
  background-color: transparent !important;
}
.bp-fix-content {
  flex: 1;
  min-width: 0;
}
.bp-fix-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.bp-fix-expand-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  color: rgba(0,0,0,.35);
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
  cursor: pointer;
}
.bp-fix-expand-btn:hover {
  background: rgba(22,119,255,0.1);
  color: #1677ff;
}
.bp-fix-item:hover .bp-fix-expand-btn,
.bp-fix-item--selected .bp-fix-expand-btn {
  opacity: 1;
}
.bp-fix-file {
  font-family: monospace;
  font-size: 13px;
  color: #1677ff;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
.bp-fix-lines {
  font-size: 12px;
  color: #888;
  flex-shrink: 0;
}
.bp-fix-problem {
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
  line-height: 1.5;
  word-break: break-all;
  overflow-wrap: anywhere;
}
.bp-fix-solution {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  word-break: break-all;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  max-width: 100%;
  overflow: hidden;
}
.bp-fix-diff {
  margin-top: 8px;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}
.bp-fix-diff :deep(.hljs-block) {
  overflow-x: auto;
  max-width: 100%;
}
.bp-tip {
  font-size: 12px;
  color: #faad14;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.bp-tip::before {
  content: "⚠";
}

/* 右侧配置抽屉 */
.bp-drawer {
  position: fixed !important;
  z-index: 1000;
}

:deep(.bp-drawer .ant-drawer-body) {
  overflow-x: hidden !important;
  width: 560px !important;
  max-width: 560px !important;
  box-sizing: border-box !important;
}

/* ── Bug 模态框 ── */
.bug-list { border:1px solid #f0f0f0; border-radius:6px; min-height:368px; }
.bug-list-item {
  display:flex; align-items:center; gap:10px; padding:10px 14px;
  cursor:pointer; border-bottom:1px solid #f5f5f5; transition:background .15s;
}
.bug-list-item:last-child { border-bottom:none; }
.bug-list-item:hover { background:#f0f5ff; }
.bug-list-item-selected { background:#e6f4ff; border-left:3px solid #1677ff; }
.bug-list-id    { flex-shrink:0; font-size:12px; font-weight:600; color:#1677ff; min-width:76px; }
.bug-list-title { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:13px; color:rgba(0,0,0,.75); }
.bug-pagination { display:flex; justify-content:flex-end; margin-top:12px; }
</style>

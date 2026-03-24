<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { BugOutlined, QuestionCircleOutlined, LoadingOutlined, MessageOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { runForwardProcessflowNpeFixSse, type BugfixSseEvent, type ToolStep } from '../api'
import { runMockExecution } from '../api/mockExecution'
import { listOnlineBugs, searchOnlineBugs, type ForwardBugItem } from '../api/invoke'
import { parseModelChoice } from '../constants'
import type { ChatMsg } from '../types'
import ChatPanel from '../components/ChatPanel.vue'
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
  { id: 'project',  title: '项目配置',      desc: '选择仓库 · 关联 Bug · 上传附件', color: '#4a9eff', x: 60,   y: 180 },
  { id: 'ai',       title: 'AI 分析',      desc: '配置模型与分析深度',               color: '#722ed1', x: 380,  y: 200 },
  { id: 'confirm',  title: '分析确认',      desc: '人工审查结果',                    color: '#fa8c16', x: 700,  y: 150 },
  { id: 'fix',      title: '修复执行',      desc: '建分支 · 提交 · CR',             color: '#f5222d', x: 1020, y: 190 },
])

const NODE_W = 220
const NODE_H = 120
const PIN_R  = 7

// ─── 画布变换：平移 + 缩放 ────────────────────────────────────────────────────
const canvasRef = ref<HTMLElement | null>(null)
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

const connections = computed(() =>
  nodes.value.slice(0, -1).map((n, i) => {
    const out = outPin(n)
    const inp = inPin(nodes.value[i + 1])
    const dx  = Math.abs(inp.x - out.x) * 0.5
    return `M${out.x},${out.y} C${out.x+dx},${out.y} ${inp.x-dx},${inp.y} ${inp.x},${inp.y}`
  })
)

function wireColor(i: number) {
  if (activeWireIdx.value === i) return nodes.value[i].color
  const src = nodeState(nodes.value[i].id)
  const tgt = nodeState(nodes.value[i + 1].id)
  if (src === 'done') return nodes.value[i].color   // 已连通：节点自身颜色
  if (tgt === 'pending') return '#e2e8f4'            // 目标未就绪：极淡
  return '#c4cfe6'                                   // 源节点待配置：中等灰
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
  const confDone = aiDone && confirmDone.value
  if (id === 'project') return projDone    ? 'done' : 'active'
  if (id === 'ai')      return aiDone      ? 'done' : (projDone    ? 'active' : 'pending')
  if (id === 'confirm') return confDone    ? 'done' : (aiDone      ? 'active' : 'pending')
  if (id === 'fix')     return fixDone.value ? 'done' : (confDone  ? 'active' : 'pending')
  return 'pending'
}
function nodeStatusText(id: string) {
  const s = nodeState(id)
  // AI 分析节点特殊状态文字
  if (id === 'ai') {
    if (s === 'done') return '✓ 已完成'
    if (isExecuting.value) return '执行中…'
    if (s === 'active') return '待执行'
    return '未就绪'
  }
  return s === 'done' ? '✓ 已完成' : s === 'active' ? '待配置' : '未就绪'
}

// ─── 配置面板 ────────────────────────────────────────────────────────────────
const selectedNodeId = ref<string | null>(null)
const drawerOpen     = ref(false)
function selectNode(id: string) { selectedNodeId.value = id; drawerOpen.value = true }

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
} = repoState

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

// ─── Step 1: 文件上传 ──────────────────────────────────────────────────────
const uploadState = useUpload(() => repo.value, () => uploadRef.value)
const { uploadFileList, uploading, customUpload } = uploadState
async function handleCustomUpload(opt: any) { try { await customUpload(opt) } catch {} }

// ─── Step 2: AI 分析 ────────────────────────────────────────────────────────
const aiModel  = ref<string | undefined>('claude::claude-sonnet-4-6')
const aiDepth  = ref('standard')
const aiPrompt = ref('')
const aiDepthOptions = [
  { value: 'quick',    label: '快速' },
  { value: 'standard', label: '标准' },
  { value: 'deep',     label: '深度' },
]
const aiModelOptions = modelOptions
const aiConfigDone = computed(() => !!aiModel.value && aiPrompt.value.trim().length > 0)
// AI 真正执行完成（配置成功 + 执行成功）
const aiExecutedSuccessfully = computed(() => aiConfigDone.value && execChatMsgs.value.length > 0 && !isExecuting.value)

// ─── Step 3: 分析确认 ──────────────────────────────────────────────────────
const confirmResult = ref<'pass' | 'rework' | null>(null)
const confirmNote   = ref('')
const confirmDone   = computed(() => confirmResult.value === 'pass')

// ─── Step 4: 修复执行 ──────────────────────────────────────────────────────
const fixBranch    = ref('fix/')
const fixCommitMsg = ref('fix: ')
const fixReviewer  = ref('')
const fixDone      = computed(() => fixBranch.value.trim().length > 4 && fixCommitMsg.value.trim().length > 5)

// 节点输入输出数据（供后续节点参考）
const nodeIO = ref<Record<string, NodeIO>>({
  project: { input: {}, output: {} },
  ai:      { input: {}, output: {} },
  confirm: { input: {}, output: {} },
  fix:     { input: {}, output: {} },
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
      isExecuting.value = false
      execScrollBottom()
      // AI 分析执行完成，保存输出（供确认节点参考）
      nodeIO.value.ai.output = {
        model: aiModel.value,
        depth: aiDepth.value,
        prompt: aiPrompt.value,
        analysis: execChatMsgs.value.map(m => ({ role: m.role, content: m.content })),
      }
      saveWorkflowState()
    }
  }

  try {
    await runMockExecution(onEvent, {
      bugId:    selectedBug.value?.id,
      bugTitle: selectedBug.value?.title,
      projName: repo.value?.name,
    })
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
  }
}

const canStart = computed(() =>
  canRun.value && !!selectedBug.value && aiConfigDone.value
)

const projNextDisabled = computed(() => {
  // 刷新页面后 repo 可能还在加载中，需要同时检查 selectedProjectName
  const hasRepo = canRun.value || !!selectedProjectName.value
  return !hasRepo || !selectedBug.value
})

// ─── 顶部操作栏 ────────────────────────────────────────────────────────────
const router = useRouter()
const historyVisible = ref(false)
type HistoryStatus = 'success' | 'failed' | 'running'
interface HistoryRecord {
  id: string
  time: string
  project: string
  bugId: string
  bugTitle: string
  model: string
  depth: string
  status: HistoryStatus
  duration?: string
}

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
      aiDepth.value = 'standard'
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
      }

      // 关闭抽屉
      drawerOpen.value = false
      selectedNodeId.value = null

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
    aiDepth: aiDepth.value,
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
    if (state.aiDepth) aiDepth.value = state.aiDepth
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
const mockHistory: HistoryRecord[] = [
  { id: 'H001', time: '2026-03-24 14:32', project: 'epaas-gateway',   bugId: 'BUG-1001', bugTitle: 'ProjectServiceImpl.processFlow 空指针异常导致 500',  model: 'DeepSeek',      depth: '标准', status: 'success', duration: '2m 14s' },
  { id: 'H002', time: '2026-03-24 11:05', project: 'order-service',   bugId: 'BUG-1003', bugTitle: '订单状态流转异常：已支付订单未触发发货流程',              model: 'Claude-Haiku',  depth: '深度', status: 'success', duration: '4m 07s' },
  { id: 'H003', time: '2026-03-23 17:48', project: 'epaas-gateway',   bugId: 'BUG-1006', bugTitle: '用户登录接口返回 403 但权限配置正确',                    model: 'Claude-Sonnet', depth: '标准', status: 'failed',  duration: '1m 52s' },
  { id: 'H004', time: '2026-03-23 10:20', project: 'user-center',     bugId: 'BUG-1007', bugTitle: '定时任务在多节点部署时重复执行导致数据重复',              model: 'DeepSeek',      depth: '标准', status: 'success', duration: '3m 31s' },
  { id: 'H005', time: '2026-03-22 16:09', project: 'file-service',    bugId: 'BUG-1005', bugTitle: '文件上传并发场景偶现文件内容覆盖',                       model: 'Claude-Haiku',  depth: '快速', status: 'running' },
  { id: 'H006', time: '2026-03-22 10:10', project: 'payment-service', bugId: 'BUG-1008', bugTitle: '支付回调重试导致金额多次扣',                             model: 'DeepSeek',      depth: '深度', status: 'success', duration: '5m 03s' },
  { id: 'H007', time: '2026-03-21 15:30', project: 'order-service',   bugId: 'BUG-1009', bugTitle: 'Redis 分布式锁超时导致并发请求堆积',                     model: 'Claude-Sonnet', depth: '标准', status: 'success', duration: '3m 45s' },
  { id: 'H008', time: '2026-03-21 09:15', project: 'user-center',     bugId: 'BUG-1010', bugTitle: '用户头像上传后默认头像未刷新',                           model: 'Claude-Haiku',  depth: '快速', status: 'failed',  duration: '0m 58s' },
  { id: 'H009', time: '2026-03-20 17:22', project: 'epaas-gateway',   bugId: 'BUG-1011', bugTitle: 'Feign 调用超时未进行降级导致联调失败',                   model: 'DeepSeek',      depth: '深度', status: 'success', duration: '4m 22s' },
  { id: 'H010', time: '2026-03-20 11:08', project: 'file-service',    bugId: 'BUG-1012', bugTitle: 'OSS 大文件分片上传顺序错乱导致内容损坏',                 model: 'Claude-Sonnet', depth: '标准', status: 'success', duration: '2m 50s' },
  { id: 'H011', time: '2026-03-19 16:00', project: 'epaas-gateway',   bugId: 'BUG-1013', bugTitle: '网关路由规则匹配失败导致接口 404',                       model: 'DeepSeek',      depth: '快速', status: 'success', duration: '1m 38s' },
  { id: 'H012', time: '2026-03-19 10:45', project: 'user-center',     bugId: 'BUG-1014', bugTitle: 'JWT token 过期未刷新导致用户频繁登出',                   model: 'Claude-Haiku',  depth: '标准', status: 'failed',  duration: '2m 11s' },
]
const historyStatusMap: Record<HistoryStatus, { color: string; text: string }> = {
  success: { color: 'success', text: '成功' },
  failed:  { color: 'error',   text: '失败' },
  running: { color: 'processing', text: '进行中' },
}
const historyPageSize = 10
const historyPage     = ref(1)
const pagedHistory = computed(() => {
  const start = (historyPage.value - 1) * historyPageSize
  return mockHistory.slice(start, start + historyPageSize)
})

// ─── 画布初始居中 ────────────────────────────────────────────────────────────
onMounted(() => {
  nextTick(() => {
    if (!canvasRef.value) return
    const cw = canvasRef.value.clientWidth
    const ch = canvasRef.value.clientHeight
    const allX = nodes.value.map(n => [n.x, n.x + NODE_W]).flat()
    const allY = nodes.value.map(n => [n.y, n.y + NODE_H]).flat()
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
    aiDepth,
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
</script>

<template>
  <div class="bp-page">
    <!-- 顶部工具栏 -->
    <div class="bp-toolbar">
      <div class="bp-toolbar-left">
        <span class="bp-toolbar-title">Bugfix 流程编排</span>
        <a-divider type="vertical" style="height:20px;margin:0 12px" />
        <template v-for="(n, idx) in nodes" :key="n.id">
          <!-- 节点间箭头分隔符 -->
          <span
            v-if="idx > 0"
            class="bp-step-arrow"
            :class="nodeState(n.id) === 'pending' ? 'bp-step-arrow--pending' : 'bp-step-arrow--active'"
          >-></span>
          <span
            class="bp-step-tag"
            :class="[
              'bp-step-tag--' + nodeState(n.id),
              {
                'bp-step-tag--selected': selectedNodeId === n.id && drawerOpen,
                'bp-step-tag--spin':     nodeState(n.id) === 'active' && drawerOpen && selectedNodeId !== n.id,
              },
            ]"
            @click="nodeState(n.id) !== 'pending' && selectNode(n.id)"
          >
            <span class="bp-step-dot" :style="{ background: nodeState(n.id)==='done' ? n.color : nodeState(n.id)==='active' ? n.color : '#d9d9d9' }" />
            {{ n.title }}
          </span>
        </template>
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
            <!-- 箭头 marker：每种颜色单独定义，用 color 变量驱动 -->
            <marker v-for="(n, i) in nodes.slice(0,-1)" :key="'mk'+i"
              :id="'arrow-' + i"
              markerWidth="10" markerHeight="7"
              refX="8" refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" :fill="wireColor(i)" />
            </marker>
            <!-- 待配置节点专用动画箭头（灰色） -->
            <marker id="arrow-pending"
              markerWidth="10" markerHeight="7"
              refX="8" refY="3.5"
              orient="auto"
            >
              <polygon points="0 0, 10 3.5, 0 7" fill="#c4cfe6" /></marker>
          </defs>

          <!-- 透明加粗命中区，方便点击细线 -->
          <path
            v-for="(d, i) in connections"
            :key="'hit'+i"
            :d="d"
            fill="none"
            stroke="transparent"
            stroke-width="14"
            style="cursor:pointer"
            @click.stop="onWireClick(i)"
          />

          <!-- 可见连线（底层实线 + 箭头） -->
          <path
            v-for="(d, i) in connections"
            :key="'w'+i"
            :d="d"
            fill="none"
            :stroke="wireColor(i)"
            :stroke-width="activeWireIdx === i ? 4 : 2.5"
            :marker-end="`url(#arrow-${i})`"
            :class="{
              'wire-active': activeWireIdx === i,
              'wire-pending-anim': nodeState(nodes[i+1].id) === 'pending',
            }"
            style="pointer-events:none"
          />

          <!-- 已连通线流动光点叠加层（仅 done 节点呈现） -->
          <template v-for="(d, i) in connections" :key="'flow'+i">
            <path
              v-if="nodeState(nodes[i].id) === 'done'"
              :d="d"
              fill="none"
              :stroke="nodes[i].color"
              stroke-width="3"
              stroke-linecap="round"
              class="wire-flow-done"
              :style="{ animationDelay: `-${i * 0.45}s` }"
              style="pointer-events:none"
            />
          </template>

          <!-- 引脚圆点 -->
          <template v-for="(n, i) in nodes" :key="'p'+n.id">
            <circle v-if="i>0"
              :cx="inPin(n).x" :cy="inPin(n).y" :r="PIN_R"
              :fill="nodeState(nodes[i-1].id)==='done' ? nodes[i-1].color : '#c0cce0'"
              stroke="#e8ecf2" stroke-width="2"
            />
            <circle v-if="i<nodes.length-1"
              :cx="outPin(n).x" :cy="outPin(n).y" :r="PIN_R"
              :fill="nodeState(n.id)==='done' ? n.color : '#c0cce0'"
              stroke="#e8ecf2" stroke-width="2"
            />
          </template>
        </svg>

        <!-- 节点卡片 -->
        <div
          v-for="n in nodes"
          :key="n.id"
          class="bp-node"
          :class="[
            'bp-node--' + nodeState(n.id),
            { 'bp-node--selected': selectedNodeId === n.id && drawerOpen },
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
            <div v-if="n.id==='ai'      && aiModel"      class="bp-node-info">{{ aiModelOptions.find(m=>m.value===aiModel)?.label }} · {{ aiDepthOptions.find(d=>d.value===aiDepth)?.label }}</div>
            <div v-if="n.id==='confirm' && confirmResult" class="bp-node-info" :style="{color: confirmResult==='pass'?'#52c41a':'#f5222d'}">
              {{ confirmResult === 'pass' ? '✓ 审查通过' : '↩ 打回重改' }}
            </div>
            <div v-if="n.id==='fix' && fixDone"          class="bp-node-info">{{ fixBranch }}</div>
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
      :title="nodes.find(n => n.id === selectedNodeId)?.title ?? '节点配置'"
      placement="right"
      :width="420"
      :mask="false"
      :header-style="{ background:'#fff', borderBottom:'1px solid #f0f0f0' }"
      :body-style="{ background:'#fafafa', padding:'20px' }"
      class="bp-drawer"
      @close="drawerOpen = false"
    >
      <!-- 项目配置 -->
      <template v-if="selectedNodeId === 'project'">
        <!-- 可滚动内容区 -->
        <div class="project-scroll-content">
          <a-radio-group
            :value="repoMode" button-style="solid" size="small"
            style="margin-bottom:18px"
            @change="(e: any) => switchRepoMode(e.target.value)"
            :disabled="isExecuting || aiExecutedSuccessfully"
          >
            <a-radio-button value="select">选择项目</a-radio-button>
            <a-radio-button value="custom">自定义 URL</a-radio-button>
          </a-radio-group>

          <a-form layout="vertical" class="bp-form">
            <template v-if="repoMode === 'select'">
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
                <a-form-item v-if="readonlyUrl"    label="Git URL"><a-input :value="readonlyUrl"    disabled /></a-form-item>
                <a-form-item v-if="readonlyBranch" label="Branch"> <a-input :value="readonlyBranch" disabled /></a-form-item>
                <a-form-item v-if="readonlyCommit" label="Commit"> <a-input :value="readonlyCommit" disabled /></a-form-item>
              </template>
            </template>
            <template v-else>
              <a-form-item label="Git URL">
                <a-input
                  v-model:value="repoUrl"
                  placeholder="https://xxx.git"
                  allow-clear
                  :disabled="isExecuting || aiExecutedSuccessfully"
                />
              </a-form-item>
              <a-form-item label="Branch">
                <a-select
                  v-model:value="branch"
                  placeholder="选择分支"
                  allow-clear
                  show-search
                  :loading="loadingBranches"
                  :disabled="branchDisabled || isExecuting || aiExecutedSuccessfully"
                  :options="branchOptions"
                  :get-popup-container="getPopupContainer"
                  @search="onBranchSearch"
                  @dropdownVisibleChange="(open: boolean) => onBranchDropdown(open)"
                />
              </a-form-item>
              <a-form-item label="Commit">
                <a-select
                  v-model:value="commitId"
                  placeholder="选择 Commit"
                  allow-clear
                  show-search
                  :loading="loadingCommits"
                  :disabled="commitDisabled || isExecuting || aiExecutedSuccessfully"
                  :options="commitOptions"
                  :get-popup-container="getPopupContainer"
                  @search="onCommitSearch"
                  @dropdownVisibleChange="(open: boolean) => onCommitDropdown(open)"
                />
              </a-form-item>
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
                :custom-request="(opt: any) => handleCustomUpload(opt)"
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
      <template v-else-if="selectedNodeId === 'ai'">
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

          <a-form-item>
            <template #label>
              <span>
                分析深度
                <a-tooltip placement="right">
                  <template #title>
                    影响代码图谱遍历范围：<br/>
                    《快速》——仅分析直接出错的函数；<br/>
                    《标准》——包含一层调用者；<br/>
                    《深度》——扫描全量调用链（耗时较长）
                  </template>
                  <QuestionCircleOutlined style="margin-left:4px;color:#999;cursor:help" />
                </a-tooltip>
              </span>
            </template>
            <a-segmented
              v-model:value="aiDepth"
              :options="aiDepthOptions"
              block
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
      <template v-else-if="selectedNodeId === 'confirm'">
        <template v-if="aiConfigDone">
          <!-- AI 分析结果占位（后续接入右侧真实返回） -->
          <div class="bp-result-box">
            <div class="bp-result-box-title">🔍 AI 分析摘要</div>
            <div class="bp-result-box-body">
              <div class="bp-result-line">根本原因：<em>PayServiceImpl.execute:142 订单获取 null 返回导致 NPE</em></div>
              <div class="bp-result-line">涉及文件：<em>PayServiceImpl.java、OrderController.java</em></div>
              <div class="bp-result-line">修复建议：<em>在 execute() 函数入口添加非空校验，或将返回类型改为 Optional</em></div>
            </div>
          </div>
          <a-form layout="vertical" class="bp-form" style="margin-top:16px">
            <a-form-item label="审查结果">
              <a-radio-group v-model:value="confirmResult" button-style="solid">
                <a-radio-button value="pass"  >✓ 审查通过</a-radio-button>
                <a-radio-button value="rework">↩ 打回重改</a-radio-button>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="备注（可选）">
              <a-textarea v-model:value="confirmNote" :rows="3" placeholder="补充说明..." />
            </a-form-item>
          </a-form>
        </template>
      </template>

      <!-- 修复执行 -->
      <template v-else-if="selectedNodeId === 'fix'">
        <a-form layout="vertical" class="bp-form">
          <a-form-item label="分支名">
            <a-input v-model:value="fixBranch" placeholder="fix/BUG-1001-pay-npe" allow-clear>
              <template #prefix><span style="color:#bbb;font-size:12px">git checkout -b</span></template>
            </a-input>
          </a-form-item>
          <a-form-item label="Commit 信息">
            <a-input v-model:value="fixCommitMsg" placeholder="fix: 修复支付接口 NPE 问题" allow-clear />
          </a-form-item>
          <a-form-item label="CR Reviewer（可选）">
            <a-input v-model:value="fixReviewer" placeholder="@username" allow-clear />
          </a-form-item>
        </a-form>
        <a-alert v-if="fixDone" type="success" show-icon message="已就绪，点击《开始执行》自动提交修复" />
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
            <div class="exec-info-label">分析深度</div>
            <div class="exec-info-value">{{ aiDepthOptions.find(d => d.value === aiDepth)?.label }}</div>
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
              <a-badge :status="isExecuting ? 'processing' : (execChatMsgs.length ? 'success' : 'default')" />
              <span style="font-size:12px;color:rgba(0,0,0,.55)">
                {{ isExecuting ? 'AI 正在分析中…' : (execChatMsgs.length ? '分析完成' : '等待执行') }}
              </span>
            </div>
            <a-button v-if="isExecuting" danger style="margin-top:8px;width:100%" @click="execAbort?.abort();isExecuting=false">终止</a-button>
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
        <a-list :data-source="pagedHistory" :split="true">
          <template #renderItem="{ item }">
            <a-list-item class="hist-item">
              <!-- 左：状态 + 时间 -->
              <div class="hist-col hist-col-status">
                <a-badge :status="historyStatusMap[item.status].color" />
                <span class="hist-status-text">{{ historyStatusMap[item.status].text }}</span>
                <span class="hist-time">{{ item.time }}</span>
              </div>
              <!-- 中：项目 / BugId / 标题 -->
              <div class="hist-col hist-col-main">
                <span class="hist-project">{{ item.project }}</span>
                <a-tag color="blue" class="hist-tag-bugid">{{ item.bugId }}</a-tag>
                <a-tooltip :title="item.bugTitle" placement="topLeft">
                  <span class="hist-bug-title">{{ item.bugTitle }}</span>
                </a-tooltip>
              </div>
              <!-- 右：模型 / 深度 / 耗时 / 操作 -->
              <div class="hist-col hist-col-right">
                <a-tag class="hist-tag-sm">{{ item.model }}</a-tag>
                <a-tag class="hist-tag-sm">深度：{{ item.depth }}</a-tag>
                <span class="hist-duration">{{ item.duration ?? '—' }}</span>
                <a-button size="small" type="link" class="hist-btn" @click="() => { historyVisible = false; router.push(`/bugfix-workflow/bugfix-details/${item.id}`) }">详情</a-button>
                <a-button size="small" type="link" :disabled="item.status === 'running'" class="hist-btn">重运行</a-button>
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
          :total="mockHistory.length"
          :show-size-changer="false"
          size="small"
        />
      </div>
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
.bp-page { height: 100%; display: flex; flex-direction: column; overflow: hidden; }

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
}
.bp-result-box-body { padding: 12px 14px; display: flex; flex-direction: column; gap: 8px; }
.bp-result-line { font-size: 13px; color: rgba(0,0,0,.65); line-height: 1.6; }
.bp-result-line em { font-style: normal; color: #1677ff; font-weight: 500; }

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

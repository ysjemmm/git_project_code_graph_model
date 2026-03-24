<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import ChatPanel from '../components/ChatPanel.vue'
import type { ChatMsg } from '../types'

const route  = useRoute()
const router = useRouter()
const id     = route.params.id as string

// ─── Markdown 渲染 ────────────────────────────────────────────────────────────
marked.setOptions({ breaks: true, gfm: true })
marked.use({
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const langAlias: Record<string, string> = { vue: 'typescript', svelte: 'typescript', html: 'xml' }
      const resolved   = langAlias[lang ?? ''] ?? lang
      const language   = resolved && hljs.getLanguage(resolved) ? resolved : 'plaintext'
      const highlighted = hljs.highlight(text, { language }).value
      return `<pre class="hljs-block"><code class="hljs language-${language}">${highlighted}</code></pre>`
    },
  },
})
function renderMarkdown(text: string): string {
  try { return marked.parse(text) as string } catch { return text }
}

// ─── mock 历史数据（与 BugfixWorkflowPage 保持同源，后续替换为真实 API）─────
interface HistoryRecord {
  id: string
  time: string
  project: string
  bugId: string
  bugTitle: string
  model: string
  depth: string
  status: 'success' | 'failed' | 'running'
  duration?: string
  repoUrl?: string
  branch?: string
  commitId?: string
  aiPrompt?: string
  chatMsgs?: ChatMsg[]
}

const mockHistory: HistoryRecord[] = [
  { id: 'H001', time: '2026-03-24 14:32', project: 'epaas-gateway',   bugId: 'BUG-1001', bugTitle: 'ProjectServiceImpl.processFlow 空指针异常导致 500',  model: 'DeepSeek',      depth: '标准', status: 'success', duration: '2m 14s', repoUrl: 'https://git.example.com/epaas-gateway.git', branch: 'main', commitId: 'a1b2c3d', aiPrompt: '调用 processFlow 接口报 500，日志显示 NPE，堆栈在 ProjectServiceImpl.processFlow:87' },
  { id: 'H002', time: '2026-03-24 11:05', project: 'order-service',   bugId: 'BUG-1003', bugTitle: '订单状态流转异常：已支付订单未触发发货流程',              model: 'Claude-Haiku',  depth: '深度', status: 'success', duration: '4m 07s', repoUrl: 'https://git.example.com/order-service.git',   branch: 'release/1.2', commitId: 'e4f5g6h', aiPrompt: '已支付的订单在状态变更后未触发发货消息，发货表中无对应记录' },
  { id: 'H003', time: '2026-03-23 17:48', project: 'epaas-gateway',   bugId: 'BUG-1006', bugTitle: '用户登录接口返回 403 但权限配置正确',                    model: 'Claude-Sonnet', depth: '标准', status: 'failed',  duration: '1m 52s', repoUrl: 'https://git.example.com/epaas-gateway.git', branch: 'develop', commitId: 'i7j8k9l', aiPrompt: '/auth/login 返回 403，已检查角色配置无误，怀疑是 spring security 过滤器链问题' },
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

const record = computed(() => mockHistory.find(h => h.id === id) ?? null)

// ─── 节点定义（与 BugfixWorkflowPage 相同） ──────────────────────────────────
interface NodeDef { id: string; title: string; desc: string; color: string; x: number; y: number }
const NODE_W = 220
const NODE_H = 120

const nodes = ref<NodeDef[]>([
  { id: 'project',  title: '项目配置',  desc: '选择仓库 · 关联 Bug · 上传附件', color: '#4a9eff', x: 60,   y: 180 },
  { id: 'ai',       title: 'AI 分析',  desc: '配置模型与分析深度',               color: '#722ed1', x: 380,  y: 200 },
  { id: 'confirm',  title: '分析确认',  desc: '人工审查结果',                    color: '#fa8c16', x: 700,  y: 150 },
  { id: 'fix',      title: '修复执行',  desc: '建分支 · 提交 · CR',             color: '#f5222d', x: 1020, y: 190 },
])

// ─── 节点状态（根据历史记录判断） ────────────────────────────────────────────
const statusMap = { success: 'done', failed: 'done', running: 'active' }
function nodeState(id: string): 'done' | 'active' | 'pending' {
  if (!record.value) return 'pending'
  const s = record.value.status
  if (id === 'project') return 'done'
  if (id === 'ai')      return s === 'running' ? 'active' : 'done'
  if (id === 'confirm') return s === 'success' ? 'done' : 'pending'
  if (id === 'fix')     return s === 'success' ? 'done' : 'pending'
  return 'pending'
}
function nodeStatusText(id: string): string {
  const s = nodeState(id)
  if (id === 'ai') {
    const rs = record.value?.status
    if (rs === 'running') return '执行中…'
    if (rs === 'success') return '✓ 已完成'
    if (rs === 'failed')  return '✗ 失败'
    return '未就绪'
  }
  return s === 'done' ? '✓ 已完成' : s === 'active' ? '查看中' : '未执行'
}

// ─── 画布变换 ────────────────────────────────────────────────────────────────
const canvasRef = ref<HTMLElement | null>(null)
const panX  = ref(0)
const panY  = ref(0)
const scale = ref(1)
let isPanning = false, panStartX = 0, panStartY = 0, panStartPanX = 0, panStartPanY = 0
let canvasDragDist = 0

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const factor   = e.deltaY < 0 ? 1.1 : 0.9
  const newScale = Math.min(Math.max(scale.value * factor, 0.15), 3)
  const rect     = canvasRef.value!.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  panX.value = mx - (mx - panX.value) * (newScale / scale.value)
  panY.value = my - (my - panY.value) * (newScale / scale.value)
  scale.value = newScale
}
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
function onPointerMove(e: PointerEvent) {
  if (!isPanning) return
  const dx = e.clientX - panStartX
  const dy = e.clientY - panStartY
  canvasDragDist += Math.abs(dx) + Math.abs(dy)
  panX.value = panStartPanX + dx
  panY.value = panStartPanY + dy
}
function onPointerUp() {
  // 画布空白处点击（非拖动）→ 关闭抽屉
  if (isPanning && canvasDragDist < 5) {
    drawerOpen.value = false
  }
  isPanning = false
}

// ─── 连线 ────────────────────────────────────────────────────────────────────
function outPin(n: NodeDef) { return { x: n.x + NODE_W, y: n.y + NODE_H / 2 } }
function inPin(n: NodeDef)  { return { x: n.x,           y: n.y + NODE_H / 2 } }
const connections = computed(() =>
  nodes.value.slice(0, -1).map((n, i) => {
    const out = outPin(n); const inp = inPin(nodes.value[i + 1])
    const dx = Math.abs(inp.x - out.x) * 0.5
    return `M${out.x},${out.y} C${out.x+dx},${out.y} ${inp.x-dx},${inp.y} ${inp.x},${inp.y}`
  })
)
function wireColor(i: number) {
  const src = nodeState(nodes.value[i].id)
  if (src === 'done') return nodes.value[i].color
  const tgt = nodeState(nodes.value[i + 1].id)
  if (tgt === 'pending') return '#e2e8f4'
  return '#c4cfe6'
}

// ─── 抽屉 ─────────────────────────────────────────────────────────────────────
const drawerOpen     = ref(false)
const selectedNodeId = ref<string | null>(null)

function selectNode(id: string) {
  selectedNodeId.value = id
  drawerOpen.value = true
}
function onNodeClick(id: string) {
  selectNode(id)
}

// ─── AI 进度抽屉 ──────────────────────────────────────────────────────────────
const chatDrawerOpen     = ref(false)
const showThinkingDetail = ref(false)
const chatMsgs           = computed<ChatMsg[]>(() => record.value?.chatMsgs ?? [])

// ─── 步骤标签 ─────────────────────────────────────────────────────────────────
const stepLabels = computed(() => nodes.value.map(n => ({
  id: n.id,
  title: n.title,
  color: n.color,
  state: nodeState(n.id),
})))

function getPopupContainer(el: HTMLElement) {
  return el.closest('.bp-drawer-body') ?? document.body
}

// ─── 画布初始居中 ─────────────────────────────────────────────────────────────
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
  })
})
</script>

<template>
  <div class="bp-page">
    <!-- 顶部工具栏 -->
    <div class="bp-toolbar">
      <div class="bp-toolbar-left">
        <a-button type="text" size="small" @click="router.back()">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <a-divider type="vertical" style="height:16px;margin:0 8px" />
        <span class="bp-toolbar-title">Bugfix 执行详情</span>
        <template v-if="record">
          <a-divider type="vertical" style="height:16px;margin:0 8px" />
          <a-tag :color="record.status === 'success' ? 'success' : record.status === 'failed' ? 'error' : 'processing'" style="margin:0">
            {{ record.status === 'success' ? '成功' : record.status === 'failed' ? '失败' : '进行中' }}
          </a-tag>
          <span style="font-size:13px;color:rgba(0,0,0,.55);margin-left:4px">{{ record.time }}</span>
          <a-tag color="blue" style="margin-left:8px">{{ record.bugId }}</a-tag>
          <a-tooltip :title="record.bugTitle" placement="bottom">
            <span class="bp-toolbar-bugtitle">{{ record.bugTitle }}</span>
          </a-tooltip>
        </template>
      </div>
      <!-- 步骤标签 -->
      <div class="bp-toolbar-steps">
        <template v-for="(s, si) in stepLabels" :key="s.id">
          <div v-if="si > 0" class="bp-step-sep">›</div>
          <span
            class="bp-step-label"
            :class="{
              'bp-step-active': s.state === 'active',
              'bp-step-done':   s.state === 'done',
              'bp-step-pending': s.state === 'pending',
            }"
            @click="onNodeClick(s.id)"
          >
            <span class="bp-step-dot" :style="{ background: s.state !== 'pending' ? s.color : '#d9d9d9' }" />
            {{ s.title }}
          </span>
        </template>
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
        :style="{ transform: `translate(${panX}px,${panY}px) scale(${scale})` }"
      >
        <!-- SVG 连线层 -->
        <svg class="bp-svg">
          <defs>
            <marker v-for="(n, i) in nodes.slice(0,-1)" :key="'marker-'+i"
              :id="'arrow-'+i" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L0,6 L8,3 z" :fill="wireColor(i)" />
            </marker>
          </defs>
          <path
            v-for="(d, i) in connections"
            :key="'wire-'+i"
            :d="d"
            :stroke="wireColor(i)"
            stroke-width="2.5"
            fill="none"
            :marker-end="'url(#arrow-'+i+')'"
            :class="{ 'wire-pending': nodeState(nodes[i+1].id) === 'pending' }"
          />
        </svg>

        <!-- 节点 -->
        <div
          v-for="n in nodes"
          :key="n.id"
          class="bp-node"
          :class="{
            'bp-node-active':   nodeState(n.id) === 'active',
            'bp-node-done':     nodeState(n.id) === 'done',
            'bp-node-pending':  nodeState(n.id) === 'pending',
            'bp-node-selected': selectedNodeId === n.id && drawerOpen,
          }"
          :style="{ left: n.x + 'px', top: n.y + 'px', width: NODE_W + 'px', height: NODE_H + 'px' }"
          @pointerdown.stop
          @click="onNodeClick(n.id)"
        >
          <div class="bp-node-color-bar" :style="{ background: n.color }" />
          <div class="bp-node-body">
            <div class="bp-node-title">{{ n.title }}</div>
            <div class="bp-node-desc">{{ n.desc }}</div>
            <div class="bp-node-status">
              <span
                class="bp-node-status-badge"
                :class="{
                  'badge-done':    nodeState(n.id) === 'done',
                  'badge-active':  nodeState(n.id) === 'active',
                  'badge-pending': nodeState(n.id) === 'pending',
                }"
              >{{ nodeStatusText(n.id) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 节点详情抽屉（只读） -->
    <a-drawer
      v-model:open="drawerOpen"
      placement="right"
      :width="420"
      :mask="false"
      :get-container="false"
      :style="{ position: 'absolute' }"
      :header-style="{ background: '#fafafa', borderBottom: '1px solid #f0f0f0' }"
      :body-style="{ background: '#fafafa', padding: '20px' }"
    >
      <template #title>
        <span style="font-weight:600">
          {{ nodes.find(n => n.id === selectedNodeId)?.title ?? '详情' }}
        </span>
        <a-tag
          v-if="selectedNodeId"
          style="margin-left:8px"
          :color="nodeState(selectedNodeId) === 'done' ? 'success' : nodeState(selectedNodeId) === 'active' ? 'processing' : 'default'"
        >{{ nodeStatusText(selectedNodeId) }}</a-tag>
      </template>

      <div class="project-scroll-content">
        <!-- 项目配置（只读） -->
        <template v-if="selectedNodeId === 'project'">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="项目名称">{{ record?.project ?? '—' }}</a-descriptions-item>
            <a-descriptions-item label="仓库地址">
              <a-tooltip :title="record?.repoUrl">
                <span class="desc-ellipsis">{{ record?.repoUrl ?? '—' }}</span>
              </a-tooltip>
            </a-descriptions-item>
            <a-descriptions-item label="分支">{{ record?.branch ?? '—' }}</a-descriptions-item>
            <a-descriptions-item label="Commit">
              <code style="font-size:12px">{{ record?.commitId ?? '—' }}</code>
            </a-descriptions-item>
            <a-descriptions-item label="关联 Bug">
              <span v-if="record" style="color:#1677ff;font-weight:600">#{{ record.bugId }}</span>
              <span v-else style="color:rgba(0,0,0,.35)">—</span>
              <span v-if="record" style="margin-left:8px;font-size:13px;color:rgba(0,0,0,.65)">{{ record.bugTitle }}</span>
            </a-descriptions-item>
          </a-descriptions>
        </template>

        <!-- AI 分析（只读） -->
        <template v-else-if="selectedNodeId === 'ai'">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="AI 模型">{{ record?.model ?? '—' }}</a-descriptions-item>
            <a-descriptions-item label="分析深度">{{ record?.depth ?? '—' }}</a-descriptions-item>
            <a-descriptions-item label="问题描述">
              <div style="white-space:pre-wrap;font-size:13px;line-height:1.6">{{ record?.aiPrompt || '—' }}</div>
            </a-descriptions-item>
            <a-descriptions-item label="耗时">{{ record?.duration ?? '—' }}</a-descriptions-item>
          </a-descriptions>
          <div style="margin-top:16px;display:flex;justify-content:flex-end">
            <a-button @click="chatDrawerOpen = true">
              <template #icon><component :is="'MessageOutlined'" /></template>
              查看 AI 分析过程
            </a-button>
          </div>
        </template>

        <!-- 分析确认（只读） -->
        <template v-else-if="selectedNodeId === 'confirm'">
          <a-empty v-if="nodeState('confirm') === 'pending'" description="此次执行未到达分析确认节点" />
          <a-descriptions v-else :column="1" size="small" bordered>
            <a-descriptions-item label="审查结果">
              <a-tag color="success">✓ 审查通过</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="备注">—</a-descriptions-item>
          </a-descriptions>
        </template>

        <!-- 修复执行（只读） -->
        <template v-else-if="selectedNodeId === 'fix'">
          <a-empty v-if="nodeState('fix') === 'pending'" description="此次执行未到达修复执行节点" />
          <a-descriptions v-else :column="1" size="small" bordered>
            <a-descriptions-item label="分支名">—</a-descriptions-item>
            <a-descriptions-item label="Commit 信息">—</a-descriptions-item>
          </a-descriptions>
        </template>

        <template v-else>
          <div style="text-align:center;padding:60px 0;color:rgba(0,0,0,.25)">
            <div style="font-size:40px;margin-bottom:12px">🔧</div>
            <div>暂无数据</div>
          </div>
        </template>
      </div>
    </a-drawer>

    <!-- AI 分析过程抽屉（只读） -->
    <a-drawer
      :open="chatDrawerOpen"
      placement="right"
      :width="900"
      :mask="true"
      :mask-closable="true"
      :header-style="{ background:'#fff', borderBottom:'1px solid #f0f0f0', flexShrink: 0 }"
      :body-style="{ padding:'0', height:'100%', display:'flex', flexDirection:'column', overflow:'hidden' }"
      @close="chatDrawerOpen = false"
    >
      <template #title>
        <div style="display:flex;align-items:center;gap:12px">
          <span style="font-size:16px;font-weight:600">AI 分析过程</span>
          <a-divider type="vertical" style="height:20px;margin:0 8px" />
          <a-switch v-model:checked="showThinkingDetail" size="small" />
          <span style="font-size:13px;color:rgba(0,0,0,.65)">展示思考详情</span>
        </div>
      </template>
      <div style="flex:1;min-height:0;overflow:hidden;display:flex;flex-direction:column">
        <ChatPanel
          v-if="chatMsgs.length > 0"
          :chat-messages="chatMsgs"
          :chat-input="''"
          :loading="false"
          :can-send="false"
          :render-markdown="renderMarkdown"
          :show-thinking-detail="showThinkingDetail"
          :hide-input="true"
        />
        <a-empty v-else description="暂无 AI 分析记录" style="margin:auto" />
      </div>
    </a-drawer>
  </div>
</template>

<style scoped>
.bp-page   { height: 100%; display: flex; flex-direction: column; overflow: hidden; }

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
.bp-toolbar-steps { display: flex; align-items: center; gap: 4px; flex-shrink: 0; padding: 0 8px; }
.bp-toolbar-title { font-size: 14px; font-weight: 700; color: #1a1a2e; white-space: nowrap; }
.bp-toolbar-bugtitle {
  font-size: 13px; color: rgba(0,0,0,.65);
  max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  cursor: default;
}

/* ── 步骤标签 ── */
.bp-step-sep   { color: #bbb; font-size: 12px; }
.bp-step-label {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12px; padding: 2px 8px; border-radius: 10px;
  cursor: pointer; transition: background .15s;
  border: 1.5px solid transparent;
}
.bp-step-label:hover         { background: rgba(0,0,0,.04); }
.bp-step-done                { color: #52c41a; border-color: #b7eb8f; background: #f6ffed; }
.bp-step-active              { color: #722ed1; border-color: #d3adf7; background: #f9f0ff; }
.bp-step-pending             { color: rgba(0,0,0,.35); }
.bp-step-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }

/* ── 画布 ── */
.bp-canvas {
  flex: 1; min-height: 0; position: relative;
  background: #f4f6fb;
  background-image: radial-gradient(circle, #c8d3e8 1px, transparent 1px);
  background-size: 24px 24px;
  cursor: grab; overflow: hidden; user-select: none;
}
.bp-world { position: absolute; top: 0; left: 0; transform-origin: 0 0; }

/* SVG 连线层 */
.bp-svg   { position: absolute; top: 0; left: 0; width: 4000px; height: 3000px; pointer-events: none; overflow: visible; }
.wire-pending { stroke-dasharray: 6 4; }

/* ── 节点卡片 ── */
.bp-node {
  position: absolute; border-radius: 14px; background: #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,.09);
  border: 2px solid #e8eef8;
  transition: border-color .2s, box-shadow .2s;
  overflow: hidden; cursor: pointer;
  display: flex;
}
.bp-node:hover     { box-shadow: 0 6px 20px rgba(0,0,0,.14); border-color: #c0cfe8; }
.bp-node-active    { border-color: #722ed1 !important; box-shadow: 0 0 0 3px rgba(114,46,209,.18) !important; }
.bp-node-done      { border-color: #52c41a !important; }
.bp-node-selected  { box-shadow: 0 0 0 3px rgba(22,119,255,.3) !important; }
.bp-node-pending   { opacity: .6; cursor: pointer; }

.bp-node-color-bar { width: 6px; flex-shrink: 0; border-radius: 12px 0 0 12px; }
.bp-node-body      { flex: 1; padding: 14px 16px; display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.bp-node-title     { font-size: 15px; font-weight: 700; color: #1a1a2e; }
.bp-node-desc      { font-size: 12px; color: rgba(0,0,0,.45); line-height: 1.4; }
.bp-node-status    { margin-top: auto; }

.bp-node-status-badge { font-size: 11px; padding: 2px 8px; border-radius: 8px; }
.badge-done    { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }
.badge-active  { background: #f9f0ff; color: #722ed1; border: 1px solid #d3adf7; }
.badge-pending { background: #f5f5f5; color: rgba(0,0,0,.35); border: 1px solid #d9d9d9; }

/* ── 抽屉内容 ── */
.project-scroll-content { max-height: calc(100vh - 200px); overflow-y: auto; padding-right: 4px; }
.desc-ellipsis { display: inline-block; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: middle; }
</style>

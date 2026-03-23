<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { BulbOutlined, CodeOutlined, FileTextOutlined, FolderOpenOutlined, LinkOutlined, SearchOutlined, ToolOutlined } from '@ant-design/icons-vue'
import type { ChatMsg } from '../types'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import type { ToolStep } from '../api'

const props = defineProps<{
  loading: boolean
  chatMessages: ChatMsg[]
  chatInput: string
  canSend: boolean
  /** 发送按钮禁用时的原因，用于 title 提示 */
  sendDisabledReason?: string
  /** 输入框占位符 */
  placeholder?: string
  renderMarkdown: (s: string) => string
}>()

const emit = defineEmits<{
  (e: 'update:chatInput', v: string): void
  (e: 'send'): void
  (e: 'test'): void
  (e: 'cancel'): void
  (e: 'openNeo4j'): void
}>()

const chatInputModel = computed({
  get: () => props.chatInput,
  set: (v) => emit('update:chatInput', v),
})

const chatLogRef = ref<HTMLElement | null>(null)
const msgRefs = ref<HTMLElement[]>([])
const stickyQuestion = ref<string>('')
let scrollRaf: number | null = null

function setMsgRef(idx: number, el: Element | null) {
  if (el) msgRefs.value[idx] = el as HTMLElement
}

function computeStickyQuestion() {
  const container = chatLogRef.value
  if (!container) return
  const scrollTop = container.scrollTop
  let firstIdx = 0
  for (let i = 0; i < msgRefs.value.length; i++) {
    const el = msgRefs.value[i]
    if (!el) continue
    if (el.offsetTop + el.offsetHeight >= scrollTop + 6) {
      firstIdx = i
      break
    }
  }
  let q = ''
  for (let i = firstIdx; i >= 0; i--) {
    if (props.chatMessages[i]?.role === 'user') {
      q = props.chatMessages[i]?.content ?? ''
      break
    }
  }
  stickyQuestion.value = q
}

function onChatScroll() {
  if (programmaticScroll) return
  if (scrollRaf != null) return
  scrollRaf = window.requestAnimationFrame(() => {
    scrollRaf = null
    const el = chatLogRef.value
    if (el) {
      const dist = el.scrollHeight - (el.scrollTop + el.clientHeight)
      // 用户向上滚超过 80px 就认为主动离开底部
      userScrolledAway = dist > 80
    }
    computeStickyQuestion()
  })
}

let resizeObserver: ResizeObserver | null = null
let mutationObserver: MutationObserver | null = null
let programmaticScroll = false
// 只有 AI 正在回复时才允许自动触底
let streamingActive = false
// 用户主动向上滚时设为 true，阻止自动触底
let userScrolledAway = false

// 监听 loading prop 变化：开始时激活自动触底，结束时关闭
watch(() => props.loading, (val) => {
  streamingActive = val
}, { flush: 'sync' })

onMounted(() => {
  const el = chatLogRef.value
  if (!el) return
  el.addEventListener('scroll', onChatScroll, { passive: true })
  computeStickyQuestion()

  resizeObserver = new ResizeObserver(() => {
    if (!streamingActive || userScrolledAway) return
    if (!isNearBottom(150)) return
    scrollToBottomNow()
  })

  // 观察所有现有子元素
  const observeChildren = () => {
    Array.from(el.children).forEach(child => resizeObserver!.observe(child))
  }
  observeChildren()

  // 监听新增子节点，自动加入观察
  mutationObserver = new MutationObserver((mutations) => {
    for (const m of mutations) {
      m.addedNodes.forEach(node => {
        if (node instanceof Element) resizeObserver!.observe(node)
      })
    }
    if (streamingActive && !userScrolledAway && isNearBottom(150)) scrollToBottomNow()
  })
  mutationObserver.observe(el, { childList: true, subtree: false })
})

onUnmounted(() => {
  const el = chatLogRef.value
  if (el) el.removeEventListener('scroll', onChatScroll as any)
  if (scrollRaf != null) window.cancelAnimationFrame(scrollRaf)
  scrollRaf = null
  resizeObserver?.disconnect()
  resizeObserver = null
  mutationObserver?.disconnect()
  mutationObserver = null
})

function openNeo4jAuraQuery() {
  window.open(NEO4J_AURA_QUERY_URL, '_blank', 'noopener,noreferrer')
}

type ReadTag = { file: string; start?: number; end?: number }

function basename(p: string) {
  const s = p.replace(/\\/g, '/')
  return s.split('/').pop() || s
}

function parseReadSourceFileTags(content: string): ReadTag[] {
  // read_source_file 返回内容形如：
  // ```java
  // some/path/Foo.java  (lines 12–34 / 100)
  //    12: ...
  // ```
  // 从 content 抽取 “(lines start–end / total)”。
  const tags: ReadTag[] = []
  const re = /([^\n()]+?)\s*\(lines\s+(\d+)\s*[–-]\s*(\d+)\s*\/\s*(\d+)\)/g
  let m: RegExpExecArray | null
  while ((m = re.exec(content)) !== null) {
    const file = String(m[1]).trim()
    const start = Number(m[2])
    const end = Number(m[3])
    if (!file) continue
    tags.push({ file, start, end })
  }
  return tags
}

function parseReadTagFromTitle(step: ToolStep): ReadTag[] {
  const title = step?.title ?? ''
  const m = title.match(/(查看文件|读取文件)[:：]\s*(.+)\s*$/)
  if (!m?.[2]) return []
  return [{ file: String(m[2]).trim() }]
}

function getReadTags(step: ToolStep): ReadTag[] {
  if (!step) return []
  if (step.kind === 'read_source_file') {
    if (!step.content) return []
    return parseReadSourceFileTags(step.content)
  }
  if (step.kind === 'read_uploaded_file' || step.kind === 'file_read') {
    const fromTitle = parseReadTagFromTitle(step)
    if (fromTitle.length) return fromTitle
    if (step.kind === 'file_read' && step.content)
      return parseReadSourceFileTags(step.content)
    return []
  }
  return []
}

function getToolIcon(kind: string) {
  if (kind === 'read_uploaded_file' || kind === 'file_read') return FileTextOutlined
  if (kind === 'list_uploaded_files' || kind === 'file_list') return FolderOpenOutlined
  if (kind === 'query_code_graph' || kind === 'graph_query') return SearchOutlined
  if (kind === 'reasoning') return BulbOutlined
  if (kind === 'read_source_file') return CodeOutlined
  return ToolOutlined
}

function getToolTitle(step: ToolStep): string {
  const kind = step?.kind ?? ''
  if (kind === 'read_source_file') return '读取源代码文件'
  if (kind === 'read_uploaded_file' || kind === 'file_read') {
    // 文件名放在 tag 里展示，标题只显示「读取文件」不重复
    return '读取文件'
  }
  if (kind === 'list_uploaded_files' || kind === 'file_list') return '读取文件列表'
  if (kind === 'query_code_graph' || kind === 'graph_query') return '代码图谱查询'
  if (kind === 'search_code') return '代码搜索'
  if (kind === 'reasoning') return '思考过程'
  return step?.title ?? ''
}

/** 思考过程里展示的「目的」说明：代码图谱查询从 content 解析，代码搜索用 summary 字段 */
function getQuerySummary(step: ToolStep): string {
  if (step?.kind === 'search_code') return (step as { summary?: string }).summary ?? ''
  if (step?.kind !== 'query_code_graph' && step?.kind !== 'graph_query') return ''
  const m = (step?.content ?? '').match(/查询目的[\s\S]*?\n+([^\n🔎]+)/)
  return m?.[1]?.trim() ?? ''
}

/** 工具节点的详细说明：文件名、行号范围、用途等，与 getQuerySummary 互补使用。有 tag 时不再返回与 tag 重复的文案。 */
function getToolStepDetail(step: ToolStep): string {
  if (!step) return ''
  const kind = step.kind ?? ''
  const tags = getReadTags(step)
  if (tags.length) return '' // 已有 tag 展示文件/行号，不再重复
  const title = (step.title ?? '').trim()
  if (kind === 'read_uploaded_file' || kind === 'file_read') {
    const m = title.match(/(?:读取文件|查看文件)[:：]\s*(.+)/)
    if (m?.[1]) return m[1].trim()
    return ''
  }
  if (kind === 'reasoning') {
    const firstLine = (step.content ?? '').split('\n')[0]?.trim()
    if (firstLine && firstLine.length > 60) return firstLine.slice(0, 57) + '...'
    return firstLine ?? ''
  }
  return ''
}

const FILTERED_TOOL_KINDS = ['file_list', 'list_uploaded_files']

/** 从消息中按顺序收集所有工具步骤（支持 segments 与旧版 toolSteps） */
function getMessageToolSteps(msg: import('../types').ChatMsg): import('../api/bugfix').ToolStep[] {
  let steps: import('../api/bugfix').ToolStep[]
  if (msg.segments?.length) {
    steps = msg.segments.filter((s): s is { type: 'tool'; steps: import('../api/bugfix').ToolStep[] } => s.type === 'tool').flatMap(s => s.steps)
  } else {
    steps = msg.toolSteps ?? []
  }
  return steps.filter(s => !FILTERED_TOOL_KINDS.includes(s.kind))
}

/** 按 analysisChain 步骤的 tool_kind 字段，从 toolSteps 里找到对应的工具步骤。
 *  同一 kind 可能出现多次，按该 kind 在 analysisChain[0..ci] 中出现的次序取对应的 toolStep。
 */
function getChainToolStep(msg: import('../types').ChatMsg, ci: number): import('../api/bugfix').ToolStep | null {
  const chainStep = msg.analysisChain?.[ci]
  const kind = chainStep?.tool_kind
  if (!kind) return null

  const occurrence = (msg.analysisChain ?? []).slice(0, ci).filter(s => s.tool_kind === kind).length
  const steps = getMessageToolSteps(msg)

  let count = 0
  for (const step of steps) {
    if (step.kind === kind) {
      if (count === occurrence) return step
      count++
    }
  }
  return null
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey && !e.altKey) {
    e.preventDefault()
    if (!props.loading && props.canSend) emit('send')
  }
  // Ctrl+Enter 插入换行，默认行为即可，不拦截
}

async function scrollToBottom() {
  await nextTick()
  if (!streamingActive || userScrolledAway) return
  scrollToBottomNow()
}

async function forceScrollToBottom() {
  userScrolledAway = false  // 重置：发新消息时恢复自动触底
  await nextTick()
  scrollToBottomNow()
}

function scrollToBottomNow() {
  const el = chatLogRef.value
  if (!el) return
  programmaticScroll = true
  el.scrollTo({ top: el.scrollHeight })
  // 下一帧之后恢复监听，避免把程序滚动误判为用户滚动
  requestAnimationFrame(() => { programmaticScroll = false })
  computeStickyQuestion()
}

function isNearBottom(thresholdPx = 10) {
  const el = chatLogRef.value
  if (!el) return true
  const dist = el.scrollHeight - (el.scrollTop + el.clientHeight)
  return dist <= thresholdPx
}

async function maybeScrollToBottom(thresholdPx = 120) {
  await nextTick()
  if (!isNearBottom(thresholdPx)) return
  scrollToBottomNow()
}

const chatInputRef = ref<{ focus: () => void } | null>(null)
function focusInput() {
  const el = chatInputRef.value as any
  if (el?.focus) el.focus()
  else if (el?.$el?.querySelector) el.$el.querySelector('textarea')?.focus()
}

defineExpose({ scrollToBottom, forceScrollToBottom, maybeScrollToBottom, isNearBottom, focusInput })
</script>

<template>
  <a-card class="panel panel-chat" title="AI 对话">
    <div ref="chatLogRef" class="chat-log">
      <div v-if="stickyQuestion" class="sticky-q">
        <div class="sticky-text">{{ stickyQuestion }}</div>
      </div>

      <div
        v-for="(m, idx) in chatMessages"
        :key="idx"
        class="msg"
        :class="m.role"
        :ref="(el) => setMsgRef(idx, el as any)"
      >
        <div v-if="m.role === 'user'" class="bubble">
          <div class="text">{{ m.content }}</div>
        </div>

        <template v-if="m.role === 'ai'">
          <div class="ai-msg-body">
            <!-- 交错模式：一句文字 → 该句用的 tool → 下一句 → tool … -->
            <template v-if="m.segments?.length">
              <template v-for="(seg, segIdx) in m.segments" :key="'seg-' + idx + '-' + segIdx">
                <div v-if="seg.type === 'text'" class="bubble" :class="{ 'bubble-streaming': m.streaming && segIdx === m.segments.length - 1 }">
                  <div class="md-body" v-html="renderMarkdown(seg.content)" />
                </div>
                <div v-else-if="seg.type === 'tool' && seg.steps.length" class="thinking-wrap" :class="{ 'thinking-active': loading && idx === chatMessages.length - 1 && segIdx === m.segments.length - 1 }">
                  <a-collapse :default-active-key="[]" ghost>
                    <a-collapse-panel
                      v-for="(step, si) in seg.steps"
                      :key="'step-' + idx + '-' + segIdx + '-' + si"
                      :class="{ 'tool-running': loading && idx === chatMessages.length - 1 && segIdx === m.segments.length - 1 && si === seg.steps.length - 1 }"
                    >
                      <template #header>
                        <div class="tool-header">
                          <div class="tool-header-main">
                            <div class="tool-header-main-row">
                              <span class="tool-icon">
                                <component :is="getToolIcon(step.kind)" />
                              </span>
                              <span class="tool-header-text">{{ getToolTitle(step) }}</span>
                              <a-button
                                v-if="step.kind === 'query_code_graph' || step.kind === 'graph_query'"
                                type="text"
                                size="small"
                                class="tool-link-btn"
                                title="打开 Neo4j Aura 控制台"
                                @click.stop="openNeo4jAuraQuery"
                              >
                                <template #icon><LinkOutlined /></template>
                              </a-button>
                            </div>
                            <span v-if="getQuerySummary(step) || getToolStepDetail(step)" class="tool-query-summary">{{ getQuerySummary(step) || getToolStepDetail(step) }}</span>
                          </div>

                          <div v-if="getReadTags(step).length" class="tool-tags">
                            <a-tag
                              v-for="t in getReadTags(step)"
                              :key="t.file + ':' + (t.start ?? '-') + '-' + (t.end ?? '-')"
                              :title="t.file"
                              class="tool-file-tag"
                            >
                              <span class="tool-tag-file">{{ basename(t.file) }}</span>
                              <span v-if="t.start != null && t.end != null" class="tool-tag-range">
                                行{{ t.start }}-{{ t.end }}
                              </span>
                            </a-tag>
                          </div>
                        </div>
                      </template>
                      <div class="tool-content md-body" v-html="renderMarkdown(step.content)" />
                    </a-collapse-panel>
                  </a-collapse>
                </div>
              </template>
            </template>

            <!-- 兼容旧结构：整段 content + 整块 toolSteps -->
            <template v-else>
              <div v-if="m.content || m.streaming" class="bubble" :class="{ 'bubble-streaming': m.streaming }">
                <div class="md-body" v-html="renderMarkdown(m.content)" />
              </div>

              <div
                v-if="loading && idx === chatMessages.length - 1 && !m.content && !m.streaming"
                class="bubble thinking-pulse"
              >
                <span class="dot" /><span class="dot" /><span class="dot" />
              </div>

              <div v-if="m.toolSteps?.length" class="thinking-wrap" :class="{ 'thinking-active': loading && idx === chatMessages.length - 1 }">
                <a-collapse :default-active-key="[]" ghost>
                  <a-collapse-panel
                    v-for="(step, si) in m.toolSteps"
                    :key="'step-' + idx + '-' + si"
                    :class="{ 'tool-running': loading && idx === chatMessages.length - 1 && si === m.toolSteps.length - 1 }"
                  >
                    <template #header>
                      <div class="tool-header">
                        <div class="tool-header-main">
                          <div class="tool-header-main-row">
                            <span class="tool-icon">
                              <component :is="getToolIcon(step.kind)" />
                            </span>
                            <span class="tool-header-text">{{ getToolTitle(step) }}</span>
                            <a-button
                              v-if="step.kind === 'query_code_graph' || step.kind === 'graph_query'"
                              type="text"
                              size="small"
                              class="tool-link-btn"
                              title="打开 Neo4j Aura 控制台"
                              @click.stop="openNeo4jAuraQuery"
                            >
                              <template #icon><LinkOutlined /></template>
                            </a-button>
                          </div>
                          <span v-if="getQuerySummary(step) || getToolStepDetail(step)" class="tool-query-summary">{{ getQuerySummary(step) || getToolStepDetail(step) }}</span>
                        </div>

                        <div v-if="getReadTags(step).length" class="tool-tags">
                          <a-tag
                            v-for="t in getReadTags(step)"
                            :key="t.file + ':' + (t.start ?? '-') + '-' + (t.end ?? '-')"
                            :title="t.file"
                            class="tool-file-tag"
                          >
                            <span class="tool-tag-file">{{ basename(t.file) }}</span>
                            <span v-if="t.start != null && t.end != null" class="tool-tag-range">
                              行{{ t.start }}-{{ t.end }}
                            </span>
                          </a-tag>
                        </div>
                      </div>
                    </template>
                    <div class="tool-content md-body" v-html="renderMarkdown(step.content)" />
                  </a-collapse-panel>
                </a-collapse>
              </div>
            </template>

            <details v-if="m.analysisChain?.length" class="analysis-chain-wrap">
            <summary class="analysis-chain-summary">
              <span class="analysis-chain-title">分析链路</span>
              <span class="analysis-chain-count">{{ m.analysisChain.length }} 步</span>
            </summary>
            <div class="analysis-chain-steps">
              <template v-for="(chainStep, ci) in m.analysisChain" :key="'chain-' + idx + '-' + ci">
                <div class="analysis-chain-node" :class="chainStep.type">
                  <div class="acn-left">
                    <span class="acn-num">{{ ci + 1 }}</span>
                    <span v-if="ci < m.analysisChain.length - 1" class="acn-line" />
                  </div>
                  <div class="acn-body">
                    <span class="acn-label">{{ chainStep.label }}</span>
                    <span v-if="chainStep.detail" class="acn-detail">{{ chainStep.detail }}</span>
                    <details
                      v-if="getChainToolStep(m, ci)"
                      class="acn-tool-detail"
                    >
                      <summary class="acn-tool-summary">
                        <span class="acn-tool-icon"><component :is="getToolIcon(getChainToolStep(m, ci)!.kind)" /></span>
                        <span>{{ getToolTitle(getChainToolStep(m, ci)!) }}</span>
                        <span class="acn-tool-expand-hint">展开查看</span>
                      </summary>
                      <div class="acn-tool-content md-body" v-html="renderMarkdown(getChainToolStep(m, ci)!.content)" />
                    </details>
                  </div>
                </div>
              </template>
            </div>
          </details>
          </div>
        </template>
      </div>

      <div
        v-if="loading && (chatMessages.length === 0 || chatMessages[chatMessages.length - 1].role === 'user')"
        class="msg ai"
      >
        <div class="bubble thinking-pulse">
          <span class="dot" /><span class="dot" /><span class="dot" />
        </div>
      </div>
    </div>

    <div class="chat-input">
      <div class="toolbar">
        <div v-if="loading" class="chat-keepalive" aria-live="polite">
          <span class="keepalive-dots" aria-hidden="true">
            <span class="dot dot1" />
            <span class="dot dot2" />
            <span class="dot dot3" />
          </span>
          <span class="chat-keepalive-text">还在继续…</span>
        </div>
        <div v-else />
        <div class="toolbar-buttons">
          <div class="toolbar-action-row">
            <a-button
              class="toolbar-button"
              type="primary"
              :disabled="loading || !canSend"
              :title="(loading || !canSend) && sendDisabledReason ? sendDisabledReason : undefined"
              @click="emit('send')"
            >
              发送
              <span class="kbd-enter"><span>↵</span></span>
            </a-button>
            <a-button class="toolbar-test-button" :disabled="loading" @click="emit('test')" title="模拟 AI 流式回复（后端：test-analyze）">
              测试回复
            </a-button>
            <a-button v-if="loading" danger class="toolbar-cancel-button" @click="emit('cancel')">
              结束
            </a-button>
          </div>
        </div>
      </div>
      <a-textarea
        ref="chatInputRef"
        v-model:value="chatInputModel"
        :auto-size="{ minRows: 3, maxRows: 6 }"
        :placeholder="placeholder ?? '输入你的问题… （Enter 发送，Ctrl+Enter 换行）'"
        @keydown="onKeydown"
      />
    </div>
  </a-card>
</template>


<style scoped>
.chat-input .toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.chat-input .toolbar-buttons {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.chat-input .toolbar-action-row {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 10px;
}

.chat-keepalive {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.45);
  user-select: none;
}

.chat-keepalive-text {
  font-size: 12px;
  font-weight: 400;
  white-space: nowrap;
}

.keepalive-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.keepalive-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(22, 119, 255, 0.9);
  animation: keepalive-bounce 1.1s ease-in-out infinite;
}

.keepalive-dots .dot2 { animation-delay: 0.12s; }
.keepalive-dots .dot3 { animation-delay: 0.24s; }

@keyframes keepalive-bounce {
  0%, 100% { transform: translateY(0); opacity: 0.6; }
  50% { transform: translateY(-4px); opacity: 1; }
}

.chat-input .toolbar-button{
  width: 100px;
}
.chat-input .toolbar-test-button{
  width: 100px;
}
.chat-input .toolbar-cancel-button{
  width: 80px;
}
.kbd-enter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0px 0px 3px 6px;
  padding: 1px 6px;
  width: 24px;
  height: 20px;
  font-size: 13px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  color: inherit;
  font-weight: bolder;
  opacity: 0.6;
  background: rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-bottom: 2px solid rgba(0, 0, 0, 0.25);
  border-radius: 4px;
  line-height: 1;
  vertical-align: middle;
  overflow: hidden;
}
.kbd-enter > span {
  display: inline-block;
  transform: scale(1.5);
  line-height: 1;
}

/* AI 消息整体：消息在上、工具在下，统一左对齐 */
.ai-msg-body {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}
.ai-msg-body > .thinking-wrap,
.ai-msg-body > .analysis-chain-wrap {
  margin-left: 0;
  margin-right: 0;
}

/* 整个 thinking-wrap 执行中：左侧蓝色呼吸边框 */
.thinking-active {
  border-left: 2px solid #1677ff;
  padding-left: 8px;
  animation: thinking-breathe 1.8s ease-in-out infinite;
}
@keyframes thinking-breathe {
  0%, 100% { border-left-color: #1677ff; }
  50% { border-left-color: #69b1ff; }
}

/* 分析链路：固定展开，垂直时间线风格 */
.analysis-chain-wrap {
  margin-top: 10px;
  margin-bottom: 6px;
  width: 100%;
  max-width: 85%;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.07);
  border-radius: 10px;
  overflow: hidden;
}
.analysis-chain-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-bottom: 1px solid transparent;
  background: rgba(0, 0, 0, 0.02);
  cursor: pointer;
  user-select: none;
  list-style: none;
}
.analysis-chain-summary::-webkit-details-marker { display: none; }
.analysis-chain-summary::before {
  content: '▶';
  font-size: 9px;
  color: rgba(0, 0, 0, 0.3);
  transition: transform 0.15s;
  flex-shrink: 0;
}
details[open].analysis-chain-wrap > .analysis-chain-summary::before {
  transform: rotate(90deg);
}
details[open].analysis-chain-wrap > .analysis-chain-summary {
  border-bottom-color: rgba(0, 0, 0, 0.06);
}
.analysis-chain-title {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.5);
  letter-spacing: 0.3px;
  text-transform: uppercase;
}
.analysis-chain-count {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.3);
  font-weight: 400;
}
.analysis-chain-steps {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.analysis-chain-node {
  display: flex;
  align-items: flex-start;
  gap: 0;
  min-height: 36px;
}
.acn-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 28px;
  margin-right: 12px;
}
.acn-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.08);
  font-size: 11px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.45);
  flex-shrink: 0;
  line-height: 1;
}
.analysis-chain-node.problem .acn-num {
  background: rgba(22, 119, 255, 0.15);
  color: #1677ff;
}
.acn-line {
  display: block;
  width: 2px;
  flex: 1;
  min-height: 10px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: 1px;
  margin-top: 4px;
}
.acn-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 14px;
  min-width: 0;
  flex: 1;
}
.analysis-chain-node:last-child .acn-body {
  padding-bottom: 0;
}
.acn-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.82);
  line-height: 22px;
}
.acn-detail {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 分析链路：工具 input/output 展开区 */
.acn-tool-detail {
  margin-top: 6px;
  border: 1px solid rgba(0, 0, 0, 0.07);
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.acn-tool-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
  cursor: pointer;
  user-select: none;
  list-style: none;
  background: rgba(0, 0, 0, 0.02);
}
.acn-tool-summary::-webkit-details-marker { display: none; }
.acn-tool-summary::before {
  content: '▶';
  font-size: 9px;
  color: rgba(0, 0, 0, 0.3);
  transition: transform 0.15s;
  flex-shrink: 0;
}
details[open] .acn-tool-summary::before {
  transform: rotate(90deg);
}
.acn-tool-icon {
  display: inline-flex;
  align-items: center;
  color: rgba(0, 0, 0, 0.4);
  font-size: 12px;
}
.acn-tool-expand-hint {
  margin-left: auto;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.3);
}
details[open] .acn-tool-expand-hint {
  display: none;
}
.acn-tool-content {
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: rgba(0, 0, 0, 0.01);
  word-break: break-word;
}

/* 最后一个正在执行的 tool panel 标题行：扫光动画 */
:deep(.tool-running) > .ant-collapse-header {
  position: relative;
  overflow: hidden;
  background: rgba(22, 119, 255, 0.04) !important;
}
:deep(.tool-running) > .ant-collapse-header::after {
  content: '';
  position: absolute;
  top: 0; left: -60%; width: 60%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(22,119,255,0.15), transparent);
  animation: tool-shimmer 1.6s ease-in-out infinite;
}
@keyframes tool-shimmer {
  0% { left: -60%; }
  100% { left: 120%; }
}

.tool-header {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
}

.tool-header-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.tool-header-main-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.tool-icon {
  display: inline-flex;
  align-items: center;
  color: rgba(0, 0, 0, 0.45);
}

.tool-icon :deep(svg) {
  width: 14px;
  height: 14px;
  display: block;
}

.tool-tags {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  overflow-x: auto;
  max-width: 100%;
  padding-left: 0;
  white-space: nowrap;
}

.tool-tag-file {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 400;
  font-size: 12px;
}

.tool-tag-range {
  white-space: nowrap;
  margin-left: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #91caff;
}

.tool-header-text {
  white-space: nowrap;
  line-height: 1;
}

.tool-query-summary {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 让 Ant Collapse 的箭头与自定义 header 内容垂直居中 */
:deep(.thinking-wrap .ant-collapse-header) {
  align-items: center;
}

:deep(.tool-tags .ant-tag) {
  padding: 4px 10px !important;
  border-radius: 8px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

:deep(.tool-file-tag) {
  background: #e8f4ff !important;
  border-color: #91caff !important;
  color: #4096ff !important;
}
</style>

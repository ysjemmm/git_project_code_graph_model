<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { runForwardProcessflowNpeFixSse, type BugfixSseEvent, type ToolStep } from '../api'
import { modelOptions, parseModelChoice } from '../constants'
import { useRepo } from '../composables/useRepo'
import ChatPanel from '../components/ChatPanel.vue'
import type { ChatMsg } from '../types'

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

// ─── 项目 / 模型选择 ──────────────────────────────────────────────────────────
const repoState = useRepo({ projectSource: 'cacheApplicationProjects' })
const { repoOptions, selectedProjectName, readonlyUrl, branch, commitId, loadingRepos, fetchRepos } = repoState
const selectedModel = ref<string>('claude::claude-haiku-4-5-20251001')

onMounted(() => { void fetchRepos() })

// ─── 对话状态（与 BugfixChatPage 完全一致） ───────────────────────────────────
const loading = ref(false)
const chatMessages = ref<ChatMsg[]>([
  { role: 'ai', content: '这是 AI 工具单测对话，选好项目和模型后直接发消息，AI 会走完整的工具调用流程。' },
])
const currentToolSteps = ref<ToolStep[]>([])
const chatInput = ref('')
const streamingAiIndex = ref<number | null>(null)
const streamQueue = ref<Array<{ kind: 'text' | 'code'; content: string }>>([])
let diffFlushTimer: number | undefined
const streamKind = ref<'text' | 'code' | null>(null)
const activeAbort = ref<AbortController | null>(null)
const manualCancelled = ref(false)
const chatPanelRef = ref<InstanceType<typeof ChatPanel> | null>(null)

const canSend = computed(() => !loading.value && Boolean(selectedProjectName.value))
const sendDisabledReason = computed(() => {
  if (loading.value) return '请求进行中…'
  if (!selectedProjectName.value) return '请先选择项目'
  return ''
})

function scrollToBottom() {
  void chatPanelRef.value?.scrollToBottom?.()
}

function appendToStreamingBubble(chunk: string, kind: 'text' | 'code' = 'text') {
  if (streamingAiIndex.value === null || streamKind.value !== kind) {
    const leadingTools = currentToolSteps.value.length ? [...currentToolSteps.value] : []
    currentToolSteps.value = []
    const segments: import('../types/chat').ChatMsgSegment[] = []
    leadingTools.forEach((s) => segments.push({ type: 'tool', steps: [s] }))
    segments.push({ type: 'text', content: chunk })
    chatMessages.value.push({ role: 'ai', content: chunk, segments, streaming: true })
    streamingAiIndex.value = chatMessages.value.length - 1
    streamKind.value = kind
  } else {
    const msg = chatMessages.value[streamingAiIndex.value]
    if (msg.segments?.length) {
      const last = msg.segments[msg.segments.length - 1]
      if (last.type === 'text') last.content += chunk
      else msg.segments.push({ type: 'text', content: chunk })
      msg.content += chunk
    } else {
      msg.content += chunk
    }
  }
  scrollToBottom()
}

function formatUnifiedDiff(content: string) {
  const raw = String(content ?? '')
  if (!raw.trim() || raw.includes('```')) return raw
  let add = 0, del = 0
  for (const line of raw.split('\n')) {
    if (line.startsWith('+++') || line.startsWith('---')) continue
    if (line.startsWith('+')) add++
    else if (line.startsWith('-')) del++
  }
  return `新增 ${add} 行，删除 ${del} 行\n\n\`\`\`diff\n${raw}\n\`\`\``
}

function ensureDiffFlusherRunning() {
  if (diffFlushTimer) return
  diffFlushTimer = window.setInterval(() => {
    if (streamQueue.value.length === 0) {
      window.clearInterval(diffFlushTimer); diffFlushTimer = undefined; return
    }
    const item = streamQueue.value.shift()
    if (item) appendToStreamingBubble(item.content + (item.kind === 'code' ? '\n' : ''), item.kind)
  }, 20)
}

function buildOnEvent(pendingChain: { value: import('../api').AnalysisChainStep[] | null }) {
  return (ev: BugfixSseEvent) => {
    if (ev.type === 'status') return

    if (ev.type === 'analysis_chain') {
      const steps = ev.steps ?? []
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) msg.analysisChain = steps
      } else {
        pendingChain.value = steps
      }
      scrollToBottom(); return
    }

    if (ev.type === 'tool') {
      if (ev.kind === 'file_list') return
      const step = { kind: ev.kind, title: ev.title, content: ev.content, summary: ev.summary }
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg?.segments?.length) {
          const last = msg.segments[msg.segments.length - 1]
          if (last.type === 'tool') last.steps.push(step)
          else msg.segments.push({ type: 'tool', steps: [step] })
        } else if (msg) {
          msg.toolSteps = [...(msg.toolSteps ?? []), step]
        }
      } else {
        currentToolSteps.value.push(step)
      }
      scrollToBottom(); return
    }

    if (ev.type === 'text_chunk') {
      const chunk = ev.content ?? ''
      if (chunk) {
        appendToStreamingBubble(chunk, 'text')
        if (streamingAiIndex.value !== null && pendingChain.value?.length) {
          const msg = chatMessages.value[streamingAiIndex.value]
          if (msg) msg.analysisChain = pendingChain.value
          pendingChain.value = null
        }
      }
      return
    }

    if (ev.type === 'text') {
      const t = (ev.content ?? '').trimEnd()
      if (t) { streamQueue.value.push({ kind: 'text', content: t }); ensureDiffFlusherRunning() }
      return
    }

    if (ev.type === 'diff') {
      streamQueue.value.push({ kind: 'code', content: formatUnifiedDiff(ev.content ?? '') })
      ensureDiffFlusherRunning(); return
    }

    if (ev.type === 'error') {
      loading.value = false
      chatMessages.value.push({ role: 'ai', content: `[错误] ${ev.content ?? '未知错误'}` })
      currentToolSteps.value = []; return
    }

    if (ev.type === 'done') {
      loading.value = false
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) { msg.streaming = false; if (pendingChain.value?.length) msg.analysisChain = pendingChain.value }
        pendingChain.value = null
      }
      if (currentToolSteps.value.length > 0) {
        if (streamingAiIndex.value !== null) {
          const msg = chatMessages.value[streamingAiIndex.value]
          if (msg?.segments?.length) msg.segments.push({ type: 'tool', steps: [...currentToolSteps.value] })
          else if (msg) msg.toolSteps = [...(msg.toolSteps ?? []), ...currentToolSteps.value]
        } else {
          chatMessages.value.push({ role: 'ai', content: '', segments: [{ type: 'tool', steps: [...currentToolSteps.value] }] })
        }
        currentToolSteps.value = []; scrollToBottom()
      }
      pendingChain.value = null
    }
  }
}

async function onSendChat() {
  const text = chatInput.value.trim()
  if (!text) { chatPanelRef.value?.focusInput?.(); return }
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: text })
  await nextTick()
  void chatPanelRef.value?.forceScrollToBottom()

  if (activeAbort.value) { try { activeAbort.value.abort() } catch {} activeAbort.value = null }
  loading.value = true
  await nextTick()
  chatPanelRef.value?.forceScrollToBottom()

  streamingAiIndex.value = null
  streamQueue.value = []
  streamKind.value = null
  currentToolSteps.value = []
  if (diffFlushTimer) { window.clearInterval(diffFlushTimer); diffFlushTimer = undefined }

  const pendingChain: { value: import('../api').AnalysisChainStep[] | null } = { value: null }
  const onEvent = buildOnEvent(pendingChain)

  try {
    const { provider, model } = parseModelChoice(selectedModel.value)
    const ac = new AbortController()
    activeAbort.value = ac
    await runForwardProcessflowNpeFixSse(
      {
        apply: false,
        gitUrl: readonlyUrl.value || '',
        gitBranch: branch.value,
        gitCommit: commitId.value,
        message: text,
        provider,
        model,
        uploadedFileNames: [],
        sessionId: sessionId.value,
      },
      onEvent,
      { signal: ac.signal },
    )
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      if (!manualCancelled.value) chatMessages.value.push({ role: 'ai', content: '已手动终止本次请求。' })
      return
    }
    chatMessages.value.push({ role: 'ai', content: `[错误] ${e?.message ?? String(e)}` })
  } finally {
    loading.value = false
    activeAbort.value = null
    manualCancelled.value = false
    chatMessages.value.forEach((m) => { if (m.streaming) m.streaming = false })
    streamingAiIndex.value = null
  }
}

function cancelActiveRequest() {
  if (!activeAbort.value) return
  manualCancelled.value = true
  try { activeAbort.value.abort() } finally {
    activeAbort.value = null
    loading.value = false
    if (diffFlushTimer) { window.clearInterval(diffFlushTimer); diffFlushTimer = undefined }
    streamQueue.value = []
    streamKind.value = null
    currentToolSteps.value = []
    streamingAiIndex.value = null
    chatMessages.value.forEach((m) => { if (m.streaming) m.streaming = false })
    chatMessages.value.push({ role: 'ai', content: '已手动终止本次请求。' })
  }
}

// session_id 每次页面加载生成一次，清空对话时生成新的（同时删除旧 session 目录）
const sessionId = ref(`tool-test-${Date.now()}`)

async function clearChat() {
  // 删除旧 session 目录
  const oldId = sessionId.value
  if (oldId) {
    try {
      await fetch('/api/bugfix/tool-test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tool: 'cleanup_session', arguments: {}, session_id: oldId }),
      })
    } catch {}
  }
  sessionId.value = `tool-test-${Date.now()}`
  chatMessages.value = [{ role: 'ai', content: '对话已清空，可以重新开始。' }]
}
</script>

<template>
  <div class="tool-test-page">
    <!-- 顶部配置栏 -->
    <div class="config-bar">
      <a-select
        v-model:value="selectedProjectName"
        :options="repoOptions"
        :loading="loadingRepos"
        placeholder="选择项目"
        show-search
        allow-clear
        style="width: 220px;"
      />
      <a-select
        v-model:value="selectedModel"
        :options="modelOptions"
        placeholder="选择模型"
        style="width: 260px;"
      />
      <a-button @click="clearChat" :disabled="loading">清空对话</a-button>
      <span class="config-hint">选好项目和模型后直接发消息，AI 会走完整工具调用流程</span>
    </div>

    <!-- 对话面板，直接复用 ChatPanel -->
    <div class="chat-wrap">
      <ChatPanel
        ref="chatPanelRef"
        v-model:chatInput="chatInput"
        :loading="loading"
        :chatMessages="chatMessages"
        :canSend="canSend"
        :sendDisabledReason="sendDisabledReason"
        placeholder="输入问题… （Enter 发送，Ctrl+Enter 换行）"
        :renderMarkdown="renderMarkdown"
        :hideInput="false"
        @send="onSendChat"
        @cancel="cancelActiveRequest"
      />
    </div>
  </div>
</template>

<style scoped>
.tool-test-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.config-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
}

.config-hint {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

.chat-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>

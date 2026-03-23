<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

import { runForwardProcessflowNpeFixSse, runForwardProcessflowNpeFixTestSse, type BugfixResponse, type BugfixSseEvent, type ToolStep } from '../api'
import { modelOptions, parseModelChoice } from '../constants'
import type { ChatMsg } from '../types'

import { useRepo } from '../composables/useRepo'
import { useUpload } from '../composables/useUpload'
import SiderForm from '../components/SiderForm.vue'
import ChatPanel from '../components/ChatPanel.vue'

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
  try {
    return marked.parse(text) as string
  } catch {
    return text
  }
}

const loading = ref(false)
const lastOk = ref<boolean | null>(null)
const lastMessage = ref<string>('')
const error = ref<string | null>(null)
const result = ref<BugfixResponse | null>(null)

const repoState = useRepo({ projectSource: 'cacheApplicationProjects' })
const {
  repoMode,
  repoUrl,
  repo,
  branch,
  commitId,
  loadingBranches,
  loadingCommits,
  repoOptions,
  branchOptions,
  commitOptions,
  branchDisabled,
  commitDisabled,
  canSelectRef,
  canRun,
  uploadRef,
  canUpload,
  selectedProjectName,
  needRef,
  readonlyUrl,
  readonlyBranch,
  readonlyCommit,
  loadingRepos,
  onProjectDropdown,
  switchRepoMode,
  onBranchSearch,
  onCommitSearch,
} = repoState

const uploadState = useUpload(() => repo.value, () => uploadRef.value)
const {
  uploadFileList,
  uploading,
  uploadResult,
  sessionUploadedFileNames,
  customUpload,
  clearAfterSend: clearUploadAfterSend,
} = uploadState

const chatMessages = ref<ChatMsg[]>([
  { role: 'ai', content: '尊敬的开发者，欢迎使用AI Bug 专家，我将根据您的问题和提供的上下文，给出根因分析与修复建议。' },
])
const currentToolSteps = ref<ToolStep[]>([])
const chatInput = ref<string>('')
const selectedModel = ref<string>('claude::claude-haiku-4-5-20251001')

const streamingAiIndex = ref<number | null>(null)
const streamQueue = ref<Array<{ kind: 'text' | 'code'; content: string }>>([])
let diffFlushTimer: number | undefined
const streamKind = ref<'text' | 'code' | null>(null)
const activeAbort = ref<AbortController | null>(null)
const chatPanelRef = ref<InstanceType<typeof ChatPanel> | null>(null)
const manualCancelled = ref(false)

/** 是否允许点击发送（仅在不 loading 且 canRun 时；不要求输入框非空，空时点击会聚焦输入框） */
const canSend = computed(() => !loading.value && canRun.value)

/** 发送按钮禁用时的原因，用于 tooltip 提示 */
const sendDisabledReason = computed(() => {
  if (loading.value) return '请求进行中…'
  if (!canRun.value) return '请先在左侧选择项目（选择模式已选项目即可；自定义模式需填写 GIT URL 且 Branch/Commit 二选一）'
  return ''
})

/** 输入框占位符：根据状态提示 */
const chatInputPlaceholder = computed(() => {
  if (!canRun.value) return '请先在左侧选择项目后再输入问题…'
  return '输入你的问题… （Enter 发送，Ctrl+Enter 换行）'
})

function handleRepoModeUpdate(mode: 'select' | 'custom') {
  switchRepoMode(mode)
}

async function handleCustomUpload(opt: any) {
  try {
    await customUpload(opt)
  } catch (e: any) {
    const msg = e?.message ?? String(e)
    chatMessages.value.push({
      role: 'ai',
      content: `[错误] ${msg}`,
    })
  }
}

async function pushChat(role: 'user' | 'ai', content: string) {
  chatMessages.value.push({ role, content })
  await nextTick()
}

function scrollToBottom() {
  void chatPanelRef.value?.scrollToBottom?.()
}

/** 当前流式消息用 segments 交错：每句文字下紧跟该句用到的 tool */
function appendToStreamingBubble(chunk: string, kind: 'text' | 'code' = 'text') {
  if (streamingAiIndex.value === null || streamKind.value !== kind) {
    const leadingTools = currentToolSteps.value.length ? [...currentToolSteps.value] : []
    currentToolSteps.value = []
    const segments: import('../types/chat').ChatMsgSegment[] = []
    leadingTools.forEach((s) => segments.push({ type: 'tool', steps: [s] }))
    segments.push({ type: 'text', content: chunk })
    const content = leadingTools.length ? '' : chunk
    chatMessages.value.push({
      role: 'ai',
      content,
      segments,
      streaming: true,
    })
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
  if (!raw.trim()) return ''
  // 若已经是代码块，避免重复包裹
  if (raw.includes('```')) return raw
  let add = 0
  let del = 0
  for (const line of raw.split('\n')) {
    if (line.startsWith('+++') || line.startsWith('---')) continue
    if (line.startsWith('+')) add += 1
    else if (line.startsWith('-')) del += 1
  }
  const summary = `新增 ${add} 行，删除 ${del} 行`
  return `${summary}\n\n\`\`\`diff\n${raw}\n\`\`\``
}

function ensureDiffFlusherRunning() {
  if (diffFlushTimer) return
  diffFlushTimer = window.setInterval(() => {
    if (streamQueue.value.length === 0) {
      window.clearInterval(diffFlushTimer)
      diffFlushTimer = undefined
      return
    }
    const item = streamQueue.value.shift()
    if (!item) return
    appendToStreamingBubble(item.content + (item.kind === 'code' ? '\n' : ''), item.kind)
  }, 20)
}

async function onSendChat() {
  const text = chatInput.value.trim()
  if (!text) {
    chatPanelRef.value?.focusInput?.()
    return
  }
  chatInput.value = ''
  await pushChat('user', text)
  void chatPanelRef.value?.forceScrollToBottom()
  await runBugfix(text)
}

async function onTestChat() {
  const text = chatInput.value.trim() || '（测试回复：模拟 AI 流式输出）'
  chatInput.value = ''
  await pushChat('user', text)
  void chatPanelRef.value?.forceScrollToBottom()
  await runTestReply(text)
}

async function runBugfix(userMessage?: string) {
  // 若上一次还在跑，先终止
  if (activeAbort.value) {
    try { activeAbort.value.abort() } catch {}
    activeAbort.value = null
  }
  loading.value = true
  // 确保 loading 状态渲染后触底（等待符号出现时跟上）
  await nextTick()
  chatPanelRef.value?.forceScrollToBottom()
  error.value = null
  result.value = null
  streamingAiIndex.value = null
  streamQueue.value = []
  streamKind.value = null
  currentToolSteps.value = []
  if (diffFlushTimer) {
    window.clearInterval(diffFlushTimer)
    diffFlushTimer = undefined
  }

  if (!canRun.value) {
    chatMessages.value.push({
      role: 'ai',
      content: '[错误] 请先在左侧选择仓库，并在 Branch/CommitId 二选一后再发送',
    })
    loading.value = false
    return
  }

  let pendingAnalysisChain: import('../api/bugfix').AnalysisChainStep[] | null = null

  const onEvent = (ev: BugfixSseEvent) => {
    if (ev.type === 'status') return

    if (ev.type === 'analysis_chain') {
      const steps = ev.steps ?? []
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) msg.analysisChain = steps
      } else {
        pendingAnalysisChain = steps
      }
      scrollToBottom()
      return
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
        scrollToBottom()
      } else {
        currentToolSteps.value.push(step)
        scrollToBottom()
      }
      return
    }

    if (ev.type === 'text_chunk') {
      const chunk = ev.content ?? ''
      if (chunk) {
        appendToStreamingBubble(chunk, 'text')
        if (streamingAiIndex.value !== null && pendingAnalysisChain?.length) {
          const msg = chatMessages.value[streamingAiIndex.value]
          if (msg) msg.analysisChain = pendingAnalysisChain
          pendingAnalysisChain = null
        }
      }
      return
    }

    if (ev.type === 'text') {
      const t = (ev.content ?? '').trimEnd()
      if (t) {
        streamQueue.value.push({ kind: 'text', content: t })
        ensureDiffFlusherRunning()
      }
      return
    }

    if (ev.type === 'diff') {
      streamQueue.value.push({ kind: 'code', content: formatUnifiedDiff(ev.content ?? '') })
      ensureDiffFlusherRunning()
      return
    }

    if (ev.type === 'error') {
      loading.value = false
      chatMessages.value.push({
        role: 'ai',
        content: `[错误] ${ev.content ?? '未知错误'}`,
        toolSteps: currentToolSteps.value.length > 0 ? [...currentToolSteps.value] : undefined,
      })
      currentToolSteps.value = []
      return
    }

    if (ev.type === 'done') {
      loading.value = false
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) {
          msg.streaming = false
          if (pendingAnalysisChain?.length) msg.analysisChain = pendingAnalysisChain
        }
        pendingAnalysisChain = null
      }
      if (currentToolSteps.value.length > 0) {
        if (streamingAiIndex.value !== null) {
          const msg = chatMessages.value[streamingAiIndex.value]
          if (msg) {
            if (msg.segments?.length) msg.segments.push({ type: 'tool', steps: [...currentToolSteps.value] })
            else msg.toolSteps = [...(msg.toolSteps ?? []), ...currentToolSteps.value]
          }
        } else {
          chatMessages.value.push({
            role: 'ai',
            content: '',
            segments: [{ type: 'tool', steps: [...currentToolSteps.value] }],
            ...(pendingAnalysisChain?.length ? { analysisChain: pendingAnalysisChain } : {}),
          })
        }
        currentToolSteps.value = []
        scrollToBottom()
      }
      pendingAnalysisChain = null
    }
  }

  try {
    const fileNames = [...sessionUploadedFileNames.value]
    const { provider, model } = parseModelChoice(selectedModel.value)
    const ac = new AbortController()
    activeAbort.value = ac
    const r = await runForwardProcessflowNpeFixSse(
      {
        apply: false,
        gitUrl: repo.value!.url,
        gitBranch: branch.value,
        gitCommit: commitId.value,
        message: userMessage ?? '',
        provider,
        model,
        uploadedFileNames: fileNames,
      },
      onEvent,
      { signal: ac.signal },
    )
    clearUploadAfterSend()
    result.value = r
    lastOk.value = r.ok
    lastMessage.value = r.message
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      if (!manualCancelled.value) chatMessages.value.push({ role: 'ai', content: '已手动终止本次请求。' })
      return
    }
    result.value = null
    lastOk.value = false
    lastMessage.value = ''
    const msg = e?.message ?? String(e)
    chatMessages.value.push({
      role: 'ai',
      content: `[错误] ${msg}`,
    })
  } finally {
    loading.value = false
    activeAbort.value = null
    manualCancelled.value = false
    chatMessages.value.forEach((m) => {
      if (m.streaming) m.streaming = false
    })
    streamingAiIndex.value = null
  }
}

async function runTestReply(userMessage?: string) {
  if (activeAbort.value) {
    try { activeAbort.value.abort() } catch {}
    activeAbort.value = null
  }
  loading.value = true
  await nextTick()
  chatPanelRef.value?.forceScrollToBottom()
  error.value = null
  result.value = null
  streamingAiIndex.value = null
  streamQueue.value = []
  streamKind.value = null
  currentToolSteps.value = []

  if (diffFlushTimer) {
    window.clearInterval(diffFlushTimer)
    diffFlushTimer = undefined
  }

  let pendingAnalysisChain: import('../api/bugfix').AnalysisChainStep[] | null = null

  const onEvent = (ev: BugfixSseEvent) => {
    if (ev.type === 'status') return

    if (ev.type === 'analysis_chain') {
      pendingAnalysisChain = ev.steps ?? []
      scrollToBottom()
      return
    }

    if (ev.type === 'tool') {
      if (ev.kind === 'file_list') return
      currentToolSteps.value.push({ kind: ev.kind, title: ev.title, content: ev.content, summary: ev.summary })
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) {
          msg.toolSteps = [...(msg.toolSteps ?? []), { kind: ev.kind, title: ev.title, content: ev.content, summary: ev.summary }]
          streamingAiIndex.value = null
          streamKind.value = null
          // 这个 tool step 已经追加到当前气泡里了；清空队列，避免下一次 text_chunk 创建新气泡时再次携带导致重复显示
          currentToolSteps.value = []
          scrollToBottom()
        }
      }
      return
    }

    if (ev.type === 'text_chunk') {
      const chunk = ev.content ?? ''
      if (chunk) {
        appendToStreamingBubble(chunk, 'text')
        if (streamingAiIndex.value !== null && pendingAnalysisChain?.length) {
          const msg = chatMessages.value[streamingAiIndex.value]
          if (msg) msg.analysisChain = pendingAnalysisChain
          pendingAnalysisChain = null
        }
      }
      return
    }

    if (ev.type === 'text') {
      const t = (ev.content ?? '').trimEnd()
      if (t) {
        streamQueue.value.push({ kind: 'text', content: t })
        ensureDiffFlusherRunning()
      }
      return
    }

    if (ev.type === 'diff') {
      streamQueue.value.push({ kind: 'code', content: formatUnifiedDiff(ev.content ?? '') })
      ensureDiffFlusherRunning()
      return
    }

    if (ev.type === 'error') {
      loading.value = false
      chatMessages.value.push({
        role: 'ai',
        content: `[错误] ${ev.content ?? '未知错误'}`,
        toolSteps: currentToolSteps.value.length > 0 ? [...currentToolSteps.value] : undefined,
      })
      currentToolSteps.value = []
      return
    }

    if (ev.type === 'done') {
      loading.value = false
      if (streamingAiIndex.value !== null) {
        const msg = chatMessages.value[streamingAiIndex.value]
        if (msg) {
          msg.streaming = false
          if (pendingAnalysisChain?.length) msg.analysisChain = pendingAnalysisChain
        }
        pendingAnalysisChain = null
      } else if (currentToolSteps.value.length > 0) {
        chatMessages.value.push({
          role: 'ai',
          content: '',
          toolSteps: [...currentToolSteps.value],
          ...(pendingAnalysisChain?.length ? { analysisChain: pendingAnalysisChain } : {}),
        })
        currentToolSteps.value = []
        pendingAnalysisChain = null
        scrollToBottom()
      }
    }
  }

  try {
    const fileNames = [...sessionUploadedFileNames.value]
    const { provider, model } = parseModelChoice(selectedModel.value)
    const ac = new AbortController()
    activeAbort.value = ac
    const r = await runForwardProcessflowNpeFixTestSse(
      {
        message: userMessage ?? '',
        gitUrl: repo.value?.url ?? '',
        gitBranch: branch.value,
        gitCommit: commitId.value,
        provider,
        model,
        uploadedFileNames: fileNames,
      },
      onEvent,
      { signal: ac.signal },
    )
    clearUploadAfterSend()
    result.value = r
    lastOk.value = r.ok
    lastMessage.value = r.message
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      if (!manualCancelled.value) chatMessages.value.push({ role: 'ai', content: '已手动终止本次请求。' })
      return
    }
    result.value = null
    lastOk.value = false
    lastMessage.value = ''
    const msg = e?.message ?? String(e)
    chatMessages.value.push({
      role: 'ai',
      content: `[错误] ${msg}`,
    })
  } finally {
    loading.value = false
    activeAbort.value = null
    manualCancelled.value = false
    chatMessages.value.forEach((m) => {
      if (m.streaming) m.streaming = false
    })
    streamingAiIndex.value = null
  }
}

function cancelActiveRequest() {
  if (!activeAbort.value) return
  manualCancelled.value = true
  try {
    activeAbort.value.abort()
  } finally {
    activeAbort.value = null
    loading.value = false
    // 清理流式状态，避免 UI 卡住
    if (diffFlushTimer) {
      window.clearInterval(diffFlushTimer)
      diffFlushTimer = undefined
    }
    streamQueue.value = []
    streamKind.value = null
    currentToolSteps.value = []
    streamingAiIndex.value = null
    chatMessages.value.forEach((m) => {
      if (m.streaming) m.streaming = false
    })
    chatMessages.value.push({ role: 'ai', content: '已手动终止本次请求。' })
  }
}
</script>

<template>
  <a-row class="main-row" :gutter="16">
    <a-col class="sider-col" :xs="24" :lg="7" :xl="6">
      <SiderForm
        :repoMode="repoMode"
        @update:repoMode="(...args: any[]) => handleRepoModeUpdate(args[0])"
        v-model:repoUrl="repoUrl"
        v-model:selectedProjectName="selectedProjectName"
        v-model:branch="branch"
        v-model:commitId="commitId"
        v-model:uploadFileList="uploadFileList"
        v-model:selectedModel="selectedModel"
        :needRef="needRef"
        :readonlyUrl="readonlyUrl"
        :readonlyBranch="readonlyBranch"
        :readonlyCommit="readonlyCommit"
        :loadingRepos="loadingRepos"
        :lastOk="lastOk"
        :lastMessage="lastMessage"
        :error="error"
        :repoOptions="repoOptions"
        :canSelectRef="canSelectRef"
        :branchOptions="branchOptions"
        :branchDisabled="branchDisabled"
        :loadingBranches="loadingBranches"
        :commitOptions="commitOptions"
        :commitDisabled="commitDisabled"
        :loadingCommits="loadingCommits"
        :canUpload="canUpload"
        :uploading="uploading"
        :uploadResult="uploadResult"
        :modelOptions="[...modelOptions]"
        @branchSearch="(...args: any[]) => onBranchSearch(String(args[0] || ''))"
        @commitSearch="(...args: any[]) => onCommitSearch(String(args[0] || ''))"
        @branchDropdown="(open: boolean) => void repoState.onBranchDropdown(open)"
        @commitDropdown="(open: boolean) => void repoState.onCommitDropdown(open)"
        @projectDropdown="(...args: any[]) => onProjectDropdown(Boolean(args[0]))"
        @customUpload="(opt: any) => void handleCustomUpload(opt)"
      />
    </a-col>

    <a-col class="chat-col" :xs="24" :lg="17" :xl="18">
      <ChatPanel
        ref="chatPanelRef"
        v-model:chatInput="chatInput"
        :loading="loading"
        :chatMessages="chatMessages"
        :canSend="canSend"
        :sendDisabledReason="sendDisabledReason"
        :placeholder="chatInputPlaceholder"
        :renderMarkdown="renderMarkdown"
        @send="onSendChat"
        @test="onTestChat"
        @cancel="cancelActiveRequest"
      />
    </a-col>
  </a-row>
</template>


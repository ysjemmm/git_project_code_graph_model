export type ToolStep = { kind: string; title: string; content: string; summary?: string }

export type AnalysisChainStep = { type: string; label: string; detail?: string | null; tool_kind?: string }

export type BugfixSseEvent =
  | { type: 'status'; content: string }
  | { type: 'text'; content: string }
  | { type: 'text_chunk'; content: string }
  | { type: 'diff'; content: string }
  | { type: 'tool'; kind: string; title: string; content: string; summary?: string }
  | { type: 'analysis_chain'; steps: AnalysisChainStep[] }
  | { type: 'error'; content: string }
  | { type: 'done'; ok: boolean; message?: string; target_file?: string; applied?: boolean }

export type BugfixResponse = {
  ok: boolean
  message: string
  target_file: string
  applied: boolean
  unified_diff: string
}

export async function runForwardProcessflowNpeFixSse(
  params: {
    apply: boolean
    gitUrl: string
    gitBranch?: string
    gitCommit?: string
    message: string
    provider?: string
    model?: string
    uploadedFileNames?: string[]
    /** 历史对话，[{role, content}] */
    history?: Array<{ role: 'user' | 'ai'; content: string }>
  },
  onEvent: (ev: BugfixSseEvent) => void,
  opts?: { signal?: AbortSignal },
): Promise<BugfixResponse> {
  const resp = await fetch('/api/bugfix/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal: opts?.signal,
    body: JSON.stringify({
      apply: params.apply,
      git_url: params.gitUrl,
      git_branch: params.gitBranch ?? '',
      git_commit: params.gitCommit ?? '',
      message: params.message,
      provider: params.provider ?? 'deepseek',
      model: params.model ?? '',
      uploaded_file_names: params.uploadedFileNames ?? [],
      history: (params.history ?? []).map(m => ({
        role: m.role === 'ai' ? 'assistant' : 'user',
        content: m.content,
      })),
    }),
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`Request failed with status code ${resp.status}`)
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  let unifiedDiff = ''
  let done: { ok: boolean; message?: string; target_file?: string; applied?: boolean } | null = null

  while (true) {
    const { value, done: streamDone } = await reader.read()
    if (streamDone) break
    buffer += decoder.decode(value, { stream: true })

    // SSE events separated by blank line
    while (true) {
      const idx = buffer.indexOf('\n\n')
      if (idx === -1) break
      const rawEvent = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)

      // Only parse `data: ...` lines
      const dataLine = rawEvent
        .split('\n')
        .map((l) => l.trimEnd())
        .find((l) => l.startsWith('data: '))
      if (!dataLine) continue

      const jsonStr = dataLine.slice('data: '.length)
      let payload: any
      try {
        payload = JSON.parse(jsonStr)
      } catch {
        continue
      }

      const type = payload?.type
      if (type === 'diff') {
        unifiedDiff += payload.content + '\n'
        onEvent({ type: 'diff', content: payload.content })
      } else if (type === 'text_chunk') {
        onEvent({ type: 'text_chunk', content: String(payload.content ?? '') })
      } else if (type === 'text') {
        onEvent({ type: 'text', content: String(payload.content ?? '') })
      } else if (type === 'tool') {
        onEvent({
          type: 'tool',
          kind: String(payload.kind ?? ''),
          title: String(payload.title ?? ''),
          content: String(payload.content ?? ''),
          ...(payload.summary != null ? { summary: String(payload.summary) } : {}),
        })
      } else if (type === 'analysis_chain') {
        const steps = Array.isArray(payload.steps) ? payload.steps : []
        onEvent({ type: 'analysis_chain', steps })
      } else if (type === 'status') {
        onEvent({ type: 'status', content: String(payload.content ?? '') })
      } else if (type === 'error') {
        onEvent({ type: 'error', content: String(payload.content ?? '') })
      } else if (type === 'done') {
        done = {
          ok: Boolean(payload.ok),
          message: payload.message,
          target_file: payload.target_file,
          applied: payload.applied,
        }
        onEvent({ type: 'done', ...done })
      }
    }
  }

  return {
    ok: done?.ok ?? true,
    message: done?.message ?? '完成',
    target_file: done?.target_file ?? '',
    applied: Boolean(done?.applied),
    unified_diff: unifiedDiff.trimEnd(),
  }
}

export async function runForwardProcessflowNpeFixTestSse(
  params: {
    message: string
    gitUrl: string
    gitBranch?: string
    gitCommit?: string
    provider?: string
    model?: string
    uploadedFileNames?: string[]
    history?: Array<{ role: 'user' | 'ai'; content: string }>
  },
  onEvent: (ev: BugfixSseEvent) => void,
  opts?: { signal?: AbortSignal },
): Promise<BugfixResponse> {
  const resp = await fetch('/api/bugfix/test-analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal: opts?.signal,
    body: JSON.stringify({
      apply: false,
      git_url: params.gitUrl,
      git_branch: params.gitBranch ?? '',
      git_commit: params.gitCommit ?? '',
      message: params.message,
      provider: params.provider ?? 'deepseek',
      model: params.model ?? '',
      uploaded_file_names: params.uploadedFileNames ?? [],
      history: (params.history ?? []).map(m => ({
        role: m.role === 'ai' ? 'assistant' : 'user',
        content: m.content,
      })),
    }),
  })
  if (!resp.ok || !resp.body) {
    throw new Error(`Request failed with status code ${resp.status}`)
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  let unifiedDiff = ''
  let done: { ok: boolean; message?: string; target_file?: string; applied?: boolean } | null = null

  while (true) {
    const { value, done: streamDone } = await reader.read()
    if (streamDone) break
    buffer += decoder.decode(value, { stream: true })

    // SSE events separated by blank line
    while (true) {
      const idx = buffer.indexOf('\n\n')
      if (idx === -1) break
      const rawEvent = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)

      // Only parse `data: ...` lines
      const dataLine = rawEvent
        .split('\n')
        .map((l) => l.trimEnd())
        .find((l) => l.startsWith('data: '))
      if (!dataLine) continue

      const jsonStr = dataLine.slice('data: '.length)
      let payload: any
      try {
        payload = JSON.parse(jsonStr)
      } catch {
        continue
      }

      const type = payload?.type
      if (type === 'diff') {
        unifiedDiff += payload.content + '\n'
        onEvent({ type: 'diff', content: payload.content })
      } else if (type === 'text_chunk') {
        onEvent({ type: 'text_chunk', content: String(payload.content ?? '') })
      } else if (type === 'text') {
        onEvent({ type: 'text', content: String(payload.content ?? '') })
      } else if (type === 'tool') {
        onEvent({
          type: 'tool',
          kind: String(payload.kind ?? ''),
          title: String(payload.title ?? ''),
          content: String(payload.content ?? ''),
          ...(payload.summary != null ? { summary: String(payload.summary) } : {}),
        })
      } else if (type === 'analysis_chain') {
        const steps = Array.isArray(payload.steps) ? payload.steps : []
        onEvent({ type: 'analysis_chain', steps })
      } else if (type === 'status') {
        onEvent({ type: 'status', content: String(payload.content ?? '') })
      } else if (type === 'error') {
        onEvent({ type: 'error', content: String(payload.content ?? '') })
      } else if (type === 'done') {
        done = {
          ok: Boolean(payload.ok),
          message: payload.message,
          target_file: payload.target_file,
          applied: payload.applied,
        }
        onEvent({ type: 'done', ...done })
      }
    }
  }

  return {
    ok: done?.ok ?? true,
    message: done?.message ?? '完成',
    target_file: done?.target_file ?? '',
    applied: Boolean(done?.applied),
    unified_diff: unifiedDiff.trimEnd(),
  }
}


// ─── Types ────────────────────────────────────────────────────────────────────

export type CacheProjectItem = {
  id?: number | null
  repo_name: string
  project_name?: string | null
  project_key?: string | null
  project_type?: string | null
  repo_url?: string | null
  branch?: string | null
  commit_hash?: string | null
  last_update_time?: string | null
  cache_dir?: string | null
  dirty?: boolean | null
  remote_url?: string | null
  repo_exists?: boolean
  linked_by_count?: number
  linked_to_count?: number
  linked_by_preview?: string[]
  app_type?: string | null    // 'frontend' | 'backend'
  language?: string | null    // 'java' | 'python' | 'go' | 'other' | 'vue' | 'react'
}

export type ImportSettings = {
  maven_scan_enabled: boolean
  force_maven: boolean
  clear_database: boolean
  auto_link_external: boolean
}

export type ImportTask = {
  task_id: string
  status: string
  error?: string | null
  message?: string | null
  task_type?: 'auto' | 'full' | 'incremental' | string
  project_name?: string | null
  repo_url?: string | null
  branch?: string | null
  commit_id?: string | null
  created_at?: string | null
  completed_at?: string | null
  maven_scan_enabled?: boolean
  force_maven?: boolean
  clear_database?: boolean
  auto_link_external?: boolean
}

export type GitDiffSummaryFile = {
  status: 'A' | 'M' | 'D' | 'R' | string
  path: string
  old_path?: string | null
  additions: number
  deletions: number
}

export type GitDiffSummaryResult = {
  ok: boolean
  message?: string
  from_ref: string
  to_ref: string
  from_commit: string
  to_commit: string
  same_commit: boolean
  stats: {
    files: number
    additions: number
    deletions: number
  }
  files: GitDiffSummaryFile[]
}

export type GitDiffFileResult = {
  ok: boolean
  message?: string
  from_ref: string
  to_ref: string
  from_commit: string
  to_commit: string
  file_path: string
  truncated: boolean
  patch: string
}

export type GraphProject = {
  project_name: string
  repo_url?: string
  branch?: string
  commit_hash?: string
}

export type GraphQueryResult = {
  ok: boolean
  message?: string
  columns: string[]
  rows: Array<Record<string, any>>
  limit?: number
}

export type GraphDiagnosticsCheck = {
  id: string
  title: string
  status: 'ok' | 'warning' | 'error'
  message: string
  suggestion?: string
}

export type GraphDiagnosticsSummary = {
  ok: boolean
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
  checks: GraphDiagnosticsCheck[]
  alerts: {
    error: number
    warning: number
    ok: number
  }
}

export type DepItem = {
  jar_name: string
  group_id: string
  artifact_id: string
  version: string
  is_second_party: boolean
}

export type SecondPartyRule = {
  id: number
  name: string
  enabled: boolean
  sort_order: number
  group_id_regex: string
  artifact_id_regex: string
  created_at: string
  updated_at: string
}

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
  | { type: 'structured_result'; data: { bug_cause: string; bug_location: Array<{ file: string; line_range: string; description: string }>; fix_plans: Array<{ id: number; title: string; description: string; diff: string }> } }
  | { type: 'done'; ok: boolean; message?: string; target_file?: string; applied?: boolean }

export type BugfixResponse = {
  ok: boolean
  message: string
  target_file: string
  applied: boolean
  unified_diff: string
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function request<T = any>(url: string, init?: RequestInit): Promise<T> {
  const r = await fetch(url, init)
  const data = await r.json().catch(() => ({}))
  if (!r.ok) throw new Error(data?.message ?? data?.detail ?? `HTTP ${r.status}`)
  return data as T
}

function json(method: string, body: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}

// ─── Cache / Application Projects ─────────────────────────────────────────────

export async function listApplicationProjects(refresh = false): Promise<{ ok: boolean; items: CacheProjectItem[] }> {
  const url = new URL('/api/cache/application-projects', window.location.origin)
  if (refresh) url.searchParams.set('refresh', '1')
  return request(url.toString())
}

export async function listApplicationProjectsForGraph(refresh = false): Promise<{ ok: boolean; items: CacheProjectItem[] }> {
  const url = new URL('/api/cache/application-projects', window.location.origin)
  url.searchParams.set('java_only', '1')
  if (refresh) url.searchParams.set('refresh', '1')
  return request(url.toString())
}

export type LinkedByItem = {
  source_app_id: number
  source_repo_name: string
  source_project_name: string
  link_count: number
  last_linked_at?: string
}

export async function listApplicationLinkedBy(appId: number): Promise<{ ok: boolean; app_id: number; items: LinkedByItem[] }> {
  return request(`/api/cache/application-projects/${appId}/linked-by`)
}

export async function getImportSettings(appId: number): Promise<{ ok: boolean; settings: ImportSettings }> {
  return request(`/api/cache/application-projects/${appId}/import-settings`)
}

export async function updateImportSettings(appId: number, settings: ImportSettings): Promise<{ ok: boolean }> {
  return request(`/api/cache/application-projects/${appId}/import-settings`, json('PATCH', settings))
}

export async function deleteApplicationProject(appId: number): Promise<{ ok: boolean; errors?: string[] }> {
  return request(`/api/cache/application-projects/${appId}`, { method: 'DELETE' })
}

// ─── App Dependencies (pom.xml) ───────────────────────────────────────────────

export type MatchedRule = {
  name: string
  group_id_regex: string
  artifact_id_regex: string
  sort_order: number
}

export type AppDependency = {
  group_id: string
  artifact_id: string
  version: string
  scope: string
  parent_group_id?: string
  parent_artifact_id?: string
  parent_version?: string
  is_second_party: boolean
  scanned_at: string
  matched_rule_id?: number | null
  matched_rule?: MatchedRule | null
  matched_via?: 'direct' | 'parent' | 'none'
  parent_dependency?: {
    group_id: string
    artifact_id: string
    version: string
    scope: string
  } | null
}

export type AppDependencyTreeChild = AppDependency & {
  key: string
  __is_parent_group: false
  parent: string
  linked?: AppDependencyLink | null
  locked_by_parent?: boolean
}

export type AppDependencyTreeParent = {
  key: string
  __is_parent_group: true
  parent: string
  parent_group_id: string
  parent_artifact_id: string
  parent_version: string
  child_count: number
  second_party_count: number
  uniform_link?: AppDependencyLink | null
  parent_locked?: boolean
  children: AppDependencyTreeChild[]
}

export type AppMavenInfo = {
  group_id: string
  artifact_id: string
  version: string
  packaging: string
  parent_group_id: string
  parent_artifact_id: string
  parent_version: string
  pom_path: string
}

export async function getAppDependencies(appId: number): Promise<{ ok: boolean; scanned_at: string | null; items: AppDependency[] }> {
  return request(`/api/cache/application-projects/${appId}/dependencies`)
}

export async function getAppDependenciesTree(
  appId: number,
  params?: { scope?: string; secondOnly?: boolean },
): Promise<{
  ok: boolean
  scanned_at: string | null
  total_count: number
  second_party_count: number
  third_party_count: number
  filtered_count: number
  items: AppDependencyTreeParent[]
}> {
  const url = new URL(`/api/cache/application-projects/${appId}/dependencies-tree`, window.location.origin)
  if (params?.scope) url.searchParams.set('scope', params.scope)
  if (params?.secondOnly) url.searchParams.set('second_only', '1')
  return request(url.toString())
}

export async function getAppMavenInfo(appId: number): Promise<{ ok: boolean; app_id: number; maven: AppMavenInfo }> {
  return request(`/api/cache/application-projects/${appId}/maven-info`)
}

export async function refreshAppDependencies(appId: number): Promise<{ ok: boolean; total: number; second_party_count: number; parent_classified_count?: number }> {
  return request(`/api/cache/application-projects/${appId}/refresh-dependencies`, { method: 'POST' })
}

// ─── App Dependency Links（手动关联二方包项目） ────────────────────────────────

export type AppDependencyLink = {
  id: number
  app_id: number
  group_id: string
  artifact_id: string
  linked_app_id: number
  linked_project_name: string
  linked_repo_name: string
  dep_version: string
  note: string
  created_at: string
}

export async function listDependencyLinks(appId: number): Promise<{ ok: boolean; items: AppDependencyLink[] }> {
  return request(`/api/cache/application-projects/${appId}/dependency-links`)
}

export async function upsertDependencyLink(appId: number, payload: {
  group_id: string; artifact_id: string; linked_app_id: number; note?: string
}): Promise<{ ok: boolean; id: number; neo4j_synced: boolean; neo4j_message?: string | null }> {
  return request(`/api/cache/application-projects/${appId}/dependency-links`, json('PUT', payload))
}

export async function upsertDependencyLinksByParent(appId: number, payload: {
  parent_group_id: string
  parent_artifact_id: string
  parent_version?: string
  linked_app_id: number
  note?: string
}): Promise<{ ok: boolean; affected_count: number; ids?: number[]; neo4j_synced: boolean; neo4j_message?: string | null; message?: string }> {
  return request(`/api/cache/application-projects/${appId}/dependency-links/by-parent`, json('PUT', payload))
}

export async function deleteDependencyLink(linkId: number): Promise<{ ok: boolean }> {
  return request(`/api/cache/dependency-links/${linkId}`, { method: 'DELETE' })
}

export async function syncDependencyLinksToNeo4j(): Promise<{ ok: boolean; synced: number; message?: string }> {
  return request('/api/cache/dependency-links/sync-to-neo4j', { method: 'POST' })
}

export async function createApplication(payload: {
  project_name: string
  app_type: string
  language: string
  repo_url: string
  maven_scan_enabled?: boolean
  force_maven?: boolean
  clear_database?: boolean
  auto_link_external?: boolean
}): Promise<{ ok: boolean; id?: number; message?: string }> {
  return request('/api/cache/application-projects', json('POST', payload))
}

export async function createImportTask(payload: {
  project_name: string
  repo_url: string
  branch?: string
  maven_scan_enabled?: boolean
  force_maven?: boolean
  clear_database?: boolean
  auto_link_external?: boolean
  task_type?: 'auto' | 'full' | 'incremental'
  acceptance_enabled?: boolean
  acceptance_block_on_fail?: boolean
  acceptance_max_drop_ratio?: number
}): Promise<{ ok: boolean; task_id?: string; message?: string }> {
  return request('/api/import/tasks', json('POST', payload))
}

export async function listImportTasks(): Promise<{ ok: boolean; items: ImportTask[] }> {
  return request('/api/import/tasks')
}

export async function getImportTask(taskId: string): Promise<{ ok: boolean; item: ImportTask }> {
  return request(`/api/import/tasks/${taskId}`)
}

export async function getImportTaskAcceptanceDetail(taskId: string): Promise<{ ok: boolean; task_id: string; detail: any }> {
  return request(`/api/import/tasks/${taskId}/acceptance-detail`)
}

export async function cancelImportTask(taskId: string): Promise<{ ok: boolean }> {
  return request(`/api/import/tasks/${taskId}/cancel`, { method: 'POST' })
}

export async function unblockAutoImportTasks(reason?: string): Promise<{ ok: boolean; message?: string }> {
  return request('/api/import/tasks/unblock-auto', json('POST', { reason: reason || '' }))
}

export async function getImportTaskLog(taskId: string, offset: number, limit = 2000): Promise<{ ok: boolean; lines: string[]; next_offset: number }> {
  const url = new URL(`/api/import/tasks/${taskId}/logs`, window.location.origin)
  url.searchParams.set('offset', String(offset))
  url.searchParams.set('limit', String(limit))
  return request(url.toString())
}

// ─── Graph / Projects ──────────────────────────────────────────────────────────

export async function listGraphProjects(includeCounts = false): Promise<{ ok: boolean; items: GraphProject[] }> {
  const url = new URL('/api/graph/projects', window.location.origin)
  if (includeCounts) url.searchParams.set('include_counts', 'true')
  return request(url.toString())
}

export async function getSecondPartyDeps(projectName: string): Promise<{ ok: boolean; items: DepItem[] }> {
  return request(`/api/graph/projects/${encodeURIComponent(projectName)}/second-party-deps`)
}

export async function runGraphQuery(payload: {
  cypher: string
  params?: Record<string, any>
  limit?: number
}): Promise<GraphQueryResult> {
  return request('/api/graph/query', json('POST', payload))
}

export async function clearGraphDatabase(confirm = 'CLEAR_ALL'): Promise<{
  ok: boolean
  message?: string
  before_nodes?: number
  before_relationships?: number
  after_nodes?: number
}> {
  return request('/api/graph/clear', json('POST', { confirm }))
}

export async function getGraphDiagnosticsSummary(): Promise<GraphDiagnosticsSummary> {
  return request('/api/graph/diagnostics/summary')
}

// ─── Git ───────────────────────────────────────────────────────────────────────

export async function listGitRepos(): Promise<{ gitList: Array<{ name: string; url: string }> }> {
  return request('/api/git-repos')
}

export async function listGitBranches(repoUrl: string, q?: string): Promise<{ items: string[] }> {
  const url = new URL('/api/git-branches', window.location.origin)
  url.searchParams.set('repoUrl', repoUrl)
  if (q?.trim()) url.searchParams.set('q', q.trim())
  return request(url.toString())
}

export async function listGitCommits(repoUrl: string, branch?: string, q?: string): Promise<{ items: string[] }> {
  const url = new URL('/api/git-commits', window.location.origin)
  url.searchParams.set('repoUrl', repoUrl)
  if (branch) url.searchParams.set('branch', branch)
  if (q?.trim()) url.searchParams.set('q', q.trim())
  return request(url.toString())
}

export async function getGitDiffSummary(payload: {
  repo_url: string
  from_ref: string
  to_ref: string
  max_files?: number
}): Promise<GitDiffSummaryResult> {
  const url = new URL('/api/git-diff/summary', window.location.origin)
  url.searchParams.set('repoUrl', payload.repo_url)
  url.searchParams.set('fromRef', payload.from_ref)
  url.searchParams.set('toRef', payload.to_ref)
  if (payload.max_files != null) url.searchParams.set('maxFiles', String(payload.max_files))
  return request(url.toString())
}

export async function getGitDiffFile(payload: {
  repo_url: string
  from_ref: string
  to_ref: string
  file_path: string
  context?: number
  max_lines?: number
}): Promise<GitDiffFileResult> {
  const url = new URL('/api/git-diff/file', window.location.origin)
  url.searchParams.set('repoUrl', payload.repo_url)
  url.searchParams.set('fromRef', payload.from_ref)
  url.searchParams.set('toRef', payload.to_ref)
  url.searchParams.set('filePath', payload.file_path)
  if (payload.context != null) url.searchParams.set('context', String(payload.context))
  if (payload.max_lines != null) url.searchParams.set('maxLines', String(payload.max_lines))
  return request(url.toString())
}

export async function gitApplyAndCommit(payload: {
  git_url: string
  project_name?: string
  branch: string
  commit_msg: string
  diff: string
  reviewer?: string
}): Promise<{ ok: boolean; message: string; branch?: string; steps?: string[] }> {
  return request('/api/git-apply-and-commit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
}

// ─── Upload ────────────────────────────────────────────────────────────────────

export async function uploadFiles(
  projectName: string,
  ref: string,
  file: File,
  sessionId?: string,
): Promise<{ ok: boolean; saved: Array<{ filename: string }> }> {
  const form = new FormData()
  form.append('project_name', projectName)
  form.append('ref', ref)
  form.append('files', file)
  if (sessionId) form.append('session_id', sessionId)
  const r = await fetch('/api/upload', { method: 'POST', body: form })
  const data = await r.json()
  if (!r.ok || !data?.ok) throw new Error(data?.message ?? '上传失败')
  return data
}

// ─── Second-party Rules ────────────────────────────────────────────────────────

export async function fetchSecondPartyRules(): Promise<{ ok: boolean; items: SecondPartyRule[] }> {
  return request('/api/second-party-rules')
}

export async function createSecondPartyRule(payload: Omit<SecondPartyRule, 'id' | 'created_at' | 'updated_at'>): Promise<{ ok: boolean; item?: SecondPartyRule; message?: string }> {
  return request('/api/second-party-rules', json('POST', payload))
}

export async function updateSecondPartyRule(ruleId: number, payload: Omit<SecondPartyRule, 'id' | 'created_at' | 'updated_at'>): Promise<{ ok: boolean; item?: SecondPartyRule; message?: string }> {
  return request(`/api/second-party-rules/${ruleId}`, json('PUT', payload))
}

export async function deleteSecondPartyRule(ruleId: number): Promise<{ ok: boolean; deleted_id?: number; message?: string }> {
  return request(`/api/second-party-rules/${ruleId}`, { method: 'DELETE' })
}

// ─── Bugfix (SSE) ──────────────────────────────────────────────────────────────

async function runBugfixSse(
  endpoint: string,
  body: unknown,
  onEvent: (ev: BugfixSseEvent) => void,
  signal?: AbortSignal,
): Promise<BugfixResponse> {
  const resp = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    signal,
    body: JSON.stringify(body),
  })
  if (!resp.ok || !resp.body) throw new Error(`Request failed with status code ${resp.status}`)

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let unifiedDiff = ''
  let done: { ok: boolean; message?: string; target_file?: string; applied?: boolean } | null = null

  while (true) {
    const { value, done: streamDone } = await reader.read()
    if (streamDone) break
    buffer += decoder.decode(value, { stream: true })

    while (true) {
      const idx = buffer.indexOf('\n\n')
      if (idx === -1) break
      const rawEvent = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      const dataLine = rawEvent.split('\n').map((l) => l.trimEnd()).find((l) => l.startsWith('data: '))
      if (!dataLine) continue
      let payload: any
      try { payload = JSON.parse(dataLine.slice('data: '.length)) } catch { continue }

      const type = payload?.type
      if (type === 'diff') {
        unifiedDiff += payload.content + '\n'
        onEvent({ type: 'diff', content: payload.content })
      } else if (type === 'text_chunk') {
        onEvent({ type: 'text_chunk', content: String(payload.content ?? '') })
      } else if (type === 'text') {
        onEvent({ type: 'text', content: String(payload.content ?? '') })
      } else if (type === 'tool') {
        onEvent({ type: 'tool', kind: String(payload.kind ?? ''), title: String(payload.title ?? ''), content: String(payload.content ?? ''), ...(payload.summary != null ? { summary: String(payload.summary) } : {}) })
      } else if (type === 'analysis_chain') {
        onEvent({ type: 'analysis_chain', steps: Array.isArray(payload.steps) ? payload.steps : [] })
      } else if (type === 'status') {
        onEvent({ type: 'status', content: String(payload.content ?? '') })
      } else if (type === 'error') {
        onEvent({ type: 'error', content: String(payload.content ?? '') })
      } else if (type === 'structured_result') {
        onEvent({ type: 'structured_result', data: payload.data })
      } else if (type === 'done') {
        done = { ok: Boolean(payload.ok), message: payload.message, target_file: payload.target_file, applied: payload.applied }
        onEvent({ type: 'done', ...done })
      }
    }
  }

  return { ok: done?.ok ?? true, message: done?.message ?? '完成', target_file: done?.target_file ?? '', applied: Boolean(done?.applied), unified_diff: unifiedDiff.trimEnd() }
}

export async function runForwardProcessflowNpeFixSse(
  params: {
    apply: boolean; gitUrl: string; gitBranch?: string; gitCommit?: string
    message: string; provider?: string; model?: string; uploadedFileNames?: string[]
    sessionId?: string
    history?: Array<{ role: 'user' | 'ai'; content: string }>
  },
  onEvent: (ev: BugfixSseEvent) => void,
  opts?: { signal?: AbortSignal },
): Promise<BugfixResponse> {
  return runBugfixSse('/api/bugfix/analyze', {
    apply: params.apply, git_url: params.gitUrl, git_branch: params.gitBranch ?? '',
    git_commit: params.gitCommit ?? '', message: params.message,
    provider: params.provider ?? 'deepseek', model: params.model ?? '',
    uploaded_file_names: params.uploadedFileNames ?? [],
    session_id: params.sessionId ?? null,
    history: (params.history ?? []).map(m => ({ role: m.role === 'ai' ? 'assistant' : 'user', content: m.content })),
  }, onEvent, opts?.signal)
}

export async function runForwardProcessflowNpeFixTestSse(
  params: {
    message: string; gitUrl: string; gitBranch?: string; gitCommit?: string
    provider?: string; model?: string; uploadedFileNames?: string[]
    history?: Array<{ role: 'user' | 'ai'; content: string }>
  },
  onEvent: (ev: BugfixSseEvent) => void,
  opts?: { signal?: AbortSignal },
): Promise<BugfixResponse> {
  return runBugfixSse('/api/bugfix/test-analyze', {
    apply: false, git_url: params.gitUrl, git_branch: params.gitBranch ?? '',
    git_commit: params.gitCommit ?? '', message: params.message,
    provider: params.provider ?? 'deepseek', model: params.model ?? '',
    uploaded_file_names: params.uploadedFileNames ?? [],
    history: (params.history ?? []).map(m => ({ role: m.role === 'ai' ? 'assistant' : 'user', content: m.content })),
  }, onEvent, opts?.signal)
}

// ─── 用户信息获取 ──────────────────────────────────────────────────────────────
// 已迁移至 invoke.ts: invoke('forward', 'getUserInfo')
// 请使用 import { getUserInfo } from './invoke'

// ─── Bugfix 历史记录 ───────────────────────────────────────────────────────────

export type BugfixHistoryItem = {
  id: string
  time: string
  project: string
  bugId: string
  bugTitle: string
  model: string
  depth?: string
  status: 'running' | 'success' | 'failed'
  outcome?: string   // success | error_end | failed
  duration?: string
  sessionId?: string
  prompt?: string
  gitUrl?: string
  gitBranch?: string
  gitCommit?: string
  nodeIo?: Record<string, any>  // 完整链路快照
}

export async function listBugfixHistory(params?: {
  page?: number
  page_size?: number
  project?: string
  bug_id?: string
}): Promise<{ ok: boolean; total: number; page: number; page_size: number; items: BugfixHistoryItem[] }> {
  const url = new URL('/api/bugfix-history', window.location.origin)
  if (params?.page)      url.searchParams.set('page', String(params.page))
  if (params?.page_size) url.searchParams.set('page_size', String(params.page_size))
  if (params?.project)   url.searchParams.set('project', params.project)
  if (params?.bug_id)    url.searchParams.set('bug_id', params.bug_id)
  return request(url.toString())
}

export async function createBugfixHistory(payload: {
  project: string
  bug_id: string
  bug_title: string
  model: string
  depth?: string
  status?: string
  duration_ms?: number
  session_id?: string
  prompt?: string
  git_url?: string
  git_branch?: string
  git_commit?: string
}): Promise<{ ok: boolean; id: number; item?: BugfixHistoryItem }> {
  return request('/api/bugfix-history', json('POST', payload))
}

export async function updateBugfixHistoryStatus(
  recordId: number,
  status: string,
  duration_ms?: number,
  node_io_json?: string,
  outcome?: string,
): Promise<{ ok: boolean; item?: BugfixHistoryItem }> {
  return request(`/api/bugfix-history/${recordId}/status`, json('PATCH', { status, duration_ms, node_io_json, outcome }))
}

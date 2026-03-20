import { computed, ref, watch } from 'vue'

// 项目元数据（来自图谱）
interface ProjectMeta {
  name: string
  url: string
  branch?: string
  commitHash?: string
}

type ProjectSource = 'graphProjects' | 'cacheApplicationProjects' | 'gitRepos'

export function useRepo(opts?: { lazyRefFetch?: boolean; projectSource?: ProjectSource }) {
  const lazyRefFetch = Boolean(opts?.lazyRefFetch)
  const projectSource: ProjectSource = opts?.projectSource ?? 'graphProjects'
  const repos = ref<ProjectMeta[]>([])
  const loadingRepos = ref(false)
  const repoMode = ref<'select' | 'custom'>('select')
  const repoUrl = ref<string>('')
  const selectedProjectName = ref<string>('')
  const branch = ref<string | undefined>(undefined)
  const commitId = ref<string | undefined>(undefined)
  const branches = ref<string[]>([])
  const commits = ref<string[]>([])
  const loadingBranches = ref(false)
  const loadingCommits = ref(false)
  const branchSearch = ref('')
  const commitSearch = ref('')

  let branchSearchTimer: number | undefined
  let commitSearchTimer: number | undefined

  // 当前选中的项目元数据
  const selectedMeta = computed<ProjectMeta | undefined>(() =>
    repos.value.find((x) => x.name === selectedProjectName.value),
  )

  const repo = computed<{ name: string; url: string } | undefined>(() => {
    if (repoMode.value === 'select') {
      if (!selectedMeta.value) return undefined
      return { name: selectedMeta.value.name, url: selectedMeta.value.url }
    }
    if (!repoUrl.value) return undefined
    const r = repos.value.find((x) => x.url === repoUrl.value)
    if (r) return { name: r.name, url: r.url }
    const name =
      repoUrl.value.split('/').filter(Boolean).pop()?.replace(/\.git$/i, '') ?? repoUrl.value
    return { name, url: repoUrl.value }
  })

  // select 模式不需要手动填 branch/commit
  const needRef = computed(() => repoMode.value === 'custom')

  // select 模式下只读展示的字段（来自元数据）
  const readonlyUrl = computed(() =>
    repoMode.value === 'select' ? (selectedMeta.value?.url ?? '') : '',
  )
  const readonlyBranch = computed(() =>
    repoMode.value === 'select' ? (selectedMeta.value?.branch ?? '') : '',
  )
  const readonlyCommit = computed(() =>
    repoMode.value === 'select' ? (selectedMeta.value?.commitHash ?? '') : '',
  )

  const repoOptions = computed(() =>
    repos.value.map((r) => ({
      label: r.name,
      value: r.name,
      gitUrl: r.url || '',
      branch: r.branch || '',
      commitHash: r.commitHash || '',
    })),
  )
  const branchOptions = computed(() =>
    branches.value.map((b) => ({ label: b, value: b })),
  )
  const commitOptions = computed(() =>
    commits.value.map((c) => ({ label: c.slice(0, 8), value: c })),
  )
  const branchDisabled = computed(() => Boolean(commitId.value))
  const commitDisabled = computed(() => Boolean(branch.value))
  const canSelectRef = computed(() => Boolean(repo.value))
  const canRun = computed(() => {
    if (!repo.value) return false
    if (repoMode.value === 'select') return true
    return Boolean(branch.value) || Boolean(commitId.value)
  })
  const uploadRef = computed(
    () => branch.value ?? (commitId.value ? commitId.value.slice(0, 8) : '') ?? '',
  )
  const canUpload = computed(
    () => Boolean(repo.value?.name) && Boolean(uploadRef.value),
  )

  async function fetchRepos() {
    try {
      if (projectSource === 'cacheApplicationProjects') {
        loadingRepos.value = true
        const resp = await fetch('/api/cache/application-projects')
        if (!resp.ok) throw new Error(`加载 application-projects 失败：${resp.status}`)
        const data = await resp.json()
        const items: Array<{
          project_name: string
          repo_url?: string
          branch?: string
          commit_hash?: string
        }> = Array.isArray(data?.items) ? data.items : []
        repos.value = items
          .filter((x) => x.project_name && String(x.project_name).trim() && x.project_name !== '(error)' && x.project_name !== '(unknown)')
          .map((x) => ({
            name: String(x.project_name),
            url: String(x.repo_url ?? ''),
            branch: x.branch ? String(x.branch) : '',
            commitHash: x.commit_hash ? String(x.commit_hash) : '',
          }))
        return
      }

      if (projectSource === 'gitRepos') {
        loadingRepos.value = true
        const resp = await fetch('/api/git-repos')
        if (!resp.ok) throw new Error(`加载 git-repos 失败：${resp.status}`)
        const data = await resp.json()
        repos.value = (Array.isArray(data?.gitList) ? data.gitList : []).map((x: any) => ({
          name: x.name,
          url: x.url,
        }))
        return
      }

      // graphProjects: 从 Neo4j 里列出 Project 节点（含 branch/commit_hash/次数信息）
      loadingRepos.value = true
      const resp = await fetch('/api/graph/projects')
      if (resp.ok) {
        const data = await resp.json()
        const items: Array<{
          project_name: string
          repo_url?: string
          branch?: string
          commit_hash?: string
        }> = Array.isArray(data?.items) ? data.items : []
        repos.value = items
          .filter(
            (x) =>
              x.project_name &&
              x.project_name !== '(error)' &&
              x.project_name !== '(unknown)',
          )
          .map((x) => ({
            name: x.project_name,
            url: x.repo_url ?? '',
            branch: x.branch ?? '',
            commitHash: x.commit_hash ?? '',
          }))
        return
      }
    } catch {
      // 兜底
      try {
        const resp = await fetch('/api/git-repos')
        if (!resp.ok) throw new Error(`加载仓库失败：${resp.status}`)
        const data = await resp.json()
        repos.value = (Array.isArray(data?.gitList) ? data.gitList : []).map((x: any) => ({
          name: x.name,
          url: x.url,
        }))
      } catch {}
    } finally {
      loadingRepos.value = false
    }
  }

  async function ensureReposLoaded() {
    if (repos.value.length) return
    if (loadingRepos.value) return
    await fetchRepos()
  }

  async function onProjectDropdown(open: boolean) {
    if (!open) return
    if (repoMode.value !== 'select') return
    await ensureReposLoaded()
  }

  function switchRepoMode(mode: 'select' | 'custom') {
    repoMode.value = mode
    repoUrl.value = ''
    selectedProjectName.value = ''
    branch.value = undefined
    commitId.value = undefined
    branches.value = []
    commits.value = []
  }

  async function fetchBranches(q?: string) {
    if (!repo.value?.url) return
    loadingBranches.value = true
    try {
      const url = new URL('/api/git-branches', window.location.origin)
      url.searchParams.set('repoUrl', repo.value.url)
      if (q?.trim()) url.searchParams.set('q', q.trim())
      const data = await fetch(url.toString()).then((r) => r.json())
      branches.value = Array.isArray(data?.items) ? data.items : []
    } finally {
      loadingBranches.value = false
    }
  }

  async function fetchCommits(q?: string) {
    if (!repo.value?.url) return
    loadingCommits.value = true
    try {
      const url = new URL('/api/git-commits', window.location.origin)
      url.searchParams.set('repoUrl', repo.value.url)
      if (branch.value) url.searchParams.set('branch', branch.value)
      if (q?.trim()) url.searchParams.set('q', q.trim())
      const data = await fetch(url.toString()).then((r) => r.json())
      commits.value = Array.isArray(data?.items) ? data.items : []
    } finally {
      loadingCommits.value = false
    }
  }

  function onBranchSearch(val: string) {
    branchSearch.value = val
    if (!repo.value) return
    if (branchSearchTimer) window.clearTimeout(branchSearchTimer)
    branchSearchTimer = window.setTimeout(() => void fetchBranches(branchSearch.value), 200)
  }

  function onCommitSearch(val: string) {
    commitSearch.value = val
    if (!repo.value) return
    if (commitSearchTimer) window.clearTimeout(commitSearchTimer)
    commitSearchTimer = window.setTimeout(() => void fetchCommits(commitSearch.value), 200)
  }

  async function onBranchDropdown(open: boolean) {
    if (!open || !repo.value?.url) return
    if (branches.value.length) return
    await fetchBranches(branchSearch.value)
  }

  async function onCommitDropdown(open: boolean) {
    if (!open || !repo.value?.url) return
    if (commits.value.length) return
    await fetchCommits(commitSearch.value)
  }

  // custom 模式：URL 变化时重置 ref
  watch(repoUrl, async () => {
    if (repoMode.value !== 'custom') return
    branch.value = undefined
    commitId.value = undefined
    branches.value = []
    commits.value = []
    branchSearch.value = ''
    commitSearch.value = ''
    if (!repo.value) return
    if (!lazyRefFetch) {
      await fetchBranches()
      await fetchCommits()
    }
  })

  // select 模式：选中项目后，自动把元数据里的 ref 写入实际用于后端的字段（只读展示，不给用户编辑）
  watch(selectedProjectName, () => {
    if (repoMode.value !== 'select') return
    const meta = selectedMeta.value
    // 清空，避免切换项目后残留
    branch.value = undefined
    commitId.value = undefined

    if (!meta) return

    const commit = String(meta.commitHash ?? '').trim()
    const br = String(meta.branch ?? '').trim()

    // 优先使用 commitHash（因为通常能精准定位），否则用 branch
    if (commit) {
      commitId.value = commit
    } else if (br) {
      branch.value = br
    }
  })

  watch(branch, async () => {
    // 仅 custom 模式下才允许触发 fetchCommits（select 模式是只读展示）
    if (repoMode.value !== 'custom') return
    if (!repo.value?.url || !branch.value) return
    commitId.value = undefined
    commitSearch.value = ''
    await fetchCommits()
  })

  return {
    repos,
    repoMode,
    repoUrl,
    selectedProjectName,
    needRef,
    readonlyUrl,
    readonlyBranch,
    readonlyCommit,
    branch,
    commitId,
    branches,
    commits,
    loadingRepos,
    loadingBranches,
    loadingCommits,
    branchSearch,
    commitSearch,
    repo,
    repoOptions,
    branchOptions,
    commitOptions,
    branchDisabled,
    commitDisabled,
    canSelectRef,
    canRun,
    uploadRef,
    canUpload,
    fetchRepos,
    switchRepoMode,
    fetchBranches,
    fetchCommits,
    onProjectDropdown,
    onBranchSearch,
    onCommitSearch,
    onBranchDropdown,
    onCommitDropdown,
  }
}

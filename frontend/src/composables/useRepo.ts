import { computed, ref, watch } from 'vue'
import { listApplicationProjects, listGitRepos, listGraphProjects, listGraphProjectVersions, listGitBranches, listGitCommits } from '../api'
import type { GraphProjectVersion } from '../api'

// 项目元数据（来自图谱）
interface ProjectMeta {
  name: string
  url: string
  branch?: string
  commitHash?: string
  id?: number | null
  appType?: string    // 'frontend' | 'backend'
  language?: string   // 'java' | 'python' | 'go' | 'other' | 'vue' | 'react'
  hasGraph?: boolean  // 是否已有代码图谱（commit_hash 非空视为有）
  graphVersion?: string  // 图谱版本号（从 project_key 解析）
}

type ProjectSource = 'graphProjects' | 'cacheApplicationProjects' | 'gitRepos'

export function useRepo(opts?: { lazyRefFetch?: boolean; projectSource?: ProjectSource; autoFillRefFromMeta?: boolean }) {
  const lazyRefFetch = Boolean(opts?.lazyRefFetch)
  const projectSource: ProjectSource = opts?.projectSource ?? 'graphProjects'
  const autoFillRefFromMeta = opts?.autoFillRefFromMeta !== false
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

  // 版本列表（仅 cacheApplicationProjects + 有图谱时使用）
  const graphVersions = ref<GraphProjectVersion[]>([])
  const loadingVersions = ref(false)
  const selectedVersion = ref<string>('')

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
  const readonlyBranch = computed(() => {
    if (repoMode.value !== 'select') return ''
    // 有版本选择时，优先展示版本对应的 branch
    if (selectedVersion.value && graphVersions.value.length > 0) {
      const ver = graphVersions.value.find((v) => v.version === selectedVersion.value)
      if (ver) return ver.branch || ''
    }
    return selectedMeta.value?.branch ?? ''
  })
  const readonlyCommit = computed(() => {
    if (repoMode.value !== 'select') return ''
    // 有版本选择时，优先展示版本对应的 commit_hash
    if (selectedVersion.value && graphVersions.value.length > 0) {
      const ver = graphVersions.value.find((v) => v.version === selectedVersion.value)
      if (ver) return ver.commit_hash || ''
    }
    return selectedMeta.value?.commitHash ?? ''
  })

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
    // commit id 可能很长：label 使用完整值，避免用户复制时拿不到 full id
    commits.value.map((c) => ({ label: c, value: c })),
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
    () => branch.value ?? (commitId.value ? commitId.value : '') ?? '',
  )
  const canUpload = computed(
    () => Boolean(repo.value?.name) && Boolean(uploadRef.value),
  )

  async function fetchRepos() {
    try {
      if (projectSource === 'cacheApplicationProjects') {
        loadingRepos.value = true
        const [appData, graphData] = await Promise.all([
          listApplicationProjects(),
          listGraphProjects().catch(() => ({ ok: false, items: [] as any[] })),
        ])
        const items = Array.isArray(appData?.items) ? appData.items : []
        // 建立 project_name -> version 的映射（从图谱数据）
        const versionMap = new Map<string, string>()
        for (const g of (Array.isArray(graphData?.items) ? graphData.items : [])) {
          const name = String((g as any)?.project_name || '').trim()
          const ver = String((g as any)?.version || '').trim()
          if (name && ver) versionMap.set(name, ver)
        }
        repos.value = items
          .filter((x) => x.project_name && String(x.project_name).trim() && x.project_name !== '(error)' && x.project_name !== '(unknown)')
          .map((x) => ({
            name: String(x.project_name),
            url: String(x.repo_url ?? ''),
            branch: x.branch ? String(x.branch) : '',
            commitHash: x.commit_hash ? String(x.commit_hash) : '',
            id: x.id ?? null,
            appType: x.app_type ?? 'backend',
            language: x.language ?? 'java',
            hasGraph: Boolean(x.commit_hash),
            graphVersion: versionMap.get(String(x.project_name)) ?? '',
          }))
        return
      }

      if (projectSource === 'gitRepos') {
        loadingRepos.value = true
        const data = await listGitRepos()
        repos.value = (Array.isArray(data?.gitList) ? data.gitList : []).map((x: any) => ({
          name: x.name,
          url: x.url,
        }))
        return
      }

      // graphProjects
      loadingRepos.value = true
      const data = await listGraphProjects()
      const items = Array.isArray(data?.items) ? data.items : []
      repos.value = items
        .filter((x) => x.project_name && x.project_name !== '(error)' && x.project_name !== '(unknown)')
        .map((x) => ({
          name: x.project_name,
          url: x.repo_url ?? '',
          branch: x.branch ?? '',
          commitHash: x.commit_hash ?? '',
        }))
    } catch {
      // 兜底
      try {
        const data = await listGitRepos()
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

  function _applyVersionRef(v: GraphProjectVersion) {
    branch.value = v.branch || undefined
    commitId.value = v.commit_hash || undefined
  }

  function selectVersion(version: string) {
    selectedVersion.value = version
    const found = graphVersions.value.find((v) => v.version === version)
    if (found) _applyVersionRef(found)
  }

  function switchRepoMode(mode: 'select' | 'custom') {
    repoMode.value = mode
    repoUrl.value = ''
    selectedProjectName.value = ''
    branch.value = undefined
    commitId.value = undefined
    branches.value = []
    commits.value = []
    selectedVersion.value = ''
    graphVersions.value = []
  }

  async function fetchBranches(q?: string) {
    if (!repo.value?.url) return
    loadingBranches.value = true
    try {
      const data = await listGitBranches(repo.value.url, q)
      branches.value = Array.isArray(data?.items) ? data.items : []
    } finally {
      loadingBranches.value = false
    }
  }

  async function fetchCommits(q?: string) {
    if (!repo.value?.url) return
    loadingCommits.value = true
    try {
      const data = await listGitCommits(repo.value.url, branch.value, q)
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

  // select 模式：选中项目后，可选地把元数据里的 ref 写入实际用于后端字段
  watch(selectedProjectName, async () => {
    if (repoMode.value !== 'select') return
    const meta = selectedMeta.value
    // 切换应用时，清空已选 ref + 下拉候选，避免沿用上一个应用的缓存列表
    branch.value = undefined
    commitId.value = undefined
    branches.value = []
    commits.value = []
    branchSearch.value = ''
    commitSearch.value = ''
    selectedVersion.value = ''
    graphVersions.value = []

    if (!meta) return

    // 有图谱的后端 Java 应用：加载版本列表
    if (meta.hasGraph && meta.appType === 'backend' && meta.language === 'java' && projectSource === 'cacheApplicationProjects') {
      loadingVersions.value = true
      try {
        const data = await listGraphProjectVersions(meta.name)
        graphVersions.value = Array.isArray(data?.items) ? data.items : []
        // 自动选中第一个版本（最新）
        if (graphVersions.value.length > 0) {
          const first = graphVersions.value[0]
          selectedVersion.value = first.version
          _applyVersionRef(first)
        }
      } catch {
        graphVersions.value = []
      } finally {
        loadingVersions.value = false
      }
      return
    }

    if (!autoFillRefFromMeta) return

    const commit = String(meta.commitHash ?? '').trim()
    const br = String(meta.branch ?? '').trim()

    // 优先使用 commitHash（因为通常能精准定位），否则用 branch
    if (commit) {
      commitId.value = commit
    } else if (br) {
      branch.value = br
    }

    // 非 lazy 模式下，切换应用后立即加载该应用 refs
    if (!lazyRefFetch) {
      await fetchBranches()
      await fetchCommits()
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
    selectedMeta,
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
    // 所选应用的类型信息
    selectedAppType: computed(() => selectedMeta.value?.appType ?? 'backend'),
    selectedLanguage: computed(() => selectedMeta.value?.language ?? 'java'),
    selectedHasGraph: computed(() => Boolean(selectedMeta.value?.hasGraph)),
    selectedGraphVersion: computed(() => selectedMeta.value?.graphVersion ?? ''),
    // 版本列表
    graphVersions,
    loadingVersions,
    selectedVersion,
    selectVersion,
  }
}

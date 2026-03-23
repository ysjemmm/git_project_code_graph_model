<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, ReloadOutlined, SyncOutlined, LinkOutlined, DeleteOutlined, ApartmentOutlined } from '@ant-design/icons-vue'
import MavenGuideModal from '../components/MavenGuideModal.vue'
import {
  listApplicationProjects,
  listApplicationLinkedBy,
  getImportSettings,
  getAppDependencies,
  refreshAppDependencies,
  listDependencyLinks,
  listGraphProjects,
  upsertDependencyLink,
  upsertDependencyLinksByParent,
  deleteDependencyLink,
  syncDependencyLinksToNeo4j,
  type CacheProjectItem,
  type AppDependency,
  type AppDependencyLink,
  type LinkedByItem,
} from '../api'

// 还原 re.escape 转义，用于展示规则内容（模板中使用）
function unescapeRegex(s: string): string {
  return s.replace(/\\([^A-Za-z0-9_])/g, '$1')
}

const route = useRoute()
const router = useRouter()

const repoName = computed(() => String(route.params.repoName || '').trim())

const loading = ref(false)
const item = ref<CacheProjectItem | null>(null)

const depsLoading = ref(false)
const depsRefreshing = ref(false)
const deps = ref<AppDependency[]>([])
const depsScannedAt = ref<string | null>(null)
const depsError = ref<string | null>(null)
const mavenEnabled = ref<boolean | null>(null)
const linkedByLoading = ref(false)
const linkedByItems = ref<LinkedByItem[]>([])
const showOnlySecondParty = ref(false)
const scopeFilter = ref<string>('all')  // all / compile / test / provided / runtime

const depsCollapseKey = ref<string[]>([])  // 默认折叠；展开时为 ['deps']

const scopeOptions = [
  { label: '全部', value: 'all' },
  { label: 'compile', value: 'compile' },
  { label: 'test', value: 'test' },
  { label: 'provided', value: 'provided' },
  { label: 'runtime', value: 'runtime' },
]

onMounted(async () => {
  await fetchDetail({ refresh: false })
  await fetchImportSettings()
  void fetchDeps()
  void fetchLinks()
  void fetchLinkedBy()
  void fetchAllProjects()
})

const filteredDeps = computed(() => {
  let list = deps.value
  if (scopeFilter.value !== 'all') list = list.filter(d => d.scope === scopeFilter.value)
  if (showOnlySecondParty.value) list = list.filter(d => d.is_second_party)
  return list
})
const secondPartyCount = computed(() => deps.value.filter((d) => d.is_second_party).length)
const thirdPartyCount = computed(() => deps.value.filter((d) => !d.is_second_party).length)

function isParentLikeDep(dep: AppDependency): boolean {
  const artifact = String(dep.artifact_id || '').toLowerCase()
  const scope = String(dep.scope || '').toLowerCase()
  if (scope === 'import') return true
  return artifact.endsWith('-parent') || artifact.endsWith('-bom') || artifact.endsWith('-dependencies')
}

function parentPriority(dep: AppDependency): number {
  const artifact = String(dep.artifact_id || '').toLowerCase()
  if (artifact.endsWith('-parent')) return 1
  if (artifact.endsWith('-bom')) return 2
  if (artifact.endsWith('-dependencies')) return 3
  if (String(dep.scope || '').toLowerCase() === 'import') return 4
  return 9
}

const parentLabelMap = computed(() => {
  const map = new Map<string, string>()
  const parents = deps.value
    .filter(isParentLikeDep)
    .slice()
    .sort((a, b) => parentPriority(a) - parentPriority(b))

  const keyExact = (g: string, v: string) => `${g}::${v}`
  const keyGroup = (g: string) => `${g}::`
  const labelOf = (d: AppDependency) =>
    `${d.group_id}:${d.artifact_id}${d.version ? `:${d.version}` : ''}`

  const exactMap = new Map<string, AppDependency>()
  const groupMap = new Map<string, AppDependency>()

  for (const p of parents) {
    const g = String(p.group_id || '')
    const v = String(p.version || '')
    if (!g) continue
    if (v && !exactMap.has(keyExact(g, v))) exactMap.set(keyExact(g, v), p)
    if (!groupMap.has(keyGroup(g))) groupMap.set(keyGroup(g), p)
  }

  const depKey = (d: AppDependency) =>
    `${d.group_id}:${d.artifact_id}:${d.scope}:${d.parent_group_id || ''}:${d.parent_artifact_id || ''}:${d.parent_version || ''}`

  for (const d of deps.value) {
    const g = String(d.group_id || '')
    const v = String(d.version || '')
    if (!g) {
      map.set(depKey(d), '-')
      continue
    }
    const selfKey = depKey(d)
    if (isParentLikeDep(d)) {
      map.set(selfKey, labelOf(d))
      continue
    }
    const p = (v ? exactMap.get(keyExact(g, v)) : undefined) || groupMap.get(keyGroup(g))
    map.set(selfKey, p ? labelOf(p) : '-')
  }

  return map
})

function getParentLabel(dep: AppDependency): string {
  // 优先使用后端返回的 parent 字段
  const pg = String(dep.parent_group_id || '').trim()
  const pa = String(dep.parent_artifact_id || '').trim()
  const pv = String(dep.parent_version || '').trim()
  if (pg && pa) {
    return `${pg}:${pa}${pv ? `:${pv}` : ''}`
  }
  const key = `${dep.group_id}:${dep.artifact_id}:${dep.scope}:${dep.parent_group_id || ''}:${dep.parent_artifact_id || ''}:${dep.parent_version || ''}`
  return parentLabelMap.value.get(key) || '-'
}

type ParentTreeRow = {
  key: string
  __is_parent_group: true
  parent: string
  parent_group_id: string
  parent_artifact_id: string
  parent_version: string
  child_count: number
  second_party_count: number
  children: Array<AppDependency & { key: string; __is_parent_group: false; parent: string }>
}

function parseParentLabel(label: string): { group_id: string; artifact_id: string; version: string } {
  if (!label || label === '-') return { group_id: '', artifact_id: '', version: '' }
  const parts = String(label).split(':')
  return {
    group_id: parts[0] || '',
    artifact_id: parts[1] || '',
    version: parts.slice(2).join(':') || '',
  }
}

const DEFAULT_PARENT_LABEL = 'default'

const depsTreeData = computed(() => {
  const grouped = new Map<string, Array<AppDependency & { key: string; __is_parent_group: false; parent: string }>>()
  for (const dep of filteredDeps.value) {
    let parent = getParentLabel(dep)
    if (!parent || parent === '-' || parent.trim() === '') {
      parent = DEFAULT_PARENT_LABEL
    }
    const list = grouped.get(parent) || []
    list.push({
      ...dep,
      key: `dep::${dep.group_id}:${dep.artifact_id}:${dep.scope}:${dep.parent_group_id || ''}:${dep.parent_artifact_id || ''}:${dep.parent_version || ''}`,
      __is_parent_group: false,
      parent,
    })
    grouped.set(parent, list)
  }

  const rows: ParentTreeRow[] = []
  for (const [parent, children] of grouped.entries()) {
    children.sort((a, b) => {
      const aKey = `${a.artifact_id || ''}:${a.group_id || ''}:${a.version || ''}:${a.scope || ''}`
      const bKey = `${b.artifact_id || ''}:${b.group_id || ''}:${b.version || ''}:${b.scope || ''}`
      return aKey.localeCompare(bKey)
    })
    const p = parseParentLabel(parent)
    const secondPartyCount = children.filter((x) => Boolean(x.is_second_party)).length
    rows.push({
      key: `parent::${parent}`,
      __is_parent_group: true,
      parent,
      parent_group_id: p.group_id,
      parent_artifact_id: p.artifact_id,
      parent_version: p.version,
      child_count: children.length,
      second_party_count: secondPartyCount,
      children,
    })
  }

  rows.sort((a, b) => {
    const aHasSecond = a.second_party_count > 0
    const bHasSecond = b.second_party_count > 0
    if (aHasSecond !== bHasSecond) return aHasSecond ? -1 : 1
    if (a.parent === DEFAULT_PARENT_LABEL && b.parent !== DEFAULT_PARENT_LABEL) return 1
    if (a.parent !== DEFAULT_PARENT_LABEL && b.parent === DEFAULT_PARENT_LABEL) return -1
    return a.parent.localeCompare(b.parent)
  })
  return rows
})

async function fetchDetail({ refresh = false }: { refresh?: boolean } = {}) {
  if (!repoName.value) return
  loading.value = true
  try {
    const data = await listApplicationProjects(refresh)
    const list: CacheProjectItem[] = Array.isArray(data?.items) ? data.items : []
    item.value = list.find((x) => String(x.repo_name) === repoName.value) ?? null
    // 复用同一次请求结果，避免重复打接口
    allProjects.value = list
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    loading.value = false
  }
}

async function fetchImportSettings() {
  if (!item.value?.id) return
  try {
    const data = await getImportSettings(item.value.id as number)
    mavenEnabled.value = Boolean(data?.settings?.maven_scan_enabled ?? true)
  } catch {
    mavenEnabled.value = null
  }
}

async function fetchDeps() {
  if (!item.value?.id) return
  depsLoading.value = true
  depsError.value = null
  try {
    const data = await getAppDependencies(item.value.id as number)
    deps.value = Array.isArray(data.items) ? data.items : []
    depsScannedAt.value = data.scanned_at ?? null
  } catch (e: any) {
    depsError.value = e?.message ?? String(e)
  } finally {
    depsLoading.value = false
  }
}

async function doRefreshDeps() {
  if (!item.value?.id) return
  depsRefreshing.value = true
  try {
    const data = await refreshAppDependencies(item.value.id as number)
    const parentCount = Number(data.parent_classified_count || 0)
    const extra = parentCount > 0 ? `（其中按父级/BOM 归类 ${parentCount} 个）` : ''
    message.success({
      content: `解析完成，共 ${data.total} 个依赖，其中二方包 ${data.second_party_count} 个${extra}`,
      duration: 4,
    })
    await fetchDeps()
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 6 })
  } finally {
    depsRefreshing.value = false
  }
}

function backToList() {
  void router.push('/application-admin')
}

function goLinkOverview() {
  if (!repoName.value) return
  void router.push(`/application-admin/${encodeURIComponent(repoName.value)}/link-overview`)
}

function openLinkGraphView(row: ParentTreeRow, link?: AppDependencyLink | null) {
  if (!repoName.value) {
    message.warning('当前项目信息缺失，暂无法打开视图')
    return
  }
  const query: Record<string, string> = {}
  if (row.parent && row.parent !== DEFAULT_PARENT_LABEL) {
    query.parent = row.parent
  }
  if (link?.linked_app_id) {
    query.linkedAppId = String(link.linked_app_id)
  }
  if (link?.linked_project_name) {
    query.linkedProject = String(link.linked_project_name)
  }
  void router.push({
    path: `/application-admin/${encodeURIComponent(repoName.value)}/link-overview`,
    query,
  })
}

async function refresh() {
  await fetchDetail({ refresh: true })
  await fetchImportSettings()
  void fetchDeps()
  void fetchLinks()
  void fetchLinkedBy()
}

// ─── 依赖关联 ─────────────────────────────────────────────────────────────────
const links = ref<AppDependencyLink[]>([])
const allProjects = ref<CacheProjectItem[]>([])
const graphReadyProjectNames = ref<Set<string>>(new Set())

// 关联弹窗
const linkModalVisible = ref(false)
const linkModalDep = ref<AppDependency | null>(null)  // 当前正在关联的依赖行
const linkModalParent = ref<ParentTreeRow | null>(null) // 当前正在按 parent 批量关联
const linkModalMode = ref<'dep' | 'parent'>('dep')
const linkModalSelectedAppId = ref<number | null>(null)
const linkModalNote = ref('')
const linkModalSaving = ref(false)

// 同步到 Neo4j
const syncing = ref(false)

async function fetchLinks() {
  if (!item.value?.id) return
  try {
    const data = await listDependencyLinks(item.value.id as number)
    links.value = Array.isArray(data.items) ? data.items : []
  } catch { /* 静默 */ }
}

async function fetchLinkedBy() {
  if (!item.value?.id) return
  linkedByLoading.value = true
  try {
    const data = await listApplicationLinkedBy(item.value.id as number)
    linkedByItems.value = Array.isArray(data.items) ? data.items : []
  } catch {
    linkedByItems.value = []
  } finally {
    linkedByLoading.value = false
  }
}

async function fetchAllProjects() {
  // allProjects 由 fetchDetail 顺带填充，此函数保留作为独立刷新兜底
  if (allProjects.value.length > 0 && graphReadyProjectNames.value.size > 0) return
  try {
    const [apps, graphs] = await Promise.all([
      listApplicationProjects(false),
      listGraphProjects(true).catch(() => ({ ok: false, items: [] as any[] })),
    ])
    allProjects.value = Array.isArray(apps?.items) ? apps.items : []
    const ready = new Set<string>()
    for (const x of (Array.isArray(graphs?.items) ? graphs.items : [])) {
      const name = String((x as any)?.project_name || '').trim()
      const n = Number((x as any)?.node_count ?? 0)
      if (name && n > 0) ready.add(name)
    }
    graphReadyProjectNames.value = ready
  } catch { /* 静默 */ }
}

function goSourceAppDetail(repoName: string) {
  const target = String(repoName || '').trim()
  if (!target) return
  void router.push(`/application-admin/${encodeURIComponent(target)}`)
}

const linkProjectOptions = computed(() => {
  const currentId = Number(item.value?.id || 0)
  const list = allProjects.value
    .filter((p) => Number(p.id || 0) !== currentId)
    .map((p) => {
      const projectName = String(p.project_name || '').trim()
      const repoName = String(p.repo_name || '').trim()
      const labelText = projectName || repoName || '-'
      const ready = graphReadyProjectNames.value.has(projectName) || graphReadyProjectNames.value.has(repoName)
      return {
        value: Number(p.id || 0),
        labelText,
        ready,
        disabled: !ready,
      }
    })
    .sort((a, b) => {
      if (a.ready !== b.ready) return a.ready ? -1 : 1
      return a.labelText.localeCompare(b.labelText)
    })

  return list.map((x) => ({
    value: x.value,
    disabled: x.disabled,
    searchText: x.labelText.toLowerCase(),
    title: x.ready ? x.labelText : '未构建图谱',
    label: h(
      'span',
      { title: x.ready ? x.labelText : '未构建图谱' },
      x.ready ? x.labelText : `${x.labelText}（未构建图谱）`,
    ),
  }))
})

function depLinkKey(dep: Pick<AppDependency, 'group_id' | 'artifact_id'>): string {
  return `${String(dep.group_id || '')}::${String(dep.artifact_id || '')}`
}

const linkByKey = computed(() => {
  const m = new Map<string, AppDependencyLink>()
  for (const l of links.value) {
    m.set(`${String(l.group_id || '')}::${String(l.artifact_id || '')}`, l)
  }
  return m
})

const parentLockedChildKeySet = computed(() => {
  const locked = new Set<string>()
  for (const row of depsTreeData.value) {
    if (row.parent === DEFAULT_PARENT_LABEL) continue
    if (row.child_count <= 0 || row.second_party_count !== row.child_count) continue
    const childLinks = row.children
      .map((c) => linkByKey.value.get(depLinkKey(c)))
      .filter(Boolean) as AppDependencyLink[]
    if (childLinks.length !== row.children.length) continue
    const linkedIds = new Set(childLinks.map((x) => Number(x.linked_app_id)))
    if (linkedIds.size !== 1) continue
    for (const c of row.children) locked.add(depLinkKey(c))
  }
  return locked
})

function isDepLockedByParent(dep: AppDependency): boolean {
  return parentLockedChildKeySet.value.has(depLinkKey(dep))
}

function getParentUniformLink(row: ParentTreeRow): AppDependencyLink | null {
  if (!row || !Array.isArray(row.children) || row.children.length === 0) return null
  const childLinks = row.children
    .map((c) => linkByKey.value.get(depLinkKey(c)))
    .filter(Boolean) as AppDependencyLink[]
  if (childLinks.length !== row.children.length) return null
  const linkedIds = new Set(childLinks.map((x) => Number(x.linked_app_id)))
  if (linkedIds.size !== 1) return null
  return childLinks[0] ?? null
}

function openLinkModal(dep: AppDependency) {
  if (isDepLockedByParent(dep)) {
    message.warning('该依赖已由 parent 整组关联锁定，请在父级行使用“关联整组”统一修改')
    return
  }
  linkModalMode.value = 'dep'
  linkModalDep.value = dep
  linkModalParent.value = null
  // 预填已有关联
  const existing = links.value.find(
    l => l.group_id === dep.group_id && l.artifact_id === dep.artifact_id
  )
  linkModalSelectedAppId.value = existing?.linked_app_id ?? null
  linkModalNote.value = existing?.note ?? ''
  linkModalVisible.value = true
}

function openParentLinkModal(row: ParentTreeRow) {
  linkModalMode.value = 'parent'
  linkModalParent.value = row
  linkModalDep.value = null
  // 若该 parent 下已关联且目标项目一致，则预填
  const childLinks = row.children
    .map((c) => getLinkForDep(c))
    .filter(Boolean) as AppDependencyLink[]
  const linkedIds = Array.from(new Set(childLinks.map((x) => Number(x.linked_app_id)).filter((x) => Number.isFinite(x))))
  linkModalSelectedAppId.value = linkedIds.length === 1 ? linkedIds[0] : null
  linkModalNote.value = childLinks[0]?.note || ''
  linkModalVisible.value = true
}

async function saveLinkModal() {
  if (!item.value?.id || !linkModalSelectedAppId.value) return
  linkModalSaving.value = true
  try {
    if (linkModalMode.value === 'parent') {
      if (!linkModalParent.value) return
      const row = linkModalParent.value
      if (!row.parent_group_id || !row.parent_artifact_id || row.parent === DEFAULT_PARENT_LABEL) {
        message.warning('default 分组不支持整组关联，请按具体依赖逐条关联')
        return
      }
      const data = await upsertDependencyLinksByParent(item.value.id as number, {
        parent_group_id: row.parent_group_id,
        parent_artifact_id: row.parent_artifact_id,
        parent_version: row.parent_version || '',
        linked_app_id: linkModalSelectedAppId.value,
        note: linkModalNote.value,
      })
      if (data.neo4j_synced) {
        message.success(`父级分组关联已保存，共影响 ${data.affected_count} 条依赖，图谱已同步`)
      } else {
        message.success(data.neo4j_message
          ? `父级分组关联已保存（图谱同步失败：${data.neo4j_message}）`
          : `父级分组关联已保存，共影响 ${data.affected_count} 条依赖（Neo4j 未配置，图谱未同步）`
        )
      }
    } else {
      if (!linkModalDep.value) return
      if (isDepLockedByParent(linkModalDep.value)) {
        message.warning('该依赖已由 parent 整组关联锁定，请在父级行使用“关联整组”统一修改')
        return
      }
      const data = await upsertDependencyLink(item.value.id as number, {
        group_id: linkModalDep.value.group_id,
        artifact_id: linkModalDep.value.artifact_id,
        linked_app_id: linkModalSelectedAppId.value,
        note: linkModalNote.value,
      })
      if (data.neo4j_synced) {
        message.success('关联已保存，DEPENDS_ON 边已同步到图谱')
      } else {
        message.success(data.neo4j_message
          ? `关联已保存（图谱同步失败：${data.neo4j_message}）`
          : '关联已保存（Neo4j 未配置，图谱未同步）'
        )
      }
    }
    linkModalVisible.value = false
    await fetchLinks()
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    linkModalSaving.value = false
  }
}

async function removeLink(dep: AppDependency) {
  if (isDepLockedByParent(dep)) {
    message.warning('该依赖已由 parent 整组关联锁定，请在父级行调整整组关联')
    return
  }
  const existing = links.value.find(
    l => l.group_id === dep.group_id && l.artifact_id === dep.artifact_id
  )
  if (!existing) return
  // 二次确认，防误触
  const { Modal } = await import('ant-design-vue')
  Modal.confirm({
    title: '确认删除关联？',
    content: `将解除 ${dep.artifact_id} 与项目「${existing.linked_project_name}」的关联，同时删除图谱中的 DEPENDS_ON 边。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteDependencyLink(existing.id)
        message.success('关联已删除')
        await fetchLinks()
      } catch (e: any) {
        message.error(e?.message ?? String(e))
      }
    },
  })
}

async function doSyncToNeo4j() {
  syncing.value = true
  try {
    const data = await syncDependencyLinksToNeo4j()
    if (data.ok) {
      message.success(`同步完成，共写入 ${data.synced} 条 DEPENDS_ON 边`)
    } else {
      message.warning(data.message ?? '同步失败')
    }
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    syncing.value = false
  }
}

// 给依赖行查找已有关联
function getLinkForDep(dep: AppDependency): AppDependencyLink | undefined {
  return links.value.find(l => l.group_id === dep.group_id && l.artifact_id === dep.artifact_id)
}

// 二方包规则详情弹窗
const ruleDetailVisible = ref(false)
const ruleDetailDep = ref<AppDependency | null>(null)

function openRuleDetail(dep: AppDependency) {
  ruleDetailDep.value = dep
  ruleDetailVisible.value = true
}

const depColumns = [
  { title: 'parent', key: 'parent', width: 360, ellipsis: true },
  { title: 'artifactId', dataIndex: 'artifact_id', key: 'artifact_id', width: 260, ellipsis: true },
  { title: 'groupId', dataIndex: 'group_id', key: 'group_id', width: 220, ellipsis: true },
  { title: 'version', dataIndex: 'version', key: 'version', width: 160, ellipsis: true },
  { title: 'scope', dataIndex: 'scope', key: 'scope', width: 100, ellipsis: true },
  { title: '关键信息', key: 'is_second_party', width: 220, ellipsis: true },
  { title: '关联项目', key: 'link', width: 180, ellipsis: true },
] as const

const linkedByColumns = [
  { title: '来源项目', dataIndex: 'source_project_name', key: 'source_project_name', ellipsis: true },
  { title: '来源 repo', dataIndex: 'source_repo_name', key: 'source_repo_name', ellipsis: true },
  { title: '关联次数', dataIndex: 'link_count', key: 'link_count', width: 90 },
  { title: '最近关联', dataIndex: 'last_linked_at', key: 'last_linked_at', width: 180, ellipsis: true },
  { title: '操作', key: 'action', width: 90 },
] as const
</script>

<template>
  <div class="app-detail-page">
    <a-space style="margin-bottom: 12px">
      <a-button type="text" @click="backToList">
        <template #icon><ArrowLeftOutlined /></template>
        返回应用管理
      </a-button>

      <a-button :loading="loading" @click="refresh">
        <template #icon><ReloadOutlined /></template>
        刷新详情
      </a-button>

      <a-button @click="goLinkOverview">
        <template #icon><ApartmentOutlined /></template>
        关联视图
      </a-button>
    </a-space>

    <a-empty v-if="!item && !loading" description="未找到该应用（可能已被删除或缓存尚未落库）" />

    <template v-else-if="item">
      <div class="title mono">{{ item.project_name || item.repo_name }}</div>

      <a-descriptions bordered size="small" :column="2" class="desc">
        <a-descriptions-item label="project_key" :span="2">
          <span class="mono">{{ item.project_key || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="Git 地址">
          <span class="mono">{{ item.repo_url || '-' }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="Git 缓存目录">
          <span class="mono">{{ item.cache_dir || '-' }}</span>
          <a-tag :color="item.repo_exists ? 'green' : 'red'" style="margin-left: 8px;">
            {{ item.repo_exists ? '已存在' : '不存在' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="最后更新时间" :span="2">
          <span class="mono">{{ item.last_update_time || '-' }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <div class="section-title">被哪些项目关联</div>
      <a-table
        :columns="linkedByColumns as any"
        :data-source="linkedByItems as any"
        :loading="linkedByLoading"
        :pagination="{ pageSize: 8 }"
        size="small"
        bordered
        row-key="source_app_id"
        :locale="{ emptyText: '当前暂无其他项目关联到本项目' }"
        style="margin-bottom: 12px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source_project_name' || column.key === 'source_repo_name' || column.key === 'last_linked_at'">
            <a-tooltip :title="record[column.key] || ''" placement="topLeft">
              <span class="mono cell-ellipsis">{{ record[column.key] || '-' }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'link_count'">
            <a-tag :color="Number(record.link_count || 0) > 0 ? 'blue' : 'default'">
              {{ Number(record.link_count || 0) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click.stop="goSourceAppDetail(record.source_repo_name)">视图</a-button>
          </template>
        </template>
      </a-table>

      <!-- Maven 依赖（可折叠） -->
      <a-collapse v-model:activeKey="depsCollapseKey" class="deps-collapse">
        <a-collapse-panel key="deps">
          <template #header>
            <div class="collapse-header">
              <span style="font-weight: 600;">Maven 依赖</span>
              <a-tag v-if="secondPartyCount > 0" color="blue" style="margin-left: 8px;">
                二方包 {{ secondPartyCount }} 个
              </a-tag>
              <a-tag v-if="thirdPartyCount > 0" color="default" style="margin-left: 4px;">
                三方包 {{ thirdPartyCount }} 个
              </a-tag>
              <MavenGuideModal style="margin-left: 8px;" />
              <span v-if="depsScannedAt" class="scanned-at">上次扫描：{{ depsScannedAt.slice(0, 19).replace('T', ' ') }}</span>
            </div>
          </template>

          <!-- Maven 未启用提示 -->
          <a-alert
            v-if="mavenEnabled === false"
            type="info"
            show-icon
            message="Maven 扫描未启用"
            description="该应用的「解析 pom 外部依赖（Maven 扫描）」已关闭。仍可手动点击「解析依赖」从 pom.xml 读取依赖信息。"
            style="margin-bottom: 12px;"
          />

          <div class="deps-toolbar">
            <a-button
              type="primary"
              size="small"
              :loading="depsRefreshing"
              :disabled="!item.repo_exists"
              @click.stop="doRefreshDeps"
            >
              <template #icon><SyncOutlined /></template>
              解析依赖
            </a-button>

            <a-button
              size="small"
              :loading="syncing"
              :disabled="links.length === 0"
              @click.stop="doSyncToNeo4j"
            >
              同步关联到图谱
            </a-button>

            <a-divider type="vertical" />

            <a-switch
              v-model:checked="showOnlySecondParty"
              checked-children="只看二方包"
              un-checked-children="全部"
              :disabled="depsLoading || depsRefreshing"
            />

            <a-select
              v-model:value="scopeFilter"
              size="small"
              style="width: 110px;"
              :options="scopeOptions"
              :disabled="depsLoading || depsRefreshing"
            />

            <span class="deps-count">共 {{ filteredDeps.length }} 个</span>
          </div>

          <a-alert
            v-if="depsError"
            type="warning"
            :message="depsError"
            show-icon
            style="margin-bottom: 10px;"
          />

          <a-alert
            v-else-if="deps.length > 0"
            type="info"
            show-icon
            message="说明：部分依赖 pom 里未显式声明 groupId，属于 Maven 的 parent 继承机制。表格中的 groupId 以解析后的有效坐标为准。"
            style="margin-bottom: 10px;"
          />

          <a-alert
            v-else-if="!depsLoading && !depsRefreshing && deps.length === 0"
            type="info"
            show-icon
            message="暂无依赖数据"
            description="点击「解析依赖」按钮从本地 pom.xml 解析依赖信息。"
            style="margin-bottom: 10px;"
          />

          <a-table
            v-if="deps.length > 0"
            :loading="depsLoading || depsRefreshing"
            :columns="depColumns as any"
            :data-source="depsTreeData as any"
            :pagination="{ pageSize: 25, showSizeChanger: true, pageSizeOptions: ['25','50','100'] }"
            size="small"
            bordered
            row-key="key"
            :default-expand-all-rows="true"
            :scroll="{ x: 800 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="record.__is_parent_group && ['artifact_id', 'group_id', 'version'].includes(String(column.key))">
                <template v-if="column.key === 'artifact_id'">
                  <span class="unknown">-</span>
                </template>
                <template v-else-if="column.key === 'group_id'">
                  <span class="unknown">-</span>
                </template>
                <template v-else-if="column.key === 'version'">
                  <span class="unknown">-</span>
                </template>
                <template v-else>
                  <span class="unknown">-</span>
                </template>
              </template>
              <template v-else-if="column.key === 'is_second_party' && record.__is_parent_group">
                <a-space size="small" wrap>
                  <a-tag v-if="record.child_count > 0 && record.second_party_count === record.child_count" color="blue">二方包</a-tag>
                  <a-tag v-else-if="record.second_party_count > 0" color="orange">混合</a-tag>
                  <a-tag v-else color="default">三方包</a-tag>
                  <a-tag v-if="getParentUniformLink(record)" color="green">整组关联</a-tag>
                </a-space>
              </template>
              <template v-else-if="column.key === 'is_second_party'">
                <a-space size="small" wrap>
                  <a-tag
                    v-if="record.is_second_party"
                    color="blue"
                    style="cursor: pointer;"
                    @click.stop="openRuleDetail(record)"
                  >二方包</a-tag>
                  <a-tag v-else color="default">三方包</a-tag>
                  <a-tag v-if="isDepLockedByParent(record)" color="purple">整组关联</a-tag>
                </a-space>
              </template>
              <template v-else-if="column.key === 'link'">
                <template v-if="record.__is_parent_group">
                  <template v-if="record.parent !== DEFAULT_PARENT_LABEL && record.parent_group_id && record.parent_artifact_id && record.child_count > 0 && record.second_party_count === record.child_count">
                    <template v-if="getParentUniformLink(record)">
                      <a-button
                        type="link"
                        size="small"
                        @click.stop="openLinkGraphView(record, getParentUniformLink(record))"
                      >
                        视图
                      </a-button>
                      <a-button type="link" size="small" @click.stop="openParentLinkModal(record)">调整</a-button>
                    </template>
                    <a-button
                      v-else
                      type="dashed"
                      size="small"
                      @click.stop="openParentLinkModal(record)"
                    >
                      <template #icon><LinkOutlined /></template>
                      关联整组
                    </a-button>
                  </template>
                  <span v-else class="unknown">-</span>
                </template>
                <template v-else-if="isDepLockedByParent(record)">
                  <span class="unknown">-</span>
                </template>
                <template v-else-if="getLinkForDep(record)">
                  <a-tooltip
                    :title="`${getLinkForDep(record)!.linked_project_name}${getLinkForDep(record)!.dep_version ? ` v${getLinkForDep(record)!.dep_version}` : ''}`"
                    placement="topLeft"
                  >
                    <a-tag color="green" style="cursor:pointer;max-width:150px;" class="mono cell-ellipsis-tag" @click.stop="openLinkModal(record)">
                      <span class="cell-ellipsis">{{ getLinkForDep(record)!.linked_project_name }}</span>
                      <span v-if="getLinkForDep(record)!.dep_version" style="opacity:.65;font-size:10px;margin-left:2px;">
                        v{{ getLinkForDep(record)!.dep_version }}
                      </span>
                    </a-tag>
                  </a-tooltip>
                  <a-button type="text" danger size="small" @click.stop="removeLink(record)">
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </template>
                <template v-else-if="record.is_second_party">
                  <a-button type="dashed" size="small" @click.stop="openLinkModal(record)">
                    <template #icon><LinkOutlined /></template>
                    关联
                  </a-button>
                </template>
                <span v-else class="unknown">-</span>
              </template>
              <template v-else-if="column.key === 'parent'">
                <template v-if="record.__is_parent_group">
                  <a-tooltip :title="record.parent || getParentLabel(record)" placement="topLeft">
                    <span class="parent-cell-wrap parent-cell-wrap--stack">
                      <span class="parent-cell-main">
                        <span class="mono cell-ellipsis">{{ record.parent || getParentLabel(record) }}</span>
                        <span v-if="getParentUniformLink(record)" class="parent-linked-hint mono cell-ellipsis">
                          已整组关联：{{ getParentUniformLink(record)!.linked_project_name }}
                        </span>
                      </span>
                    </span>
                  </a-tooltip>
                </template>
                <span v-else class="unknown">-</span>
              </template>
              <template v-else-if="column.key === 'scope'">
                <template v-if="record.__is_parent_group">
                  <span class="unknown">-</span>
                </template>
                <a-tag
                  v-else
                  :color="record.scope === 'test' ? 'orange' : record.scope === 'provided' ? 'purple' : 'default'"
                  style="font-size: 11px;"
                >
                  {{ record.scope || 'compile' }}
                </a-tag>
              </template>
              <template v-else>
                <a-tooltip v-if="record[column.key]" :title="String(record[column.key])" placement="topLeft">
                  <span class="mono cell-ellipsis">{{ record[column.key] }}</span>
                </a-tooltip>
                <span v-else class="unknown">-</span>
              </template>
            </template>
          </a-table>
        </a-collapse-panel>
      </a-collapse>
    </template>
  </div>

  <!-- 关联项目弹窗 -->
  <a-modal
    v-model:open="linkModalVisible"
    :title="linkModalMode === 'parent' ? '按 Parent 关联图谱' : '关联二方包项目'"
    :confirm-loading="linkModalSaving"
    ok-text="保存"
    cancel-text="取消"
    @ok="saveLinkModal"
  >
    <div v-if="linkModalDep" style="margin-bottom:12px;">
      <span class="mono" style="font-size:12px;color:rgba(0,0,0,.45);">
        {{ linkModalDep.group_id }}:{{ linkModalDep.artifact_id }}
        <span v-if="linkModalDep.version" style="margin-left:4px;">v{{ linkModalDep.version }}</span>
      </span>
    </div>
    <div v-else-if="linkModalParent" style="margin-bottom:12px;">
      <span class="mono" style="font-size:12px;color:rgba(0,0,0,.45);">
        parent: {{ linkModalParent.parent }}（{{ linkModalParent.child_count }} 个子依赖）
      </span>
    </div>
    <a-form layout="vertical" size="small">
      <a-form-item label="对应已导入项目" required>
        <a-select
          v-model:value="linkModalSelectedAppId"
          placeholder="选择已导入的项目"
          show-search
          option-filter-prop="searchText"
          :filter-option="(input: string, opt: any) => String(opt?.searchText ?? '').includes(input.toLowerCase())"
          :options="linkProjectOptions"
          style="width:100%"
        />
      </a-form-item>
      <a-form-item label="备注（可选）">
        <a-input v-model:value="linkModalNote" placeholder="如：forward-dal 对应 dal 项目" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 二方包匹配规则详情弹窗 -->
  <a-modal
    v-model:open="ruleDetailVisible"
    title="二方包匹配详情"
    :footer="null"
    width="480px"
  >
    <template v-if="ruleDetailDep">
      <a-descriptions bordered size="small" :column="1" style="margin-bottom: 16px;">
        <a-descriptions-item label="groupId">
          <span class="mono">{{ ruleDetailDep.group_id }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="artifactId">
          <span class="mono">{{ ruleDetailDep.artifact_id }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="version">
          <span class="mono">{{ ruleDetailDep.version || '-' }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <template v-if="ruleDetailDep.matched_rule">
        <div style="font-weight: 600; margin-bottom: 8px;">匹配的二方包规则</div>
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item label="识别依据">
            <span>{{ ruleDetailDep.matched_via === 'parent' ? '父级/BOM 归类' : '规则直匹配' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="规则名称">
            <span>{{ ruleDetailDep.matched_rule.name || '-' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="groupId 规则">
            <span class="mono">{{ unescapeRegex(ruleDetailDep.matched_rule.group_id_regex) }}</span>
            <a-tag v-if="ruleDetailDep.matched_rule.group_id_regex !== unescapeRegex(ruleDetailDep.matched_rule.group_id_regex)" size="small" style="margin-left:6px;font-size:10px;">正则</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="artifactId 规则">
            <span class="mono">{{ unescapeRegex(ruleDetailDep.matched_rule.artifact_id_regex) }}</span>
            <a-tag v-if="ruleDetailDep.matched_rule.artifact_id_regex !== unescapeRegex(ruleDetailDep.matched_rule.artifact_id_regex)" size="small" style="margin-left:6px;font-size:10px;">正则</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="优先级">
            <span class="mono">{{ ruleDetailDep.matched_rule.sort_order }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="ruleDetailDep.matched_via === 'parent' && ruleDetailDep.parent_dependency" label="归类父级/BOM">
            <span class="mono">
              {{ ruleDetailDep.parent_dependency.group_id }}:{{ ruleDetailDep.parent_dependency.artifact_id }}
              <span v-if="ruleDetailDep.parent_dependency.version" style="margin-left: 4px;">v{{ ruleDetailDep.parent_dependency.version }}</span>
            </span>
          </a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 10px; font-size: 12px; color: rgba(0,0,0,.45);">
          {{
            ruleDetailDep.matched_via === 'parent'
              ? '该依赖由同 group 的父级/BOM 依赖命中规则后归类为二方包。'
              : '该依赖的 groupId 和 artifactId 均满足上述规则，因此被识别为二方包。'
          }}
        </div>
      </template>

      <a-alert
        v-else
        type="warning"
        show-icon
        message="未找到匹配规则信息"
        description="该依赖在上次解析时被标记为二方包，但规则记录已丢失（可能规则已被删除）。重新点击「解析依赖」可刷新匹配结果。"
        style="margin-top: 8px;"
      />
    </template>
  </a-modal>
</template>

<style scoped>
.app-detail-page {
  padding: 4px 0;
  height: 100%;
  overflow-y: auto;
}

.title {
  font-weight: 800;
  font-size: 18px;
  margin: 10px 0 14px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.desc :deep(.ant-descriptions-item-content) {
  word-break: break-word;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  margin: 20px 0 10px;
  display: flex;
  align-items: center;
}

.scanned-at {
  margin-left: auto;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.35);
  font-weight: 400;
}

.deps-collapse {
  margin-top: 16px;
}

.deps-collapse :deep(.ant-collapse-header) {
  display: flex;
  align-items: center;
}

.collapse-header {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.deps-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.deps-count {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.unknown {
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
  font-style: italic;
}

.cell-ellipsis {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.parent-cell-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: calc(100% - 28px);
  max-width: 100%;
  min-width: 0;
  vertical-align: middle;
}

.parent-cell-wrap--stack {
  align-items: flex-start;
}

.parent-cell-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  flex: 1 1 auto;
  min-width: 0;
}

.parent-linked-hint {
  color: #389e0d;
  font-size: 11px;
}

.parent-cell-wrap .cell-ellipsis {
  flex: 1 1 auto;
  min-width: 0;
}

.cell-ellipsis-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  max-width: 150px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.cell-ellipsis-tag .cell-ellipsis {
  display: inline-block;
  max-width: 100%;
}

.deps-collapse :deep(.ant-table-cell) {
  overflow: hidden;
}
</style>

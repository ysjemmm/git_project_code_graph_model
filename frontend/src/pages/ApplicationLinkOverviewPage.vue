<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, ReloadOutlined, ExportOutlined, EyeOutlined } from '@ant-design/icons-vue'
import {
  listApplicationProjects,
  listDependencyLinks,
  getAppDependencies,
  type CacheProjectItem,
  type AppDependency,
  type AppDependencyLink,
} from '../api'

const route = useRoute()
const router = useRouter()
const repoName = computed(() => String(route.params.repoName || '').trim())
const DEFAULT_PARENT_LABEL = 'default'
const focusParent = computed(() => String(route.query.parent || '').trim())
const focusLinkedProject = computed(() => String(route.query.linkedProject || '').trim())
const focusLinkedAppId = computed(() => {
  const raw = String(route.query.linkedAppId || '').trim()
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
})

const loading = ref(false)
const item = ref<CacheProjectItem | null>(null)
const deps = ref<AppDependency[]>([])
const links = ref<AppDependencyLink[]>([])
const parentDetailVisible = ref(false)
const parentDetailRow = ref<ParentOverviewRow | null>(null)
const viewWidth = 1000

const scopedDeps = computed(() => {
  let list = deps.value
  if (focusParent.value) {
    list = list.filter((d) => parentLabel(d) === focusParent.value)
  }
  return list
})

const scopedDepKeySet = computed(() => new Set(scopedDeps.value.map((d) => depKey(d.group_id, d.artifact_id))))

const scopedLinks = computed(() => {
  let list = links.value.filter((l) => scopedDepKeySet.value.has(depKey(l.group_id, l.artifact_id)))
  if (focusLinkedAppId.value) {
    list = list.filter((l) => Number(l.linked_app_id) === focusLinkedAppId.value)
  }
  return list
})

const linkByKey = computed(() => {
  const m = new Map<string, AppDependencyLink>()
  for (const l of scopedLinks.value) {
    m.set(`${String(l.group_id || '')}::${String(l.artifact_id || '')}`, l)
  }
  return m
})

function depKey(groupId: string, artifactId: string): string {
  return `${String(groupId || '')}::${String(artifactId || '')}`
}

function parentLabel(dep: AppDependency): string {
  const pg = String(dep.parent_group_id || '').trim()
  const pa = String(dep.parent_artifact_id || '').trim()
  const pv = String(dep.parent_version || '').trim()
  if (pg && pa) return `${pg}:${pa}${pv ? `:${pv}` : ''}`
  return DEFAULT_PARENT_LABEL
}

async function fetchAll() {
  if (!repoName.value) return
  loading.value = true
  try {
    const p = await listApplicationProjects(false)
    const list: CacheProjectItem[] = Array.isArray(p.items) ? p.items : []
    const current = list.find((x) => String(x.repo_name) === repoName.value) ?? null
    item.value = current
    if (!current?.id) return

    const [depsRes, linksRes] = await Promise.all([
      getAppDependencies(current.id),
      listDependencyLinks(current.id),
    ])
    deps.value = Array.isArray(depsRes.items) ? depsRes.items : []
    links.value = Array.isArray(linksRes.items) ? linksRes.items : []
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    loading.value = false
  }
}

onMounted(() => { void fetchAll() })

const linkedTargets = computed(() => {
  const m = new Map<number, { linked_app_id: number; linked_project_name: string; linked_repo_name: string; count: number }>()
  for (const l of scopedLinks.value) {
    const id = Number(l.linked_app_id)
    const old = m.get(id)
    if (old) {
      old.count += 1
    } else {
      m.set(id, {
        linked_app_id: id,
        linked_project_name: String(l.linked_project_name || l.linked_repo_name || `#${id}`),
        linked_repo_name: String(l.linked_repo_name || ''),
        count: 1,
      })
    }
  }
  return Array.from(m.values()).sort((a, b) => b.count - a.count || a.linked_project_name.localeCompare(b.linked_project_name))
})

type ParentChildRow = {
  key: string
  group_id: string
  artifact_id: string
  version: string
  scope: string
  is_second_party: boolean
  linked_project_name: string
  linked_repo_name: string
  linked_app_id?: number
}

type ParentOverviewRow = {
  parent: string
  total: number
  second_party_count: number
  linked_count: number
  linked_project_name: string
  linked_repo_name: string
  linked_summary: string
  linked_summary_title: string
  status: string
  detail_rows: ParentChildRow[]
}

const parentOverview = computed(() => {
  const grouped = new Map<string, AppDependency[]>()
  for (const d of scopedDeps.value) {
    const p = parentLabel(d)
    const list = grouped.get(p) || []
    list.push(d)
    grouped.set(p, list)
  }
  const rows: ParentOverviewRow[] = Array.from(grouped.entries()).map(([parent, children]) => {
    const second = children.filter((x) => Boolean(x.is_second_party)).length
    const detailChildren: ParentChildRow[] = children.map((c) => {
      const l = linkByKey.value.get(depKey(c.group_id, c.artifact_id))
      return {
        key: `${c.group_id}:${c.artifact_id}:${c.version || ''}:${c.scope || ''}`,
        group_id: c.group_id,
        artifact_id: c.artifact_id,
        version: c.version,
        scope: c.scope,
        is_second_party: Boolean(c.is_second_party),
        linked_project_name: l?.linked_project_name || '-',
        linked_repo_name: l?.linked_repo_name || '',
        linked_app_id: l?.linked_app_id,
      }
    })
    const childLinks = detailChildren.filter((x) => x.linked_app_id) as ParentChildRow[]
    const linkedCount = childLinks.length
    const linkedIds = new Set(childLinks.map((x) => Number(x.linked_app_id)))
    const uniform = linkedCount === children.length && linkedIds.size === 1
    const linkedMap = new Map<string, number>()
    for (const c of childLinks) {
      const n = String(c.linked_project_name || c.linked_repo_name || '-')
      linkedMap.set(n, (linkedMap.get(n) || 0) + 1)
    }
    const linkedSummaryList = Array.from(linkedMap.entries())
      .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
      .map(([name, count]) => `${name}(${count})`)
    const linkedSummaryTitle = linkedSummaryList.join('，')
    const linkedSummary = linkedSummaryList.length === 0
      ? '-'
      : linkedSummaryList.length <= 2
        ? linkedSummaryList.join('，')
        : `${linkedSummaryList.slice(0, 2).join('，')} 等${linkedSummaryList.length}个项目`
    return {
      parent,
      total: children.length,
      second_party_count: second,
      linked_count: linkedCount,
      linked_project_name: uniform ? childLinks[0]?.linked_project_name || '-' : '-',
      linked_repo_name: uniform ? childLinks[0]?.linked_repo_name || '' : '',
      linked_summary: linkedSummary,
      linked_summary_title: linkedSummaryTitle,
      status: uniform ? '整组已关联' : linkedCount > 0 ? '部分关联' : '未关联',
      detail_rows: detailChildren.sort((a, b) => {
        const x = `${a.artifact_id}:${a.group_id}:${a.version}:${a.scope}`
        const y = `${b.artifact_id}:${b.group_id}:${b.version}:${b.scope}`
        return x.localeCompare(y)
      }),
    }
  })
  return rows.sort((a, b) => {
    const aHasSecond = a.second_party_count > 0
    const bHasSecond = b.second_party_count > 0
    if (aHasSecond !== bHasSecond) return aHasSecond ? -1 : 1
    if (a.parent === DEFAULT_PARENT_LABEL && b.parent !== DEFAULT_PARENT_LABEL) return 1
    if (a.parent !== DEFAULT_PARENT_LABEL && b.parent === DEFAULT_PARENT_LABEL) return -1
    return a.parent.localeCompare(b.parent)
  })
})

const boardHeight = computed(() => Math.max(280, 140 + linkedTargets.value.length * 96))
const sourceY = computed(() => Math.round(boardHeight.value / 2))
const sourceX = Math.round(viewWidth * 0.22)
const targetX = Math.round(viewWidth * 0.78)
const targetYs = computed(() => {
  const n = linkedTargets.value.length
  if (n <= 0) return []
  const gap = n === 1 ? 0 : (boardHeight.value - 120) / (n - 1)
  return linkedTargets.value.map((_, i) => Math.round(60 + i * gap))
})

const maxTargetCount = computed(() => Math.max(1, ...linkedTargets.value.map((x) => Number(x.count || 0))))

function edgeWidth(count: number): number {
  const max = Math.max(1, maxTargetCount.value)
  const ratio = Math.max(0, Number(count || 0)) / max
  return Math.max(1, Math.round(1 + ratio * 2))
}

function targetShare(count: number): string {
  const total = Math.max(0, Number(scopedLinks.value.length || 0))
  if (total <= 0) return '0%'
  return `${Math.round((Math.max(0, Number(count || 0)) / total) * 1000) / 10}%`
}

function edgePath(targetY: number): string {
  const x1 = sourceX + 44
  const y1 = sourceY.value
  const x2 = targetX - 44
  const y2 = targetY
  const c1x = x1 + (x2 - x1) * 0.36
  const c2x = x1 + (x2 - x1) * 0.64
  return `M ${x1} ${y1} C ${c1x} ${y1}, ${c2x} ${y2}, ${x2} ${y2}`
}

function nodeAnimStyle(i: number): Record<string, string> {
  return { animationDelay: `${80 + i * 70}ms` }
}

function back() {
  if (!repoName.value) {
    void router.push('/application-admin')
    return
  }
  void router.push(`/application-admin/${encodeURIComponent(repoName.value)}`)
}

function clearScope() {
  if (!repoName.value) return
  void router.replace({
    path: `/application-admin/${encodeURIComponent(repoName.value)}/link-overview`,
  })
}

const scopeHint = computed(() => {
  if (!focusParent.value && !focusLinkedProject.value && !focusLinkedAppId.value) return ''
  const bits: string[] = []
  if (focusParent.value) bits.push(`parent=${focusParent.value}`)
  if (focusLinkedProject.value) bits.push(`目标项目=${focusLinkedProject.value}`)
  else if (focusLinkedAppId.value) bits.push(`目标ID=${focusLinkedAppId.value}`)
  return bits.join('，')
})

function jumpToProject(repo: string) {
  const target = String(repo || '').trim()
  if (!target) return
  void router.push(`/application-admin/${encodeURIComponent(target)}`)
}

function openParentDetail(row: ParentOverviewRow) {
  parentDetailRow.value = row
  parentDetailVisible.value = true
}

const targetColumns = [
  { title: '目标项目', dataIndex: 'linked_project_name', key: 'linked_project_name', ellipsis: true },
  { title: 'repo', dataIndex: 'linked_repo_name', key: 'linked_repo_name', ellipsis: true },
  { title: '关联依赖数', dataIndex: 'count', key: 'count', width: 120 },
  { title: '操作', key: 'action', width: 96 },
] as const

const parentColumns = [
  { title: 'parent', dataIndex: 'parent', key: 'parent', ellipsis: true },
  { title: '总依赖数', dataIndex: 'total', key: 'total', width: 90 },
  { title: '二方包数', dataIndex: 'second_party_count', key: 'second_party_count', width: 90 },
  { title: '已关联数', dataIndex: 'linked_count', key: 'linked_count', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '关联概览', dataIndex: 'linked_summary', key: 'linked_summary', width: 260, ellipsis: true },
  { title: '操作', key: 'action', width: 72 },
] as const

const detailDepColumns = [
  { title: 'artifactId', dataIndex: 'artifact_id', key: 'artifact_id', ellipsis: true },
  { title: 'groupId', dataIndex: 'group_id', key: 'group_id', ellipsis: true },
  { title: 'version', dataIndex: 'version', key: 'version', width: 180, ellipsis: true },
  { title: 'scope', dataIndex: 'scope', key: 'scope', width: 90, ellipsis: true },
  { title: '类型', key: 'is_second_party', width: 90 },
  { title: '已关联项目', dataIndex: 'linked_project_name', key: 'linked_project_name', ellipsis: true },
  { title: '操作', key: 'action', width: 96 },
] as const
</script>

<template>
  <div class="link-overview-page">
    <a-space style="margin-bottom: 12px">
      <a-button type="text" @click="back">
        <template #icon><ArrowLeftOutlined /></template>
        返回应用详情
      </a-button>
      <a-button :loading="loading" @click="fetchAll">
        <template #icon><ReloadOutlined /></template>
        刷新
      </a-button>
    </a-space>

    <a-alert
      v-if="!item"
      type="info"
      show-icon
      message="未找到应用"
      description="该应用可能已删除，或尚未完成落库。"
    />

    <template v-else>
      <div class="title mono">{{ item.project_name || item.repo_name }} · 关联视图</div>
      <a-alert
        v-if="scopeHint"
        type="info"
        show-icon
        style="margin-bottom: 12px;"
        :message="`按需视图：${scopeHint}`"
      >
        <template #action>
          <a-button size="small" @click="clearScope">查看全部</a-button>
        </template>
      </a-alert>

      <a-row :gutter="12" style="margin-bottom: 12px;">
        <a-col :span="8"><a-statistic :title="scopeHint ? '当前依赖数' : '总依赖数'" :value="scopedDeps.length" /></a-col>
        <a-col :span="8"><a-statistic :title="scopeHint ? '当前已关联数' : '已关联依赖数'" :value="scopedLinks.length" /></a-col>
        <a-col :span="8"><a-statistic title="目标项目数" :value="linkedTargets.length" /></a-col>
      </a-row>

      <a-card :size="'small'" :title="scopeHint ? '关联拓扑（当前筛选）' : '关联拓扑（应用 -> 目标项目）'" style="margin-bottom: 12px;">
        <div class="graph-board" :style="{ height: `${boardHeight}px` }">
          <svg class="graph-svg" :viewBox="`0 0 ${viewWidth} ${boardHeight}`" preserveAspectRatio="none">
            <defs>
              <linearGradient id="edgeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stop-color="rgba(22,119,255,0.12)" />
                <stop offset="60%" stop-color="rgba(22,119,255,0.58)" />
                <stop offset="100%" stop-color="rgba(82,196,26,0.62)" />
              </linearGradient>
              <marker id="edgeArrow" markerWidth="6" markerHeight="6" refX="5.5" refY="3" orient="auto">
                <path d="M0,0 L6,3 L0,6 Z" fill="rgba(82,196,26,0.68)" />
              </marker>
            </defs>
            <g v-for="(t, i) in linkedTargets" :key="`edge-${t.linked_app_id}`">
              <path
                :id="`edge-path-${i}`"
                :d="edgePath(targetYs[i])"
                class="edge-line"
                :stroke-width="edgeWidth(t.count)"
                marker-end="url(#edgeArrow)"
              />
              <path
                :d="edgePath(targetYs[i])"
                class="edge-flow"
                :stroke-width="edgeWidth(t.count)"
                :style="nodeAnimStyle(i)"
              />
              <text
                :x="(sourceX + targetX) / 2 - 14"
                :y="(sourceY + targetYs[i]) / 2 - 4"
                fill="rgba(4,26,53,.72)"
                font-size="12"
                class="edge-label"
              >{{ t.count }} ({{ targetShare(t.count) }})</text>
            </g>
          </svg>

          <div class="node source-node" :style="{ top: `${sourceY}px`, left: `${(sourceX / viewWidth) * 100}%` }" :title="item.project_name || item.repo_name">
            <div class="node-title">{{ item.project_name || item.repo_name }}</div>
            <div class="node-sub">当前应用 · 依赖 {{ scopedDeps.length }} · 已关联 {{ scopedLinks.length }}</div>
          </div>

          <div
            v-for="(t, i) in linkedTargets"
            :key="`node-${t.linked_app_id}`"
            class="node target-node"
            :style="{ top: `${targetYs[i]}px`, left: `${(targetX / viewWidth) * 100}%`, ...nodeAnimStyle(i) }"
            :title="`${t.linked_project_name}（${t.count}）`"
          >
            <div class="node-title">{{ t.linked_project_name }}</div>
            <div class="node-sub">关联 {{ t.count }} 个依赖（{{ targetShare(t.count) }}）</div>
            <div class="node-repo" :title="t.linked_repo_name || '-'">{{ t.linked_repo_name || '-' }}</div>
          </div>

          <a-empty v-if="linkedTargets.length === 0" description="暂无关联数据" style="padding-top: 40px;" />
        </div>
      </a-card>

      <a-card size="small" title="目标项目分布" style="margin-bottom: 12px;">
        <a-table
          :columns="targetColumns as any"
          :data-source="linkedTargets as any"
          :pagination="false"
          size="small"
          bordered
          row-key="linked_app_id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="jumpToProject(record.linked_repo_name)">
                <template #icon><ExportOutlined /></template>
                项目详情
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-card size="small" title="Parent 关联状态">
        <a-table
          :columns="parentColumns as any"
          :data-source="parentOverview as any"
          :pagination="{ pageSize: 20, showSizeChanger: true, pageSizeOptions: ['20','50','100'] }"
          size="small"
          bordered
          row-key="parent"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag v-if="record.status === '整组已关联'" color="green">整组已关联</a-tag>
              <a-tag v-else-if="record.status === '部分关联'" color="orange">部分关联</a-tag>
              <a-tag v-else color="default">未关联</a-tag>
            </template>
            <template v-else-if="column.key === 'linked_summary'">
              <a-tooltip v-if="record.linked_summary_title" :title="record.linked_summary_title" placement="topLeft">
                <span class="cell-ellipsis">{{ record.linked_summary || '-' }}</span>
              </a-tooltip>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openParentDetail(record)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </template>
  </div>

  <a-drawer
    v-model:open="parentDetailVisible"
    width="980"
    :title="parentDetailRow ? `Parent 详情：${parentDetailRow.parent}` : 'Parent 详情'"
    placement="right"
    destroy-on-close
  >
    <template v-if="parentDetailRow">
      <a-alert
        type="info"
        show-icon
        style="margin-bottom: 12px;"
        :message="`该 parent 下共 ${parentDetailRow.total} 个依赖，已关联 ${parentDetailRow.linked_count} 个`"
      />
      <a-table
        :columns="detailDepColumns as any"
        :data-source="parentDetailRow.detail_rows as any"
        :pagination="{ pageSize: 20, showSizeChanger: true, pageSizeOptions: ['20','50','100'] }"
        size="small"
        bordered
        row-key="key"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'is_second_party'">
            <a-tag v-if="record.is_second_party" color="blue">二方包</a-tag>
            <a-tag v-else color="default">三方包</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              :disabled="!record.linked_repo_name"
              @click="jumpToProject(record.linked_repo_name)"
            >
              <template #icon><ExportOutlined /></template>
              项目详情
            </a-button>
          </template>
          <template v-else-if="['artifact_id','group_id','version','linked_project_name'].includes(String(column.key))">
            <a-tooltip v-if="record[column.key]" :title="String(record[column.key])" placement="topLeft">
              <span class="cell-ellipsis">{{ record[column.key] }}</span>
            </a-tooltip>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </template>
  </a-drawer>
</template>

<style scoped>
.link-overview-page {
  padding: 4px 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

.title {
  font-weight: 800;
  font-size: 18px;
  margin: 10px 0 14px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.graph-board {
  position: relative;
  width: 100%;
  min-height: 240px;
  background:
    radial-gradient(circle at 20% 20%, rgba(22, 119, 255, 0.15) 0%, rgba(22, 119, 255, 0) 42%),
    radial-gradient(circle at 86% 76%, rgba(82, 196, 26, 0.14) 0%, rgba(82, 196, 26, 0) 48%),
    linear-gradient(180deg, #f8fbff 0%, #eef6ff 100%);
  border: 1px solid rgba(22, 119, 255, 0.14);
  border-radius: 10px;
  overflow: hidden;
}

.graph-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.edge-line {
  fill: none;
  stroke: url(#edgeGradient);
  opacity: 0.82;
}

.edge-flow {
  fill: none;
  stroke: rgba(255, 255, 255, 0.9);
  stroke-width: 1.2;
  stroke-dasharray: 8 16;
  stroke-linecap: round;
  opacity: 0.62;
  animation: edgeFlow 2.4s linear infinite;
}

.edge-label {
  font-weight: 700;
  paint-order: stroke;
  stroke: rgba(255, 255, 255, 0.88);
  stroke-width: 3px;
  stroke-linejoin: round;
}

.node {
  position: absolute;
  transform: translate(-50%, -50%);
  width: min(280px, 34vw);
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  box-shadow: 0 10px 22px rgba(5, 31, 73, 0.14);
  backdrop-filter: blur(2px);
}

.source-node {
  border-color: rgba(22, 119, 255, 0.35);
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.22), 0 12px 24px rgba(22, 119, 255, 0.18);
}

.target-node {
  border-color: rgba(82, 196, 26, 0.35);
  animation: nodePopIn 0.4s ease-out both, nodeFloat 3.6s ease-in-out infinite;
}

.node-title {
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.node-sub {
  margin-top: 2px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.5);
}

.node-repo {
  margin-top: 2px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.35);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

@keyframes edgeFlow {
  from { stroke-dashoffset: 0; }
  to { stroke-dashoffset: -48; }
}

@keyframes nodePopIn {
  from {
    opacity: 0;
    filter: blur(2px);
  }
  to {
    opacity: 1;
    filter: blur(0);
  }
}

@keyframes nodeFloat {
  0%, 100% { transform: translate(-50%, -50%); }
  50% { transform: translate(-50%, calc(-50% - 3px)); }
}
</style>
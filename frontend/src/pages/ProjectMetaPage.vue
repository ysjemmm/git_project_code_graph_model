<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { FolderOpenOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

type CacheProjectItem = {
  repo_name: string
  project_name?: string | null
  project_key?: string | null
  project_type?: string | null
  repo_url?: string | null
  branch?: string | null
  commit_hash?: string | null
  last_update_time?: string | null
  cache_dir?: string | null
  repo_exists: boolean
  remote_url?: string | null
  head_branch?: string | null
  head_commit?: string | null
  dirty?: boolean | null
  cache_size_mb?: number | null
  merkle_branches: string[]
}

type MerkleResp = {
  summary: {
    repo_name: string
    branch: string
    node_count: number
    file_count: number
    dir_count: number
    max_depth: number
  }
  tree: any
}

const loading = ref(false)
const error = ref<string | null>(null)
const items = ref<CacheProjectItem[]>([])

const selectedRepo = ref<string | null>(null)
const selectedBranch = ref<string | null>(null)

const merkleLoading = ref(false)
const merkleError = ref<string | null>(null)
const merkle = ref<MerkleResp | null>(null)
const merkleDepth = ref(2)
const deleting = ref(false)
const deleteResult = ref<string | null>(null)
/** 删除前检查：存在进行中的导入任务或 AI 会话时不为空 */
const deleteBlockReason = ref<string | null>(null)
const deleteCheckLoading = ref(false)

let lastShownDeleteBlockReason: string | null = null
let lastShownDeleteResult: string | null = null
// error/merkleError 的 message 由 main.ts 全局响应拦截器统一处理

watch(deleteBlockReason, (v) => {
  if (!v) {
    lastShownDeleteBlockReason = null
    return
  }
  if (lastShownDeleteBlockReason === v) return
  lastShownDeleteBlockReason = v
  message.warning({ content: `当前不可删除：${v}`, duration: 4 })
})

watch(deleteResult, (v) => {
  if (!v) {
    lastShownDeleteResult = null
    return
  }
  if (lastShownDeleteResult === v) return
  lastShownDeleteResult = v
  if (String(v).includes('删除失败')) message.error({ content: v, duration: 4 })
  else message.success({ content: v, duration: 3 })
})

async function fetchProjects() {
  loading.value = true
  error.value = null
  try {
    const r = await fetch('/api/cache/application-projects')
    const data = await r.json()
    if (!r.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    items.value = Array.isArray(data?.items) ? data.items : []
    if (!selectedRepo.value && items.value.length) {
      selectedRepo.value = items.value[0].repo_name
    }
  } catch (e: any) {
    error.value = e?.message ?? String(e)
  } finally {
    loading.value = false
  }
}

const selectedItem = computed(() => {
  if (!selectedRepo.value) return null
  return items.value.find((x) => x.repo_name === selectedRepo.value) ?? null
})

/** 选中项目变化时检查是否可删除（进行中导入任务 / AI 会话会阻止删除） */
async function checkCanDelete() {
  const it = selectedItem.value
  deleteBlockReason.value = null
  if (!it) return
  deleteCheckLoading.value = true
  try {
    const url = new URL('/api/cache/application/can-delete', window.location.origin)
    url.searchParams.set('repo_name', it.repo_name)
    url.searchParams.set('project_name', it.project_name ?? it.repo_name)
    const r = await fetch(url.toString())
    const data = await r.json().catch(() => ({}))
    if (data?.ok !== true && data?.block_reason) {
      deleteBlockReason.value = data.block_reason
    }
  } catch {
    deleteBlockReason.value = null
  } finally {
    deleteCheckLoading.value = false
  }
}

watch(selectedItem, () => {
  void checkCanDelete()
}, { immediate: true })

const branchOptions = computed(() => {
  const it = selectedItem.value
  const arr = it?.merkle_branches ?? []
  return arr.map((b) => ({ label: b, value: b }))
})

async function fetchMerkle() {
  const it = selectedItem.value
  if (!it) return
  const branch = selectedBranch.value || it.branch || it.merkle_branches?.[0]
  if (!branch) {
    merkle.value = null
    return
  }
  selectedBranch.value = branch

  merkleLoading.value = true
  merkleError.value = null
  try {
    const url = new URL('/api/cache/merkle', window.location.origin)
    url.searchParams.set('repo_name', it.repo_name)
    url.searchParams.set('branch', branch)
    url.searchParams.set('depth', String(merkleDepth.value))
    const r = await fetch(url.toString())
    const data = await r.json()
    if (!r.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    merkle.value = data
  } catch (e: any) {
    merkleError.value = e?.message ?? String(e)
    merkle.value = null
  } finally {
    merkleLoading.value = false
  }
}

onMounted(() => {
  void fetchProjects()
})

const tableData = computed(() =>
  items.value.map((x) => ({
    key: x.repo_name,
    ...x,
    commit_short: (x.commit_hash ?? '').slice(0, 8),
    head_short: (x.head_commit ?? '').slice(0, 8),
  })),
)

const columns = [
  { title: '项目', dataIndex: 'repo_name', key: 'repo_name', width: 180 },
  { title: '类型', dataIndex: 'project_type', key: 'project_type', width: 110 },
  { title: 'Git 仓库', dataIndex: 'repo_url', key: 'repo_url', ellipsis: true },
  { title: '分支', dataIndex: 'branch', key: 'branch', width: 160, ellipsis: true },
  { title: 'Commit', dataIndex: 'commit_short', key: 'commit_short', width: 110 },
  { title: '缓存(MB)', dataIndex: 'cache_size_mb', key: 'cache_size_mb', width: 120 },
] as const

function onRowClick(record: any) {
  selectedRepo.value = record.repo_name
  selectedBranch.value = null
  merkle.value = null
  merkleError.value = null
}

function fmtMb(v: any) {
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return n.toFixed(2)
}

async function deleteApplication() {
  const it = selectedItem.value
  if (!it) return
  if (deleteBlockReason.value) return
  deleting.value = true
  deleteResult.value = null
  try {
    const r = await fetch('/api/cache/application/delete', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        project_name: it.project_name ?? it.repo_name,
        project_key: it.project_key ?? null,
        repo_name: it.repo_name,
      }),
    })
    const data = await r.json().catch(() => ({}))
    if (!r.ok || !data?.ok) throw new Error(data?.message ?? `HTTP ${r.status}`)
    deleteResult.value = data?.message ?? '删除完成'
    deleteBlockReason.value = null
    // 重新加载列表
    selectedRepo.value = null
    selectedBranch.value = null
    merkle.value = null
    await fetchProjects()
  } catch (e: any) {
    deleteResult.value = `删除失败：${e?.message ?? String(e)}`
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="meta-page">
    <a-card title="项目元数据（Application）" class="panel">
      <a-space style="margin-bottom: 12px">
        <a-button :loading="loading" @click="fetchProjects">
          <template #icon><ReloadOutlined /></template>
          刷新列表
        </a-button>
        <!-- 当前不可删除：由 watch(deleteBlockReason) 使用 message 提示 -->
        <a-popconfirm
          v-if="selectedItem"
          ok-text="确认删除"
          cancel-text="取消"
          :ok-button-props="{ danger: true, loading: deleting, disabled: !!deleteBlockReason }"
          @confirm="deleteApplication"
        >
          <template #title>
            <div style="display: flex; flex-direction: column; gap: 8px; max-width: 520px">
              <div>
                确认删除 Application：
                <span class="mono" style="font-weight: 700">{{ selectedItem.project_name || selectedItem.repo_name }}</span>
              </div>
              <div class="danger">
                该操作不可恢复，将同时删除图谱与本地缓存相关内容。
              </div>
              <div class="kv">
                <div><span class="k">project_key</span><span class="v mono">{{ selectedItem.project_key || '-' }}</span></div>
                <div><span class="k">repo_name</span><span class="v mono">{{ selectedItem.repo_name }}</span></div>
              </div>
              <div class="list">
                <div>将删除：</div>
                <ul>
                  <li><span class="danger">Neo4j 代码图谱</span>：该 Application 的全部子图节点/关系</li>
                  <li><span class="danger">本地 Git 缓存</span>：<span class="mono">.cache/git_repos/{{ selectedItem.repo_name }}</span></li>
                  <li><span class="danger">Merkle 树文件</span>：<span class="mono">.cache/merkle_trees/{{ selectedItem.repo_name }}_*.json</span></li>
                  <li><span class="danger">元数据</span>：<span class="mono">.cache/metadata/{{ selectedItem.repo_name }}.json</span></li>
                </ul>
              </div>
            </div>
          </template>
          <a-button danger :loading="deleting || deleteCheckLoading" :disabled="!!deleteBlockReason">删除应用</a-button>
        </a-popconfirm>
      </a-space>
      <!-- 删除结果由 watch(deleteResult) 使用 message 提示 -->

      <!-- 加载失败由 watch(error) 使用 message 提示 -->

      <div class="two-col">
        <div class="left">
          <a-table
            :columns="columns as any"
            :data-source="tableData as any"
            :pagination="{ pageSize: 8 }"
            size="middle"
            bordered
            :row-class-name="(r: any) => (r.repo_name === selectedRepo ? 'row-selected' : '')"
            :custom-row="(record: any) => ({ onClick: () => onRowClick(record) })"
            :scroll="{ x: 760 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'project_type'">
                <a-tag :color="String(record.project_type || '').toLowerCase() === 'application' ? 'blue' : 'gold'">
                  {{ record.project_type || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'repo_url'">
                <a-tooltip :title="record.repo_url || ''" placement="topLeft">
                  <span class="mono cell-ellipsis">{{ record.repo_url || '-' }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'branch'">
                <a-tooltip :title="record.branch || ''" placement="topLeft">
                  <span class="mono cell-ellipsis">{{ record.branch || '-' }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'commit_short'">
                <a-tooltip :title="record.commit_hash || ''" placement="topLeft">
                  <span class="mono">{{ record.commit_short || '-' }}</span>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'cache_size_mb'">
                <span class="mono">{{ fmtMb(record.cache_size_mb) }}</span>
              </template>
            </template>
          </a-table>
        </div>

        <div class="right">
          <a-empty v-if="!selectedItem" description="请选择一个 Application 项目" />
          <template v-else>
            <a-tabs>
              <a-tab-pane key="git" tab="本地 Git 仓库">
                <a-descriptions bordered size="small" :column="1">
                  <a-descriptions-item label="缓存目录">
                    <span class="mono">{{ selectedItem.cache_dir }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="仓库存在">{{ selectedItem.repo_exists ? '是' : '否' }}</a-descriptions-item>
                  <a-descriptions-item label="remote.origin.url">
                    <span class="mono">{{ selectedItem.remote_url || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="HEAD 分支">
                    <span class="mono">{{ selectedItem.head_branch || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="HEAD commit">
                    <span class="mono">{{ selectedItem.head_commit || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="工作区是否脏">{{ selectedItem.dirty === null ? '-' : (selectedItem.dirty ? '是' : '否') }}</a-descriptions-item>
                </a-descriptions>
              </a-tab-pane>

              <a-tab-pane key="meta" tab="元数据（metadata）">
                <a-descriptions bordered size="small" :column="1">
                  <a-descriptions-item label="repo_url">
                    <span class="mono">{{ selectedItem.repo_url || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="branch">
                    <span class="mono">{{ selectedItem.branch || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="commit_hash">
                    <span class="mono">{{ selectedItem.commit_hash || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="last_update_time">
                    <span class="mono">{{ selectedItem.last_update_time || '-' }}</span>
                  </a-descriptions-item>
                  <a-descriptions-item label="cache_size_mb">
                    <span class="mono">{{ fmtMb(selectedItem.cache_size_mb) }}</span>
                  </a-descriptions-item>
                </a-descriptions>
              </a-tab-pane>

              <a-tab-pane key="merkle" tab="Merkle 树">
                <div class="merkle-tab-content">
                <a-space style="margin-bottom: 12px; flex-shrink: 0">
                  <a-select
                    v-model:value="selectedBranch"
                    style="min-width: 220px"
                    placeholder="选择 Merkle 分支"
                    :options="branchOptions"
                    allow-clear
                  />
                  <a-input-number v-model:value="merkleDepth" :min="0" :max="6" placeholder="展开深度" style="width: 100px">
                    <template #addonBefore><span style="font-size:12px;color:rgba(0,0,0,.45)">深度</span></template>
                  </a-input-number>
                  <a-button :loading="merkleLoading" @click="fetchMerkle">
                    <template #icon><FolderOpenOutlined /></template>
                    加载 Merkle
                  </a-button>
                </a-space>

                <!-- Merkle 加载失败由 watch(merkleError) 使用 message 提示 -->
                <a-empty v-if="!merkle" description="请选择分支并加载 Merkle 树" />
                <template v-else>
                  <pre class="json">{{ JSON.stringify(merkle.tree, null, 2) }}</pre>
                </template>
                </div>
              </a-tab-pane>
            </a-tabs>
          </template>
        </div>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.meta-page { height: 100%; }
.two-col {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}
.left, .right { min-width: 0; }
.left { overflow: auto; }
.right { overflow: visible; }
.merkle-tab-content { display: flex; flex-direction: column; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 12px; }
.danger { color: #cf1322; font-weight: 700; }
.kv { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: 6px 10px; }
.kv > div { display: contents; }
.kv .k { color: rgba(0,0,0,.55); }
.kv .v { word-break: break-all; }
.list ul { margin: 6px 0 0; padding-left: 18px; }
.list li { margin: 4px 0; line-height: 1.4; }
.json {
  min-height: 200px;
  height: calc(100vh - 320px);
  overflow: auto;
  padding: 12px 14px 32px;
  margin-bottom: 24px;
  border-radius: 8px;
  background: rgba(0,0,0,.03);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre;
}
:deep(.row-selected td) { background: rgba(22,119,255,.08) !important; }
.cell-ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
</style>


<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import {
  listApplicationProjects,
  getImportSettings,
  updateImportSettings,
  deleteApplicationProject,
  createImportTask,
  getImportTask,
  type CacheProjectItem,
} from '../api'

const loading = ref(false)
const items = ref<CacheProjectItem[]>([])

const router = useRouter()

const createModalOpen = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  project_name: '',
  repo_url: '',
  maven_scan_enabled: true,
  force_maven: false,
  clear_database: false,
  auto_link_external: false,
})

async function fetchProjects({ refresh = false }: { refresh?: boolean } = {}) {
  loading.value = true
  try {
    const data = await listApplicationProjects(refresh)
    items.value = Array.isArray(data?.items) ? data.items : []
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchProjects({ refresh: false })
})

const tableData = computed(() =>
  items.value.map((x) => ({
    key: x.repo_name,
    ...x,
    display_project_name: x.project_name || x.repo_name,
    display_cache_dir: x.cache_dir || '-',
  })),
)

const columns = [
  { title: '应用', dataIndex: 'display_project_name', key: 'display_project_name', width: 220, ellipsis: true },
  { title: '被关联', dataIndex: 'linked_by_count', key: 'linked_by_count', width: 90 },
  { title: 'Git 地址', dataIndex: 'repo_url', key: 'repo_url', width: 260, ellipsis: true },
  { title: 'Git 缓存地址', dataIndex: 'display_cache_dir', key: 'display_cache_dir', width: 280, ellipsis: true },
  { title: '最后更新', dataIndex: 'last_update_time', key: 'last_update_time', width: 190, ellipsis: true },
  { title: '操作', key: 'actions', width: 120, fixed: 'right' },
] as const

// ---- 编辑导入配置 ----
const editModalOpen = ref(false)
const editLoading = ref(false)
const editAppId = ref<number | null>(null)
const editRepoName = ref('')
const editForm = reactive({
  maven_scan_enabled: true,
  force_maven: false,
  clear_database: false,
  auto_link_external: false,
})

async function openEditModal(record: any) {
  editAppId.value = record.id ?? null
  editRepoName.value = record.repo_name
  editLoading.value = true
  editModalOpen.value = true
  try {
    const id = record.id
    if (!id) throw new Error('应用缺少 id，无法加载配置')
    const data = await getImportSettings(id)
    if (data?.ok && data.settings) {
      editForm.maven_scan_enabled = Boolean(data.settings.maven_scan_enabled ?? true)
      editForm.force_maven = Boolean(data.settings.force_maven ?? false)
      editForm.clear_database = Boolean(data.settings.clear_database ?? false)
      editForm.auto_link_external = Boolean(data.settings.auto_link_external ?? true)
    } else {
      editForm.maven_scan_enabled = true
      editForm.force_maven = false
      editForm.clear_database = false
      editForm.auto_link_external = true
    }
  } catch {
    editForm.maven_scan_enabled = true
    editForm.force_maven = false
    editForm.clear_database = false
    editForm.auto_link_external = true
  } finally {
    editLoading.value = false
  }
}

async function submitEdit() {
  if (!editAppId.value) {
    message.error({ content: '应用 id 缺失，无法保存', duration: 4 })
    return
  }
  editLoading.value = true
  try {
    await updateImportSettings(editAppId.value, {
      maven_scan_enabled: editForm.maven_scan_enabled,
      force_maven: editForm.force_maven,
      clear_database: editForm.clear_database,
      auto_link_external: editForm.auto_link_external,
    })
    message.success({ content: '保存成功', duration: 3 })
    editModalOpen.value = false
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    editLoading.value = false
  }
}

// ---- 删除应用 ----
const deleteModalOpen = ref(false)
const deleteLoading = ref(false)
const deleteTarget = ref<{ id: number; name: string } | null>(null)
const deleteCountdown = ref(0)
let deleteCountdownTimer: number | undefined

function openDeleteModal(record: any) {
  deleteTarget.value = { id: record.id, name: record.display_project_name || record.repo_name }
  deleteCountdown.value = 3
  deleteModalOpen.value = true
  if (deleteCountdownTimer) window.clearInterval(deleteCountdownTimer)
  deleteCountdownTimer = window.setInterval(() => {
    deleteCountdown.value -= 1
    if (deleteCountdown.value <= 0) {
      window.clearInterval(deleteCountdownTimer)
      deleteCountdownTimer = undefined
    }
  }, 1000)
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  deleteLoading.value = true
  try {
    const data = await deleteApplicationProject(deleteTarget.value.id)
    const errs: string[] = Array.isArray(data.errors) ? data.errors : []
    if (errs.length) {
      message.warning({ content: `删除完成，但有部分错误：${errs.join('；')}`, duration: 6 })
    } else {
      message.success({ content: '删除成功', duration: 3 })
    }
    deleteModalOpen.value = false
    await fetchProjects({ refresh: false })
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 6 })
  } finally {
    deleteLoading.value = false
  }
}

// 关闭删除弹窗时清理倒计时
function closeDeleteModal() {
  deleteModalOpen.value = false
  if (deleteCountdownTimer) {
    window.clearInterval(deleteCountdownTimer)
    deleteCountdownTimer = undefined
  }
  deleteCountdown.value = 0
}

function goDetail(repoName: string) {
  if (!repoName) return
  void router.push(`/application-admin/${encodeURIComponent(repoName)}`)
}

function openCreateModal() {
  createForm.project_name = ''
  createForm.repo_url = ''
  createForm.maven_scan_enabled = true
  createForm.force_maven = false
  createForm.clear_database = false
  createForm.auto_link_external = false
  createModalOpen.value = true
}

async function waitTaskAndRefresh(taskId: string, timeoutMs = 60000, intervalMs = 2000) {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    try {
      const data = await getImportTask(taskId)
      if (!data?.ok) break
      const status = String(data?.item?.status || '').toLowerCase()
      if (status === 'success') {
        await fetchProjects({ refresh: true })
        message.success({ content: '导入完成，已刷新应用列表', duration: 3 })
        return
      }
      if (status === 'failed') {
        message.error({
          content: `导入失败：${data?.item?.error || data?.item?.message || '未知错误'}`,
          duration: 6,
        })
        return
      }
    } catch {
      // 忽略一次轮询失败，继续等下一轮
    }
    await new Promise((res) => setTimeout(res, intervalMs))
  }
  message.info({ content: '导入可能仍在进行，请手动刷新列表查看结果', duration: 4 })
}

async function submitCreate() {
  const project_name = String(createForm.project_name || '').trim()
  const repo_url = String(createForm.repo_url || '').trim()
  if (!project_name) {
    message.error({ content: '应用名称不能为空', duration: 4 })
    return
  }
  if (!repo_url) {
    message.error({ content: 'Git 仓库地址不能为空', duration: 4 })
    return
  }

  createLoading.value = true
  try {
    const data = await createImportTask({
      project_name,
      repo_url,
      branch: 'main',
      maven_scan_enabled: Boolean(createForm.maven_scan_enabled),
      force_maven: Boolean(createForm.force_maven),
      clear_database: Boolean(createForm.clear_database),
      auto_link_external: Boolean(createForm.auto_link_external),
    })
    if (!data?.ok) throw new Error(data?.task_id ? '' : '提交失败')
    const taskId = String(data?.task_id || '')
    message.success({ content: taskId ? `提交成功，task_id=${taskId}` : '提交成功', duration: 4 })
    await fetchProjects({ refresh: true })
    if (taskId) void waitTaskAndRefresh(taskId)
    createModalOpen.value = false
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 6 })
  } finally {
    createLoading.value = false
  }
}
</script>

<template>
  <div class="app-admin-page">
    <a-space style="margin-bottom: 12px">
      <a-button :loading="loading" @click="() => fetchProjects({ refresh: true })">
        <template #icon><ReloadOutlined /></template>
        刷新列表
      </a-button>

      <a-button type="primary" :loading="createLoading" @click="openCreateModal">
        新增应用
      </a-button>
    </a-space>

    <a-table
      :columns="columns as any"
      :data-source="tableData as any"
      :pagination="{ pageSize: 10 }"
      size="middle"
      bordered
      :scroll="{ x: 1000 }"
      :custom-row="(record: any) => ({ onClick: () => goDetail(record.repo_name) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'display_project_name'">
          <a class="app-name-link" @click.stop="goDetail(record.repo_name)">{{ record.display_project_name || '-' }}</a>
        </template>
        <template v-else-if="column.key === 'repo_url' || column.key === 'display_cache_dir' || column.key === 'last_update_time'">
          <a-tooltip :title="record[column.key] || ''" placement="topLeft">
            <span class="mono cell-ellipsis">{{ record[column.key] || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'linked_by_count'">
          <a-tooltip v-if="Number(record.linked_by_count || 0) > 0" placement="topLeft">
            <template #title>
              <div style="max-width: 360px;">
                <div style="margin-bottom: 6px; font-weight: 600;">关联来源（项目名/次数）</div>
                <div
                  v-for="(x, idx) in (Array.isArray(record.linked_by_preview) ? record.linked_by_preview : [])"
                  :key="`lb-${record.key}-${idx}`"
                  class="mono"
                  style="line-height: 1.6;"
                >
                  {{ x }}
                </div>
              </div>
            </template>
            <a-tag color="blue">
              {{ Number(record.linked_by_count || 0) }}
            </a-tag>
          </a-tooltip>
          <a-tag v-else color="default">0</a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space size="small">
            <a-button type="link" size="small" style="padding: 0;" @click.stop="openEditModal(record)">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
            <a-button danger type="link" size="small" style="padding: 0;" @click.stop="openDeleteModal(record)">
              <template #icon><DeleteOutlined /></template>
              删除
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>

  <a-modal
    v-model:open="createModalOpen"
    title="新增应用（提交导入任务）"
    :footer="null"
    width="720px"
    @cancel="createModalOpen = false"
  >
    <a-form layout="vertical">
      <a-form-item label="应用名称" required>
        <a-input v-model:value="createForm.project_name" placeholder="例如 epaas-gateway" />
      </a-form-item>

      <a-form-item label="Git 仓库地址" required>
        <a-input v-model:value="createForm.repo_url" placeholder="例如 https://github.com/xxx/yyy.git" />
      </a-form-item>

      <div class="hint">
        本次新增只需填应用名称和 Git 地址。（必要时可后续在图谱管理页选择分支/Commit）。
      </div>

      <a-form-item label="解析 pom 外部依赖（Maven 扫描）">
        <a-switch v-model:checked="createForm.maven_scan_enabled" checked-children="启用" un-checked-children="关闭" />
      </a-form-item>

      <a-form-item label="强制重新解析 Maven（可能较慢）">
        <a-switch v-model:checked="createForm.force_maven" checked-children="强制" un-checked-children="关闭" />
      </a-form-item>

      <a-form-item label="导入前清理旧图谱（clear_database，重建）">
        <a-switch v-model:checked="createForm.clear_database" checked-children="清理" un-checked-children="不清理" />
      </a-form-item>

      <a-form-item label="自动关联外部类（ExternalClassLinker）">
        <a-switch v-model:checked="createForm.auto_link_external" checked-children="启用" un-checked-children="关闭" />
      </a-form-item>

      <a-space style="margin-top: 12px">
        <a-button :disabled="createLoading" @click="createModalOpen = false">取消</a-button>
        <a-button type="primary" :loading="createLoading" @click="submitCreate">提交</a-button>
      </a-space>
    </a-form>
  </a-modal>

  <!-- 编辑导入配置 -->
  <a-modal
    v-model:open="editModalOpen"
    :title="`编辑导入配置：${editRepoName}`"
    width="480px"
    :confirm-loading="editLoading"
    ok-text="保存"
    cancel-text="取消"
    @ok="submitEdit"
    @cancel="editModalOpen = false"
  >
    <a-spin :spinning="editLoading">
      <a-form layout="vertical" style="margin-top: 8px;">
        <a-form-item label="解析 pom 外部依赖（Maven 扫描）">
          <a-switch v-model:checked="editForm.maven_scan_enabled" checked-children="启用" un-checked-children="关闭" />
        </a-form-item>
        <a-form-item label="强制重新解析 Maven（可能较慢）">
          <a-switch v-model:checked="editForm.force_maven" checked-children="强制" un-checked-children="关闭" />
        </a-form-item>
        <a-form-item label="导入前清理旧图谱（clear_database，重建）">
          <a-switch v-model:checked="editForm.clear_database" checked-children="清理" un-checked-children="不清理" />
        </a-form-item>
        <a-form-item label="自动关联外部类（ExternalClassLinker）">
          <a-switch v-model:checked="editForm.auto_link_external" checked-children="启用" un-checked-children="关闭" />
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
  <!-- 删除确认 -->
  <a-modal
    v-model:open="deleteModalOpen"
    title="确认删除应用"
    :ok-text="deleteCountdown > 0 ? `确认删除 (${deleteCountdown})` : '确认删除'"
    cancel-text="取消"
    ok-type="danger"
    :ok-button-props="{ disabled: deleteCountdown > 0 }"
    :confirm-loading="deleteLoading"
    @ok="confirmDelete"
    @cancel="closeDeleteModal"
  >
    <a-alert type="warning" show-icon style="margin-bottom: 12px;">
      <template #message>此操作不可恢复，将同时删除以下内容：</template>
      <template #description>
        <ul style="margin: 6px 0 0; padding-left: 18px; line-height: 1.8;">
          <li>Neo4j 图谱中该应用的所有节点与关系</li>
          <li>本地 Git 缓存目录</li>
          <li>Merkle 树缓存文件</li>
          <li>元数据文件</li>
          <li>应用信息数据库记录</li>
        </ul>
      </template>
    </a-alert>
    <div v-if="deleteTarget" style="color: rgba(0,0,0,.65); margin-top: 8px;">
      即将删除应用：<strong>{{ deleteTarget.name }}</strong>
    </div>
  </a-modal>

</template>

<style scoped>
.app-admin-page {
  padding-top: 10px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.app-name-link {
  color: #1677ff;
  cursor: pointer;
  font-weight: 500;
  text-decoration: none;
}
.app-name-link:hover {
  text-decoration: underline;
  color: #4096ff;
}

.hint {
  padding: 8px 10px;
  border: 1px dashed rgba(0, 0, 0, 0.18);
  border-radius: 8px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 12px;
  background: rgba(0, 0, 0, 0.02);
  font-size: 12px;
}
</style>


<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  createImportTask,
  createTestImportTask,
  listGitBranches,
  listGitCommits,
  listProjectVersions,
} from '../api'
import { useRepo } from '../composables/useRepo'
import ImportTaskList from './ImportTaskList.vue'

const taskListRef = ref<InstanceType<typeof ImportTaskList> | null>(null)

const {
  repos,
  loadingRepos,
  onProjectDropdown,
  readonlyUrl,
  selectedProjectName,
} = useRepo({ projectSource: 'cacheApplicationProjects' })

// ── Ref ───────────────────────────────────────────────────────────────────────
const refType = ref<'branch' | 'commit'>('branch')
const branch = ref<string>('')
const commitId = ref<string>('')
const branches = ref<string[]>([])
const commits = ref<string[]>([])
const loadingBranches = ref(false)
const loadingCommits = ref(false)
const branchOptions = computed(() => branches.value.map(b => ({ label: b, value: b })))
const commitOptions = computed(() => commits.value.map(c => ({ label: c, value: c })))

async function fetchBranches(q?: string) {
  if (!readonlyUrl.value) return
  loadingBranches.value = true
  try {
    const data = await listGitBranches(readonlyUrl.value, q)
    branches.value = Array.isArray(data?.items) ? data.items : []
  } finally {
    loadingBranches.value = false
  }
}

async function fetchCommits(q?: string) {
  if (!readonlyUrl.value) return
  loadingCommits.value = true
  try {
    const data = await listGitCommits(readonlyUrl.value, branch.value || undefined, q)
    commits.value = Array.isArray(data?.items) ? data.items : []
  } finally {
    loadingCommits.value = false
  }
}

let branchTimer: number | undefined
let commitTimer: number | undefined
function onBranchSearch(val: string) {
  if (branchTimer) window.clearTimeout(branchTimer)
  branchTimer = window.setTimeout(() => fetchBranches(val), 200)
}
function onCommitSearch(val: string) {
  if (commitTimer) window.clearTimeout(commitTimer)
  commitTimer = window.setTimeout(() => fetchCommits(val), 200)
}
async function onBranchDropdown(open: boolean) {
  if (!open) return
  if (!branches.value.length) await fetchBranches()
}
async function onCommitDropdown(open: boolean) {
  if (!open) return
  if (!commits.value.length) await fetchCommits()
}

// ── Version AutoComplete ──────────────────────────────────────────────────────
const appVersion = ref<string>('')
const versionOptions = ref<{ value: string }[]>([])
const loadingVersions = ref(false)

async function fetchVersionOptions() {
  if (!selectedProjectName.value) return
  loadingVersions.value = true
  try {
    const data = await listProjectVersions(selectedProjectName.value)
    const items = Array.isArray(data?.items) ? data.items : []
    versionOptions.value = items
      .filter((v: any) => v.version)
      .map((v: any) => ({ value: v.version }))
  } catch {
    versionOptions.value = []
  } finally {
    loadingVersions.value = false
  }
}

function onVersionSearch(val: string) {
  if (!val) { fetchVersionOptions(); return }
  versionOptions.value = versionOptions.value.filter(o =>
    o.value.toLowerCase().includes(val.toLowerCase())
  )
}

// ── Advanced collapse ─────────────────────────────────────────────────────────
const advancedOpen = ref<string[]>([])
const importForm = ref({
  maven_scan_enabled: true,
  force_maven: false,
  clear_database: false,
  auto_link_external: true,
  acceptance_enabled: true,
  acceptance_block_on_fail: false,
  acceptance_max_drop_ratio: 0.3,
})

// ── Reset on project change ───────────────────────────────────────────────────
watch(selectedProjectName, () => {
  refType.value = 'branch'
  branch.value = ''
  commitId.value = ''
  branches.value = []
  commits.value = []
  appVersion.value = ''
  versionOptions.value = []
  if (selectedProjectName.value) {
    fetchBranches()
    fetchVersionOptions()
  }
})

// ── Submit ────────────────────────────────────────────────────────────────────
const importSubmitting = ref(false)
const testTaskSubmitting = ref(false)
const testWaitSeconds = ref<number>(10)

const canSelectRef = computed(() => Boolean(readonlyUrl.value))
const importSubmitEnabled = computed(() => {
  if (!selectedProjectName.value) return false
  if (!String(appVersion.value || '').trim()) return false
  if (refType.value === 'commit') return Boolean(String(commitId.value || '').trim())
  return Boolean(String(branch.value || '').trim())
})

async function submitImport() {
  if (!selectedProjectName.value) { message.error({ content: '请先选择应用', duration: 4 }); return }
  if (!String(readonlyUrl.value || '').trim()) { message.error({ content: '应用的 Git 地址为空', duration: 4 }); return }
  if (refType.value === 'commit' && !commitId.value) { message.error({ content: '请选择或输入 CommitId', duration: 4 }); return }
  if (refType.value === 'branch' && !branch.value) { message.error({ content: '请选择或输入 Branch', duration: 4 }); return }
  if (!String(appVersion.value || '').trim()) { message.error({ content: '版本号不能为空', duration: 4 }); return }

  importSubmitting.value = true
  try {
    const payload: any = {
      repo_url: String(readonlyUrl.value || '').trim(),
      project_name: String(selectedProjectName.value || '').trim(),
      app_version: String(appVersion.value || '').trim(),
      maven_scan_enabled: Boolean(importForm.value.maven_scan_enabled),
      force_maven: Boolean(importForm.value.force_maven),
      clear_database: Boolean(importForm.value.clear_database),
      auto_link_external: Boolean(importForm.value.auto_link_external),
      task_type: 'auto',
      acceptance_enabled: Boolean(importForm.value.acceptance_enabled),
      acceptance_block_on_fail: Boolean(importForm.value.acceptance_block_on_fail),
      acceptance_max_drop_ratio: Number(importForm.value.acceptance_max_drop_ratio || 0.3),
    }
    if (refType.value === 'commit') {
      payload.commit_id = String(commitId.value || '').trim()
    } else {
      payload.branch = String(branch.value || '').trim()
    }
    const data = await createImportTask(payload)
    if (!data?.ok) throw new Error(String((data as any)?.message || '提交失败'))
    message.success('导入任务已提交')
    await taskListRef.value?.fetchImportTasks()
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    importSubmitting.value = false
  }
}

async function submitTestTask() {
  if (!selectedProjectName.value) { message.error({ content: '请先选择应用', duration: 4 }); return }
  testTaskSubmitting.value = true
  try {
    const data = await createTestImportTask({
      project_name: String(selectedProjectName.value || '').trim(),
      wait_seconds: Number(testWaitSeconds.value || 10),
    })
    if (!data?.ok) throw new Error(String((data as any)?.message || '提交失败'))
    message.success(`测试任务已创建，将模拟等待 ${testWaitSeconds.value} 秒`)
    await taskListRef.value?.fetchImportTasks()
  } catch (e: any) {
    message.error(e?.message ?? String(e))
  } finally {
    testTaskSubmitting.value = false
  }
}
</script>

<template>
  <div class="import-pane">
    <div class="import-pane-left">
      <div class="import-card">
        <a-form class="import-form-scroll" layout="vertical">

          <a-form-item label="选择应用" required>
            <a-select
              v-model:value="selectedProjectName"
              placeholder="请选择应用"
              :loading="loadingRepos"
              show-search
              :filter-option="false"
              allow-clear
              @dropdownVisibleChange="onProjectDropdown"
            >
              <template #notFoundContent>
                <a-spin v-if="loadingRepos" size="small" style="display:block;text-align:center;padding:20px" />
                <span v-else>暂无数据</span>
              </template>
              <a-select-option v-for="p in repos" :key="p.name" :value="p.name">
                <div style="display:flex;justify-content:space-between;align-items:center">
                  <span>{{ p.name }}</span>
                  <span style="color:#999;font-size:12px;margin-left:16px;overflow:hidden;text-overflow:ellipsis;max-width:160px">{{ p.url }}</span>
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="Ref 类型" required>
            <a-radio-group v-model:value="refType" button-style="solid">
              <a-radio-button value="branch">Branch</a-radio-button>
              <a-radio-button value="commit">CommitId</a-radio-button>
            </a-radio-group>
          </a-form-item>

          <a-form-item v-if="refType === 'branch'" label="Branch" required>
            <a-select
              v-model:value="branch"
              placeholder="请选择或搜索 Branch"
              :options="branchOptions"
              :disabled="!canSelectRef"
              :loading="loadingBranches"
              show-search
              :filter-option="false"
              allow-clear
              @search="onBranchSearch"
              @dropdownVisibleChange="onBranchDropdown"
            >
              <template #notFoundContent>
                <a-space v-if="loadingBranches" size="small"><a-spin size="small" /><span>加载中...</span></a-space>
                <span v-else>暂无数据</span>
              </template>
            </a-select>
          </a-form-item>

          <a-form-item v-if="refType === 'commit'" label="CommitId" required>
            <a-select
              v-model:value="commitId"
              placeholder="请选择或搜索 CommitId"
              :options="commitOptions"
              :disabled="!canSelectRef"
              :loading="loadingCommits"
              show-search
              :filter-option="false"
              allow-clear
              @search="onCommitSearch"
              @dropdownVisibleChange="onCommitDropdown"
            >
              <template #notFoundContent>
                <a-space v-if="loadingCommits" size="small"><a-spin size="small" /><span>加载中...</span></a-space>
                <span v-else>暂无数据</span>
              </template>
            </a-select>
          </a-form-item>

          <a-form-item label="版本号" required extra="可从已有版本中选择，也可直接输入新版本号">
            <a-auto-complete
              v-model:value="appVersion"
              :options="versionOptions"
              placeholder="如 2.0.5，可选已有版本或自定义"
              allow-clear
              :disabled="!selectedProjectName"
              @search="onVersionSearch"
              @focus="fetchVersionOptions"
              style="width:100%"
            />
          </a-form-item>

          <a-collapse v-model:activeKey="advancedOpen" ghost style="margin-top:4px">
            <a-collapse-panel key="adv" header="高级选项">
              <a-form-item label="导入验收判定" extra="关闭时仍采集快照；开启时输出验收结论并可联动阻断后续 auto 任务">
                <a-switch v-model:checked="importForm.acceptance_enabled" />
              </a-form-item>
              <a-form-item label="验收失败时阻断后续 auto 任务">
                <a-switch v-model:checked="importForm.acceptance_block_on_fail" :disabled="!importForm.acceptance_enabled" />
              </a-form-item>
              <a-form-item label="最大允许降幅" extra="导入后节点/关系最多允许下降的比例">
                <a-space>
                  <a-input-number
                    v-model:value="importForm.acceptance_max_drop_ratio"
                    :min="0" :max="0.95" :step="0.05" :precision="2"
                    style="width:140px"
                    :disabled="!importForm.acceptance_enabled"
                  />
                  <span class="mono">{{ (Number(importForm.acceptance_max_drop_ratio || 0) * 100).toFixed(0) }}%</span>
                </a-space>
              </a-form-item>
              <a-form-item label="Maven 扫描">
                <a-switch v-model:checked="importForm.maven_scan_enabled" />
              </a-form-item>
              <a-form-item label="强制重新解析 Maven">
                <a-switch v-model:checked="importForm.force_maven" />
              </a-form-item>
              <a-form-item label="导入前清理旧图谱（重建）">
                <a-switch v-model:checked="importForm.clear_database" />
              </a-form-item>
              <a-form-item label="自动关联外部类">
                <a-switch v-model:checked="importForm.auto_link_external" />
              </a-form-item>
            </a-collapse-panel>
          </a-collapse>

        </a-form>

        <div class="import-form-footer">
          <a-space>
            <a-tooltip title="创建模拟任务，验证调度流程，不执行真实导入">
              <a-button type="primary" ghost :loading="testTaskSubmitting" :disabled="!selectedProjectName" @click="submitTestTask">
                测试调度
              </a-button>
            </a-tooltip>
            <a-input-number
              v-model:value="testWaitSeconds"
              :min="1" :max="60" :step="1"
              style="width:90px"
              :disabled="testTaskSubmitting"
            >
              <template #addonAfter>秒</template>
            </a-input-number>
            <a-divider type="vertical" />
            <a-button type="primary" :loading="importSubmitting" :disabled="!importSubmitEnabled" @click="submitImport">
              提交任务
            </a-button>
          </a-space>
        </div>
      </div>
    </div>

    <div class="import-pane-right">
      <ImportTaskList ref="taskListRef" />
    </div>
  </div>
</template>

<style scoped>
.import-pane {
  height: 100%;
  display: flex;
  gap: 12px;
}
.import-pane-left {
  flex: 2;
  min-width: 0;
}
.import-pane-right {
  flex: 5;
  min-width: 0;
}
.import-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.import-form-scroll {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
}
.import-form-footer {
  margin-top: 10px;
  padding: 16px 0;
  border-top: 1px solid rgba(0,0,0,0.08);
}
.mono {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
</style>

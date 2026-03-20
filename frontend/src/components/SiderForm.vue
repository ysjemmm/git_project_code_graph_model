<script setup lang="ts">
import { computed, ref } from 'vue'
const props = defineProps<{
  lastOk: boolean | null
  lastMessage: string
  error: string | null
  repoMode: 'select' | 'custom'
  repoUrl: string
  selectedProjectName: string
  needRef: boolean
  readonlyUrl?: string
  readonlyBranch?: string
  readonlyCommit?: string
  repoOptions: Array<{ label: string; value: string; gitUrl?: string; branch?: string; commitHash?: string }>
  canSelectRef: boolean
  loadingRepos: boolean
  branch?: string
  branchOptions: Array<{ label: string; value: string }>
  branchDisabled: boolean
  loadingBranches: boolean
  commitId?: string
  commitOptions: Array<{ label: string; value: string }>
  commitDisabled: boolean
  loadingCommits: boolean
  canUpload: boolean
  uploading: boolean
  uploadResult: string | null
  uploadFileList: any[]
  selectedModel: string
  modelOptions: Array<{ label: string; value: string }>
}>()

const emit = defineEmits<{
  (e: 'update:repoMode', v: 'select' | 'custom'): void
  (e: 'update:repoUrl', v: string): void
  (e: 'update:selectedProjectName', v: string): void
  (e: 'update:branch', v: string | undefined): void
  (e: 'update:commitId', v: string | undefined): void
  (e: 'update:uploadFileList', v: any[]): void
  (e: 'update:selectedModel', v: string): void
  (e: 'branchSearch', v: string): void
  (e: 'commitSearch', v: string): void
  (e: 'branchDropdown', open: boolean): void
  (e: 'commitDropdown', open: boolean): void
  (e: 'projectDropdown', open: boolean): void
  (e: 'customUpload', opt: any): void
}>()

const repoUrlModel = computed({
  get: () => props.repoUrl,
  set: (v) => emit('update:repoUrl', v),
})
const selectedProjectNameModel = computed({
  get: () => props.selectedProjectName,
  set: (v) => emit('update:selectedProjectName', v),
})
const branchModel = computed({
  get: () => props.branch,
  set: (v) => emit('update:branch', v),
})
const commitModel = computed({
  get: () => props.commitId,
  set: (v) => emit('update:commitId', v),
})
const uploadFileListModel = computed({
  get: () => props.uploadFileList,
  set: (v) => emit('update:uploadFileList', v),
})
const selectedModelModel = computed({
  get: () => props.selectedModel,
  set: (v) => emit('update:selectedModel', v),
})

function switchRepoMode(mode: 'select' | 'custom') {
  emit('update:repoMode', mode)
}

const projectDropdownOpen = ref(false)

function getPopupContainer() {
  return document.body
}

function onProjectDropdown(open: boolean) {
  projectDropdownOpen.value = open
  emit('projectDropdown', open)
}
</script>

<template>
  <a-card class="panel panel-sider" title="前置信息">

    <a-form layout="vertical">
      <template v-if="repoMode === 'select'">
        <a-form-item label="项目名称" required>
          <a-select
            v-model:value="selectedProjectNameModel"
            placeholder="请搜索或选择项目"
            :loading="loadingRepos"
            show-search
            :filter-option="(input: string, opt: any) => (opt?.label ?? '').toLowerCase().includes((input || '').toLowerCase())"
            allow-clear
            :list-height="228"
            :get-popup-container="getPopupContainer"
            popup-class-name="project-select-dropdown"
            @dropdownVisibleChange="(open: boolean) => onProjectDropdown(open)"
          >
            <template #notFoundContent>
              <a-space v-if="loadingRepos" size="small">
                <a-spin size="small" />
                <span>加载中...</span>
              </a-space>
              <span v-else>暂无数据</span>
            </template>
            <a-select-option
              v-for="opt in repoOptions"
              :key="opt.value"
              :value="opt.value"
              :label="opt.label"
            >
              <a-tooltip
                placement="right"
                :get-popup-container="getPopupContainer"
                :overlay-inner-style="{ fontSize: '12px', minWidth: '260px' }"
              >
                <template #title>
                  <div class="opt-tooltip">
                    <div class="opt-tooltip-row"><span class="opt-k">URL</span><span class="opt-v">{{ opt.gitUrl || '-' }}</span></div>
                    <div class="opt-tooltip-row"><span class="opt-k">Branch</span><span class="opt-v">{{ opt.branch || '-' }}</span></div>
                    <div class="opt-tooltip-row"><span class="opt-k">Commit</span><span class="opt-v">{{ opt.commitHash || '-' }}</span></div>
                  </div>
                </template>
                {{ opt.label }}
              </a-tooltip>
            </a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="selectedProjectName">
          <a-form-item v-if="readonlyUrl" label="GIT URL">
            <a-input :value="readonlyUrl" disabled />
          </a-form-item>
          <a-form-item v-if="readonlyBranch" label="Branch">
            <a-input :value="readonlyBranch" disabled />
          </a-form-item>
          <a-form-item v-if="readonlyCommit" label="Commit">
            <a-input :value="readonlyCommit" disabled />
          </a-form-item>
        </template>
        <a-form-item :wrapper-col="{ span: 24 }">
          <a-typography-link @click="switchRepoMode('custom')">需要指定分支/版本？切换为自定义输入</a-typography-link>
        </a-form-item>
      </template>
      <template v-else>
        <a-form-item label="GIT URL" required>
          <a-input v-model:value="repoUrlModel" placeholder="请输入仓库 URL（如 https://xxx.git）" allow-clear />
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 24 }">
          <a-typography-link @click="switchRepoMode('select')">切换为选择项目</a-typography-link>
        </a-form-item>
      </template>

      <template v-if="needRef">
        <a-form-item label="GIT Branch（与 CommitId 二选一）">
          <a-select
            v-model:value="branchModel"
            placeholder="请选择 Branch"
            :options="branchOptions"
            :disabled="!canSelectRef || branchDisabled"
            :loading="loadingBranches"
            show-search
            :filter-option="false"
            @search="(v: string) => emit('branchSearch', v)"
            @dropdownVisibleChange="(open: boolean) => emit('branchDropdown', open)"
            allow-clear
          >
            <template #notFoundContent>
              <a-space v-if="loadingBranches" size="small">
                <a-spin size="small" />
                <span>加载中...</span>
              </a-space>
              <span v-else>暂无数据</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="GIT CommitId（与 Branch 二选一）">
          <a-select
            v-model:value="commitModel"
            placeholder="请选择 CommitId"
            :options="commitOptions"
            :disabled="!canSelectRef || commitDisabled"
            :loading="loadingCommits"
            show-search
            :filter-option="false"
            @search="(v: string) => emit('commitSearch', v)"
            @dropdownVisibleChange="(open: boolean) => emit('commitDropdown', open)"
            allow-clear
          >
            <template #notFoundContent>
              <a-space v-if="loadingCommits" size="small">
                <a-spin size="small" />
                <span>加载中...</span>
              </a-space>
              <span v-else>暂无数据</span>
            </template>
          </a-select>
        </a-form-item>
      </template>

      <a-form-item label="上传文件">
        <a-upload
          v-model:file-list="uploadFileListModel"
          :custom-request="(opt: any) => emit('customUpload', opt)"
          :disabled="!canUpload"
          :max-count="5"
          :show-upload-list="{ showRemoveIcon: true }"
        >
          <a-button :loading="uploading" :disabled="!canUpload">选择文件</a-button>
        </a-upload>
        <div v-if="uploadResult" class="hint" style="margin-top: 6px">{{ uploadResult }}</div>
      </a-form-item>
    </a-form>

    <a-form layout="vertical">
      <a-form-item label="模型">
        <a-select v-model:value="selectedModelModel" class="model" :options="modelOptions" />
      </a-form-item>
    </a-form>

    <!-- 由 watch(props.error) 统一用 message 提示替代 -->
  </a-card>
</template>

<style scoped>
.opt-tooltip { display: flex; flex-direction: column; gap: 4px; }
.opt-tooltip-row { display: flex; gap: 8px; align-items: baseline; }
.opt-k { color: rgba(255,255,255,.55); font-size: 11px; min-width: 46px; flex-shrink: 0; }
.opt-v { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 11px; word-break: break-all; }
</style>


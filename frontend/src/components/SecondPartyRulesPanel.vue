<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'

import {
  createSecondPartyRule,
  deleteSecondPartyRule,
  fetchSecondPartyRules,
  updateSecondPartyRule,
  type SecondPartyRule,
} from '../api/secondPartyRules'

const loading = ref(false)
const rules = ref<SecondPartyRule[]>([])

type RuleForm = {
  enabled: boolean
  sort_order: number
  group_id_regex: string
  artifact_id_regex: string
  target_project_name: string
}

const form = reactive<RuleForm>({
  enabled: true,
  sort_order: 0,
  group_id_regex: '',
  artifact_id_regex: '',
  target_project_name: '',
})

const editingRuleId = ref<number | null>(null)
const modalOpen = ref(false)
const mappingEnabled = ref(false)

function resetForm() {
  editingRuleId.value = null
  form.enabled = true
  form.sort_order = 0
  form.group_id_regex = ''
  form.artifact_id_regex = ''
  form.target_project_name = ''
  mappingEnabled.value = false
}

async function loadRules() {
  loading.value = true
  try {
    const resp = await fetchSecondPartyRules()
    if (resp?.ok) rules.value = Array.isArray(resp.items) ? resp.items : []
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(r: SecondPartyRule) {
  editingRuleId.value = r.id
  form.enabled = r.enabled
  form.sort_order = r.sort_order
  form.group_id_regex = r.group_id_regex
  form.artifact_id_regex = r.artifact_id_regex
  form.target_project_name = r.target_project_name
  mappingEnabled.value = Boolean(String(r.target_project_name || '').trim())
  modalOpen.value = true
}

async function submit() {
  const payload: any = {
    enabled: Boolean(form.enabled),
    sort_order: Number(form.sort_order || 0),
    group_id_regex: String(form.group_id_regex || '').trim(),
    artifact_id_regex: String(form.artifact_id_regex || '').trim(),
    // 允许先不映射：后续再编辑开启 mappingEnabled 并填写 target_project_name
    target_project_name: mappingEnabled.value ? String(form.target_project_name || '').trim() : '',
  }

  if (!payload.group_id_regex || !payload.artifact_id_regex) {
    message.error({ content: 'groupId_regex、artifactId_regex 均不能为空', duration: 4 })
    return
  }

  if (mappingEnabled.value && !payload.target_project_name) {
    message.error({ content: '已开启源码映射，请填写 target_project_name', duration: 4 })
    return
  }

  loading.value = true
  try {
    if (editingRuleId.value != null) {
      const resp = await updateSecondPartyRule(editingRuleId.value, payload)
      if (!resp?.ok) throw new Error(resp?.message ?? '更新失败')
      message.success({ content: '更新成功', duration: 2 })
    } else {
      const resp = await createSecondPartyRule(payload)
      if (!resp?.ok) throw new Error(resp?.message ?? '新增失败')
      message.success({ content: '新增成功', duration: 2 })
    }
    modalOpen.value = false
    await loadRules()
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    loading.value = false
  }
}

async function remove(ruleId: number) {
  loading.value = true
  try {
    const resp = await deleteSecondPartyRule(ruleId)
    if (!resp?.ok) throw new Error(resp?.message ?? '删除失败')
    message.success({ content: '删除成功', duration: 2 })
    await loadRules()
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadRules()
})
</script>

<template>
  <div class="second-party-rules">
    <a-space style="margin-bottom: 12px">
      <a-button type="primary" :loading="loading" @click="openCreate">新增二方包规则</a-button>
    </a-space>

    <a-table
      :loading="loading"
      :data-source="rules as any"
      :pagination="{ pageSize: 8 }"
      row-key="id"
      size="small"
      bordered
    >
      <a-table-column title="启用" key="enabled" data-index="enabled" :width="90">
        <template #default="{ record }">
          <a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '是' : '否' }}</a-tag>
        </template>
      </a-table-column>

      <a-table-column title="优先级(order)" key="sort_order" data-index="sort_order" :width="160">
        <template #default="{ record }">
          <span class="mono">{{ record.sort_order }}</span>
        </template>
      </a-table-column>

      <a-table-column title="groupId_regex" key="group_id_regex" data-index="group_id_regex" :width="260">
        <template #default="{ record }">
          <span class="mono">{{ record.group_id_regex }}</span>
        </template>
      </a-table-column>

      <a-table-column title="artifactId_regex" key="artifact_id_regex" data-index="artifact_id_regex" :width="320">
        <template #default="{ record }">
          <span class="mono">{{ record.artifact_id_regex }}</span>
        </template>
      </a-table-column>

      <a-table-column title="目标 B 源码项目(project_name)" key="target_project_name" data-index="target_project_name" :width="320">
        <template #default="{ record }">
          <span>{{ record.target_project_name ? record.target_project_name : '未映射' }}</span>
        </template>
      </a-table-column>

      <a-table-column title="操作" key="ops" :width="180">
        <template #default="{ record }">
          <a-space size="small">
            <a-button type="link" size="small" @click.stop="openEdit(record)">
              编辑
            </a-button>
            <a-popconfirm
              title="确认删除该规则？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="remove(record.id)"
            >
              <a-button type="link" danger size="small" @click.stop>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <a-modal
      v-model:open="modalOpen"
      :title="editingRuleId ? '编辑二方包规则' : '新增二方包规则'"
      :footer="null"
      width="720px"
      @cancel="modalOpen = false"
    >
      <a-form layout="vertical">
        <a-form-item label="启用(enabled)">
          <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>

        <a-form-item label="优先级(sort_order)">
          <a-input-number v-model:value="form.sort_order" :min="0" style="width: 180px" />
        </a-form-item>

        <a-form-item label="groupId_regex（正则包含匹配）" required>
          <a-input v-model:value="form.group_id_regex" placeholder="例如 com\\.xxx\\..* 或 com\\.xxx" />
        </a-form-item>

        <a-form-item label="artifactId_regex（正则包含匹配）" required>
          <a-input v-model:value="form.artifact_id_regex" placeholder="例如 epaas-gateway.* 或 epaas-gateway" />
        </a-form-item>

        <a-form-item label="是否进行源码映射（可选）">
          <a-switch
            v-model:checked="mappingEnabled"
            checked-children="开启"
            un-checked-children="不映射(仅识别)"
          />
        </a-form-item>

        <a-form-item v-if="mappingEnabled" label="目标 project_name（B 的 Application 源码项目名）" :required="mappingEnabled">
          <a-input v-model:value="form.target_project_name" placeholder="例如 epaas-gateway" />
        </a-form-item>

        <a-space style="margin-top: 12px">
          <a-button @click="modalOpen = false" :disabled="loading">取消</a-button>
          <a-button type="primary" @click="submit" :loading="loading">
            提交
          </a-button>
        </a-space>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>


<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { message } from 'ant-design-vue'

import {
  createSecondPartyRule,
  deleteSecondPartyRule,
  fetchSecondPartyRules,
  updateSecondPartyRule,
  type SecondPartyRule,
} from '../api'

// ── regex escape/unescape helpers ─────────────────────────────────────────────
// JS equivalent of Python's re.escape: escapes all non-alphanumeric chars
function reEscape(s: string): string {
  return s.replace(/[^A-Za-z0-9_]/g, (c) => '\\' + c)
}

// Reverse: strip backslash escapes added by reEscape
function reUnescape(s: string): string {
  return s.replace(/\\([^A-Za-z0-9_])/g, '$1')
}

// Detect if a stored value was produced by reEscape (round-trip check)
function isPlainEscaped(s: string): boolean {
  try {
    return reEscape(reUnescape(s)) === s
  } catch {
    return false
  }
}

// ── state ─────────────────────────────────────────────────────────────────────
const loading = ref(false)
const rawRules = ref<SecondPartyRule[]>([])

// 启用的排前面，同组内按 sort_order asc
const rules = computed(() =>
  [...rawRules.value].sort((a, b) => {
    if (a.enabled !== b.enabled) return a.enabled ? -1 : 1
    return a.sort_order - b.sort_order
  })
)

type RuleForm = {
  name: string
  enabled: boolean
  sort_order: number
  group_id_value: string
  group_id_plain: boolean
  artifact_id_value: string
  artifact_id_plain: boolean
}

const form = reactive<RuleForm>({
  name: '',
  enabled: true,
  sort_order: 0,
  group_id_value: '',
  group_id_plain: true,
  artifact_id_value: '',
  artifact_id_plain: true,
})

const editingRuleId = ref<number | null>(null)
const modalOpen = ref(false)

// ── display helpers ───────────────────────────────────────────────────────────
function displayValue(raw: string): string {
  return isPlainEscaped(raw) ? reUnescape(raw) : raw
}

function displayMode(raw: string): string {
  return isPlainEscaped(raw) ? '文本' : '正则'
}

// ── presets ───────────────────────────────────────────────────────────────────
const presets = [
  {
    label: '匹配某公司所有包',
    desc: '例：groupId 前缀是 com.example，所有包都算二方包',
    group_id: 'com.example',
    artifact_id: '',
    group_id_plain: true,
    artifact_id_plain: false, // artifact_id = .* (regex wildcard)
    artifact_id_raw: '.*',
  },
  {
    label: '匹配某个具体包',
    desc: '例：只有 com.example:user-service 这一个包',
    group_id: 'com.example',
    artifact_id: 'user-service',
    group_id_plain: true,
    artifact_id_plain: true,
    artifact_id_raw: '',
  },
]

function applyPreset(p: typeof presets[0]) {
  form.group_id_value = p.group_id
  form.group_id_plain = p.group_id_plain
  form.artifact_id_plain = p.artifact_id_plain
  form.artifact_id_value = p.artifact_id_plain ? p.artifact_id : p.artifact_id_raw
}

// ── form helpers ──────────────────────────────────────────────────────────────
function resetForm() {
  editingRuleId.value = null
  form.name = ''
  form.enabled = true
  form.sort_order = 0
  form.group_id_value = ''
  form.group_id_plain = true
  form.artifact_id_value = ''
  form.artifact_id_plain = true
}

function toRegex(value: string, plain: boolean): string {
  return plain ? reEscape(value) : value
}

// ── API ───────────────────────────────────────────────────────────────────────
async function loadRules() {
  loading.value = true
  try {
    const resp = await fetchSecondPartyRules()
    if (resp?.ok) rawRules.value = Array.isArray(resp.items) ? resp.items : []
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

// openEdit 保留供后续启用，当前前端不展示编辑入口
function openEdit(r: SecondPartyRule) {
  editingRuleId.value = r.id
  form.name = r.name
  form.enabled = r.enabled
  form.sort_order = r.sort_order

  const gPlain = isPlainEscaped(r.group_id_regex)
  form.group_id_plain = gPlain
  form.group_id_value = gPlain ? reUnescape(r.group_id_regex) : r.group_id_regex

  const aPlain = isPlainEscaped(r.artifact_id_regex)
  form.artifact_id_plain = aPlain
  form.artifact_id_value = aPlain ? reUnescape(r.artifact_id_regex) : r.artifact_id_regex

  modalOpen.value = true
}

async function submit() {
  const gVal = String(form.group_id_value || '').trim()
  const aVal = String(form.artifact_id_value || '').trim()
  const nameVal = String(form.name || '').trim()

  if (!nameVal) {
    message.error({ content: '请填写规则名称', duration: 4 })
    return
  }
  if (!gVal || !aVal) {
    message.error({ content: '公司/组织标识和包名都要填写', duration: 4 })
    return
  }

  const payload: any = {
    name: nameVal,
    enabled: Boolean(form.enabled),
    sort_order: Number(form.sort_order || 0),
    group_id_regex: toRegex(gVal, form.group_id_plain),
    artifact_id_regex: toRegex(aVal, form.artifact_id_plain),
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

async function toggleEnabled(r: SecondPartyRule) {
  try {
    const resp = await updateSecondPartyRule(r.id, {
      name: r.name,
      enabled: !r.enabled,
      sort_order: r.sort_order,
      group_id_regex: r.group_id_regex,
      artifact_id_regex: r.artifact_id_regex,
    })
    if (!resp?.ok) throw new Error(resp?.message ?? '操作失败')
    await loadRules()
  } catch (e: any) {
    message.error({ content: e?.message ?? String(e), duration: 4 })
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
    <a-alert type="info" show-icon style="margin-bottom: 16px;">
      <template #message>什么是二方包规则？</template>
      <template #description>
        <p style="margin: 4px 0 0;">
          Maven 依赖里，来自<strong>本公司/本团队</strong>的包叫「二方包」，其他的叫「三方包」。
          系统通过这里配置的规则来自动识别哪些依赖属于二方包。
        </p>
        <p style="margin: 6px 0 0;">
          每个 Maven 包都有 <code>groupId</code>（公司/组织标识，如 <code>com.example</code>）
          和 <code>artifactId</code>（包名，如 <code>user-service</code>）。
          直接填写文本即可，无需关心正则语法。
        </p>
      </template>
    </a-alert>

    <a-space style="margin-bottom: 12px">
      <a-button type="primary" :loading="loading" @click="openCreate">新增规则</a-button>
    </a-space>

    <a-table
      :loading="loading"
      :data-source="rules as any"
      :pagination="{ pageSize: 8 }"
      row-key="id"
      size="small"
      bordered
    >
      <a-table-column title="启用" key="enabled" data-index="enabled" :width="80">
        <template #default="{ record }">
          <a-switch
            :checked="record.enabled"
            checked-children="启用"
            un-checked-children="禁用"
            size="small"
            @change="toggleEnabled(record)"
          />
        </template>
      </a-table-column>

      <a-table-column title="规则名称" key="name" data-index="name">
        <template #default="{ record }">
          <span>{{ record.name || '-' }}</span>
        </template>
      </a-table-column>

      <a-table-column title="优先级" key="sort_order" data-index="sort_order" :width="80">
        <template #default="{ record }">
          <span class="mono">{{ record.sort_order }}</span>
        </template>
      </a-table-column>

      <a-table-column title="公司/组织标识（groupId）" key="group_id_regex" data-index="group_id_regex">
        <template #default="{ record }">
          <span class="mono">{{ displayValue(record.group_id_regex) }}</span>
          <a-tag size="small" style="margin-left: 6px; font-size: 10px;">{{ displayMode(record.group_id_regex) }}</a-tag>
        </template>
      </a-table-column>

      <a-table-column title="包名（artifactId）" key="artifact_id_regex" data-index="artifact_id_regex">
        <template #default="{ record }">
          <span class="mono">{{ displayValue(record.artifact_id_regex) }}</span>
          <a-tag size="small" style="margin-left: 6px; font-size: 10px;">{{ displayMode(record.artifact_id_regex) }}</a-tag>
        </template>
      </a-table-column>

      <a-table-column title="操作" key="ops" :width="80">
        <template #default="{ record }">
          <a-popconfirm
            title="确认删除该规则？"
            ok-text="确认"
            cancel-text="取消"
            @confirm="remove(record.id)"
          >
            <a-button type="link" danger size="small" @click.stop>删除</a-button>
          </a-popconfirm>
        </template>
      </a-table-column>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <!-- openEdit 保留供后续启用 -->
    <span v-if="false" @click="openEdit(rules[0])"></span>
    <a-modal
      v-model:open="modalOpen"
      :title="editingRuleId ? '编辑二方包规则' : '新增二方包规则'"
      :footer="null"
      width="600px"
      @cancel="modalOpen = false"
    >
      <!-- 快速预设（仅新增时显示） -->
      <template v-if="!editingRuleId">
        <div class="preset-label">快速填入常见场景：</div>
        <a-space wrap style="margin-bottom: 16px;">
          <a-tooltip v-for="p in presets" :key="p.label" :title="p.desc">
            <a-button size="small" @click="applyPreset(p)">{{ p.label }}</a-button>
          </a-tooltip>
        </a-space>
      </template>

      <a-form layout="vertical">
        <!-- 规则名称 -->
        <a-form-item label="规则名称" required>
          <a-input
            v-model:value="form.name"
            placeholder="例：公司内部包、timevale 全系列"
            :maxlength="64"
            show-count
          />
        </a-form-item>

        <!-- groupId -->
        <a-form-item>
          <template #label>
            <span>公司/组织标识</span>
            <span class="field-hint">Maven groupId，如 <code>com.example</code></span>
          </template>
          <a-input-group compact>
            <a-radio-group
              v-model:value="form.group_id_plain"
              option-type="button"
              button-style="solid"
              style="width: 120px"
            >
              <a-radio-button :value="true">文本</a-radio-button>
              <a-radio-button :value="false">正则</a-radio-button>
            </a-radio-group>
            <a-input
              v-model:value="form.group_id_value"
              style="width: calc(100% - 120px)"
              :placeholder="form.group_id_plain ? '例：com.example（包含匹配）' : '例：^com\\.example\\..*$'"
            />
          </a-input-group>
          <div class="input-tip" v-if="form.group_id_plain">文本模式：只要 groupId 包含这段文字就匹配，无需转义</div>
          <div class="input-tip" v-else>正则模式：使用 Java/Python 正则语法，<code>.</code> 需写成 <code>\.</code></div>
        </a-form-item>

        <!-- artifactId -->
        <a-form-item>
          <template #label>
            <span>包名</span>
            <span class="field-hint">Maven artifactId，如 <code>user-service</code></span>
          </template>
          <a-input-group compact>
            <a-radio-group
              v-model:value="form.artifact_id_plain"
              option-type="button"
              button-style="solid"
              style="width: 120px"
            >
              <a-radio-button :value="true">文本</a-radio-button>
              <a-radio-button :value="false">正则</a-radio-button>
            </a-radio-group>
            <a-input
              v-model:value="form.artifact_id_value"
              style="width: calc(100% - 120px)"
              :placeholder="form.artifact_id_plain ? '例：user-service，留空则匹配所有包名' : '例：.*（匹配所有）'"
            />
          </a-input-group>
          <div class="input-tip" v-if="form.artifact_id_plain">
            文本模式：包含匹配。若想匹配该 groupId 下<strong>所有包</strong>，切换到正则模式填 <code>.*</code>
          </div>
          <div class="input-tip" v-else>正则模式：<code>.*</code> 匹配所有，<code>gateway.*</code> 匹配 gateway 开头</div>
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="优先级（数字越小越先匹配）">
              <a-input-number v-model:value="form.sort_order" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用">
              <a-switch v-model:checked="form.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-space style="margin-top: 4px">
          <a-button @click="modalOpen = false" :disabled="loading">取消</a-button>
          <a-button type="primary" @click="submit" :loading="loading">保存</a-button>
        </a-space>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.second-party-rules {
  padding-top: 10px;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.preset-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 6px;
}

.field-hint {
  margin-left: 8px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.4);
  font-weight: 400;
}

.input-tip {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.4);
}

code {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 4px;
  border-radius: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}
</style>

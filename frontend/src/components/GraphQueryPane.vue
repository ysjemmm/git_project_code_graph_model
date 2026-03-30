<script setup lang="ts">
import { computed, ref } from 'vue'
import { LinkOutlined } from '@ant-design/icons-vue'
import { NEO4J_AURA_QUERY_URL } from '../constants'
import { runGraphQuery } from '../api'

interface QueryResult {
  columns: string[]
  rows: Array<Record<string, any>>
}

type QueryTemplate = {
  key: string
  name: string
  cypher: string
  desc: string
}

const queryTemplates: QueryTemplate[] = [
  {
    key: 'project-overview',
    name: '项目节点概览',
    desc: '查看每个项目节点数量（Top 30）',
    cypher: `MATCH (n)
WHERE coalesce(n.belong_project, '') <> ''
RETURN n.belong_project AS project, count(*) AS node_count
ORDER BY node_count DESC
LIMIT coalesce($limit, 30)`,
  },
  {
    key: 'depends-on-top',
    name: '依赖关联 Top',
    desc: '查看 DEPENDS_ON 关联最密集的项目',
    cypher: `MATCH (a:Project)-[r:DEPENDS_ON]->(b:Project)
RETURN a.name AS source_project, b.name AS target_project, count(r) AS rel_count
ORDER BY rel_count DESC
LIMIT coalesce($limit, 50)`,
  },
  {
    key: 'javaobject-type',
    name: '对象类型分布',
    desc: '统计 JavaObject 的 from_type 分布',
    cypher: `MATCH (n:JavaObject)
RETURN coalesce(n.from_type, 'UNKNOWN') AS from_type, count(*) AS cnt
ORDER BY cnt DESC
LIMIT coalesce($limit, 30)`,
  },
]

const selectedTemplateKey = ref(queryTemplates[0]?.key || '')
const queryText = ref(queryTemplates[0]?.cypher || '')
const queryLimit = ref(200)
const queryLoading = ref(false)
const queryError = ref<string | null>(null)
const queryResult = ref<QueryResult>({ columns: [], rows: [] })

const emit = defineEmits<{
  (e: 'open-aura'): void
}>()

function applyQueryTemplate(key: string) {
  const t = queryTemplates.find((x) => x.key === key)
  if (!t) return
  selectedTemplateKey.value = key
  queryText.value = t.cypher
}

function formatCell(v: any): string {
  if (v == null) return '-'
  if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') return String(v)
  try {
    return JSON.stringify(v)
  } catch {
    return String(v)
  }
}

const queryTableColumns = computed(() =>
  queryResult.value.columns.map((c) => ({ title: c, dataIndex: c, key: c, ellipsis: true })),
)

const queryTableData = computed(() =>
  queryResult.value.rows.map((r, i) => ({ __rowKey: `row-${i}`, ...r })),
)

const rowCount = computed(() => queryResult.value.rows.length)

const visualMetric = computed(() => {
  const rows = queryResult.value.rows
  const columns = queryResult.value.columns
  if (!rows.length || !columns.length) return null
  const numberCol = columns.find((col) => rows.every((x) => x[col] == null || typeof x[col] === 'number'))
  if (!numberCol) return null
  const dimCol = columns.find((col) => col !== numberCol && rows.some((x) => typeof x[col] === 'string'))
  if (!dimCol) return null
  const items = rows
    .slice(0, 12)
    .map((x) => ({ name: String(x[dimCol] ?? '-'), value: Number(x[numberCol] ?? 0) }))
  const max = Math.max(1, ...items.map((x) => x.value))
  return { dimCol, numberCol, items, max }
})

async function runQuery() {
  const cypher = String(queryText.value || '').trim()
  if (!cypher) {
    queryError.value = '请先输入 Cypher 查询语句'
    return
  }
  queryLoading.value = true
  queryError.value = null
  try {
    const data = await runGraphQuery({
      cypher,
      limit: Number(queryLimit.value || 200),
      params: { limit: Number(queryLimit.value || 200) },
    })
    if (!data?.ok) {
      queryError.value = data?.message || '查询失败'
      queryResult.value = { columns: [], rows: [] }
      return
    }
    queryResult.value = {
      columns: Array.isArray(data.columns) ? data.columns : [],
      rows: Array.isArray(data.rows) ? data.rows : [],
    }
  } catch (e: any) {
    queryError.value = e?.message ?? String(e)
    queryResult.value = { columns: [], rows: [] }
  } finally {
    queryLoading.value = false
  }
}

function openAura() {
  window.open(NEO4J_AURA_QUERY_URL, '_blank')
}
</script>

<template>
  <div class="query-pane">
    <div class="query-left">
      <a-card class="graph-query-card" size="small" title="查询模板与编辑器">
        <a-form class="graph-query-form" layout="vertical">
          <a-form-item class="graph-query-form-item" label="模板">
            <a-select
              v-model:value="selectedTemplateKey"
              :options="queryTemplates.map(t => ({ value: t.key, label: t.name }))"
              @change="(v: string) => applyQueryTemplate(v)"
            />
            <div class="query-template-desc">
              {{ queryTemplates.find(t => t.key === selectedTemplateKey)?.desc || '-' }}
            </div>
          </a-form-item>
          <a-form-item class="graph-query-form-item" label="最大返回行数">
            <a-input-number v-model:value="queryLimit" :min="1" :max="2000" style="width: 180px;" />
          </a-form-item>
          <a-form-item class="graph-query-form-item" label="Cypher">
            <a-textarea v-model:value="queryText" :rows="16" class="mono query-editor" />
          </a-form-item>
        </a-form>
        <a-space class="graph-query-buttons">
          <a-button type="primary" :loading="queryLoading" @click="runQuery">执行查询</a-button>
          <a-button @click="openAura">
            <template #icon><LinkOutlined /></template>
            在 Aura 中打开
          </a-button>
        </a-space>
      </a-card>
    </div>
    <div class="query-right">
      <a-card class="graph-query-result-card" size="small" title="查询结果与可视化">
        <div class="query-result-summary">
          <a-statistic title="返回行数" :value="rowCount" />
        </div>
        <div v-if="visualMetric" class="query-chart">
          <div class="query-chart-title">
            可视化（{{ visualMetric.numberCol }} by {{ visualMetric.dimCol }}，Top {{ visualMetric.items.length }}）
          </div>
          <div v-for="it in visualMetric.items" :key="it.name" class="query-chart-row">
            <div class="query-chart-name mono">{{ it.name }}</div>
            <div class="query-chart-bar-wrap">
              <div class="query-chart-bar" :style="{ width: `${Math.max(2, Math.round((it.value / visualMetric.max) * 100))}%` }" />
            </div>
            <div class="query-chart-value mono">{{ it.value }}</div>
          </div>
        </div>
        <a-table
          :loading="queryLoading"
          :columns="queryTableColumns as any"
          :data-source="queryTableData as any"
          :pagination="{ pageSize: 10, showSizeChanger: true }"
          size="small"
          bordered
          row-key="__rowKey"
          :scroll="{ x: true }"
        >
          <template #bodyCell="{ column, record }">
            <a-tooltip :title="formatCell(record[column.key])" placement="topLeft">
              <span class="mono cell-ellipsis">{{ formatCell(record[column.key]) }}</span>
            </a-tooltip>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<style scoped>
.graph-query-card, .graph-query-result-card{
  height: 100%;
}

.graph-query-card > :deep(.ant-card-body),
.graph-query-result-card > :deep(.ant-card-body) {
  overflow-y: auto;
  max-height: calc(100% - 38px - 12px);
  display: flex;
  flex-direction: column;
  padding: 12px 12px 0 12px
}

.query-pane {
  height: 100%;
  display: flex;
  gap: 16px;
  padding-bottom: 16px;
}

.query-left {
  width: 500px;
}

.query-right {
  flex: 1;
  min-width: 0;
}

.graph-query-form {
  flex: 1;
  overflow-y: auto;
}

.query-template-desc {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.query-result-summary {
  margin-bottom: 12px;
}

.query-chart {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
}

.query-chart-title {
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
}

.query-chart-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.query-chart-name {
  width: 180px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
}

.query-chart-bar-wrap {
  flex: 1;
  height: 16px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  overflow: hidden;
}

.query-chart-bar {
  height: 100%;
  background: linear-gradient(90deg, #1890ff, #36cfc9);
  transition: width 0.3s ease;
}

.query-chart-value {
  width: 60px;
  text-align: right;
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

</style>

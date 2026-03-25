<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { FileOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

const props = defineProps<{ diff: string }>()

interface DiffLine {
  type: 'add' | 'del' | 'hunk' | 'ctx'
  content: string
  newNo: number | null
  oldNo: number | null
}

interface FileDiff {
  oldPath: string
  newPath: string
  displayPath: string
  filename: string
  changeType: 'modified' | 'added' | 'deleted'
  addCount: number
  delCount: number
  lines: DiffLine[]
}

function extractPath(raw: string, prefix: 'a' | 'b'): string {
  const body = raw.slice(4).split('\t')[0].trim()
  return body.replace(new RegExp(`^${prefix}\\/`), '')
}

function basename(p: string) {
  return p.replace(/\\/g, '/').split('/').pop() || p
}

const files = computed<FileDiff[]>(() => {
  const result: FileDiff[] = []
  if (!props.diff?.trim()) return result

  const rawLines = props.diff.split('\n')
  let current: FileDiff | null = null
  let oldLineNo = 0
  let newLineNo = 0
  let awaitingNewPath = false

  const pushCurrent = () => {
    if (current && (current.lines.length > 0 || current.newPath)) result.push(current)
    current = null
  }

  for (const raw of rawLines) {
    if (raw.startsWith('diff --git ')) {
      pushCurrent()
      const m = raw.match(/^diff --git a\/(.+?) b\/(.+)$/)
      current = { oldPath: m?.[1] ?? '', newPath: m?.[2] ?? '', displayPath: m?.[2] ?? m?.[1] ?? '', filename: '', changeType: 'modified', addCount: 0, delCount: 0, lines: [] }
      oldLineNo = 0; newLineNo = 0; awaitingNewPath = false
      continue
    }
    if (raw.startsWith('index ') || raw.startsWith('new file mode') || raw.startsWith('deleted file mode') ||
        raw.startsWith('old mode') || raw.startsWith('new mode') || raw.startsWith('similarity index') ||
        raw.startsWith('rename from') || raw.startsWith('rename to') || raw.startsWith('Binary files')) {
      if (current) {
        if (raw.startsWith('new file mode')) current.changeType = 'added'
        if (raw.startsWith('deleted file mode')) current.changeType = 'deleted'
      }
      continue
    }
    if (raw.startsWith('--- ')) {
      if (!current) {
        pushCurrent()
        current = { oldPath: '', newPath: '', displayPath: '', filename: '', changeType: 'modified', addCount: 0, delCount: 0, lines: [] }
        oldLineNo = 0; newLineNo = 0
      }
      current.oldPath = extractPath(raw, 'a')
      awaitingNewPath = true
      continue
    }
    if (raw.startsWith('+++ ') && current && awaitingNewPath) {
      current.newPath = extractPath(raw, 'b')
      awaitingNewPath = false
      if (current.oldPath === '/dev/null') { current.changeType = 'added'; current.displayPath = current.newPath }
      else if (current.newPath === '/dev/null') { current.changeType = 'deleted'; current.displayPath = current.oldPath }
      else { current.changeType = 'modified'; current.displayPath = current.newPath || current.oldPath }
      current.filename = basename(current.displayPath)
      continue
    }
    if (!current) continue
    if (raw.startsWith('@@ ')) {
      const m = raw.match(/@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@/)
      if (m) { oldLineNo = parseInt(m[1]); newLineNo = parseInt(m[2]) }
      current.lines.push({ type: 'hunk', content: raw, newNo: null, oldNo: null })
    } else if (raw.startsWith('+')) {
      current.lines.push({ type: 'add', content: raw.slice(1), newNo: newLineNo++, oldNo: null })
      current.addCount++
    } else if (raw.startsWith('-')) {
      current.lines.push({ type: 'del', content: raw.slice(1), oldNo: oldLineNo++, newNo: null })
      current.delCount++
    } else if (raw.startsWith(' ')) {
      current.lines.push({ type: 'ctx', content: raw.slice(1), oldNo: oldLineNo++, newNo: newLineNo++ })
    }
  }
  pushCurrent()
  return result
})

const activeIdx = ref(0)
const activeFile = computed(() => files.value[activeIdx.value] ?? null)

// 标签栏横向滚动
const tabsRef = ref<HTMLElement | null>(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

function updateScrollState() {
  const el = tabsRef.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 2
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 2
}

function scrollTabs(dir: 'left' | 'right') {
  const el = tabsRef.value
  if (!el) return
  el.scrollBy({ left: dir === 'left' ? -200 : 200, behavior: 'smooth' })
}

// 切换 tab 时确保对应 tab 可见
watch(activeIdx, async (i) => {
  await nextTick()
  const el = tabsRef.value
  if (!el) return
  const tab = el.children[i] as HTMLElement | undefined
  tab?.scrollIntoView({ inline: 'nearest', block: 'nearest' })
})

onMounted(() => {
  nextTick(() => {
    tabsRef.value?.addEventListener('scroll', updateScrollState, { passive: true })
    updateScrollState()
    // ResizeObserver 监听容器宽度变化
    ro = new ResizeObserver(updateScrollState)
    if (tabsRef.value) ro.observe(tabsRef.value)
  })
})

let ro: ResizeObserver | null = null
onUnmounted(() => {
  tabsRef.value?.removeEventListener('scroll', updateScrollState)
  ro?.disconnect()
})

function changeTypeLabel(t: FileDiff['changeType']) {
  return t === 'added' ? '新增' : t === 'deleted' ? '删除' : '修改'
}
function changeTypeCls(t: FileDiff['changeType']) {
  return t === 'added' ? 'dv-badge--add' : t === 'deleted' ? 'dv-badge--del' : 'dv-badge--mod'
}
</script>

<template>
  <div class="dv-root">
    <div v-if="files.length === 0" class="dv-empty">暂无 diff 内容</div>
    <template v-else>
      <!-- 文件标签页导航 -->
      <div class="dv-tabs-wrap">
        <button v-if="canScrollLeft" class="dv-tabs-arrow dv-tabs-arrow--left" @click="scrollTabs('left')">
          <LeftOutlined />
        </button>
        <div ref="tabsRef" class="dv-tabs" @scroll="updateScrollState">
          <div
            v-for="(f, i) in files"
            :key="i"
            class="dv-tab"
            :class="{ 'dv-tab--active': activeIdx === i }"
            @click="activeIdx = i"
          >
            <FileOutlined class="dv-tab-icon" />
            <span class="dv-tab-name" :title="f.displayPath">{{ f.filename || f.displayPath }}</span>
            <span class="dv-badge" :class="changeTypeCls(f.changeType)">{{ changeTypeLabel(f.changeType) }}</span>
            <span class="dv-tab-stat">
              <span class="dv-s-add">+{{ f.addCount }}</span>
              <span class="dv-s-del">-{{ f.delCount }}</span>
            </span>
          </div>
        </div>
        <button v-if="canScrollRight" class="dv-tabs-arrow dv-tabs-arrow--right" @click="scrollTabs('right')">
          <RightOutlined />
        </button>
      </div>

      <!-- 当前文件 diff -->
      <template v-if="activeFile">
        <!-- 文件路径栏 -->
        <div class="dv-filepath">
          <FileOutlined style="opacity:.5;flex-shrink:0" />
          <span class="dv-filepath-text">{{ activeFile.displayPath }}</span>
          <span class="dv-badge" :class="changeTypeCls(activeFile.changeType)">{{ changeTypeLabel(activeFile.changeType) }}</span>
          <span class="dv-s-add">+{{ activeFile.addCount }}</span>
          <span class="dv-s-del">-{{ activeFile.delCount }}</span>
        </div>

        <!-- diff 表格 -->
        <div class="dv-body">
          <table class="dv-table">
            <tbody>
              <tr v-for="(line, li) in activeFile.lines" :key="li" :class="'dv-line dv-line--' + line.type">
                <td class="dv-gutter">{{ line.type === 'del' || line.type === 'ctx' ? line.oldNo : '' }}</td>
                <td class="dv-gutter">{{ line.type === 'add' || line.type === 'ctx' ? line.newNo : '' }}</td>
                <td class="dv-sign">
                  <span v-if="line.type === 'add'">+</span>
                  <span v-else-if="line.type === 'del'">-</span>
                </td>
                <td class="dv-code">
                  <span v-if="line.type === 'hunk'" class="dv-hunk-label">{{ line.content }}</span>
                  <span v-else>{{ line.content }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </template>
  </div>
</template>

<style scoped>
.dv-root {
  font-family: ui-monospace, 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.dv-empty { padding: 24px; text-align: center; color: rgba(0,0,0,.35); font-family: sans-serif; }

/* 标签页导航 */
.dv-tabs-wrap {
  position: relative;
  display: flex;
  align-items: stretch;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}
.dv-tabs {
  display: flex;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none;
}
.dv-tabs::-webkit-scrollbar { display: none; }
.dv-tabs-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  border: none;
  background: #ececec;
  color: rgba(0,0,0,.5);
  cursor: pointer;
  font-size: 11px;
  transition: background 0.12s, color 0.12s;
  z-index: 1;
}
.dv-tabs-arrow:hover { background: #e0e0e0; color: rgba(0,0,0,.85); }
.dv-tabs-arrow--left { border-right: 1px solid #e0e0e0; }
.dv-tabs-arrow--right { border-left: 1px solid #e0e0e0; }
.dv-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  cursor: pointer;
  border-right: 1px solid #e8e8e8;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: background 0.12s, border-color 0.12s;
  color: rgba(0,0,0,.55);
  font-size: 12px;
}
.dv-tab:hover { background: #ebebeb; color: rgba(0,0,0,.85); }
.dv-tab--active {
  background: #fff;
  border-bottom-color: #1677ff;
  color: #1677ff;
  font-weight: 600;
}
.dv-tab-icon { font-size: 11px; opacity: .6; }
.dv-tab-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dv-tab-stat { display: flex; gap: 4px; margin-left: 2px; }

/* 文件路径栏 */
.dv-filepath {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 12px;
  color: rgba(0,0,0,.65);
}
.dv-filepath-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: rgba(0,0,0,.85);
}

/* 变更类型徽章 */
.dv-badge {
  flex-shrink: 0;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  border: 1px solid;
  font-family: sans-serif;
}
.dv-badge--add { color: #389e0d; border-color: #b7eb8f; background: #f6ffed; }
.dv-badge--del { color: #cf1322; border-color: #ffa39e; background: #fff1f0; }
.dv-badge--mod { color: #1677ff; border-color: #91caff; background: #e6f4ff; }

.dv-s-add { color: #389e0d; font-weight: 600; }
.dv-s-del { color: #cf1322; font-weight: 600; }

/* diff 表格 */
.dv-body { overflow-x: auto; }
.dv-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.dv-gutter {
  width: 44px; min-width: 44px;
  text-align: right; padding: 1px 8px;
  color: rgba(0,0,0,.3); user-select: none;
  vertical-align: top; border-right: 1px solid #e8e8e8;
  font-size: 11px; line-height: 20px;
}
.dv-sign {
  width: 18px; min-width: 18px;
  text-align: center; padding: 1px 0;
  font-weight: 700; vertical-align: top; line-height: 20px;
}
.dv-code {
  padding: 1px 8px 1px 4px;
  white-space: pre; word-break: normal;
  overflow: hidden; line-height: 20px; vertical-align: top;
}

.dv-line--add { background: #f6ffed; }
.dv-line--add .dv-gutter { background: #d9f7be; }
.dv-line--add .dv-sign   { color: #389e0d; }
.dv-line--add .dv-code   { color: #135200; }

.dv-line--del { background: #fff1f0; }
.dv-line--del .dv-gutter { background: #ffccc7; }
.dv-line--del .dv-sign   { color: #cf1322; }
.dv-line--del .dv-code   { color: #820014; }

.dv-line--hunk { background: #e6f4ff; }
.dv-line--hunk .dv-gutter { background: #bae0ff; }
.dv-line--hunk .dv-code   { color: #0958d9; }
.dv-hunk-label { font-style: italic; }

.dv-line--ctx { background: #fff; }
.dv-line--ctx .dv-code { color: rgba(0,0,0,.75); }
</style>

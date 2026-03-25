<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import {
  ApartmentOutlined,
  BugOutlined,
  DatabaseOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  NodeIndexOutlined,
  ToolOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { getUserInfo, logout } from './auth'

const collapsed = ref(false)
const router = useRouter()
const route = useRoute()

const selectedKeys = computed(() => {
  if (route.path.startsWith('/application-admin')) return ['application-admin']
  if (route.path.startsWith('/second-party-rules')) return ['second-party-rules']
  if (route.path.startsWith('/graph')) return ['graph']
  if (route.path.startsWith('/bugfix-workflow')) return ['bugfix-workflow']
  if (route.path.startsWith('/bugfix')) return ['bugfix']
  return ['bugfix']
})

function go(key: string) {
  const k = String(key || '').trim()
  const map: Record<string, string> = {
    'bugfix': '/bugfix',
    'bugfix-workflow': '/bugfix-workflow',
    'graph': '/graph',
    'application-admin': '/application-admin',
    'second-party-rules': '/second-party-rules',
  }
  const target = map[k]
  if (target) void router.push(target)
}

const pageTitle = ref('Bugfix 管理台')
watchEffect(() => {
  const t = (route.meta?.title as string) || ''
  pageTitle.value = t ? `Bugfix 管理台 · ${t}` : 'Bugfix 管理台'
  document.title = pageTitle.value
})

// 用户信息（从本地缓存获取）
const userInfo = computed(() => {
  const info = getUserInfo()
  return info || { alias: '未知用户', name: '未知用户' }
})

// 登出
function handleLogout() {
  logout()
}
</script>

<template>
  <a-config-provider :theme="{ token: { borderRadius: 8 } }">
    <a-layout class="layout-root">
      <a-layout-sider
        class="layout-sider"
        :collapsed="collapsed"
        collapsible
        :trigger="null"
        width="220"
      >
        <div class="brand">
          <ApartmentOutlined class="brand-icon" />
          <span v-if="!collapsed" class="brand-text">Bugfix 管理台</span>
        </div>

        <a-menu
          theme="dark"
          mode="inline"
          :selectedKeys="selectedKeys"
          @click="(info: any) => go(String(info.key))"
        >
          <a-menu-item key="bugfix">
            <template #icon><BugOutlined /></template>
            <span>Bugfix 对话</span>
          </a-menu-item>

          <a-menu-item key="bugfix-workflow">
            <template #icon><NodeIndexOutlined /></template>
            <span>Bugfix 流程编排</span>
          </a-menu-item>

          <a-menu-item key="graph">
            <template #icon><ApartmentOutlined /></template>
            <span>代码图谱</span>
          </a-menu-item>

          <a-menu-item key="application-admin">
            <template #icon><DatabaseOutlined /></template>
            <span>应用管理</span>
          </a-menu-item>

          <a-menu-item key="second-party-rules">
            <template #icon><ToolOutlined /></template>
            <span>二方包规则</span>
          </a-menu-item>
        </a-menu>

        <div class="sider-user" :title="collapsed ? `${userInfo.alias} (${userInfo.name})` : undefined">
          <a-avatar class="sider-avatar">
            <template #icon><UserOutlined /></template>
          </a-avatar>
          <div v-if="!collapsed" class="sider-user-info">
            <div class="sider-username">{{ userInfo.alias }}</div>
            <div class="sider-user-id">{{ userInfo.name }}</div>
          </div>
        </div>
        
        <!-- 登出按钮 -->
        <div v-if="!collapsed" class="sider-logout">
          <a-button type="text" size="small" @click="handleLogout" style="width: 100%; color: rgba(255,255,255,.75)">
            <template #icon><LogoutOutlined /></template>
            登出
          </a-button>
        </div>
      </a-layout-sider>

      <a-layout>
        <a-layout-header class="layout-header">
          <a-button type="text" class="collapse-btn" @click="collapsed = !collapsed">
            <template #icon>
              <MenuUnfoldOutlined v-if="collapsed" />
              <MenuFoldOutlined v-else />
            </template>
          </a-button>
          <div class="header-title">{{ pageTitle }}</div>
        </a-layout-header>

        <a-layout-content class="layout-content">
          <div class="page">
            <RouterView />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-config-provider>
</template>

<style>
.layout-root { min-height: 100vh; }
.layout-sider {
  box-shadow: 0 0 0 1px rgba(255,255,255,.06) inset;
  position: relative;
  z-index: 10000;
}
.layout-sider :deep(.ant-layout-sider-children) {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.layout-sider :deep(.ant-menu) {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 0;
}
.sider-user {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  margin-bottom: 8px;
  border-top: 1px solid rgba(255,255,255,.08);
  background: #001529;
  cursor: default;
}
.sider-avatar {
  background: #aa3bff !important;
  color: #fff !important;
  font-weight: 600;
  flex-shrink: 0;
}
.sider-user-info {
  flex: 1;
  min-width: 0;
}
.sider-username {
  color: rgba(255,255,255,.95);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}
.sider-user-id {
  color: rgba(255,255,255,.65);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sider-logout {
  padding: 0 16px 16px;
}
.brand {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  color: rgba(255,255,255,.92);
  border-bottom: 1px solid rgba(255,255,255,.08);
}
.brand-icon { font-size: 18px; }
.brand-text { font-weight: 700; letter-spacing: .2px; }

.layout-header {
  background: #fff;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid rgba(0,0,0,.06);
}
.collapse-btn { width: 40px; height: 40px; }
.header-title { font-size: 14px; font-weight: 700; color: rgba(0,0,0,.85); }
.layout-content { padding: 2px 16px; background: #f5f7fb; overflow: hidden; }

.page { width: 100%; height: calc(100vh - 56px - 32px); box-sizing: border-box; overflow: hidden; }
.main-row { height: 100%; }
.sider-col, .chat-col { height: 100%; }
.hint { color: rgba(0,0,0,.65); }
.panel { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.panel .ant-card-body { flex: 1; min-height: 0; overflow: hidden; }
.panel-sider .ant-card-body { overflow: auto; }
.panel-chat .ant-card-body { display: flex; flex-direction: column; padding: 0 10px; }

.chat-log {
  flex: 1;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
}

.sticky-q {
  position: sticky; top: 0; z-index: 2;
  padding: 10px 12px; margin-bottom: 10px; border-radius: 12px;
  background: #e8f0fe; border: 1px solid #adc6ff;
}
.sticky-text { font-size: 14px; font-weight: 600; line-height: 1.45; white-space: pre-wrap; word-break: break-word; color: #1d3461; }

.msg { display: flex; margin: 18px 0; }
.msg.user { justify-content: flex-end; }
.msg.ai { flex-direction: column; align-items: flex-start; gap: 8px; }

.bubble {
  padding: 10px 14px; border-radius: 12px;
  line-height: 1.6; word-break: break-word; text-align: left; position: relative;
}
.msg.user .bubble { background: #1677ff; color: #fff; }
.msg.ai .bubble { background: rgba(0,0,0,.04); }

.cursor {
  display: inline-block; width: 2px; height: 1em;
  background: currentColor; margin-left: 2px; vertical-align: text-bottom;
  animation: blink .8s step-end infinite;
}
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.thinking-pulse { display: flex; align-items: center; gap: 5px; padding: 12px 16px; }
.dot {
  width: 7px; height: 7px; border-radius: 50%; background: rgba(0,0,0,.3);
  animation: bounce 1.2s ease-in-out infinite;
}
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%,80%,100%{transform:scale(0.7);opacity:.5} 40%{transform:scale(1);opacity:1} }

.md-body { font-size: 14px; line-height: 1.7; }
.md-body p { margin: 0 0 8px; }
.md-body p:last-child { margin-bottom: 0; }
.md-body h1, .md-body h2, .md-body h3, .md-body h4 { margin: 12px 0 6px; font-weight: 600; }
.md-body ul, .md-body ol { padding-left: 20px; margin: 6px 0; }
.md-body li { margin: 3px 0; }
.md-body strong { font-weight: 600; }
.md-body em { font-style: italic; }
.md-body code:not(.hljs) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12.5px; padding: 1px 5px; border-radius: 4px;
  background: rgba(0,0,0,.07); color: #c7254e;
}
.md-body .hljs-block {
  margin: 8px 0; border-radius: 8px; overflow: auto;
  background: #1e1e2e; border: 1px solid rgba(255,255,255,.08);
}
.md-body .hljs-block code {
  display: block; padding: 12px 14px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12.5px; line-height: 1.5; color: #cdd6f4;
}
.md-body blockquote {
  margin: 8px 0; padding: 6px 12px;
  border-left: 3px solid #1677ff; background: rgba(22,119,255,.05);
  border-radius: 0 6px 6px 0; color: rgba(0,0,0,.65);
}
.md-body table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 13px; }
.md-body th, .md-body td { border: 1px solid rgba(0,0,0,.12); padding: 6px 10px; }
.md-body th { background: rgba(0,0,0,.04); font-weight: 600; }

.thinking-wrap { width: 100%; }
.thinking-wrap .ant-collapse { background: transparent; border: none; }
.thinking-wrap .ant-collapse-item {
  border: 1px solid rgba(0,0,0,.08) !important; border-radius: 8px !important;
  margin-bottom: 4px; overflow: hidden; background: rgba(0,0,0,.02);
}
.thinking-wrap .ant-collapse-header {
  padding: 6px 10px !important;
  font-size: 12px;
  color: rgba(0,0,0,.55);
  justify-content: flex-start !important;
  text-align: left;
}
.thinking-wrap .ant-collapse-header .ant-collapse-header-text {
  flex: 1 1 auto;
  text-align: left;
}
.thinking-wrap .ant-collapse-content-box { padding: 0 !important; }

.tool-content {
  margin: 0; padding: 10px 14px;
  font-size: 12.5px; line-height: 1.6;
  text-align: left;
  word-break: break-word;
  background: rgba(0,0,0,.025);
  max-height: 360px; overflow: auto;
}
.tool-content.md-body { font-size: 12.5px; }
.tool-content.md-body p { margin: 0 0 6px; }
.tool-content.md-body p:last-child { margin-bottom: 0; }
.tool-content.md-body code:not(.hljs) {
  font-size: 12px; padding: 1px 4px; border-radius: 3px;
  background: rgba(0,0,0,.07); color: #c7254e;
}
.tool-content.md-body .hljs-block {
  margin: 6px 0; border-radius: 6px;
  background: #1a1b26; border: 1px solid rgba(255,255,255,.06);
  overflow-x: auto; max-width: 100%;
}
.tool-content.md-body .hljs-block code {
  display: block; padding: 10px 14px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px; line-height: 1.55; color: #cdd6f4;
  white-space: pre; word-break: normal; overflow-wrap: normal;
}
.tool-content.md-body strong { font-weight: 600; }

.tool-header { display: inline-flex; align-items: center; gap: 5px; }
.tool-icon { font-size: 13px; }
.tool-header-text { display: inline-block; }
.tool-link-btn { padding: 0 4px; height: 20px; line-height: 20px; }

.chat-input { margin-top: 14px; flex: 0 0 auto; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.model { width: 220px; }
</style>

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
  ExperimentOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { getUserInfo } from './auth'
import './style.css'

const collapsed = ref(false)
const router = useRouter()
const route = useRoute()

const selectedKeys = computed(() => {
  if (route.path.startsWith('/application-admin')) return ['application-admin']
  if (route.path.startsWith('/second-party-rules')) return ['second-party-rules']
  if (route.path.startsWith('/tool-test')) return ['tool-test']
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
    'tool-test': '/tool-test',
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
        <div class="main-container">
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

            <a-menu-item key="tool-test">
              <template #icon><ExperimentOutlined /></template>
              <span>AI 工具单测</span>
            </a-menu-item>
          </a-menu>
        </div>  

        <div class="main-container-footer">
          <!-- 折叠状态：只显示头像和按钮 -->
          <div v-if="collapsed" class="sider-collapsed-view">
            <a-avatar class="sider-collapsed-avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <div class="sider-collapse-btn-wrapper">
              <a-button type="text" class="sider-collapse-btn" @click="collapsed = !collapsed">
                <template #icon>
                  <MenuUnfoldOutlined />
                </template>
              </a-button>
            </div>
          </div>
          
          <!-- 展开状态：显示完整用户信息 + 按钮 -->
          <div v-else class="sider-user-wrapper">
            <div class="sider-user">
              <a-avatar class="sider-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <div class="sider-user-info">
                <div class="sider-username">{{ userInfo.alias }}</div>
                <div class="sider-user-id">{{ userInfo.name }}</div>
              </div>
            </div>
            <div class="sider-collapse-btn-wrapper">
              <a-button type="text" class="sider-collapse-btn" @click="collapsed = !collapsed">
                <template #icon>
                  <MenuFoldOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </a-layout-sider>

      <div class="page">
        <RouterView />
      </div>
    </a-layout>
  </a-config-provider>
</template>

<style scoped>
/* App.vue 布局样式（全局，因为需要作用于 Ant Design 组件） */
.layout-root { 
  height: 100vh;
  overflow-y: auto;
}
.layout-sider {
  box-shadow: 0 0 0 1px rgba(255,255,255,.06) inset;
  z-index: 100;
}
.page {
  flex: 1;
  min-width: 0;
}
.layout-sider :deep(.ant-layout-sider-children) {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.layout-sider .ant-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 0;
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
  fontflow: hidden;
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
.main-container-footer {
  flex-shrink: 0;
  background: #001529;
  border-top: 1px solid rgba(255,255,255,.08);
}
/* 用户信息包装器（左右布局） */
.sider-user-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  gap: 10px;
}
/* 折叠状态视图（垂直布局） */
.sider-collapsed-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  gap: 8px;
}
.sider-collapsed-avatar {
  background: #aa3bff !important;
  color: #fff !important;
  font-weight: 600;
}
/* 用户信息区域（左侧） */
.sider-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
/* 折叠按钮包装器（右侧） */
.sider-collapse-btn-wrapper {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
/* 折叠按钮样式 */
.sider-collapse-btn {
  width: 32px;
  height: 32px;
  font-size: 14px;
  color: rgba(255,255,255,.65) !important;
  cursor: pointer;
  transition: all 0.3s;
  background: transparent !important;
}
.sider-collapse-btn:hover {
  color: rgba(255,255,255,.85) !important;
  background: rgba(255,255,255,.1) !important;
}

.brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
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
  padding-inline: 0px !important;
}
.layout-header .collapse-btn {
  width: 40px;
  height: 40px;
  font-size: 16px;
  color: white !important;
  cursor: pointer;
  transition: all 0.3s;
  background: transparent !important;
}
.layout-header .collapse-btn:hover {
  color: rgba(221, 221, 221, 0.85) !important;
  background: rgba(0,0,0,.06) !important;
}
.header-title { font-size: 14px; font-weight: 700; color: rgba(0,0,0,.85); }
</style>

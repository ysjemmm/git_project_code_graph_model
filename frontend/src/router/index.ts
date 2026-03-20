import { createRouter, createWebHistory } from 'vue-router'

export const routes = [
  {
    path: '/',
    redirect: '/bugfix',
  },
  {
    path: '/bugfix',
    name: 'bugfix',
    component: () => import('../pages/BugfixChatPage.vue'),
    meta: { title: 'Bugfix 对话' },
  },
  {
    path: '/graph',
    name: 'graph',
    component: () => import('../pages/GraphAdminPage.vue'),
    meta: { title: '管理代码图谱' },
  },
  {
    path: '/project-meta',
    name: 'project-meta',
    component: () => import('../pages/ProjectMetaPage.vue'),
    meta: { title: '项目元数据' },
  },
  {
    path: '/second-party-rules',
    name: 'second-party-rules',
    component: () => import('../pages/SecondPartyRulesPage.vue'),
    meta: { title: '二方包规则' },
  },
] as const

const router = createRouter({
  history: createWebHistory(),
  routes: routes as any,
  scrollBehavior: () => ({ top: 0 }),
})

export default router


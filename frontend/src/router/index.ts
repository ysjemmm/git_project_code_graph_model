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
    path: '/application-admin',
    name: 'application-admin',
    component: () => import('../pages/ApplicationAdminPage.vue'),
    meta: { title: '应用管理' },
  },
  {
    path: '/application-admin/:repoName',
    name: 'application-admin-detail',
    component: () => import('../pages/ApplicationDetailPage.vue'),
    meta: { title: '应用详情' },
  },
  {
    path: '/application-admin/:repoName/link-overview',
    name: 'application-link-overview',
    component: () => import('../pages/ApplicationLinkOverviewPage.vue'),
    meta: { title: '关联视图' },
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


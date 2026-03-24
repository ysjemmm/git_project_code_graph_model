import { createRouter, createWebHistory } from 'vue-router'
import { fetchAndCacheUserInfo } from '../auth'

export const routes = [
  {
    path: '/',
    redirect: '/bugfix',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../pages/LoginPage.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/bugfix',
    name: 'bugfix',
    component: () => import('../pages/BugfixChatPage.vue'),
    meta: { title: 'Bugfix 对话' },
  },
  {
    path: '/bugfix-workflow',
    name: 'bugfix-workflow',
    component: () => import('../pages/BugfixWorkflowPage.vue'),
    meta: { title: 'Bugfix 流程编排' },
  },
  {
    path: '/bugfix-workflow/bugfix-details/:id',
    name: 'bugfix-details',
    component: () => import('../pages/BugfixDetailsPage.vue'),
    meta: { title: 'Bugfix 执行详情' },
  },
  {
    path: '/graph',
    name: 'graph',
    component: () => import('../pages/GraphAdminPage.vue'),
    meta: { title: '代码图谱' },
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

// 全局路由守卫：静默获取用户信息，不做认证拦截
router.beforeEach(async (to, from, next) => {
  await fetchAndCacheUserInfo()
  next()
})

export default router


import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser, clearAuth } from '@/utils/auth'

/**
 * 路由分三大部分：
 * - /user     普通用户端
 * - /merchant 商家后台（merchant 角色）
 * - /admin    管理员后台（admin 角色）
 * 页面在阶段 5 逐个开发，脚手架阶段先提供占位页。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    redirect: '/user/home',
    meta: { roles: ['user', 'merchant', 'admin'] },
    children: [
      {
        path: 'home',
        name: 'UserHome',
        component: () => import('@/views/user/Home.vue'),
        meta: { title: '推荐首页' }
      },
      {
        path: 'shops/:id',
        name: 'ShopDetail',
        component: () => import('@/views/user/ShopDetail.vue'),
        meta: { title: '商铺详情' }
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/merchant',
    component: () => import('@/layouts/MerchantLayout.vue'),
    redirect: '/merchant/dashboard',
    meta: { roles: ['merchant'] },
    children: [
      {
        path: 'dashboard',
        name: 'MerchantDashboard',
        component: () => import('@/views/merchant/Dashboard.vue'),
        meta: { title: '商家工作台' }
      },
      {
        path: 'shops',
        name: 'MerchantShops',
        component: () => import('@/views/merchant/ShopManage.vue'),
        meta: { title: '商铺管理' }
      },
      {
        path: 'vouchers',
        name: 'MerchantVouchers',
        component: () => import('@/views/merchant/VoucherManage.vue'),
        meta: { title: '优惠券管理' }
      },
      {
        path: 'writeoff',
        name: 'MerchantWriteoff',
        component: () => import('@/views/merchant/WriteoffManage.vue'),
        meta: { title: '核销记录' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/overview',
    meta: { roles: ['admin'] },
    children: [
      {
        path: 'overview',
        name: 'AdminOverview',
        component: () => import('@/views/admin/Overview.vue'),
        meta: { title: '管理后台' }
      },
      {
        path: 'review-audit',
        name: 'AdminReviewAudit',
        component: () => import('@/views/admin/ReviewAudit.vue'),
        meta: { title: '点评审核' }
      },
      {
        path: 'shop-audit',
        name: 'AdminShopAudit',
        component: () => import('@/views/admin/ShopAudit.vue'),
        meta: { title: '商铺审核' }
      },
      {
        path: 'merchant-audit',
        name: 'AdminMerchantAudit',
        component: () => import('@/views/admin/MerchantAudit.vue'),
        meta: { title: '入驻审核' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '账号管理' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限' }
  },
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 免登录白名单
const WHITE_LIST = ['/login']

// 角色默认首页（多角色用户按优先级进入对应后台）
const ROLE_HOME = {
  admin: '/admin/overview',
  merchant: '/merchant/dashboard',
  user: '/user/home'
}
const ROLE_PRIORITY = ['admin', 'merchant', 'user']

/**
 * 全局前置守卫：登录校验 + 基于角色的权限控制
 */
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 本地生活探店平台` : '本地生活探店平台'

  const token = getToken()
  // 未登录
  if (!token) {
    if (WHITE_LIST.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
    return
  }

  // 已登录访问登录页：直接进入其默认首页
  if (to.path === '/login') {
    next(getHomePath())
    return
  }

  // 校验该路由要求的角色
  const requiredRoles = to.matched.find((r) => r.meta.roles)?.meta.roles
  if (requiredRoles) {
    const user = getUser()
    const roles = user?.roles || []
    if (!requiredRoles.some((role) => roles.includes(role))) {
      // 有权限但不是该区域：回自己的首页；无任何有效角色：踢回登录
      const home = getHomePath()
      next(home === '/login' ? '/login' : home === to.path ? '/403' : home)
      return
    }
  }
  next()
})

function getHomePath() {
  const user = getUser()
  if (!user || !Array.isArray(user.roles) || user.roles.length === 0) {
    clearAuth()
    return '/login'
  }
  const role = ROLE_PRIORITY.find((r) => user.roles.includes(r))
  return role ? ROLE_HOME[role] : '/login'
}

export default router
export { getHomePath }

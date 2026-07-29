import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '管理员登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'Odometer' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'products/create',
        name: 'ProductCreate',
        component: () => import('@/views/product/ProductForm.vue'),
        meta: { title: '新增商品', hidden: true }
      },
      {
        path: 'products/:id/edit',
        name: 'ProductEdit',
        component: () => import('@/views/product/ProductForm.vue'),
        meta: { title: '编辑商品', hidden: true }
      },
      {
        path: 'inventory',
        name: 'InventoryList',
        component: () => import('@/views/product/InventoryList.vue'),
        meta: { title: '库存管理', icon: 'Box' }
      },
      {
        path: 'brands',
        name: 'BrandList',
        component: () => import('@/views/system/BrandList.vue'),
        meta: { title: '品牌管理', icon: 'Medal' }
      },
      {
        path: 'categories',
        name: 'CategoryList',
        component: () => import('@/views/system/CategoryList.vue'),
        meta: { title: '分类管理', icon: 'Menu' }
      },
      {
        path: 'banners',
        name: 'BannerList',
        component: () => import('@/views/system/BannerList.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      },
      {
        path: 'coupons',
        name: 'CouponList',
        component: () => import('@/views/system/CouponList.vue'),
        meta: { title: '优惠券管理', icon: 'Ticket' }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/OrderDetail.vue'),
        meta: { title: '订单详情', hidden: true }
      },
      {
        path: 'refunds',
        name: 'RefundList',
        component: () => import('@/views/order/RefundList.vue'),
        meta: { title: '退款管理', icon: 'RefreshLeft' }
      },
      {
        path: 'reviews',
        name: 'ReviewList',
        component: () => import('@/views/system/ReviewList.vue'),
        meta: { title: '评价管理', icon: 'ChatDotRound' }
      },
      {
        path: 'access',
        name: 'AccessList',
        component: () => import('@/views/system/AccessList.vue'),
        meta: { title: '角色权限', icon: 'Lock' }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'points-products',
        name: 'PointsProductList',
        component: () => import('@/views/points/PointsProductList.vue'),
        meta: { title: '积分兑换管理', icon: 'Gift' }
      },
      {
        path: 'member-levels',
        name: 'MemberLevelList',
        component: () => import('@/views/member/MemberLevelList.vue'),
        meta: { title: '会员等级管理', icon: 'Medal' }
      },
      {
        path: 'marketing-activities',
        name: 'MarketingActivityList',
        component: () => import('@/views/marketing/MarketingActivityList.vue'),
        meta: { title: '营销活动管理', icon: 'Promotion' }
      },
      {
        path: 'aftersale',
        name: 'AfterSaleList',
        component: () => import('@/views/aftersale/AfterSaleList.vue'),
        meta: { title: '售后管理', icon: 'Refresh' }
      },
      {
        path: 'operation-logs',
        name: 'OperationLogList',
        component: () => import('@/views/system/OperationLogList.vue'),
        meta: { title: '审计日志', icon: 'Document' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心', icon: 'UserFilled' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from) => {
  const adminStore = useAdminStore()

  if (to.meta.requiresAuth && !adminStore.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.name === 'Login' && adminStore.isLoggedIn) {
    return { name: 'Dashboard' }
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - 管理后台`
  }
})

export default router

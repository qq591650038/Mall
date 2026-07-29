import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/products',
    name: 'ProductList',
    component: () => import('@/views/product/ProductList.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/ProductDetail.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/compare',
    name: 'ProductCompare',
    component: () => import('@/views/product/ProductCompare.vue'),
    meta: { title: '商品对比' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/Cart.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/orders/create',
    name: 'OrderCreate',
    component: () => import('@/views/order/OrderCreate.vue'),
    meta: { title: '确认订单', requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/orders/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/OrderDetail.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/orders/:id/payment',
    name: 'Payment',
    component: () => import('@/views/order/Payment.vue'),
    meta: { title: '收银台', requiresAuth: true }
  },
  {
    path: '/orders/:id/payment-result',
    name: 'PaymentResult',
    component: () => import('@/views/order/PaymentResult.vue'),
    meta: { title: '支付结果', requiresAuth: true }
  },
  {
    path: '/orders/:id/refund',
    name: 'RefundApply',
    component: () => import('@/views/order/RefundApply.vue'),
    meta: { title: '申请退款', requiresAuth: true }
  },
  {
    path: '/refunds',
    name: 'RefundList',
    component: () => import('@/views/order/RefundList.vue'),
    meta: { title: '退款管理', requiresAuth: true }
  },
  {
    path: '/refunds/:id',
    name: 'RefundDetail',
    component: () => import('@/views/order/RefundDetail.vue'),
    meta: { title: '售后详情', requiresAuth: true }
  },
  {
    path: '/orders/:id/review',
    name: 'ReviewCreate',
    component: () => import('@/views/order/ReviewCreate.vue'),
    meta: { title: '发表评价', requiresAuth: true }
  },
  {
    path: '/orders/:id/logistics',
    name: 'Logistics',
    component: () => import('@/views/order/Logistics.vue'),
    meta: { title: '物流跟踪', requiresAuth: true }
  },
  {
    path: '/reviews/mine',
    name: 'MyReviews',
    component: () => import('@/views/user/MyReviews.vue'),
    meta: { title: '我的评价', requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/user/Notifications.vue'),
    meta: { title: '消息中心', requiresAuth: true }
  },
  {
    path: '/points',
    name: 'PointsCenter',
    component: () => import('@/views/user/PointsCenter.vue'),
    meta: { title: '积分中心', requiresAuth: true }
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/user/UserCenter.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/addresses',
    name: 'AddressList',
    component: () => import('@/views/address/AddressList.vue'),
    meta: { title: '收货地址', requiresAuth: true }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('@/views/user/Favorites.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/coupons',
    name: 'MyCoupons',
    component: () => import('@/views/user/MyCoupons.vue'),
    meta: { title: '我的优惠券', requiresAuth: true }
  },
  {
    path: '/browse-history',
    name: 'BrowseHistory',
    component: () => import('@/views/user/BrowseHistory.vue'),
    meta: { title: '浏览历史', requiresAuth: true }
  },
  {
    path: '/marketing/activities',
    name: 'MarketingList',
    component: () => import('@/views/marketing/MarketingList.vue'),
    meta: { title: '营销活动' }
  },
  {
    path: '/marketing/activities/:id',
    name: 'MarketingDetail',
    component: () => import('@/views/marketing/MarketingDetail.vue'),
    meta: { title: '活动详情' }
  },
  {
    path: '/categories',
    name: 'Category',
    component: () => import('@/views/category/Category.vue'),
    meta: { title: '全部分类' }
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('@/views/Leaderboard.vue'),
    meta: { title: 'Leaderboard' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    return { name: 'Home' }
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - 商城`
  }
})

export default router

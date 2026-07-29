<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useRouter, useRoute } from 'vue-router'
import { getActiveBanners, getCategoryList } from '@/api/common'
import type { Banner, Category } from '@/types'
import { getPopularSearches, getSearchSuggestions } from '@/api/product'
import { getUnreadNotificationCount } from '@/api/notification'

const userStore = useUserStore()
const cartStore = useCartStore()
const router = useRouter()
const route = useRoute()

const banners = ref<Banner[]>([])
const categories = ref<Category[]>([])
const searchQuery = ref('')
const popularSearches = ref<string[]>([])
const searchHistory = ref<string[]>(JSON.parse(localStorage.getItem('mall_search_history') || '[]'))
const unreadNotificationCount = ref(0)
let suggestTimer: ReturnType<typeof setTimeout> | undefined

onMounted(async () => {
  try {
    const [b, c] = await Promise.all([
      getActiveBanners().catch(() => []),
      getCategoryList().catch(() => [])
    ])
    banners.value = b
    categories.value = c
    popularSearches.value = await getPopularSearches().catch(() => [])
  } catch {
    // ignore
  }
  if (userStore.isLoggedIn) {
    cartStore.fetchCart().catch(() => {})
    getUnreadNotificationCount().then(count => { unreadNotificationCount.value = count || 0 }).catch(() => {})
  }
})

function handleSearch() {
  if (searchQuery.value.trim()) {
    const value = searchQuery.value.trim()
    searchHistory.value = [value, ...searchHistory.value.filter(item => item !== value)].slice(0, 8)
    localStorage.setItem('mall_search_history', JSON.stringify(searchHistory.value))
    router.push({ name: 'ProductList', query: { keyword: value } })
  }
}

function querySearch(query: string, callback: (items: { value: string }[]) => void) {
  clearTimeout(suggestTimer)
  suggestTimer = setTimeout(async () => {
    const values = query.trim() ? await getSearchSuggestions(query.trim()).catch(() => []) : [...searchHistory.value, ...popularSearches.value]
    callback([...new Set(values)].slice(0, 10).map(value => ({ value })))
  }, query.trim() ? 220 : 0)
}

function goLogin() {
  router.push({ name: 'Login' })
}

function goRegister() {
  router.push({ name: 'Register' })
}

function goProfile() {
  router.push({ name: 'UserCenter' })
}

async function handleLogout() {
  await userStore.logout()
  cartStore.items = []
  router.push({ name: 'Home' })
}

const isActive = (name: string) => route.name === name
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span class="logo-icon">🛍️</span>
          <span class="logo-text">Mall</span>
        </router-link>
        <nav class="nav">
          <router-link to="/" :class="{ active: isActive('Home') }">首页</router-link>
          <router-link to="/categories" :class="{ active: isActive('Category') }">分类</router-link>
          <router-link to="/products" :class="{ active: isActive('ProductList') }">商品</router-link>
          <router-link to="/marketing/activities" :class="{ active: isActive('MarketingList') }">活动</router-link>
          <router-link to="/leaderboard" :class="{ active: isActive('Leaderboard') }">排行榜</router-link>
        </nav>
      </div>
      <div class="header-center">
        <el-autocomplete
          v-model="searchQuery"
          placeholder="搜索商品..."
          size="large"
          clearable
          :fetch-suggestions="querySearch"
          @select="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-autocomplete>
      </div>
      <div class="header-right">
        <router-link to="/cart" class="cart-link">
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
            <el-icon :size="22"><ShoppingCart /></el-icon>
          </el-badge>
        </router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link to="/notifications" class="notification-link" aria-label="消息中心">
            <el-badge :value="unreadNotificationCount" :hidden="unreadNotificationCount === 0" :max="99">
              <el-icon :size="22"><Bell /></el-icon>
            </el-badge>
          </router-link>
          <el-dropdown>
            <span class="user-info" @click.stop>
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/notifications')">消息中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/favorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="router.push('/reviews/mine')">我的评价</el-dropdown-item>
                <el-dropdown-item @click="router.push('/coupons')">优惠券</el-dropdown-item>
                <el-dropdown-item @click="router.push('/points')">积分中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/browse-history')">浏览历史</el-dropdown-item>
                <el-dropdown-item @click="router.push('/addresses')">收货地址</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="goLogin">登录</el-button>
          <el-button @click="goRegister">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.app-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  .header-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 32px;

    .logo {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      color: #ff6b35;
      font-weight: 700;

      .logo-icon { font-size: 28px; }
      .logo-text { font-size: 22px; }
    }

    .nav {
      display: flex;
      gap: 24px;

      a {
        text-decoration: none;
        color: #666;
        font-size: 15px;
        padding: 6px 12px;
        border-radius: 6px;
        transition: all 0.2s;

        &:hover { color: #ff6b35; background: #fff5f0; }
        &.active { color: #ff6b35; font-weight: 500; }
      }
    }
  }

  .header-center {
    flex: 1;
    max-width: 480px;

    :deep(.el-input-group) {
      border-radius: 20px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .cart-link {
      text-decoration: none;
      color: #333;
      display: flex;
      align-items: center;
    }

    .notification-link {
      display: flex;
      align-items: center;
      color: #333;
      text-decoration: none;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username { font-size: 14px; color: #333; }
    }
  }
}

@media (max-width: 760px) {
  .app-header {
    .header-inner {
      height: 58px;
      padding: 0 12px;
      gap: 12px;
    }

    .header-left {
      flex: 1;
      min-width: 0;
      gap: 14px;

      .logo {
        flex: none;
        .logo-icon { font-size: 22px; }
        .logo-text { font-size: 18px; }
      }

      .nav {
        min-width: 0;
        gap: 2px;
        overflow-x: auto;
        scrollbar-width: none;
        a { padding: 6px 7px; font-size: 13px; white-space: nowrap; }
      }
    }

    .header-center { display: none; }
    .header-right {
      flex: none;
      gap: 8px;
      .username { display: none; }
      :deep(.el-button) { padding: 7px 9px; }
    }
  }
}

@media (max-width: 390px) {
  .app-header {
    .header-left .nav a { padding-inline: 5px; }
    .header-right .cart-link { display: none; }
  }
}
</style>

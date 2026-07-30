<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateUser } from '@/api/auth'
import { ElMessage } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const userStore = useUserStore()
const activeMenu = ref('profile')
const saving = ref(false)
const profileForm = ref({ nickname: '', email: '' })

const menus = [
  { key: 'profile', label: '个人信息', icon: '👤' },
  { key: 'orders', label: '我的订单', icon: '📦' },
  { key: 'addresses', label: '收货地址', icon: '📍' },
  { key: 'favorites', label: '我的收藏', icon: '❤️' },
  { key: 'coupons', label: '优惠券', icon: '🎟️' },
  { key: 'history', label: '浏览历史', icon: '🕐' }
  ,{ key: 'points', label: '积分中心', icon: '⭐' }
]

async function loadUserInfo() {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch { /* handled */ }
  }
  profileForm.value.nickname = userStore.userInfo?.nickname || ''
  profileForm.value.email = userStore.userInfo?.email || ''
}

onMounted(loadUserInfo)

function navigateTo(key: string) {
  const routeMap: Record<string, string> = {
    orders: 'OrderList',
    addresses: 'AddressList',
    favorites: 'Favorites',
    coupons: 'MyCoupons',
    history: 'BrowseHistory',
    points: 'PointsCenter'
  }
  if (routeMap[key]) {
    router.push({ name: routeMap[key] })
  } else {
    activeMenu.value = key
  }
}

async function handleLogout() {
  await userStore.logout()
  router.push({ name: 'Home' })
}

async function saveProfile() {
  if (!userStore.userInfo) return
  saving.value = true
  try {
    await updateUser({ nickname: profileForm.value.nickname.trim(), email: profileForm.value.email.trim() })
    await userStore.fetchUserInfo()
    ElMessage.success('个人资料已保存')
  } finally {
    saving.value = false
  }
}

const userInfo = computed(() => userStore.userInfo)
</script>

<template>
  <div class="user-center-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <aside class="sidebar">
          <div class="user-card">
            <el-avatar :size="64" :src="userInfo?.avatar">
              {{ userInfo?.username?.[0]?.toUpperCase() }}
            </el-avatar>
            <h3>{{ userInfo?.nickname || userInfo?.username }}</h3>
            <p class="user-level">普通会员</p>
          </div>
          <nav class="menu">
            <div
              v-for="menu in menus"
              :key="menu.key"
              class="menu-item"
              :class="{ active: activeMenu === menu.key }"
              @click="navigateTo(menu.key)"
            >
              <span class="menu-icon">{{ menu.icon }}</span>
              <span>{{ menu.label }}</span>
            </div>
          </nav>
          <el-button @click="handleLogout" class="logout-btn">退出登录</el-button>
        </aside>
        <section class="content">
          <div v-if="activeMenu === 'profile'" class="profile-section">
            <h2>个人信息</h2>
            <el-form :model="userInfo" label-width="100px" class="profile-form">
              <el-form-item label="用户名">
                <el-input :model-value="userInfo?.username" disabled />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="30" show-word-limit />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input :model-value="userInfo?.phone" disabled />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="profileForm.email" placeholder="请输入邮箱" maxlength="100" />
              </el-form-item>
              <el-form-item label="注册时间">
                <el-input :model-value="userInfo?.createTime" disabled />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
              </el-form-item>
            </el-form>
          </div>
          <div v-else class="placeholder-section">
            <el-empty description="敬请期待" />
          </div>
        </section>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.user-center-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; display: flex; gap: 20px; }

.sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;

  h3 { margin: 12px 0 4px; font-size: 16px; }
  .user-level { color: #999; margin: 0; font-size: 13px; }
}

.menu {
  background: #fff;
  border-radius: 12px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;

  &:hover { background: #f5f5f5; }
  &.active { background: rgba(216, 169, 169, 0.12); color: #C4908F; font-weight: 500; }
  .menu-icon { font-size: 18px; }
}

.logout-btn { margin-top: auto; }

.content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  min-height: 500px;
}

.profile-section h2 { margin: 0 0 20px; font-size: 18px; }
.profile-form { max-width: 500px; }
.placeholder-section { display: flex; justify-content: center; align-items: center; height: 400px; }
</style>

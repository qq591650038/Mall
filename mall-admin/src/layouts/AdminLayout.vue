<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const menuCollapsed = ref(false)

const menuItems = computed(() => {
  const main = router.options.routes.find(r => r.path === '/')
  if (!main?.children) return []
  return main.children.filter(c => c.meta && !c.meta.hidden && c.meta.icon)
})

const currentTitle = computed(() => {
  const matched = route.matched
  for (let i = matched.length - 1; i >= 0; i--) {
    if (matched[i].meta?.title) return matched[i].meta.title as string
  }
  return '管理后台'
})

async function handleLogout() {
  await adminStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Login' })
}

function toggleMenu() {
  menuCollapsed.value = !menuCollapsed.value
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="menuCollapsed ? '64px' : '220px'" class="sidebar" :class="{ collapsed: menuCollapsed }">
      <div class="logo">
        <span v-if="!menuCollapsed" class="logo-text">Mall Admin</span>
        <span v-else class="logo-icon">M</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="menuCollapsed"
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ff6b35"
        router
        class="menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="`/${item.path}`"
        >
          <el-icon><component :is="item.meta?.icon" /></el-icon>
          <template #title>{{ item.meta?.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleMenu">
            <Fold v-if="!menuCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="adminStore.adminInfo?.avatar">
                {{ adminStore.adminInfo?.username?.[0]?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ adminStore.adminInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/')">返回商城</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-layout { height: 100vh; }

.sidebar {
  background: #001529;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #002140;
  color: #fff;
  .logo-text { font-size: 18px; font-weight: 600; }
  .logo-icon { font-size: 24px; font-weight: 700; }
}

.menu {
  border-right: none;
  :deep(.el-menu-item) {
    &:hover { background: #1890ff1a !important; }
    &.is-active { background: #1890ff1a !important; }
  }
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { cursor: pointer; color: #666; }

.header-right { display: flex; align-items: center; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  .username { font-size: 14px; color: #333; }
}

.main-content { background: #f0f2f5; padding: 20px; }
@media (max-width: 768px) {
  .sidebar:not(.collapsed) { width: 220px !important; box-shadow: 8px 0 24px rgba(0,0,0,.12); }
  .main-content { padding: 14px; }
}
</style>

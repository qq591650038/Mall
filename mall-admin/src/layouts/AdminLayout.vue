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
        background-color="#2E3238"
        text-color="#a6adb4"
        active-text-color="#C4908F"
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
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

$color-sidebar-bg: #2E3238;
$color-sidebar-active: #D8A9A9;
$color-header-bg: #FFF9F5;
$color-main-bg: #F5E6D3;
$color-accent: #D8A9A9;
$shadow-soft: 0 4px 20px rgba(212, 169, 169, 0.15);
$shadow-elevated: 0 8px 30px rgba(0, 0, 0, 0.1);

.admin-layout {
  height: 100vh;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
}

.sidebar {
  background: $color-sidebar-bg;
  transition: width 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.1);

  &.collapsed {
    .logo {
      .logo-text { opacity: 0; transform: translateX(-20px); }
    }
  }
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(216, 169, 169, 0.15) 0%, transparent 100%);
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(216, 169, 169, 0.1) 0%, transparent 70%);
    animation: pulse 8s ease-in-out infinite;
  }

  .logo-text {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 1px;
    transition: all 0.3s;
    z-index: 1;
  }

  .logo-icon {
    font-size: 26px;
    font-weight: 700;
    z-index: 1;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.menu {
  border-right: none;
  padding: 16px 0;

  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin: 4px 12px;
    border-radius: 12px;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

    &:hover {
      background: rgba(216, 169, 169, 0.1) !important;
      transform: translateX(4px);
    }

    &.is-active {
      background: linear-gradient(90deg, rgba(216, 169, 169, 0.2) 0%, rgba(216, 169, 169, 0.05) 100%) !important;
      color: $color-sidebar-active !important;
      font-weight: 600;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 24px;
        background: $color-sidebar-active;
        border-radius: 0 4px 4px 0;
      }
    }
  }
}

.header {
  background: $color-header-bg;
  border-bottom: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(216, 169, 169, 0.3), transparent);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  cursor: pointer;
  color: $color-sidebar-bg;
  transition: all 0.3s;
  padding: 8px;
  border-radius: 8px;

  &:hover {
    background: rgba(216, 169, 169, 0.1);
    transform: scale(1.1);
  }

  &:active {
    transform: scale(0.95);
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 24px;
  transition: all 0.3s;

  &:hover {
    background: rgba(216, 169, 169, 0.1);
  }

  .username {
    font-size: 15px;
    color: #3A3A3A;
    font-weight: 500;
  }

  :deep(.el-avatar) {
    border: 2px solid $color-accent;
    transition: transform 0.3s;
  }

  &:hover :deep(.el-avatar) {
    transform: scale(1.1);
  }
}

.main-content {
  background: linear-gradient(135deg, $color-main-bg 0%, $color-header-bg 100%);
  padding: 24px;
  position: relative;
  overflow: auto;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23D8A9A9' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
    pointer-events: none;
    opacity: 0.6;
  }
}

@media (max-width: 768px) {
  .sidebar:not(.collapsed) {
    width: 220px !important;
    box-shadow: 8px 0 32px rgba(0, 0, 0, 0.2);
    z-index: 1000;
  }

  .main-content {
    padding: 16px;
  }

  .header {
    padding: 0 20px;
  }
}
</style>

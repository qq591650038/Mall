<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserPage, getUserById, updateUserStatus, type AdminUser } from '@/api/user'

const list = ref<AdminUser[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref<number>()
const detailVisible = ref(false)
const selected = ref<AdminUser>()

async function load() {
  loading.value = true
  try {
    const result = await getUserPage({ current: current.value, size: size.value, keyword: keyword.value || undefined, status: status.value })
    list.value = result.list || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  current.value = 1
  load()
}

async function changeStatus(user: AdminUser, enabled: boolean) {
  const nextStatus = enabled ? 1 : 0
  await updateUserStatus(user.id, nextStatus)
  user.status = nextStatus
  ElMessage.success('用户状态已更新')
}

async function showDetail(user: AdminUser) {
  selected.value = await getUserById(user.id)
  detailVisible.value = true
}

onMounted(load)
</script>

<template>
  <div class="user-list-page">
    <div class="page-header"><h2>用户管理</h2></div>
    <div class="filter-bar">
      <el-input v-model="keyword" clearable placeholder="用户名、昵称、手机或邮箱" style="width: 280px" @keyup.enter="search" />
      <el-select v-model="status" clearable placeholder="用户状态" style="width: 130px"><el-option label="正常" :value="1" /><el-option label="已禁用" :value="0" /></el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div class="table-card">
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="190" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-switch :model-value="row.status === 1" @change="(value: boolean) => changeStatus(row, value)" /></template></el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column prop="lastLoginTime" label="最近登录" width="180" />
        <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无用户" />
      <el-pagination v-if="total" class="pagination" background layout="total, sizes, prev, pager, next" v-model:current-page="current" v-model:page-size="size" :page-sizes="[20, 50, 100]" :total="total" @size-change="load" @current-change="load" />
    </div>
    <el-dialog v-model="detailVisible" title="用户详情" width="520px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="用户ID">{{ selected.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selected.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ selected.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ selected.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selected.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近登录IP">{{ selected.lastLoginIp || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ selected.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header h2 { margin: 0; }
.filter-bar { display: flex; gap: 12px; }
.table-card { padding: 16px; background: #fff; }
.pagination { justify-content: flex-end; margin-top: 16px; }
</style>

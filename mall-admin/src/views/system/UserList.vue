<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {type AdminUser, getUserById, getUserCursorPage, updateUserStatus} from '@/api/user'

const list = ref<AdminUser[]>([]);
const loading = ref(false);
const size = ref(20);
const keyword = ref('');
const status = ref<number>();
const cursor = ref<string>();
const nextCursor = ref<string>();
const history = ref<string[]>([]);
const page = ref(1);
const detailVisible = ref(false);
const selected = ref<AdminUser>()

async function load() {
  loading.value = true;
  try {
    const result = await getUserCursorPage({
      size: size.value,
      keyword: keyword.value || undefined,
      status: status.value,
      cursor: cursor.value
    });
    list.value = result.list || [];
    nextCursor.value = result.nextCursor
  } finally {
    loading.value = false
  }
}

function search() {
  cursor.value = undefined;
  nextCursor.value = undefined;
  history.value = [];
  page.value = 1;
  load()
}

function next() {
  if (!nextCursor.value) return;
  history.value.push(cursor.value || '');
  cursor.value = nextCursor.value;
  page.value++;
  load()
}

function previous() {
  const previous = history.value.pop();
  if (previous === undefined) return;
  cursor.value = previous || undefined;
  page.value--;
  load()
}

async function changeStatus(user: AdminUser, enabled: boolean) {
  user.status = enabled ? 1 : 0;
  await updateUserStatus(user.id, user.status);
  ElMessage.success('状态已更新')
}

async function showDetail(user: AdminUser) {
  selected.value = await getUserById(user.id);
  detailVisible.value = true
}
onMounted(load)
</script>
<template>
  <div class="user-list-page">
    <div class="page-header"><h2>用户管理</h2></div>
    <div class="filter-bar">
      <el-input v-model="keyword" clearable placeholder="用户名、昵称、手机或邮箱" style="width:280px"
                @keyup.enter="search"/>
      <el-select v-model="status" clearable placeholder="用户状态" style="width:130px">
        <el-option :value="1" label="正常"/>
        <el-option :value="0" label="已禁用"/>
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div class="table-card">
      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="ID" prop="id" width="80"/>
        <el-table-column label="用户名" prop="username" width="150"/>
        <el-table-column label="昵称" prop="nickname" width="150"/>
        <el-table-column label="手机号" prop="phone" width="150"/>
        <el-table-column label="邮箱" min-width="190" prop="email"/>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-switch :model-value="row.status === 1" @change="(value:boolean)=>changeStatus(row,value)"/>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" prop="createTime" width="180"/>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading&&!list.length" description="暂无用户"/>
      <div class="pagination"><span>第 {{ page }} 页</span>
        <el-button :disabled="loading||!history.length" @click="previous">上一页</el-button>
        <el-button :disabled="loading||!nextCursor" type="primary" @click="next">下一页</el-button>
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="用户详情" width="520px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="用户ID">{{ selected.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selected.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ selected.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ selected.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ selected.email || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>
<style lang="scss" scoped>.user-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px
}

.page-header h2 {
  margin: 0
}

.filter-bar, .pagination {
  display: flex;
  gap: 12px;
  align-items: center
}

.table-card {
  padding: 16px;
  background: #fff
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px
}</style>

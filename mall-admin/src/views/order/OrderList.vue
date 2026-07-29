<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {getOrderCursorPage} from '@/api/order'
import type {OrderVO} from '@/types'

const router = useRouter()
const orders = ref<OrderVO[]>([])
const loading = ref(false)
const pageSize = ref(20)
const orderStatus = ref<number | undefined>()
const orderNo = ref('')
const cursor = ref<string>()
const nextCursor = ref<string>()
const history = ref<string[]>([])
const page = ref(1)

async function load() {
  loading.value = true
  try {
    const result = await getOrderCursorPage({
      size: pageSize.value,
      orderStatus: orderStatus.value,
      orderNo: orderNo.value || undefined,
      cursor: cursor.value
    })
    orders.value = result.list || []
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

onMounted(load)
</script>

<template>
  <div class="order-list-page">
    <div class="page-header"><h2>订单管理</h2></div>
    <div class="filter-bar">
      <el-input v-model="orderNo" clearable placeholder="订单号" style="width: 240px" @keyup.enter="search"/>
      <el-select v-model="orderStatus" clearable placeholder="订单状态" style="width: 140px">
        <el-option :value="0" label="待付款"/>
        <el-option :value="1" label="待发货"/>
        <el-option :value="2" label="待收货"/>
        <el-option :value="3" label="已完成"/>
        <el-option :value="4" label="已取消"/>
        <el-option :value="5" label="退款中"/>
        <el-option :value="6" label="已退款"/>
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column label="订单号" prop="orderNo" width="200"/>
      <el-table-column label="用户" prop="username" width="120"/>
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="orderStatusText" width="100"/>
      <el-table-column label="创建时间" prop="createTime" width="180"/>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link size="small" type="primary"
                     @click="router.push({ name: 'OrderDetail', params: { id: row.id } })">详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination"><span>第 {{ page }} 页</span>
      <el-button :disabled="loading || !history.length" @click="previous">上一页</el-button>
      <el-button :disabled="loading || !nextCursor" type="primary" @click="next">下一页</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>.order-list-page {
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

.pagination {
  justify-content: flex-end
}</style>

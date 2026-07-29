<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderPage } from '@/api/order'
import type { OrderVO } from '@/types'

const router = useRouter()
const orders = ref<OrderVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const orderStatus = ref<number | ''>('')
const keyword = ref('')
const loading = ref(false)

async function loadOrders() {
  loading.value = true
  try {
    const params: any = { current: currentPage.value, size: pageSize.value }
    if (orderStatus.value !== '') params.orderStatus = orderStatus.value
    if (keyword.value) params.keyword = keyword.value
    const res = await getOrderPage(params)
    orders.value = res.list
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadOrders)

function handleSearch() { currentPage.value = 1; loadOrders() }
function changePage(page: number) { currentPage.value = page; loadOrders() }

function goDetail(id: number) { router.push({ name: 'OrderDetail', params: { id } }) }
</script>

<template>
  <div class="order-list-page">
    <div class="page-header"><h2>订单管理</h2></div>
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索订单号/用户名" clearable style="width: 240px" @keyup.enter="handleSearch">
        <template #append><el-button @click="handleSearch">搜索</el-button></template>
      </el-input>
      <el-select v-model="orderStatus" placeholder="订单状态" clearable style="width: 140px" @change="handleSearch">
        <el-option :value="0" label="待付款" />
        <el-option :value="1" label="待发货" />
        <el-option :value="2" label="待收货" />
        <el-option :value="3" label="已完成" />
        <el-option :value="4" label="已取消" />
        <el-option :value="5" label="退款中" />
        <el-option :value="6" label="已退款" />
      </el-select>
    </div>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="200" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="orderStatusText" label="状态" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="goDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="changePage" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.order-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header h2 { margin: 0; }
.filter-bar { display: flex; gap: 16px; }
.pagination { display: flex; justify-content: flex-end; }
</style>

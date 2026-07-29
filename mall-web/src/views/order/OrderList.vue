<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderPage, cancelOrder } from '@/api/order'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const orders = ref<OrderVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref<number | null>(null)
const loading = ref(false)

const tabs = [
  { label: '全部', value: null },
  { label: '待付款', value: 0 },
  { label: '待成团', value: 7 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 }
]

async function loadOrders() {
  loading.value = true
  try {
    const res = await getOrderPage({
      current: currentPage.value,
      size: pageSize.value,
      orderStatus: activeTab.value ?? undefined
    })
    orders.value = res.list
    total.value = res.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)

function changeTab(tab: number | null) {
  activeTab.value = tab
  currentPage.value = 1
  loadOrders()
}

function changePage(page: number) {
  currentPage.value = page
  loadOrders()
}

function handlePay(id: number) {
  router.push({ name: 'Payment', params: { id } })
}

function goRefund(id: number) {
  router.push({ name: 'RefundApply', params: { id } })
}

function goReview(id: number) {
  router.push({ name: 'ReviewCreate', params: { id } })
}

function goLogistics(id: number) {
  router.push({ name: 'Logistics', params: { id } })
}

async function handleCancel(id: number) {
  ElMessage.success('订单已取消')
  await cancelOrder(id)
  loadOrders()
}

function goDetail(id: number) {
  router.push({ name: 'OrderDetail', params: { id } })
}

function getOrderStatusClass(status: number) {
  const map: Record<number, string> = { 0: 'pending-pay', 1: 'pending-ship', 2: 'pending-receive', 3: 'completed', 4: 'cancelled', 7: 'pending-group' }
  return map[status] || ''
}
</script>

<template>
  <div class="order-list-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">我的订单</h1>
        <div class="order-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.value ?? 'all'"
            class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="changeTab(tab.value)"
          >{{ tab.label }}</div>
        </div>
        <div v-loading="loading">
          <div v-if="orders.length === 0 && !loading" class="empty-state">
            <p>暂无订单</p>
          </div>
          <div v-else class="order-list">
            <div v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-header">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="order-time">{{ order.createTime }}</span>
                <span :class="['order-status', getOrderStatusClass(order.orderStatus)]">
                  {{ order.orderStatusText }}
                </span>
              </div>
              <div class="order-body">
                <div class="order-items" @click="goDetail(order.id)">
                  <div v-for="item in order.items.slice(0, 3)" :key="item.id" class="item">
                    <img :src="item.productImage" :alt="''" class="item-img" />
                    <div class="item-info">
                      <p>{{ item.productName }}</p>
                      <span class="sku">{{ item.skuInfo }}</span>
                    </div>
                    <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
                  </div>
                  <div v-if="order.items.length > 3" class="more-items">
                    共 {{ order.items.length }} 件商品
                  </div>
                </div>
                <div class="order-summary">
                  <div class="total">
                    共{{ order.items.length }}件，实付：
                    <span class="price">¥{{ order.payAmount.toFixed(2) }}</span>
                  </div>
                  <div class="actions">
                    <el-button
                      v-if="order.orderStatus === 0"
                      type="primary"
                      size="small"
                      @click="handlePay(order.id)"
                    >去付款</el-button>
                    <el-button
                      v-if="order.orderStatus === 0"
                      size="small"
                      @click="handleCancel(order.id)"
                    >取消订单</el-button>
                    <el-button
                      v-if="order.orderStatus === 2"
                      size="small"
                      @click="goLogistics(order.id)"
                    >查看物流</el-button>
                    <el-button
                      v-if="order.orderStatus >= 1 && order.orderStatus <= 3"
                      type="warning"
                      size="small"
                      @click="goRefund(order.id)"
                    >申请退款</el-button>
                    <el-button
                      v-if="order.orderStatus === 3"
                      type="primary"
                      size="small"
                      @click="goReview(order.id)"
                    >发表评价</el-button>
                    <el-button size="small" @click="goDetail(order.id)">订单详情</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="total > pageSize" class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="changePage"
            />
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.order-list-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1000px; margin: 0 auto; padding: 0 20px; }
.page-title { font-size: 20px; margin: 0 0 16px; }

.order-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 8px;
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;

  &:hover { background: #f5f5f5; }
  &.active { background: #ff6b35; color: #fff; }
}

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;
}

.order-list { display: flex; flex-direction: column; gap: 16px; }

.order-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.order-header {
  background: #fafafa;
  padding: 12px 20px;
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #666;
  align-items: center;

  .order-status { font-weight: 500; }
  .pending-pay { color: #ff6b35; }
  .pending-ship { color: #1890ff; }
  .pending-receive { color: #722ed1; }
  .completed { color: #52c41a; }
  .cancelled { color: #999; }
}

.order-body { padding: 16px 20px; display: flex; gap: 20px; }

.order-items { flex: 1; cursor: pointer; }

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child { border-bottom: none; }
  .item-img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; }
  .item-info {
    flex: 1;
    p { margin: 0; font-size: 14px; color: #333; }
    .sku { font-size: 12px; color: #999; }
  }
  .item-price { color: #666; font-size: 14px; }
}

.more-items { text-align: center; color: #999; font-size: 13px; padding: 8px; }

.order-summary {
  width: 200px;
  border-left: 1px solid #f0f0f0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  .total { font-size: 13px; color: #666; }
  .price { color: #ff6b35; font-size: 20px; font-weight: 700; }
  .actions { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
}

.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>

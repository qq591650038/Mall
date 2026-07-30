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
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

$color-bg: #FFF9F5;
$color-bg-warm: #F5E6D3;
$color-accent: #D8A9A9;
$color-accent-dark: #C4908F;
$color-text: #3A3A3A;
$color-text-light: #6B6B6B;
$color-text-muted: #9B9B9B;
$shadow-card: 0 4px 20px rgba(212, 169, 169, 0.12);
$shadow-hover: 0 8px 32px rgba(212, 169, 169, 0.2);

.order-list-page {
  background: $color-bg;
  min-height: 100vh;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
}

.main-content {
  padding: 32px 0;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 48px;
}

.page-title {
  font-size: 32px;
  margin: 0 0 32px;
  color: $color-text;
  font-weight: 700;
  letter-spacing: 0.5px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(216, 169, 169, 0.2);
}

.order-tabs {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 12px;
  margin-bottom: 28px;
  display: flex;
  gap: 10px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 20px;
  border-radius: 14px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: $color-text-light;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  &:hover {
    background: rgba(216, 169, 169, 0.1);
    color: $color-accent-dark;
  }

  &.active {
    background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
    color: #fff;
    transform: scale(1.05);
    box-shadow: 0 4px 16px rgba(216, 169, 169, 0.3);
  }
}

.empty-state {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 80px;
  text-align: center;
  color: $color-text-muted;
  font-size: 16px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-hover;
  }
}

.order-header {
  background: linear-gradient(135deg, rgba(245, 230, 211, 0.5) 0%, rgba(255, 249, 245, 0.5) 100%);
  padding: 16px 24px;
  display: flex;
  gap: 28px;
  font-size: 14px;
  color: $color-text-light;
  align-items: center;
  border-bottom: 1px solid rgba(216, 169, 169, 0.1);

  .order-no {
    font-weight: 500;
    color: $color-text;
  }

  .order-status {
    font-weight: 600;
    font-size: 15px;
  }

  .pending-pay {
    color: $color-accent-dark;
  }

  .pending-ship {
    color: #1890ff;
  }

  .pending-receive {
    color: #722ed1;
  }

  .completed {
    color: #52c41a;
  }

  .cancelled {
    color: $color-text-muted;
  }

  .pending-group {
    color: #fa8c16;
  }
}

.order-body {
  padding: 20px 24px;
  display: flex;
  gap: 24px;
}

.order-items {
  flex: 1;
  cursor: pointer;
}

.item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed rgba(216, 169, 169, 0.2);
  transition: all 0.3s;

  &:hover {
    background: rgba(216, 169, 169, 0.05);
    border-radius: 12px;
    margin: 0 -12px;
    padding-left: 12px;
    padding-right: 12px;
  }

  &:last-child {
    border-bottom: none;
  }

  .item-img {
    width: 70px;
    height: 70px;
    border-radius: 12px;
    object-fit: cover;
    background: $color-bg-warm;
  }

  .item-info {
    flex: 1;

    p {
      margin: 0;
      font-size: 15px;
      color: $color-text;
      font-weight: 500;
    }

    .sku {
      font-size: 13px;
      color: $color-text-muted;
      margin-top: 4px;
    }
  }

  .item-price {
    color: $color-text-light;
    font-size: 15px;
    font-weight: 500;
  }
}

.more-items {
  text-align: center;
  color: $color-text-muted;
  font-size: 14px;
  padding: 12px;
  background: rgba(245, 230, 211, 0.3);
  border-radius: 12px;
  margin-top: 8px;
}

.order-summary {
  width: 220px;
  border-left: 1px solid rgba(216, 169, 169, 0.15);
  padding-left: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  .total {
    font-size: 15px;
    color: $color-text-light;
    line-height: 1.6;
  }

  .price {
    color: $color-accent-dark;
    font-size: 26px;
    font-weight: 700;
    font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  }

  .actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 16px;

    .el-button {
      border-radius: 12px;
      font-weight: 500;
      transition: all 0.3s;

      &:hover {
        transform: scale(1.05);
      }

      &:active {
        transform: scale(0.95);
      }
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 36px;

  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 10px;
      margin: 0 4px;
      transition: all 0.3s;
      font-weight: 500;

      &:hover {
        background: rgba(216, 169, 169, 0.1);
      }

      &.is-active {
        background: $color-accent;
      }
    }
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 24px;
  }

  .page-title {
    font-size: 26px;
  }

  .order-tabs {
    overflow-x: auto;
    gap: 8px;
    padding: 8px;

    .tab-item {
      flex: none;
      padding: 12px 16px;
      white-space: nowrap;
    }
  }

  .order-body {
    flex-direction: column;
    gap: 16px;
  }

  .order-summary {
    width: 100%;
    border-left: none;
    padding-left: 0;
    border-top: 1px solid rgba(216, 169, 169, 0.15);
    padding-top: 16px;

    .actions {
      flex-direction: row;
      flex-wrap: wrap;

      .el-button {
        flex: 1;
        min-width: 120px;
      }
    }
  }
}
</style>

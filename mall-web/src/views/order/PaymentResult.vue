<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderById } from '@/api/order'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.id))
const order = ref<OrderVO | null>(null)
const loading = ref(true)
const resultTimer = ref<ReturnType<typeof setInterval> | null>(null)
const pollCount = ref(0)
const MAX_POLL = 30

const isSuccess = computed(() => [1, 2, 3, 7].includes(order.value?.orderStatus ?? -1))
const isGroupPending = computed(() => order.value?.orderStatus === 7)

onMounted(() => {
  pollOrderStatus()
})

onUnmounted(() => {
  if (resultTimer.value) {
    clearInterval(resultTimer.value)
  }
})

async function pollOrderStatus() {
  try {
    order.value = await getOrderById(orderId.value)
    loading.value = false
    if (order.value.orderStatus !== 0) {
      return
    }
  } catch {
    loading.value = false
    ElMessage.error('获取订单状态失败')
    return
  }

  pollCount.value++
  if (pollCount.value >= MAX_POLL) {
    ElMessage.warning('支付结果确认超时，请查看订单列表')
    return
  }

  resultTimer.value = setInterval(async () => {
    try {
      const updated = await getOrderById(orderId.value)
      if (updated.orderStatus !== 0) {
        order.value = updated
        if (resultTimer.value) clearInterval(resultTimer.value)
      }
    } catch {
      if (resultTimer.value) clearInterval(resultTimer.value)
    }
  }, 2000)
}

function goDetail() {
  router.push({ name: 'OrderDetail', params: { id: orderId.value } })
}

function goList() {
  router.push({ name: 'OrderList' })
}

function retryPay() {
  router.push({ name: 'Payment', params: { id: orderId.value } })
}
</script>

<template>
  <div class="payment-result-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <template v-if="order && !loading">
          <div class="result-card" v-if="order.orderStatus !== 0">
            <template v-if="isSuccess">
              <div class="result-icon success">✓</div>
              <h1>{{ isGroupPending ? '支付成功，待成团' : '支付成功' }}</h1>
              <p class="result-desc">{{ isGroupPending ? '达到成团人数后，订单将进入待发货状态' : '感谢您的购买，订单已支付成功' }}</p>
              <div class="order-info">
                <p>订单编号：{{ order.orderNo }}</p>
                <p>支付金额：<span class="price">¥{{ order.payAmount.toFixed(2) }}</span></p>
                <p>支付时间：{{ order.payTime }}</p>
              </div>
            </template>
            <template v-else>
              <div class="result-icon failed">✗</div>
              <h1>支付失败</h1>
              <p class="result-desc">支付过程中出现问题，请重试</p>
              <div class="order-info">
                <p>订单编号：{{ order.orderNo }}</p>
                <p class="price">¥{{ order.payAmount.toFixed(2) }}</p>
              </div>
            </template>
            <div class="result-actions">
              <el-button type="primary" size="large" @click="goDetail">查看订单详情</el-button>
              <el-button size="large" @click="goList">返回订单列表</el-button>
            </div>
          </div>
          <div class="result-card pending" v-else>
            <div class="result-icon pending">⏳</div>
            <h1>支付确认中</h1>
            <p class="result-desc">正在确认支付结果，请稍候...</p>
            <div class="result-actions">
              <el-button type="primary" size="large" @click="retryPay">返回重新支付</el-button>
              <el-button size="large" @click="goList">返回订单列表</el-button>
            </div>
          </div>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.payment-result-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 40px 0; }
.container { max-width: 600px; margin: 0 auto; padding: 0 20px; }

.result-card {
  background: #fff;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .result-icon {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40px;
    color: #fff;
    margin: 0 auto 20px;

    &.success { background: #52c41a; }
    &.failed { background: #ff4d4f; }
    &.pending { background: #faad14; }
  }

  h1 { margin: 0 0 12px; font-size: 24px; color: #333; }
  .result-desc { color: #666; margin-bottom: 30px; }

  .order-info {
    background: #f9f9f9;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 30px;
    text-align: left;

    p { margin: 8px 0; color: #666; font-size: 14px; }
    .price { color: #C4908F; font-size: 20px; font-weight: 700; }
  }

  .result-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
  }
}
</style>

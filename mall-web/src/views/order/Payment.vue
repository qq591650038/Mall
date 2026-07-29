<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { payOrder, getOrderById, cancelOrder } from '@/api/order'
import { confirmMockPayment } from '@/api/order'
import type { OrderVO, PayResultVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => Number(route.params.id))
const order = ref<OrderVO | null>(null)
const payResult = ref<PayResultVO | null>(null)
const countdown = ref(0)
const paying = ref(false)
const selectedMethod = ref<number>(1)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)
const countdownTimer = ref<ReturnType<typeof setInterval> | null>(null)

const countdownText = computed(() => {
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(async () => {
  try {
    order.value = await getOrderById(orderId.value)
    if (order.value && order.value.orderStatus !== 0) {
      ElMessage.info('订单已处理')
      router.push({ name: 'OrderDetail', params: { id: orderId.value } })
      return
    }
    if (order.value?.expireTime) {
      const expireMs = new Date(order.value.expireTime.replace(/-/g, '/').replace('T', ' ')).getTime()
      const remaining = Math.max(0, Math.floor((expireMs - Date.now()) / 1000))
      countdown.value = remaining
    } else {
      countdown.value = 1800
    }
    startCountdown()
  } catch {
    ElMessage.error('获取订单信息失败')
    router.push({ name: 'OrderList' })
  }
})

onUnmounted(() => {
  stopCountdown()
  stopPolling()
})

function startCountdown() {
  countdownTimer.value = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      handleTimeout()
    }
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
}

function handleTimeout() {
  stopCountdown()
  ElMessageBox.confirm('支付时间已超时，是否取消订单？', '提示', {
    confirmButtonText: '取消订单',
    cancelButtonText: '重新获取',
    type: 'warning'
  }).then(async () => {
    try {
      await cancelOrder(orderId.value)
      ElMessage.success('订单已取消')
      router.push({ name: 'OrderList' })
    } catch {
      router.push({ name: 'OrderList' })
    }
  }).catch(async () => {
    try {
      order.value = await getOrderById(orderId.value)
      if (order.value?.expireTime) {
        const expireMs = new Date(order.value.expireTime.replace(/-/g, '/').replace('T', ' ')).getTime()
        countdown.value = Math.max(0, Math.floor((expireMs - Date.now()) / 1000))
      } else {
        countdown.value = 1800
      }
      startCountdown()
    } catch {
      countdown.value = 1800
      startCountdown()
    }
  })
}

async function handlePay() {
  if (paying.value) return
  paying.value = true
  try {
    payResult.value = await payOrder(orderId.value)
    stopCountdown()
    startPolling()
  } catch {
    paying.value = false
  }
}

function startPolling() {
  let attempts = 0
  const maxAttempts = 60
  pollTimer.value = setInterval(async () => {
    attempts++
    try {
      const updated = await getOrderById(orderId.value)
      if (updated.orderStatus !== 0) {
        stopPolling()
        handlePaySuccess()
        return
      }
    } catch {
      stopPolling()
      handlePayFail()
      return
    }
    if (attempts >= maxAttempts) {
      stopPolling()
      handlePayFail()
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

function handlePaySuccess() {
  ElMessage.success('支付成功')
  setTimeout(() => {
    router.push({ name: 'PaymentResult', params: { id: orderId.value } })
  }, 1500)
}

function handlePayFail() {
  ElMessageBox.confirm(
    '支付结果不确定，请前往订单列表查看最新状态。',
    '支付提醒',
    {
      confirmButtonText: '查看订单',
      cancelButtonText: '继续等待',
      type: 'warning'
    }
  ).then(() => {
    router.push({ name: 'OrderDetail', params: { id: orderId.value } })
  }).catch(() => {
    startPolling()
  })
}

async function simulatePaySuccess() {
  stopPolling()
  stopCountdown()
  if (payResult.value) {
    /* Payment confirmation is performed by the authenticated mock endpoint. */
    const amount = String(payResult.value.amount)
    const timestamp = String(Math.floor(Date.now() / 1000))
    const payload = `${payResult.value.paymentNo}|${payResult.value.orderNo}|${amount}|${timestamp}`
    const callbackData = {
      paymentNo: payResult.value.paymentNo,
      orderNo: payResult.value.orderNo,
      amount,
      timestamp,
      signature: ''
    }
    try {
      await confirmMockPayment(orderId.value, payResult.value.paymentNo)
      const response = { ok: true }
      /*
      const response = await fetch('/api/orders/payment/callback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(callbackData)
      }) */
      if (!response.ok) {
        throw new Error('支付回调处理失败')
      }
      ElMessage.success('支付成功')
      setTimeout(() => {
        router.push({ name: 'PaymentResult', params: { id: orderId.value } })
      }, 1500)
    } catch {
      ElMessage.error('支付回调失败，请稍后重试')
      startPolling()
    }
  }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '提示')
    await cancelOrder(orderId.value)
    ElMessage.success('订单已取消')
    router.push({ name: 'OrderList' })
  } catch { /* cancelled */ }
}
</script>

<template>
  <div class="payment-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-if="order">
        <div class="page-header">
          <h1>收银台</h1>
        </div>

        <div class="payment-layout">
          <div class="payment-card">
            <div class="payment-amount">
              <span class="currency">¥</span>
              <span class="amount">{{ order.payAmount.toFixed(2) }}</span>
            </div>
            <div class="payment-info">
              <p>订单编号：{{ order.orderNo }}</p>
              <p>商品数量：{{ order.items?.length || 0 }} 件</p>
              <p>下单时间：{{ order.createTime }}</p>
            </div>
          </div>

          <div class="countdown-card" v-if="!payResult">
            <div class="countdown-icon">⏰</div>
            <h3>请在以下时间内完成支付</h3>
            <div class="countdown-time">{{ countdownText }}</div>
            <p class="countdown-tip">超时后订单将自动取消</p>
          </div>

          <div class="payment-methods" v-if="!payResult">
            <h3>选择支付方式</h3>
            <div class="method-list">
              <div class="method-item" :class="{ active: selectedMethod === 1 }" @click="selectedMethod = 1">
                <span class="method-icon">💚</span>
                <span class="method-name">微信支付</span>
                <el-radio v-model="selectedMethod" :label="1" />
              </div>
              <div class="method-item" :class="{ active: selectedMethod === 0 }" @click="selectedMethod = 0">
                <span class="method-icon">💙</span>
                <span class="method-name">支付宝</span>
                <el-radio v-model="selectedMethod" :label="0" />
              </div>
              <div class="method-item" :class="{ active: selectedMethod === 2 }" @click="selectedMethod = 2">
                <span class="method-icon">💳</span>
                <span class="method-name">银行卡</span>
                <el-radio v-model="selectedMethod" :label="2" />
              </div>
            </div>
          </div>

          <div class="payment-processing" v-if="payResult">
            <div class="processing-icon">
              <el-icon :size="48" class="is-loading"><Loading /></el-icon>
            </div>
            <h3>正在处理支付...</h3>
            <p>支付单号：{{ payResult.paymentNo }}</p>
            <p class="processing-tip">请在新窗口中完成支付，页面将自动更新</p>
            <el-button type="success" @click="simulatePaySuccess">模拟支付成功</el-button>
          </div>

          <div class="payment-actions" v-if="!payResult">
            <el-button
              type="primary"
              size="large"
              class="pay-btn"
              :loading="paying"
              @click="handlePay"
            >
              立即支付 ¥{{ order.payAmount.toFixed(2) }}
            </el-button>
            <el-button size="large" @click="handleCancel">取消订单</el-button>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.payment-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 800px; margin: 0 auto; padding: 0 20px; }
.page-header { margin-bottom: 24px; h1 { font-size: 20px; margin: 0; } }

.payment-layout { display: flex; flex-direction: column; gap: 20px; }

.payment-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  text-align: center;

  .payment-amount { margin-bottom: 20px; }
  .currency { font-size: 24px; color: #ff6b35; }
  .amount { font-size: 48px; font-weight: 700; color: #ff6b35; }
  .payment-info { color: #999; font-size: 14px; p { margin: 4px 0; } }
}

.countdown-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  border: 2px solid #ff6b35;

  .countdown-icon { font-size: 32px; margin-bottom: 8px; }
  h3 { margin: 0 0 12px; color: #333; }
  .countdown-time {
    font-size: 36px;
    font-weight: 700;
    color: #ff6b35;
    font-family: monospace;
    letter-spacing: 4px;
  }
  .countdown-tip { color: #999; margin: 8px 0 0; }
}

.payment-methods {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  h3 { margin: 0 0 16px; }
  .method-list { display: flex; flex-direction: column; gap: 12px; }
  .method-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    border: 2px solid #eee;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &.active { border-color: #ff6b35; background: #fffaf7; }
    .method-icon { font-size: 24px; }
    .method-name { flex: 1; font-size: 16px; }
  }
}

.payment-processing {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  text-align: center;

  .processing-icon { margin-bottom: 16px; color: #ff6b35; }
  h3 { margin: 0 0 12px; }
  p { color: #666; margin: 4px 0; }
  .processing-tip { color: #999; margin-bottom: 16px; }
}

.payment-actions {
  display: flex;
  gap: 16px;
  justify-content: center;

  .pay-btn {
    min-width: 300px;
    background: #ff6b35;
    border-color: #ff6b35;
    &:hover { background: #ff5722; border-color: #ff5722; }
  }
}
</style>

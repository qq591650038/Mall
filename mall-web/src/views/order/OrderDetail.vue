<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderById, cancelOrder, confirmReceive } from '@/api/order'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const order = ref<OrderVO | null>(null)
const loading = ref(false)
const orderId = computed(() => Number(route.params.id))

async function loadOrder() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    order.value = await getOrderById(id)
  } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadOrder)

function goPayment() {
  router.push({ name: 'Payment', params: { id: orderId.value } })
}

function goRefund() {
  router.push({ name: 'RefundApply', params: { id: orderId.value } })
}

function goReview() {
  router.push({ name: 'ReviewCreate', params: { id: orderId.value } })
}

function goLogistics() {
  router.push({ name: 'Logistics', params: { id: orderId.value } })
}

function getStatusIcon(status: number) {
  const icons: Record<number, string> = { 0: '💳', 1: '✓', 2: '📦', 3: '✓', 4: '✗', 5: '↻', 6: '✓', 7: '◷' }
  return icons[status] || '•'
}

async function handleCancel() {
  if (!order.value) return
  await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
  loadOrder()
}

async function handleConfirm() {
  if (!order.value) return
  await confirmReceive(order.value.id)
  ElMessage.success('已确认收货')
  loadOrder()
}

function getStatusText(status: number) {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款', 7: '待成团' }
  return map[status] || '未知'
}
</script>

<template>
  <div class="order-detail-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <template v-if="order">
          <div class="page-header">
            <el-page-header @back="router.back()" :content="`订单详情 - ${order.orderNo}`" />
          </div>
          <div class="status-card">
            <div class="status-info">
              <h2>{{ getStatusIcon(order.orderStatus) }} {{ getStatusText(order.orderStatus) }}</h2>
              <p>{{ order.orderStatusText }}</p>
            </div>
            <div class="status-actions" v-if="order.orderStatus === 0">
              <el-button type="primary" size="large" @click="goPayment">立即付款</el-button>
              <el-button size="large" @click="handleCancel">取消订单</el-button>
            </div>
            <div class="status-actions" v-if="order.orderStatus === 1">
              <el-button type="warning" size="large" @click="goRefund">申请退款</el-button>
            </div>
            <div class="status-actions" v-if="order.orderStatus === 2">
              <el-button type="primary" size="large" @click="handleConfirm">确认收货</el-button>
              <el-button size="large" @click="goLogistics">查看物流</el-button>
              <el-button type="warning" size="large" @click="goRefund">申请退款</el-button>
            </div>
            <div class="status-actions" v-if="order.orderStatus === 3">
              <el-button type="primary" size="large" @click="goReview">发表评价</el-button>
              <el-button v-if="order.logisticsNo" size="large" @click="goLogistics">查看物流</el-button>
              <el-button type="warning" size="large" @click="goRefund">申请退款</el-button>
            </div>
          </div>

          <div class="info-card" v-if="order.orderStatus >= 1 && order.timeline && order.timeline.length">
            <h3>订单进度</h3>
            <el-timeline>
              <el-timeline-item
                v-for="(item, idx) in order.timeline"
                :key="idx"
                :timestamp="item.time"
                :color="idx === 0 ? '#ff6b35' : '#e4e7ed'"
                :type="idx === 0 ? 'primary' : ''"
              >
                <div class="timeline-content">
                  <span class="timeline-status">{{ item.statusText }}</span>
                  <span v-if="item.description" class="timeline-desc">{{ item.description }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div class="info-card" v-if="order.orderStatus === 2 && order.logisticsNo">
            <h3>物流信息</h3>
            <div class="logistics-info">
              <p><strong>物流公司：</strong>{{ order.logisticsCompany || '-' }}</p>
              <p><strong>运单号：</strong>{{ order.logisticsNo }}</p>
              <p class="logistics-tip">商品已发出，请注意查收</p>
            </div>
          </div>

          <div class="info-card auto-confirm-warning" v-if="order.orderStatus === 2 && order.autoConfirmDeadline">
            <el-alert type="warning" :closable="false" show-icon>
              <template #title>
                系统将在 <strong>{{ order.autoConfirmDeadline }}</strong> 后自动确认收货
              </template>
            </el-alert>
          </div>
          <div class="info-card" v-if="order.addressSnapshot">
            <h3>收货信息</h3>
            <div class="address-info">
              <span>{{ order.addressSnapshot.receiverName }}</span>
              <span>{{ order.addressSnapshot.receiverPhone }}</span>
              <span>{{ order.addressSnapshot.province }}{{ order.addressSnapshot.city }}{{ order.addressSnapshot.district }}{{ order.addressSnapshot.detailAddress }}</span>
            </div>
          </div>
          <div class="info-card">
            <h3>商品信息</h3>
            <div class="item-list">
              <div v-for="item in order.items" :key="item.id" class="item-row">
                <img :src="item.productImage" :alt="''" class="item-img" />
                <div class="item-info">
                  <p class="name">{{ item.productName }}</p>
                  <p class="sku">{{ item.skuInfo }}</p>
                </div>
                <div class="item-price">¥{{ item.price.toFixed(2) }}</div>
                <div class="item-qty">×{{ item.quantity }}</div>
                <div class="item-subtotal">¥{{ item.subtotal.toFixed(2) }}</div>
              </div>
            </div>
          </div>
          <div class="info-card">
            <h3>费用明细</h3>
            <div class="fee-list">
              <div class="fee-row">
                <span>商品总额</span>
                <span>¥{{ order.totalAmount.toFixed(2) }}</span>
              </div>
              <div class="fee-row" v-if="order.discountAmount > 0">
                <span>优惠金额</span>
                <span class="discount">-¥{{ order.discountAmount.toFixed(2) }}</span>
              </div>
              <div class="fee-row">
                <span>运费</span>
                <span>¥{{ order.freightAmount.toFixed(2) }}</span>
              </div>
              <div class="fee-row total">
                <span>实付金额</span>
                <span class="price">¥{{ order.payAmount.toFixed(2) }}</span>
              </div>
            </div>
          </div>
          <div class="info-card">
            <h3>订单信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="收货时间">{{ order.receiveTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.order-detail-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1000px; margin: 0 auto; padding: 0 20px; }
.page-header { margin-bottom: 16px; }

.status-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .status-info h2 { margin: 0 0 8px; font-size: 20px; color: #333; }
  .status-info p { margin: 0; color: #999; font-size: 14px; }
  .status-actions { display: flex; gap: 12px; flex-wrap: wrap; }
}

.timeline-content {
  .timeline-status { font-weight: 500; color: #333; }
  .timeline-desc { margin-left: 8px; color: #999; font-size: 12px; }
}

.logistics-info p { margin: 4px 0; color: #666; }
.logistics-info .logistics-tip { color: #ff6b35; margin-top: 8px; }

.auto-confirm-warning { padding: 12px 24px; }

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-top: 16px;

  h3 { margin: 0 0 16px; font-size: 16px; color: #333; }
}

.address-info {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 14px;
}

.item-list { display: flex; flex-direction: column; }
.item-row {
  display: grid;
  grid-template-columns: 60px 2fr 100px 60px 100px;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child { border-bottom: none; }
  .item-img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; }
  .item-info p { margin: 0; }
  .item-info .name { font-size: 14px; color: #333; }
  .item-info .sku { font-size: 12px; color: #999; }
  .item-price, .item-qty { text-align: center; color: #666; }
  .item-subtotal { text-align: right; color: #ff6b35; font-weight: 600; }
}

.fee-list { max-width: 400px; }
.fee-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;

  &.total { border-top: 2px solid #f0f0f0; border-bottom: none; padding-top: 16px; }
  &.discount .discount { color: #52c41a; }
  .price { color: #ff6b35; font-size: 20px; font-weight: 700; }
}
</style>

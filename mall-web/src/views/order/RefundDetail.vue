<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmReturnReceived, getRefundById, submitReturnLogistics } from '@/api/refund'
import type { RefundVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const refund = ref<RefundVO | null>(null)
const loading = ref(true)
const logisticsDialog = ref(false)
const submitting = ref(false)
const logistics = ref({ logisticsCompany: '', logisticsNo: '' })
const refundId = computed(() => Number(route.params.id))

const statusMap: Record<number, string> = {
  0: '待审核', 1: '审核通过', 2: '退款中', 3: '已退款', 4: '已拒绝',
  5: '退货中', 6: '换货配送中', 7: '退款失败'
}

const typeText = computed(() => refund.value?.type === 1 ? '退货退款' : '仅退款')
const canSubmitLogistics = computed(() => refund.value?.type === 1 && refund.value.status === 1)
const canConfirm = computed(() => refund.value?.type === 1 && [1, 5, 6].includes(refund.value.status))

async function loadDetail() {
  loading.value = true
  try {
    refund.value = await getRefundById(refundId.value)
    logistics.value = {
      logisticsCompany: refund.value.logisticsCompany || '',
      logisticsNo: refund.value.logisticsNo || ''
    }
  } finally {
    loading.value = false
  }
}

async function handleLogistics() {
  if (!logistics.value.logisticsCompany.trim() || !logistics.value.logisticsNo.trim()) {
    ElMessage.warning('请填写物流公司和物流单号')
    return
  }
  submitting.value = true
  try {
    await submitReturnLogistics(refundId.value, logistics.value)
    ElMessage.success('退货物流已提交')
    logisticsDialog.value = false
    await loadDetail()
  } finally {
    submitting.value = false
  }
}

async function handleConfirm() {
  try {
    await ElMessageBox.confirm('确认售后流程已经完成吗？', '确认完成', { type: 'warning' })
    await confirmReturnReceived(refundId.value)
    ElMessage.success('售后已完成')
    await loadDetail()
  } catch { /* cancelled or handled by request interceptor */ }
}

onMounted(loadDetail)
</script>

<template>
  <div class="refund-detail-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <el-page-header content="售后详情" @back="router.back()" />
        <template v-if="refund">
          <section class="detail-card summary">
            <div class="summary-head">
              <div>
                <h1>{{ typeText }}</h1>
                <p>售后单号：{{ refund.refundNo }}</p>
              </div>
              <el-tag :type="refund.status === 3 ? 'success' : refund.status === 4 || refund.status === 7 ? 'danger' : 'warning'">
                {{ statusMap[refund.status] || refund.statusText || '未知状态' }}
              </el-tag>
            </div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="关联订单">{{ refund.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="申请时间">{{ refund.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="退款金额">¥{{ Number(refund.amount || 0).toFixed(2) }}</el-descriptions-item>
              <el-descriptions-item label="退款原因">{{ refund.reason || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="refund.reviewRemark" label="审核备注" :span="2">{{ refund.reviewRemark }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section v-if="refund.type === 1" class="detail-card">
            <h2>退货物流</h2>
            <div class="logistics-info">
              <p>物流公司：{{ refund.logisticsCompany || '尚未提交' }}</p>
              <p>物流单号：{{ refund.logisticsNo || '尚未提交' }}</p>
              <p v-if="refund.returnAddress">退货地址：{{ refund.returnAddress }}</p>
            </div>
            <div class="actions">
              <el-button v-if="canSubmitLogistics" type="primary" @click="logisticsDialog = true">提交退货物流</el-button>
              <el-button v-if="canConfirm" type="success" @click="handleConfirm">确认售后完成</el-button>
            </div>
          </section>

          <section v-if="refund.orderInfo" class="detail-card">
            <h2>关联订单</h2>
            <p>{{ refund.orderInfo.orderNo }} · {{ refund.orderInfo.items?.length || 0 }} 件商品</p>
            <el-button link type="primary" @click="router.push({ name: 'OrderDetail', params: { id: refund.orderId } })">查看订单</el-button>
          </section>
        </template>
      </div>
    </main>
    <el-dialog v-model="logisticsDialog" title="提交退货物流" width="420px">
      <el-form label-width="90px">
        <el-form-item label="物流公司"><el-input v-model="logistics.logisticsCompany" placeholder="例如：顺丰速运" /></el-form-item>
        <el-form-item label="物流单号"><el-input v-model="logistics.logisticsNo" placeholder="请输入物流单号" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logisticsDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleLogistics">提交</el-button>
      </template>
    </el-dialog>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.refund-detail-page { min-height: 100vh; background: #f5f6f8; }
.main-content { padding: 24px 0 48px; }
.container { max-width: 900px; margin: 0 auto; padding: 0 20px; }
.detail-card { background: #fff; border-radius: 8px; padding: 24px; margin-top: 18px; }
.summary-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
h1 { margin: 0 0 8px; font-size: 20px; }
h2 { margin: 0 0 16px; font-size: 16px; }
p { color: #666; margin: 8px 0; }
.logistics-info { margin-bottom: 16px; }
.actions { display: flex; gap: 12px; }
@media (max-width: 600px) { .detail-card { padding: 16px; } .summary-head { gap: 12px; } }
</style>

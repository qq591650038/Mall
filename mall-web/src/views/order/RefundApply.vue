<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderById } from '@/api/order'
import { applyRefund } from '@/api/refund'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.id))
const order = ref<OrderVO | null>(null)
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  amount: 0,
  type: 0,
  reason: '',
  images: ''
})

const refundReasons = [
  '商品质量问题',
  '商品与描述不符',
  '发错货',
  '商品损坏',
  '不想要了',
  '其他原因'
]

const refundableAmount = computed(() => {
  const amount = Number(order.value?.payAmount)
  return Number.isFinite(amount) && amount > 0 ? amount : 0
})

onMounted(async () => {
  try {
    order.value = await getOrderById(orderId.value)
    form.value.amount = refundableAmount.value
  } catch {
    ElMessage.error('获取订单信息失败')
    router.push({ name: 'OrderList' })
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!form.value.reason) {
    ElMessage.warning('请选择退款原因')
    return
  }
  if (refundableAmount.value <= 0) {
    ElMessage.warning('该订单没有可退金额')
    return
  }
  if (!form.value.amount || form.value.amount <= 0) {
    ElMessage.warning('请输入退款金额')
    return
  }
  if (form.value.amount > refundableAmount.value) {
    ElMessage.warning('退款金额不能超过实付金额')
    return
  }

  try {
    await ElMessageBox.confirm('确认提交退款申请？', '提示')
    submitting.value = true
    await applyRefund(orderId.value, {
      amount: form.value.amount,
      type: form.value.type,
      reason: form.value.reason,
      images: form.value.images
    })
    ElMessage.success('退款申请已提交')
    router.push({ name: 'RefundList' })
  } catch {
    if (!submitting.value) return
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="refund-apply-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <template v-if="order">
          <div class="page-header">
            <el-page-header @back="goBack" content="申请退款" />
          </div>

          <div class="order-summary">
            <h3>订单信息</h3>
            <div class="summary-row">
              <span>订单编号：{{ order.orderNo }}</span>
              <span>商品数量：{{ order.items?.length || 0 }} 件</span>
              <span class="pay-amount">实付金额：¥{{ order.payAmount.toFixed(2) }}</span>
            </div>
            <div class="items-preview">
              <div v-for="item in order.items?.slice(0, 3)" :key="item.id" class="item">
                <img :src="item.productImage" :alt="''" />
                <span>{{ item.productName }}</span>
                <span class="price">¥{{ item.price.toFixed(2) }}</span>
              </div>
            </div>
          </div>

          <el-form :model="form" label-width="100px" class="refund-form">
            <el-form-item label="售后类型" required>
              <el-radio-group v-model="form.type">
                <el-radio :value="0">仅退款</el-radio>
                <el-radio :value="1">退货退款</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="退款原因" required>
              <el-select v-model="form.reason" placeholder="请选择退款原因" style="width: 100%">
                <el-option v-for="r in refundReasons" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="refundableAmount > 0" label="退款金额" required>
              <el-input-number
                v-model="form.amount"
                :min="0.01"
                :max="refundableAmount"
                :precision="2"
                :step="10"
              />
              <span class="form-tip">最多可退 ¥{{ refundableAmount.toFixed(2) }}</span>
            </el-form-item>
            <el-alert
              v-else
              title="该订单没有可退金额，暂不能提交退款申请"
              type="warning"
              :closable="false"
              show-icon
              class="refund-unavailable"
            />

            <el-form-item label="详细说明">
              <el-input
                v-model="form.reason"
                type="textarea"
                :rows="4"
                placeholder="请详细说明退款原因（选填）"
              />
            </el-form-item>

            <el-form-item label="凭证图片">
              <el-input v-model="form.images" placeholder="请输入图片URL，多个用逗号分隔" />
              <span class="form-tip">可上传商品问题截图等</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" :disabled="refundableAmount <= 0" @click="handleSubmit">
                提交退款申请
              </el-button>
              <el-button size="large" @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.refund-apply-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 800px; margin: 0 auto; padding: 0 20px; }
.page-header { margin-bottom: 24px; }

.order-summary {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;

  h3 { margin: 0 0 16px; font-size: 16px; }
  .summary-row {
    display: flex;
    gap: 24px;
    color: #666;
    font-size: 14px;
    margin-bottom: 12px;
    .pay-amount { color: #C4908F; font-weight: 600; }
  }
  .items-preview {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    .item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: #f9f9f9;
      border-radius: 8px;
      img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
      .price { color: #C4908F; font-size: 12px; }
    }
  }
}

.refund-form {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  .form-tip { color: #999; font-size: 12px; margin-left: 12px; }
}

.refund-unavailable { margin-bottom: 18px; }
</style>

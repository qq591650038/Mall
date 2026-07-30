<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyRefunds, cancelRefund } from '@/api/refund'
import type { RefundVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const refunds = ref<RefundVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref<number | null>(null)
const loading = ref(false)

const tabs = [
  { label: '全部', value: null },
  { label: '待处理', value: 0 },
  { label: '已同意', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '已完成', value: 3 }
]

async function loadRefunds() {
  loading.value = true
  try {
    const res = await getMyRefunds({
      current: currentPage.value,
      size: pageSize.value,
      status: activeTab.value ?? undefined
    })
    refunds.value = res.list
    total.value = res.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

onMounted(loadRefunds)

function changeTab(tab: number | null) {
  activeTab.value = tab
  currentPage.value = 1
  loadRefunds()
}

function changePage(page: number) {
  currentPage.value = page
  loadRefunds()
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要撤销此退款申请吗？', '提示')
    await cancelRefund(id)
    ElMessage.success('退款申请已撤销')
    loadRefunds()
  } catch {
    // cancelled
  }
}

function getRefundStatusClass(status: number) {
  const map: Record<number, string> = {
    0: 'pending',
    1: 'approved',
    2: 'rejected',
    3: 'completed'
    ,4: 'rejected', 5: 'shipping', 6: 'shipping', 7: 'failed'
  }
  return map[status] || ''
}

function getRefundStatusText(status: number) {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已同意',
    2: '已拒绝',
    3: '已完成'
    ,4: '已拒绝', 5: '待寄回商品', 6: '换货配送中', 7: '退款失败'
  }
  return map[status] || '未知'
}

function goRefundDetail(id: number) {
  router.push({ name: 'RefundDetail', params: { id } })
}
</script>

<template>
  <div class="refund-list-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">我的退款</h1>
        <div class="refund-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.value ?? 'all'"
            class="tab-item"
            :class="{ active: activeTab === tab.value }"
            @click="changeTab(tab.value)"
          >{{ tab.label }}</div>
        </div>
        <div v-loading="loading">
          <div v-if="refunds.length === 0 && !loading" class="empty-state">
            <p>暂无退款记录</p>
          </div>
          <div v-else class="refund-list">
            <div v-for="refund in refunds" :key="refund.id" class="refund-card">
              <div class="refund-header">
                <span class="refund-no">退款单号：{{ refund.refundNo }}</span>
                <span class="refund-time">{{ refund.createTime }}</span>
                <span :class="['refund-status', getRefundStatusClass(refund.status)]">
                  {{ getRefundStatusText(refund.status) }}
                </span>
              </div>
              <div class="refund-body" @click="goRefundDetail(refund.id)">
                <div class="refund-info">
                  <p class="order-no">关联订单：{{ refund.orderNo }}</p>
                  <p class="reason">退款原因：{{ refund.reason }}</p>
                  <div class="order-preview" v-if="refund.orderInfo?.items?.length">
                    <span v-for="item in refund.orderInfo.items.slice(0, 2)" :key="item.id" class="item-tag">
                      {{ item.productName }}
                    </span>
                  </div>
                </div>
                <div class="refund-summary">
                  <div class="amount">
                    退款金额：
                    <span class="price">¥{{ refund.amount.toFixed(2) }}</span>
                  </div>
                  <div class="actions">
                    <el-button
                      v-if="refund.status === 0"
                      type="danger"
                      size="small"
                      @click="handleCancel(refund.id)"
                    >撤销申请</el-button>
                    <el-button size="small" @click.stop="goRefundDetail(refund.id)">查看详情</el-button>
                  </div>
                </div>
              </div>
              <div class="refund-review" v-if="refund.status === 2 && refund.reviewRemark">
                <p class="review-remark">拒绝原因：{{ refund.reviewRemark }}</p>
              </div>
              <div class="refund-review" v-if="refund.status === 1 && refund.reviewTime">
                <p class="review-time">审核通过时间：{{ refund.reviewTime }}</p>
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
.refund-list-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1000px; margin: 0 auto; padding: 0 20px; }
.page-title { font-size: 20px; margin: 0 0 16px; }

.refund-tabs {
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
  &.active { background: #C4908F; color: #fff; }
}

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;
}

.refund-list { display: flex; flex-direction: column; gap: 16px; }

.refund-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.refund-header {
  background: #fafafa;
  padding: 12px 20px;
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #666;
  align-items: center;

  .refund-status { font-weight: 500; }
  .pending { color: #faad14; }
  .approved { color: #52c41a; }
  .rejected { color: #ff4d4f; }
  .completed { color: #1890ff; }
}

.refund-body { padding: 16px 20px; display: flex; gap: 20px; }

.refund-info { flex: 1; cursor: pointer; }
.refund-info .order-no { margin: 0 0 4px; color: #333; font-size: 14px; }
.refund-info .reason { margin: 0 0 8px; color: #666; font-size: 13px; }

.order-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  .item-tag {
    background: #f5f5f5;
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 12px;
    color: #666;
  }
}

.refund-summary {
  width: 200px;
  border-left: 1px solid #f0f0f0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  .amount { font-size: 13px; color: #666; }
  .price { color: #C4908F; font-size: 20px; font-weight: 700; }
  .actions { display: flex; flex-direction: column; gap: 8px; margin-top: 12px; }
}

.refund-review {
  padding: 12px 20px;
  background: #fffbe6;
  border-top: 1px solid #f0f0f0;
  .review-remark, .review-time { margin: 0; font-size: 13px; color: #666; }
}

.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>

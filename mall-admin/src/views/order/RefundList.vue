<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefundPage, reviewRefund, completeRefund } from '@/api/refund'

const loading = ref(false)
const refunds = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref<number | undefined>(undefined)
const searchOrderNo = ref('')

const reviewDialogVisible = ref(false)
const currentRefund = ref<any>(null)
const reviewForm = ref({ status: 1, remark: '' })

const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '审核通过', type: 'primary' },
  2: { text: '退款中', type: 'info' },
  3: { text: '已退款', type: 'success' },
  4: { text: '已拒绝', type: 'danger' },
  5: { text: '退货中', type: 'warning' },
  6: { text: '换货中', type: 'primary' },
  7: { text: '退款失败', type: 'danger' }
}

async function loadRefunds() {
  loading.value = true
  try {
    const res = await getRefundPage({
      current: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value,
      orderNo: searchOrderNo.value
    })
    refunds.value = res?.list || []
    total.value = res?.total || 0
  } catch { refunds.value = [] }
  finally { loading.value = false }
}

function openReviewDialog(refund: any, status: number) {
  currentRefund.value = refund
  reviewForm.value = { status, remark: '' }
  reviewDialogVisible.value = true
}

async function handleReview() {
  if (!currentRefund.value) return
  try {
    await ElMessageBox.confirm(
      reviewForm.value.status === 1 ? '确认审核通过？' : '确认拒绝此退款申请？',
      '提示'
    )
    await reviewRefund(currentRefund.value.id, reviewForm.value.status, reviewForm.value.remark)
    ElMessage.success('审核完成')
    reviewDialogVisible.value = false
    loadRefunds()
  } catch { /* cancelled */ }
}

async function handleComplete(refund: any) {
  try {
    await ElMessageBox.confirm('确认退款已完成？', '提示')
    await completeRefund(refund.id)
    ElMessage.success('退款已完成')
    loadRefunds()
  } catch { /* cancelled */ }
}

onMounted(loadRefunds)
</script>

<template>
  <div class="refund-page">
    <div class="page-header">
      <h2>退款管理</h2>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterStatus" placeholder="退款状态" clearable style="width: 150px">
        <el-option v-for="(v, k) in statusMap" :key="k" :label="v.text" :value="Number(k)" />
      </el-select>
      <el-input v-model="searchOrderNo" placeholder="订单编号" clearable style="width: 200px" />
      <el-button type="primary" @click="loadRefunds">查询</el-button>
    </div>

    <div class="table-card">
      <el-table :data="refunds" v-loading="loading" border>
        <el-table-column prop="refundNo" label="退款单号" width="180" />
        <el-table-column prop="orderNo" label="订单编号" width="200" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="amount" label="退款金额" width="120">
          <template #default="{ row }">
            <span style="color: #ff6b35; font-weight: 600">¥{{ row.amount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="openReviewDialog(row, 1)"
            >通过</el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              size="small"
              @click="openReviewDialog(row, 4)"
            >拒绝</el-button>
            <el-button
              v-if="row.status === 2 || row.status === 5"
              type="primary"
              size="small"
              @click="handleComplete(row)"
            >完成退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadRefunds"
        @current-change="loadRefunds"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </div>

    <el-dialog v-model="reviewDialogVisible" title="审核退款申请" width="500px">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="审核结果">
          <el-tag :type="reviewForm.status === 1 ? 'success' : 'danger'">
            {{ reviewForm.status === 1 ? '通过' : '拒绝' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="reviewForm.remark" type="textarea" :rows="3" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.refund-page { padding: 20px; }
.page-header { margin-bottom: 20px; h2 { margin: 0; } }
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}
.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getAfterSalePage,
  getAfterSaleDetail,
  auditAfterSale,
  updateLogistics
} from '@/api/aftersale'
import type { RefundVO } from '@/types'

// ========== 数据加载 ==========
const loading = ref(false)
const tableData = ref<RefundVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// ========== 搜索条件 ==========
const searchKeyword = ref('')
const filterType = ref<number | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)

// ========== 售后类型映射（与后端 RefundStatus 对应） ==========
// type: 0-仅退款 1-退货 2-换货
const afterSaleTypeMap: Record<number, { text: string; type: string }> = {
  0: { text: '仅退款', type: 'info' },
  1: { text: '退货', type: 'warning' },
  2: { text: '换货', type: 'success' }
}

// ========== 售后状态映射（与后端 RefundStatus 对应） ==========
const afterSaleStatusMap: Record<number, { text: string; type: string }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '审核通过', type: 'primary' },
  2: { text: '退款中', type: 'info' },
  3: { text: '已退款', type: 'success' },
  4: { text: '已拒绝', type: 'danger' },
  5: { text: '退货中', type: 'warning' },
  6: { text: '换货中', type: 'primary' },
  7: { text: '退款失败', type: 'danger' }
}

// ========== 售后类型选项 ==========
const afterSaleTypeOptions = [
  { value: 0, label: '仅退款' },
  { value: 1, label: '退货' },
  { value: 2, label: '换货' }
]

// ========== 表格数据加载 ==========
async function loadData() {
  loading.value = true
  try {
    const res = await getAfterSalePage({
      current: currentPage.value,
      size: pageSize.value,
      type: filterType.value,
      status: filterStatus.value,
      orderNo: searchKeyword.value || undefined
    })
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function resetSearch() {
  searchKeyword.value = ''
  filterType.value = undefined
  filterStatus.value = undefined
  currentPage.value = 1
  loadData()
}

// ========== 详情对话框 ==========
const showDetailDialog = ref(false)
const detailLoading = ref(false)
const detailData = ref<RefundVO | null>(null)

// 审核表单
const showAuditDialog = ref(false)
const auditTarget = ref<RefundVO | null>(null)
const auditForm = ref<{ status: number; remark: string }>({ status: 1, remark: '' })

// 物流表单
const showLogisticsDialog = ref(false)
const logisticsTarget = ref<RefundVO | null>(null)
const logisticsForm = ref<{ logisticsCompany: string; logisticsNo: string }>({
  logisticsCompany: '',
  logisticsNo: ''
})

async function openDetail(row: RefundVO) {
  detailLoading.value = true
  showDetailDialog.value = true
  try {
    detailData.value = await getAfterSaleDetail(row.id)
  } catch {
    detailData.value = row
    ElMessage.warning('加载详情失败，显示列表数据')
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  showDetailDialog.value = false
  detailData.value = null
}

// ========== 审核操作 ==========
function openAuditDialog(row: RefundVO, status: number) {
  auditTarget.value = row
  auditForm.value = { status, remark: '' }
  showAuditDialog.value = true
}

async function handleAudit() {
  if (!auditTarget.value) return
  try {
    const actionText = auditForm.value.status === 1 ? '通过' : '拒绝'
    await ElMessageBox.confirm(
      `确定${actionText}此售后申请？`,
      '审核确认',
      { type: 'warning' }
    )
    await auditAfterSale(auditTarget.value.id, {
      status: auditForm.value.status,
      remark: auditForm.value.remark
    })
    ElMessage.success(`已${actionText}`)
    showAuditDialog.value = false
    if (showDetailDialog.value && detailData.value?.id === auditTarget.value?.id) {
      openDetail(auditTarget.value)
    }
    loadData()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 物流操作 ==========
function openLogisticsDialog(row: RefundVO) {
  logisticsTarget.value = row
  logisticsForm.value = {
    logisticsCompany: row.logisticsCompany || '',
    logisticsNo: row.logisticsNo || ''
  }
  showLogisticsDialog.value = true
}

async function handleUpdateLogistics() {
  if (!logisticsTarget.value) return
  if (!logisticsForm.value.logisticsCompany.trim()) {
    ElMessage.warning('请填写物流公司')
    return
  }
  if (!logisticsForm.value.logisticsNo.trim()) {
    ElMessage.warning('请填写物流单号')
    return
  }
  try {
    await updateLogistics(logisticsTarget.value.id, {
      logisticsCompany: logisticsForm.value.logisticsCompany,
      logisticsNo: logisticsForm.value.logisticsNo
    })
    ElMessage.success('物流信息更新成功')
    showLogisticsDialog.value = false
    if (showDetailDialog.value && detailData.value?.id === logisticsTarget.value?.id) {
      openDetail(logisticsTarget.value)
    }
    loadData()
  } catch {
    // 错误已在拦截器中处理
  }
}

// ========== 辅助方法 ==========
function getTypeLabel(type: number) {
  return afterSaleTypeMap[type]?.text || '未知'
}

function getTypeTagType(type: number) {
  return afterSaleTypeMap[type]?.type || 'info'
}

function getStatusLabel(status: number) {
  return afterSaleStatusMap[status]?.text || '未知'
}

function getStatusTagType(status: number) {
  return afterSaleStatusMap[status]?.type || 'info'
}

function formatAmount(amount: number) {
  return `¥${amount?.toFixed(2) || '0.00'}`
}

onMounted(loadData)
</script>

<template>
  <div class="aftersale-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>售后管理</h2>
    </div>

    <!-- 搜索区 -->
    <div class="search-bar">
      <el-select
        v-model="filterType"
        placeholder="售后类型"
        clearable
        style="width: 140px"
      >
        <el-option
          v-for="t in afterSaleTypeOptions"
          :key="t.value"
          :label="t.label"
          :value="t.value"
        />
      </el-select>

      <el-select
        v-model="filterStatus"
        placeholder="售后状态"
        clearable
        style="width: 140px"
      >
        <el-option
          v-for="(v, k) in afterSaleStatusMap"
          :key="k"
          :label="v.text"
          :value="Number(k)"
        />
      </el-select>

      <el-input
        v-model="searchKeyword"
        placeholder="搜索订单号"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />

      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="refundNo" label="退款单号" width="180" />

      <el-table-column prop="orderNo" label="订单号" width="180" />

      <el-table-column prop="username" label="用户" width="120">
        <template #default="{ row }">
          <span>{{ row.username || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="售后类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type ?? 0)" effect="light">
            {{ getTypeLabel(row.type ?? 0) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="申请金额" width="120">
        <template #default="{ row }">
          <span class="amount-text">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" effect="light">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="申请时间" width="160">
        <template #default="{ row }">
          <span v-if="row.createTime">{{ row.createTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="success"
            size="small"
            @click="openAuditDialog(row, 1)"
          >通过</el-button>
          <el-button
            v-if="row.status === 0"
            link
            type="danger"
            size="small"
            @click="openAuditDialog(row, 2)"
          >拒绝</el-button>
          <el-button
            v-if="row.status === 5 || row.status === 6"
            link
            type="warning"
            size="small"
            @click="openLogisticsDialog(row)"
          >物流</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无售后数据" />
      </template>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="售后详情"
      width="760px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detailData">
          <!-- 基本信息 -->
          <el-descriptions :column="2" border>
            <el-descriptions-item label="退款单号">{{ detailData.refundNo }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ detailData.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="用户">
              {{ detailData.username || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="售后类型">
              <el-tag :type="getTypeTagType(detailData.type ?? 0)">
                {{ getTypeLabel(detailData.type ?? 0) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请金额">
              <span class="amount-text">{{ formatAmount(detailData.amount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusTagType(detailData.status)">
                {{ getStatusLabel(detailData.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="申请原因" :span="2">
              {{ detailData.reason || '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ detailData.reviewTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核备注" :span="2">
              {{ detailData.reviewRemark || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 物流信息 -->
          <el-divider content-position="left">
            <span class="divider-title">物流信息</span>
          </el-divider>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="物流公司">
              <span v-if="detailData.logisticsCompany">{{ detailData.logisticsCompany }}</span>
              <span v-else class="text-muted">未填写</span>
            </el-descriptions-item>
            <el-descriptions-item label="物流单号">
              <span v-if="detailData.logisticsNo">{{ detailData.logisticsNo }}</span>
              <span v-else class="text-muted">未填写</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="detailData.trackingNo" label="换货发出物流">
              {{ detailData.trackingNo }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 快捷操作 -->
          <div class="detail-actions">
            <el-button
              v-if="detailData.status === 0"
              type="success"
              size="small"
              @click="openAuditDialog(detailData!, 1)"
            >审核通过</el-button>
            <el-button
              v-if="detailData.status === 0"
              type="danger"
              size="small"
              @click="openAuditDialog(detailData!, 2)"
            >审核拒绝</el-button>
            <el-button
              v-if="detailData.status === 5 || detailData.status === 6"
              type="warning"
              size="small"
              @click="openLogisticsDialog(detailData!)"
            >更新物流</el-button>
          </div>
        </template>

        <template v-else-if="!detailLoading">
          <el-empty description="暂无数据" />
        </template>
      </div>

      <template #footer>
        <el-button @click="closeDetail">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="showAuditDialog"
      :title="auditForm.status === 1 ? '审核通过' : '审核拒绝'"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="审核结果">
          <el-tag :type="auditForm.status === 1 ? 'success' : 'danger'">
            {{ auditForm.status === 1 ? '通过' : '拒绝' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入审核备注（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAudit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 物流对话框 -->
    <el-dialog
      v-model="showLogisticsDialog"
      title="更新物流信息"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="100px">
        <el-form-item label="物流公司" required>
          <el-input
            v-model="logisticsForm.logisticsCompany"
            placeholder="请输入物流公司名称"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input
            v-model="logisticsForm.logisticsNo"
            placeholder="请输入物流单号"
            maxlength="50"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLogisticsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateLogistics">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.aftersale-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
  }
}

.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #c0c4cc;
}

.amount-text {
  color: #C4908F;
  font-weight: 600;
}

.detail-content {
  min-height: 200px;
}

.divider-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>

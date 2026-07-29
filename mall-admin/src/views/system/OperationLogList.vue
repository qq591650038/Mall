<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOperationLogPage, type OperationLog } from '@/api/operationLog'

const list = ref<OperationLog[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)
const status = ref<number>()
const eventType = ref('')
const userId = ref<number>()

async function load() {
  loading.value = true
  try {
    const result = await getOperationLogPage({ current: current.value, size: 20, status: status.value, eventType: eventType.value || undefined, userId: userId.value })
    list.value = result.list || []
    total.value = result.total || 0
  } finally { loading.value = false }
}
function search() { current.value = 1; load() }
function eventLabel(value?: string) { return ({ RECEIVED: '领取', LOCKED: '锁定', RELEASED: '释放', REFUNDED: '退款释放' } as Record<string, string>)[value || ''] || value || '-' }
onMounted(load)
</script>

<template>
  <div class="audit-page">
    <div class="page-header"><h2>审计日志</h2></div>
    <div class="filter-bar">
      <el-input-number v-model="userId" :min="1" :controls="false" placeholder="用户ID" />
      <el-select v-model="status" clearable placeholder="操作结果" style="width: 130px"><el-option label="成功" :value="1" /><el-option label="失败" :value="0" /></el-select>
      <el-select v-model="eventType" clearable placeholder="业务事件" style="width: 150px"><el-option label="优惠券领取" value="RECEIVED" /><el-option label="优惠券锁定" value="LOCKED" /><el-option label="优惠券释放" value="RELEASED" /><el-option label="退款释放" value="REFUNDED" /></el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div class="table-card">
      <el-table :data="list" v-loading="loading" border>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="adminName" label="操作者" width="120"><template #default="{ row }">{{ row.adminName || (row.eventType ? '系统' : '-') }}</template></el-table-column>
        <el-table-column prop="module" label="模块" width="150" />
        <el-table-column prop="operation" label="操作" width="150" />
        <el-table-column label="优惠券事件" width="120"><template #default="{ row }"><el-tag v-if="row.eventType" type="warning">{{ eventLabel(row.eventType) }}</el-tag><span v-else>-</span></template></el-table-column>
        <el-table-column label="关联数据" min-width="220"><template #default="{ row }">用户 {{ row.userId || '-' }} · 券 {{ row.userCouponId || '-' }} · 订单 {{ row.orderId || '-' }}</template></el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="结果" width="80"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'danger' : 'success'">{{ row.status === 0 ? '失败' : '成功' }}</el-tag></template></el-table-column>
      </el-table>
      <el-pagination v-if="total" class="pagination" background layout="total, prev, pager, next" :page-size="20" :current-page="current" :total="total" @current-change="(page: number) => { current = page; load() }" />
    </div>
  </div>
</template>

<style scoped lang="scss">.audit-page{display:flex;flex-direction:column;gap:16px}.page-header h2{margin:0}.filter-bar{display:flex;gap:12px}.table-card{background:#fff;padding:16px}.pagination{margin-top:16px;justify-content:flex-end}</style>

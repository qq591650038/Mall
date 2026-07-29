<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {completeRefund, getRefundCursorPage, reviewRefund} from '@/api/refund'

const loading = ref(false)
const refunds = ref<any[]>([])
const size = ref(20)
const status = ref<number>()
const orderNo = ref('')
const cursor = ref<string>()
const nextCursor = ref<string>()
const history = ref<string[]>([])
const page = ref(1)
const dialog = ref(false)
const current = ref<any>()
const review = ref({status: 1, remark: ''})
const statusMap: Record<number, { text: string; type: any }> = {
  0: {text: 'Pending', type: 'warning'}, 1: {text: 'Approved', type: 'primary'},
  2: {text: 'Refunding', type: 'info'}, 3: {text: 'Refunded', type: 'success'},
  4: {text: 'Rejected', type: 'danger'}, 5: {text: 'Returning', type: 'warning'},
  6: {text: 'Exchanging', type: 'primary'}, 7: {text: 'Failed', type: 'danger'}
}

async function load() {
  loading.value = true
  try {
    const result = await getRefundCursorPage({
      size: size.value,
      status: status.value,
      orderNo: orderNo.value || undefined,
      cursor: cursor.value
    })
    refunds.value = result.list || []
    nextCursor.value = result.nextCursor
  } finally {
    loading.value = false
  }
}

function search() {
  cursor.value = undefined;
  nextCursor.value = undefined;
  history.value = [];
  page.value = 1;
  load()
}

function next() {
  if (!nextCursor.value) return;
  history.value.push(cursor.value || '');
  cursor.value = nextCursor.value;
  page.value++;
  load()
}

function previous() {
  const previous = history.value.pop();
  if (previous === undefined) return;
  cursor.value = previous || undefined;
  page.value--;
  load()
}

function openReview(row: any, nextStatus: number) {
  current.value = row;
  review.value = {status: nextStatus, remark: ''};
  dialog.value = true
}

async function submitReview() {
  if (!current.value) return;
  await reviewRefund(current.value.id, review.value.status, review.value.remark);
  ElMessage.success('Review completed');
  dialog.value = false;
  load()
}

async function complete(row: any) {
  await ElMessageBox.confirm('Confirm refund completion?', 'Confirm');
  await completeRefund(row.id);
  ElMessage.success('Refund completed');
  load()
}

onMounted(load)
</script>

<template>
  <div class="refund-page">
    <div class="page-header"><h2>Refund Management</h2></div>
    <div class="filter-bar">
      <el-select v-model="status" clearable placeholder="Status" style="width: 150px">
        <el-option v-for="(value, key) in statusMap" :key="key" :label="value.text" :value="Number(key)"/>
      </el-select>
      <el-input v-model="orderNo" clearable placeholder="Order number" style="width: 200px" @keyup.enter="search"/>
      <el-button type="primary" @click="search">Search</el-button>
    </div>
    <div class="table-card">
      <el-table :data="refunds" v-loading="loading" border>
        <el-table-column label="Refund No." prop="refundNo" width="180"/>
        <el-table-column label="Order No." prop="orderNo" width="200"/>
        <el-table-column label="User" prop="username" width="120"/>
        <el-table-column label="Amount" prop="amount" width="120">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="Reason" prop="reason" show-overflow-tooltip/>
        <el-table-column label="Status" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Created" prop="createTime" width="170"/>
        <el-table-column fixed="right" label="Actions" width="200">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" @click="openReview(row, 1)">Approve
            </el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" @click="openReview(row, 4)">Reject</el-button>
            <el-button v-if="row.status === 2 || row.status === 5" size="small" type="primary" @click="complete(row)">
              Complete
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><span>Page {{ page }}</span>
        <el-button :disabled="loading || !history.length" @click="previous">Previous</el-button>
        <el-button :disabled="loading || !nextCursor" type="primary" @click="next">Next</el-button>
      </div>
    </div>
    <el-dialog v-model="dialog" title="Review refund" width="500px">
      <el-form :model="review" label-width="100px">
        <el-form-item label="Result">
          <el-tag :type="review.status === 1 ? 'success' : 'danger'">{{
              review.status === 1 ? 'Approved' : 'Rejected'
            }}
          </el-tag>
        </el-form-item>
        <el-form-item label="Remark">
          <el-input v-model="review.remark" :rows="3" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">Cancel</el-button>
        <el-button type="primary" @click="submitReview">Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>.refund-page {
  padding: 20px
}

.page-header {
  margin-bottom: 20px
}

.filter-bar, .pagination {
  display: flex;
  gap: 12px;
  align-items: center
}

.filter-bar {
  margin-bottom: 20px
}

.table-card {
  padding: 20px;
  background: #fff
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px
}</style>

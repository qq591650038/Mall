<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCouponPage, createCoupon, updateCoupon, deleteCoupon } from '@/api/coupon'
import type { Coupon } from '@/types'

const loading = ref(false)
const tableData = ref<Coupon[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const showDialog = ref(false)
const editing = ref<Coupon | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  type: 1,
  value: 0,
  minAmount: 0,
  totalCount: 0,
  status: 1,
  description: '',
  startTime: '',
  endTime: ''
})

const couponTypes = [
  { value: 1, label: '满减券' },
  { value: 2, label: '折扣券' },
  { value: 3, label: '无门槛券' }
]

async function loadData() {
  loading.value = true
  try {
    const res = await getCouponPage({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    tableData.value = res.list
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function resetSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  loadData()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', type: 1, value: 0, minAmount: 0, totalCount: 0, status: 1, description: '', startTime: '', endTime: '' })
  showDialog.value = true
}

function openEdit(row: Coupon) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    type: row.type,
    value: row.value,
    minAmount: row.minAmount || 0,
    totalCount: row.totalCount,
    status: row.status ?? 1,
    description: row.description || '',
    startTime: row.startTime || '',
    endTime: row.endTime || ''
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const payload: any = { ...form }
      if (form.type !== 1) payload.minAmount = 0
      if (!payload.startTime) delete payload.startTime
      if (!payload.endTime) delete payload.endTime
      if (editing.value) {
        await updateCoupon({ ...payload, id: editing.value.id })
        ElMessage.success('更新成功')
      } else {
        await createCoupon(payload)
        ElMessage.success('新增成功')
      }
      showDialog.value = false
      loadData()
    } catch { /* handled */ }
  })
}

async function handleDelete(row: Coupon) {
  try {
    await ElMessageBox.confirm(`确定删除优惠券「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteCoupon(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled or error */ }
}

function handleStatusChange(row: Coupon, val: number) {
  updateCoupon({ id: row.id, status: val }).then(() => {
    row.status = val
    ElMessage.success('状态更新成功')
  })
}

function getTypeLabel(type: number) {
  return couponTypes.find(t => t.value === type)?.label || '未知'
}

onMounted(loadData)
</script>

<template>
  <div class="coupon-list-page">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="openCreate">+ 新增优惠券</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索优惠券名称"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ getTypeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="value" label="面值" width="100">
        <template #default="{ row }">
          <span v-if="row.type === 2">{{ row.value }}折</span>
          <span v-else>¥{{ row.value }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="minAmount" label="门槛" width="100">
        <template #default="{ row }">
          <span v-if="row.minAmount && row.minAmount > 0">¥{{ row.minAmount }}</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="库存" width="120">
        <template #default="{ row }">{{ row.remainCount }} / {{ row.totalCount }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="(val: boolean) => handleStatusChange(row, val ? 1 : 0)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <el-dialog v-model="showDialog" :title="editing ? '编辑优惠券' : '新增优惠券'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="优惠券名称" required>
          <el-input v-model="form.name" placeholder="请输入优惠券名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option v-for="t in couponTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="面值" required>
          <el-input-number v-model="form.value" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="使用门槛">
          <el-input-number v-model="form.minAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="发放数量" required>
          <el-input-number v-model="form.totalCount" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="优惠券描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="可选" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="可选" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.coupon-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; } }
.search-bar { display: flex; gap: 8px; }
.pagination-wrap { display: flex; justify-content: flex-end; }
</style>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLowStockProducts, getInventoryLogs, adjustStock, retryInventoryLog } from '@/api/inventory'

const loading = ref(false)
const lowStockProducts = ref<any[]>([])
const logs = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref<number | undefined>(undefined)

const adjustDialogVisible = ref(false)
const adjustForm = ref({ type: 'sku' as 'sku' | 'product', skuId: 0, productId: 0, quantity: 0, reason: '' })

async function loadLowStock() {
  try {
    const res = await getLowStockProducts()
    lowStockProducts.value = res || []
  } catch { lowStockProducts.value = [] }
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getInventoryLogs({
      current: currentPage.value,
      size: pageSize.value,
      operation: filterStatus.value === undefined ? undefined : ['RESERVE', 'RELEASE', 'DEDUCT', 'ADJUST'][filterStatus.value]
    })
    logs.value = res?.list || []
    total.value = res?.total || 0
  } catch { logs.value = [] }
  finally { loading.value = false }
}

async function handleAdjust() {
  try {
    await ElMessageBox.confirm('确认调整库存？', '提示')
    await adjustStock(adjustForm.value.skuId, adjustForm.value.productId, adjustForm.value.quantity, adjustForm.value.reason, adjustForm.value.type)
    ElMessage.success('库存调整成功')
    adjustDialogVisible.value = false
    loadLowStock()
    loadLogs()
  } catch { /* cancelled or error */ }
}

function openSkuAdjustDialog(product: any) {
  adjustForm.value = {
    type: 'sku',
    skuId: product.id || product.skuId || 0,
    productId: product.id || product.productId || 0,
    quantity: 0,
    reason: ''
  }
  adjustDialogVisible.value = true
}

function openProductAdjustDialog(product: any) {
  adjustForm.value = { type: 'product', skuId: 0, productId: product.productId || product.id || 0, quantity: 0, reason: '' }
  adjustDialogVisible.value = true
}

function getStatusType(status: number) {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

function getOperationType(operation: string) {
  const map: Record<string, string> = {
    'RESERVE': 'warning', 'RELEASE': 'success', 'DEDUCT': 'danger', 'ADJUST': 'info'
  }
  return map[operation] || 'info'
}

async function handleRetry(row: any) {
  try { await retryInventoryLog(row.id); ElMessage.success('重试成功'); loadLogs() } catch { /* handled */ }
}

onMounted(() => {
  loadLowStock()
  loadLogs()
})
</script>

<template>
  <div class="inventory-page">
    <div class="page-header">
      <h2>库存管理</h2>
    </div>

    <div class="section-card">
      <h3>库存预警</h3>
      <el-alert
        v-if="lowStockProducts.length > 0"
        :title="`共 ${lowStockProducts.length} 件商品库存不足`"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-table :data="lowStockProducts" v-if="lowStockProducts.length > 0" border>
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="skuInfo" label="规格" width="150" />
        <el-table-column prop="stock" label="当前库存" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.stock < 10 ? '#ff4d4f' : '#fa8c16', fontWeight: 600 }">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openSkuAdjustDialog(row)">调整 SKU</el-button>
            <el-button link type="warning" size="small" @click="openProductAdjustDialog(row)">商品总库存</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无库存预警商品" />
    </div>

    <div class="section-card">
      <h3>库存流水</h3>
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="操作类型" clearable style="width: 150px">
          <el-option label="预占" :value="0" />
          <el-option label="释放" :value="1" />
          <el-option label="扣减" :value="2" />
          <el-option label="调整" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadLogs">查询</el-button>
      </div>
      <el-table :data="logs" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="skuId" label="SKU ID" width="100" />
        <el-table-column prop="quantity" label="数量" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.operation === 'DEDUCT' ? '#ff4d4f' : '#52c41a' }">
              {{ row.operation === 'DEDUCT' ? '-' : '+' }}{{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationType(row.operation)">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 2 && row.operation === 'RELEASE'" link type="warning" @click="handleRetry(row)">重试</el-button>
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
        @size-change="loadLogs"
        @current-change="loadLogs"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </div>

    <el-dialog v-model="adjustDialogVisible" :title="adjustForm.type === 'sku' ? '调整 SKU 库存' : '调整商品总库存'" width="500px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="调整对象">
          <el-tag :type="adjustForm.type === 'sku' ? 'primary' : 'warning'">{{ adjustForm.type === 'sku' ? 'SKU 库存' : '商品总库存' }}</el-tag>
        </el-form-item>
        <el-form-item v-if="adjustForm.type === 'sku'" label="SKU ID">
          <el-input v-model="adjustForm.skuId" disabled />
        </el-form-item>
        <el-form-item label="商品 ID"><el-input v-model="adjustForm.productId" disabled /></el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="adjustForm.quantity" :min="-9999" :max="9999" />
          <span class="tip">正数增加，负数减少</span>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.inventory-page { padding: 20px; }
.page-header { margin-bottom: 20px; h2 { margin: 0; } }
.section-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;

  h3 { margin: 0 0 16px; font-size: 16px; }
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.tip { color: #999; font-size: 12px; margin-left: 8px; }
</style>

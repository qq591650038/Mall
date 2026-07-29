<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCouponPage } from '@/api/coupon'
import { getProductPage, getProductById } from '@/api/product'
import type { Coupon, Product, ProductSku } from '@/types'
import {
  getPointsProductPage,
  createPointsProduct,
  updatePointsProduct,
  deletePointsProduct,
  updatePointsProductStatus,
  getPointsRedemptions,
  initPointsProducts,
  type PointsProduct,
  type PointsRedemption
} from '@/api/points'

// 列表数据
const tableData = ref<PointsProduct[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

// 搜索筛选
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 新增/编辑弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增兑换商品')
const formRef = ref<FormInstance>()
const formData = reactive<Partial<PointsProduct>>({
  name: '',
  description: '',
  pointsCost: 100,
  stock: 0,
  rewardType: 'COUPON',
  rewardRefId: undefined,
  rewardSkuId: undefined,
  status: 1
})
const isEdit = ref(false)
const editId = ref<number | null>(null)
const rewardOptionsLoading = ref(false)
const couponOptions = ref<Coupon[]>([])
const productOptions = ref<Product[]>([])
const skuOptions = ref<ProductSku[]>([])

// 兑换记录弹窗
const redemptionDialogVisible = ref(false)
const redemptionLoading = ref(false)
const redemptionData = ref<PointsRedemption[]>([])
const redemptionTotal = ref(0)
const redemptionCurrent = ref(1)
const currentRedemptionProductId = ref<number | null>(null)

// 加载列表
async function loadData() {
  loading.value = true
  try {
    const result = await getPointsProductPage({
      current: current.value,
      size: pageSize.value,
      keyword: searchForm.keyword || undefined,
      status: searchForm.status
    })
    tableData.value = result.list || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  current.value = 1
  loadData()
}

// 重置搜索
function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  current.value = 1
  loadData()
}

// 分页
function handlePageChange(page: number) {
  current.value = page
  loadData()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  current.value = 1
  loadData()
}

// 打开新增弹窗
function handleAdd() {
  isEdit.value = false
  editId.value = null
  dialogTitle.value = '新增兑换商品'
  Object.assign(formData, {
    name: '',
    description: '',
    pointsCost: 100,
    stock: 0,
    rewardType: 'COUPON',
    rewardRefId: undefined,
    rewardSkuId: undefined,
    status: 1
  })
  dialogVisible.value = true
  loadRewardOptions()
}

// 打开编辑弹窗
function handleEdit(row: PointsProduct) {
  isEdit.value = true
  editId.value = row.id ?? null
  dialogTitle.value = '编辑兑换商品'
  Object.assign(formData, {
    name: row.name,
    description: row.description || '',
    pointsCost: row.pointsCost,
    stock: row.stock,
    rewardType: row.rewardType,
    rewardRefId: row.rewardRefId,
    rewardSkuId: row.rewardSkuId,
    status: row.status
  })
  dialogVisible.value = true
  loadRewardOptions()
}

async function loadRewardOptions() {
  rewardOptionsLoading.value = true
  try {
    if (formData.rewardType === 'COUPON') {
      couponOptions.value = (await getCouponPage({ current: 1, size: 200 })).list.filter(c => c.status === 1)
    } else {
      productOptions.value = (await getProductPage({ current: 1, size: 200, status: 1 })).list
      if (formData.rewardRefId) await loadSkuOptions(formData.rewardRefId)
    }
  } finally {
    rewardOptionsLoading.value = false
  }
}

async function loadSkuOptions(productId: number) {
  const product = await getProductById(productId)
  skuOptions.value = (product.skus || []).filter(sku => sku.status !== 0)
}

async function handleRewardTypeChange() {
  formData.rewardRefId = undefined
  formData.rewardSkuId = undefined
  skuOptions.value = []
  await loadRewardOptions()
}

async function handleRewardProductChange(productId: number) {
  formData.rewardSkuId = undefined
  await loadSkuOptions(productId)
}

// 提交表单
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (isEdit.value && editId.value) {
      await updatePointsProduct(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createPointsProduct(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 验证失败已由表单处理
  }
}

// 删除
async function handleDelete(row: PointsProduct) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.name}」吗？删除后将变为下架状态，保留历史记录。`,
      '确认删除',
      { type: 'warning' }
    )
    await deletePointsProduct(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消
  }
}

// 切换上下架
async function handleToggleStatus(row: PointsProduct) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}「${row.name}」吗？`, `确认${action}`, {
      type: 'warning'
    })
    await updatePointsProductStatus(row.id!, newStatus)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch {
    // 用户取消
  }
}

// 查看兑换记录
async function handleViewRedemptions(row: PointsProduct) {
  redemptionDialogVisible.value = true
  redemptionCurrent.value = 1
  currentRedemptionProductId.value = row.id!
  await loadRedemptions(row.id!)
}

async function loadRedemptions(productId: number) {
  redemptionLoading.value = true
  try {
    const result = await getPointsRedemptions({
      current: redemptionCurrent.value,
      size: 10,
      productId
    })
    redemptionData.value = result.list || []
    redemptionTotal.value = result.total || 0
  } finally {
    redemptionLoading.value = false
  }
}

// 兑换记录分页切换
function handleRedemptionPageChange(page: number) {
  redemptionCurrent.value = page
  if (currentRedemptionProductId.value != null) {
    loadRedemptions(currentRedemptionProductId.value)
  }
}

// 初始化数据
async function handleInitData() {
  try {
    await ElMessageBox.confirm(
      '确定要初始化兑换商品数据吗？仅在无数据时生效。',
      '确认初始化',
      { type: 'warning' }
    )
    const products = await initPointsProducts()
    if (products && products.length > 0) {
      ElMessage.success(`初始化成功，新增 ${products.length} 个兑换商品`)
    } else {
      ElMessage.info('已有数据，跳过初始化')
    }
    loadData()
  } catch {
    // 用户取消
  }
}

// 状态标签
function getStatusTag(status: number) {
  return status === 1 ? 'success' : 'info'
}

function getStatusText(status: number) {
  return status === 1 ? '已上架' : '已下架'
}

// 奖励类型
function getRewardTypeText(type: string) {
  const map: Record<string, string> = {
    COUPON: '优惠券',
    PHYSICAL: '实物',
    VIRTUAL: '虚拟',
    SERVICE: '服务'
  }
  return map[type] || type
}

const formRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  pointsCost: [{ required: true, message: '请输入所需积分', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }]
}

onMounted(loadData)
</script>

<template>
  <div class="points-product-list">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="search-form">
        <el-form :model="searchForm" inline>
          <el-form-item label="商品名称">
            <el-input
              v-model="searchForm.keyword"
              placeholder="请输入商品名称"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="searchForm.status"
              placeholder="全部状态"
              clearable
              style="width: 120px"
            >
              <el-option label="已上架" :value="1" />
              <el-option label="已下架" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      <div class="actions">
        <el-button @click="handleInitData">初始化数据</el-button>
        <el-button type="primary" @click="handleAdd">新增兑换商品</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="pointsCost" label="所需积分" width="100">
        <template #default="{ row }">
          <span class="points">{{ row.pointsCost }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80">
        <template #default="{ row }">
          <span :class="{ 'low-stock': row.stock <= 10 }">{{ row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="rewardType" label="奖励类型" width="100">
        <template #default="{ row }">
          {{ getRewardTypeText(row.rewardType) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button
            size="small"
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" @click="handleViewRedemptions(row)">兑换记录</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      class="pagination"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      v-model:current-page="current"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入兑换商品名称" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-select
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>
        <el-form-item label="所需积分" prop="pointsCost">
          <el-input-number
            v-model="formData.pointsCost"
            :min="1"
            :max="999999"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="库存数量" prop="stock">
          <el-input-number
            v-model="formData.stock"
            :min="0"
            :max="999999"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="奖励类型">
          <el-select v-model="formData.rewardType" style="width: 100%" @change="handleRewardTypeChange">
            <el-option label="优惠券" value="COUPON" />
            <el-option label="实物" value="PHYSICAL" />
            <el-option label="虚拟" value="VIRTUAL" />
            <el-option label="服务" value="SERVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖励标识">
          <el-select
            v-model="formData.rewardRefId"
            filterable
            :loading="rewardOptionsLoading"
            @change="formData.rewardType !== 'COUPON' && handleRewardProductChange($event)"
            placeholder="如：COUPON_10_OFF_100"
          >
            <el-option v-for="option in formData.rewardType === 'COUPON' ? couponOptions : productOptions" :key="option.id" :label="option.name" :value="option.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-if="formData.rewardType !== 'COUPON'" v-model="formData.rewardSkuId" placeholder="请选择奖励 SKU" filterable style="width: 100%; margin-bottom: 12px">
            <el-option v-for="sku in skuOptions" :key="sku.id" :label="`${sku.specInfo || sku.skuCode || '默认规格'}（库存 ${sku.stock}）`" :value="sku.id" />
          </el-select>
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 兑换记录弹窗 -->
    <el-dialog v-model="redemptionDialogVisible" title="兑换记录" width="700px">
      <el-table :data="redemptionData" v-loading="redemptionLoading" border>
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="productId" label="商品ID" width="100" />
        <el-table-column prop="points" label="消耗积分" width="100">
          <template #default="{ row }">
            <span class="points">-{{ row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="redemptionCode" label="兑换码" min-width="200" />
        <el-table-column prop="createTime" label="兑换时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!redemptionLoading && !redemptionData.length"
        description="暂无兑换记录"
      />
      <el-pagination
        v-if="redemptionTotal > 10"
        class="pagination"
        background
        layout="prev, pager, next"
        :total="redemptionTotal"
        v-model:current-page="redemptionCurrent"
        @current-change="handleRedemptionPageChange"
      />
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.points-product-list {
  padding: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.search-form {
  flex: 1;
}
.actions {
  display: flex;
  gap: 8px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.points {
  color: #ff6b35;
  font-weight: 600;
}
.low-stock {
  color: #e6a23c;
}
</style>

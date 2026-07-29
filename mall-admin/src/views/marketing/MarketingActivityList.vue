<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import {
  getActivityPage,
  getActivityDetail,
  getActivityItems,
  createActivity,
  updateActivity,
  deleteActivity,
  cancelActivity
  ,getActivityGroups
} from '@/api/marketing'
import { getCategoryList, getProductById, getProductPage } from '@/api/product'
import type { MarketingActivity, MarketingActivityItem, MarketingActivityCreateRequest } from '@/types'
import type { MarketingGroup } from '@/types'
import type { Category, Product, ProductSku } from '@/types'

// ========== 数据加载 ==========
const loading = ref(false)
const tableData = ref<MarketingActivity[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// ========== 搜索条件 ==========
const searchKeyword = ref('')
const filterType = ref<string | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)

// ========== 活动类型映射 ==========
const activityTypeMap: Record<string, { text: string; type: string }> = {
  LIMITED_DISCOUNT: { text: '限时折扣', type: 'warning' },
  FULL_REDUCTION: { text: '满减', type: 'danger' },
  FLASH_SALE: { text: '秒杀', type: 'primary' },
  GROUP_BUY: { text: '拼团', type: 'success' }
}

// ========== 活动状态映射 ==========
const activityStatusMap: Record<number, { text: string; type: string }> = {
  0: { text: '未开始', type: 'info' },
  1: { text: '进行中', type: 'success' },
  2: { text: '已结束', type: 'warning' },
  3: { text: '已取消', type: 'danger' }
}

// ========== 活动类型选项（用于下拉选择） ==========
const activityTypeOptions = [
  { value: 'LIMITED_DISCOUNT', label: '限时折扣' },
  { value: 'FULL_REDUCTION', label: '满减' },
  { value: 'FLASH_SALE', label: '秒杀' },
  { value: 'GROUP_BUY', label: '拼团' }
]

// ========== 表格数据加载 ==========
async function loadData() {
  loading.value = true
  try {
    const res = await getActivityPage({
      current: currentPage.value,
      size: pageSize.value,
      type: filterType.value,
      status: filterStatus.value
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

// ========== 创建/编辑对话框 ==========
const showFormDialog = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const products = ref<Product[]>([])
const productLoading = ref(false)

interface ActivityFormItem extends MarketingActivityItem {
  categoryId?: number
  skuOptions: ProductSku[]
}

const defaultItem = (): ActivityFormItem => ({
  productId: 0,
  skuId: undefined,
  activityPrice: 0,
  originalPrice: 0,
  stock: 0,
  limitPerUser: undefined,
  skuOptions: []
})

const form = reactive<{
  name: string
  type: string
  description: string
  startTime: string
  endTime: string
  sort: number
  groupTarget?: number
  items: ActivityFormItem[]
}>({
  name: '',
  type: 'LIMITED_DISCOUNT',
  description: '',
  startTime: '',
  endTime: '',
  sort: 0,
  groupTarget: undefined,
  items: [defaultItem()]
})

async function loadProductOptions() {
  productLoading.value = true
  try {
    const [categoryResult, productResult] = await Promise.all([
      getCategoryList(),
      getProductPage({ current: 1, size: 1000, status: 1 })
    ])
    categories.value = categoryResult || []
    products.value = productResult?.list || []
  } finally {
    productLoading.value = false
  }
}

function productsForCategory(categoryId?: number) {
  if (!categoryId) return []
  return products.value.filter(product => product.categoryId === categoryId)
}

async function handleProductChange(item: ActivityFormItem) {
  item.skuId = undefined
  item.skuOptions = []
  item.activityPrice = 0
  item.originalPrice = 0
  item.stock = 0
  if (!item.productId) return
  const detail = await getProductById(item.productId) as Product & { skus?: ProductSku[] }
  item.skuOptions = detail.skus || []
  item.originalPrice = detail.originalPrice || detail.price || 0
  item.stock = detail.totalStock || 0
  if (item.skuOptions.length === 1) {
    handleSkuChange(item, item.skuOptions[0].id)
  }
}

function handleSkuChange(item: ActivityFormItem, skuId?: number) {
  const sku = item.skuOptions.find(option => option.id === skuId)
  if (!sku) return
  item.skuId = sku.id
  item.originalPrice = sku.price || item.originalPrice
  item.activityPrice = sku.price || item.activityPrice
  item.stock = sku.stock ?? item.stock
}

// 商品明细操作
function addItem() {
  form.items.push(defaultItem())
}

function removeItem(index: number) {
  if (form.items.length <= 1) {
    ElMessage.warning('至少保留一条商品明细')
    return
  }
  form.items.splice(index, 1)
}

function openCreate() {
  isEditing.value = false
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: 'LIMITED_DISCOUNT',
    description: '',
    startTime: '',
    endTime: '',
    sort: 0,
    groupTarget: undefined,
    items: [defaultItem()]
  })
  loadProductOptions()
  showFormDialog.value = true
}

async function openEdit(row: MarketingActivity) {
  isEditing.value = true
  editingId.value = row.id
  try {
    // 活动详情和商品明细分两次请求
    const [detail, items] = await Promise.all([
      getActivityDetail(row.id),
      getActivityItems(row.id)
    ])
    Object.assign(form, {
      name: detail.name,
      type: detail.type,
      description: detail.description || '',
      startTime: detail.startTime || '',
      endTime: detail.endTime || '',
      sort: detail.sort || 0,
      groupTarget: detail.groupTarget,
      items: items && items.length > 0
        ? items.map(item => ({ ...item, skuOptions: [] }))
        : [defaultItem()]
    })
    await loadProductOptions()
    for (const item of form.items) {
      item.categoryId = products.value.find(product => product.id === item.productId)?.categoryId
      await handleProductChange(item)
      const matchedItem = items?.find(detailItem => detailItem.id === item.id)
      item.skuId = matchedItem?.skuId
    }
    showFormDialog.value = true
  } catch {
    ElMessage.error('加载活动详情失败')
  }
}

// 表单校验
const validateItems = (): boolean => {
  if (form.items.length === 0) {
    ElMessage.warning('请至少添加一条商品明细')
    return false
  }
  for (let i = 0; i < form.items.length; i++) {
    const item = form.items[i]
    if (!item.productId || item.productId <= 0) {
      ElMessage.warning(`第 ${i + 1} 条明细：请填写有效的商品ID`)
      return false
    }
    if (item.activityPrice <= 0) {
      ElMessage.warning(`第 ${i + 1} 条明细：活动价格必须大于 0`)
      return false
    }
    if (item.stock < 0) {
      ElMessage.warning(`第 ${i + 1} 条明细：库存不能为负数`)
      return false
    }
  }
  if (form.type === 'GROUP_BUY' && (!form.groupTarget || form.groupTarget < 2)) {
    ElMessage.warning('拼团活动的成团人数必须至少为2人')
    return false
  }
  return true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!validateItems()) return

    const payload: MarketingActivityCreateRequest = {
      name: form.name,
      type: form.type,
      description: form.description || undefined,
      startTime: form.startTime,
      endTime: form.endTime,
      sort: form.sort || 0,
      groupTarget: form.type === 'GROUP_BUY' ? form.groupTarget : undefined,
      items: form.items.map(item => ({
        id: item.id,
        productId: item.productId,
        skuId: item.skuId,
        activityPrice: item.activityPrice,
        originalPrice: item.originalPrice,
        stock: item.stock,
        limitPerUser: item.limitPerUser
      }))
    }

    try {
      if (isEditing.value && editingId.value) {
        await updateActivity(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createActivity(payload)
        ElMessage.success('创建成功')
      }
      showFormDialog.value = false
      loadData()
    } catch {
      // 错误已在拦截器中处理
    }
  })
}

// ========== 详情对话框 ==========
const showDetailDialog = ref(false)
const detailData = ref<MarketingActivity | null>(null)
const detailLoading = ref(false)

async function openDetail(row: MarketingActivity) {
  detailLoading.value = true
  showDetailDialog.value = true
  try {
    // 活动详情和商品明细分两次请求
    const [detail, items] = await Promise.all([
      getActivityDetail(row.id),
      getActivityItems(row.id)
    ])
    detailData.value = { ...detail, items }
  } catch {
    detailData.value = row
    ElMessage.warning('加载商品明细失败，显示基本信息')
  } finally {
    detailLoading.value = false
  }
}

const showGroupsDialog = ref(false)
const groupsLoading = ref(false)
const groups = ref<MarketingGroup[]>([])
const groupsActivity = ref<MarketingActivity | null>(null)

async function openGroups(row: MarketingActivity) {
  groupsActivity.value = row
  showGroupsDialog.value = true
  groupsLoading.value = true
  try { groups.value = await getActivityGroups(row.id) } finally { groupsLoading.value = false }
}

function groupStatusText(status: number) { return ({ 1: '待成团', 2: '已成团', 3: '成团失败' } as Record<number, string>)[status] || '未知' }
function groupStatusTag(status: number) { return status === 2 ? 'success' : status === 3 ? 'danger' : 'warning' }
function orderStatusText(status?: number, text?: string) { return text || ({ 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款', 7: '待成团' } as Record<number, string>)[status || 0] || '未知' }

function closeDetail() {
  showDetailDialog.value = false
  detailData.value = null
}

// ========== 操作：取消活动 ==========
async function handleCancel(row: MarketingActivity) {
  try {
    await ElMessageBox.confirm(
      `确定取消活动「${row.name}」？取消后不可恢复。`,
      '取消活动',
      { type: 'warning' }
    )
    await cancelActivity(row.id)
    ElMessage.success('活动已取消')
    loadData()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 操作：删除活动 ==========
async function handleDelete(row: MarketingActivity) {
  try {
    await ElMessageBox.confirm(
      `确定删除活动「${row.name}」？此操作不可恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteActivity(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 辅助方法 ==========
function getTypeLabel(type: string) {
  return activityTypeMap[type]?.text || type
}

function getTypeTagType(type: string) {
  return activityTypeMap[type]?.type || 'info'
}

function getStatusLabel(status: number) {
  return activityStatusMap[status]?.text || '未知'
}

function getStatusTagType(status: number) {
  return activityStatusMap[status]?.type || 'info'
}

// 获取商品数量
function getProductCount(row: MarketingActivity): number {
  return row.itemCount ?? 0
}

onMounted(loadData)
</script>

<template>
  <div class="marketing-activity-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>营销活动管理</h2>
      <el-button type="primary" @click="openCreate">+ 创建活动</el-button>
    </div>

    <!-- 搜索区 -->
    <div class="search-bar">
      <el-select
        v-model="filterType"
        placeholder="活动类型"
        clearable
        style="width: 160px"
      >
        <el-option
          v-for="t in activityTypeOptions"
          :key="t.value"
          :label="t.label"
          :value="t.value"
        />
      </el-select>

      <el-select
        v-model="filterStatus"
        placeholder="活动状态"
        clearable
        style="width: 140px"
      >
        <el-option
          v-for="(v, k) in activityStatusMap"
          :key="k"
          :label="v.text"
          :value="Number(k)"
        />
      </el-select>

      <el-input
        v-model="searchKeyword"
        placeholder="搜索活动名称"
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
      <el-table-column prop="id" label="ID" width="80" />

      <el-table-column prop="name" label="活动名称" min-width="160" show-overflow-tooltip />

      <el-table-column label="活动类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.type)" effect="light">
            {{ getTypeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)" effect="light">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="startTime" label="开始时间" width="160">
        <template #default="{ row }">
          <span v-if="row.startTime">{{ row.startTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column prop="endTime" label="结束时间" width="160">
        <template #default="{ row }">
          <span v-if="row.endTime">{{ row.endTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="商品数量" width="100" align="center">
        <template #default="{ row }">
          <span>{{ getProductCount(row) }}</span>
        </template>
      </el-table-column>

      <el-table-column prop="sort" label="排序" width="80" align="center" />

      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span v-if="row.createTime">{{ row.createTime }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.type === 'GROUP_BUY'" link type="success" size="small" @click="openGroups(row)">拼团明细</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 1"
            link
            type="warning"
            size="small"
            @click="handleCancel(row)"
          >取消</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无活动数据" />
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

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showFormDialog"
      :title="isEditing ? '编辑营销活动' : '创建营销活动'"
      width="820px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="{
          name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
          type: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
          startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
          endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
        }"
        label-width="110px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="活动名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入活动名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="活动类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择活动类型" style="width: 100%">
                <el-option
                  v-for="t in activityTypeOptions"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                placeholder="选择结束时间"
                style="width: 100%"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序值">
              <el-input-number v-model="form.sort" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.type === 'GROUP_BUY'" :span="12">
            <el-form-item label="成团人数" prop="groupTarget">
              <el-input-number v-model="form.groupTarget" :min="2" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="活动描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="请输入活动描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 商品明细 -->
        <el-divider content-position="left">
          <span class="divider-title">商品明细</span>
        </el-divider>

        <div class="items-toolbar">
          <el-button type="primary" size="small" @click="addItem">+ 添加商品</el-button>
          <span class="items-count">共 {{ form.items.length }} 条</span>
        </div>

        <el-table :data="form.items" border size="small" class="items-table">
          <el-table-column label="分类" width="150">
            <template #default="{ row }">
              <el-select v-model="row.categoryId" placeholder="选择分类" clearable style="width: 100%" @change="row.productId = 0; row.skuId = undefined; row.skuOptions = []">
                <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="商品" width="220">
            <template #default="{ row }">
              <el-select v-model="row.productId" placeholder="选择商品" filterable :loading="productLoading" :disabled="!row.categoryId" style="width: 100%" @change="handleProductChange(row)">
                <el-option v-for="product in productsForCategory(row.categoryId)" :key="product.id" :label="product.name" :value="product.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="SKU" width="180">
            <template #default="{ row }">
              <el-select v-model="row.skuId" placeholder="选择 SKU" clearable :disabled="!row.productId" style="width: 100%" @change="handleSkuChange(row, $event)">
                <el-option v-for="sku in row.skuOptions" :key="sku.id" :label="`${sku.specInfo || sku.skuCode || sku.id} / 库存 ${sku.stock}`" :value="sku.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="商品ID" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.productId"
                :min="1"
                controls-position="right"
                style="width: 100%"
                placeholder="商品ID"
              />
            </template>
          </el-table-column>

          <el-table-column label="SKU ID" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.skuId"
                :min="1"
                controls-position="right"
                style="width: 100%"
                placeholder="可选"
              />
            </template>
          </el-table-column>

          <el-table-column label="活动价格" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.activityPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                placeholder="活动价"
              />
            </template>
          </el-table-column>

          <el-table-column label="原价" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.originalPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
                placeholder="原价"
              />
            </template>
          </el-table-column>

          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.stock"
                :min="0"
                controls-position="right"
                style="width: 100%"
                placeholder="库存"
              />
            </template>
          </el-table-column>

          <el-table-column label="限购/人" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.limitPerUser"
                :min="1"
                controls-position="right"
                style="width: 100%"
                placeholder="可选"
              />
            </template>
          </el-table-column>

          <el-table-column label="操作" width="70" fixed="right" align="center">
            <template #default="{ $index }">
              <el-button
                link
                type="danger"
                size="small"
                @click="removeItem($index)"
              >删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="请添加商品明细" :image-size="60" />
          </template>
        </el-table>
      </el-form>

      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showGroupsDialog" :title="`拼团明细 - ${groupsActivity?.name || ''}`" width="900px">
      <div v-loading="groupsLoading" class="groups-container">
        <el-empty v-if="!groups.length && !groupsLoading" description="暂无拼团记录" />
        <el-card v-for="group in groups" :key="group.groupNo" class="group-card" shadow="never">
          <template #header>
            <div class="group-header">
              <span>团号：{{ group.groupNo }}</span>
              <span>{{ group.joinedQuantity }} / {{ group.target || '-' }} 人</span>
              <el-tag :type="groupStatusTag(group.groupStatus)">{{ groupStatusText(group.groupStatus) }}</el-tag>
            </div>
          </template>
          <el-table :data="group.members" size="small" border>
            <el-table-column prop="username" label="用户" min-width="120"><template #default="{ row }">{{ row.username || '-' }}</template></el-table-column>
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="quantity" label="商品数量" width="90" />
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column label="参与状态" width="100"><template #default="{ row }">{{ row.participantStatus === 1 ? '已支付' : row.participantStatus === 2 ? '已取消' : '待支付' }}</template></el-table-column>
            <el-table-column label="订单状态" min-width="110"><template #default="{ row }">{{ orderStatusText(row.orderStatus, row.orderStatusText) }}</template></el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-dialog>

    <!-- 活动详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="活动详情"
      width="760px"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detailData">
          <!-- 基本信息 -->
          <el-descriptions :column="2" border>
            <el-descriptions-item label="活动ID">{{ detailData.id }}</el-descriptions-item>
            <el-descriptions-item label="活动名称">{{ detailData.name }}</el-descriptions-item>
            <el-descriptions-item label="活动类型">
              <el-tag :type="getTypeTagType(detailData.type)">
                {{ getTypeLabel(detailData.type) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="活动状态">
              <el-tag :type="getStatusTagType(detailData.status)">
                {{ getStatusLabel(detailData.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ detailData.startTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ detailData.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ detailData.sort ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="活动描述" :span="2">
              {{ detailData.description || '无' }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 商品明细 -->
          <el-divider content-position="left">
            <span class="divider-title">商品明细</span>
          </el-divider>

          <el-table
            :data="detailData.items || []"
            border
            size="small"
          >
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column prop="productName" label="商品名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="skuId" label="SKU ID" width="90">
              <template #default="{ row }">
                <span>{{ row.skuId || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="活动价格" width="110">
              <template #default="{ row }">
                <span class="price-activity">¥{{ row.activityPrice?.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="原价" width="110">
              <template #default="{ row }">
                <span class="price-original">¥{{ row.originalPrice?.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="90" align="center" />
            <el-table-column prop="soldCount" label="已售" width="90" align="center">
              <template #default="{ row }">
                <span>{{ row.soldCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="limitPerUser" label="限购/人" width="90" align="center">
              <template #default="{ row }">
                <span>{{ row.limitPerUser || '-' }}</span>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="暂无商品数据" :image-size="60" />
            </template>
          </el-table>
        </template>

        <template v-else-if="!detailLoading">
          <el-empty description="暂无数据" />
        </template>
      </div>

      <template #footer>
        <el-button @click="closeDetail">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.marketing-activity-page {
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

.items-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .items-count {
    font-size: 13px;
    color: #909399;
  }
}

.items-table {
  margin-top: 8px;
}

/* 隐藏旧版手工 ID 输入列，保留字段仅用于兼容历史活动数据。 */
.items-table :deep(th:nth-child(4)),
.items-table :deep(td:nth-child(4)),
.items-table :deep(th:nth-child(5)),
.items-table :deep(td:nth-child(5)) {
  display: none;
}

.divider-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.detail-content {
  min-height: 200px;
}

.price-activity {
  color: #f56c6c;
  font-weight: 600;
}

.price-original {
  color: #909399;
  text-decoration: line-through;
}
</style>

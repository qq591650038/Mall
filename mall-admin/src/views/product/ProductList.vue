<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductPage, deleteProduct, onShelf, offShelf } from '@/api/product'
import type { Product } from '@/types'

const router = useRouter()
const products = ref<Product[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref<number | ''>('')
const loading = ref(false)

async function loadProducts() {
  loading.value = true
  try {
    const params: any = { current: currentPage.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value
    const res = await getProductPage(params)
    products.value = res.list
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadProducts)

function handleSearch() { currentPage.value = 1; loadProducts() }
function changePage(page: number) { currentPage.value = page; loadProducts() }

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复！', '警告', { type: 'warning' })
  try {
    await deleteProduct(id)
    ElMessage.success('删除成功')
    loadProducts()
  } catch { /* handled */ }
}

async function handleOnShelf(id: number) {
  try { await onShelf(id); ElMessage.success('上架成功'); loadProducts() } catch { /* handled */ }
}

async function handleOffShelf(id: number) {
  try { await offShelf(id); ElMessage.success('下架成功'); loadProducts() } catch { /* handled */ }
}

function goCreate() { router.push({ name: 'ProductCreate' }) }
function goEdit(id: number) { router.push({ name: 'ProductEdit', params: { id } }) }

function getStatusTag(status: number) {
  return status === 1 ? { type: 'success', text: '已上架' } : { type: 'info', text: '已下架' }
}
</script>

<template>
  <div class="product-list-page">
    <div class="page-header">
      <h2>商品管理</h2>
      <el-button type="primary" @click="goCreate">+ 新增商品</el-button>
    </div>
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 240px" @keyup.enter="handleSearch">
        <template #append><el-button @click="handleSearch">搜索</el-button></template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px" @change="handleSearch">
        <el-option :value="1" label="已上架" />
        <el-option :value="0" label="已下架" />
      </el-select>
    </div>
    <el-table :data="products" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <el-image :src="row.mainImage" :preview-src-list="[row.mainImage]" style="width: 50px; height: 50px; border-radius: 4px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="totalStock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status).type">{{ getStatusTag(row.status).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="goEdit(row.id)">编辑</el-button>
          <el-button
            v-if="row.status === 0"
            link type="success" size="small"
            @click="handleOnShelf(row.id)"
          >上架</el-button>
          <el-button
            v-else
            link type="warning" size="small"
            @click="handleOffShelf(row.id)"
          >下架</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total" layout="total, prev, pager, next, jumper" @current-change="changePage" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.product-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; } }
.filter-bar { display: flex; gap: 16px; }
.pagination { display: flex; justify-content: flex-end; }
</style>

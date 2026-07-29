<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductPage } from '@/api/product'
import { getBrandList, getCategoryList } from '@/api/common'
import { addToCompare, isInCompare, getCompareCount } from '@/api/compare'
import type { Brand, Category, Product } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const brands = ref<Brand[]>([])
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const keyword = ref('')
const selectedCategory = ref<number | null>(null)
const selectedBrand = ref<number | null>(null)
const priceRange = ref<[number, number]>([0, 10000])
const minRating = ref<number | null>(null)
const inStock = ref(false)
const sortBy = ref<'default' | 'sales' | 'priceAsc' | 'priceDesc' | 'rating' | 'newest'>('default')

async function loadProducts() {
  loading.value = true
  try {
    const result = await getProductPage({
      current: currentPage.value,
      size: 12,
      keyword: keyword.value || undefined,
      categoryId: selectedCategory.value || undefined,
      brandId: selectedBrand.value || undefined,
      minPrice: priceRange.value[0] || undefined,
      maxPrice: priceRange.value[1] < 10000 ? priceRange.value[1] : undefined,
      minRating: minRating.value || undefined,
      inStock: inStock.value || undefined,
      sort: sortBy.value
    })
    products.value = result.list || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

function applyFilters() { currentPage.value = 1; loadProducts() }
function selectCategory(id: number | null) { selectedCategory.value = id; applyFilters() }
function resetFilters() {
  selectedCategory.value = null
  selectedBrand.value = null
  priceRange.value = [0, 10000]
  minRating.value = null
  inStock.value = false
  sortBy.value = 'default'
  applyFilters()
}

onMounted(async () => {
  keyword.value = String(route.query.keyword || '')
  selectedCategory.value = route.query.categoryId ? Number(route.query.categoryId) : null
  const [categoryList, brandList] = await Promise.all([
    getCategoryList().catch(() => []),
    getBrandList().catch(() => [])
  ])
  categories.value = categoryList
  brands.value = brandList
  loadProducts()
})

watch(() => route.query, () => {
  keyword.value = String(route.query.keyword || '')
  selectedCategory.value = route.query.categoryId ? Number(route.query.categoryId) : null
  applyFilters()
}, { deep: true })
watch(sortBy, applyFilters)

// 添加商品到对比列表
function handleAddToCompare(productId: number) {
  const result = addToCompare(productId)
  if (result.success) {
    ElMessage.success(`${result.message}（当前 ${getCompareCount()}/4）`)
  } else {
    ElMessage.warning(result.message)
  }
}
</script>

<template>
  <div class="product-list-page">
    <AppHeader />
    <main class="main-content">
      <div class="page-shell">
        <header class="page-heading">
          <div>
            <h1>全部商品</h1>
            <p>{{ keyword ? `“${keyword}”的搜索结果` : '发现适合你的商品' }}</p>
          </div>
          <span class="result-count">共 {{ total }} 件</span>
        </header>

        <div class="catalog-layout">
          <aside class="filter-panel">
            <div class="filter-title">
              <span>筛选</span>
              <el-button link type="primary" @click="resetFilters">重置</el-button>
            </div>

            <div class="filter-group category-group">
              <h2>商品分类</h2>
              <div class="category-options">
                <button :class="{ active: selectedCategory === null }" @click="selectCategory(null)">全部分类</button>
                <button v-for="cat in categories" :key="cat.id" :class="{ active: selectedCategory === cat.id }" @click="selectCategory(cat.id)">{{ cat.name }}</button>
              </div>
            </div>

            <div class="filter-group">
              <h2>品牌</h2>
              <el-select v-model="selectedBrand" clearable placeholder="全部品牌" @change="applyFilters">
                <el-option v-for="brand in brands" :key="brand.id" :label="brand.name" :value="brand.id" />
              </el-select>
            </div>

            <div class="filter-group price-filter">
              <h2>价格区间</h2>
              <el-slider v-model="priceRange" range :min="0" :max="10000" :step="100" @change="applyFilters" />
              <div class="price-labels"><span>¥{{ priceRange[0] }}</span><span>{{ priceRange[1] === 10000 ? '¥10000+' : `¥${priceRange[1]}` }}</span></div>
            </div>

            <div class="filter-group">
              <h2>最低评分</h2>
              <el-select v-model="minRating" clearable placeholder="不限评分" @change="applyFilters">
                <el-option v-for="star in [4, 3, 2, 1]" :key="star" :label="`${star} 星及以上`" :value="star" />
              </el-select>
            </div>

            <div class="stock-filter"><el-checkbox v-model="inStock" @change="applyFilters">仅看有货商品</el-checkbox></div>
          </aside>

          <section class="product-results">
            <div class="result-toolbar">
              <el-input v-model="keyword" class="list-search" placeholder="在商品中搜索" clearable @keyup.enter="applyFilters">
                <template #prefix><el-icon><Search /></el-icon></template>
                <template #append><el-button @click="applyFilters">搜索</el-button></template>
              </el-input>
              <div class="sort-control">
                <span>排序</span>
                <el-radio-group v-model="sortBy" size="small">
                  <el-radio-button value="default">综合</el-radio-button>
                  <el-radio-button value="sales">销量</el-radio-button>
                  <el-radio-button value="rating">评分</el-radio-button>
                  <el-radio-button value="newest">新品</el-radio-button>
                  <el-radio-button value="priceAsc">价格 ↑</el-radio-button>
                  <el-radio-button value="priceDesc">价格 ↓</el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <div v-if="loading" class="loading-panel"><el-skeleton :rows="8" animated /></div>
            <div v-else-if="products.length" class="product-grid">
              <article v-for="product in products" :key="product.id" class="product-card" tabindex="0" @click="router.push(`/products/${product.id}`)" @keyup.enter="router.push(`/products/${product.id}`)">
                <div class="product-image"><img :src="product.mainImage || '/favicon.svg'" :alt="product.name" loading="lazy" /></div>
                <div class="product-info">
                  <h3>{{ product.name }}</h3>
                  <div class="review-line">
                    <el-rate :model-value="product.averageRating || 0" disabled allow-half />
                    <span class="score">{{ (product.averageRating || 0).toFixed(1) }}</span>
                    <span class="review-count">{{ product.reviewCount || 0 }} 条</span>
                  </div>
                  <div class="product-meta">
                    <strong><small>¥</small>{{ product.price.toFixed(2) }}</strong>
                    <span>已售 {{ product.sales }}</span>
                  </div>
                  <div class="product-actions" @click.stop>
                    <el-button
                      size="small"
                      :type="isInCompare(product.id) ? 'info' : 'primary'"
                      :disabled="isInCompare(product.id)"
                      @click.stop="handleAddToCompare(product.id)"
                    >
                      {{ isInCompare(product.id) ? '已加入对比' : '加入对比' }}
                    </el-button>
                  </div>
                </div>
              </article>
            </div>
            <el-empty v-else description="暂无符合条件的商品" />

            <el-pagination v-if="total > 12" v-model:current-page="currentPage" background :page-size="12" :total="total" layout="prev, pager, next" @current-change="loadProducts" />
          </section>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.product-list-page { min-height: 100vh; background: #f4f5f7; }
.main-content { padding: 24px 0 48px; }
.page-shell { width: min(1240px, calc(100% - 40px)); margin: 0 auto; }
.page-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 18px; }
.page-heading h1 { margin: 0; color: #202124; font-size: 24px; line-height: 1.25; }
.page-heading p { margin: 6px 0 0; color: #8a8f98; font-size: 13px; }
.result-count { color: #767b84; font-size: 13px; }
.catalog-layout { display: grid; grid-template-columns: 224px minmax(0, 1fr); align-items: start; gap: 20px; }

.filter-panel { position: sticky; top: 84px; overflow: hidden; background: #fff; border: 1px solid #e7e9ed; border-radius: 8px; }
.filter-title { display: flex; align-items: center; justify-content: space-between; height: 52px; padding: 0 16px; border-bottom: 1px solid #eceef1; color: #292d32; font-size: 15px; font-weight: 600; }
.filter-group { padding: 16px; border-bottom: 1px solid #eff0f2; }
.filter-group h2 { margin: 0 0 12px; color: #50545b; font-size: 13px; font-weight: 600; }
.filter-group :deep(.el-select) { width: 100%; }
.category-options { display: grid; gap: 3px; max-height: 258px; overflow-y: auto; }
.category-options button { width: 100%; padding: 8px 10px; border: 0; border-radius: 4px; background: transparent; color: #646970; font: inherit; font-size: 13px; text-align: left; cursor: pointer; }
.category-options button:hover { background: #f7f8fa; color: #ff5f2e; }
.category-options button.active { background: #fff1eb; color: #e94f20; font-weight: 600; }
.price-filter { padding-bottom: 14px; }
.price-filter :deep(.el-slider) { margin: 2px 5px 3px; width: calc(100% - 10px); }
.price-labels { display: flex; justify-content: space-between; color: #90949c; font-size: 11px; }
.stock-filter { padding: 15px 16px; }

.product-results { min-width: 0; }
.result-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; min-height: 62px; padding: 12px 14px; margin-bottom: 16px; background: #fff; border: 1px solid #e7e9ed; border-radius: 8px; }
.list-search { width: min(330px, 40%); flex: 0 1 330px; }
.sort-control { display: flex; align-items: center; min-width: 0; gap: 10px; color: #858a92; font-size: 12px; }
.sort-control > span { flex: none; }
.sort-control :deep(.el-radio-group) { flex-wrap: wrap; justify-content: flex-end; }
.loading-panel { min-height: 520px; padding: 28px; background: #fff; border: 1px solid #e7e9ed; border-radius: 8px; }

.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(205px, 1fr)); gap: 16px; }
.product-card { min-width: 0; overflow: hidden; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; transition: border-color .2s, box-shadow .2s, transform .2s; }
.product-card:hover, .product-card:focus-visible { border-color: #ffb59d; box-shadow: 0 8px 22px rgba(37, 40, 45, .09); transform: translateY(-2px); outline: 0; }
.product-image { aspect-ratio: 1 / 1; overflow: hidden; background: #f7f8fa; }
.product-image img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform .25s ease; }
.product-card:hover .product-image img { transform: scale(1.025); }
.product-info { padding: 13px 14px 15px; }
.product-info h3 { height: 40px; margin: 0 0 9px; overflow: hidden; color: #303238; font-size: 14px; font-weight: 500; line-height: 20px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.review-line { display: flex; align-items: center; min-height: 18px; gap: 5px; white-space: nowrap; }
.review-line :deep(.el-rate) { height: 16px; }
.review-line :deep(.el-rate__icon) { margin-right: 0; font-size: 13px; }
.score { color: #d98c00; font-size: 11px; font-weight: 600; }
.review-count { color: #9ba0a8; font-size: 11px; }
.product-meta { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-top: 12px; }
.product-meta strong { min-width: 0; overflow: hidden; color: #ef5425; font-size: 18px; text-overflow: ellipsis; white-space: nowrap; }
.product-meta strong small { margin-right: 1px; font-size: 12px; }
.product-meta > span { flex: none; color: #999da5; font-size: 11px; }
.product-actions { margin-top: 12px; display: flex; justify-content: center; }
.product-actions .el-button { width: 100%; }
:deep(.el-pagination) { justify-content: center; margin-top: 28px; }

@media (max-width: 1050px) {
  .catalog-layout { grid-template-columns: 200px minmax(0, 1fr); gap: 14px; }
  .result-toolbar { align-items: stretch; flex-direction: column; }
  .list-search { width: 100%; max-width: none; flex-basis: auto; }
  .sort-control { justify-content: space-between; }
}

@media (max-width: 760px) {
  .main-content { padding-top: 16px; }
  .page-shell { width: min(100% - 24px, 600px); }
  .page-heading { align-items: start; }
  .page-heading h1 { font-size: 20px; }
  .catalog-layout { display: block; }
  .filter-panel { position: static; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-bottom: 14px; }
  .filter-title { grid-column: 1 / -1; }
  .filter-group { min-width: 0; border-right: 1px solid #eff0f2; }
  .category-group { grid-column: 1 / -1; }
  .category-options { display: flex; max-height: none; overflow-x: auto; padding-bottom: 2px; }
  .category-options button { width: auto; flex: none; white-space: nowrap; }
  .stock-filter { display: flex; align-items: center; }
  .result-toolbar { padding: 12px; }
  .sort-control { align-items: flex-start; flex-direction: column; }
  .sort-control :deep(.el-radio-group) { justify-content: flex-start; flex-wrap: nowrap; width: 100%; overflow-x: auto; padding-bottom: 3px; }
  .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .product-info { padding: 10px; }
  .product-meta { align-items: flex-start; flex-direction: column; gap: 4px; }
}

@media (max-width: 390px) {
  .filter-panel { grid-template-columns: 1fr; }
  .filter-title, .category-group { grid-column: 1; }
  .product-grid { grid-template-columns: 1fr; }
  .product-meta { flex-direction: row; align-items: baseline; }
}
</style>

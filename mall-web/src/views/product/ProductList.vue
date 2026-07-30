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

            <div class="filter-group">
              <h2>商品分类</h2>
              <el-select v-model="selectedCategory" clearable placeholder="全部分类" @change="applyFilters">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
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
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;700&display=swap');

$color-bg: #FFF9F5;
$color-bg-warm: #F5E6D3;
$color-accent: #D8A9A9;
$color-accent-dark: #C4908F;
$color-text: #3A3A3A;
$color-text-light: #6B6B6B;
$color-text-muted: #9B9B9B;
$shadow-card: 0 4px 20px rgba(212, 169, 169, 0.12);
$shadow-hover: 0 8px 32px rgba(212, 169, 169, 0.2);

.product-list-page {
  min-height: 100vh;
  background: $color-bg;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
}

.main-content {
  padding: 32px 0 56px;
}

.page-shell {
  width: min(1400px, calc(100% - 80px));
  margin: 0 auto;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 2px solid rgba(216, 169, 169, 0.2);

  h1 {
    margin: 0;
    color: $color-text;
    font-size: 32px;
    font-weight: 700;
    letter-spacing: 0.5px;
  }

  p {
    margin: 8px 0 0;
    color: $color-text-light;
    font-size: 15px;
  }

  .result-count {
    color: $color-text-muted;
    font-size: 14px;
    font-weight: 500;
  }
}

.catalog-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  align-items: start;
  gap: 28px;
}

.filter-panel {
  position: sticky;
  top: 100px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
}

.filter-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  border-bottom: 1px solid rgba(216, 169, 169, 0.15);
  color: $color-text;
  font-size: 17px;
  font-weight: 600;
}

.filter-group {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(216, 169, 169, 0.1);

  h2 {
    margin: 0 0 14px;
    color: $color-text;
    font-size: 15px;
    font-weight: 600;
  }

  :deep(.el-select) {
    width: 100%;
  }
}

.price-filter {
  padding-bottom: 18px;

  :deep(.el-slider) {
    margin: 4px 6px;
    width: calc(100% - 12px);
  }
}

.price-labels {
  display: flex;
  justify-content: space-between;
  color: $color-text-muted;
  font-size: 13px;
  margin-top: 8px;
}

.stock-filter {
  padding: 18px 24px;
}

.product-results {
  min-width: 0;
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 70px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
}

.list-search {
  width: min(380px, 40%);
  flex: 0 1 380px;
}

.sort-control {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
  color: $color-text-light;
  font-size: 13px;

  > span {
    flex: none;
    font-weight: 500;
  }

  :deep(.el-radio-group) {
    flex-wrap: wrap;
    justify-content: flex-end;

    .el-radio-button__inner {
      border-radius: 10px !important;
      border: 1px solid rgba(216, 169, 169, 0.2);
      transition: all 0.3s;

      &:hover {
        background: rgba(216, 169, 169, 0.1);
      }
    }

    .el-radio-button__original-radio:checked + .el-radio-button__inner {
      background: $color-accent;
      border-color: $color-accent;
    }
  }
}

.loading-panel {
  min-height: 580px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: $shadow-card;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.product-card {
  min-width: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);

  &:hover,
  &:focus-visible {
    transform: translateY(-8px) scale(1.02);
    box-shadow: $shadow-hover;
    outline: 0;
  }

  &:active {
    transform: translateY(-4px) scale(0.98);
  }
}

.product-image {
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: $color-bg-warm;
  position: relative;

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s;
  }
}

.product-card:hover .product-image img {
  transform: scale(1.08);
}

.product-info {
  padding: 20px;
}

.product-info h3 {
  height: 44px;
  margin: 0 0 12px;
  overflow: hidden;
  color: $color-text;
  font-size: 16px;
  font-weight: 600;
  line-height: 22px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.review-line {
  display: flex;
  align-items: center;
  min-height: 20px;
  gap: 8px;
  white-space: nowrap;

  :deep(.el-rate) {
    height: 18px;
  }

  :deep(.el-rate__icon) {
    margin-right: 0;
    font-size: 14px;
  }

  .score {
    color: #D8A9A9;
    font-size: 13px;
    font-weight: 600;
  }

  .review-count {
    color: $color-text-muted;
    font-size: 12px;
  }
}

.product-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 14px;

  strong {
    min-width: 0;
    overflow: hidden;
    color: $color-accent-dark;
    font-size: 22px;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-weight: 700;

    small {
      margin-right: 2px;
      font-size: 14px;
    }
  }

  > span {
    flex: none;
    color: $color-text-muted;
    font-size: 13px;
  }
}

.product-actions {
  margin-top: 14px;
  display: flex;
  justify-content: center;

  .el-button {
    width: 100%;
    border-radius: 12px;
    font-weight: 500;
    transition: all 0.3s;

    &:hover {
      transform: scale(1.05);
    }

    &:active {
      transform: scale(0.95);
    }
  }
}

:deep(.el-pagination) {
  justify-content: center;
  margin-top: 36px;

  .el-pager li {
    border-radius: 10px;
    margin: 0 4px;
    transition: all 0.3s;

    &:hover {
      background: rgba(216, 169, 169, 0.1);
    }

    &.is-active {
      background: $color-accent;
    }
  }
}

@media (max-width: 1200px) {
  .catalog-layout {
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 20px;
  }

  .result-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .list-search {
    width: 100%;
    max-width: none;
    flex-basis: auto;
  }

  .sort-control {
    justify-content: space-between;
  }
}

@media (max-width: 760px) {
  .main-content {
    padding-top: 20px;
  }

  .page-shell {
    width: min(100% - 32px, 600px);
  }

  .page-heading {
    align-items: start;

    h1 {
      font-size: 26px;
    }
  }

  .catalog-layout {
    display: block;
  }

  .filter-panel {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin-bottom: 20px;
  }

  .filter-title {
    grid-column: 1 / -1;
  }

  .filter-group {
    min-width: 0;
    border-right: 1px solid rgba(216, 169, 169, 0.1);
  }

  .stock-filter {
    display: flex;
    align-items: center;
  }

  .result-toolbar {
    padding: 16px;
  }

  .sort-control {
    align-items: flex-start;
    flex-direction: column;

    :deep(.el-radio-group) {
      justify-content: flex-start;
      flex-wrap: nowrap;
      width: 100%;
      overflow-x: auto;
      padding-bottom: 4px;
    }
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .product-info {
    padding: 16px;
  }

  .product-meta {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}

@media (max-width: 390px) {
  .filter-panel {
    grid-template-columns: 1fr;
  }

  .filter-title {
    grid-column: 1;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .product-meta {
    flex-direction: row;
    align-items: baseline;
  }
}
</style>

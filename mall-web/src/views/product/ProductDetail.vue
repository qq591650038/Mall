<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, getRelatedProducts } from '@/api/product'
import { getProductReviews, getProductReviewSummary } from '@/api/review'
import { addFavorite, isFavorite, removeFavorite } from '@/api/common'
import { addBrowseHistory, getBrowseHistoryList } from '@/api/browseHistory'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import type { Product, ProductDetailVO, ProductSkuVO, Review, BrowseHistory } from '@/types'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref<ProductDetailVO | null>(null)
const loading = ref(false)
const selectedSku = ref<ProductSkuVO | null>(null)
const quantity = ref(1)
const activeImage = ref(0)
const activeTab = ref('detail')
const reviews = ref<Review[]>([])
const reviewSummary = ref({ total: 0, good: 0, average: 0, ratingCounts: {} as Record<string, number> })
const reviewPage = ref(1)
const reviewTotal = ref(0)
const reviewsLoading = ref(false)
const reviewFilter = ref<'all' | 'good' | 'middle' | 'bad' | 'images'>('all')
const favorite = ref(false)
const favoriteLoading = ref(false)
const recentProducts = ref<BrowseHistory[]>([])
const relatedProducts = ref<Product[]>([])

async function loadDetail() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const data = await getProductDetail(id)
    product.value = data
    relatedProducts.value = await getRelatedProducts(id).catch(() => [])
    if (data.skus && data.skus.length) {
      selectedSku.value = data.skus[0]
    }
    if (userStore.isLoggedIn) {
      await Promise.all([isFavorite(id).then(value => { favorite.value = value }), addBrowseHistory(id)])
      recentProducts.value = (await getBrowseHistoryList()).filter(item => item.productId !== id).slice(0, 5)
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function loadReviews(page = 1) {
  if (!product.value) return
  reviewsLoading.value = true
  try {
    const [summary, result] = await Promise.all([
      getProductReviewSummary(product.value.id),
      getProductReviews(product.value.id, {
        current: page, size: 5,
        ratingType: reviewFilter.value === 'good' ? 3 : reviewFilter.value === 'middle' ? 2 : reviewFilter.value === 'bad' ? 1 : undefined,
        hasImages: reviewFilter.value === 'images' ? true : undefined
      })
    ])
    reviewSummary.value = summary
    reviews.value = result.list || []
    reviewTotal.value = result.total || 0
    reviewPage.value = page
  } finally {
    reviewsLoading.value = false
  }
}

function changeReviewFilter(filter: typeof reviewFilter.value) { reviewFilter.value = filter; loadReviews(1) }
async function toggleProductFavorite() {
  if (!userStore.isLoggedIn) { router.push({ name: 'Login', query: { redirect: route.fullPath } }); return }
  if (!product.value) return
  favoriteLoading.value = true
  try {
    if (favorite.value) await removeFavorite(product.value.id)
    else await addFavorite(product.value.id)
    favorite.value = !favorite.value
    ElMessage.success(favorite.value ? '收藏成功' : '已取消收藏')
  } finally { favoriteLoading.value = false }
}

onMounted(loadDetail)
watch(() => route.params.id, loadDetail)
watch(activeTab, (tab) => { if (tab === 'reviews' && !reviews.value.length) loadReviews() })

const images = computed(() => {
  if (!product.value) return []
  if (product.value.images?.length) return product.value.images
  return product.value.mainImage ? [product.value.mainImage] : []
})

function selectSku(sku: ProductSkuVO) {
  selectedSku.value = sku
}

function prevQty() { if (quantity.value > 1) quantity.value-- }
function nextQty() {
  const max = selectedSku.value?.stock || 99
  if (quantity.value < max) quantity.value++
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  await cartStore.addItem(selectedSku.value.id, quantity.value)
}

function buyNow() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (!selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  const items = [{
    productId: product.value!.id,
    skuId: selectedSku.value.id,
    quantity: quantity.value
  }]
  sessionStorage.setItem('checkout_items', JSON.stringify(items))
  router.push({ name: 'OrderCreate' })
}
</script>

<template>
  <div class="product-detail-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <template v-if="product">
          <div class="detail-layout">
            <div class="images-section">
              <div class="main-image">
                <img :src="images[activeImage]" :alt="product.name" />
              </div>
              <div class="thumbnail-list" v-if="images.length > 1">
                <div
                  v-for="(img, idx) in images"
                  :key="idx"
                  class="thumbnail"
                  :class="{ active: activeImage === idx }"
                  @click="activeImage = idx"
                >
                  <img :src="img" :alt="''" />
                </div>
              </div>
            </div>
            <div class="info-section">
              <h1 class="product-name">{{ product.name }}</h1>
              <p class="product-subtitle" v-if="product.subtitle">{{ product.subtitle }}</p>
              <div class="price-section">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ (selectedSku?.price || product.price).toFixed(2) }}</span>
                <span v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice.toFixed(2) }}</span>
              </div>
              <div class="meta-section">
                <span>品牌：{{ product.brandName || '自有品牌' }}</span>
                <span>分类：{{ product.categoryName }}</span>
                <span>销量：{{ product.sales }}</span>
              </div>
              <div class="sku-section" v-if="product.skus?.length">
                <h3>规格选择</h3>
                <div class="sku-list">
                  <div
                    v-for="sku in product.skus"
                    :key="sku.id"
                    class="sku-item"
                    :class="{ active: selectedSku?.id === sku.id }"
                    @click="selectSku(sku)"
                  >
                    {{ sku.specInfo }}
                    <span class="sku-price">¥{{ sku.price.toFixed(2) }}</span>
                  </div>
                </div>
              </div>
              <div class="quantity-section">
                <span>数量</span>
                <div class="quantity-control">
                  <el-button @click="prevQty" :disabled="quantity <= 1">-</el-button>
                  <span class="qty-value">{{ quantity }}</span>
                  <el-button @click="nextQty">+</el-button>
                </div>
                <span class="stock-info">库存：{{ selectedSku?.stock || product.totalStock }} 件</span>
              </div>
              <div class="action-section">
                <el-button type="warning" size="large" class="action-btn" @click="addToCart">
                  加入购物车
                </el-button>
                <el-button type="danger" size="large" class="action-btn primary" @click="buyNow">
                  立即购买
                </el-button>
                <el-button size="large" :loading="favoriteLoading" @click="toggleProductFavorite">
                  <el-icon><StarFilled v-if="favorite" /><Star v-else /></el-icon>
                  {{ favorite ? '已收藏' : '收藏' }}
                </el-button>
              </div>
            </div>
          </div>
          <div class="detail-tabs">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="商品详情" name="detail">
                <div class="detail-content" v-html="product.description || '<p>暂无详情</p>'"></div>
              </el-tab-pane>
              <el-tab-pane label="规格参数" name="spec">
                <el-descriptions :column="2" border v-if="product">
                  <el-descriptions-item label="品牌">{{ product.brandName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="分类">{{ product.categoryName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="库存">{{ product.totalStock }}</el-descriptions-item>
                  <el-descriptions-item label="销量">{{ product.sales }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
              <el-tab-pane label="商品评价" name="reviews">
                <div class="reviews-content" v-loading="reviewsLoading">
                  <div class="review-summary">
                    <div class="summary-score"><strong>{{ reviewSummary.average.toFixed(1) }}</strong><span>平均星级</span></div>
                    <div class="summary-stat"><strong>{{ reviewSummary.total }}</strong><span>评价总数</span></div>
                    <div class="summary-stat good"><strong>{{ reviewSummary.good }}</strong><span>好评数（4-5星）</span></div>
                  </div>
                  <div class="rating-distribution" v-if="reviewSummary.total">
                    <div v-for="star in [5,4,3,2,1]" :key="star" class="rating-row">
                      <span>{{ star }} 星</span><el-progress :percentage="Math.round(((reviewSummary.ratingCounts[String(star)] || 0) / reviewSummary.total) * 100)" :show-text="false" /><span>{{ reviewSummary.ratingCounts[String(star)] || 0 }}</span>
                    </div>
                  </div>
                  <div class="review-filters">
                    <el-button v-for="item in [{key:'all',label:'全部'},{key:'good',label:'好评'},{key:'middle',label:'中评'},{key:'bad',label:'差评'},{key:'images',label:'有图'}]" :key="item.key" size="small" :type="reviewFilter === item.key ? 'primary' : 'default'" @click="changeReviewFilter(item.key as typeof reviewFilter)">{{ item.label }}</el-button>
                  </div>
                  <el-empty v-if="!reviewsLoading && !reviews.length" description="暂无评价" />
                  <div v-else class="review-list">
                    <article v-for="review in reviews" :key="review.id" class="review-item">
                      <div class="review-head"><span class="review-user">匿名用户</span><el-rate :model-value="review.rating" disabled text-color="#ff9900" /><time>{{ review.createTime?.slice(0, 10) }}</time></div>
                      <p class="review-text">{{ review.content }}</p>
                      <p v-if="review.reply" class="review-reply">商家回复：{{ review.reply }}</p>
                    </article>
                  </div>
                  <el-pagination v-if="reviewTotal > 5" class="review-pagination" background layout="prev, pager, next" :page-size="5" :current-page="reviewPage" :total="reviewTotal" @current-change="loadReviews" />
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
          <section v-if="recentProducts.length" class="recent-section">
            <h2>最近浏览</h2>
            <div class="recent-list">
              <div v-for="item in recentProducts" :key="item.productId" class="recent-item" @click="router.push({ name: 'ProductDetail', params: { id: item.productId } })">
                <img :src="item.product?.mainImage || '/favicon.svg'" alt="" />
                <div><p>{{ item.product?.name }}</p><strong>¥{{ item.product?.price?.toFixed(2) }}</strong></div>
              </div>
            </div>
          </section>
          <section v-if="relatedProducts.length" class="related-section"><h2>相似商品</h2><div class="related-list"><article v-for="item in relatedProducts" :key="item.id" @click="router.push(`/products/${item.id}`)"><img :src="item.mainImage || '/favicon.svg'" :alt="item.name" /><h3>{{ item.name }}</h3><div><strong>¥{{ item.price.toFixed(2) }}</strong><span>{{ (item.averageRating || 0).toFixed(1) }} 分</span></div></article></div></section>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.product-detail-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }

.detail-layout {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: grid;
  grid-template-columns: 480px 1fr;
  gap: 40px;
}

.images-section {
  .main-image {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 8px;
    overflow: hidden;
    background: #f9f9f9;
    img { width: 100%; height: 100%; object-fit: cover; }
  }

  .thumbnail-list {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    overflow-x: auto;
  }

  .thumbnail {
    width: 60px;
    height: 60px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    flex-shrink: 0;
    &.active { border-color: #ff6b35; }
    img { width: 100%; height: 100%; object-fit: cover; }
  }
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-name { font-size: 22px; color: #333; margin: 0; line-height: 1.4; }
.product-subtitle { color: #999; margin: 0; font-size: 14px; }

.price-section {
  background: #fff5f0;
  padding: 16px;
  border-radius: 8px;
  display: flex;
  align-items: baseline;
  gap: 8px;

  .price-symbol { color: #ff6b35; font-size: 18px; }
  .price-value { color: #ff6b35; font-size: 32px; font-weight: 700; }
  .original-price { color: #999; text-decoration: line-through; font-size: 14px; }
}

.meta-section {
  display: flex;
  gap: 24px;
  color: #999;
  font-size: 13px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.sku-section h3 { font-size: 14px; margin: 0 0 12px; color: #333; }
.sku-list { display: flex; flex-wrap: wrap; gap: 8px; }
.sku-item {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;

  &.active { border-color: #ff6b35; color: #ff6b35; background: #fff5f0; }
  .sku-price { color: #999; font-size: 12px; }
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 16px;

  .quantity-control {
    display: flex;
    align-items: center;
    border: 1px solid #ddd;
    border-radius: 4px;
    overflow: hidden;
    .qty-value { min-width: 40px; text-align: center; }
  }

  .stock-info { color: #999; font-size: 13px; }
}

.action-section { display: flex; gap: 16px; margin-top: 8px; }
.action-btn {
  min-width: 140px;
  font-size: 16px;
  &.primary { background: #ff6b35; border-color: #ff6b35; }
  &.primary:hover { background: #ff5722; border-color: #ff5722; }
}

.detail-tabs {
  margin-top: 24px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.detail-content { padding: 20px; line-height: 1.8; }
.reviews-content { padding: 20px; min-height: 180px; }
.review-summary { display: flex; gap: 56px; padding: 16px 24px; margin-bottom: 12px; background: #fff8f2; border-radius: 8px; }
.review-summary > div { display: flex; flex-direction: column; gap: 6px; color: #999; font-size: 13px; }
.review-summary strong { color: #ff6b35; font-size: 26px; }
.review-summary .good strong { color: #67c23a; }
.review-item { padding: 16px 0; border-bottom: 1px solid #f0f0f0; }
.review-head { display: flex; align-items: center; gap: 14px; color: #999; font-size: 13px; }
.review-user { color: #555; }
.review-text { margin: 10px 0 0; color: #333; line-height: 1.7; }
.review-reply { margin: 8px 0 0; padding: 8px 12px; color: #888; background: #f7f7f7; font-size: 13px; }
.review-pagination { margin-top: 16px; justify-content: center; }
.rating-distribution { width: 360px; margin: 16px 0; }
.rating-row { display: grid; grid-template-columns: 42px 1fr 30px; align-items: center; gap: 10px; margin: 6px 0; color: #777; font-size: 12px; }
.review-filters { display: flex; gap: 8px; margin: 16px 0 4px; }
.recent-section { margin-top: 24px; padding: 20px; background: #fff; border-radius: 8px; }
.recent-section h2 { margin: 0 0 16px; font-size: 17px; }
.recent-list { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.recent-item { display: flex; gap: 10px; min-width: 0; cursor: pointer; }
.recent-item img { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; }
.recent-item div { min-width: 0; }
.recent-item p { margin: 2px 0 8px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; font-size: 13px; color: #444; }
.recent-item strong { color: #ff6b35; font-size: 13px; }
.related-section { margin-top: 24px; padding: 20px; background: #fff; border-radius: 8px; }.related-section h2{margin:0 0 16px;font-size:17px}.related-list{display:grid;grid-template-columns:repeat(5,1fr);gap:14px}.related-list article{min-width:0;cursor:pointer}.related-list img{width:100%;aspect-ratio:1;object-fit:cover;border-radius:6px}.related-list h3{height:40px;margin:8px 0;font-size:13px;line-height:1.5;overflow:hidden}.related-list article>div{display:flex;justify-content:space-between;font-size:12px;color:#999}.related-list strong{color:#ff6b35}
</style>

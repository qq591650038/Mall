<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, getRelatedProducts } from '@/api/product'
import { getProductReviews, getProductReviewSummary } from '@/api/review'
import { addFavorite, isFavorite, removeFavorite } from '@/api/common'
import { addBrowseHistory, getBrowseHistoryList } from '@/api/browseHistory'
import { subscribeStock } from '@/api/stockSubscription'
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

async function subscribeArrival() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  if (!product.value) return
  await subscribeStock(product.value.id, selectedSku.value?.id)
  ElMessage.success('已订阅补货提醒')
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
                <el-button v-if="(selectedSku?.stock ?? product.totalStock) <= 0" size="large" @click="subscribeArrival">补货提醒</el-button>
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

.product-detail-page {
  background: $color-bg;
  min-height: 100vh;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
}

.main-content {
  padding: 32px 0;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 48px;
}

.detail-layout {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 40px;
  display: grid;
  grid-template-columns: 520px 1fr;
  gap: 48px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);
}

.images-section {
  .main-image {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 20px;
    overflow: hidden;
    background: $color-bg-warm;
    box-shadow: $shadow-card;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s;
    }

    &:hover img {
      transform: scale(1.05);
    }
  }

  .thumbnail-list {
    display: flex;
    gap: 12px;
    margin-top: 16px;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .thumbnail {
    width: 70px;
    height: 70px;
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    border: 3px solid transparent;
    flex-shrink: 0;
    transition: all 0.3s;
    background: rgba(255, 255, 255, 0.8);

    &:hover {
      transform: translateY(-4px);
    }

    &.active {
      border-color: $color-accent;
      box-shadow: 0 4px 12px rgba(216, 169, 169, 0.3);
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.product-name {
  font-size: 32px;
  color: $color-text;
  margin: 0;
  line-height: 1.4;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.product-subtitle {
  color: $color-text-light;
  margin: 0;
  font-size: 16px;
  font-weight: 400;
}

.price-section {
  background: linear-gradient(135deg, rgba(245, 230, 211, 0.6) 0%, rgba(255, 249, 245, 0.8) 100%);
  padding: 24px;
  border-radius: 16px;
  display: flex;
  align-items: baseline;
  gap: 12px;
  box-shadow: inset 0 2px 8px rgba(212, 169, 169, 0.1);

  .price-symbol {
    color: $color-accent-dark;
    font-size: 22px;
    font-weight: 600;
  }

  .price-value {
    color: $color-accent-dark;
    font-size: 42px;
    font-weight: 700;
    font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  }

  .original-price {
    color: $color-text-muted;
    text-decoration: line-through;
    font-size: 18px;
    margin-left: 12px;
  }
}

.meta-section {
  display: flex;
  gap: 28px;
  color: $color-text-light;
  font-size: 15px;
  padding: 16px 0;
  border-top: 2px solid rgba(216, 169, 169, 0.15);
  border-bottom: 2px solid rgba(216, 169, 169, 0.15);
  font-weight: 500;
}

.sku-section h3 {
  font-size: 17px;
  margin: 0 0 16px;
  color: $color-text;
  font-weight: 600;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.sku-item {
  padding: 12px 20px;
  border: 2px solid rgba(216, 169, 169, 0.2);
  border-radius: 12px;
  cursor: pointer;
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.8);

  &:hover {
    background: rgba(216, 169, 169, 0.1);
    transform: translateY(-2px);
  }

  &.active {
    border-color: $color-accent;
    color: $color-accent-dark;
    background: rgba(216, 169, 169, 0.15);
    box-shadow: 0 4px 12px rgba(216, 169, 169, 0.2);
  }

  .sku-price {
    color: $color-text-muted;
    font-size: 13px;
    font-weight: 500;
  }
}

.quantity-section {
  display: flex;
  align-items: center;
  gap: 20px;

  > span {
    color: $color-text;
    font-weight: 600;
    font-size: 17px;
  }

  .quantity-control {
    display: flex;
    align-items: center;
    border: 2px solid rgba(216, 169, 169, 0.2);
    border-radius: 12px;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.8);

    .el-button {
      border-radius: 0;
      border: none;
      font-size: 18px;
      padding: 12px 16px;

      &:hover {
        background: rgba(216, 169, 169, 0.1);
      }
    }

    .qty-value {
      min-width: 50px;
      text-align: center;
      font-size: 18px;
      font-weight: 600;
      color: $color-text;
    }
  }

  .stock-info {
    color: $color-text-muted;
    font-size: 14px;
    font-weight: 500;
  }
}

.action-section {
  display: flex;
  gap: 16px;
  margin-top: 8px;

  .el-button {
    border-radius: 14px;
    font-weight: 600;
    font-size: 16px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
    }

    &:active {
      transform: translateY(0);
    }
  }

  .action-btn {
    min-width: 160px;

    &.primary {
      background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
      border-color: $color-accent;
    }

    &.primary:hover {
      background: linear-gradient(135deg, $color-accent-dark 0%, $color-accent 100%);
      border-color: $color-accent-dark;
    }
  }
}

.detail-tabs {
  margin-top: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 24px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);

  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__item) {
    font-size: 17px;
    font-weight: 500;
    color: $color-text-light;

    &.is-active {
      color: $color-accent-dark;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: $color-accent;
  }
}

.detail-content {
  padding: 24px;
  line-height: 1.9;
  font-size: 16px;
  color: $color-text-light;
}

.reviews-content {
  padding: 24px;
  min-height: 200px;
}

.review-summary {
  display: flex;
  gap: 64px;
  padding: 20px 28px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(245, 230, 211, 0.5) 0%, rgba(255, 249, 245, 0.7) 100%);
  border-radius: 16px;
  box-shadow: inset 0 2px 8px rgba(212, 169, 169, 0.1);

  > div {
    display: flex;
    flex-direction: column;
    gap: 8px;
    color: $color-text-light;
    font-size: 15px;
  }

  strong {
    color: $color-accent-dark;
    font-size: 32px;
  }

  .good strong {
    color: #52c41a;
  }
}

.rating-distribution {
  width: 400px;
  margin: 20px 0;
}

.rating-row {
  display: grid;
  grid-template-columns: 50px 1fr 40px;
  align-items: center;
  gap: 12px;
  margin: 8px 0;
  color: $color-text-light;
  font-size: 14px;
}

.review-filters {
  display: flex;
  gap: 10px;
  margin: 20px 0 8px;

  .el-button {
    border-radius: 12px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

.review-item {
  padding: 20px 0;
  border-bottom: 1px solid rgba(216, 169, 169, 0.15);
}

.review-head {
  display: flex;
  align-items: center;
  gap: 16px;
  color: $color-text-muted;
  font-size: 14px;
}

.review-user {
  color: $color-text;
  font-weight: 500;
}

.review-text {
  margin: 12px 0 0;
  color: $color-text;
  line-height: 1.8;
  font-size: 15px;
}

.review-reply {
  margin: 12px 0 0;
  padding: 12px 16px;
  color: $color-text-light;
  background: rgba(245, 230, 211, 0.4);
  font-size: 14px;
  border-radius: 12px;
  border-left: 4px solid $color-accent;
}

.review-pagination {
  margin-top: 20px;
  justify-content: center;

  :deep(.el-pager li) {
    border-radius: 10px;
    margin: 0 4px;

    &.is-active {
      background: $color-accent;
    }
  }
}

.recent-section,
.related-section {
  margin-top: 32px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: $shadow-card;
  border: 1px solid rgba(216, 169, 169, 0.1);

  h2 {
    margin: 0 0 20px;
    font-size: 20px;
    color: $color-text;
    font-weight: 700;
  }
}

.recent-list,
.related-list {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.recent-item,
.related-list article {
  display: flex;
  gap: 12px;
  min-width: 0;
  cursor: pointer;
  transition: all 0.3s;
  padding: 12px;
  border-radius: 16px;

  &:hover {
    background: rgba(216, 169, 169, 0.1);
    transform: translateY(-4px);
  }

  img {
    width: 70px;
    height: 70px;
    object-fit: cover;
    border-radius: 12px;
    background: $color-bg-warm;
  }

  div {
    min-width: 0;
  }

  p,
  h3 {
    margin: 0 0 8px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    font-size: 14px;
    color: $color-text;
    font-weight: 500;
    line-height: 1.4;
    height: 20px;
  }

  strong {
    color: $color-accent-dark;
    font-size: 16px;
    font-weight: 700;
  }
}

.related-list article > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;

  span {
    color: $color-text-muted;
    font-size: 13px;
  }
}

@media (max-width: 1200px) {
  .detail-layout {
    grid-template-columns: 1fr;
    padding: 32px;
  }

  .recent-list,
  .related-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 24px;
  }

  .detail-layout {
    padding: 24px;
    gap: 32px;
  }

  .product-name {
    font-size: 26px;
  }

  .price-value {
    font-size: 36px;
  }

  .recent-list,
  .related-list {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-section {
    flex-wrap: wrap;

    .action-btn {
      min-width: 140px;
    }
  }
}
</style>

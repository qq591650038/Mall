<script setup lang="ts">import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getActiveBanners, getCategoryList, getAvailableCoupons } from '@/api/common';
import { getProductPage, getRecommendations } from '@/api/product';
import { getBrowseHistoryList } from '@/api/browseHistory';
import { getActiveActivities } from '@/api/marketing';
import { useUserStore } from '@/stores/user';
import type { Banner, BrowseHistory, Category, Coupon, Product } from '@/types';
import AppHeader from '@/layouts/AppHeader.vue';
import AppFooter from '@/layouts/AppFooter.vue';
const router = useRouter();
const userStore = useUserStore();
const banners = ref<Banner[]>([]);
const categories = ref<Category[]>([]);
const hotProducts = ref<Product[]>([]);
const newProducts = ref<Product[]>([]);
const recommended = ref<Product[]>([]);
const topRated = ref<Product[]>([]);
const recent = ref<BrowseHistory[]>([]);
const coupons = ref<Coupon[]>([]);
const activeActivities = ref<any[]>([]);
const currentBanner = ref(0);
const loading = ref(true);
const loadError = ref(false);
onMounted(async () => {
 loading.value = true;
 try {
 const [b, c, hot, nw, rec, rated, couponList, history, activities] = await Promise.all([
 getActiveBanners().catch(() => []),
 getCategoryList().catch(() => []),
 getProductPage({ current: 1, size: 10, sort: 'sales' }).catch(() => ({ list: [] })),
 getProductPage({ current: 1, size: 10, sort: 'newest' }).catch(() => ({ list: [] })),
 getRecommendations().catch(() => []),
 getProductPage({ current: 1, size: 5, minRating: 4, sort: 'rating' }).catch(() => ({ list: [] })),
 getAvailableCoupons().catch(() => []),
 userStore.isLoggedIn ? getBrowseHistoryList().catch(() => []) : Promise.resolve([]),
 getActiveActivities().catch(() => [])
 ]);
 banners.value = b;
 categories.value = c;
 hotProducts.value = hot.list || [];
newProducts.value = nw.list || [];
 recommended.value = rec || [];
 topRated.value = rated.list || [];
 coupons.value = couponList.slice(0, 3);
 recent.value = history.slice(0, 5);
 activeActivities.value = activities.slice(0, 4);
 }
 catch {
 loadError.value = true;
 } finally { loading.value = false;
 }
});
function goProductDetail(id: number) {
  router.push({ name: 'ProductDetail', params: { id } });
}
function goCategory(categoryId: number) {
  router.push({ name: 'ProductList', query: { categoryId: String(categoryId) } });
}
function goActivityDetail(id: number) {
  router.push({ name: 'MarketingDetail', params: { id } })
}
function getActivityTypeLabel(type: string) {
  const map: Record<string, string> = { LIMITED_DISCOUNT: '限时折扣', FULL_REDUCTION: '满减', FLASH_SALE: '秒杀', GROUP_BUY: '拼团' }
  return map[type] || type
}
function getActivityTypeIcon(type: string) {
  const map: Record<string, string> = { LIMITED_DISCOUNT: '⏰', FULL_REDUCTION: '🎁', FLASH_SALE: '⚡', GROUP_BUY: '👥' }
  return map[type] || '📌'
}
function goBannerLink(linkUrl?: string) {
  if (linkUrl) {
    window.location.href = linkUrl
  }
}
</script>

<template>
  <div class="home-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <el-skeleton v-if="loading" :rows="6" animated class="home-skeleton" />
        <el-alert v-else-if="loadError" title="首页内容加载失败，请稍后重试" type="warning" show-icon :closable="false" />
        <el-carousel
          v-else-if="banners.length"
          v-model="currentBanner"
          height="340px"
          class="banner-carousel"
          :interval="4000"
        >
          <el-carousel-item v-for="banner in banners" :key="banner.id">
            <div
              class="banner-item"
              :style="{ backgroundImage: `url(${banner.imageUrl})` }"
              @click="goBannerLink(banner.linkUrl)"
            >
              <div class="banner-overlay">
                <h2>{{ banner.title }}</h2>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>

        <div v-if="!loading" class="category-nav">
          <div
            v-for="cat in categories.slice(0, 10)"
            :key="cat.id"
            class="category-item"
            @click="goCategory(cat.id)"
          >
            <div class="category-icon">{{ cat.icon || '📦' }}</div>
            <span>{{ cat.name }}</span>
          </div>
        </div>

        <section v-if="!loading" class="product-section">
          <div class="section-header">
            <h2>🔥 热销商品</h2>
            <router-link to="/products" class="more-link">查看更多 →</router-link>
          </div>
          <div class="product-grid">
            <div
              v-for="product in hotProducts"
              :key="product.id"
              class="product-card"
              @click="goProductDetail(product.id)"
            >
              <div class="product-image">
                <img :src="product.mainImage || '/favicon.svg'" :alt="product.name" loading="lazy" decoding="async" @error="($event.target as HTMLImageElement).src = '/favicon.svg'" />
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-subtitle" v-if="product.subtitle">{{ product.subtitle }}</p>
                <div class="product-bottom">
                  <span class="price">¥{{ product.price.toFixed(2) }}</span>
                  <span class="sales">已售 {{ product.sales }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section v-if="!loading && recommended.length" class="product-section"><div class="section-header"><h2>猜你喜欢</h2><router-link to="/products" class="more-link">查看更多</router-link></div><div class="product-grid"><div v-for="product in recommended" :key="product.id" class="product-card" @click="goProductDetail(product.id)"><div class="product-image"><img :src="product.mainImage || '/favicon.svg'" :alt="product.name" /></div><div class="product-info"><h3 class="product-name">{{ product.name }}</h3><div class="product-bottom"><span class="price">¥{{ product.price.toFixed(2) }}</span><span class="sales">{{ (product.averageRating || 0).toFixed(1) }} 分</span></div></div></div></div></section>
        <section v-if="!loading && recent.length" class="product-section"><div class="section-header"><h2>最近浏览</h2><router-link to="/browse-history" class="more-link">全部记录</router-link></div><div class="product-grid"><div v-for="item in recent" :key="item.productId" class="product-card" @click="goProductDetail(item.productId)"><div class="product-image"><img :src="item.product?.mainImage || '/favicon.svg'" alt="" /></div><div class="product-info"><h3 class="product-name">{{ item.product?.name }}</h3><div class="product-bottom"><span class="price">¥{{ item.product?.price?.toFixed(2) }}</span></div></div></div></div></section>
        <section v-if="!loading && topRated.length" class="product-section"><div class="section-header"><h2>高评分商品</h2></div><div class="product-grid"><div v-for="product in topRated" :key="product.id" class="product-card" @click="goProductDetail(product.id)"><div class="product-image"><img :src="product.mainImage || '/favicon.svg'" :alt="product.name" /></div><div class="product-info"><h3 class="product-name">{{ product.name }}</h3><el-rate :model-value="product.averageRating || 0" disabled allow-half /><div class="product-bottom"><span class="price">¥{{ product.price.toFixed(2) }}</span><span class="sales">{{ product.reviewCount || 0 }} 条评价</span></div></div></div></div></section>
        <section v-if="!loading && coupons.length" class="product-section"><div class="section-header"><h2>限时优惠</h2><router-link to="/coupons" class="more-link">领取优惠券</router-link></div><div class="coupon-strip"><div v-for="coupon in coupons" :key="coupon.id"><strong>¥{{ coupon.value }}</strong><span>{{ coupon.name }}</span><small>满 {{ coupon.minAmount || 0 }} 可用</small></div></div></section>
        <section v-if="!loading && activeActivities.length" class="product-section">
          <div class="section-header">
            <h2>🎉 热门活动</h2>
            <router-link to="/marketing/activities" class="more-link">查看全部 →</router-link>
          </div>
          <div class="activity-grid">
            <div v-for="activity in activeActivities" :key="activity.id" class="activity-card" @click="goActivityDetail(activity.id)">
              <div class="activity-badge">{{ getActivityTypeIcon(activity.type) }} {{ getActivityTypeLabel(activity.type) }}</div>
              <h3 class="activity-name">{{ activity.name }}</h3>
              <p class="activity-desc">{{ activity.description || '' }}</p>
              <div class="activity-footer">
                <span class="activity-time">{{ activity.startTime?.slice(5, 16) }} - {{ activity.endTime?.slice(5, 16) }}</span>
                <span class="activity-status" :class="{ active: activity.status === 1 }">
                  {{ activity.status === 1 ? '进行中' : activity.status === 0 ? '未开始' : '已结束' }}
                </span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="!loading" class="product-section">
          <div class="section-header">
            <h2>✨ 新品上架</h2>
            <router-link to="/products" class="more-link">查看更多 →</router-link>
          </div>
          <div class="product-grid">
            <div
              v-for="product in newProducts"
              :key="product.id"
              class="product-card"
              @click="goProductDetail(product.id)"
            >
              <div class="product-image">
                <img :src="product.mainImage || '/favicon.svg'" :alt="product.name" loading="lazy" decoding="async" @error="($event.target as HTMLImageElement).src = '/favicon.svg'" />
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-subtitle" v-if="product.subtitle">{{ product.subtitle }}</p>
                <div class="product-bottom">
                  <span class="price">¥{{ product.price.toFixed(2) }}</span>
                  <span class="sales">库存 {{ product.totalStock }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>
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
$shadow-soft: 0 4px 20px rgba(212, 169, 169, 0.15);
$shadow-hover: 0 8px 30px rgba(212, 169, 169, 0.25);

@mixin texture-overlay {
  position: relative;
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.03'/%3E%3C/svg%3E");
    pointer-events: none;
    opacity: 0.4;
  }
}

.home-page {
  background: $color-bg;
  min-height: 100vh;
  font-family: 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif;
  @include texture-overlay;
}

.main-content {
  padding: 32px 0;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 48px;
}

.home-skeleton {
  background: $color-bg-warm;
  border-radius: 24px;
  padding: 32px;
  min-height: 340px;
}

.banner-carousel {
  border-radius: 24px;
  overflow: hidden;
  margin-bottom: 40px;
  box-shadow: $shadow-soft;

  &::before {
    content: '今天穿什么？你说了算';
    position: absolute;
    right: 8%;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    color: rgba(255, 255, 255, 0.9);
    font-weight: 600;
    font-style: italic;
    z-index: 10;
    pointer-events: none;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }
}

.banner-item {
  height: 100%;
  background-size: cover;
  background-position: 65% 50%;
  display: flex;
  align-items: flex-end;
  padding: 48px;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(216, 169, 169, 0.25) 0%, transparent 60%),
                linear-gradient(to top, rgba(0, 0, 0, 0.35) 0%, transparent 50%);
  }

  .banner-overlay {
    background: linear-gradient(135deg, rgba(196, 144, 143, 0.85) 0%, rgba(216, 169, 169, 0.75) 100%);
    backdrop-filter: blur(10px);
    padding: 20px 36px;
    border-radius: 16px;
    color: #fff;
    text-align: left;
    z-index: 2;
    box-shadow: 0 8px 24px rgba(196, 144, 143, 0.3);
    border: 1px solid rgba(255, 255, 255, 0.2);
    h2 {
      font-size: 28px;
      margin: 0;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
  }
}

.category-nav {
  background: transparent;
  padding: 0;
  margin-bottom: 48px;
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 12px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px 8px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(216, 169, 169, 0.2);
    transform: translateY(-8px) scale(1.05);
    box-shadow: $shadow-hover;
  }

  .category-icon {
    width: 52px;
    height: 52px;
    background: linear-gradient(135deg, $color-bg-warm 0%, $color-accent 100%);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    transition: transform 0.3s;
  }

  &:hover .category-icon {
    transform: rotate(-8deg) scale(1.1);
  }

  span {
    font-size: 14px;
    color: $color-text;
    font-weight: 500;
  }
}

.product-section {
  margin-bottom: 56px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid rgba(216, 169, 169, 0.2);

  h2 {
    font-size: 26px;
    color: $color-text;
    margin: 0;
    font-weight: 700;
    letter-spacing: 0.5px;
  }

  .more-link {
    color: $color-accent-dark;
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    padding: 8px 20px;
    border-radius: 20px;
    background: rgba(216, 169, 169, 0.15);
    transition: all 0.3s;

    &:hover {
      background: $color-accent;
      color: #fff;
      transform: scale(1.05);
    }

    &:active {
      transform: scale(0.95);
    }
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 24px;

  .product-card:nth-child(3n+1) {
    transform: translateY(12px);
  }

  .product-card:nth-child(3n+2) {
    transform: translateY(0);
  }

  .product-card:nth-child(3n+3) {
    transform: translateY(-10px);
  }
}

.product-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    transition: left 0.6s;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-12px) scale(1.02);
    box-shadow: $shadow-hover;
  }

  &:hover::before {
    left: 100%;
  }

  &:active {
    transform: translateY(-8px) scale(0.95);
  }

  .product-image {
    aspect-ratio: 1;
    overflow: hidden;
    background: $color-bg-warm;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.5s;
      filter: brightness(0.95);
    }
  }

  &:hover img {
    transform: scale(1.08);
    filter: brightness(1);
  }

  .product-info {
    padding: 16px 20px 20px;
  }

  .product-name {
    font-size: 15px;
    color: $color-text;
    margin: 0 0 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.5;
    height: 45px;
    font-weight: 500;
  }

  .product-subtitle {
    font-size: 13px;
    color: $color-text-muted;
    margin: 0 0 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .product-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .price {
      color: $color-accent-dark;
      font-size: 20px;
      font-weight: 700;
      font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
    }

    .sales {
      color: $color-text-muted;
      font-size: 13px;
    }
  }
}

.coupon-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  > div {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 8px 20px;
    align-items: center;
    padding: 24px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-left: 5px solid $color-accent;
    border-radius: 16px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-6px);
      box-shadow: $shadow-hover;
    }

    strong {
      grid-row: 1 / 3;
      color: $color-accent-dark;
      font-size: 36px;
      font-weight: 700;
    }

    span {
      font-weight: 600;
      color: $color-text;
      font-size: 16px;
    }

    small {
      color: $color-text-muted;
      font-size: 13px;
    }
  }
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.activity-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-10px) scale(1.02);
    box-shadow: $shadow-hover;
  }

  &:active {
    transform: translateY(-6px) scale(0.96);
  }

  .activity-badge {
    padding: 14px 18px;
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    background: linear-gradient(135deg, $color-accent 0%, $color-accent-dark 100%);
    display: flex;
    align-items: center;
    gap: 6px;
    letter-spacing: 0.5px;
  }

  .activity-name {
    font-size: 17px;
    color: $color-text;
    margin: 16px 18px 10px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .activity-desc {
    font-size: 14px;
    color: $color-text-light;
    margin: 0 18px 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.5;
    min-height: 42px;
  }

  .activity-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 18px 16px;
    font-size: 13px;
  }

  .activity-time {
    color: $color-text-muted;
  }

  .activity-status {
    color: $color-text-muted;
    padding: 4px 12px;
    border-radius: 12px;
    background: rgba(216, 169, 169, 0.15);
    font-weight: 500;

    &.active {
      color: #52c41a;
      background: rgba(82, 196, 26, 0.1);
    }
  }
}

@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .activity-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);

    .product-card:nth-child(4n) {
      grid-column: span 1;
    }
  }

  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .category-nav {
    grid-template-columns: repeat(5, 1fr);
  }
}

@media (max-width: 768px) {
  .container {
    padding: 0 24px;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .coupon-strip {
    grid-template-columns: 1fr;
  }

  .category-nav {
    grid-template-columns: repeat(5, 1fr);
  }

  .banner-item::before {
    display: none;
  }
}
</style>

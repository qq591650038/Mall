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
  router.push({ name: 'ProductList', query: { categoryId: String(categoryId) } })
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
.home-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 20px 0; }
.home-skeleton { background: #fff; border-radius: 12px; padding: 24px; min-height: 340px; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }

.banner-carousel {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.banner-item {
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  .banner-overlay {
    background: rgba(0, 0, 0, 0.4);
    padding: 24px 48px;
    border-radius: 12px;
    color: #fff;
    text-align: center;
    h2 { font-size: 28px; margin: 0; }
  }
}

.category-nav {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { background: #fff5f0; transform: translateY(-2px); }

  .category-icon {
    width: 48px;
    height: 48px;
    background: #fff5f0;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
  }

  span { font-size: 13px; color: #666; }
}

.product-section { margin-bottom: 40px; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 { font-size: 20px; color: #333; margin: 0; }
  .more-link { color: #ff6b35; text-decoration: none; font-size: 14px; }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .product-image {
    aspect-ratio: 1;
    overflow: hidden;
    background: #f9f9f9;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s;
    }
  }

  &:hover img { transform: scale(1.05); }

  .product-info { padding: 12px; }

  .product-name {
    font-size: 14px;
    color: #333;
    margin: 0 0 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
    height: 40px;
  }

  .product-subtitle {
    font-size: 12px;
    color: #999;
    margin: 0 0 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .product-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .price { color: #ff6b35; font-size: 16px; font-weight: 600; }
    .sales { color: #999; font-size: 12px; }
  }
}
.coupon-strip{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.coupon-strip>div{display:grid;grid-template-columns:auto 1fr;gap:4px 16px;align-items:center;padding:18px;background:#fff;border-left:4px solid #ff6b35;border-radius:6px}.coupon-strip strong{grid-row:1/3;color:#ff6b35;font-size:25px}.coupon-strip span{font-weight:600}.coupon-strip small{color:#999}
.activity-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}
.activity-card{background:#fff;border-radius:12px;overflow:hidden;cursor:pointer;transition:all 0.25s;display:flex;flex-direction:column}
.activity-card:hover{transform:translateY(-4px);box-shadow:0 8px 24px rgba(0,0,0,0.1)}
.activity-card .activity-badge{padding:10px 14px;font-size:13px;font-weight:500;color:#fff;background:linear-gradient(135deg,#ff6b35,#ff4d4f);display:flex;align-items:center;gap:4px}
.activity-card .activity-name{font-size:15px;color:#333;margin:12px 14px 6px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.activity-card .activity-desc{font-size:12px;color:#999;margin:0 14px 10px;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;line-height:1.4;min-height:34px}
.activity-card .activity-footer{display:flex;justify-content:space-between;align-items:center;padding:0 14px 12px;font-size:12px}
.activity-card .activity-time{color:#999}
.activity-card .activity-status{color:#999;padding:2px 8px;border-radius:4px;background:#f5f5f5}
.activity-card .activity-status.active{color:#52c41a;background:#f6ffed}
@media(max-width:768px){.activity-grid{grid-template-columns:repeat(2,1fr)}}
</style>

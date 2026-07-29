<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppFooter from '@/layouts/AppFooter.vue'
import AppHeader from '@/layouts/AppHeader.vue'
import { getLeaderboard, type Leaderboard, type UserLeaderboardEntry } from '@/api/leaderboard'

const router = useRouter()
const loading = ref(true)
const board = ref<Leaderboard>({ points: [], spending: [], products: [] })
const activeTab = ref<'points' | 'spending' | 'products'>('points')
const tabs = [{ key: 'points', label: '积分榜' }, { key: 'spending', label: '消费榜' }, { key: 'products', label: '销量榜' }] as const
const activeTabMeta = computed(() => tabs.find(tab => tab.key === activeTab.value)!)
const userEntries = computed(() => board.value[activeTab.value] as UserLeaderboardEntry[])
const rankClass = (rank: number) => rank <= 3 ? `rank-${rank}` : ''
const formatValue = (value: number) => activeTab.value === 'spending' ? `¥${Number(value).toFixed(2)}` : Number(value).toLocaleString()

async function loadLeaderboard() {
  loading.value = true
  try { board.value = await getLeaderboard() } finally { loading.value = false }
}
onMounted(loadLeaderboard)
</script>

<template>
  <div class="leaderboard-page">
    <AppHeader />
    <main class="content"><div class="container">
      <header class="page-heading"><h1>排行榜</h1><p>查看积分、消费与商品销量的实时排名</p></header>
      <div class="tabs" role="tablist" aria-label="排行榜类型"><button v-for="tab in tabs" :key="tab.key" class="tab-button" :class="{ active: activeTab === tab.key }" type="button" @click="activeTab = tab.key">{{ tab.label }}</button></div>
      <section v-loading="loading" class="leaderboard" aria-live="polite">
        <template v-if="activeTab === 'products'">
          <div v-if="!board.products.length && !loading" class="empty">暂无销量数据</div>
          <div v-else class="product-list"><button v-for="item in board.products" :key="item.productId" class="product-row" type="button" @click="router.push({ name: 'ProductDetail', params: { id: item.productId } })"><span class="rank" :class="rankClass(item.rank)">{{ item.rank }}</span><el-image class="product-image" :src="item.image" fit="cover"><template #error><div class="image-placeholder">商品</div></template></el-image><span class="product-name">{{ item.name }}</span><span class="product-price">¥{{ Number(item.price).toFixed(2) }}</span><span class="metric">{{ item.sales.toLocaleString() }} 件</span></button></div>
        </template>
        <template v-else>
          <div v-if="!userEntries.length && !loading" class="empty">暂无{{ activeTabMeta.label }}数据</div>
          <div v-else class="user-list"><div v-for="item in userEntries" :key="item.rank" class="user-row"><span class="rank" :class="rankClass(item.rank)">{{ item.rank }}</span><el-avatar :size="42" :src="item.avatar">{{ item.nickname[0] }}</el-avatar><span class="nickname">{{ item.nickname }}</span><span class="metric">{{ formatValue(item.value) }}{{ activeTab === 'points' ? ' 积分' : '' }}</span></div></div>
        </template>
      </section>
    </div></main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.leaderboard-page { min-height: 100vh; background: #f5f7fa; color: #202938; }.content { padding: 32px 0 56px; }.container { width: min(900px, calc(100% - 32px)); margin: 0 auto; }.page-heading { margin-bottom: 24px; }.page-heading h1 { margin: 0 0 6px; font-size: 28px; font-weight: 700; }.page-heading p { margin: 0; color: #6b7280; font-size: 14px; }.tabs { display: flex; border-bottom: 1px solid #dfe3e8; margin-bottom: 20px; }.tab-button { min-width: 96px; height: 42px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: #667085; cursor: pointer; font-size: 15px; }.tab-button.active { color: #d9480f; border-bottom-color: #d9480f; font-weight: 600; }.leaderboard { min-height: 320px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; }.user-row, .product-row { display: grid; grid-template-columns: 54px 42px minmax(0, 1fr) auto; align-items: center; gap: 14px; width: 100%; min-height: 70px; padding: 12px 20px; border: 0; border-bottom: 1px solid #eef0f2; background: #fff; text-align: left; }.product-row { grid-template-columns: 54px 52px minmax(0, 1fr) auto auto; cursor: pointer; }.product-row:hover { background: #fff8f4; }.user-row:last-child, .product-row:last-child { border-bottom: 0; }.rank { display: inline-grid; width: 28px; height: 28px; place-items: center; color: #667085; font-size: 14px; font-weight: 600; }.rank-1, .rank-2, .rank-3 { border-radius: 50%; color: #fff; }.rank-1 { background: #d4a017; }.rank-2 { background: #8492a6; }.rank-3 { background: #b86a3c; }.nickname, .product-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 15px; }.metric { color: #d9480f; font-size: 14px; font-weight: 600; white-space: nowrap; }.product-image { width: 52px; height: 52px; border-radius: 4px; background: #f2f4f7; }.image-placeholder { display: grid; width: 100%; height: 100%; place-items: center; color: #98a2b3; font-size: 12px; }.product-price { color: #475467; font-size: 14px; white-space: nowrap; }.empty { display: grid; min-height: 320px; place-items: center; color: #98a2b3; }@media (max-width: 600px) { .content { padding-top: 20px; }.user-row, .product-row { gap: 10px; padding: 10px 12px; }.user-row { grid-template-columns: 34px 38px minmax(0, 1fr) auto; }.product-row { grid-template-columns: 34px 44px minmax(0, 1fr) auto; }.product-image { width: 44px; height: 44px; }.product-price { display: none; }.metric { font-size: 13px; }.tab-button { flex: 1; min-width: 0; } }
</style>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {getActivityDetail, getActivityItems, getSeckillRequest, participate, seckillParticipate} from '@/api/marketing'
import type {MarketingActivity, MarketingActivityItem} from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const activity = ref<MarketingActivity | null>(null)
const items = ref<MarketingActivityItem[]>([])
const countdownText = ref('')

let countdownTimer: ReturnType<typeof setInterval> | null = null

// 活动类型映射
const activityTypeMap: Record<string, { text: string; type: string; icon: string }> = {
  LIMITED_DISCOUNT: { text: '限时折扣', type: 'warning', icon: '⏰' },
  FULL_REDUCTION: { text: '满减', type: 'danger', icon: '🎁' },
  FLASH_SALE: { text: '秒杀', type: 'primary', icon: '⚡' },
  GROUP_BUY: { text: '拼团', type: 'success', icon: '👥' }
}

// 活动状态
const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '未开始', type: 'info' },
  1: { text: '进行中', type: 'success' },
  2: { text: '已结束', type: 'danger' }
}

const activityId = computed(() => Number(route.params.id))

// 类型信息
const typeInfo = computed(() => {
  if (!activity.value) return { text: '', type: '', icon: '' }
  return activityTypeMap[activity.value.type] || { text: activity.value.type, type: '', icon: '📌' }
})

// 状态信息
const statusInfo = computed(() => {
  if (!activity.value) return { text: '', type: '' }
  return statusMap[activity.value.status] || { text: '未知', type: 'info' }
})

// 加载活动详情
async function loadDetail() {
  loading.value = true
  try {
    const [detail, itemList] = await Promise.all([
      getActivityDetail(activityId.value).catch(() => null),
      getActivityItems(activityId.value).catch(() => [])
    ])
    activity.value = detail
    items.value = itemList
    startCountdown()
  } finally {
    loading.value = false
  }
}

// 倒计时
function startCountdown() {
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}

function updateCountdown() {
  if (!activity.value) return
  const now = new Date()
  let targetTime: Date | null = null
  let prefix = ''

  if (activity.value.status === 0 && activity.value.startTime) {
    targetTime = new Date(activity.value.startTime)
    prefix = '距开始'
  } else if (activity.value.status === 1 && activity.value.endTime) {
    targetTime = new Date(activity.value.endTime)
    prefix = '距结束'
  }

  if (!targetTime) {
    countdownText.value = ''
    return
  }

  const diff = targetTime.getTime() - now.getTime()
  if (diff <= 0) {
    countdownText.value = '活动已结束'
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    return
  }

  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  const seconds = Math.floor((diff % 60000) / 1000)

  countdownText.value = `${prefix} ${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

// 抢购
async function handleBuy(item: MarketingActivityItem) {
  try {
    await ElMessageBox.confirm(
      `您确认参与"${item.productName}"的活动吗？活动价格 ¥${item.activityPrice.toFixed(2)}`,
      '确认参与',
      {
        confirmButtonText: '确认抢购',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    const result = activity.value?.type === 'FLASH_SALE'
      ? await seckillParticipate(activityId.value, item.id || 0, 1)
      : await participate(activityId.value, item.id || 0, item.productId, item.skuId, 1)
    if (result.requestId) {
      await waitForSeckillOrder(result.requestId)
      return
    }
    ElMessage.success('抢购成功！正在跳转订单页面...')
    if (result.orderId) {
      router.push({ name: 'OrderDetail', params: { id: result.orderId } })
    } else {
      router.push({ name: 'Cart' })
    }
  } catch {
    // 错误已由 request 拦截器处理
  }
}

async function waitForSeckillOrder(requestId: string) {
  for (let attempt = 0; attempt < 20; attempt++) {
    await new Promise(resolve => setTimeout(resolve, 500))
    const request = await getSeckillRequest(requestId)
    if (request.status === 1 && request.orderId) {
      ElMessage.success('Order created')
      router.push({name: 'OrderDetail', params: {id: request.orderId}})
      return
    }
    if (request.status === 2) {
      ElMessage.error(request.errorMessage || 'Seckill request failed')
      return
    }
  }
  ElMessage.info('Request is queued. Check your orders shortly.')
}

// 库存格式化
function formatStock(stock: number) {
  if (stock <= 0) return '已售罄'
  if (stock < 10) return `仅剩 ${stock} 件`
  return `库存 ${stock}`
}

onMounted(() => {
  loadDetail()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<template>
  <div class="marketing-detail-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-wrap">
          <el-skeleton :rows="8" animated />
        </div>

        <template v-else-if="activity">
          <!-- 活动头部 -->
          <div class="activity-header" :class="activity.type">
            <div class="header-left">
              <div class="type-badge">
                {{ typeInfo.icon }} {{ typeInfo.text }}
              </div>
              <h1 class="activity-title">{{ activity.name }}</h1>
              <div class="activity-time">
                📅 {{ activity.startTime?.replace('T', ' ') }} ~ {{ activity.endTime?.replace('T', ' ') }}
              </div>
            </div>
            <div class="header-right">
              <el-tag :type="statusInfo.type as any" effect="dark" size="large">
                {{ statusInfo.text }}
              </el-tag>
              <div v-if="countdownText" class="countdown">
                ⏱ {{ countdownText }}
              </div>
            </div>
          </div>

          <!-- 活动描述 -->
          <div v-if="activity.description" class="activity-desc">
            {{ activity.description }}
          </div>

          <!-- 活动商品 -->
          <div class="section-title">
            <h2>活动商品</h2>
            <span class="count">共 {{ items.length }} 件商品</span>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="!items.length" description="暂无活动商品" />

          <!-- 商品列表 -->
          <div v-else class="product-list">
            <div
              v-for="item in items"
              :key="item.id || item.skuId"
              class="product-card"
            >
              <div class="product-image">
                <img
                  :src="item.productImage || '/favicon.svg'"
                  :alt="item.productName"
                  @error="($event.target as HTMLImageElement).src = '/favicon.svg'"
                />
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ item.productName }}</h3>
                <div class="price-row">
                  <span class="activity-price">¥{{ item.activityPrice.toFixed(2) }}</span>
                  <span class="original-price">¥{{ item.originalPrice.toFixed(2) }}</span>
                  <span class="discount-tag">
                    {{ Math.round((1 - item.activityPrice / item.originalPrice) * 100) }}% OFF
                  </span>
                </div>
                <div class="stock-row">
                  <span class="stock-label">{{ formatStock(item.remainingStock ?? item.stock) }}</span>
                  <div v-if="item.soldCount" class="sold-info">
                    已售 {{ item.soldCount }} 件
                  </div>
                </div>
                <el-button
                  type="danger"
                  size="large"
                  round
                  :disabled="(item.remainingStock ?? item.stock) <= 0 || activity.status !== 1"
                  @click="handleBuy(item)"
                >
                  {{
                    (item.remainingStock ?? item.stock) <= 0 ? '已售罄' : activity.status !== 1 ? '活动未开始' : '立即抢购'
                  }}
                </el-button>
              </div>
            </div>
          </div>
        </template>

        <!-- 活动不存在 -->
        <el-empty v-else description="活动不存在或已下线">
          <el-button type="primary" @click="router.push('/marketing/activities')">返回活动列表</el-button>
        </el-empty>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.marketing-detail-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.main-content {
  padding: 24px 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.loading-wrap {
  padding: 40px 0;
}

.activity-header {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
  }

  &.LIMITED_DISCOUNT::before { background: linear-gradient(90deg, #faad14, #ff7a45); }
  &.FULL_REDUCTION::before { background: linear-gradient(90deg, #f5222d, #ff7875); }
  &.FLASH_SALE::before { background: linear-gradient(90deg, #1890ff, #40a9ff); }
  &.GROUP_BUY::before { background: linear-gradient(90deg, #52c41a, #73d13d); }
}

.header-left {
  flex: 1;

  .type-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 13px;
    color: #fff;
    margin-bottom: 12px;
    background: #ff6b35;
  }

  .activity-title {
    font-size: 24px;
    color: #333;
    margin: 0 0 10px;
    font-weight: 700;
  }

  .activity-time {
    font-size: 14px;
    color: #999;
  }
}

.header-right {
  text-align: right;
  flex-shrink: 0;

  .countdown {
    margin-top: 12px;
    font-size: 16px;
    font-weight: 600;
    color: #ff6b35;
    font-variant-numeric: tabular-nums;
  }
}

.activity-desc {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.8;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    font-size: 18px;
    color: #333;
    margin: 0;
  }

  .count {
    font-size: 13px;
    color: #999;
  }
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  gap: 20px;
  align-items: center;
  transition: all 0.25s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  .product-image {
    width: 120px;
    height: 120px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
    background: #f9f9f9;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .product-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-width: 0;
  }

  .product-name {
    font-size: 16px;
    color: #333;
    margin: 0;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
  }

  .price-row {
    display: flex;
    align-items: baseline;
    gap: 10px;

    .activity-price {
      font-size: 22px;
      color: #ff4d4f;
      font-weight: 700;
    }

    .original-price {
      font-size: 14px;
      color: #bbb;
      text-decoration: line-through;
    }

    .discount-tag {
      background: #fff1f0;
      color: #ff4d4f;
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 4px;
      font-weight: 500;
    }
  }

  .stock-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .stock-label {
      font-size: 13px;
      color: #666;
    }

    .sold-info {
      font-size: 12px;
      color: #999;
    }
  }

  .el-button {
    align-self: flex-end;
  }
}

@media (max-width: 768px) {
  .activity-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;

    .header-right {
      text-align: left;
    }
  }

  .product-card {
    flex-direction: column;
    align-items: flex-start;

    .product-image {
      width: 100%;
      max-width: 200px;
      height: auto;
      aspect-ratio: 1;
    }

    .el-button {
      align-self: stretch;
    }
  }
}
</style>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getActiveActivities } from '@/api/marketing'
import type { MarketingActivity } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()

const loading = ref(true)
const activities = ref<MarketingActivity[]>([])
const activeType = ref<string>('')

// 活动类型映射
const activityTypeMap: Record<string, { text: string; type: string; icon: string }> = {
  LIMITED_DISCOUNT: { text: '限时折扣', type: 'warning', icon: '⏰' },
  FULL_REDUCTION: { text: '满减', type: 'danger', icon: '🎁' },
  FLASH_SALE: { text: '秒杀', type: 'primary', icon: '⚡' },
  GROUP_BUY: { text: '拼团', type: 'success', icon: '👥' }
}

// 活动状态映射
const statusMap: Record<number, { text: string; type: string }> = {
  0: { text: '未开始', type: 'info' },
  1: { text: '进行中', type: 'success' },
  2: { text: '已结束', type: 'danger' }
}

// 筛选标签
const typeTabs = [
  { key: '', label: '全部' },
  { key: 'LIMITED_DISCOUNT', label: '限时折扣' },
  { key: 'FULL_REDUCTION', label: '满减' },
  { key: 'FLASH_SALE', label: '秒杀' },
  { key: 'GROUP_BUY', label: '拼团' }
]

// 筛选后的活动列表
const filteredActivities = computed(() => {
  if (!activeType.value) return activities.value
  return activities.value.filter(a => a.type === activeType.value)
})

// 获取活动列表
async function loadActivities() {
  loading.value = true
  try {
    activities.value = await getActiveActivities(activeType.value || undefined).catch(() => [])
  } finally {
    loading.value = false
  }
}

// 切换类型
function handleTypeChange(type: string) {
  activeType.value = type
  loadActivities()
}

// 跳转到详情
function goDetail(id: number) {
  router.push({ name: 'MarketingDetail', params: { id } })
}

// 获取类型信息
function getTypeInfo(type: string) {
  return activityTypeMap[type] || { text: type, type: '', icon: '📌' }
}

// 获取状态信息
function getStatusInfo(status: number) {
  return statusMap[status] || { text: '未知', type: 'info' }
}

onMounted(() => {
  loadActivities()
})
</script>

<template>
  <div class="marketing-list-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1>🎉 营销活动</h1>
          <p class="subtitle">精选活动，优惠多多</p>
        </div>

        <!-- 类型筛选 -->
        <div class="type-tabs">
          <div
            v-for="tab in typeTabs"
            :key="tab.key"
            :class="['tab-item', { active: activeType === tab.key }]"
            @click="handleTypeChange(tab.key)"
          >
            {{ tab.label }}
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-wrap">
          <el-skeleton :rows="5" animated />
        </div>

        <!-- 空状态 -->
        <el-empty v-else-if="!filteredActivities.length" description="暂无进行中的活动" />

        <!-- 活动卡片网格 -->
        <el-row v-else :gutter="20">
          <el-col
            v-for="activity in filteredActivities"
            :key="activity.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            class="col-item"
          >
            <div class="activity-card" @click="goDetail(activity.id)">
              <div class="card-badge" :class="activity.type">
                {{ getTypeInfo(activity.type).icon }} {{ getTypeInfo(activity.type).text }}
              </div>
              <div class="card-body">
                <h3 class="activity-name">{{ activity.name }}</h3>
                <p class="activity-desc" v-if="activity.description">{{ activity.description }}</p>
                <div class="activity-info">
                  <div class="info-row">
                    <span class="label">活动时间</span>
                    <span class="value">{{ activity.startTime?.slice(5, 16) }} - {{ activity.endTime?.slice(5, 16) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="label">商品数量</span>
                    <span class="value">{{ activity.items?.length || 0 }} 件</span>
                  </div>
                </div>
                <div class="card-footer">
                  <el-tag :type="getStatusInfo(activity.status).type as any" effect="light" size="small">
                    {{ getStatusInfo(activity.status).text }}
                  </el-tag>
                  <el-button type="primary" size="small" round>
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.marketing-list-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.main-content {
  padding: 24px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 28px;

  h1 {
    font-size: 28px;
    color: #333;
    margin: 0 0 8px;
  }

  .subtitle {
    color: #999;
    font-size: 14px;
    margin: 0;
  }
}

.type-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 28px;
  flex-wrap: wrap;

  .tab-item {
    padding: 8px 20px;
    border-radius: 20px;
    background: #fff;
    color: #666;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.25s;
    border: 1px solid #e8e8e8;

    &:hover {
      color: #ff6b35;
      border-color: #ff6b35;
    }

    &.active {
      background: #ff6b35;
      color: #fff;
      border-color: #ff6b35;
    }
  }
}

.loading-wrap {
  padding: 40px 0;
}

.col-item {
  margin-bottom: 20px;
}

.activity-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .card-badge {
    padding: 12px 16px;
    font-size: 14px;
    font-weight: 500;
    color: #fff;
    display: flex;
    align-items: center;
    gap: 6px;

    &.LIMITED_DISCOUNT { background: linear-gradient(135deg, #faad14, #ff7a45); }
    &.FULL_REDUCTION { background: linear-gradient(135deg, #f5222d, #ff7875); }
    &.FLASH_SALE { background: linear-gradient(135deg, #1890ff, #40a9ff); }
    &.GROUP_BUY { background: linear-gradient(135deg, #52c41a, #73d13d); }
  }

  .card-body {
    padding: 16px;
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .activity-name {
    font-size: 16px;
    color: #333;
    margin: 0 0 8px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .activity-desc {
    font-size: 13px;
    color: #999;
    margin: 0 0 12px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.5;
    min-height: 38px;
  }

  .activity-info {
    margin-bottom: 12px;

    .info-row {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 6px;

      .label { color: #999; }
      .value { color: #666; }
    }
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
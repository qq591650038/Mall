<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/admin'
import type { DashboardStats } from '@/types'

const stats = ref<DashboardStats | null>(null)
const loading = ref(true)

const conversionColor = computed(() => {
  const rate = stats.value?.conversionRate ?? 0
  return rate <= 50 ? '#1890ff' : '#52c41a'
})
let chartInstance: echarts.ECharts | null = null
let hotProductsChart: echarts.ECharts | null = null
let userTrendChart: echarts.ECharts | null = null

async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
    await nextTick()
    initSalesChart()
    initHotProductsChart()
    initUserTrendChart()
  } catch { /* handled */ }
  finally { loading.value = false }
}

function initSalesChart() {
  const el = document.getElementById('sales-chart')
  if (!el || !stats.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(el)
  const data = stats.value.salesTrend
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLabel: { color: '#999' },
      axisLine: { lineStyle: { color: '#e0e0e0' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [{
      name: '销售额',
      type: 'line',
      smooth: true,
      data: data.map(d => d.amount),
      itemStyle: { color: '#1890ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
          { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
        ])
      }
    }]
  })
}

function initHotProductsChart() {
  const el = document.getElementById('hot-products-chart')
  if (!el || !stats.value?.hotProducts?.length) return
  if (hotProductsChart) hotProductsChart.dispose()
  hotProductsChart = echarts.init(el)
  const data = stats.value.hotProducts
  hotProductsChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    yAxis: {
      type: 'category',
      data: data.map(d => d.name).reverse(),
      axisLabel: { color: '#666' },
      axisLine: { lineStyle: { color: '#e0e0e0' } }
    },
    series: [{
      name: '销量',
      type: 'bar',
      data: data.map(d => d.sales).reverse(),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#1890ff' },
          { offset: 1, color: '#36cfc9' }
        ]),
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: 18
    }]
  })
}

function initUserTrendChart() {
  const el = document.getElementById('user-trend-chart')
  if (!el || !stats.value?.userTrend?.length) return
  if (userTrendChart) userTrendChart.dispose()
  userTrendChart = echarts.init(el)
  const data = stats.value.userTrend
  userTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '活跃用户'], right: 10, top: 0 },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLabel: { color: '#999' },
      axisLine: { lineStyle: { color: '#e0e0e0' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } }
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: data.map(d => d.newUsers),
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        data: data.map(d => d.activeUsers),
        itemStyle: { color: '#fa8c16' }
      }
    ]
  })
}

function handleResize() {
  chartInstance?.resize()
  hotProductsChart?.resize()
  userTrendChart?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <div class="stat-cards">
      <div class="stat-card primary">
        <div class="stat-icon">📦</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats?.totalProducts ?? 0 }}</div>
          <div class="stat-label">商品总数</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats?.totalOrders ?? 0 }}</div>
          <div class="stat-label">订单总数</div>
        </div>
      </div>
      <div class="stat-card warning">
        <div class="stat-icon">💰</div>
        <div class="stat-info">
          <div class="stat-value">¥{{ stats?.totalSales?.toFixed(2) ?? '0.00' }}</div>
          <div class="stat-label">销售总额</div>
        </div>
      </div>
      <div class="stat-card info">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats?.totalUsers ?? 0 }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
    </div>

    <div class="detail-card operating-metrics">
      <h3>经营指标</h3>
      <div class="metric-grid">
        <div class="metric-item"><span class="label">平均客单价</span><strong>¥{{ (stats?.avgOrderAmount ?? 0).toFixed(2) }}</strong></div>
        <div class="metric-item"><span class="label">退款率</span><strong>{{ (stats?.refundRate ?? 0).toFixed(1) }}%</strong></div>
        <div class="metric-item"><span class="label">累计退款金额</span><strong>¥{{ (stats?.totalRefundAmount ?? 0).toFixed(2) }}</strong></div>
        <div class="metric-item"><span class="label">今日退款</span><strong>{{ stats?.todayRefundCount ?? 0 }} 笔</strong><small>¥{{ (stats?.todayRefundAmount ?? 0).toFixed(2) }}</small></div>
      </div>
    </div>

    <div class="detail-cards">
      <div class="detail-card">
        <h3>销售趋势</h3>
        <div id="sales-chart" class="chart"></div>
      </div>

      <div class="detail-card">
        <h3>今日数据</h3>
        <div class="today-stats">
          <div class="today-item">
            <span class="label">今日订单</span>
            <span class="value">{{ stats?.todayOrders ?? 0 }}</span>
          </div>
          <div class="today-item">
            <span class="label">今日销售额</span>
            <span class="value">¥{{ stats?.todaySales?.toFixed(2) ?? '0.00' }}</span>
          </div>
          <div class="today-item warning">
            <span class="label">库存预警</span>
            <span class="value">{{ stats?.lowStockProducts ?? 0 }} 件</span>
          </div>
          <div class="today-item danger">
            <span class="label">待处理评价</span>
            <span class="value">{{ stats?.pendingReviews ?? 0 }} 条</span>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-card">
      <h3>最新订单</h3>
      <el-table :data="stats?.recentOrders ?? []" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="orderStatusText" label="状态" width="100" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
    </div>

    <div class="detail-card category-hot-products">
      <h3>分类热销商品</h3>
      <el-table :data="stats?.categoryHotProducts ?? []" size="small" max-height="320">
        <el-table-column prop="categoryName" label="分类" width="180" show-overflow-tooltip />
        <el-table-column prop="productName" label="商品" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sales" label="销量" width="100" />
        <el-table-column prop="price" label="单价" width="120"><template #default="{ row }">¥{{ Number(row.price ?? 0).toFixed(2) }}</template></el-table-column>
      </el-table>
      <el-empty v-if="!stats?.categoryHotProducts?.length" description="暂无分类热销数据" />
    </div>

    <div class="detail-cards">
      <div class="detail-card">
        <h3>热销商品排行</h3>
        <div id="hot-products-chart" class="chart"></div>
      </div>
      <div class="detail-card conversion-card">
        <h3>订单转化率</h3>
        <div class="conversion-content">
          <div class="conversion-ring">
            <el-progress
              type="dashboard"
              :percentage="Math.round(stats?.conversionRate ?? 0)"
              :stroke-width="12"
              :color="conversionColor"
            >
              <template #default>
                <div class="progress-text">
                  <span class="value">{{ (stats?.conversionRate ?? 0).toFixed(1) }}%</span>
                  <span class="label">转化率</span>
                </div>
              </template>
            </el-progress>
          </div>
          <div class="conversion-tips">
            <p>访问用户到下单用户的转化比例</p>
            <p class="sub">行业平均: 3% - 5%</p>
          </div>
        </div>
      </div>
    </div>

    <div class="detail-cards">
      <div class="detail-card">
        <h3>用户趋势</h3>
        <div id="user-trend-chart" class="chart"></div>
      </div>
      <div class="detail-card">
        <h3>库存预警详情</h3>
        <el-table :data="stats?.stockWarnings ?? []" size="small" max-height="280">
          <el-table-column prop="name" label="商品名称" show-overflow-tooltip />
          <el-table-column prop="skuInfo" label="规格" width="120" />
          <el-table-column prop="stock" label="库存" width="80">
            <template #default="{ row }">
              <span :style="{ color: row.stock < 10 ? '#ff4d4f' : '#fa8c16', fontWeight: 600 }">
                {{ row.stock }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page { display: flex; flex-direction: column; gap: 20px; }
.operating-metrics, .category-hot-products { padding: 20px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.metric-item { display: flex; flex-direction: column; gap: 8px; padding: 14px 16px; background: #fafafa; border-radius: 6px; }
.metric-item .label { color: #888; font-size: 13px; }
.metric-item strong { color: #222; font-size: 21px; }
.metric-item small { color: #999; font-size: 12px; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
  }

  &.primary .stat-icon { background: #e6f7ff; }
  &.success .stat-icon { background: #f6ffed; }
  &.warning .stat-icon { background: #fffbe6; }
  &.info .stat-icon { background: #f9f0ff; }

  .stat-info { .stat-value { font-size: 24px; font-weight: 700; color: #333; } .stat-label { color: #999; font-size: 13px; margin-top: 4px; } }
}

.detail-cards { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }

.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;

  h3 { margin: 0 0 16px; font-size: 16px; color: #333; }
}

.chart { height: 280px; }

.today-stats { display: flex; flex-direction: column; gap: 12px; }
.today-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 6px;

  .label { color: #666; font-size: 14px; }
  .value { font-size: 18px; font-weight: 600; color: #333; }
  &.warning .value { color: #fa8c16; }
  &.danger .value { color: #ff4d4f; }
}

.conversion-card {
  display: flex;
  flex-direction: column;
}

.conversion-content {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  justify-content: center;
  padding: 20px 0;
}

.conversion-ring {
  width: 160px;
}

.progress-text {
  text-align: center;
  .value { font-size: 24px; font-weight: 700; color: #1890ff; display: block; }
  .label { font-size: 13px; color: #999; display: block; margin-top: 4px; }
}

.conversion-tips {
  p { margin: 0; color: #666; font-size: 14px; }
  .sub { color: #999; font-size: 12px; margin-top: 8px; }
}
</style>

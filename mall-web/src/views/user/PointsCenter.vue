<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getAddressList } from '@/api/address'
import type { Address } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import {
  checkInPoints,
  getPointsLedger,
  getPointsProducts,
  getPointsRedemptions,
  getPointsSummary,
  redeemPoints,
  type PointsLedger,
  type PointsProduct,
  type PointsRedemption,
  type PointsSummary
} from '@/api/points'

// 积分概览
const summary = ref<PointsSummary>()
// 积分明细
const ledger = ref<PointsLedger[]>([])
// 兑换商品
const products = ref<PointsProduct[]>([])
// 兑换记录
const redemptions = ref<PointsRedemption[]>([])

// 状态
const loading = ref(false)
const checkingIn = ref(false)
const redeemingId = ref<number>()
const router = useRouter()

// 分页
const total = ref(0)
const current = ref(1)

// 兑换记录分页
const redemptionTotal = ref(0)
const redemptionCurrent = ref(1)

async function load() {
  loading.value = true
  try {
    const [summaryResult, ledgerResult, productResult, redemptionResult] = await Promise.all([
      getPointsSummary(),
      getPointsLedger({ current: current.value, size: 10 }),
      getPointsProducts(),
      getPointsRedemptions({ current: redemptionCurrent.value, size: 5 })
    ])
    summary.value = summaryResult
    ledger.value = ledgerResult.list || []
    total.value = ledgerResult.total || 0
    products.value = productResult || []
    redemptions.value = redemptionResult.list || []
    redemptionTotal.value = redemptionResult.total || 0
  } finally {
    loading.value = false
  }
}

async function redeem(product: PointsProduct) {
  let addressId: number | undefined
  if (product.rewardType === 'PHYSICAL') {
    const addresses = await getAddressList()
    if (!addresses.length) {
      await ElMessageBox.confirm('兑换实物奖励需要收货地址，是否前往新增？', '请选择收货地址')
      router.push('/addresses')
      return
    }
    addressId = await chooseAddress(addresses)
    if (!addressId) return
  }
  // 二次确认兑换
  if (!confirm(`确定使用 ${product.pointsCost} 积分兑换「${product.name}」吗？`)) {
    return
  }
  redeemingId.value = product.id
  try {
    const result = await redeemPoints(product.id, addressId)
    ElMessage.success(`兑换成功，兑换码：${result.redemptionCode}`)
    await load()
  } catch (err: any) {
    // 统一异常处理由 axios 拦截器完成
  } finally {
    redeemingId.value = undefined
  }
}

async function chooseAddress(addresses: Address[]): Promise<number | undefined> {
  try {
    const lines = addresses.map((address, index) => `${index + 1}. ${address.receiverName} ${address.receiverPhone} ${address.province}${address.city}${address.district}${address.detailAddress}`)
    const result = await ElMessageBox.prompt(`请输入地址序号：\n${lines.join('\n')}`, '选择收货地址', {
      inputValue: String(Math.max(1, addresses.findIndex(address => address.isDefault === 1) + 1)),
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入有效地址序号'
    })
    const index = Number(result.value) - 1
    return index >= 0 && index < addresses.length ? addresses[index].id : undefined
  } catch {
    return undefined
  }
}

async function checkIn() {
  if (summary.value?.checkedInToday) return
  checkingIn.value = true
  try {
    summary.value = await checkInPoints()
    ElMessage.success('签到成功，获得 10 积分')
    await loadLedger()
  } finally {
    checkingIn.value = false
  }
}

async function loadLedger() {
  const result = await getPointsLedger({ current: current.value, size: 10 })
  ledger.value = result.list || []
  total.value = result.total || 0
}

async function loadRedemptions() {
  const result = await getPointsRedemptions({ current: redemptionCurrent.value, size: 5 })
  redemptions.value = result.list || []
  redemptionTotal.value = result.total || 0
}

onMounted(load)
</script>

<template>
  <div class="points-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <!-- 页面标题 -->
        <div class="page-heading">
          <div>
            <h1>积分中心</h1>
            <p>签到、消费和活动都可以积累会员成长值</p>
          </div>
        </div>

        <!-- 积分概览 -->
        <section class="summary-grid" v-loading="loading">
          <div class="summary-card primary">
            <span>当前积分</span>
            <strong>{{ summary?.balance ?? 0 }}</strong>
            <small>可用于积分兑换</small>
          </div>
          <div class="summary-card level-card">
            <span>会员等级</span>
            <strong>{{ summary?.memberLevel || '普通会员' }}</strong>
            <div class="level-progress" v-if="summary">
              <span class="rate-tag">{{ summary.pointsRate }}x 积分</span>
              <div class="progress-info">
                <span v-if="summary.nextLevelName && summary.nextLevelPoints > 0">
                  距「{{ summary.nextLevelName }}」还需 {{ summary.pointsToNextLevel }} 积分
                </span>
                <span v-else class="max-level">已满级 🎉</span>
              </div>
            </div>
          </div>
          <div class="summary-card">
            <span>累计获得</span>
            <strong>{{ summary?.totalEarned ?? 0 }}</strong>
            <small>包含签到和活动奖励</small>
          </div>
          <div class="summary-card checkin-card">
            <span>每日签到</span>
            <strong>+10</strong>
            <el-button
              type="primary"
              :disabled="summary?.checkedInToday"
              :loading="checkingIn"
              @click="checkIn"
            >
              {{ summary?.checkedInToday ? '今日已签到' : '立即签到' }}
            </el-button>
          </div>
        </section>

        <!-- 积分兑换 -->
        <section class="exchange-panel">
          <div class="panel-header">
            <h2>积分兑换</h2>
            <span class="panel-tip">使用积分兑换精选好礼</span>
          </div>
          <el-empty v-if="!loading && !products.length" description="暂无兑换商品" />
          <div class="product-grid" v-else>
            <div
              v-for="product in products"
              :key="product.id"
              class="product-card"
            >
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-desc" v-if="product.description">{{ product.description }}</p>
                <div class="product-meta">
                  <span class="points-cost">{{ product.pointsCost }} 积分</span>
                  <span class="stock" :class="{ low: product.stock <= 5 }">
                    库存：{{ product.stock }}
                  </span>
                </div>
              </div>
              <el-button
                type="primary"
                size="small"
                :loading="redeemingId === product.id"
                :disabled="(summary?.balance ?? 0) < product.pointsCost || product.stock <= 0"
                @click="redeem(product)"
              >
                {{ product.stock <= 0 ? '已兑完' : '立即兑换' }}
              </el-button>
            </div>
          </div>
        </section>

        <!-- 兑换记录 -->
        <section class="redemption-panel">
          <div class="panel-header">
            <h2>兑换记录</h2>
          </div>
          <el-empty v-if="!loading && !redemptions.length" description="暂无兑换记录" />
          <el-table v-else :data="redemptions" v-loading="loading">
            <el-table-column prop="createTime" label="兑换时间" width="180" />
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column prop="points" label="消耗积分" width="120">
              <template #default="{ row }">
                <span class="points">-{{ row.points }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="redemptionCode" label="兑换码" min-width="200">
              <template #default="{ row }">
                <span class="code">{{ row.redemptionCode }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="redemptionTotal > 5"
            class="pagination"
            background
            layout="prev, pager, next"
            :page-size="5"
            :total="redemptionTotal"
            @current-change="(page: number) => { redemptionCurrent = page; loadRedemptions() }"
          />
        </section>

        <!-- 积分明细 -->
        <section class="ledger-panel">
          <div class="panel-header">
            <h2>积分明细</h2>
          </div>
          <el-table :data="ledger" v-loading="loading">
            <el-table-column prop="createTime" label="时间" width="180" />
            <el-table-column prop="remark" label="说明" min-width="220" />
            <el-table-column label="变动" width="120">
              <template #default="{ row }">
                <span :class="row.amount > 0 ? 'income' : 'expense'">
                  {{ row.amount > 0 ? '+' : '' }}{{ row.amount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变动后余额" width="130" />
          </el-table>
          <el-empty v-if="!loading && !ledger.length" description="暂无积分记录" />
          <el-pagination
            v-if="total > 10"
            class="pagination"
            background
            layout="prev, pager, next"
            :page-size="10"
            :total="total"
            @current-change="(page: number) => { current = page; loadLedger() }"
          />
        </section>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.points-page { min-height: 100vh; background: #f5f5f5; }
.main-content { padding: 24px 0 48px; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-heading h1 { margin: 0; color: #333; font-size: 20px; }
.page-heading p { margin: 6px 0 0; color: #999; font-size: 13px; }

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.summary-card,
.exchange-panel,
.redemption-panel,
.ledger-panel {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
}
.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.summary-card span { color: #777; font-size: 13px; }
.summary-card strong { color: #333; font-size: 26px; }
.summary-card small { color: #aaa; font-size: 12px; }
.summary-card.primary strong { color: #ff6b35; }
.checkin-card .el-button { align-self: flex-start; margin-top: 4px; }

.level-card .level-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 6px;
}
.rate-tag {
  display: inline-block;
  padding: 2px 8px;
  background: linear-gradient(135deg, #ff6b35, #f7931e);
  color: #fff;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  align-self: flex-start;
}
.progress-info { font-size: 12px; color: #888; }
.max-level { color: #67c23a; font-weight: 600; }

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.panel-header h2 { margin: 0; color: #333; font-size: 17px; }
.panel-tip { color: #999; font-size: 13px; }

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.product-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #eee;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}
.product-info { flex: 1; }
.product-name { margin: 0 0 8px; color: #333; font-size: 15px; font-weight: 600; }
.product-desc {
  margin: 0 0 12px;
  color: #888;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.points-cost { color: #ff6b35; font-weight: 600; font-size: 16px; }
.stock { color: #666; font-size: 12px; }
.stock.low { color: #e6a23c; }

.points { color: #f56c6c; font-weight: 600; }
.code { font-family: monospace; color: #333; }

.ledger-panel,
.redemption-panel {
  margin-top: 16px;
}
.income { color: #67c23a; font-weight: 600; }
.expense { color: #f56c6c; font-weight: 600; }
.pagination { justify-content: center; margin-top: 20px; }

@media (max-width: 800px) {
  .summary-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .container { padding: 0 12px; }
  .summary-grid { grid-template-columns: 1fr; }
  .product-grid { grid-template-columns: 1fr; }
}
</style>

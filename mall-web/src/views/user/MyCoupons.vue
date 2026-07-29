<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyCoupons, getAvailableCoupons, receiveCoupon } from '@/api/coupon'
import type { Coupon, UserCoupon } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const tab = ref<'mine' | 'available'>('mine')
const mineList = ref<UserCoupon[]>([])
const availableList = ref<Coupon[]>([])
const loading = ref(false)

const activeList = computed(() => tab.value === 'mine' ? mineList.value : availableList.value)

async function loadMine() {
  loading.value = true
  try {
    mineList.value = await getMyCoupons()
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function loadAvailable() {
  loading.value = true
  try {
    availableList.value = await getAvailableCoupons()
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function loadData() {
  if (tab.value === 'mine') {
    await loadMine()
  } else {
    await loadAvailable()
  }
}

onMounted(loadData)

function onTabChange() {
  loadData()
}

async function handleReceive(couponId: number) {
  try {
    await receiveCoupon(couponId)
    ElMessage.success('领取成功')
    loadAvailable()
    loadMine()
  } catch { /* handled */ }
}

/* formatType is intentionally omitted */
/*
  if (coupon.type === 1) return `满 ${coupon.minAmount} 减 ${coupon.value}`
  return `¥${coupon.value} 代金券`
}*/

function formatTime(time?: string) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}
</script>

<template>
  <div class="coupons-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">我的优惠券</h1>
        <el-tabs v-model="tab" @tab-change="onTabChange">
          <el-tab-pane label="我的优惠券" name="mine" />
          <el-tab-pane label="可领取" name="available" />
        </el-tabs>

        <div v-loading="loading">
          <div v-if="activeList.length === 0 && !loading" class="empty-state">
            <p>{{ tab === 'mine' ? '暂无优惠券' : '暂无可领取优惠券' }}</p>
            <el-button
              v-if="tab === 'available'"
              type="primary"
              @click="tab = 'mine'"
            >去查看</el-button>
          </div>
          <div v-else class="coupon-grid">
            <div
              v-for="item in activeList"
              :key="tab === 'mine' ? (item as UserCoupon).id : (item as Coupon).id"
              class="coupon-card"
              :class="{ received: tab === 'mine' }"
            >
              <template v-if="tab === 'mine'">
                <div class="coupon-left">
                  <span class="value" v-if="(item as UserCoupon).type === 1">¥{{ Number((item as UserCoupon).value || 0).toFixed(0) }}</span>
                  <span class="value" v-else-if="(item as UserCoupon).type === 2">{{ (item as UserCoupon).value }}折</span>
                  <span class="value" v-else>¥{{ Number((item as UserCoupon).value || 0).toFixed(0) }}</span>
                </div>
                <div class="coupon-right">
                  <h3>{{ (item as UserCoupon).name }}</h3>
                  <p class="condition">
                    {{ (item as UserCoupon).type === 1
                      ? `满 ${(item as UserCoupon).minAmount || 0} 元可用`
                      : (item as UserCoupon).type === 2
                        ? `${(item as UserCoupon).value}折优惠`
                        : '无门槛代金券' }}
                  </p>
                  <p class="time">
                    有效期至 {{ formatTime((item as UserCoupon).endTime) }}
                  </p>
                  <el-tag
                    v-if="(item as UserCoupon).status === 0"
                    type="success"
                    size="small"
                  >未使用</el-tag>
                  <el-tag
                    v-else-if="(item as UserCoupon).status === 1"
                    type="info"
                    size="small"
                  >已使用</el-tag>
                  <el-tag v-else type="danger" size="small">已过期</el-tag>
                </div>
              </template>
              <template v-else>
                <div class="coupon-left">
                  <span class="value" v-if="(item as Coupon).type === 1">¥{{ (item as Coupon).value.toFixed(0) }}</span>
                  <span class="value" v-else-if="(item as Coupon).type === 2">{{ (item as Coupon).value }}折</span>
                  <span class="value" v-else>¥{{ (item as Coupon).value.toFixed(0) }}</span>
                </div>
                <div class="coupon-right">
                  <h3>{{ (item as Coupon).name }}</h3>
                  <p class="condition">
                    {{ (item as Coupon).type === 1
                      ? `满 ${(item as Coupon).minAmount} 元可用`
                      : (item as Coupon).type === 2
                        ? `${(item as Coupon).value}折优惠`
                        : '无门槛代金券' }}
                  </p>
                  <p class="time">
                    {{ (item as Coupon).remainCount > 0
                      ? `剩余 ${(item as Coupon).remainCount} 张`
                      : '已领完' }}
                  </p>
                  <el-button
                    type="primary"
                    size="small"
                    :disabled="(item as Coupon).remainCount <= 0"
                    @click="handleReceive((item as Coupon).id)"
                  >立即领取</el-button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.coupons-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.page-title { font-size: 20px; margin: 0 0 16px; }

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;
  p { margin-bottom: 24px; }
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.coupon-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;

  &.received {
    opacity: 0.7;
  }

  .coupon-left {
    width: 140px;
    min-height: 120px;
    background: linear-gradient(135deg, #ff6b35, #ff8c42);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .value {
      font-size: 28px;
      font-weight: 700;
    }
  }

  .coupon-right {
    flex: 1;
    padding: 16px;

    h3 { margin: 0 0 8px; font-size: 16px; color: #333; }
    .condition { margin: 0 0 4px; font-size: 13px; color: #666; }
    .time { margin: 0 0 8px; font-size: 12px; color: #999; }
  }
}
</style>

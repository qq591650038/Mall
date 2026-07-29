<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLogistics } from '@/api/order'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const order = ref<OrderVO>()
const loading = ref(false)
const events = computed(() => (order.value?.timeline || []).filter(item => item.status >= 2))

onMounted(async () => {
  loading.value = true
  try { order.value = await getLogistics(Number(route.params.id)) }
  finally { loading.value = false }
})
</script>

<template>
  <div class="page"><AppHeader /><main><div class="container" v-loading="loading">
    <el-page-header content="物流跟踪" @back="router.back()" />
    <section v-if="order" class="panel">
      <div class="shipment-head"><div><span>物流公司</span><strong>{{ order.logisticsCompany || '暂未分配' }}</strong></div><div><span>运单号</span><strong>{{ order.logisticsNo || '暂未生成' }}</strong></div></div>
      <el-empty v-if="!order.logisticsNo" description="订单尚未发货，暂无物流信息" />
      <el-timeline v-else>
        <el-timeline-item v-for="(item, index) in events" :key="index" :timestamp="item.time" :type="index === 0 ? 'primary' : ''" :hollow="index !== 0"><strong>{{ item.statusText }}</strong><p>{{ item.description || '物流状态已更新' }}</p></el-timeline-item>
        <el-timeline-item :timestamp="order.shipTime" type="success"><strong>商品已发货</strong><p>包裹已交由物流公司承运</p></el-timeline-item>
      </el-timeline>
    </section>
  </div></main><AppFooter /></div>
</template>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f5f5; } main { padding: 24px 0; } .container { max-width: 900px; min-height: 400px; margin: 0 auto; padding: 0 20px; }
.panel { margin-top: 18px; padding: 24px; background: #fff; border-radius: 8px; } .shipment-head { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; padding-bottom: 22px; margin-bottom: 24px; border-bottom: 1px solid #eee; }
.shipment-head div { display: flex; flex-direction: column; gap: 7px; } .shipment-head span { color: #999; font-size: 13px; } .shipment-head strong { color: #333; } :deep(.el-timeline-item__content p) { margin: 6px 0 0; color: #888; font-size: 13px; }
</style>

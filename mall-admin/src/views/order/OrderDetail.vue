<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderById, shipOrder } from '@/api/order'
import type { OrderVO } from '@/types'

const route = useRoute()
const router = useRouter()
const order = ref<OrderVO | null>(null)
const loading = ref(false)

const shipDialogVisible = ref(false)
const shipForm = ref({ logisticsCompany: '', trackingNo: '' })

async function loadOrder() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try { order.value = await getOrderById(id) } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadOrder)

function getStatusTag(status: number) {
  const map: Record<number, { type: string; text: string }> = {
    0: { type: 'warning', text: '待付款' },
    1: { type: 'primary', text: '待发货' },
    2: { type: 'info', text: '待收货' },
    3: { type: 'success', text: '已完成' },
    4: { type: 'danger', text: '已取消' },
    5: { type: 'warning', text: '退款中' },
    6: { type: 'danger', text: '已退款' }
  }
  return map[status] || { type: 'info', text: '未知' }
}

const statusTextMap: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款'
}

void statusTextMap

const timelineItems = computed(() => {
  if (!order.value) return []
  const o = order.value

  if (o.timeline?.length) {
    return o.timeline.map(t => ({
      status: t.status,
      title: t.statusText || t.title || '',
      time: t.time,
      description: t.description
    }))
  }

  const items: { status: number; title: string; time?: string; description?: string }[] = []
  if (o.createTime) {
    items.push({ status: 0, title: '创建订单', time: o.createTime })
  }
  if (o.payTime) {
    items.push({ status: 1, title: '支付成功', time: o.payTime })
  }
  if (o.shipTime && o.orderStatus !== 5 && o.orderStatus !== 6) {
    items.push({
      status: 2,
      title: '商品出库',
      time: o.shipTime,
      description: o.logisticsCompany ? `${o.logisticsCompany} ${o.logisticsNo || ''}` : undefined
    })
  }
  if (o.receiveTime || o.orderStatus === 3) {
    items.push({ status: 3, title: '确认收货', time: o.receiveTime })
  }
  if (o.orderStatus === 5) {
    items.push({ status: 5, title: '退款中', time: o.updateTime || o.createTime })
  }
  if (o.orderStatus === 6) {
    items.push({ status: 6, title: '已退款', time: o.updateTime || o.createTime })
  }

  return items
})

function openShipDialog() {
  shipForm.value = { logisticsCompany: '', trackingNo: '' }
  shipDialogVisible.value = true
}

async function handleShip() {
  if (!order.value) return
  if (!shipForm.value.logisticsCompany || !shipForm.value.trackingNo) {
    ElMessage.warning('请填写物流公司和运单号')
    return
  }
  try {
    await ElMessageBox.confirm('确认发货？', '提示')
    await shipOrder(order.value.id, {
      logisticsCompany: shipForm.value.logisticsCompany,
      logisticsNo: shipForm.value.trackingNo
    })
    ElMessage.success('发货成功')
    shipDialogVisible.value = false
    loadOrder()
  } catch { /* cancelled or error */ }
}
</script>

<template>
  <div class="order-detail-page">
    <div class="page-header">
      <el-page-header @back="router.back()" content="订单详情" />
    </div>
    <div v-loading="loading" v-if="order" class="detail-content">
      <div class="info-card">
        <h3>基本信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTag(order.orderStatus).type">{{ getStatusTag(order.orderStatus).text }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户">{{ order.username || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="info-card" v-if="order.addressSnapshot">
        <h3>收货信息</h3>
        <p>{{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.receiverName : '' }} {{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.receiverPhone : '' }}</p>
        <p>{{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.province : '' }}{{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.city : '' }}{{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.district : '' }}{{ typeof order.addressSnapshot === 'object' ? order.addressSnapshot.detailAddress : '' }}</p>
      </div>
      <div class="info-card">
        <h3>商品清单</h3>
        <el-table :data="order.items || []" border>
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="skuInfo" label="规格" width="150" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="subtotal" label="小计" width="100">
            <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div class="info-card">
        <h3>费用信息</h3>
        <el-descriptions :column="1" border style="max-width: 400px">
          <el-descriptions-item label="商品总额">¥{{ order.totalAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="运费">¥{{ order.freightAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">¥{{ order.discountAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额"><strong style="color:#ff6b35;font-size:18px">¥{{ order.payAmount?.toFixed(2) }}</strong></el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-card" v-if="order.orderStatus >= 1 && order.orderStatus !== 4 && order.orderStatus !== 5 && order.orderStatus !== 6">
        <h3>物流信息</h3>
        <div v-if="order.orderStatus === 1" class="ship-action">
          <el-button type="primary" @click="openShipDialog">立即发货</el-button>
        </div>
        <div v-else>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="物流公司">{{ order.logisticsCompany || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运单号">{{ order.logisticsNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货时间">{{ order.receiveTime || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <div class="info-card">
        <h3>订单时间线</h3>
        <el-timeline v-if="timelineItems.length > 0">
          <el-timeline-item
            v-for="(item, index) in timelineItems"
            :key="index"
            :timestamp="item.time"
            :color="order.orderStatus >= item.status ? '#1890ff' : '#e4e7ed'"
          >
            <div class="timeline-title">{{ item.title }}</div>
            <div v-if="item.description" class="timeline-desc">{{ item.description }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无时间记录" />
      </div>
    </div>

    <el-dialog v-model="shipDialogVisible" title="订单发货" width="500px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" filterable>
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="申通快递" value="申通快递" />
            <el-option label="邮政EMS" value="邮政EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.order-detail-page { .page-header { margin-bottom: 20px; } }
.detail-content { display: flex; flex-direction: column; gap: 16px; }
.info-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  h3 { margin: 0 0 16px; font-size: 16px; }
}
.ship-action {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}
.timeline-title {
  font-weight: 600;
  color: #333;
}
.timeline-desc {
  color: #666;
  font-size: 13px;
  margin-top: 4px;
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAddressList } from '@/api/address'
import { createOrder } from '@/api/order'
import { getUsableCoupons } from '@/api/common'
import { getProductDetail } from '@/api/product'
import { getShippingTemplates, type ShippingTemplate } from '@/api/shipping'
import type { Address, UsableCoupon, OrderItemDTO, ProductDetailVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()

interface CheckoutItem {
  productId: number
  skuId: number
  quantity: number
  productDetail?: ProductDetailVO
  price?: number
  productName?: string
  productImage?: string
  skuInfo?: string
}

const addresses = ref<Address[]>([])
const coupons = ref<UsableCoupon[]>([])
const selectedAddressId = ref<number | null>(null)
const selectedCouponId = ref<number | null>(null)
const remark = ref('')
const items = ref<CheckoutItem[]>([])
const loading = ref(false)
const shippingTemplates = ref<ShippingTemplate[]>([])
const selectedShippingTemplateId = ref<number | null>(null)

async function loadItemsDetail() {
  const saved = sessionStorage.getItem('checkout_items')
  if (saved) {
    const parsed: CheckoutItem[] = JSON.parse(saved)
    items.value = parsed
    for (const item of items.value) {
      try {
        const detail = await getProductDetail(item.productId)
        item.productDetail = detail
        item.productName = detail.name
        item.productImage = detail.mainImage
        const sku = detail.skus?.find(s => s.id === item.skuId)
        item.price = sku?.price ?? detail.price
        item.skuInfo = sku?.specInfo
      } catch {
        item.productName = `商品#${item.productId}`
      }
    }
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadItemsDetail()
    if (!items.value.length) {
      ElMessage.warning('没有待结算的商品')
      router.push({ name: 'Cart' })
      return
    }
    addresses.value = await getAddressList()
    const defaultAddr = addresses.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id!
    } else if (addresses.value.length) {
      selectedAddressId.value = addresses.value[0].id!
    }
    coupons.value = await getUsableCoupons()
    shippingTemplates.value = await getShippingTemplates()
    if (shippingTemplates.value.length) selectedShippingTemplateId.value = shippingTemplates.value[0].id
  } catch {
    /* handled */
  } finally {
    loading.value = false
  }
})

const itemCount = computed(() =>
  items.value.reduce((sum, i) => sum + i.quantity, 0)
)

const subtotal = computed(() =>
  items.value.reduce((sum, i) => sum + (i.price || 0) * i.quantity, 0)
)

const selectedCoupon = computed(() =>
  selectedCouponId.value ? coupons.value.find(c => c.id === selectedCouponId.value) : null
)

const discountAmount = computed(() => {
  const coupon = selectedCoupon.value
  if (!coupon) return 0
  if (coupon.type === 1 || coupon.type === 3) {
    return coupon.value
  }
  if (coupon.type === 2) {
    return Math.round(subtotal.value * (100 - coupon.value) / 100)
  }
  return 0
})

const selectedShippingTemplate = computed(() => shippingTemplates.value.find(item => item.id === selectedShippingTemplateId.value))
const freightAmount = computed(() => {
  const template = selectedShippingTemplate.value
  if (!template || (template.freeAmount != null && subtotal.value >= template.freeAmount)) return 0
  return template.baseFreight || 0
})
const payAmount = computed(() => Math.max(0, subtotal.value - discountAmount.value) + freightAmount.value)
function templateFreightText(template: ShippingTemplate) {
  if (template.freeAmount != null && subtotal.value >= template.freeAmount) return '免运费'
  return `运费 ¥${template.baseFreight.toFixed(2)}`
}
function templateFreeShippingText(template: ShippingTemplate) {
  return template.freeAmount != null ? `，满¥${template.freeAmount.toFixed(2)}包邮` : ''
}

function selectCoupon(id: number | null) {
  if (id !== null) {
    const coupon = coupons.value.find(c => c.id === id)
    if (coupon && subtotal.value < (coupon.minAmount || 0)) {
      ElMessage.warning(`订单满 ¥${coupon.minAmount} 才可使用此优惠券`)
      return
    }
  }
  selectedCouponId.value = id
}

async function submitOrder() {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (!items.value.length) {
    ElMessage.warning('没有待结算的商品')
    return
  }
  const orderItems: OrderItemDTO[] = items.value.map(i => ({
    productId: i.productId,
    skuId: i.skuId,
    quantity: i.quantity
  }))
  try {
    const order = await createOrder({
      addressId: selectedAddressId.value,
      couponId: selectedCouponId.value ?? undefined,
      shippingTemplateId: selectedShippingTemplateId.value ?? undefined,
      deliveryMethod: selectedShippingTemplate.value?.deliveryMethod,
      remark: remark.value || undefined,
      items: orderItems
    })
    sessionStorage.removeItem('checkout_items')
    sessionStorage.setItem('pending_order_id', String(order.id))
    ElMessage.success('下单成功，请完成支付')
    router.push({ name: 'Payment', params: { id: order.id } })
  } catch {
    /* handled */
  }
}

function goBack() {
  router.push({ name: 'Cart' })
}
</script>

<template>
  <div class="order-create-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <div class="page-header">
          <el-page-header @back="goBack" content="确认订单" />
        </div>
        <div class="layout">
          <div class="left-section">
            <div class="section-card">
              <h3>收货地址</h3>
              <div v-if="addresses.length === 0" class="empty-tip">
                <p>还没有收货地址</p>
                <el-button type="primary" @click="router.push('/addresses')">添加地址</el-button>
              </div>
              <div v-else class="address-list">
                <div
                  v-for="addr in addresses"
                  :key="addr.id"
                  class="address-item"
                  :class="{ active: selectedAddressId === addr.id }"
                  @click="addr.id && (selectedAddressId = addr.id)"
                >
                  <div class="addr-header">
                    <span class="name">{{ addr.receiverName }}</span>
                    <span class="phone">{{ addr.receiverPhone }}</span>
                    <el-tag v-if="addr.isDefault" type="warning" size="small">默认</el-tag>
                  </div>
                  <p class="addr-detail">
                    {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
                  </p>
                </div>
              </div>
            </div>

            <div class="section-card">
              <h3>商品清单</h3>
              <div class="item-list">
                <div v-for="(item, idx) in items" :key="idx" class="item-row">
                  <img :src="item.productImage || '/favicon.svg'" class="item-img" :alt="''" />
                  <div class="item-info">
                    <p class="item-name">{{ item.productName }}</p>
                    <p class="item-sku">{{ item.skuInfo }}</p>
                  </div>
                  <div class="item-price">¥{{ (item.price || 0).toFixed(2) }}</div>
                  <div class="item-qty">× {{ item.quantity }}</div>
                  <div class="item-subtotal">¥{{ ((item.price || 0) * item.quantity).toFixed(2) }}</div>
                </div>
              </div>
            </div>

            <div class="section-card">
              <h3>优惠券</h3>
              <div v-if="coupons.length === 0" class="empty-tip">
                <p>暂无可用优惠券</p>
              </div>
              <div v-else class="coupon-list">
                <div
                  v-for="coupon in coupons"
                  :key="coupon.id"
                  class="coupon-item"
                  :class="{
                    active: selectedCouponId === coupon.id,
                    disabled: subtotal < (coupon.minAmount || 0)
                  }"
                  @click="selectCoupon(coupon.id)"
                >
                  <span class="coupon-value" v-if="coupon.type === 1">¥{{ coupon.value }}</span>
                    <span class="coupon-value" v-else-if="coupon.type === 2">{{ coupon.value }}折</span>
                    <span class="coupon-value" v-else>¥{{ coupon.value }}</span>
                  <span class="coupon-info">
                    <span class="coupon-name">{{ coupon.name }}</span>
                    <span class="coupon-condition" v-if="coupon.type === 1 && coupon.minAmount">满¥{{ coupon.minAmount }}可用</span>
                    <span class="coupon-condition" v-else-if="coupon.type === 2">全品类{{ coupon.value }}折</span>
                    <span class="coupon-condition" v-else>无门槛</span>
                    <span class="coupon-condition" v-if="coupon.endTime">有效期至 {{ coupon.endTime.replace('T', ' ').substring(0, 10) }}</span>
                  </span>
                </div>
                <div
                  class="coupon-item none"
                  :class="{ active: selectedCouponId === null }"
                  @click="selectCoupon(null)"
                >
                  <span>不使用优惠券</span>
                </div>
              </div>
            </div>

            <div class="section-card">
              <h3>配送方式</h3>
              <el-radio-group v-if="shippingTemplates.length" v-model="selectedShippingTemplateId">
                <el-radio v-for="template in shippingTemplates" :key="template.id" :value="template.id">
                  {{ template.name }}（{{ templateFreightText(template) }}{{ templateFreeShippingText(template) }}）
                </el-radio>
              </el-radio-group>
              <span v-else>商家包邮</span>
            </div>

            <div class="section-card">
              <h3>订单备注</h3>
              <el-input
                v-model="remark"
                type="textarea"
                :rows="2"
                placeholder="选填，请与卖家协商一致"
                maxlength="200"
                show-word-limit
              />
            </div>
          </div>

          <div class="right-section">
            <div class="summary-card">
              <h3>订单金额</h3>
              <div class="summary-row">
                <span>商品数量</span>
                <span>{{ itemCount }} 件</span>
              </div>
              <div class="summary-row">
                <span>商品总额</span>
                <span>¥{{ subtotal.toFixed(2) }}</span>
              </div>
              <div class="summary-row">
                <span>运费</span>
                <span>{{ freightAmount > 0 ? `¥${freightAmount.toFixed(2)}` : '免运费' }}</span>
              </div>
              <div class="summary-row discount" v-if="discountAmount > 0">
                <span>优惠</span>
                <span class="discount-amount">-¥{{ discountAmount.toFixed(2) }}</span>
              </div>
              <div class="summary-row total">
                <span>应付金额</span>
                <span class="pay-amount">¥{{ payAmount.toFixed(2) }}</span>
              </div>
              <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :disabled="!selectedAddressId"
                :loading="loading"
                @click="submitOrder"
              >
                提交订单
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.order-create-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.page-header { margin-bottom: 16px; }

.layout { display: flex; gap: 20px; }
.left-section { flex: 1; display: flex; flex-direction: column; gap: 16px; }
.right-section { width: 320px; flex-shrink: 0; }

.section-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;

  h3 { margin: 0 0 16px; font-size: 16px; color: #333; }
}

.empty-tip { text-align: center; padding: 20px; color: #999; p { margin-bottom: 12px; } }

.address-list { display: flex; flex-direction: column; gap: 12px; }
.address-item {
  border: 2px solid #eee;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;

  &.active { border-color: #ff6b35; background: #fffaf7; }
  .addr-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
  .name { font-size: 16px; font-weight: 500; }
  .phone { color: #666; }
  .addr-detail { color: #999; margin: 0; font-size: 14px; }
}

.item-list { display: flex; flex-direction: column; gap: 0; }
.item-row {
  display: grid;
  grid-template-columns: 60px 2fr 80px 60px 80px;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;
  &:last-child { border-bottom: none; }
  .item-img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; }
  .item-info p { margin: 0; }
  .item-name { font-size: 14px; color: #333; }
  .item-sku { font-size: 12px; color: #999; }
  .item-price, .item-qty { text-align: center; color: #666; }
  .item-subtotal { text-align: right; color: #ff6b35; font-weight: 600; }
}

.coupon-list { display: flex; flex-direction: column; gap: 12px; }
.coupon-item {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;

  &.active { border-color: #ff6b35; background: #fffaf7; }
  &.disabled { opacity: 0.5; cursor: not-allowed; }
  &.none { justify-content: center; color: #999; }

  .coupon-value {
    background: #ff6b35;
    color: #fff;
    padding: 4px 12px;
    border-radius: 4px;
    font-weight: 600;
    min-width: 60px;
    text-align: center;
  }
  .coupon-name { flex: 1; }
  .coupon-condition { color: #999; font-size: 12px; }
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  position: sticky;
  top: 100px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;

  &.total {
    border-top: 2px solid #f0f0f0;
    border-bottom: none;
    padding-top: 16px;
    margin-top: 8px;
  }
  &.discount .discount-amount { color: #52c41a; }
  .pay-amount { color: #ff6b35; font-size: 24px; font-weight: 700; }
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
  background: #ff6b35;
  border-color: #ff6b35;
  &:hover { background: #ff5722; border-color: #ff5722; }
}
</style>

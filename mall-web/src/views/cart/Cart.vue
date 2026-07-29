<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => { cartStore.fetchCart() })

const cartItems = computed(() => cartStore.items)
const allSelected = computed(() => cartStore.allSelected)
const selectedTotal = computed(() => cartStore.selectedTotal)
const selectedCount = computed(() => cartStore.selectedItems.length)

async function toggleSelectAll() {
  await cartStore.toggleSelectAll()
}

async function updateQty(id: number, qty: number) {
  if (qty < 1) return
  await cartStore.updateQuantity(id, qty)
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？', '提示')
  await cartStore.removeItems([id])
}

async function removeSelected() {
  if (!selectedCount.value) {
    ElMessage.warning('请先选择商品')
    return
  }
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedCount.value} 件商品吗？`, '提示')
  cartStore.clearSelected()
}

function goCheckout() {
  if (!selectedCount.value) {
    ElMessage.warning('请先选择商品')
    return
  }
  const selected = cartStore.selectedItems
  const items = selected.map(i => ({ productId: i.productId, skuId: i.skuId, quantity: i.quantity }))
  sessionStorage.setItem('checkout_items', JSON.stringify(items))
  router.push({ name: 'OrderCreate' })
}

function goShopping() {
  router.push({ name: 'Home' })
}
</script>

<template>
  <div class="cart-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">购物车</h1>
        <div v-if="cartItems.length === 0" class="empty-cart">
          <div class="empty-icon">🛒</div>
          <p>购物车是空的</p>
          <el-button type="primary" @click="goShopping">去逛逛</el-button>
        </div>
        <template v-else>
          <div class="cart-list">
            <div class="cart-header">
              <el-checkbox :model-value="allSelected" @change="toggleSelectAll">全选</el-checkbox>
              <span class="col-product">商品信息</span>
              <span class="col-price">单价</span>
              <span class="col-qty">数量</span>
              <span class="col-subtotal">小计</span>
              <span class="col-action">操作</span>
            </div>
            <div v-for="item in cartItems" :key="item.id" class="cart-item">
              <el-checkbox :model-value="item.selected === 1" @change="cartStore.toggleSelect(item.id)" />
              <div class="col-product">
                <img :src="item.productImage" :alt="item.productName" class="product-img" />
                <div class="product-detail">
                  <h4>{{ item.productName }}</h4>
                  <p class="sku-info">{{ item.skuInfo }}</p>
                </div>
              </div>
              <div class="col-price">¥{{ item.price.toFixed(2) }}</div>
              <div class="col-qty">
                <el-input-number
                  :model-value="item.quantity"
                  :min="1"
                  :max="item.stock || 99"
                  @change="(val: number | undefined) => val !== undefined && updateQty(item.id, val)"
                />
              </div>
              <div class="col-subtotal price">¥{{ (item.price * item.quantity).toFixed(2) }}</div>
              <div class="col-action">
                <el-button link type="danger" @click="removeItem(item.id)">删除</el-button>
              </div>
            </div>
          </div>
          <div class="cart-footer">
            <div class="footer-left">
              <el-checkbox :model-value="allSelected" @change="toggleSelectAll">全选</el-checkbox>
              <el-button link @click="removeSelected">删除选中</el-button>
            </div>
            <div class="footer-right">
              <span class="selected-count">已选 {{ selectedCount }} 件商品</span>
              <span class="total-label">合计：</span>
              <span class="total-price">¥{{ selectedTotal.toFixed(2) }}</span>
              <el-button type="primary" size="large" @click="goCheckout" :disabled="selectedCount === 0">
                去结算
              </el-button>
            </div>
          </div>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.cart-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.page-title { font-size: 20px; margin: 0 0 16px; }

.empty-cart {
  background: #fff;
  border-radius: 12px;
  padding: 80px;
  text-align: center;
  .empty-icon { font-size: 64px; margin-bottom: 16px; }
  p { color: #999; margin-bottom: 24px; }
}

.cart-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.cart-header, .cart-item {
  display: grid;
  grid-template-columns: 40px 2fr 1fr 120px 1fr 80px;
  align-items: center;
  padding: 16px 24px;
  gap: 16px;
}

.cart-header {
  background: #fafafa;
  font-weight: 500;
  color: #666;
  font-size: 14px;
}

.cart-item {
  border-top: 1px solid #f0f0f0;
  &:hover { background: #fffaf7; }

  .col-product { display: flex; align-items: center; gap: 12px; }
  .product-img {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    object-fit: cover;
  }
  .product-detail h4 { margin: 0 0 4px; font-size: 14px; color: #333; }
  .sku-info { margin: 0; font-size: 12px; color: #999; }

  .col-price, .col-subtotal { text-align: center; color: #666; }
  .col-subtotal.price { color: #ff6b35; font-weight: 600; font-size: 16px; }
  .col-action { text-align: center; }
}

.cart-footer {
  position: sticky;
  bottom: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px 24px;
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);

  .footer-left { display: flex; align-items: center; gap: 24px; }
  .footer-right { display: flex; align-items: center; gap: 16px; }
  .selected-count { color: #666; font-size: 14px; }
  .total-label { color: #333; }
  .total-price { color: #ff6b35; font-size: 24px; font-weight: 700; }
}
</style>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderById } from '@/api/order'
import { addReview } from '@/api/review'
import type { OrderVO } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.id))
const order = ref<OrderVO | null>(null)
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  rating: 5,
  content: '',
  images: ''
})

const hoverRating = ref(0)
const ratingDescriptions = ['非常不满意', '不满意', '一般', '满意', '非常满意']

onMounted(async () => {
  try {
    order.value = await getOrderById(orderId.value)
    if (order.value.orderStatus !== 3) {
      ElMessage.warning('仅已完成的订单可以评价')
      router.push({ name: 'OrderDetail', params: { id: orderId.value } })
    }
  } catch {
    ElMessage.error('获取订单信息失败')
    router.push({ name: 'OrderList' })
  } finally {
    loading.value = false
  }
})

function setRating(rating: number) {
  form.value.rating = rating
}

async function handleSubmit() {
  if (!form.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  if (form.value.content.trim().length < 5) {
    ElMessage.warning('评价内容至少5个字')
    return
  }

  try {
    submitting.value = true
    const firstItem = order.value?.items?.[0]
    await addReview({
      productId: firstItem?.productId || 0,
      orderId: orderId.value,
      rating: form.value.rating,
      content: form.value.content.trim(),
      images: form.value.images || undefined
    })
    ElMessage.success('评价提交成功，感谢您的反馈')
    router.push({ name: 'OrderDetail', params: { id: orderId.value } })
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="review-create-page">
    <AppHeader />
    <main class="main-content">
      <div class="container" v-loading="loading">
        <template v-if="order">
          <div class="page-header">
            <el-page-header @back="goBack" content="发表评价" />
          </div>

          <div class="order-summary">
            <h3>订单商品</h3>
            <div class="item-list">
              <div v-for="item in order.items" :key="item.id" class="item-row">
                <img :src="item.productImage" :alt="''" class="item-img" />
                <div class="item-info">
                  <p class="item-name">{{ item.productName }}</p>
                  <p class="item-sku">{{ item.skuInfo }}</p>
                </div>
                <div class="item-price">¥{{ item.price.toFixed(2) }} × {{ item.quantity }}</div>
              </div>
            </div>
          </div>

          <div class="review-form">
            <div class="form-section">
              <h3>总体评分</h3>
              <div class="rating-section">
                <div class="rating-stars">
                  <span
                    v-for="star in 5"
                    :key="star"
                    class="star"
                    :class="{ active: star <= (hoverRating || form.rating) }"
                    @mouseenter="hoverRating = star"
                    @mouseleave="hoverRating = 0"
                    @click="setRating(star)"
                  >★</span>
                </div>
                <span class="rating-label">{{ ratingDescriptions[(hoverRating || form.rating) - 1] || '' }}</span>
              </div>
            </div>

            <div class="form-section">
              <h3>评价内容</h3>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="6"
                placeholder="请分享您对商品的真实感受，至少5个字..."
                maxlength="500"
                show-word-limit
              />
            </div>

            <div class="form-section">
              <h3>上传图片</h3>
              <el-input
                v-model="form.images"
                placeholder="请输入图片URL，多个用英文逗号分隔"
              />
              <p class="form-tip">支持上传商品实拍图、问题截图等，最多5张</p>
            </div>

            <div class="form-actions">
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="handleSubmit"
              >
                提交评价
              </el-button>
              <el-button size="large" @click="goBack">取消</el-button>
            </div>
          </div>
        </template>
      </div>
    </main>
    <AppFooter />
  </div>
</template>



<style scoped lang="scss">
.review-create-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 800px; margin: 0 auto; padding: 0 20px; }
.page-header { margin-bottom: 24px; }

.order-summary {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;

  h3 { margin: 0 0 16px; font-size: 16px; }
}

.item-list { display: flex; flex-direction: column; }
.item-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child { border-bottom: none; }
  .item-img { width: 60px; height: 60px; border-radius: 6px; object-fit: cover; }
  .item-info { flex: 1; p { margin: 0; } }
  .item-name { font-size: 14px; color: #333; }
  .item-sku { font-size: 12px; color: #999; }
  .item-price { color: #666; font-size: 14px; }
}

.review-form {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.form-section {
  padding: 16px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-of-type { border-bottom: none; }
  h3 { margin: 0 0 12px; font-size: 16px; color: #333; }
}

.rating-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rating-stars {
  display: flex;
  gap: 6px;

  .star {
    font-size: 36px;
    color: #e0e0e0;
    cursor: pointer;
    transition: color 0.2s;

    &.active { color: #faad14; }
  }
}

.rating-label {
  font-size: 16px;
  color: #faad14;
  font-weight: 500;
}

.form-tip { color: #999; font-size: 12px; margin: 8px 0 0; }

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding-top: 24px;
}
</style>
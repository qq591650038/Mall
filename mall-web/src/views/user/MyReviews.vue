<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReviews } from '@/api/review'
import type { Review } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const list = ref<Review[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
async function load(current = 1) { loading.value = true; try { const result = await getMyReviews({ current, size: 8 }); list.value = result.list || []; total.value = result.total || 0; page.value = current } finally { loading.value = false } }
function imageList(images?: string) { if (!images) return []; try { const value = JSON.parse(images); return Array.isArray(value) ? value : [] } catch { return images.split(',').map(item => item.trim()).filter(Boolean) } }
onMounted(() => load())
</script>

<template><div class="page"><AppHeader /><main><div class="container"><h1>我的评价</h1><div class="list" v-loading="loading">
  <el-empty v-if="!loading && !list.length" description="还没有发表过评价" />
  <article v-for="review in list" :key="review.id" class="item"><div class="head"><button @click="router.push(`/products/${review.productId}`)">{{ review.productName || `商品 ${review.productId}` }}</button><time>{{ review.createTime?.replace('T', ' ').slice(0, 16) }}</time></div><el-rate :model-value="review.rating" disabled /><p>{{ review.content }}</p>
    <div v-if="imageList(review.images).length" class="images"><el-image v-for="image in imageList(review.images)" :key="image" :src="image" :preview-src-list="imageList(review.images)" fit="cover" /></div><div v-if="review.reply" class="reply"><strong>商家回复</strong>{{ review.reply }}</div></article>
  <el-pagination v-if="total > 8" background layout="prev, pager, next" :page-size="8" :current-page="page" :total="total" @current-change="load" />
</div></div></main><AppFooter /></div></template>

<style scoped lang="scss">
.page { min-height: 100vh; background: #f5f5f5; } main { padding: 24px 0; } .container { max-width: 960px; margin: 0 auto; padding: 0 20px; } h1 { margin: 0 0 18px; font-size: 21px; } .list { min-height: 360px; } .item { padding: 20px; margin-bottom: 12px; background: #fff; border-radius: 8px; }
.head { display: flex; justify-content: space-between; margin-bottom: 8px; } .head button { padding: 0; border: 0; background: none; color: #333; font-weight: 600; cursor: pointer; } time { color: #999; font-size: 12px; } .item > p { margin: 12px 0; color: #444; line-height: 1.7; }
.images { display: flex; gap: 8px; } .images :deep(.el-image) { width: 72px; height: 72px; border-radius: 6px; } .reply { margin-top: 14px; padding: 10px 12px; background: #f5f7fa; color: #666; font-size: 13px; } .reply strong { margin-right: 10px; color: #C4908F; } :deep(.el-pagination) { justify-content: center; margin-top: 18px; }
</style>

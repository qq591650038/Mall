<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getReviewPage, replyReview, updateReviewStatus } from '@/api/review'
import type { Review } from '@/types'

const reviews = ref<Review[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const size = ref(10)
const rating = ref<number>()
const status = ref<number>()
const replyVisible = ref(false)
const replying = ref(false)
const selected = ref<Review>()
const replyContent = ref('')

async function loadReviews() {
  loading.value = true
  try {
    const data = await getReviewPage({ current: current.value, size: size.value, rating: rating.value, status: status.value })
    reviews.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function search() { current.value = 1; loadReviews() }
function openReply(review: Review) { selected.value = review; replyContent.value = review.reply || ''; replyVisible.value = true }
async function submitReply() {
  if (!selected.value || !replyContent.value.trim()) { ElMessage.warning('请输入回复内容'); return }
  replying.value = true
  try { await replyReview(selected.value.id, replyContent.value.trim()); ElMessage.success('回复成功'); replyVisible.value = false; loadReviews() }
  finally { replying.value = false }
}
async function changeStatus(review: Review) {
  const next = review.status === 1 ? 0 : 1
  await updateReviewStatus(review.id, next)
  ElMessage.success(next === 1 ? '评价已显示' : '评价已隐藏')
  loadReviews()
}

onMounted(loadReviews)
</script>

<template>
  <div class="review-list-page">
    <div class="page-header"><h2>评价管理</h2></div>
    <div class="filter-bar">
      <el-select v-model="rating" clearable placeholder="评价星级" style="width: 140px">
        <el-option v-for="star in 5" :key="star" :label="`${star} 星`" :value="star" />
      </el-select>
      <el-select v-model="status" clearable placeholder="显示状态" style="width: 140px">
        <el-option label="显示中" :value="1" /><el-option label="已隐藏" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <div class="table-card">
      <el-table :data="reviews" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="星级" width="150"><template #default="{ row }"><el-rate :model-value="row.rating" disabled /></template></el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '显示' : '隐藏' }}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="openReply(row)">{{ row.reply ? '修改回复' : '回复' }}</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="changeStatus(row)">{{ row.status === 1 ? '隐藏' : '显示' }}</el-button>
        </template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && reviews.length === 0" description="暂无评价数据" />
      <el-pagination v-if="total" class="pagination" background layout="total, prev, pager, next" :page-size="size" :current-page="current" :total="total" @current-change="(page: number) => { current = page; loadReviews() }" />
    </div>
    <el-dialog v-model="replyVisible" title="回复评价" width="520px">
      <p class="review-content">{{ selected?.content }}</p>
      <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入商家回复" />
      <template #footer><el-button @click="replyVisible = false">取消</el-button><el-button type="primary" :loading="replying" @click="submitReply">确认回复</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.review-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header h2 { margin: 0; }
.filter-bar { display: flex; gap: 12px; }
.table-card { background: #fff; padding: 16px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.review-content { padding: 12px; margin: 0 0 16px; background: #f5f7fa; color: #606266; line-height: 1.6; }
</style>

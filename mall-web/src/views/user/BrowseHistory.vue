<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBrowseHistoryList, deleteBrowseHistory, clearBrowseHistory } from '@/api/browseHistory'
import type { BrowseHistory } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const list = ref<BrowseHistory[]>([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    list.value = await getBrowseHistoryList()
  } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadList)

async function handleDelete(productId: number) {
  try {
    await deleteBrowseHistory(productId)
    list.value = list.value.filter(item => item.productId !== productId)
    ElMessage.success('已删除')
  } catch { /* handled */ }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空所有浏览历史吗？', '提示', {
      type: 'warning'
    })
    await clearBrowseHistory()
    list.value = []
    ElMessage.success('已清空')
  } catch { /* handled */ }
}

function goDetail(id: number) {
  router.push({ name: 'ProductDetail', params: { id } })
}
</script>

<template>
  <div class="history-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <div class="header-bar">
          <h1 class="page-title">浏览历史</h1>
          <el-button
            v-if="list.length > 0"
            type="danger"
            plain
            size="small"
            @click="handleClear"
          >清空历史</el-button>
        </div>
        <div v-loading="loading">
          <div v-if="list.length === 0 && !loading" class="empty-state">
            <p>暂无浏览记录</p>
            <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
          </div>
          <div v-else class="history-grid">
            <div
              v-for="item in list"
              :key="item.productId"
              class="history-card"
            >
              <div class="product-image" @click="goDetail(item.productId)">
                <img :src="item.product?.mainImage" :alt="''" />
              </div>
              <div class="product-info">
                <h3 @click="goDetail(item.productId)">{{ item.product?.name }}</h3>
                <p class="time">{{ item.browseTime }}</p>
                <div class="bottom">
                  <span class="price">¥{{ item.product?.price?.toFixed(2) }}</span>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click="handleDelete(item.productId)"
                  >删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.history-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title { font-size: 20px; margin: 0; }

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;
  p { margin-bottom: 24px; }
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.history-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;

  .product-image {
    aspect-ratio: 1;
    overflow: hidden;
    background: #f9f9f9;
    cursor: pointer;
    img { width: 100%; height: 100%; object-fit: cover; }
  }

  .product-info { padding: 12px; }
  h3 { margin: 0 0 6px; font-size: 14px; color: #333; cursor: pointer; }
  .time { margin: 0 0 8px; font-size: 12px; color: #999; }
  .bottom { display: flex; justify-content: space-between; align-items: center; }
  .price { color: #C4908F; font-size: 16px; font-weight: 600; }
}
</style>
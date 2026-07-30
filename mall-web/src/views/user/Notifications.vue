<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import { getNotificationPage, markAllNotificationsRead, markNotificationRead, type Notification } from '@/api/notification'

const router = useRouter()
const list = ref<Notification[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const unreadOnly = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await getNotificationPage({ current: current.value, size: 12, unreadOnly: unreadOnly.value || undefined })
    list.value = result.list || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

async function read(item: Notification) {
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = 1
  }
  if (item.businessType === 'ORDER' && item.businessId) {
    router.push({ name: 'OrderDetail', params: { id: item.businessId } })
  } else if (item.businessType === 'REFUND' && item.businessId) {
    router.push({ name: 'RefundList' })
  }
}

async function readAll() {
  await markAllNotificationsRead()
  list.value.forEach(item => { item.isRead = 1 })
  ElMessage.success('已全部标记为已读')
}

function changeFilter(value: boolean) {
  unreadOnly.value = value
  current.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="notifications-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <div class="page-heading">
          <div><h1>消息中心</h1><p>订单、退款和优惠活动的重要提醒</p></div>
          <el-button link type="primary" @click="readAll">全部已读</el-button>
        </div>
        <div class="toolbar">
          <el-radio-group :model-value="unreadOnly" @update:model-value="changeFilter">
            <el-radio-button :value="false">全部消息</el-radio-button>
            <el-radio-button :value="true">未读消息</el-radio-button>
          </el-radio-group>
        </div>
        <section class="message-panel" v-loading="loading">
          <article v-for="item in list" :key="item.id" class="message-item" :class="{ unread: !item.isRead }" @click="read(item)">
            <span class="dot" />
            <div class="message-body"><div class="message-title"><strong>{{ item.title }}</strong><time>{{ item.createTime?.replace('T', ' ').slice(0, 16) }}</time></div><p>{{ item.content }}</p></div>
          </article>
          <el-empty v-if="!loading && !list.length" description="暂无消息" />
        </section>
        <el-pagination v-if="total > 12" v-model:current-page="current" background layout="prev, pager, next" :page-size="12" :total="total" @current-change="load" />
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.notifications-page { min-height: 100vh; background: #f5f5f5; }
.main-content { padding: 24px 0 48px; }
.container { max-width: 900px; margin: 0 auto; padding: 0 20px; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 16px; }
.page-heading h1 { margin: 0; color: #333; font-size: 20px; }
.page-heading p { margin: 6px 0 0; color: #999; font-size: 13px; }
.toolbar, .message-panel { padding: 16px; background: #fff; border-radius: 12px; }
.toolbar { margin-bottom: 12px; }
.message-item { display: flex; gap: 12px; padding: 18px 8px; border-bottom: 1px solid #f2f2f2; cursor: pointer; }
.message-item:last-child { border-bottom: 0; }
.message-item .dot { width: 8px; height: 8px; margin-top: 7px; border-radius: 50%; background: transparent; }
.message-item.unread .dot { background: #C4908F; }
.message-body { flex: 1; min-width: 0; }
.message-title { display: flex; justify-content: space-between; gap: 16px; }
.message-title strong { color: #333; font-size: 14px; }
.message-title time { flex: none; color: #aaa; font-size: 12px; }
.message-body p { margin: 8px 0 0; color: #777; font-size: 13px; line-height: 1.6; }
.el-pagination { justify-content: center; margin-top: 24px; }
@media (max-width: 600px) { .container { padding: 0 12px; } .message-title { align-items: flex-start; flex-direction: column; gap: 4px; } }
</style>

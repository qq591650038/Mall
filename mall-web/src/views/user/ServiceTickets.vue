<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import { createServiceTicket, getServiceMessages, getServiceTickets, replyServiceTicket, type ServiceMessage, type ServiceTicket } from '@/api/serviceTicket'
import { getOrderPage } from '@/api/order'
import type { OrderVO } from '@/types'

const tickets = ref<ServiceTicket[]>([])
const messages = ref<ServiceMessage[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)
const dialogVisible = ref(false)
const activeTicket = ref<ServiceTicket | null>(null)
const reply = ref('')
const form = ref({ subject: '', category: 'ORDER', orderId: undefined as number | undefined, refundId: undefined as number | undefined, content: '' })
const orders = ref<OrderVO[]>([])
const ordersLoading = ref(false)
let messagePollTimer: ReturnType<typeof window.setInterval> | undefined
let pollingMessages = false

const statusText: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已结案' }
const statusType: Record<number, 'warning' | 'primary' | 'info'> = { 0: 'warning', 1: 'primary', 2: 'info' }

async function load() {
  loading.value = true
  try { const result = await getServiceTickets({ current: current.value, size: 10 }); tickets.value = result.list || []; total.value = result.total || 0 } finally { loading.value = false }
}
async function openTicket(ticket: ServiceTicket) { activeTicket.value = ticket; messages.value = await getServiceMessages(ticket.id) }
async function pollActiveTicket() {
  if (pollingMessages || document.visibilityState !== 'visible' || !activeTicket.value) return
  pollingMessages = true
  try { messages.value = await getServiceMessages(activeTicket.value.id) } catch { /* A later poll retries transient failures. */ } finally { pollingMessages = false }
}
function startMessagePolling() {
  messagePollTimer = window.setInterval(pollActiveTicket, 10000)
}
function stopMessagePolling() {
  if (messagePollTimer !== undefined) window.clearInterval(messagePollTimer)
  messagePollTimer = undefined
}
function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void pollActiveTicket()
    startMessagePolling()
  } else {
    stopMessagePolling()
  }
}
async function openCreateDialog() {
  dialogVisible.value = true
  if (orders.value.length) return
  ordersLoading.value = true
  try {
    const result = await getOrderPage({ current: 1, size: 100 })
    orders.value = result.list || []
  } finally {
    ordersLoading.value = false
  }
}
async function submit() {
  if (!form.value.subject.trim() || !form.value.content.trim()) { ElMessage.warning('请填写工单标题和问题描述'); return }
  if (form.value.category !== 'ACCOUNT' && !form.value.orderId) { ElMessage.warning('请选择关联订单'); return }
  const ticket = await createServiceTicket({ ticket: { subject: form.value.subject, category: form.value.category, orderId: form.value.orderId, refundId: form.value.refundId }, content: form.value.content })
  dialogVisible.value = false; form.value = { subject: '', category: 'ORDER', orderId: undefined, refundId: undefined, content: '' }; await load(); await openTicket(ticket); ElMessage.success('工单已提交')
}
async function sendReply() { if (!activeTicket.value || !reply.value.trim()) return; await replyServiceTicket(activeTicket.value.id, reply.value); reply.value = ''; await openTicket(activeTicket.value) }
onMounted(async () => { await load(); document.addEventListener('visibilitychange', handleVisibilityChange); startMessagePolling() })
onBeforeUnmount(() => { document.removeEventListener('visibilitychange', handleVisibilityChange); stopMessagePolling() })
</script>

<template>
  <div class="tickets-page"><AppHeader /><main class="main-content"><div class="container">
    <header class="heading"><div><h1>客服工单</h1><p>订单、物流和售后问题可在这里提交并跟进处理进度。</p></div><el-button type="primary" @click="openCreateDialog">创建工单</el-button></header>
    <div class="layout"><section class="ticket-list" v-loading="loading"><el-empty v-if="!tickets.length && !loading" description="暂无工单" />
      <article v-for="ticket in tickets" :key="ticket.id" class="ticket" :class="{ active: activeTicket?.id === ticket.id }" @click="openTicket(ticket)"><div><strong>#{{ ticket.id }} {{ ticket.subject }}</strong><p>{{ ticket.category }} · {{ ticket.updateTime?.replace('T', ' ').slice(0, 16) }}</p></div><el-tag :type="statusType[ticket.status]">{{ statusText[ticket.status] }}</el-tag></article>
      <el-pagination v-if="total > 10" v-model:current-page="current" small background layout="prev, pager, next" :page-size="10" :total="total" @current-change="load" />
    </section><section class="conversation"><el-empty v-if="!activeTicket" description="选择工单查看处理记录" />
      <template v-else><header><strong>{{ activeTicket.subject }}</strong><el-tag :type="statusType[activeTicket.status]">{{ statusText[activeTicket.status] }}</el-tag></header><div class="messages"><article v-for="message in messages" :key="message.id" :class="['message', message.senderRole === 'USER' ? 'mine' : 'staff']"><span>{{ message.senderRole === 'USER' ? '我' : '客服' }}</span><p>{{ message.content }}</p><time>{{ message.createTime?.replace('T', ' ').slice(0, 16) }}</time></article></div><div v-if="activeTicket.status !== 2" class="reply"><el-input v-model="reply" type="textarea" :rows="2" placeholder="补充问题或回复客服" /><el-button type="primary" @click="sendReply">发送</el-button></div></template>
    </section></div>
  </div></main><AppFooter />
  <el-dialog v-model="dialogVisible" title="创建客服工单" width="480px"><el-form label-width="80px"><el-form-item label="问题类型"><el-select v-model="form.category"><el-option label="订单问题" value="ORDER" /><el-option label="物流配送" value="LOGISTICS" /><el-option label="售后争议" value="AFTER_SALE" /><el-option label="账户问题" value="ACCOUNT" /></el-select></el-form-item><el-form-item v-if="form.category !== 'ACCOUNT'" label="关联订单" required><el-select v-model="form.orderId" :loading="ordersLoading" filterable placeholder="请选择我的订单" style="width:100%"><el-option v-for="order in orders" :key="order.id" :label="`${order.orderNo} · ¥${order.payAmount.toFixed(2)} · ${order.orderStatusText}`" :value="order.id" /></el-select><div v-if="!ordersLoading && !orders.length" class="order-tip">暂无可关联订单</div></el-form-item><el-form-item label="标题"><el-input v-model="form.subject" maxlength="100" /></el-form-item><el-form-item label="问题描述"><el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">提交</el-button></template></el-dialog>
  </div>
</template>

<style scoped lang="scss">
.tickets-page { min-height: 100vh; background: #f5f5f5; } .main-content { padding: 24px 0 48px; } .container { max-width: 1100px; margin: auto; padding: 0 20px; } .heading { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; } h1 { margin:0; font-size:22px; } .heading p { color:#888; font-size:13px; } .layout { display:grid; grid-template-columns: 380px 1fr; gap:16px; min-height:460px; } .ticket-list,.conversation { background:#fff; border-radius:12px; padding:16px; } .ticket { display:flex; justify-content:space-between; gap:12px; padding:14px 8px; border-bottom:1px solid #f1f1f1; cursor:pointer; } .ticket.active { background:#fff5f0; } .ticket strong { font-size:14px; color:#333; } .ticket p { margin:7px 0 0; color:#999; font-size:12px; } .conversation { display:flex; flex-direction:column; } .conversation > header { display:flex; justify-content:space-between; padding-bottom:14px; border-bottom:1px solid #eee; } .messages { flex:1; padding:14px 0; overflow:auto; } .message { max-width:78%; margin-bottom:12px; } .message.mine { margin-left:auto; text-align:right; } .message span,.message time { color:#999; font-size:12px; } .message p { display:inline-block; margin:5px 0; padding:10px 12px; text-align:left; background:#f4f4f5; border-radius:8px; line-height:1.5; } .message.mine p { background:#fff0e8; } .reply { display:flex; gap:10px; } .reply .el-button { align-self:end; } @media(max-width:760px){.layout{grid-template-columns:1fr}.conversation{min-height:400px}.heading{align-items:flex-start}.container{padding:0 12px}}
</style>

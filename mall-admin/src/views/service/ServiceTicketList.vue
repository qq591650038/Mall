<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { closeServiceTicket, getServiceMessages, getServiceTickets, replyServiceTicket, type ServiceMessage, type ServiceTicket } from '@/api/serviceTicket'

const rows = ref<ServiceTicket[]>([])
const total = ref(0)
const current = ref(1)
const status = ref<number | undefined>()
const loading = ref(false)
const detailVisible = ref(false)
const selected = ref<ServiceTicket | null>(null)
const messages = ref<ServiceMessage[]>([])
const reply = ref('')
const statusText: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已结案' }
const statusType: Record<number, 'warning' | 'primary' | 'info'> = { 0: 'warning', 1: 'primary', 2: 'info' }

async function load() { loading.value = true; try { const result = await getServiceTickets({ current: current.value, size: 15, status: status.value }); rows.value = result.list || []; total.value = result.total || 0 } finally { loading.value = false } }
async function open(row: ServiceTicket) { selected.value = row; messages.value = await getServiceMessages(row.id); detailVisible.value = true }
async function replyTicket() { if (!selected.value || !reply.value.trim()) return; await replyServiceTicket(selected.value.id, reply.value); reply.value = ''; messages.value = await getServiceMessages(selected.value.id); selected.value.status = 1; await load(); ElMessage.success('回复已发送') }
async function closeTicket() { if (!selected.value) return; await ElMessageBox.confirm('结案后用户将不能继续回复，确定继续吗？', '确认结案', { type: 'warning' }); await closeServiceTicket(selected.value.id); selected.value.status = 2; await load(); ElMessage.success('工单已结案') }
onMounted(load)
</script>

<template>
  <div class="service-ticket-page"><div class="page-header"><h2>客服工单</h2><p>集中处理用户的订单、物流、售后与账户咨询。</p></div>
    <div class="toolbar"><el-select v-model="status" clearable placeholder="处理状态" @change="current = 1; load()"><el-option label="待处理" :value="0" /><el-option label="处理中" :value="1" /><el-option label="已结案" :value="2" /></el-select><el-button type="primary" @click="load">查询</el-button></div>
    <el-table :data="rows" v-loading="loading" border><el-table-column prop="id" label="工单ID" width="90" /><el-table-column prop="userId" label="用户ID" width="100" /><el-table-column prop="subject" label="标题" min-width="230" show-overflow-tooltip /><el-table-column prop="category" label="类型" width="120" /><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="statusType[row.status]">{{ statusText[row.status] }}</el-tag></template></el-table-column><el-table-column prop="updateTime" label="最近更新" width="180"><template #default="{ row }">{{ row.updateTime?.replace('T', ' ').slice(0, 16) }}</template></el-table-column><el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="open(row)">处理</el-button></template></el-table-column></el-table>
    <el-pagination v-if="total > 15" v-model:current-page="current" background layout="total, prev, pager, next" :page-size="15" :total="total" @current-change="load" />
    <el-dialog v-model="detailVisible" width="720px" :title="selected ? `工单 #${selected.id}` : '工单详情'"><template v-if="selected"><div class="ticket-meta"><strong>{{ selected.subject }}</strong><el-tag :type="statusType[selected.status]">{{ statusText[selected.status] }}</el-tag><span>用户：{{ selected.userId }} · 类型：{{ selected.category }}</span></div><div class="messages"><article v-for="message in messages" :key="message.id" :class="['message', message.senderRole === 'ADMIN' ? 'admin' : 'user']"><span>{{ message.senderRole === 'ADMIN' ? '商户客服' : '用户' }}</span><p>{{ message.content }}</p><time>{{ message.createTime?.replace('T', ' ').slice(0, 16) }}</time></article></div><div v-if="selected.status !== 2" class="reply"><el-input v-model="reply" type="textarea" :rows="3" placeholder="输入回复内容" /><div><el-button type="primary" @click="replyTicket">发送回复</el-button><el-button @click="closeTicket">结案</el-button></div></div></template></el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-header { margin-bottom:16px; } h2 { margin:0; font-size:20px; } .page-header p { color:#909399; font-size:13px; } .toolbar { display:flex; gap:12px; margin-bottom:16px; } .el-pagination { margin-top:16px; justify-content:flex-end; } .ticket-meta { display:flex; flex-direction:column; gap:8px; padding-bottom:12px; border-bottom:1px solid #eee; } .ticket-meta span { color:#909399; font-size:13px; } .messages { height:320px; overflow:auto; padding:16px 0; } .message { max-width:78%; margin-bottom:12px; } .message.admin { margin-left:auto; text-align:right; } .message span,.message time { display:block; color:#999; font-size:12px; } .message p { display:inline-block; margin:5px 0; padding:10px 12px; border-radius:6px; background:#f4f4f5; text-align:left; line-height:1.5; } .message.admin p { background:#ecf5ff; } .reply { display:flex; gap:12px; align-items:flex-end; } .reply .el-input { flex:1; }
</style>

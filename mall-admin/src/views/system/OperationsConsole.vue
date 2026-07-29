<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { blockUser, getOperationsHealth, unblockUser } from '@/api/operations'
const health = ref<Record<string, unknown>>({}); const loading = ref(false); const form = reactive({userId: undefined as number|undefined, reason:'', type:'BLACKLIST'})
async function load(){loading.value=true;try{health.value=await getOperationsHealth()}finally{loading.value=false}}
async function block(){if(!form.userId||!form.reason.trim()){ElMessage.warning('请输入用户ID和限制原因');return}await blockUser(form.userId,{type:form.type,reason:form.reason});ElMessage.success('限制已生效')}
async function unblock(){if(!form.userId){ElMessage.warning('请输入用户ID');return}await unblockUser(form.userId,form.type);ElMessage.success('限制已解除')}
onMounted(load)
</script>
<template><div class="page"><header><h2>运营健康与风控</h2><el-button @click="load">刷新</el-button></header><div class="stats" v-loading="loading"><div><strong>{{health.failedInventoryReleases ?? '-'}}</strong><span>失败库存恢复</span></div><div><strong>{{health.staleSeckillRequests ?? '-'}}</strong><span>超时秒杀请求</span></div><div><strong>{{health.healthy ? '正常' : '需处理'}}</strong><span>运行状态</span></div></div><section><h3>用户风控</h3><el-form inline><el-form-item label="用户ID"><el-input-number v-model="form.userId" :min="1" controls-position="right"/></el-form-item><el-form-item label="限制类型"><el-select v-model="form.type"><el-option label="黑名单" value="BLACKLIST"/><el-option label="交易限制" value="TRADE_BLOCK"/></el-select></el-form-item><el-form-item label="原因"><el-input v-model="form.reason"/></el-form-item><el-button type="danger" @click="block">限制用户</el-button><el-button @click="unblock">解除限制</el-button></el-form></section></div></template>
<style scoped>.page{display:flex;flex-direction:column;gap:16px}header{display:flex;justify-content:space-between;align-items:center}h2{margin:0;font-size:20px}.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.stats div,section{background:#fff;padding:20px;border-radius:6px}.stats strong{display:block;font-size:26px;color:#303133}.stats span{color:#909399;font-size:13px}h3{margin-top:0}</style>

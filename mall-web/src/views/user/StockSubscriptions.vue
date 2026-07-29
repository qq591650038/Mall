<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import { getStockSubscriptions, unsubscribeStock, type StockSubscription } from '@/api/stockSubscription'
const rows=ref<StockSubscription[]>([]);const loading=ref(false)
async function load(){loading.value=true;try{rows.value=await getStockSubscriptions()}finally{loading.value=false}}
async function remove(row:StockSubscription){await ElMessageBox.confirm('取消后商品补货将不再通知，确定继续吗？','取消订阅',{type:'warning'});await unsubscribeStock(row.productId,row.skuId);await load();ElMessage.success('已取消订阅')}
onMounted(load)
</script>
<template><div class="page"><AppHeader/><main><div class="container"><header><div><h1>补货提醒</h1><p>管理已订阅的缺货商品，库存恢复后会发送站内消息。</p></div></header><section v-loading="loading"><el-table :data="rows"><el-table-column label="商品" min-width="280"><template #default="{row}"><div class="product"><el-image v-if="row.productImage" :src="row.productImage" fit="cover"/><span>{{row.productName}}</span></div></template></el-table-column><el-table-column label="规格" min-width="180"><template #default="{row}">{{row.skuSpecInfo || '全部规格'}}</template></el-table-column><el-table-column prop="createTime" label="订阅时间"><template #default="{row}">{{row.createTime?.replace('T',' ').slice(0,16)}}</template></el-table-column><el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="danger" @click="remove(row)">取消订阅</el-button></template></el-table-column></el-table><el-empty v-if="!loading&&!rows.length" description="暂无补货订阅"/></section></div></main><AppFooter/></div></template>
<style scoped>.page{min-height:100vh;background:#f5f5f5}main{padding:24px 0 48px}.container{max-width:1000px;margin:auto;padding:0 20px}header{margin-bottom:16px}h1{margin:0;font-size:22px}p{color:#909399;font-size:13px}section{background:#fff;padding:20px;border-radius:12px}.product{display:flex;align-items:center;gap:10px}.product .el-image{width:42px;height:42px;flex:none;border-radius:4px}</style>

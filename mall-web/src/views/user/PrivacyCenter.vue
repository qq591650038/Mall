<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'
import { closeAccount, exportPersonalData, getPrivacyDocuments } from '@/api/privacy'
import { clearAll } from '@/utils/storage'
const router=useRouter();const documents=ref<Record<string,string>>({})
async function exportData(){const data=await exportPersonalData();const blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download='mall-personal-data.json';link.click();URL.revokeObjectURL(url);ElMessage.success('个人数据已导出')}
async function close(){await ElMessageBox.confirm('注销后将无法登录，且此操作不可撤销。确定注销账户吗？','注销账户',{type:'error',confirmButtonText:'确认注销'});await closeAccount();clearAll();ElMessage.success('账户已注销');router.push('/')}
onMounted(async()=>{documents.value=await getPrivacyDocuments()})
</script>
<template><div class="page"><AppHeader/><main><div class="container"><h1>隐私与账户</h1><section><h3>隐私政策</h3><p>{{documents.privacyPolicy}}</p><h3>用户协议</h3><p>{{documents.userAgreement}}</p></section><section><h3>个人数据</h3><p>可导出账户资料和收货地址。订单、支付与售后记录受财务留存要求管理。</p><el-button type="primary" @click="exportData">导出我的数据</el-button></section><section class="danger"><h3>注销账户</h3><p>注销后会立即退出登录，账户不可恢复。</p><el-button type="danger" @click="close">注销账户</el-button></section></div></main><AppFooter/></div></template>
<style scoped>.page{min-height:100vh;background:#f5f5f5}main{padding:24px 0 48px}.container{max-width:900px;margin:auto;padding:0 20px}h1{font-size:22px}section{background:#fff;border-radius:12px;padding:20px;margin-top:16px}h3{margin-top:0}p{line-height:1.7;color:#606266;font-size:14px}.danger{border-left:4px solid #f56c6c}</style>

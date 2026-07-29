<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getBannerList, createBanner, updateBanner, deleteBanner } from '@/api/banner'
import type { Banner } from '@/types'

const loading = ref(false)
const tableData = ref<Banner[]>([])
const showDialog = ref(false)
const editing = ref<Banner | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 1,
  startTime: '',
  endTime: ''
})

async function loadData() {
  loading.value = true
  try { tableData.value = await getBannerList() } catch { /* handled */ }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { title: '', imageUrl: '', linkUrl: '', sort: 0, status: 1, startTime: '', endTime: '' })
  showDialog.value = true
}

function openEdit(row: Banner) {
  editing.value = row
  Object.assign(form, {
    title: row.title || '',
    imageUrl: row.imageUrl,
    linkUrl: row.linkUrl || '',
    sort: row.sort || 0,
    status: row.status ?? 1,
    startTime: row.startTime || '',
    endTime: row.endTime || ''
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const payload: any = { ...form }
      if (!payload.startTime) delete payload.startTime
      if (!payload.endTime) delete payload.endTime
      if (editing.value) {
        await updateBanner({ ...payload, id: editing.value.id })
        ElMessage.success('更新成功')
      } else {
        await createBanner(payload)
        ElMessage.success('新增成功')
      }
      showDialog.value = false
      loadData()
    } catch { /* handled */ }
  })
}

async function handleDelete(row: Banner) {
  try {
    await ElMessageBox.confirm(`确定删除轮播图「${row.title || row.id}」？`, '删除确认', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled or error */ }
}

function handleStatusChange(row: Banner, val: number) {
  updateBanner({ id: row.id, status: val }).then(() => {
    row.status = val
    ElMessage.success('状态更新成功')
  })
}

onMounted(loadData)
</script>

<template>
  <div class="banner-list-page">
    <div class="page-header">
      <h2>轮播图管理</h2>
      <el-button type="primary" @click="openCreate">+ 新增轮播图</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="imageUrl" label="图片" width="120">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" :preview-src-list="[row.imageUrl]" style="width:80px;height:40px" fit="contain" />
          <span v-else style="color:#999">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="linkUrl" label="跳转链接" min-width="180" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="(val: boolean) => handleStatusChange(row, val ? 1 : 0)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '编辑轮播图' : '新增轮播图'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="轮播图标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="图片URL" required>
          <el-input v-model="form.imageUrl" placeholder="图片URL地址" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="form.linkUrl" placeholder="点击跳转的URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="可选" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="可选" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.banner-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; } }
</style>
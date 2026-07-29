<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getBrandPage, createBrand, updateBrand, deleteBrand } from '@/api/brand'
import type { Brand } from '@/types'

const loading = ref(false)
const tableData = ref<Brand[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const showDialog = ref(false)
const editing = ref<Brand | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  logo: '',
  description: '',
  sort: 0,
  status: 1
})

async function loadData() {
  loading.value = true
  try {
    const res = await getBrandPage({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    tableData.value = res.list
    total.value = res.total
  } catch { /* handled */ }
  finally { loading.value = false }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function resetSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  loadData()
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', logo: '', description: '', sort: 0, status: 1 })
  showDialog.value = true
}

function openEdit(row: Brand) {
  editing.value = row
  Object.assign(form, {
    name: row.name,
    logo: row.logo || '',
    description: row.description || '',
    sort: row.sort || 0,
    status: row.status ?? 1
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (editing.value) {
        await updateBrand({ ...form, id: editing.value.id })
        ElMessage.success('更新成功')
      } else {
        await createBrand({ ...form })
        ElMessage.success('新增成功')
      }
      showDialog.value = false
      loadData()
    } catch { /* handled */ }
  })
}

async function handleDelete(row: Brand) {
  try {
    await ElMessageBox.confirm(`确定删除品牌「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteBrand(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled or error */ }
}

function handleStatusChange(row: Brand, val: number) {
  updateBrand({ id: row.id, status: val }).then(() => {
    row.status = val
    ElMessage.success('状态更新成功')
  })
}

onMounted(loadData)
</script>

<template>
  <div class="brand-list-page">
    <div class="page-header">
      <h2>品牌管理</h2>
      <el-button type="primary" @click="openCreate">+ 新增品牌</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索品牌名称"
        :prefix-icon="Search"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="品牌名称" min-width="140" />
      <el-table-column prop="logo" label="Logo" width="100">
        <template #default="{ row }">
          <el-image v-if="row.logo" :src="row.logo" :preview-src-list="[row.logo]" style="width:60px;height:30px" fit="contain" />
          <span v-else style="color:#999">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
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

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="showDialog" :title="editing ? '编辑品牌' : '新增品牌'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="{ required: true }" label-width="100px">
        <el-form-item label="品牌名称" required>
          <el-input v-model="form.name" placeholder="请输入品牌名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="Logo地址">
          <el-input v-model="form.logo" placeholder="Logo图片URL" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="品牌描述" />
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
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.brand-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; } }
.search-bar { display: flex; gap: 8px; }
.pagination-wrap { display: flex; justify-content: flex-end; }
</style>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCategoryList } from '@/api/product'
import type { Category } from '@/types'

const categories = ref<Category[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editingCategory = ref<Category | null>(null)
const form = ref({ name: '', parentId: undefined as number | undefined, sort: 0, icon: '' })

async function loadCategories() {
  loading.value = true
  try { categories.value = await getCategoryList() } catch { /* handled */ }
  finally { loading.value = false }
}

onMounted(loadCategories)

function openAddDialog() {
  editingCategory.value = null
  form.value = { name: '', parentId: undefined, sort: 0, icon: '' }
  showDialog.value = true
}

function openEditDialog(cat: Category) {
  editingCategory.value = cat
  form.value = { name: cat.name, parentId: cat.parentId, sort: cat.sort || 0, icon: cat.icon || '' }
  showDialog.value = true
}

async function handleSubmit() {
  ElMessage.success('保存成功（演示）')
  showDialog.value = false
  loadCategories()
}
</script>

<template>
  <div class="category-list-page">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="openAddDialog">+ 新增分类</el-button>
    </div>
    <el-table :data="categories" v-loading="loading" row-key="id" :default-expand-all="true">
      <el-table-column prop="name" label="分类名称" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="icon" label="图标" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="showDialog" :title="editingCategory ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.category-list-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; h2 { margin: 0; } }
</style>

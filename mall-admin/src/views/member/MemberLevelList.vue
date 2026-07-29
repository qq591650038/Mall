<template>
  <div class="member-level-list">
    <div class="page-header">
      <h2>会员等级管理</h2>
      <el-button type="primary" @click="openCreate">新增等级</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="等级名称" width="140" />
      <el-table-column prop="level" label="等级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="getLevelTagType(row.level)">{{ row.level }} 级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="积分范围" min-width="160" align="center">
        <template #default="{ row }">
          {{ row.minPoints }} - {{ row.maxPoints === 0 ? '∞' : row.maxPoints }}
        </template>
      </el-table-column>
      <el-table-column label="积分倍率" width="120" align="center">
        <template #default="{ row }">
          <el-tag type="warning" effect="light">{{ row.pointsRate }}x</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="折扣率" width="100" align="center">
        <template #default="{ row }">
          {{ row.discountRate < 1 ? (row.discountRate * 10).toFixed(1) + '折' : '无折扣' }}
        </template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="80" align="center">
        <template #default="{ row }">
          <span style="font-size: 20px;">{{ row.icon || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @size-change="loadData"
      @current-change="loadData"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEditing ? '编辑会员等级' : '新增会员等级'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="等级名称" prop="name">
          <el-input v-model="form.name" placeholder="如：黄金会员" />
        </el-form-item>
        <el-form-item label="等级数值" prop="level">
          <el-input-number v-model="form.level" :min="1" :max="10" />
          <span class="hint">数值越大等级越高</span>
        </el-form-item>
        <el-form-item label="最少积分" prop="minPoints">
          <el-input-number v-model="form.minPoints" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="最多积分" prop="maxPoints">
          <el-input-number v-model="form.maxPoints" :min="0" :step="500" />
          <span class="hint">0 表示无上限</span>
        </el-form-item>
        <el-form-item label="积分倍率" prop="pointsRate">
          <el-input-number v-model="form.pointsRate" :min="0.5" :max="10" :step="0.5" :precision="2" />
          <span class="hint">1.0 = 1倍, 1.5 = 1.5倍, 2.0 = 2倍</span>
        </el-form-item>
        <el-form-item label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="1" :step="0.05" :precision="2" />
          <span class="hint">0.9 = 9折, 1.0 = 无折扣</span>
        </el-form-item>
        <el-form-item label="等级图标">
          <el-input v-model="form.icon" placeholder="Emoji 或图标名，如 🥇" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="等级权益说明" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MemberLevel } from '@/types'
import {
  getMemberLevelPage,
  createMemberLevel,
  updateMemberLevel,
  deleteMemberLevel
} from '@/api/memberLevel'

const loading = ref(false)
const tableData = ref<MemberLevel[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })

const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const defaultForm = (): Partial<MemberLevel> => ({
  name: '',
  level: 1,
  minPoints: 0,
  maxPoints: 0,
  pointsRate: 1.0,
  discountRate: 1.0,
  icon: '',
  description: '',
  sort: 0,
  status: 1
})

const form = reactive<Partial<MemberLevel>>(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
  level: [{ required: true, message: '请输入等级数值', trigger: 'blur' }],
  minPoints: [{ required: true, message: '请输入最少积分', trigger: 'blur' }],
  maxPoints: [{ required: true, message: '请输入最多积分', trigger: 'blur' }],
  pointsRate: [{ required: true, message: '请输入积分倍率', trigger: 'blur' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }]
}

function getLevelTagType(level: number) {
  if (level >= 4) return 'danger'
  if (level >= 3) return 'warning'
  if (level >= 2) return 'success'
  return 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMemberLevelPage(pagination.current, pagination.size)
    tableData.value = res?.list || []
    pagination.total = res?.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  isEditing.value = false
  editingId.value = null
  Object.assign(form, defaultForm())
  showDialog.value = true
}

function openEdit(row: MemberLevel) {
  isEditing.value = true
  editingId.value = row.id ?? null
  Object.assign(form, { ...row })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if ((form.maxPoints ?? 0) !== 0 && (form.maxPoints ?? 0) < (form.minPoints ?? 0)) {
    ElMessage.warning('最多积分必须大于或等于最少积分，或填写 0 表示无上限')
    return
  }

  if (isEditing.value && editingId.value) {
    await updateMemberLevel(editingId.value, form)
    ElMessage.success('更新成功')
  } else {
    await createMemberLevel(form)
    ElMessage.success('创建成功')
  }
  showDialog.value = false
  loadData()
}

async function handleDelete(row: MemberLevel) {
  await ElMessageBox.confirm(`确定删除等级"${row.name}"？`, '确认删除', { type: 'warning' })
  if (row.id) {
    await deleteMemberLevel(row.id)
    ElMessage.success('删除成功')
    loadData()
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, deleteRole, getPermissions, getRoles, grantPermission, revokePermission, updateRole, type Permission, type Role } from '@/api/access'

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const selected = ref<Role | null>(null)
const roleFormVisible = ref(false)
const editing = ref<Partial<Role>>({ name: '', code: '', description: '' })

async function load() { [roles.value, permissions.value] = await Promise.all([getRoles(), getPermissions()]); if (!selected.value && roles.value.length) selected.value = roles.value[0] }
function openCreate() { editing.value = { name: '', code: '', description: '' }; roleFormVisible.value = true }
function openEdit(role: Role) { editing.value = { ...role }; roleFormVisible.value = true }
async function save() { if (!editing.value.name || !editing.value.code) return ElMessage.warning('请填写角色名称和编码'); if (editing.value.id) await updateRole(editing.value.id, editing.value); else await createRole(editing.value); roleFormVisible.value = false; await load(); ElMessage.success('保存成功') }
async function remove(role: Role) { await ElMessageBox.confirm(`确认删除角色“${role.name}”？`, '提示'); await deleteRole(role.id); if (selected.value?.id === role.id) selected.value = null; await load(); ElMessage.success('删除成功') }
async function togglePermission(permission: Permission, checked: boolean) { if (!selected.value) return; if (checked) await grantPermission(selected.value.id, permission.id); else await revokePermission(selected.value.id, permission.id); ElMessage.success('权限已更新') }
onMounted(load)
</script>
<template>
  <div class="access-page">
    <div class="page-header"><h2>角色权限</h2><el-button type="primary" @click="openCreate">新增角色</el-button></div>
    <div class="access-grid">
      <div class="section-card"><el-table :data="roles" highlight-current-row @current-change="selected = $event" border><el-table-column prop="name" label="角色名称" /><el-table-column prop="code" label="编码" /><el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table></div>
      <div class="section-card"><h3>{{ selected ? `${selected.name} 的权限` : '请选择角色' }}</h3><el-checkbox-group v-if="selected"><el-checkbox v-for="permission in permissions" :key="permission.id" :label="permission.id" @change="(checked: boolean) => togglePermission(permission, checked)">{{ permission.name }} ({{ permission.code }})</el-checkbox></el-checkbox-group><el-empty v-else description="请选择左侧角色" /></div>
    </div>
    <el-dialog v-model="roleFormVisible" title="角色信息" width="460px"><el-form :model="editing" label-width="90px"><el-form-item label="名称"><el-input v-model="editing.name" /></el-form-item><el-form-item label="编码"><el-input v-model="editing.code" /></el-form-item><el-form-item label="描述"><el-input v-model="editing.description" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="roleFormVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
  </div>
</template>
<style scoped lang="scss">.access-page{padding:20px}.page-header{display:flex;justify-content:space-between;margin-bottom:20px}.access-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px}.section-card{background:#fff;border-radius:8px;padding:20px}.section-card h3{margin:0 0 16px}.el-checkbox{display:block;margin:0 0 14px}@media(max-width:800px){.access-grid{grid-template-columns:1fr}}</style>

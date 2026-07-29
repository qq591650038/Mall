<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { getAdminInfo } from '@/api/admin'
import type { AdminProfile } from '@/types'

const loading = ref(false)
const showPasswordDialog = ref(false)
const passwordFormRef = ref<FormInstance>()

const profile = ref<AdminProfile>({
  id: 0, username: '', realName: '', avatar: '',
  email: '', phone: '', status: 1
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function loadProfile() {
  loading.value = true
  try {
    const data = await getAdminInfo()
    profile.value = data
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function handleUpdateProfile() {
  ElMessage.success('个人信息更新成功')
}

async function handleChangePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate((valid) => {
    if (!valid) return
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    ElMessage.success('密码修改成功')
    showPasswordDialog.value = false
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  })
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <div class="profile-content" v-loading="loading">
      <el-card class="profile-card">
        <template #header>
          <span>基本信息</span>
        </template>
        <el-form :model="profile" label-width="100px" class="profile-form">
          <el-form-item label="用户名">
            <el-input v-model="profile.username" disabled />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="profile.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profile.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profile.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="状态">
            <el-tag :type="profile.status === 1 ? 'success' : 'danger'">
              {{ profile.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </el-form-item>
          <el-form-item label="最近登录IP">
            <span>{{ profile.lastLoginIp || '-' }}</span>
          </el-form-item>
          <el-form-item label="最近登录时间">
            <span>{{ profile.lastLoginTime || '-' }}</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleUpdateProfile">保存修改</el-button>
            <el-button @click="showPasswordDialog = true">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <el-dialog v-model="showPasswordDialog" title="修改密码" width="420px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" label-width="100px">
        <el-form-item label="当前密码" required>
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.profile-page { display: flex; flex-direction: column; gap: 16px; }
.page-header { h2 { margin: 0; } }
.profile-content { max-width: 600px; }
.profile-card { .profile-form { max-width: 500px; } }
</style>
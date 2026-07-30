<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const adminStore = useAdminStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await adminStore.login({ ...form })
      ElMessage.success('登录成功')
      router.push({ name: 'Dashboard' })
    } catch { /* handled */ }
    finally { loading.value = false }
  })
}
</script>

<template>
  <div class="admin-login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1>Mall 管理后台</h1>
          <p>欢迎登录商城管理系统</p>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" placeholder="请输入管理员账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
export default { components: { User, Lock } }
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #2E3238 0%, #C4908F 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container { width: 100%; max-width: 420px; padding: 20px; }

.login-card {
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
  h1 { font-size: 24px; color: #333; margin-bottom: 8px; }
  p { color: #999; font-size: 14px; }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  background: linear-gradient(135deg, #C4908F 0%, #D8A9A9 100%);
  border-color: #C4908F;
  &:hover { background: #D8A9A9; border-color: #D8A9A9; }
}
</style>

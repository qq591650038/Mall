<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getVerifyCode } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)
const loginType = ref<'account' | 'phone'>('account')

const form = reactive({
  account: '',
  password: '',
  phone: '',
  verifyCode: '',
  verifyKey: ''
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  verifyCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function sendCode() {
  if (!form.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  try {
    const res = await getVerifyCode(form.phone)
    form.verifyKey = res.key
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
    ElMessage.success('验证码已发送')
  } catch {
    // handled by interceptor
  }
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const loginData = {
        account: loginType.value === 'phone' ? form.phone : form.account,
        password: form.password,
        loginType: loginType.value === 'phone' ? 2 : 1,
        verifyKey: form.verifyKey || undefined,
        verifyCode: form.verifyCode || undefined
      }
      await userStore.login(loginData)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch {
      // handled by interceptor
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  form.account = route.query.account as string || ''
})
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1>欢迎登录</h1>
          <p>登录 Mall 商城，享受优质购物体验</p>
        </div>
        <el-tabs v-model="loginType" class="login-tabs">
          <el-tab-pane label="账号登录" name="account" />
          <el-tab-pane label="手机登录" name="phone" />
        </el-tabs>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          label-position="top"
          class="login-form"
        >
          <el-form-item v-if="loginType === 'account'" label="账号" prop="account">
            <el-input v-model="form.account" placeholder="请输入用户名/邮箱" clearable />
          </el-form-item>
          <el-form-item v-else label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item v-if="loginType === 'phone'" label="验证码" prop="verifyCode">
            <div class="verify-code">
              <el-input v-model="form.verifyCode" placeholder="请输入验证码" />
              <el-button
                :disabled="countdown > 0"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
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
        <div class="login-footer">
          <router-link to="/register">没有账号？立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8de 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-container {
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 12px 40px rgba(255, 107, 53, 0.12);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    color: #333;
    margin-bottom: 8px;
  }
  p {
    color: #999;
    font-size: 14px;
  }
}

.login-tabs {
  margin-bottom: 24px;
}

.login-form {
  .verify-code {
    display: flex;
    gap: 12px;
    width: 100%;
  }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  background: #ff6b35;
  border-color: #ff6b35;
  &:hover { background: #ff5722; border-color: #ff5722; }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  a {
    color: #ff6b35;
    text-decoration: none;
    font-size: 14px;
  }
}
</style>

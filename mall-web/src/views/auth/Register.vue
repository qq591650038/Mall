<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register as registerApi, getVerifyCode } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const countdown = ref(0)

const form = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  verifyCode: '',
  verifyKey: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
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

async function handleRegister() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await registerApi({
        username: form.username,
        phone: form.phone,
        email: form.email || undefined,
        password: form.password,
        confirmPassword: form.confirmPassword,
        verifyKey: form.verifyKey,
        verifyCode: form.verifyCode
      })
      ElMessage.success('注册成功，请登录')
      router.push({ name: 'Login', query: { account: form.username } })
    } catch {
      // handled by interceptor
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <h1>注册账号</h1>
          <p>加入 Mall 商城，开启购物之旅</p>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          label-position="top"
          class="register-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="3-20位字符" clearable />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
          </el-form-item>
          <el-form-item label="验证码" prop="verifyCode">
            <div class="verify-code">
              <el-input v-model="form.verifyCode" placeholder="请输入验证码" />
              <el-button :disabled="countdown > 0" @click="sendCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="选填" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="6-20位字符" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form>
        <div class="register-footer">
          <router-link to="/login">已有账号？立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(216, 169, 169, 0.12) 0%, #ffe8de 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-container {
  width: 100%;
  max-width: 440px;
  padding: 20px;
}

.register-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 12px 40px rgba(196, 144, 143, 0.12);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;
  h1 { font-size: 24px; color: #333; margin-bottom: 8px; }
  p { color: #999; font-size: 14px; }
}

.register-form {
  .verify-code {
    display: flex;
    gap: 12px;
    width: 100%;
  }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  background: #C4908F;
  border-color: #D8A9A9;
  &:hover { background: #B07878; border-color: #B07878; }
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  a { color: #C4908F; text-decoration: none; font-size: 14px; }
}
</style>

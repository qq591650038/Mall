import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, getUser, setUser, clearAll } from '@/utils/storage'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import type { LoginRequest, UserVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserVO | null>(getUser<UserVO>())

  const isLoggedIn = computed(() => !!token.value)

  async function login(data: LoginRequest) {
    const res = await loginApi(data)
    token.value = res.token
    setToken(res.token)
    if (res.userInfo) {
      userInfo.value = res.userInfo as unknown as UserVO
      setUser(res.userInfo)
    }
    return res
  }

  async function fetchUserInfo() {
    const data = await getUserInfo()
    userInfo.value = data
    setUser(data)
    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore
    }
    clearAll()
    token.value = null
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    fetchUserInfo,
    logout
  }
})

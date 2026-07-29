import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, getAdmin, setAdmin, clearAll } from '@/utils/storage'
import { login as loginApi, logout as logoutApi, getAdminInfo } from '@/api/admin'
import type { AdminLoginRequest, AdminInfoVO } from '@/types'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string | null>(getToken())
  const adminInfo = ref<AdminInfoVO | null>(getAdmin<AdminInfoVO>())

  const isLoggedIn = computed(() => !!token.value)

  async function login(data: AdminLoginRequest) {
    const res = await loginApi(data)
    token.value = res.token
    setToken(res.token)
    if (res.adminInfo) {
      adminInfo.value = res.adminInfo as unknown as AdminInfoVO
      setAdmin(res.adminInfo)
    }
    return res
  }

  async function fetchAdminInfo() {
    const data = await getAdminInfo()
    adminInfo.value = data
    setAdmin(data)
    return data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch { /* ignore */ }
    clearAll()
    token.value = null
    adminInfo.value = null
  }

  return {
    token,
    adminInfo,
    isLoggedIn,
    login,
    fetchAdminInfo,
    logout
  }
})

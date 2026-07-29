import { request } from '@/utils/request'
import type { AdminLoginRequest, AdminLoginVO, AdminInfoVO, DashboardStats } from '@/types'

export function login(data: AdminLoginRequest) {
  return request<AdminLoginVO>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request<void>({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getAdminInfo() {
  return request<AdminInfoVO>({
    url: '/profile/info',
    method: 'get'
  })
}

export function getDashboardStats() {
  return request<DashboardStats>({
    url: '/dashboard/stats',
    method: 'get'
  })
}

import { request } from '@/utils/request'
import type { LoginRequest, RegisterRequest, LoginVO, UserVO } from '@/types'

export function login(data: LoginRequest) {
  return request<LoginVO>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function register(data: RegisterRequest) {
  return request<void>({
    url: '/auth/register',
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

export function getVerifyCode(phone: string) {
  return request<{ key: string; code: string }>({
    url: '/auth/verify-code',
    method: 'get',
    params: { phone }
  })
}

export function getUserInfo() {
  return request<UserVO>({
    url: '/user/info',
    method: 'get'
  })
}

export function updateUser(data: Partial<UserVO>) {
  return request<void>({
    url: '/user/info',
    method: 'put',
    data
  })
}

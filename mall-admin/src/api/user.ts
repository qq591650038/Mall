import {request} from '@/utils/request'
import type {PageResult} from '@/types'
import type {CursorPage} from './order'

export interface AdminUser {
  id: number
  username: string
  nickname?: string
  phone?: string
  email?: string
  status: number
  createTime?: string
  lastLoginTime?: string
  lastLoginIp?: string
}

export function getUserCursorPage(params: { size: number; keyword?: string; status?: number; cursor?: string }) {
    return request<CursorPage<AdminUser>>({url: '/users/cursor', method: 'get', params})
}

export function getUserPage(params: { current: number; size: number; keyword?: string; status?: number }) {
  return request<PageResult<AdminUser>>({ url: '/users/page', method: 'get', params })
}

export function updateUserStatus(id: number, status: number) {
  return request<void>({ url: `/users/${id}/status`, method: 'put', data: { status } })
}

export function getUserById(id: number) {
  return request<AdminUser>({ url: `/users/${id}`, method: 'get' })
}

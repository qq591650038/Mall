import { request } from '@/utils/request'
import type { PageResult } from '@/types'

export interface Notification {
  id: number
  type?: string
  title: string
  content?: string
  businessType?: string
  businessId?: number
  isRead: number
  createTime?: string
  readTime?: string
}

export function getNotificationPage(params: { current: number; size: number; unreadOnly?: boolean }) {
  return request<PageResult<Notification>>({ url: '/notifications/page', method: 'get', params })
}

export function getUnreadNotificationCount() {
  return request<number>({ url: '/notifications/unread-count', method: 'get' })
}

export function markNotificationRead(id: number) {
  return request<void>({ url: `/notifications/${id}/read`, method: 'put' })
}

export function markAllNotificationsRead() {
  return request<void>({ url: '/notifications/read-all', method: 'put' })
}

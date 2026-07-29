import { request } from '@/utils/request'
import type { PageResult } from '@/types'

export interface ServiceTicket {
  id: number
  orderId?: number
  refundId?: number
  subject: string
  category: string
  status: number
  priority: number
  createTime?: string
  updateTime?: string
}

export interface ServiceMessage { id: number; ticketId: number; senderRole: string; content: string; createTime?: string }

export function getServiceTickets(params: { current: number; size: number }) {
  return request<PageResult<ServiceTicket>>({ url: '/service-tickets', method: 'get', params })
}
export function createServiceTicket(data: { ticket: Partial<ServiceTicket>; content: string }) {
  return request<ServiceTicket>({ url: '/service-tickets', method: 'post', data })
}
export function getServiceMessages(id: number) {
  return request<ServiceMessage[]>({ url: `/service-tickets/${id}/messages`, method: 'get' })
}
export function replyServiceTicket(id: number, content: string) {
  return request<void>({ url: `/service-tickets/${id}/messages`, method: 'post', data: { content } })
}

import { request } from '@/utils/request'

export interface ServiceTicket { id: number; userId: number; orderId?: number; refundId?: number; subject: string; category: string; status: number; priority: number; createTime?: string; updateTime?: string }
export interface ServiceMessage { id: number; ticketId: number; senderRole: string; content: string; createTime?: string }
export interface PageResult<T> { total: number; list: T[]; current: number; size: number }

export function getServiceTickets(params: { current: number; size: number; status?: number }) { return request<PageResult<ServiceTicket>>({ url: '/service-tickets', method: 'get', params }) }
export function getServiceMessages(id: number) { return request<ServiceMessage[]>({ url: `/service-tickets/${id}/messages`, method: 'get' }) }
export function replyServiceTicket(id: number, content: string) { return request<void>({ url: `/service-tickets/${id}/messages`, method: 'post', data: { content } }) }
export function closeServiceTicket(id: number) { return request<void>({ url: `/service-tickets/${id}/close`, method: 'post' }) }

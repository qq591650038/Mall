import { request } from '@/utils/request'
export function getOperationsHealth() { return request<Record<string, unknown>>({ url: '/operations/health', method: 'get' }) }
export function blockUser(userId: number, data: { type?: string; reason: string }) { return request<void>({ url: `/risk-controls/${userId}`, method: 'post', data }) }
export function unblockUser(userId: number, type?: string) { return request<void>({ url: `/risk-controls/${userId}`, method: 'delete', params: { type } }) }

import { request } from '@/utils/request'
import type { RefundVO, PageResult } from '@/types'

export function applyRefund(orderId: number, data: { amount: number; reason: string; images?: string; type?: number }) {
  return request<RefundVO>({
    url: '/refunds',
    method: 'post',
    data: { orderId, ...data }
  })
}

export function submitReturnLogistics(id: number, data: { logisticsCompany: string; logisticsNo: string }) {
  return request<void>({ url: `/refunds/${id}/return-logistics`, method: 'post', data })
}

export function confirmReturnReceived(id: number) {
  return request<void>({ url: `/refunds/${id}/confirm`, method: 'post' })
}

export function getRefundById(id: number) {
  return request<RefundVO>({
    url: `/refunds/${id}`,
    method: 'get'
  })
}

export function getMyRefunds(params: { current: number; size: number; status?: number }) {
  return request<PageResult<RefundVO>>({
    url: '/refunds/page',
    method: 'get',
    params
  })
}
export function cancelRefund(id: number) { return request<void>({ url: `/refunds/${id}`, method: 'delete' }) }

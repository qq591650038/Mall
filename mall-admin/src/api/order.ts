import { request } from '@/utils/request'
import type { OrderVO, PageResult } from '@/types'

export function getOrderPage(params: { current: number; size: number; orderStatus?: number; keyword?: string }) {
  return request<PageResult<OrderVO>>({
    url: '/orders/page',
    method: 'get',
    params
  })
}

export function getOrderById(id: number) {
  return request<OrderVO>({
    url: `/orders/${id}`,
    method: 'get'
  })
}

export function shipOrder(id: number, data: { logisticsCompany: string; logisticsNo: string }) {
  return request<void>({
    url: `/orders/${id}/ship`,
    method: 'put',
    data
  })
}

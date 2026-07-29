import { request } from '@/utils/request'
import type { CreateOrderDTO, OrderVO, PageResult, PayResultVO } from '@/types'

export function createOrder(data: CreateOrderDTO) {
  return request<OrderVO>({
    url: '/orders',
    method: 'post',
    data
  })
}

export function getOrderById(id: number) {
  return request<OrderVO>({
    url: `/orders/${id}`,
    method: 'get'
  })
}

export function getOrderPage(params: { current: number; size: number; orderStatus?: number }) {
  return request<PageResult<OrderVO>>({
    url: '/orders/page',
    method: 'get',
    params
  })
}

export function cancelOrder(id: number) {
  return request<void>({
    url: `/orders/${id}/cancel`,
    method: 'post'
  })
}

export function payOrder(id: number) {
  return request<PayResultVO>({
    url: `/orders/${id}/pay`,
    method: 'post'
  })
}

export function confirmMockPayment(id: number, paymentNo: string) {
  return request<void>({ url: `/orders/${id}/payment/mock-confirm`, method: 'post', params: { paymentNo } })
}

export function confirmReceive(id: number) {
  return request<void>({
    url: `/orders/${id}/confirm-receive`,
    method: 'post'
  })
}

export function getOrderTimeline(id: number) {
  return request<OrderVO>({
    url: `/orders/${id}`,
    method: 'get'
  })
}

export function getLogistics(id: number) {
  return request<OrderVO>({ url: `/logistics/${id}`, method: 'get' })
}

export function shipOrder(id: number, data: { logisticsCompany: string; logisticsNo: string }) {
  return request<void>({
    url: `/admin/orders/${id}/ship`,
    method: 'put',
    data
  })
}

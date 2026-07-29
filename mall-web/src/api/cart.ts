import { request } from '@/utils/request'
import type { CartVO } from '@/types'

export function getCartList() {
  return request<CartVO[]>({
    url: '/cart/list',
    method: 'get'
  })
}

export function addCart(data: { skuId: number; quantity: number }) {
  return request<void>({
    url: '/cart',
    method: 'post',
    data
  })
}

export function updateCartQuantity(id: number, quantity: number) {
  return request<void>({
    url: `/cart/${id}/quantity`,
    method: 'put',
    data: { quantity }
  })
}

export function deleteCart(id: number) {
  return request<void>({
    url: `/cart/${id}`,
    method: 'delete'
  })
}

export function batchDeleteCart(ids: number[]) {
  return request<void>({
    url: '/cart/batch-delete',
    method: 'post',
    data: { ids }
  })
}

export function selectAllCart(selected: boolean) {
  return request<void>({
    url: '/cart/select-all',
    method: 'put',
    data: { selected }
  })
}

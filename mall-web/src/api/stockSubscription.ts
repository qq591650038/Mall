import { request } from '@/utils/request'
export interface StockSubscription {
  id: number
  productId: number
  productName: string
  productImage?: string
  skuId?: number
  skuSpecInfo?: string
  createTime?: string
}

export function getStockSubscriptions() {
  return request<StockSubscription[]>({ url: '/stock-subscriptions', method: 'get' })
}

export function subscribeStock(productId: number, skuId?: number) {
  return request<void>({ url: '/stock-subscriptions', method: 'post', data: { productId, skuId } })
}

export function unsubscribeStock(productId: number, skuId?: number) {
  return request<void>({ url: '/stock-subscriptions', method: 'delete', params: { productId, skuId } })
}

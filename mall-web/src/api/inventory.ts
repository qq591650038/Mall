import { request } from '@/utils/request'

export function getInventoryLogs(params: { current: number; size: number; status?: number; operation?: string }) {
  return request({
    url: '/inventory/page',
    method: 'get',
    params
  })
}

export function adjustStock(skuId: number, productId: number, quantity: number, reason: string) {
  return request<void>({
    url: '/inventory/adjust',
    method: 'post',
    data: { skuId, productId, quantity, reason }
  })
}

export function getLowStockProducts() {
  return request({
    url: '/inventory/low-stock',
    method: 'get'
  })
}
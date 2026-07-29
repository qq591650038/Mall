import { request } from '@/utils/request'
import type { InventoryLog, PageResult } from '@/types'

export interface InventoryWarning { id: number; productId: number; name: string; stock: number; skuInfo?: string }

export function getLowStockProducts() {
  return request<InventoryWarning[]>({
    url: '/inventory/low-stock',
    method: 'get'
  })
}

export function getInventoryLogs(params: { current: number; size: number; operation?: string }) {
  return request<PageResult<InventoryLog>>({
    url: '/inventory/page',
    method: 'get',
    params
  })
}

export function adjustStock(skuId: number, productId: number, quantity: number, reason: string, type: 'sku' | 'product' = 'sku') {
  return request({
    url: '/inventory/adjust',
    method: 'post',
    data: { type, action: quantity >= 0 ? 'in' : 'out', skuId: type === 'sku' ? skuId : undefined, productId, quantity: Math.abs(quantity), reason }
  })
}

export function retryInventoryLog(id: number) {
  return request<void>({ url: `/inventory/${id}/retry`, method: 'post' })
}

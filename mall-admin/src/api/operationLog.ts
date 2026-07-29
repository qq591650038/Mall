import { request } from '@/utils/request'
import type { PageResult } from '@/types'

export interface OperationLog {
  id: number
  adminId?: number
  adminName?: string
  module?: string
  operation?: string
  method?: string
  params?: string
  ip?: string
  status?: number
  costTime?: number
  createTime?: string
  eventType?: string
  userCouponId?: number
  userId?: number
  couponId?: number
  orderId?: number
  remark?: string
}

export function getOperationLogPage(params: { current: number; size: number; status?: number; eventType?: string; userId?: number }) {
  return request<PageResult<OperationLog>>({ url: '/operation-logs/page', method: 'get', params })
}

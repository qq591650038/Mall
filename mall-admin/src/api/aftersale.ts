import { request } from '@/utils/request'
import type { PageResult } from '@/types'
import type { RefundVO } from '@/types'

export function getAfterSalePage(params: {
  current?: number
  size?: number
  type?: number
  status?: number
  orderNo?: string
  afterSaleNo?: string
}) {
  return request<PageResult<RefundVO>>({
    url: '/refunds/page',
    method: 'get',
    params
  })
}

export function getAfterSaleDetail(id: number) {
  return request<RefundVO>({
    url: `/refunds/${id}`,
    method: 'get'
  })
}

export function auditAfterSale(id: number, data: { status: number; remark?: string }) {
  return request<void>({
    url: `/refunds/${id}/review`,
    method: 'put',
    data
  })
}

export function updateLogistics(id: number, data: { logisticsCompany: string; logisticsNo: string }) {
  return request<void>({
    url: `/refunds/${id}/return-logistics`,
    method: 'put',
    data
  })
}

export function updateExchangeLogistics(id: number, data: { trackingNo: string }) {
  return request<void>({
    url: `/refunds/${id}/exchange-logistics`,
    method: 'put',
    data
  })
}
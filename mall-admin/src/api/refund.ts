import {request} from '@/utils/request'
import type {PageResult, RefundVO} from '@/types'
import type {CursorPage} from './order'

export function getRefundPage(params: { current: number; size: number; status?: number; orderNo?: string }) {
  return request<PageResult<RefundVO>>({
    url: '/refunds/page',
    method: 'get',
    params
  })
}

export function getRefundCursorPage(params: { size: number; status?: number; orderNo?: string; cursor?: string }) {
    return request<CursorPage<RefundVO>>({url: '/refunds/cursor', method: 'get', params})
}

export function getRefundById(id: number) {
  return request<RefundVO>({
    url: `/refunds/${id}`,
    method: 'get'
  })
}

export function reviewRefund(id: number, status: number, remark: string) {
  return request({
    url: `/refunds/${id}/review`,
    method: 'put',
    data: { status, remark }
  })
}

export function completeRefund(id: number) {
  return request({
    url: `/refunds/${id}/complete`,
    method: 'put'
  })
}

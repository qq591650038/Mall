import { request } from '@/utils/request'
import type { PageResult, Review } from '@/types'

export function getReviewPage(params: { current: number; size: number; rating?: number; status?: number }) {
  return request<PageResult<Review>>({ url: '/reviews/page', method: 'get', params })
}

export function replyReview(id: number, reply: string) {
  return request<void>({ url: `/reviews/${id}/reply`, method: 'put', data: { reply } })
}

export function updateReviewStatus(id: number, status: number) {
  return request<void>({ url: `/reviews/${id}/status`, method: 'put', data: { status } })
}

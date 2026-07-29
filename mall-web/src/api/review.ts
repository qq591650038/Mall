import { request } from '@/utils/request'
import type { Review, PageResult } from '@/types'

export function getProductReviews(productId: number, params: { current: number; size: number; ratingType?: number; hasImages?: boolean }) {
  return request<PageResult<Review>>({
    url: `/reviews/product/${productId}`,
    method: 'get',
    params
  })
}

export function getProductReviewSummary(productId: number) {
  return request<{ total: number; good: number; average: number; ratingCounts: Record<string, number> }>({
    url: `/reviews/product/${productId}/summary`, method: 'get'
  })
}

export function getMyReviews(params: { current: number; size: number }) {
  return request<PageResult<Review>>({
    url: '/reviews/mine',
    method: 'get',
    params
  })
}

export function addReview(data: { productId: number; orderId: number; rating: number; content: string; images?: string; parentId?: number }) {
  return request<void>({
    url: '/reviews',
    method: 'post',
    data
  })
}

export function replyReview(id: number, reply: string) {
  return request<void>({
    url: `/admin/reviews/${id}/reply`,
    method: 'put',
    data: { reply }
  })
}

export function updateReviewStatus(id: number, status: number) {
  return request<void>({
    url: `/admin/reviews/${id}/status`,
    method: 'put',
    data: { status }
  })
}

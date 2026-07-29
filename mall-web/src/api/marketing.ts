import { request } from '@/utils/request'
import type { MarketingActivity, MarketingActivityItem, MarketingParticipateResult } from '@/types'

// 获取进行中的活动列表
export function getActiveActivities(type?: string) {
  return request<MarketingActivity[]>({
    url: '/marketing/activities/active',
    method: 'get',
    params: type ? { type } : {}
  })
}

// 获取活动详情（含商品明细）
export function getActivityDetail(id: number) {
  return request<MarketingActivity>({
    url: `/marketing/activities/${id}`,
    method: 'get'
  })
}

// 获取活动商品列表
export function getActivityItems(id: number) {
  return request<MarketingActivityItem[]>({
    url: `/marketing/activities/${id}/items`,
    method: 'get'
  })
}

// 参与活动（抢购）
export function participate(activityId: number, itemId: number, productId: number, skuId: number | undefined, quantity: number = 1) {
  return request<MarketingParticipateResult>({
    url: '/marketing/participate',
    method: 'post',
    data: { activityId, itemId, productId, skuId, quantity }
  })
}

export function seckillParticipate(activityId: number, itemId: number, quantity: number = 1) {
  return request<MarketingParticipateResult>({ url: '/marketing/seckill/participate', method: 'post', data: { activityId, itemId, quantity } })
}

export function getSeckillRequest(requestId: string) {
  return request<{ status: number; orderId?: number; errorMessage?: string }>({ url: `/marketing/seckill/requests/${requestId}`, method: 'get' })
}

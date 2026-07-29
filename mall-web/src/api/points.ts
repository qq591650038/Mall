import { request } from '@/utils/request'
import type { PageResult } from '@/types'

export interface PointsSummary {
  balance: number
  totalEarned: number
  totalSpent: number
  memberLevel: string
  memberLevelId: number
  pointsRate: number
  nextLevelPoints: number
  nextLevelName: string
  pointsToNextLevel: number
  checkedInToday: boolean
}

export interface PointsLedger {
  id: number
  amount: number
  balanceAfter: number
  eventType: string
  remark?: string
  createTime?: string
}

export interface PointsProduct {
  id: number
  name: string
  description?: string
  pointsCost: number
  stock: number
  rewardType: string
  rewardRefId?: number
  rewardSkuId?: number
  rewardValue?: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface PointsRedemption {
  id: number
  productId: number
  points: number
  redemptionCode: string
  rewardType?: string
  orderId?: number
  userCouponId?: number
  fulfillmentStatus?: string
  createTime?: string
}

export function getPointsSummary() {
  return request<PointsSummary>({ url: '/points/summary', method: 'get' })
}

export function getPointsLedger(params: { current: number; size: number }) {
  return request<PageResult<PointsLedger>>({ url: '/points/ledger', method: 'get', params })
}

export function checkInPoints() {
  return request<PointsSummary>({ url: '/points/check-in', method: 'post' })
}

export function getPointsProducts() {
  return request<PointsProduct[]>({ url: '/points/products', method: 'get' })
}

export function redeemPoints(productId: number, addressId?: number) {
  return request<PointsRedemption>({ url: `/points/redeem/${productId}`, method: 'post', data: { addressId } })
}

export function getPointsRedemptions(params: { current: number; size: number }) {
  return request<PageResult<PointsRedemption>>({ url: '/points/redemptions', method: 'get', params })
}

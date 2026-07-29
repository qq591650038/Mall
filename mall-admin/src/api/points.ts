import { request } from '@/utils/request'
import type { PageResult } from '@/types'

export interface PointsProduct {
  id?: number
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
  userId: number
  productId: number
  points: number
  redemptionCode: string
  createTime?: string
}

export function getPointsProductPage(params: {
  current: number
  size: number
  status?: number
  keyword?: string
}) {
  return request<PageResult<PointsProduct>>({
    url: '/points-products/page',
    method: 'get',
    params
  })
}

export function getPointsProductById(id: number) {
  return request<PointsProduct>({
    url: `/points-products/${id}`,
    method: 'get'
  })
}

export function createPointsProduct(data: Partial<PointsProduct>) {
  return request<PointsProduct>({
    url: '/points-products',
    method: 'post',
    data
  })
}

export function updatePointsProduct(id: number, data: Partial<PointsProduct>) {
  return request<PointsProduct>({
    url: `/points-products/${id}`,
    method: 'put',
    data
  })
}

export function deletePointsProduct(id: number) {
  return request<void>({
    url: `/points-products/${id}`,
    method: 'delete'
  })
}

export function updatePointsProductStatus(id: number, status: number) {
  return request<void>({
    url: `/points-products/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getPointsRedemptions(params: {
  current: number
  size: number
  productId?: number
}) {
  return request<PageResult<PointsRedemption>>({
    url: '/points-products/redemptions',
    method: 'get',
    params
  })
}

export function initPointsProducts() {
  return request<PointsProduct[]>({
    url: '/points-products/init',
    method: 'post'
  })
}

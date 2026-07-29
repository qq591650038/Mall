import { request } from '@/utils/request'
import type { Coupon, PageResult } from '@/types'

export function getCouponPage(params: { current: number; size: number; keyword?: string }) {
  return request<PageResult<Coupon>>({
    url: '/coupons/page',
    method: 'get',
    params
  })
}

export function getCouponById(id: number) {
  return request<Coupon>({
    url: `/coupons/${id}`,
    method: 'get'
  })
}

export function createCoupon(data: Partial<Coupon>) {
  return request<void>({
    url: '/coupons',
    method: 'post',
    data
  })
}

export function updateCoupon(data: Partial<Coupon>) {
  return request<void>({
    url: '/coupons',
    method: 'put',
    data
  })
}

export function deleteCoupon(id: number) {
  return request<void>({
    url: `/coupons/${id}`,
    method: 'delete'
  })
}
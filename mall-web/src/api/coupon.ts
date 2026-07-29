import { request } from '@/utils/request'
import type { Coupon, UserCoupon } from '@/types'

export function getAvailableCoupons() {
  return request<Coupon[]>({
    url: '/coupons/available',
    method: 'get'
  })
}

export function receiveCoupon(id: number) {
  return request<UserCoupon>({
    url: `/coupons/${id}/receive`,
    method: 'post'
  })
}

export function getMyCoupons() {
  return request<UserCoupon[]>({
    url: '/coupons/mine',
    method: 'get'
  })
}
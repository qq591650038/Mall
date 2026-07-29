import { request } from '@/utils/request'
import type { Banner, Category, Brand, Coupon, UserCoupon, UsableCoupon, Review, Favorite, PageResult } from '@/types'

export function getActiveBanners() {
  return request<Banner[]>({
    url: '/banner/list',
    method: 'get'
  })
}

export function getCategoryList() {
  return request<Category[]>({
    url: '/categories/list',
    method: 'get'
  })
}

export function getSubCategories(parentId: number) {
  return request<Category[]>({
    url: `/categories/parent/${parentId}`,
    method: 'get'
  })
}

export function getBrandList() {
  return request<Brand[]>({
    url: '/brands/list',
    method: 'get'
  })
}

export function getAvailableCoupons() {
  return request<Coupon[]>({
    url: '/coupons/available',
    method: 'get'
  })
}

export function getUsableCoupons() {
  return request<UsableCoupon[]>({
    url: '/coupons/usable',
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

export function getProductReviews(productId: number, params: { current: number; size: number }) {
  return request<PageResult<Review>>({
    url: `/reviews/product/${productId}`,
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

export function getFavorites() {
  return request<Favorite[]>({
    url: '/favorites/list',
    method: 'get'
  })
}

export function getFavoritesByGroup(groupId: number) {
  return request<Favorite[]>({
    url: `/favorites/group/${groupId}`,
    method: 'get'
  })
}

export function getUngroupedFavorites() {
  return request<Favorite[]>({
    url: '/favorites/ungrouped',
    method: 'get'
  })
}

export function toggleFavorite(productId: number) {
  return addFavorite(productId)
}

export function isFavorite(productId: number) {
  return request<boolean>({
    url: `/favorites/check/${productId}`,
    method: 'get'
  })
}

export function addFavorite(productId: number, data?: { groupId?: number; originalPrice?: number }) {
  return request<void>({
    url: `/favorites/product/${productId}`,
    method: 'post',
    data
  })
}

export function removeFavorite(productId: number) {
  return request<void>({ url: `/favorites/product/${productId}`, method: 'delete' })
}

export function updateFavoriteGroup(productId: number, groupId: number | null) {
  return request<void>({
    url: `/favorites/product/${productId}/group`,
    method: 'put',
    data: { groupId }
  })
}

export function updateFavoritePriceAlert(productId: number, enabled: boolean) {
  return request<void>({
    url: `/favorites/product/${productId}/price-alert`,
    method: 'put',
    data: { enabled }
  })
}

export function updateFavoriteStockAlert(productId: number, enabled: boolean) {
  return request<void>({
    url: `/favorites/product/${productId}/stock-alert`,
    method: 'put',
    data: { enabled }
  })
}

export function getFavoriteGroups() {
  return request<any[]>({
    url: '/favorite-groups/list',
    method: 'get'
  })
}

export function createFavoriteGroup(name: string, sort?: number) {
  return request<any>({
    url: '/favorite-groups',
    method: 'post',
    data: { name, sort }
  })
}

export function updateGroup(id: number, name: string, sort?: number) {
  return request<any>({
    url: `/favorite-groups/${id}`,
    method: 'put',
    data: { name, sort }
  })
}

export function deleteFavoriteGroup(id: number) {
  return request<void>({
    url: `/favorite-groups/${id}`,
    method: 'delete'
  })
}

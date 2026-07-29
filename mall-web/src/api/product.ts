import { request } from '@/utils/request'
import type { Product, ProductDetailVO, PageResult } from '@/types'

export interface ProductQuery {
  current: number; size: number; keyword?: string; categoryId?: number; brandId?: number
  minPrice?: number; maxPrice?: number; minRating?: number; inStock?: boolean
  sort?: 'default' | 'sales' | 'priceAsc' | 'priceDesc' | 'rating' | 'newest'
}

export function getProductPage(params: ProductQuery) {
  return request<PageResult<Product>>({
    url: '/products/page',
    method: 'get',
    params
  })
}

export function getSearchSuggestions(keyword: string) {
  return request<string[]>({ url: '/products/suggestions', method: 'get', params: { keyword } })
}

export function getPopularSearches() {
  return request<string[]>({ url: '/products/popular-searches', method: 'get' })
}

export function getRelatedProducts(id: number) {
  return request<Product[]>({ url: `/products/${id}/related`, method: 'get' })
}

export function getRecommendations() {
  return request<Product[]>({ url: '/products/recommendations', method: 'get' })
}

export function getProductDetail(id: number) {
  return request<ProductDetailVO>({
    url: `/products/${id}`,
    method: 'get'
  })
}

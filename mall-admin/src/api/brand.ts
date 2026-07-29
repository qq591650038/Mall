import { request } from '@/utils/request'
import type { Brand, PageResult } from '@/types'

export function getBrandPage(params: { current: number; size: number; keyword?: string }) {
  return request<PageResult<Brand>>({
    url: '/brands/page',
    method: 'get',
    params
  })
}

export function getBrandById(id: number) {
  return request<Brand>({
    url: `/brands/${id}`,
    method: 'get'
  })
}

export function createBrand(data: Partial<Brand>) {
  return request<void>({
    url: '/brands',
    method: 'post',
    data
  })
}

export function updateBrand(data: Partial<Brand>) {
  return request<void>({
    url: '/brands',
    method: 'put',
    data
  })
}

export function deleteBrand(id: number) {
  return request<void>({
    url: `/brands/${id}`,
    method: 'delete'
  })
}
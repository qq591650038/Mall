import { request } from '@/utils/request'
import type { Banner } from '@/types'

export function getBannerList() {
  return request<Banner[]>({
    url: '/banners/list',
    method: 'get'
  })
}

export function createBanner(data: Partial<Banner>) {
  return request<void>({
    url: '/banners',
    method: 'post',
    data
  })
}

export function updateBanner(data: Partial<Banner>) {
  return request<void>({
    url: '/banners',
    method: 'put',
    data
  })
}

export function deleteBanner(id: number) {
  return request<void>({
    url: `/banners/${id}`,
    method: 'delete'
  })
}
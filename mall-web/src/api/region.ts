import { request } from '@/utils/request'
import type { Region } from '@/types'

export function getProvinces() {
  return request<Region[]>({
    url: '/regions/provinces',
    method: 'get'
  })
}

export function getCities(provinceId: number) {
  return request<Region[]>({
    url: `/regions/cities/${provinceId}`,
    method: 'get'
  })
}

export function getDistricts(cityId: number) {
  return request<Region[]>({
    url: `/regions/districts/${cityId}`,
    method: 'get'
  })
}

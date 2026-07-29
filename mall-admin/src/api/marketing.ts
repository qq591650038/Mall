import { request } from '@/utils/request'
import type { MarketingActivity, MarketingActivityCreateRequest, MarketingActivityItem, PageResult } from '@/types'
import type { MarketingGroup } from '@/types'

// 营销 Controller 挂载在 /api/marketing，不属于管理端默认的 /api/admin 前缀。
const marketingRequestBase = '/api'

export function getActivityPage(params: {
  current?: number
  size?: number
  type?: string
  status?: number
}) {
  return request<PageResult<MarketingActivity>>({
    baseURL: marketingRequestBase,
    url: '/marketing/admin/activities',
    method: 'get',
    params
  })
}

export function getActivityDetail(id: number) {
  return request<MarketingActivity>({
    baseURL: marketingRequestBase,
    url: `/marketing/activities/${id}`,
    method: 'get'
  })
}

export function createActivity(data: MarketingActivityCreateRequest) {
  return request<MarketingActivity>({
    baseURL: marketingRequestBase,
    url: '/marketing/admin/activities',
    method: 'post',
    data
  })
}

export function updateActivity(id: number, data: MarketingActivityCreateRequest) {
  return request<MarketingActivity>({
    baseURL: marketingRequestBase,
    url: `/marketing/admin/activities/${id}`,
    method: 'put',
    data
  })
}

export function deleteActivity(id: number) {
  return request<void>({
    baseURL: marketingRequestBase,
    url: `/marketing/admin/activities/${id}`,
    method: 'delete'
  })
}

export function cancelActivity(id: number) {
  return request<void>({
    baseURL: marketingRequestBase,
    url: `/marketing/admin/activities/${id}/cancel`,
    method: 'post'
  })
}

export function getActivityItems(id: number) {
  return request<MarketingActivityItem[]>({
    baseURL: marketingRequestBase,
    url: `/marketing/activities/${id}/items`,
    method: 'get'
  })
}

export function getActivityGroups(id: number) {
  return request<MarketingGroup[]>({ baseURL: marketingRequestBase, url: `/marketing/admin/activities/${id}/groups`, method: 'get' })
}

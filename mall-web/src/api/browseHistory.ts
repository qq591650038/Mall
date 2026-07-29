import { request } from '@/utils/request'
import type { BrowseHistory } from '@/types'

export function getBrowseHistoryList() {
  return request<BrowseHistory[]>({
    url: '/browse-history/list',
    method: 'get'
  })
}

export function addBrowseHistory(productId: number) {
  return request<void>({
    url: `/browse-history/product/${productId}`,
    method: 'post'
  })
}

export function deleteBrowseHistory(productId: number) {
  return request<void>({
    url: `/browse-history/product/${productId}`,
    method: 'delete'
  })
}

export function clearBrowseHistory() {
  return request<void>({
    url: '/browse-history/clear',
    method: 'delete'
  })
}
import { request } from '@/utils/request'
import type { Address } from '@/types'

export function getAddressList() {
  return request<Address[]>({
    url: '/addresses/list',
    method: 'get'
  })
}

export function addAddress(data: Omit<Address, 'id'>) {
  return request<Address>({
    url: '/addresses',
    method: 'post',
    data
  })
}

export function updateAddress(data: Address) {
  return request<void>({
    url: '/addresses',
    method: 'put',
    data
  })
}

export function deleteAddress(id: number) {
  return request<void>({
    url: `/addresses/${id}`,
    method: 'delete'
  })
}

export function setDefaultAddress(id: number) {
  return request<void>({
    url: `/addresses/${id}/default`,
    method: 'put'
  })
}

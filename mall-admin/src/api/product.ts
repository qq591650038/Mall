import { request } from '@/utils/request'
import type { Product, PageResult, Category, Brand } from '@/types'

export function getProductPage(params: { current: number; size: number; keyword?: string; categoryId?: number; status?: number }) {
  return request<PageResult<Product>>({
    url: '/products/page',
    method: 'get',
    params
  })
}

export function getProductById(id: number) {
  return request<Product>({
    url: `/products/${id}`,
    method: 'get'
  })
}

export function createProduct(data: Partial<Product>) {
  return request<Product>({
    url: '/products',
    method: 'post',
    data
  })
}

export function updateProduct(data: Partial<Product>) {
  return request<void>({
    url: '/products',
    method: 'put',
    data
  })
}

export function deleteProduct(id: number) {
  return request<void>({
    url: `/products/${id}`,
    method: 'delete'
  })
}

export function onShelf(id: number) {
  return request<void>({
    url: `/products/${id}/on-shelf`,
    method: 'put'
  })
}

export function offShelf(id: number) {
  return request<void>({
    url: `/products/${id}/off-shelf`,
    method: 'put'
  })
}

export function getCategoryList() {
  return request<Category[]>({
    url: '/categories/list',
    method: 'get'
  })
}

export function getBrandList() {
  return request<Brand[]>({
    url: '/brands/list',
    method: 'get'
  })
}

export function uploadImage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return request<string>({ url: '/api/files/images', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } })
}

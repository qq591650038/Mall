import { request } from '@/utils/request'
import type { ProductDetailVO } from '@/types'

/**
 * 商品对比相关 API
 * 使用 localStorage 存储对比商品ID，最多支持 4 个商品对比
 */

const COMPARE_STORAGE_KEY = 'mall_compare_products'
const MAX_COMPARE_COUNT = 4

/** 获取对比商品列表 */
export function getCompareProductIds(): number[] {
  try {
    const data = localStorage.getItem(COMPARE_STORAGE_KEY)
    return data ? JSON.parse(data) : []
  } catch {
    return []
  }
}

/** 保存对比商品列表 */
function saveCompareProductIds(ids: number[]): void {
  localStorage.setItem(COMPARE_STORAGE_KEY, JSON.stringify(ids))
}

/** 添加商品到对比列表 */
export function addToCompare(productId: number): { success: boolean; message: string } {
  const ids = getCompareProductIds()
  if (ids.includes(productId)) {
    return { success: false, message: '该商品已在对比列表中' }
  }
  if (ids.length >= MAX_COMPARE_COUNT) {
    return { success: false, message: `最多只能对比 ${MAX_COMPARE_COUNT} 个商品` }
  }
  ids.push(productId)
  saveCompareProductIds(ids)
  return { success: true, message: '已添加到对比列表' }
}

/** 从对比列表移除商品 */
export function removeFromCompare(productId: number): void {
  const ids = getCompareProductIds().filter(id => id !== productId)
  saveCompareProductIds(ids)
}

/** 清空对比列表 */
export function clearCompare(): void {
  localStorage.removeItem(COMPARE_STORAGE_KEY)
}

/** 检查商品是否在对比列表中 */
export function isInCompare(productId: number): boolean {
  return getCompareProductIds().includes(productId)
}

/** 获取对比商品数量 */
export function getCompareCount(): number {
  return getCompareProductIds().length
}

/**
 * 批量获取商品详情用于对比
 */
export function getProductsForCompare(ids: number[]) {
  if (ids.length === 0) {
    return Promise.resolve([])
  }
  // 逐个获取商品详情（后端暂无批量接口，使用 Promise.all）
  return Promise.all(ids.map(id =>
    request<ProductDetailVO>({
      url: `/products/${id}`,
      method: 'get'
    }).catch(() => null)  // 单个商品获取失败不影响其他商品
  )) as Promise<(ProductDetailVO | null)[]>
}

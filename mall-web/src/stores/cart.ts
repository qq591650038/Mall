import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCartList, addCart as addCartApi, updateCartQuantity, batchDeleteCart, selectAllCart } from '@/api/cart'
import type { CartVO } from '@/types'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartVO[]>([])
  const loading = ref(false)

  const selectedItems = computed(() => items.value.filter(i => i.selected === 1))
  const totalCount = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const selectedTotal = computed(() =>
    selectedItems.value.reduce((sum, i) => sum + i.price * i.quantity, 0)
  )
  const allSelected = computed(() =>
    items.value.length > 0 && items.value.every(i => i.selected === 1)
  )

  async function fetchCart() {
    loading.value = true
    try {
      items.value = await getCartList()
    } catch {
      // ignore
    } finally {
      loading.value = false
    }
  }

  async function addItem(skuId: number, quantity: number) {
    await addCartApi({ skuId, quantity })
    ElMessage.success('已加入购物车')
    await fetchCart()
  }

  async function updateQuantity(id: number, quantity: number) {
    await updateCartQuantity(id, quantity)
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.quantity = quantity
    }
  }

  async function removeItems(ids: number[]) {
    await batchDeleteCart(ids)
    items.value = items.value.filter(i => !ids.includes(i.id))
    ElMessage.success('删除成功')
  }

  async function toggleSelect(id: number) {
    const item = items.value.find(i => i.id === id)
    if (!item) return
    item.selected = item.selected === 1 ? 0 : 1
  }

  async function toggleSelectAll() {
    const newVal = allSelected.value ? 0 : 1
    await selectAllCart(newVal === 1)
    items.value.forEach(i => i.selected = newVal)
  }

  function clearSelected() {
    const ids = selectedItems.value.map(i => i.id)
    if (ids.length > 0) {
      removeItems(ids)
    }
  }

  return {
    items,
    loading,
    selectedItems,
    totalCount,
    selectedTotal,
    allSelected,
    fetchCart,
    addItem,
    updateQuantity,
    removeItems,
    toggleSelect,
    toggleSelectAll,
    clearSelected
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const globalLoadingCount = ref(0)

  function showLoading() {
    globalLoadingCount.value++
    loading.value = true
  }

  function hideLoading() {
    globalLoadingCount.value--
    if (globalLoadingCount.value <= 0) {
      globalLoadingCount.value = 0
      loading.value = false
    }
  }

  return {
    loading,
    showLoading,
    hideLoading
  }
})

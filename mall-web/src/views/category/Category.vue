<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryList } from '@/api/common'
import type { Category } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const categories = ref<Category[]>([])
const activeCategory = ref<Category | null>(null)

async function loadCategories() {
  try {
    categories.value = await getCategoryList()
    if (categories.value.length) {
      activeCategory.value = categories.value[0]
    }
  } catch { /* handled */ }
}

onMounted(loadCategories)

function selectCategory(cat: Category) {
  activeCategory.value = cat
  router.push({ name: 'ProductList', query: { categoryId: String(cat.id) } })
}
</script>

<template>
  <div class="category-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">全部分类</h1>
        <div class="category-grid">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            @click="selectCategory(cat)"
          >
            <div class="category-icon">{{ cat.icon || '📦' }}</div>
            <span>{{ cat.name }}</span>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.category-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
.page-title { font-size: 20px; margin: 0 0 16px; }

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .category-icon {
    width: 64px;
    height: 64px;
    background: #fff5f0;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    margin: 0 auto 12px;
  }

  span { font-size: 14px; color: #333; }
}
</style>

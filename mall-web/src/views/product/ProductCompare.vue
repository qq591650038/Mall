<script setup lang="ts">import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import AppHeader from '@/layouts/AppHeader.vue';
import AppFooter from '@/layouts/AppFooter.vue';
import { getCompareProductIds, removeFromCompare, clearCompare, getProductsForCompare } from '@/api/compare';
import type { ProductDetailVO } from '@/types';
// 对比商品列表
const products = ref<(ProductDetailVO | null)[]>([]);
const loading = ref(false);
const compareIds = ref<number[]>([]);
// 加载对比商品
async function loadProducts() {
 loading.value = true;
 try {
 compareIds.value = getCompareProductIds();
 if (compareIds.value.length === 0) {
 products.value = [];
 return;
 }
 const result = await getProductsForCompare(compareIds.value);
 products.value = result;
 } catch {
 ElMessage.error('加载对比商品失败');
 } finally {
 loading.value = false;
 }
}
// 移除单个商品
async function handleRemove(productId: number) {
 try {
 await ElMessageBox.confirm('确定要从对比列表中移除该商品吗？', '确认移除', {
 type: 'warning'
 });
 removeFromCompare(productId);
 ElMessage.success('已移除');
 loadProducts();
 }
 catch {
 // 用户取消
 }
}
// 清空对比列表
async function handleClear() {
 try {
 await ElMessageBox.confirm('确定要清空对比列表吗？', '确认清空', {
 type: 'warning'
 });
 clearCompare();
 ElMessage.success('已清空');
 loadProducts();
 }
 catch {
 // 用户取消
 }
}
// 获取商品属性值用于对比
function getProductValue(product: ProductDetailVO | null | undefined, field: string): string {
 if (!product)
 return '-';
 const value = product[field as keyof ProductDetailVO];
 if (value === null || value === undefined || value === '') {
 return '-';
 }
 return String(value);
}
// 获取价格显示
function getPriceDisplay(product: ProductDetailVO | null | undefined): string {
 if (!product || !product.price)
 return '-';
 return `¥${product.price}`;
}
// 获取库存状态
function getStockStatus(product: ProductDetailVO | null | undefined): string {
 if (!product)
 return '-';
 if (product.status !== 1)
 return '已下架';
 const stock = product.totalStock ?? 0;
 if (stock === 0)
 return '缺货';
 if (stock <= 10)
 return `紧张 (${stock})`;
 return `充足 (${stock})`;
}
// 获取商品状态标签
function getStatusTag(product: ProductDetailVO | null | undefined): {
 type: string;
 text: string;
} {
 if (!product)
 return { type: 'info', text: '未知' };
 if (product.status !== 1)
 return { type: 'info', text: '已下架' };
 const stock = product.totalStock ?? 0;
 if (stock === 0)
 return { type: 'danger', text: '缺货' };
 if (stock <= 10)
 return { type: 'warning', text: '库存紧张' };
 return { type: 'success', text: '在售' };
}
onMounted(loadProducts);
</script>

<template>
  <div class="product-compare-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <!-- 页面标题 -->
        <div class="page-header">
          <div>
            <h1>商品对比</h1>
            <p class="subtitle">最多可同时对比 4 件商品</p>
          </div>
          <div class="actions" v-if="products.length > 0">
            <span class="count-tip">已选择 {{ products.length }}/4 件商品</span>
            <el-button type="danger" plain @click="handleClear">清空对比</el-button>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="!loading && products.length === 0" description="暂无对比商品，请从商品列表添加对比">
          <el-button type="primary" @click="$router.push('/products')">去选购</el-button>
        </el-empty>

        <!-- 对比表格 -->
        <div v-else class="compare-container" v-loading="loading">
          <!-- 商品卡片行 -->
          <div class="compare-table">
            <!-- 表头：商品信息 -->
            <div class="compare-header">
              <div class="compare-label">对比项</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="product-column"
              >
                <div v-if="product" class="product-card">
                  <div class="product-image">
                    <img
                      v-if="product.mainImage"
                      :src="product.mainImage"
                      :alt="product.name"
                    />
                    <el-empty v-else description="无图片" :image-size="80" />
                  </div>
                  <h3 class="product-name" :title="product.name">
                    {{ product.name }}
                  </h3>
                  <div class="product-price">
                    <span class="price">{{ getPriceDisplay(product) }}</span>
                  </div>
                  <el-tag
                    :type="getStatusTag(product).type"
                    size="small"
                    class="status-tag"
                  >
                    {{ getStatusTag(product).text }}
                  </el-tag>
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click="handleRemove(compareIds[index])"
                  >
                    移除
                  </el-button>
                </div>
                <div v-else class="product-card missing">
                  <el-empty description="商品不可用" />
                  <el-button
                    type="danger"
                    size="small"
                    text
                    @click="handleRemove(compareIds[index])"
                  >
                    移除
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 对比项：价格 -->
            <div class="compare-row">
              <div class="compare-label">价格</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                <span class="highlight price">{{ getPriceDisplay(product) }}</span>
              </div>
            </div>

            <!-- 对比项：库存 -->
            <div class="compare-row">
              <div class="compare-label">库存状态</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                <span>{{ getStockStatus(product) }}</span>
              </div>
            </div>

            <!-- 对比项：品牌 -->
            <div class="compare-row">
              <div class="compare-label">品牌</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                {{ getProductValue(product, 'brandName') }}
              </div>
            </div>

            <!-- 对比项：分类 -->
            <div class="compare-row">
              <div class="compare-label">分类</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                {{ getProductValue(product, 'categoryName') }}
              </div>
            </div>

            <!-- 对比项：销量 -->
            <div class="compare-row">
              <div class="compare-label">月销量</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                {{ getProductValue(product, 'sales') }}
              </div>
            </div>

            <!-- 对比项：商品描述 -->
            <div class="compare-row">
              <div class="compare-label">商品描述</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell desc-cell"
              >
                <span :title="getProductValue(product, 'description')">
                  {{ getProductValue(product, 'description') }}
                </span>
              </div>
            </div>

            <!-- 对比项：商品状态 -->
            <div class="compare-row">
              <div class="compare-label">商品状态</div>
              <div
                v-for="(product, index) in products"
                :key="index"
                class="compare-cell"
              >
                {{ product?.status === 1 ? '在售' : '已下架' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped lang="scss">
.product-compare-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.main-content {
  padding: 24px 0 48px;
}
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h1 {
  margin: 0;
  color: #333;
  font-size: 20px;
}
.subtitle {
  margin: 6px 0 0;
  color: #999;
  font-size: 13px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.count-tip {
  color: #666;
  font-size: 13px;
}

.compare-container {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  overflow-x: auto;
}

.compare-table {
  display: flex;
  flex-direction: column;
  min-width: 600px;
}

.compare-header {
  display: flex;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 16px;
  margin-bottom: 16px;
}

.compare-label {
  width: 100px;
  flex-shrink: 0;
  font-weight: 600;
  color: #666;
  font-size: 13px;
  padding: 8px 0;
}

.product-column {
  flex: 1;
  min-width: 200px;
  padding: 0 12px;
}

.product-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #eee;
  text-align: center;

  &.missing {
    opacity: 0.6;
  }
}

.product-image {
  width: 100%;
  max-width: 160px;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
  }
}

.product-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price {
  .price {
    color: #C4908F;
    font-size: 18px;
    font-weight: 600;
  }
}

.status-tag {
  margin: 4px 0;
}

.compare-row {
  display: flex;
  border-bottom: 1px solid #f5f5f5;
  padding: 12px 0;

  &:last-child {
    border-bottom: none;
  }
}

.compare-cell {
  flex: 1;
  min-width: 200px;
  padding: 0 12px;
  font-size: 14px;
  color: #333;

  &.desc-cell {
    .desc-text {
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
      color: #666;
      line-height: 1.5;
    }
  }
}

.highlight {
  &.price {
    color: #C4908F;
    font-weight: 600;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .compare-label {
    width: 80px;
    font-size: 12px;
  }

  .product-column,
  .compare-cell {
    min-width: 150px;
  }
}
</style>

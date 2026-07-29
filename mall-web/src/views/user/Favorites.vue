<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFavorites,
  getFavoritesByGroup,
  getUngroupedFavorites,
  removeFavorite,
  updateFavoriteGroup,
  updateFavoritePriceAlert,
  updateFavoriteStockAlert,
  getFavoriteGroups,
  createFavoriteGroup,
  updateGroup as updateGroupApi,
  deleteFavoriteGroup
} from '@/api/common'
import type { Favorite, FavoriteGroup } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const router = useRouter()
const favorites = ref<Favorite[]>([])
const groups = ref<FavoriteGroup[]>([])
const loading = ref(false)
const activeGroupId = ref<number | 'ungrouped' | 'all'>('all')

// 新建分组对话框
const showCreateGroupDialog = ref(false)
const newGroupName = ref('')

// 编辑分组对话框
const showEditGroupDialog = ref(false)
const editingGroupId = ref<number | null>(null)
const editingGroupName = ref('')

// 移动收藏对话框
const showMoveDialog = ref(false)
const movingFavoriteId = ref<number | null>(null)
const targetGroupId = ref<number | null>(null)

async function loadFavorites() {
  loading.value = true
  try {
    if (activeGroupId.value === 'all') {
      favorites.value = await getFavorites()
    } else if (activeGroupId.value === 'ungrouped') {
      favorites.value = await getUngroupedFavorites()
    } else {
      favorites.value = await getFavoritesByGroup(activeGroupId.value as number)
    }
  } catch {
    ElMessage.error('加载收藏失败')
  } finally {
    loading.value = false
  }
}

async function loadGroups() {
  try {
    groups.value = await getFavoriteGroups()
  } catch {
    // 分组接口可能还未配置，忽略错误
  }
}

onMounted(async () => {
  await Promise.all([loadGroups(), loadFavorites()])
})

// 切换分组
function switchGroup(groupId: number | 'ungrouped' | 'all') {
  activeGroupId.value = groupId
  loadFavorites()
}

// 新建分组
async function handleCreateGroup() {
  if (!newGroupName.value.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  try {
    await createFavoriteGroup(newGroupName.value.trim())
    ElMessage.success('创建成功')
    showCreateGroupDialog.value = false
    newGroupName.value = ''
    await loadGroups()
  } catch {
    ElMessage.error('创建分组失败')
  }
}

// 开始编辑分组
function startEditGroup(group: FavoriteGroup) {
  editingGroupId.value = group.id
  editingGroupName.value = group.name
  showEditGroupDialog.value = true
}

// 保存编辑分组
async function handleSaveEditGroup() {
  if (!editingGroupName.value.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  try {
    await updateGroupApi(editingGroupId.value!, editingGroupName.value.trim())
    ElMessage.success('更新成功')
    showEditGroupDialog.value = false
    await loadGroups()
  } catch {
    ElMessage.error('更新分组失败')
  }
}

// 删除分组
async function handleDeleteGroup(group: FavoriteGroup) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分组"${group.name}"吗？分组下的收藏将移至未分组。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteFavoriteGroup(group.id)
    ElMessage.success('删除成功')
    // 如果当前选中的分组被删除，切换到全部
    if (activeGroupId.value === group.id) {
      activeGroupId.value = 'all'
    }
    await loadGroups()
    await loadFavorites()
  } catch {
    // 用户取消
  }
}

// 移动收藏到分组
function startMoveFavorite(fav: Favorite) {
  movingFavoriteId.value = fav.id
  targetGroupId.value = fav.groupId ?? null
  showMoveDialog.value = true
}

async function handleMoveFavorite() {
  if (movingFavoriteId.value === null) return
  try {
    const favorite = favorites.value.find(f => f.id === movingFavoriteId.value)
    if (!favorite) return
    await updateFavoriteGroup(favorite.productId, targetGroupId.value)
    ElMessage.success('移动成功')
    showMoveDialog.value = false
    await loadFavorites()
  } catch {
    ElMessage.error('移动失败')
  }
}

// 切换降价提醒
async function togglePriceAlert(fav: Favorite) {
  const enabled = fav.priceAlert !== 1
  try {
    await updateFavoritePriceAlert(fav.productId, enabled)
    fav.priceAlert = enabled ? 1 : 0
    ElMessage.success(enabled ? '已开启降价提醒' : '已关闭降价提醒')
  } catch {
    ElMessage.error('操作失败')
  }
}

// 切换到货提醒
async function toggleStockAlert(fav: Favorite) {
  const enabled = fav.stockAlert !== 1
  try {
    await updateFavoriteStockAlert(fav.productId, enabled)
    fav.stockAlert = enabled ? 1 : 0
    ElMessage.success(enabled ? '已开启到货提醒' : '已关闭到货提醒')
  } catch {
    ElMessage.error('操作失败')
  }
}

// 取消收藏
async function handleRemove(productId: number) {
  try {
    await ElMessageBox.confirm('确定要取消收藏该商品吗？', '确认取消', { type: 'warning' })
    await removeFavorite(productId)
    favorites.value = favorites.value.filter(f => f.productId !== productId)
    ElMessage.success('已取消收藏')
  } catch {
    // 用户取消
  }
}

// 查看商品详情
function goDetail(id: number) {
  router.push({ name: 'ProductDetail', params: { id } })
}

// 计算分组下的收藏数量
function getGroupCount(groupId: number): number {
  return favorites.value.filter(f => f.groupId === groupId).length
}
</script>

<template>
  <div class="favorites-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1>我的收藏</h1>
          <el-button type="primary" size="small" @click="showCreateGroupDialog = true">
            + 新建分组
          </el-button>
        </div>

        <div v-loading="loading" class="favorites-container">
          <!-- 左侧分组列表 -->
          <aside class="group-sidebar">
            <div class="group-section">
              <div
                class="group-item"
                :class="{ active: activeGroupId === 'all' }"
                @click="switchGroup('all')"
              >
                <span class="group-name">全部收藏</span>
                <span class="group-count">{{ favorites.length }}</span>
              </div>
              <div
                class="group-item"
                :class="{ active: activeGroupId === 'ungrouped' }"
                @click="switchGroup('ungrouped')"
              >
                <span class="group-name">未分组</span>
                <span class="group-count">{{ favorites.filter(f => !f.groupId).length }}</span>
              </div>
            </div>

            <div class="group-section">
              <div v-if="groups.length === 0" class="no-groups">暂无分组</div>
              <div
                v-for="group in groups"
                :key="group.id"
                class="group-item"
                :class="{ active: activeGroupId === group.id }"
                @click="switchGroup(group.id)"
              >
                <span class="group-name">{{ group.name }}</span>
                <div class="group-actions" @click.stop>
                  <span class="group-count">{{ getGroupCount(group.id) }}</span>
                  <el-button link size="small" @click="startEditGroup(group)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button link size="small" type="danger" @click="handleDeleteGroup(group)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </aside>

          <!-- 右侧收藏列表 -->
          <div class="favorites-content">
            <div v-if="favorites.length === 0 && !loading" class="empty-state">
              <p>暂无收藏</p>
              <el-button type="primary" @click="router.push('/')">去逛逛</el-button>
            </div>
            <div v-else class="favorites-grid">
              <div
                v-for="fav in favorites"
                :key="fav.id"
                class="favorite-card"
              >
                <div class="product-image" @click="goDetail(fav.productId)">
                  <img :src="fav.product?.mainImage" :alt="fav.product?.name" />
                  <div class="alert-badges">
                    <el-tooltip content="降价提醒" v-if="fav.priceAlert === 1">
                      <span class="badge price-alert">💰</span>
                    </el-tooltip>
                    <el-tooltip content="到货提醒" v-if="fav.stockAlert === 1">
                      <span class="badge stock-alert">📦</span>
                    </el-tooltip>
                  </div>
                </div>
                <div class="product-info">
                  <h3 @click="goDetail(fav.productId)">{{ fav.product?.name }}</h3>
                  <div class="price-row">
                    <span class="current-price">¥{{ fav.product?.price?.toFixed(2) }}</span>
                    <span v-if="fav.originalPrice && fav.product?.price && fav.product.price < fav.originalPrice" class="original-price">
                      ¥{{ fav.originalPrice.toFixed(2) }}
                    </span>
                  </div>

                  <!-- 提醒设置 -->
                  <div class="alert-settings">
                    <div class="alert-item">
                      <span class="alert-label">降价提醒</span>
                      <el-switch
                        v-model="fav.priceAlert"
                        :active-value="1"
                        :inactive-value="0"
                        @change="togglePriceAlert(fav)"
                        size="small"
                      />
                    </div>
                    <div class="alert-item">
                      <span class="alert-label">到货提醒</span>
                      <el-switch
                        v-model="fav.stockAlert"
                        :active-value="1"
                        :inactive-value="0"
                        @change="toggleStockAlert(fav)"
                        size="small"
                      />
                    </div>
                  </div>

                  <div class="card-actions">
                    <el-button size="small" plain @click="startMoveFavorite(fav)">
                      移动
                    </el-button>
                    <el-button size="small" type="danger" text @click="handleRemove(fav.productId)">
                      取消收藏
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />

    <!-- 新建分组对话框 -->
    <el-dialog v-model="showCreateGroupDialog" title="新建分组" width="400px">
      <el-form @submit.prevent="handleCreateGroup">
        <el-form-item label="分组名称">
          <el-input v-model="newGroupName" placeholder="请输入分组名称" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑分组对话框 -->
    <el-dialog v-model="showEditGroupDialog" title="编辑分组" width="400px">
      <el-form @submit.prevent="handleSaveEditGroup">
        <el-form-item label="分组名称">
          <el-input v-model="editingGroupName" placeholder="请输入分组名称" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditGroupDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEditGroup">保存</el-button>
      </template>
    </el-dialog>

    <!-- 移动收藏对话框 -->
    <el-dialog v-model="showMoveDialog" title="移动到分组" width="400px">
      <el-form @submit.prevent="handleMoveFavorite">
        <el-form-item label="选择分组">
          <el-select v-model="targetGroupId" placeholder="选择分组" clearable>
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="group.name"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMoveDialog = false">取消</el-button>
        <el-button type="primary" @click="handleMoveFavorite">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.favorites-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.main-content {
  padding: 24px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h1 {
    font-size: 20px;
    margin: 0;
  }
}

.favorites-container {
  display: flex;
  gap: 20px;
  min-height: 400px;
}

.group-sidebar {
  width: 200px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  flex-shrink: 0;
  height: fit-content;
  position: sticky;
  top: 80px;
}

.group-section {
  &:not(:last-child) {
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
  }
}

.no-groups {
  color: #999;
  font-size: 13px;
  padding: 8px 0;
}

.group-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f5f5f5;
  }

  &.active {
    background: #fff1eb;
    color: #ff6b35;
  }
}

.group-name {
  font-size: 14px;
}

.group-count {
  font-size: 12px;
  color: #999;
}

.group-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.favorites-content {
  flex: 1;
  min-width: 0;
}

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;

  p {
    margin-bottom: 24px;
  }
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.favorite-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }
}

.product-image {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  background: #f9f9f9;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.alert-badges {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
}

.badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.product-info {
  padding: 12px;
}

h3 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.current-price {
  color: #ff6b35;
  font-size: 18px;
  font-weight: 600;
}

.original-price {
  color: #999;
  font-size: 13px;
  text-decoration: line-through;
}

.alert-settings {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 8px;
}

.alert-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .alert-label {
    font-size: 12px;
    color: #666;
  }
}

.card-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .favorites-container {
    flex-direction: column;
  }

  .group-sidebar {
    width: 100%;
    position: static;
  }

  .favorites-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}
</style>
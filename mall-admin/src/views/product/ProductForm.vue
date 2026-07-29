<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getProductById, createProduct, updateProduct, getCategoryList, getBrandList, uploadImage } from '@/api/product'
import type { Category, Brand } from '@/types'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const productId = computed(() => Number(route.params.id))

const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const brands = ref<Brand[]>([])

const form = reactive({
  name: '',
  subtitle: '',
  mainImage: '',
  price: 0,
  originalPrice: 0,
  totalStock: 0,
  categoryId: undefined as number | undefined,
  brandId: undefined as number | undefined,
  status: 0,
  description: ''
  ,skus: [] as Array<{ skuCode: string; specInfo: string; price: number; stock: number; image?: string; status?: number }>
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

async function loadData() {
  try {
    const [cats, brads] = await Promise.all([
      getCategoryList().catch(() => []),
      getBrandList().catch(() => [])
    ])
    categories.value = cats
    brands.value = brads
  } catch { /* ignore */ }

  if (isEdit.value) {
    try {
      const data = await getProductById(productId.value)
      Object.assign(form, {
        name: data.name,
        subtitle: data.subtitle || '',
        mainImage: data.mainImage || '',
        price: data.price,
        originalPrice: data.originalPrice || 0,
        totalStock: data.totalStock,
        categoryId: data.categoryId,
        brandId: data.brandId,
        status: data.status,
        description: data.description || '',
        skus: (data.skus || []).map(s => ({ skuCode: s.skuCode || '', specInfo: s.specInfo || '', price: s.price || 0, stock: s.stock || 0, image: s.image || '', status: s.status ?? 1 }))
      })
    } catch { /* handled */ }
  }
}

onMounted(loadData)

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateProduct({ ...form, id: productId.value })
        ElMessage.success('更新成功')
      } else {
        await createProduct({ ...form })
        ElMessage.success('创建成功')
      }
      router.push({ name: 'ProductList' })
    } catch { /* handled */ }
  })
}

function goBack() { router.push({ name: 'ProductList' }) }
async function handleImageUpload(options: any) {
  try { form.mainImage = await uploadImage(options.file); ElMessage.success('图片上传成功') } catch { /* handled */ }
}
</script>

<template>
  <div class="product-form-page">
    <div class="page-header">
      <el-page-header @back="goBack" :content="isEdit ? '编辑商品' : '新增商品'" />
    </div>
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="product-form"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="副标题">
            <el-input v-model="form.subtitle" placeholder="请输入副标题" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="品牌">
            <el-select v-model="form.brandId" placeholder="请选择品牌" clearable style="width: 100%">
              <el-option v-for="brand in brands" :key="brand.id" :label="brand.name" :value="brand.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="售价" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="原价">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="库存" prop="totalStock">
            <el-input-number v-model="form.totalStock" :min="0" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="主图">
        <el-upload :http-request="handleImageUpload" :show-file-list="false" accept="image/*">
          <el-button>上传主图</el-button>
        </el-upload>
        <el-input v-model="form.mainImage" placeholder="或输入图片 URL" style="margin-top: 8px" />
        <el-image v-if="form.mainImage" :src="form.mainImage" fit="cover" style="width: 120px;height:120px;margin-top:8px;border-radius:8px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">下架</el-radio>
          <el-radio :value="1">上架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="商品描述">
        <el-input v-model="form.description" type="textarea" :rows="6" placeholder="请输入商品描述" />
      </el-form-item>
      <el-form-item label="SKU 规格">
        <div class="sku-editor">
          <el-button type="primary" plain size="small" @click="form.skus.push({ skuCode: '', specInfo: '', price: form.price, stock: 0, image: '', status: 1 })">新增 SKU</el-button>
          <el-table v-if="form.skus.length" :data="form.skus" border style="margin-top: 12px">
            <el-table-column label="SKU 编码" min-width="140"><template #default="{ row }"><el-input v-model="row.skuCode" placeholder="可选" /></template></el-table-column>
            <el-table-column label="规格" min-width="180"><template #default="{ row }"><el-input v-model="row.specInfo" placeholder="如：红色 / XL" /></template></el-table-column>
            <el-table-column label="价格" width="130"><template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="2" /></template></el-table-column>
            <el-table-column label="库存" width="130"><template #default="{ row }"><el-input-number v-model="row.stock" :min="0" /></template></el-table-column>
            <el-table-column label="操作" width="80"><template #default="{ $index }"><el-button link type="danger" @click="form.skus.splice($index, 1)">删除</el-button></template></el-table-column>
          </el-table>
          <el-empty v-else description="暂无 SKU，商品将使用总库存" :image-size="60" />
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" size="large" @click="handleSubmit">保存</el-button>
        <el-button size="large" @click="goBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.product-form-page { .page-header { margin-bottom: 20px; } }
.product-form { background: #fff; border-radius: 8px; padding: 24px; max-width: 900px; }
</style>

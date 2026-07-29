<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/address'
import { getProvinces, getCities, getDistricts } from '@/api/region'
import type { Address, Region } from '@/types'
import AppHeader from '@/layouts/AppHeader.vue'
import AppFooter from '@/layouts/AppFooter.vue'

const addresses = ref<Address[]>([])
const loading = ref(false)
const showDialog = ref(false)
const formRef = ref<FormInstance>()
const editingAddress = ref<Address | null>(null)

const provinceOptions = ref<Region[]>([])
const cityOptions = ref<Region[]>([])
const districtOptions = ref<Region[]>([])

const emptyForm = (): Omit<Address, 'id'> => ({
  userId: 0,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const form = ref<Omit<Address, 'id'>>(emptyForm())

const rules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请选择省份', trigger: 'change' }],
  city: [{ required: true, message: '请选择城市', trigger: 'change' }],
  district: [{ required: true, message: '请选择区县', trigger: 'change' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

async function loadAddresses() {
  loading.value = true
  try {
    addresses.value = await getAddressList()
  } catch { /* handled */ }
  finally { loading.value = false }
}

async function loadProvinces() {
  try {
    provinceOptions.value = await getProvinces()
  } catch { /* handled */ }
}

async function loadCities(provinceName: string) {
  const province = provinceOptions.value.find(p => p.name === provinceName)
  if (!province) {
    cityOptions.value = []
    return
  }
  try {
    cityOptions.value = await getCities(province.id)
  } catch { /* handled */ }
}

async function loadDistricts(cityName: string) {
  const city = cityOptions.value.find(c => c.name === cityName)
  if (!city) {
    districtOptions.value = []
    return
  }
  try {
    districtOptions.value = await getDistricts(city.id)
  } catch { /* handled */ }
}

onMounted(async () => {
  await loadProvinces()
  loadAddresses()
})

function openAddDialog() {
  editingAddress.value = null
  form.value = emptyForm()
  cityOptions.value = []
  districtOptions.value = []
  showDialog.value = true
}

function openEditDialog(addr: Address) {
  editingAddress.value = addr
  form.value = { ...addr }
  cityOptions.value = []
  districtOptions.value = []
  loadCities(addr.province)
  loadDistricts(addr.city)
  showDialog.value = true
}

function onProvinceChange(val: string) {
  form.value.city = ''
  form.value.district = ''
  districtOptions.value = []
  loadCities(val)
}

function onCityChange(val: string) {
  form.value.district = ''
  districtOptions.value = []
  loadDistricts(val)
}

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (editingAddress.value) {
        await updateAddress({ ...form.value, id: editingAddress.value.id } as Address)
        ElMessage.success('更新成功')
      } else {
        await addAddress(form.value)
        ElMessage.success('添加成功')
      }
      showDialog.value = false
      loadAddresses()
    } catch { /* handled */ }
  })
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该地址吗？', '提示')
  await deleteAddress(id)
  ElMessage.success('删除成功')
  loadAddresses()
}

async function handleSetDefault(id: number) {
  await setDefaultAddress(id)
  ElMessage.success('已设为默认')
  loadAddresses()
}
</script>

<template>
  <div class="address-page">
    <AppHeader />
    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1>收货地址</h1>
          <el-button type="primary" @click="openAddDialog">+ 新增地址</el-button>
        </div>
        <div v-loading="loading">
          <div v-if="addresses.length === 0 && !loading" class="empty-state">
            <p>暂无收货地址</p>
          </div>
          <div v-else class="address-list">
            <div v-for="addr in addresses" :key="addr.id" class="address-item">
              <div class="address-info">
                <div class="address-header">
                  <span class="receiver">{{ addr.receiverName }}</span>
                  <span class="phone">{{ addr.receiverPhone }}</span>
                  <el-tag v-if="addr.isDefault" type="warning" size="small">默认</el-tag>
                </div>
                <p class="address-detail">
                  {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
                </p>
              </div>
              <div class="address-actions">
                <el-button link @click="openEditDialog(addr)">编辑</el-button>
                <el-button link type="danger" @click="addr.id && handleDelete(addr.id)">删除</el-button>
                <el-button
                  v-if="!addr.isDefault"
                  link
                  type="primary"
                  @click="addr.id && handleSetDefault(addr.id)"
                >设为默认</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <AppFooter />
    <el-dialog v-model="showDialog" :title="editingAddress ? '编辑地址' : '新增地址'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-select
            v-model="form.province"
            placeholder="请选择省份"
            style="width: 100%"
            @change="onProvinceChange"
          >
            <el-option
              v-for="p in provinceOptions"
              :key="p.id"
              :label="p.name"
              :value="p.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-select
            v-model="form.city"
            placeholder="请选择城市"
            style="width: 100%"
            :disabled="!form.province"
            @change="onCityChange"
          >
            <el-option
              v-for="c in cityOptions"
              :key="c.id"
              :label="c.name"
              :value="c.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-select
            v-model="form.district"
            placeholder="请选择区县"
            style="width: 100%"
            :disabled="!form.city"
          >
            <el-option
              v-for="d in districtOptions"
              :key="d.id"
              :label="d.name"
              :value="d.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="2" placeholder="请输入详细地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.address-page { background: #f5f5f5; min-height: 100vh; }
.main-content { padding: 24px 0; }
.container { max-width: 1000px; margin: 0 auto; padding: 0 20px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h1 { font-size: 20px; margin: 0; }
}

.empty-state {
  background: #fff;
  border-radius: 12px;
  padding: 60px;
  text-align: center;
  color: #999;
}

.address-list { display: flex; flex-direction: column; gap: 12px; }

.address-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .address-info { flex: 1; }
  .address-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
  .receiver { font-size: 16px; font-weight: 500; }
  .phone { color: #666; }
  .address-detail { color: #999; margin: 0; font-size: 14px; line-height: 1.5; }
  .address-actions { display: flex; gap: 16px; }
}
</style>

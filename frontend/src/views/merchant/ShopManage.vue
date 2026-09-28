<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">商铺管理</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建商铺</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="shops" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商铺" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="shop-cell">
              <el-image :src="row.coverImg || IMG_PLACEHOLDER" fit="cover" class="shop-cover">
                <template #error>
                  <el-image :src="IMG_PLACEHOLDER" fit="cover" class="shop-cover" />
                </template>
              </el-image>
              <div>
                <div class="shop-name">{{ row.name }}</div>
                <div class="shop-addr">{{ row.city }} {{ row.address }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ row.typeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="人均" width="90">
          <template #default="{ row }">
            <span class="price">{{ fmtPrice(row.avgPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="shopStatusType(row.status)">
              {{ shopStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" plain
                       @click="toggleStatus(row, 2)">下架</el-button>
            <el-button v-else-if="row.status === 2" size="small" type="success" plain
                       @click="toggleStatus(row, 1)">上架</el-button>
            <el-tag v-else-if="row.status === 0" size="small" type="info">待管理员审核</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background
                       @current-change="fetchShops" />
      </div>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商铺' : '新建商铺'" width="620px"
               :close-on-click-modal="false">
      <el-alert title="新建/编辑后商铺进入待审核状态，管理员审核通过并上架后用户端可见"
                type="info" :closable="false" show-icon class="tip" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="商铺名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="typeName">
          <el-select v-model="form.typeName" placeholder="选择类型（自动关联类型ID）" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t.name" :label="t.name" :value="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" maxlength="20" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" maxlength="20" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" maxlength="100" />
        </el-form-item>
        <el-form-item label="经纬度">
          <div class="coord-row">
            <el-input v-model.number="form.longitude" placeholder="经度 longitude" />
            <el-input v-model.number="form.latitude" placeholder="纬度 latitude" />
          </div>
        </el-form-item>
        <el-form-item label="人均消费">
          <el-input-number v-model="form.avgPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImg" placeholder="封面图片URL（选填）" />
        </el-form-item>
        <el-form-item label="相册">
          <el-input v-model="form.images" type="textarea" :rows="2"
                    placeholder="图片URL，多个用英文逗号分隔（选填）" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200"
                    show-word-limit placeholder="一句话介绍你的商铺" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { fmtPrice, IMG_PLACEHOLDER } from '@/utils/format'

const shops = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

// 常用商铺类型（typeId 与管理端类型表对应；typeName 冗余存储便于展示）
const typeOptions = [
  { id: 1, name: '美食' },
  { id: 2, name: '休闲娱乐' },
  { id: 3, name: '丽人' },
  { id: 4, name: '酒店' },
  { id: 5, name: '运动健身' },
  { id: 6, name: '购物' }
]

const emptyForm = () => ({
  name: '', typeId: null, typeName: '',
  province: '', city: '', address: '',
  longitude: null, latitude: null, avgPrice: 0,
  coverImg: '', images: '', description: ''
})
const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入商铺名称', trigger: 'blur' }],
  typeName: [{ required: true, message: '请选择商铺类型', trigger: 'change' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

function shopStatusText(s) {
  return { 0: '待审核', 1: '营业中', 2: '已下架' }[s] || '未知'
}
function shopStatusType(s) {
  return { 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info'
}

async function fetchShops() {
  loading.value = true
  try {
    const page = await request.get('/merchant/shops', {
      params: { pageNum: pageNum.value, pageSize }
    })
    shops.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, typeId: row.typeId, typeName: row.typeName,
    province: row.province, city: row.city, address: row.address,
    longitude: row.longitude, latitude: row.latitude, avgPrice: row.avgPrice ?? 0,
    coverImg: row.coverImg || '', images: row.images || '', description: row.description || ''
  })
  dialogVisible.value = true
}

function submit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    // typeName → typeId 映射（后端两个字段都存）
    const type = typeOptions.find((t) => t.name === form.typeName)
    const payload = { ...form, typeId: type?.id ?? form.typeId }
    submitting.value = true
    try {
      if (editingId.value) {
        await request.put(`/merchant/shops/${editingId.value}`, payload)
        ElMessage.success('已保存，等待管理员复核')
      } else {
        await request.post('/merchant/shops', payload)
        ElMessage.success('已提交，等待管理员审核')
      }
      dialogVisible.value = false
      fetchShops()
    } catch (e) {
      /* 错误提示由 request.js 统一弹出 */
    } finally {
      submitting.value = false
    }
  })
}

async function toggleStatus(row, status) {
  const action = status === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定${action}商铺「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await request.put(`/merchant/shops/${row.id}/status`, null, { params: { status } })
    ElMessage.success(`${action}成功`)
    fetchShops()
  } catch (e) {
    /* 待审核商铺直接上架会被后端拦截并提示 */
  }
}

onMounted(fetchShops)
</script>

<style scoped>
.page-manage {
  padding: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-weight: 600;
}
.shop-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.shop-cover {
  width: 56px;
  height: 40px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f5f7fa;
}
.shop-name {
  font-weight: 600;
  font-size: 13px;
}
.shop-addr {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
.tip {
  margin-bottom: 14px;
}
.coord-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>

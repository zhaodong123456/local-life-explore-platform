<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="page-title">优惠券管理</span>
            <el-select v-model="filterShopId" placeholder="全部商铺" clearable style="width: 200px"
                       @change="search">
              <el-option v-for="s in myShops" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </div>
          <el-button type="primary" :icon="Plus" @click="openCreate">发布优惠券</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="vouchers" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="shopName" label="所属商铺" min-width="130" show-overflow-tooltip />
        <el-table-column prop="title" label="券标题" min-width="140" show-overflow-tooltip />
        <el-table-column label="面额" width="110">
          <template #default="{ row }">
            <span class="price">¥{{ row.payValue }}</span>
            <span class="condition">满{{ row.actualValue }}可用</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'danger' : 'info'" effect="plain">
              {{ row.type === 1 ? '秒杀券' : '普通券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="80">
          <template #default="{ row }">
            {{ row.type === 1 ? (row.stock ?? '-') : '不限' }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="180">
          <template #default="{ row }">
            <div class="time-cell">{{ fmtTime(row.beginTime) }}</div>
            <div class="time-cell">至 {{ fmtTime(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '已上架' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" plain
                       @click="toggleStatus(row, 0)">下架</el-button>
            <el-button v-else size="small" type="success" plain
                       @click="toggleStatus(row, 1)">上架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background
                       @current-change="fetchVouchers" />
      </div>
    </el-card>

    <!-- 发布/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '发布优惠券'" width="560px"
               :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="所属商铺" prop="shopId">
          <el-select v-model="form.shopId" placeholder="选择名下商铺" style="width: 100%">
            <el-option v-for="s in myShops" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="券类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">普通券</el-radio>
            <el-radio :value="1">秒杀券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="券标题" prop="title">
          <el-input v-model="form.title" maxlength="30" show-word-limit placeholder="如：国庆8折优惠" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subTitle" maxlength="50" placeholder="补充说明（选填）" />
        </el-form-item>
        <el-form-item label="使用规则">
          <el-input v-model="form.rules" type="textarea" :rows="2" maxlength="200"
                    placeholder="使用规则说明（选填）" />
        </el-form-item>
        <el-form-item label="售价" prop="payValue">
          <el-input-number v-model="form.payValue" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="门槛金额" prop="actualValue">
          <el-input-number v-model="form.actualValue" :min="0" :precision="2" :step="50" />
          <span class="form-hint">满该金额可用</span>
        </el-form-item>
        <el-form-item label="有效期" prop="beginTime">
          <el-date-picker v-model="dateRange" type="datetimerange" start-placeholder="生效时间"
                          end-placeholder="失效时间" value-format="YYYY-MM-DDTHH:mm:ss"
                          style="width: 100%" />
        </el-form-item>
        <template v-if="form.type === 1">
          <el-form-item label="秒杀库存" prop="stock">
            <el-input-number v-model="form.stock" :min="1" :step="50" />
            <span class="form-hint">仅秒杀券需要，上架时预热到 Redis</span>
          </el-form-item>
          <el-form-item label="秒杀时段" prop="seckillBegin">
            <el-date-picker v-model="seckillRange" type="datetimerange" start-placeholder="开抢时间"
                            end-placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss"
                            style="width: 100%" />
          </el-form-item>
        </template>
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
import { fmtTime } from '@/utils/format'

const vouchers = ref([])
const myShops = ref([])
const filterShopId = ref(null)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)
const dateRange = ref([])
const seckillRange = ref([])

const emptyForm = () => ({
  shopId: null, type: 0, title: '', subTitle: '', rules: '',
  payValue: null, actualValue: 0,
  stock: 100
})
const form = reactive(emptyForm())

const rules = {
  shopId: [{ required: true, message: '请选择商铺', trigger: 'change' }],
  type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入券标题', trigger: 'blur' }],
  payValue: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  actualValue: [{ required: true, message: '请输入门槛金额', trigger: 'blur' }],
  beginTime: [{ required: true, message: '请选择有效期', trigger: 'change' }],
  stock: [{ required: true, message: '请输入秒杀库存', trigger: 'blur' }]
}

async function fetchMyShops() {
  try {
    const page = await request.get('/merchant/shops', { params: { pageNum: 1, pageSize: 100 } })
    myShops.value = page?.records || []
  } catch (e) {
    /* 静默 */
  }
}

async function fetchVouchers() {
  loading.value = true
  try {
    const page = await request.get('/merchant/vouchers', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        shopId: filterShopId.value || undefined
      }
    })
    vouchers.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  fetchVouchers()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dateRange.value = []
  seckillRange.value = []
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    shopId: row.shopId, type: row.type, title: row.title,
    subTitle: row.subTitle || '', rules: row.rules || '',
    payValue: Number(row.payValue), actualValue: Number(row.actualValue ?? 0),
    stock: row.stock ?? 100
  })
  dateRange.value = [row.beginTime, row.endTime]
  seckillRange.value = []
  dialogVisible.value = true
}

function submit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    if (!dateRange.value || dateRange.value.length !== 2) {
      ElMessage.warning('请选择优惠券有效期')
      return
    }
    if (form.type === 1 && (!seckillRange.value || seckillRange.value.length !== 2)) {
      ElMessage.warning('秒杀券请选择秒杀时段')
      return
    }
    const payload = {
      ...form,
      beginTime: dateRange.value[0],
      endTime: dateRange.value[1],
      seckillBegin: form.type === 1 ? seckillRange.value[0] : undefined,
      seckillEnd: form.type === 1 ? seckillRange.value[1] : undefined
    }
    submitting.value = true
    try {
      if (editingId.value) {
        await request.put(`/merchant/vouchers/${editingId.value}`, payload)
        ElMessage.success('保存成功')
      } else {
        await request.post('/merchant/vouchers', payload)
        ElMessage.success('发布成功（新券默认下架，请手动上架）')
      }
      dialogVisible.value = false
      fetchVouchers()
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
    await ElMessageBox.confirm(
      status === 1 && row.type === 1
        ? `确定上架秒杀券「${row.title}」吗？库存将预热到 Redis 供秒杀扣减。`
        : `确定${action}优惠券「${row.title}」吗？`,
      '提示', { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await request.put(`/merchant/vouchers/${row.id}/status`, null, { params: { status } })
    ElMessage.success(`${action}成功`)
    fetchVouchers()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  }
}

onMounted(() => {
  fetchMyShops()
  fetchVouchers()
})
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
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-weight: 600;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.condition {
  display: block;
  color: #909399;
  font-size: 12px;
}
.time-cell {
  color: #606266;
  font-size: 12px;
  line-height: 18px;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
.form-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

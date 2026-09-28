<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">商铺审核</span>
          <el-radio-group v-model="statusFilter" @change="search">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">营业中</el-radio-button>
            <el-radio-button :value="2">已下架</el-radio-button>
            <el-radio-button :value="-1">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="shops" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商铺" min-width="200">
          <template #default="{ row }">
            <div class="shop-cell">
              <el-image :src="row.coverImg || IMG_PLACEHOLDER" fit="cover" class="shop-cover">
                <template #error>
                  <el-image :src="IMG_PLACEHOLDER" fit="cover" class="shop-cover" />
                </template>
              </el-image>
              <div>
                <div class="shop-name">{{ row.name }}</div>
                <div class="shop-addr">{{ row.province }} {{ row.city }} {{ row.address }}</div>
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
          <template #default="{ row }">{{ fmtPrice(row.avgPrice) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" plain
                       @click="audit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" plain
                       @click="audit(row, false)">驳回</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" plain
                       @click="forceOffline(row)">强制下架</el-button>
            <span v-if="row.status === 2" class="muted">已下架</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background @current-change="fetchList" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { fmtPrice, IMG_PLACEHOLDER } from '@/utils/format'

const shops = ref([])
const statusFilter = ref(0)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

function statusText(s) {
  return { 0: '待审核', 1: '营业中', 2: '已下架' }[s] || '未知'
}
function statusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const page = await request.get('/admin/shops', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        status: statusFilter.value === -1 ? undefined : statusFilter.value
      }
    })
    shops.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  fetchList()
}

async function doAudit(row, approved, forceOffline, tip) {
  let remark = ''
  if (!approved && !forceOffline) {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', `驳回商铺「${row.name}」`, {
        confirmButtonText: '确认驳回', cancelButtonText: '取消',
        inputPlaceholder: '驳回原因（必填）',
        inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因')
      })
      remark = value.trim()
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(`确认${tip}「${row.name}」吗？`, '审核确认', { type: 'warning' })
    } catch {
      return
    }
  }
  try {
    await request.put(`/admin/shops/${row.id}/audit`, null, {
      params: {
        approved: forceOffline ? undefined : approved,
        forceOffline: forceOffline || undefined,
        remark: remark || undefined
      }
    })
    ElMessage.success(`${tip}成功`)
    fetchList()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  }
}

function audit(row, approved) {
  doAudit(row, approved, false, approved ? '通过' : '驳回')
}
function forceOffline(row) {
  doAudit(row, undefined, true, '强制下架')
}

onMounted(fetchList)
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
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
}
</style>

<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">商家入驻审核</span>
          <el-radio-group v-model="statusFilter" @change="search">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已驳回</el-radio-button>
            <el-radio-button :value="-1">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="merchants" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="merchantName" label="商家名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="licenseNo" label="执照号" width="170" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="审核备注" min-width="130" show-overflow-tooltip />
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 || row.status === 2" size="small" type="success" plain
                       @click="audit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0 || row.status === 2" size="small" type="danger" plain
                       @click="audit(row, false)">驳回</el-button>
            <span v-if="row.status === 1" class="muted">已入驻</span>
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
import { fmtTime } from '@/utils/format'

const merchants = ref([])
const statusFilter = ref(0)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

function statusText(s) {
  return { 0: '待审核', 1: '已通过', 2: '已驳回' }[s] || '未知'
}
function statusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const page = await request.get('/admin/merchants', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        status: statusFilter.value === -1 ? undefined : statusFilter.value
      }
    })
    merchants.value = page?.records || []
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

/** 审核入驻：通过自动授予 merchant 角色（后端幂等处理） */
async function audit(row, approved) {
  let remark = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', `驳回「${row.merchantName}」`, {
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
      await ElMessageBox.confirm(
        `确认通过「${row.merchantName}」的入驻申请吗？通过后将自动授予商家角色。`,
        '审核确认', { type: 'success', confirmButtonText: '确认通过' })
    } catch {
      return
    }
  }
  try {
    await request.put(`/admin/merchants/${row.id}/audit`, { approved, remark: remark || undefined })
    ElMessage.success(approved ? '已通过并授予商家角色' : '已驳回')
    fetchList()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  }
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

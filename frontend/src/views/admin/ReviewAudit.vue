<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">点评审核</span>
          <el-radio-group v-model="statusFilter" @change="search">
            <el-radio-button :value="0">待审核</el-radio-button>
            <el-radio-button :value="1">已通过</el-radio-button>
            <el-radio-button :value="2">已驳回</el-radio-button>
            <el-radio-button :value="-1">全部</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="reviews" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" width="130">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28">{{ (row.nickname || '匿').slice(0, 1) }}</el-avatar>
              <span>{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商铺" width="90">
          <template #default="{ row }">#{{ row.shopId }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="评分" width="130">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="success" plain
                       @click="audit(row, true)">通过</el-button>
            <el-button v-if="row.status === 0" size="small" type="danger" plain
                       @click="audit(row, false)">驳回</el-button>
            <span v-else class="muted">已处理</span>
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

const reviews = ref([])
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
    const page = await request.get('/admin/reviews', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        status: statusFilter.value === -1 ? undefined : statusFilter.value
      }
    })
    reviews.value = page?.records || []
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

async function audit(row, approved) {
  let remark = ''
  if (!approved) {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因（将通知用户并可修改后重新提交）',
        `驳回点评「${row.title}」`, {
          confirmButtonText: '确认驳回',
          cancelButtonText: '取消',
          inputPlaceholder: '驳回原因（必填）',
          inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因')
        })
      remark = value.trim()
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(`确认通过点评「${row.title}」吗？通过后将公开展示。`, '审核确认',
        { type: 'success', confirmButtonText: '确认通过' })
    } catch {
      return
    }
  }
  try {
    await request.put(`/admin/reviews/${row.id}/audit`, { approved, remark: remark || undefined })
    ElMessage.success(approved ? '已通过并通知用户' : '已驳回并通知用户')
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
.user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
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

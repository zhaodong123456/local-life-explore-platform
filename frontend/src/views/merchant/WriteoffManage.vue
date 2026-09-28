<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">核销记录</span>
          <el-radio-group v-model="statusFilter" @change="search">
            <el-radio-button :value="-1">全部</el-radio-button>
            <el-radio-button :value="2">待核销</el-radio-button>
            <el-radio-button :value="3">已核销</el-radio-button>
            <el-radio-button :value="1">未支付</el-radio-button>
            <el-radio-button :value="4">已退款</el-radio-button>
            <el-radio-button :value="5">已关闭</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="id" label="订单号" width="190" show-overflow-tooltip />
        <el-table-column prop="voucherTitle" label="优惠券" min-width="150" show-overflow-tooltip />
        <el-table-column prop="shopName" label="商铺" min-width="120" show-overflow-tooltip />
        <el-table-column label="金额" width="90">
          <template #default="{ row }">
            <span class="price">¥{{ row.payValue }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderType === 1 ? 'danger' : 'info'" effect="plain">
              {{ row.orderType === 1 ? '秒杀' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="支付时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.payTime) }}</template>
        </el-table-column>
        <el-table-column label="核销时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.useTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 2" size="small" type="primary"
                       :loading="writingId === row.id" @click="writeOff(row)">核销</el-button>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background
                       @current-change="fetchOrders" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { fmtTime } from '@/utils/format'

const orders = ref([])
const statusFilter = ref(-1)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const writingId = ref(null)

function statusText(s) {
  return { 1: '未支付', 2: '待核销', 3: '已核销', 4: '已退款', 5: '已关闭' }[s] || '未知'
}
function statusType(s) {
  return { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'info' }[s] || 'info'
}

async function fetchOrders() {
  loading.value = true
  try {
    const page = await request.get('/merchant/voucher-orders', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        status: statusFilter.value === -1 ? undefined : statusFilter.value
      }
    })
    orders.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  fetchOrders()
}

/** 核销：仅"已支付待核销"订单（后端二次校验归属与状态） */
async function writeOff(order) {
  try {
    await ElMessageBox.confirm(
      `确认核销订单 ${order.id}（${order.voucherTitle}）吗？核销后不可撤销。`,
      '核销确认', { type: 'warning', confirmButtonText: '确认核销' }
    )
  } catch {
    return
  }
  writingId.value = order.id
  try {
    await request.put(`/merchant/voucher-orders/${order.id}/writeoff`)
    ElMessage.success('核销成功')
    fetchOrders()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    writingId.value = null
  }
}

onMounted(fetchOrders)
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
.price {
  color: #f56c6c;
  font-weight: 600;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
.muted {
  color: #c0c4cc;
}
</style>

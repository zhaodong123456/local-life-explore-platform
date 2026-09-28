<template>
  <div class="profile">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- ============ 基本信息 ============ -->
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="infoForm" label-width="72px" class="info-form" v-loading="infoLoading">
            <el-form-item label="头像">
              <div class="avatar-row">
                <el-avatar :size="56" :src="infoForm.avatar || undefined">
                  {{ (infoForm.nickname || '用').slice(0, 1) }}
                </el-avatar>
                <el-input v-model="infoForm.avatar" placeholder="头像图片URL（选填）" class="avatar-input" />
              </div>
            </el-form-item>
            <el-form-item label="昵称" required>
              <el-input v-model="infoForm.nickname" maxlength="20" show-word-limit class="narrow" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input :model-value="myInfo.phone" disabled class="narrow" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="infoForm.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="0">女</el-radio>
                <el-radio :value="2">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="角色">
              <el-tag v-for="r in myInfo.roles || []" :key="r" type="success" effect="plain" class="role-tag">
                {{ roleText(r) }}
              </el-tag>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingInfo" @click="saveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ============ 我的点评 ============ -->
        <el-tab-pane label="我的点评" name="reviews">
          <el-empty v-if="!myReviewLoading && myReviews.length === 0" description="还没有发布过点评"
                    :image-size="80" />
          <div v-loading="myReviewLoading">
            <div v-for="r in myReviews" :key="r.id" class="item review-item">
              <div class="item-main">
                <div class="item-head">
                  <el-tag size="small" type="warning" effect="plain">商铺 #{{ r.shopId }}</el-tag>
                  <el-rate :model-value="r.score" disabled size="small" />
                  <el-tag size="small" :type="reviewStatusType(r.status)">
                    {{ reviewStatusText(r.status) }}
                  </el-tag>
                  <span class="item-time">{{ fmtTime(r.createTime) }}</span>
                </div>
                <div class="review-title">{{ r.title }}</div>
                <div class="review-content">{{ r.content }}</div>
                <el-alert v-if="r.status === 2 && r.auditRemark" :title="`驳回原因：${r.auditRemark}`"
                          type="error" :closable="false" class="reject-tip" />
              </div>
            </div>
          </div>
          <div class="pager">
            <el-pagination v-model:current-page="myReviewPage" :page-size="myReviewPageSize"
                           :total="myReviewTotal" layout="prev, pager, next" background
                           @current-change="fetchMyReviews" />
          </div>
        </el-tab-pane>

        <!-- ============ 我的收藏 ============ -->
        <el-tab-pane label="我的收藏" name="favorites">
          <el-empty v-if="!favLoading && favorites.length === 0" description="还没有收藏商铺"
                    :image-size="80" />
          <el-row v-loading="favLoading" :gutter="16">
            <el-col v-for="f in favorites" :key="f.shopId" :xs="24" :sm="12" :md="8">
              <div class="fav-card" @click="goShop(f.shopId)">
                <el-image :src="f.coverImg || IMG_PLACEHOLDER" fit="cover" class="fav-cover">
                  <template #error>
                    <el-image :src="IMG_PLACEHOLDER" fit="cover" class="fav-cover" />
                  </template>
                </el-image>
                <div class="fav-info">
                  <div class="fav-name">
                    {{ f.name }}
                    <el-tag v-if="f.status === 2" size="small" type="info">已下架</el-tag>
                  </div>
                  <div class="fav-meta">
                    <span>{{ f.typeName }}</span>
                    <span class="price">人均 {{ fmtPrice(f.avgPrice) }}</span>
                  </div>
                  <div class="fav-time">收藏于 {{ fmtDate(f.favoriteTime) }}</div>
                </div>
              </div>
            </el-col>
          </el-row>
          <div class="pager">
            <el-pagination v-model:current-page="favPage" :page-size="favPageSize" :total="favTotal"
                           layout="prev, pager, next" background @current-change="fetchFavorites" />
          </div>
        </el-tab-pane>

        <!-- ============ 我的订单 ============ -->
        <el-tab-pane label="我的订单" name="orders">
          <el-empty v-if="!orderLoading && orders.length === 0" description="还没有优惠券订单，去商铺页抢券吧"
                    :image-size="80" />
          <el-table v-loading="orderLoading" :data="orders" stripe>
            <el-table-column prop="voucherTitle" label="优惠券" min-width="160" show-overflow-tooltip />
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
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="orderStatusType(row.status)">
                  {{ orderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" size="small" type="primary"
                           :loading="payingId === row.id" @click="pay(row)">
                  去支付
                </el-button>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination v-model:current-page="orderPage" :page-size="orderPageSize"
                           :total="orderTotal" layout="prev, pager, next" background
                           @current-change="fetchOrders" />
          </div>
        </el-tab-pane>

        <!-- ============ 我的消息 ============ -->
        <el-tab-pane name="messages">
          <template #label>
            <el-badge :value="unread" :hidden="!unread" :max="99">我的消息</el-badge>
          </template>
          <div class="msg-toolbar">
            <el-radio-group v-model="msgFilter" @change="fetchMessages">
              <el-radio-button :value="-1">全部</el-radio-button>
              <el-radio-button :value="0">未读</el-radio-button>
              <el-radio-button :value="1">已读</el-radio-button>
            </el-radio-group>
            <el-button link type="primary" @click="readAll">全部已读</el-button>
          </div>
          <el-empty v-if="!msgLoading && messages.length === 0" description="暂无消息" :image-size="80" />
          <div v-loading="msgLoading">
            <div v-for="m in messages" :key="m.id" class="item msg-item" :class="{ unread: !m.isRead }"
                 @click="readMessage(m)">
              <div class="item-main">
                <div class="item-head">
                  <el-tag size="small" :type="msgTypeTag(m.type)" effect="plain">
                    {{ msgTypeText(m.type) }}
                  </el-tag>
                  <span class="msg-title">{{ m.title }}</span>
                  <span class="item-time">{{ fmtTime(m.createTime) }}</span>
                </div>
                <div class="msg-content">{{ m.content }}</div>
              </div>
              <el-badge v-if="!m.isRead" is-dot class="msg-dot" />
            </div>
          </div>
          <div class="pager">
            <el-pagination v-model:current-page="msgPage" :page-size="msgPageSize" :total="msgTotal"
                           layout="prev, pager, next" background @current-change="fetchMessages" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { fmtTime, fmtDate, fmtPrice, IMG_PLACEHOLDER } from '@/utils/format'
import { getUser, getToken, setAuth } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

// ---------- Tab 定位（支持 /user/profile?tab=orders 直达） ----------
const activeTab = ref(route.query.tab ? String(route.query.tab) : 'info')
watch(() => route.query.tab, (t) => {
  if (t) activeTab.value = String(t)
})

// ---------- 基本信息 ----------
const myInfo = ref({})
const infoForm = ref({ nickname: '', avatar: '', gender: 2 })
const infoLoading = ref(false)
const savingInfo = ref(false)

function roleText(r) {
  return { admin: '管理员', merchant: '商家', user: '用户' }[r] || r
}

async function fetchMe() {
  infoLoading.value = true
  try {
    const data = (await request.get('/users/me')) || {}
    myInfo.value = data
    infoForm.value = {
      nickname: data.nickname || '',
      avatar: data.avatar || '',
      gender: data.gender ?? 2
    }
  } catch (e) {
    /* 静默 */
  } finally {
    infoLoading.value = false
  }
}

async function saveInfo() {
  if (!infoForm.value.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  savingInfo.value = true
  try {
    await request.put('/users/me', infoForm.value)
    ElMessage.success('保存成功')
    // 同步本地缓存的昵称，顶栏立即生效（auth.js 统一存储）
    const cached = getUser()
    if (cached) {
      setAuth(getToken(), { ...cached, nickname: infoForm.value.nickname })
    }
    fetchMe()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    savingInfo.value = false
  }
}

// ---------- 我的点评（含待审核/驳回） ----------
const myReviews = ref([])
const myReviewPage = ref(1)
const myReviewPageSize = 5
const myReviewTotal = ref(0)
const myReviewLoading = ref(false)

function reviewStatusText(s) {
  return { 0: '待审核', 1: '已通过', 2: '已驳回' }[s] || '未知'
}
function reviewStatusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info'
}

async function fetchMyReviews() {
  myReviewLoading.value = true
  try {
    const page = await request.get('/reviews/me', {
      params: { pageNum: myReviewPage.value, pageSize: myReviewPageSize }
    })
    myReviews.value = page?.records || []
    myReviewTotal.value = Number(page?.total || 0)
  } catch (e) {
    /* 静默 */
  } finally {
    myReviewLoading.value = false
  }
}

// ---------- 我的收藏 ----------
const favorites = ref([])
const favPage = ref(1)
const favPageSize = 6
const favTotal = ref(0)
const favLoading = ref(false)

async function fetchFavorites() {
  favLoading.value = true
  try {
    const page = await request.get('/favorites', {
      params: { pageNum: favPage.value, pageSize: favPageSize }
    })
    favorites.value = page?.records || []
    favTotal.value = Number(page?.total || 0)
  } catch (e) {
    /* 静默 */
  } finally {
    favLoading.value = false
  }
}

function goShop(shopId) {
  router.push(`/user/shops/${shopId}`)
}

// ---------- 我的订单 ----------
const orders = ref([])
const orderPage = ref(1)
const orderPageSize = 10
const orderTotal = ref(0)
const orderLoading = ref(false)
const payingId = ref(null)

function orderStatusText(s) {
  return { 1: '未支付', 2: '待核销', 3: '已核销', 4: '已退款', 5: '已关闭' }[s] || '未知'
}
function orderStatusType(s) {
  return { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'info' }[s] || 'info'
}

async function fetchOrders() {
  orderLoading.value = true
  try {
    const page = await request.get('/voucher-orders/me', {
      params: { pageNum: orderPage.value, pageSize: orderPageSize }
    })
    orders.value = page?.records || []
    orderTotal.value = Number(page?.total || 0)
  } catch (e) {
    /* 静默 */
  } finally {
    orderLoading.value = false
  }
}

/** 模拟支付（仅未支付订单） */
async function pay(order) {
  payingId.value = order.id
  try {
    await request.put(`/voucher-orders/${order.id}/pay`)
    ElMessage.success('支付成功，请到店出示核销')
    fetchOrders()
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    payingId.value = null
  }
}

// ---------- 我的消息 ----------
const messages = ref([])
const msgPage = ref(1)
const msgPageSize = 10
const msgTotal = ref(0)
const msgLoading = ref(false)
const msgFilter = ref(-1) // -1 全部 / 0 未读 / 1 已读
const unread = ref(0)

function msgTypeText(t) {
  return { 1: '点赞提醒', 3: '入驻审核', 4: '点评审核' }[t] || '通知'
}
function msgTypeTag(t) {
  return { 1: 'danger', 3: 'success', 4: 'warning' }[t] || 'info'
}

async function fetchMessages() {
  msgLoading.value = true
  try {
    const page = await request.get('/messages', {
      params: {
        pageNum: msgPage.value,
        pageSize: msgPageSize,
        isRead: msgFilter.value === -1 ? undefined : msgFilter.value
      }
    })
    messages.value = page?.records || []
    msgTotal.value = Number(page?.total || 0)
    fetchUnread()
  } catch (e) {
    /* 静默 */
  } finally {
    msgLoading.value = false
  }
}

async function fetchUnread() {
  try {
    unread.value = (await request.get('/messages/unread-count')) || 0
  } catch (e) {
    /* 静默 */
  }
}

/** 点击消息：未读则标记已读（幂等） */
async function readMessage(m) {
  if (m.isRead) return
  try {
    await request.put(`/messages/${m.id}/read`)
    m.isRead = 1
    fetchUnread()
  } catch (e) {
    /* 静默 */
  }
}

async function readAll() {
  try {
    await request.put('/messages/read-all')
    ElMessage.success('已全部标记为已读')
    fetchMessages()
  } catch (e) {
    /* 静默 */
  }
}

onMounted(() => {
  fetchMe()
  fetchMyReviews()
  fetchFavorites()
  fetchOrders()
  fetchMessages()
})
</script>

<style scoped>
.profile {
  padding: 16px 0;
}
.info-form {
  max-width: 480px;
  padding-top: 8px;
}
.narrow {
  max-width: 280px;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.avatar-input {
  max-width: 320px;
}
.role-tag {
  margin-right: 8px;
}
.item {
  padding: 14px 4px;
  border-bottom: 1px solid #f0f0f0;
  border-radius: 4px;
}
.item:last-child {
  border-bottom: none;
}
.item-main {
  flex: 1;
  min-width: 0;
}
.item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.item-time {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: auto;
}
.review-title {
  font-weight: 600;
  font-size: 14px;
  margin-top: 6px;
}
.review-content {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.reject-tip {
  margin-top: 8px;
}
.fav-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.fav-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.fav-cover {
  width: 100%;
  height: 120px;
  display: block;
  background: #f5f7fa;
}
.fav-info {
  padding: 10px 12px;
}
.fav-name {
  font-weight: 600;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.fav-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.price {
  color: #f56c6c;
  font-weight: 600;
}
.fav-time {
  color: #c0c4cc;
  font-size: 12px;
  margin-top: 4px;
}
.msg-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.msg-item {
  display: flex;
  align-items: center;
  cursor: pointer;
}
.msg-item.unread {
  background: #f0f7ff;
}
.msg-title {
  font-weight: 600;
  font-size: 13px;
}
.msg-content {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
}
.msg-dot {
  flex-shrink: 0;
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

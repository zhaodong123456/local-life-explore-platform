<template>
  <div class="detail" v-loading="loading">
    <!-- 商铺信息头卡 -->
    <el-card shadow="never" class="head-card">
      <div class="head">
        <el-image :src="shop.coverImg || IMG_PLACEHOLDER" fit="cover" class="head-cover">
          <template #error>
            <el-image :src="IMG_PLACEHOLDER" fit="cover" class="head-cover" />
          </template>
        </el-image>
        <div class="head-info">
          <div class="title-row">
            <h2 class="name">{{ shop.name || '加载中...' }}</h2>
            <el-tag v-if="shop.typeName" type="warning" effect="plain">{{ shop.typeName }}</el-tag>
            <el-tag v-if="shop.status === 2" type="info">已下架</el-tag>
          </div>
          <div class="line"><el-icon><Location /></el-icon>{{ shop.province }} {{ shop.city }} {{ shop.address }}</div>
          <div class="line">人均消费：<span class="price">{{ fmtPrice(shop.avgPrice) }}</span></div>
          <div v-if="shop.description" class="line desc">{{ shop.description }}</div>
          <div class="actions">
            <el-button :type="favored ? 'warning' : 'default'" round :loading="favoriteLoading"
                       @click="toggleFavorite">
              {{ favored ? '★ 已收藏' : '☆ 收藏' }}
            </el-button>
            <el-button type="primary" round @click="openPublish">写点评</el-button>
          </div>
        </div>
      </div>

      <!-- 商铺相册 -->
      <div v-if="shopImages.length" class="gallery">
        <el-image v-for="(img, i) in shopImages" :key="i" :src="img" fit="cover"
                  class="gallery-item" :preview-src-list="shopImages" :initial-index="i">
          <template #error>
            <el-image :src="IMG_PLACEHOLDER" fit="cover" class="gallery-item" />
          </template>
        </el-image>
      </div>
    </el-card>

    <!-- 优惠券区 -->
    <el-card shadow="never" class="section-card">
      <template #header><span class="section-title">优惠券</span></template>
      <el-empty v-if="!voucherLoading && vouchers.length === 0" description="该商铺暂无优惠券"
                :image-size="80" />
      <div v-loading="voucherLoading" class="voucher-list">
        <div v-for="v in vouchers" :key="v.id" class="voucher">
          <div class="v-left">
            <div class="v-amount"><span class="unit">¥</span>{{ v.payValue }}</div>
            <div class="v-condition">满{{ v.actualValue }}可用</div>
          </div>
          <div class="v-right">
            <div class="v-title-row">
              <span class="v-title">{{ v.title }}</span>
              <el-tag v-if="v.type === 1" size="small" type="danger" effect="dark">秒杀</el-tag>
            </div>
            <div v-if="v.subTitle" class="v-sub">{{ v.subTitle }}</div>
            <div class="v-time">{{ fmtTime(v.beginTime) }} 至 {{ fmtTime(v.endTime) }}</div>
            <div class="v-bottom">
              <span v-if="v.type === 1" class="v-stock">剩余 {{ v.stock ?? '-' }} 件</span>
              <el-button v-if="v.type === 1 && v.status === 1" size="small" type="danger"
                         :loading="seckillingId === v.id" @click="seckill(v)">
                立即抢购
              </el-button>
              <el-tag v-else-if="v.type === 1" size="small" type="info">已下架</el-tag>
              <el-tag v-else size="small" type="info">到店使用</el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 点评区 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">用户点评（{{ reviewTotal }}）</span>
        </div>
      </template>

      <el-empty v-if="!reviewLoading && reviews.length === 0" description="还没有点评，快来抢沙发"
                :image-size="80" />

      <div v-loading="reviewLoading">
        <div v-for="r in reviews" :key="r.id" class="review">
          <el-avatar :size="40">{{ (r.nickname || '匿').slice(0, 1) }}</el-avatar>
          <div class="r-body">
            <div class="r-head">
              <span class="r-nickname">{{ r.nickname }}</span>
              <el-rate :model-value="r.score" disabled size="small" />
              <span class="r-time">{{ fmtTime(r.createTime) }}</span>
            </div>
            <div class="r-title">{{ r.title }}</div>
            <div class="r-content">{{ r.content }}</div>
            <div v-if="splitImages(r.images).length" class="r-images">
              <el-image v-for="(img, i) in splitImages(r.images)" :key="i" :src="img" fit="cover"
                        class="r-img" :preview-src-list="splitImages(r.images)" :initial-index="i">
                <template #error>
                  <el-image :src="IMG_PLACEHOLDER" fit="cover" class="r-img" />
                </template>
              </el-image>
            </div>
            <div class="r-footer">
              <el-button link :type="r.liked ? 'danger' : 'default'" @click="toggleLike(r)">
                <el-icon><component :is="r.liked ? StarFilled : Star" /></el-icon>
                <span>{{ r.liked ? '已赞' : '点赞' }} {{ r.likeCount || 0 }}</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="pager">
        <el-pagination v-model:current-page="reviewPageNum" :page-size="reviewPageSize"
                       :total="reviewTotal" layout="prev, pager, next" background
                       @current-change="fetchReviews" />
      </div>
    </el-card>

    <!-- 发布点评对话框 -->
    <el-dialog v-model="publishVisible" title="发布点评" width="520px">
      <el-alert title="点评提交后需管理员审核通过才会公开展示" type="info" :closable="false" show-icon
                class="publish-tip" />
      <el-form :model="publishForm" label-width="64px">
        <el-form-item label="评分" required>
          <el-rate v-model="publishForm.score" show-text :texts="['很差', '较差', '一般', '不错', '推荐']" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="publishForm.title" maxlength="30" show-word-limit placeholder="一句话概括体验" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="publishForm.content" type="textarea" :rows="4" maxlength="500"
                    show-word-limit placeholder="分享你的探店体验..." />
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="publishForm.images" placeholder="图片URL，多个用英文逗号分隔（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">提交点评</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { fmtTime, fmtPrice, splitImages, IMG_PLACEHOLDER } from '@/utils/format'
import { getUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const shopId = computed(() => route.params.id)

const shop = ref({})
const loading = ref(false)
const shopImages = computed(() => splitImages(shop.value.images))

// ---------- 收藏 ----------
const favored = ref(false)
const favoriteLoading = ref(false)

// ---------- 优惠券 ----------
const vouchers = ref([])
const voucherLoading = ref(false)
const seckillingId = ref(null)

// ---------- 点评 ----------
const reviews = ref([])
const reviewPageNum = ref(1)
const reviewPageSize = 5
const reviewTotal = ref(0)
const reviewLoading = ref(false)

// ---------- 发布点评 ----------
const publishVisible = ref(false)
const publishing = ref(false)
const publishForm = ref({ title: '', content: '', images: '', score: 5 })
// 一人一铺一评（后端约束：待审核/已通过占用资格，已驳回可重评）
const myReviewExists = ref(false)
const myUserId = getUser()?.userId

async function fetchDetail() {
  loading.value = true
  try {
    shop.value = (await request.get(`/shops/${shopId.value}`)) || {}
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

/** 是否已收藏：复用"我的收藏"分页接口判断（个人数据量小可接受；
 *  更优方案是后端在 ShopVO 上补 isFavorite 字段，本批次不改后端） */
async function fetchFavoriteState() {
  try {
    const page = await request.get('/favorites', { params: { pageNum: 1, pageSize: 100 } })
    favored.value = (page?.records || []).some((f) => String(f.shopId) === String(shopId.value))
  } catch (e) {
    /* 静默 */
  }
}

async function toggleFavorite() {
  favoriteLoading.value = true
  try {
    if (favored.value) {
      await request.delete(`/shops/${shopId.value}/favorite`)
      favored.value = false
      ElMessage.success('已取消收藏')
    } else {
      await request.put(`/shops/${shopId.value}/favorite`)
      favored.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    favoriteLoading.value = false
  }
}

async function fetchVouchers() {
  voucherLoading.value = true
  try {
    const list = (await request.get(`/shops/${shopId.value}/vouchers`)) || []
    // 只展示已上架的券（status 1上架 0下架）
    vouchers.value = list.filter((v) => v.status === 1)
  } catch (e) {
    /* 静默 */
  } finally {
    voucherLoading.value = false
  }
}

/** 秒杀领券（仅秒杀券 type=1；成功返回订单ID，MQ 异步落单，去个人中心支付） */
async function seckill(voucher) {
  seckillingId.value = voucher.id
  try {
    const orderId = await request.post(`/vouchers/${voucher.id}/seckill`)
    ElMessageBox.confirm(
      `抢购成功！订单号 ${orderId}，已进入"个人中心-我的订单"，请尽快支付。`,
      '秒杀成功',
      { confirmButtonText: '去支付', cancelButtonText: '稍后再说', type: 'success' }
    ).then(() => {
      routeToProfileOrders()
    }).catch(() => {})
  } catch (e) {
    /* 失败原因（库存空/重复下单/不在活动时间）由 request.js 弹出后端消息 */
  } finally {
    seckillingId.value = null
  }
}

function routeToProfileOrders() {
  // 跳个人中心并直接打开订单 Tab（Profile 通过 query.tab 定位）
  router.push({ path: '/user/profile', query: { tab: 'orders' } })
}

async function fetchReviews() {
  reviewLoading.value = true
  try {
    const page = await request.get(`/shops/${shopId.value}/reviews`, {
      params: { pageNum: reviewPageNum.value, pageSize: reviewPageSize }
    })
    reviews.value = page?.records || []
    reviewTotal.value = Number(page?.total || 0)
    // 判断我是否已点评过（一人一铺一评）
    myReviewExists.value = reviews.value.some((r) => r.userId === myUserId)
  } catch (e) {
    /* 静默 */
  } finally {
    reviewLoading.value = false
  }
}

/** 点赞 / 取消点赞：本地乐观更新 liked 与 likeCount */
async function toggleLike(review) {
  try {
    if (review.liked) {
      await request.delete(`/reviews/${review.id}/like`)
      review.liked = false
      review.likeCount = Math.max(0, Number(review.likeCount || 0) - 1)
    } else {
      await request.put(`/reviews/${review.id}/like`)
      review.liked = true
      review.likeCount = Number(review.likeCount || 0) + 1
    }
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  }
}

function openPublish() {
  if (myReviewExists.value) {
    ElMessage.warning('你已点评过该商铺（一人一铺一评），审核通过后即可展示')
    return
  }
  publishForm.value = { title: '', content: '', images: '', score: 5 }
  publishVisible.value = true
}

async function submitPublish() {
  if (!publishForm.value.title.trim() || !publishForm.value.content.trim()) {
    ElMessage.warning('请填写点评标题和内容')
    return
  }
  publishing.value = true
  try {
    await request.post(`/shops/${shopId.value}/reviews`, publishForm.value)
    ElMessage.success('点评已提交，等待管理员审核')
    publishVisible.value = false
    myReviewExists.value = true
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    publishing.value = false
  }
}

// 路由参数变化（从详情A切到详情B）时整体重新加载
watch(shopId, () => {
  if (!shopId.value) return
  fetchDetail()
  fetchVouchers()
  fetchReviews()
  fetchFavoriteState()
})

onMounted(() => {
  fetchDetail()
  fetchVouchers()
  fetchReviews()
  fetchFavoriteState()
})
</script>

<style scoped>
.detail {
  padding: 16px 0;
}
.head-card {
  margin-bottom: 16px;
}
.head {
  display: flex;
  gap: 20px;
}
.head-cover {
  width: 280px;
  height: 190px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f5f7fa;
}
.head-info {
  flex: 1;
  min-width: 0;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.name {
  margin: 0 0 4px;
  font-size: 20px;
}
.line {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.desc {
  color: #909399;
}
.price {
  color: #f56c6c;
  font-weight: 600;
  font-size: 15px;
}
.actions {
  margin-top: 14px;
}
.gallery {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  overflow-x: auto;
}
.gallery-item {
  width: 140px;
  height: 90px;
  border-radius: 6px;
  flex-shrink: 0;
  background: #f5f7fa;
}
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-title {
  font-weight: 600;
  font-size: 15px;
}
.voucher-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
  min-height: 40px;
}
.voucher {
  display: flex;
  border: 1px dashed #f89898;
  border-radius: 8px;
  overflow: hidden;
  background: #fff5f5;
}
.v-left {
  width: 100px;
  background: linear-gradient(135deg, #f56c6c, #fb9296);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.v-amount {
  font-size: 22px;
  font-weight: 700;
}
.v-amount .unit {
  font-size: 13px;
}
.v-condition {
  font-size: 11px;
  opacity: 0.9;
  margin-top: 2px;
}
.v-right {
  flex: 1;
  padding: 10px 12px;
  min-width: 0;
}
.v-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.v-title {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.v-sub {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}
.v-time {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.v-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.v-stock {
  color: #f56c6c;
  font-size: 12px;
}
.review {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}
.review:last-child {
  border-bottom: none;
}
.r-body {
  flex: 1;
  min-width: 0;
}
.r-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.r-nickname {
  font-weight: 600;
  font-size: 13px;
}
.r-time {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: auto;
}
.r-title {
  font-weight: 600;
  font-size: 14px;
  margin-top: 6px;
}
.r-content {
  color: #606266;
  font-size: 13px;
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.r-images {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.r-img {
  width: 96px;
  height: 96px;
  border-radius: 6px;
  background: #f5f7fa;
}
.r-footer {
  margin-top: 6px;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
.publish-tip {
  margin-bottom: 14px;
}

@media (max-width: 768px) {
  .head {
    flex-direction: column;
  }
  .head-cover {
    width: 100%;
  }
}
</style>

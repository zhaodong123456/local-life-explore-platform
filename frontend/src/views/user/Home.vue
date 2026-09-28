<template>
  <div class="home">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索商铺名称 / 美食 / 休闲娱乐..." clearable
                  class="search-input" @keyup.enter="search">
          <template #append>
            <el-button :icon="Search" @click="search" />
          </template>
        </el-input>
      </div>
    </el-card>

    <!-- 个性化推荐区（User-CF 协同过滤，冷启动兜底热门榜） -->
    <el-card v-if="recommendList.length > 0" shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span class="section-title">为你推荐</span>
          <el-tag size="small" effect="plain" round>协同过滤推荐流</el-tag>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col v-for="s in recommendList" :key="'r' + s.id" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="shop-card recommend" @click="goDetail(s.id)">
            <el-image :src="s.coverImg || IMG_PLACEHOLDER" fit="cover" class="cover">
              <template #error>
                <el-image :src="IMG_PLACEHOLDER" fit="cover" class="cover" />
              </template>
            </el-image>
            <div class="info">
              <div class="name">{{ s.name }}</div>
              <div class="meta">
                <el-tag size="small" type="warning" effect="plain">{{ s.typeName || '其他' }}</el-tag>
                <span class="price">人均 {{ fmtPrice(s.avgPrice) }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 全部商铺列表 -->
    <el-card shadow="never" class="section-card" v-loading="loading">
      <template #header>
        <div class="section-header">
          <span class="section-title">{{ keyword ? `“${keyword}” 的搜索结果` : '全部商铺' }}</span>
        </div>
      </template>

      <el-empty v-if="!loading && shopList.length === 0" description="没有找到相关商铺" />

      <el-row :gutter="16">
        <el-col v-for="s in shopList" :key="s.id" :xs="24" :sm="12" :md="8" :lg="6">
          <div class="shop-card" @click="goDetail(s.id)">
            <el-image :src="s.coverImg || IMG_PLACEHOLDER" fit="cover" class="cover">
              <template #error>
                <el-image :src="IMG_PLACEHOLDER" fit="cover" class="cover" />
              </template>
            </el-image>
            <div class="info">
              <div class="name">{{ s.name }}</div>
              <div class="meta">
                <el-tag size="small" type="warning" effect="plain">{{ s.typeName || '其他' }}</el-tag>
                <span class="price">人均 {{ fmtPrice(s.avgPrice) }}</span>
              </div>
              <div class="address">{{ s.city }} {{ s.address }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background
                       @current-change="fetchShops" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { fmtPrice, IMG_PLACEHOLDER } from '@/utils/format'

const router = useRouter()

const keyword = ref('')
const shopList = ref([])
const pageNum = ref(1)
const pageSize = 12
const total = ref(0)
const loading = ref(false)

/** 推荐商铺（后端 User-CF：个性化 → 热门兜底） */
const recommendList = ref([])

async function fetchShops() {
  loading.value = true
  try {
    const page = await request.get('/shops', {
      params: {
        pageNum: pageNum.value,
        pageSize,
        keyword: keyword.value || undefined
      }
    })
    shopList.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

async function fetchRecommend() {
  try {
    recommendList.value = (await request.get('/recommendations', { params: { count: 8 } })) || []
  } catch (e) {
    /* 推荐失败不影响主列表 */
  }
}

/** 搜索：重置回第一页 */
function search() {
  pageNum.value = 1
  fetchShops()
}

function goDetail(id) {
  router.push(`/user/shops/${id}`)
}

onMounted(() => {
  fetchShops()
  fetchRecommend()
})
</script>

<style scoped>
.home {
  padding: 16px 0;
}
.search-card {
  margin-bottom: 16px;
}
.search-bar {
  display: flex;
  justify-content: center;
}
.search-input {
  width: 100%;
  max-width: 560px;
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
.shop-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  background: #fff;
  transition: transform 0.2s, box-shadow 0.2s;
}
.shop-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.shop-card.recommend {
  border-color: #f3d19e;
}
.cover {
  width: 100%;
  height: 140px;
  display: block;
  background: #f5f7fa;
}
.info {
  padding: 10px 12px;
}
.name {
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.price {
  color: #f56c6c;
  font-size: 13px;
  font-weight: 600;
}
.address {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}
</style>

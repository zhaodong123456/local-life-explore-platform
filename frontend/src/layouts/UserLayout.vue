<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand" @click="router.push('/user/home')">
        <el-icon><Location /></el-icon>
        <span>本地生活探店</span>
      </div>
      <el-menu mode="horizontal" :default-active="activeMenu" router class="nav" :ellipsis="false">
        <el-menu-item index="/user/home">首页</el-menu-item>
        <el-menu-item index="/user/profile">
          <el-badge :value="unread" :hidden="!unread" :max="99">个人中心</el-badge>
        </el-menu-item>
        <el-menu-item v-if="isMerchant" index="/merchant/dashboard">商家后台</el-menu-item>
      </el-menu>
      <div class="right">
        <span class="nickname">{{ user?.nickname || '用户' }}</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-main class="main">
      <div class="page">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'
import { getUser, clearAuth } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const user = computed(() => getUser())

/** 多角色用户（user+merchant）可在用户端与商家后台间切换 */
const isMerchant = computed(() => (user.value?.roles || []).includes('merchant'))

const activeMenu = computed(() => (route.path.startsWith('/user/shops/') ? '/user/home' : route.path))

/** 消息未读数（小红点）：挂载与每次路由切换时刷新 */
const unread = ref(0)
let timer = null

async function refreshUnread() {
  try {
    unread.value = (await request.get('/messages/unread-count')) || 0
  } catch (e) {
    /* 静默：红点刷新失败不打扰用户 */
  }
}

function handleLogout() {
  clearAuth()
  router.push('/login')
}

onMounted(() => {
  refreshUnread()
  // 60s 轻量轮询兜底，保证红点基本实时
  timer = setInterval(refreshUnread, 60000)
})

// 路由切换（如从消息页操作返回）时立即刷新红点
watch(() => route.path, refreshUnread)

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 32px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
}
.brand {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 16px;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}
.nav {
  flex: 1;
  border-bottom: none;
}
.nav :deep(.el-badge) {
  vertical-align: baseline;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
.nickname {
  color: #606266;
}
.main {
  background-color: #f5f7fa;
}
.page {
  max-width: 1100px;
  margin: 0 auto;
}
</style>

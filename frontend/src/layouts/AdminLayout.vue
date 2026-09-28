<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">管理后台</div>
      <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409eff">
        <el-menu-item index="/admin/overview">
          <el-icon><Platform /></el-icon><span>平台总览</span>
        </el-menu-item>
        <el-menu-item index="/admin/review-audit">
          <el-icon><ChatDotRound /></el-icon><span>点评审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/shop-audit">
          <el-icon><Shop /></el-icon><span>商铺审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/merchant-audit">
          <el-icon><Avatar /></el-icon><span>入驻审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>账号管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="nickname">{{ user?.nickname || '管理员' }}</span>
        <el-button link type="primary" @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUser, clearAuth } from '@/utils/auth'

const router = useRouter()
const user = computed(() => getUser())

function handleLogout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 600;
}
.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
}
</style>

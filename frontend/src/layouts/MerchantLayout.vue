<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">商家后台</div>
      <el-menu :default-active="$route.path" router background-color="#304156" text-color="#bfcbd9"
               active-text-color="#409eff">
        <el-menu-item index="/merchant/dashboard">
          <el-icon><DataLine /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/merchant/shops">
          <el-icon><Shop /></el-icon><span>商铺管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/vouchers">
          <el-icon><Ticket /></el-icon><span>优惠券管理</span>
        </el-menu-item>
        <el-menu-item index="/merchant/writeoff">
          <el-icon><Finished /></el-icon><span>核销记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="nickname">{{ user?.nickname || '商家' }}</span>
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

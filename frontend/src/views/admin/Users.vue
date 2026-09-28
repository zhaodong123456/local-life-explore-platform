<template>
  <div class="page-manage">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="page-title">账号管理</span>
          <el-input v-model="keyword" placeholder="手机号 / 昵称搜索" clearable style="width: 240px"
                    @keyup.enter="search">
            <template #append>
              <el-button :icon="Search" @click="search" />
            </template>
          </el-input>
        </div>
      </template>

      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像/昵称" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar || undefined">
                {{ (row.nickname || '用').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            {{ { 0: '女', 1: '男', 2: '保密' }[row.gender] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles || []" :key="r" size="small" effect="plain"
                    :type="roleType(r)" class="role-tag">
              {{ roleText(r) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                       layout="prev, pager, next, total" background @current-change="fetchUsers" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

const users = ref([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)

function roleText(r) {
  return { admin: '管理员', merchant: '商家', user: '用户' }[r] || r
}
function roleType(r) {
  return { admin: 'danger', merchant: 'warning', user: 'info' }[r] || 'info'
}

async function fetchUsers() {
  loading.value = true
  try {
    const page = await request.get('/admin/users', {
      params: { pageNum: pageNum.value, pageSize, keyword: keyword.value || undefined }
    })
    users.value = page?.records || []
    total.value = Number(page?.total || 0)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  fetchUsers()
}

onMounted(fetchUsers)
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
  gap: 8px;
}
.role-tag {
  margin-right: 6px;
}
.pager {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}
</style>

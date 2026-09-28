import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'

/**
 * 统一 axios 封装
 * - baseURL：/api（开发环境由 Vite 代理到 http://localhost:8080）
 * - 请求拦截：自动在请求头携带 JWT
 * - 响应拦截：按后端统一 Result 处理，401 自动跳登录
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：携带 Token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理 Result 与异常
service.interceptors.response.use(
  (response) => {
    const result = response.data
    // 非标准 Result（如文件流）直接原样返回
    if (result === null || typeof result !== 'object' || result.code === undefined) {
      return result
    }
    if (result.code === 200) {
      // 成功：直接返回业务数据 data
      return result.data
    }
    // 业务失败：弹出后端返回的错误信息
    ElMessage.error(result.message || '请求失败')
    if (result.code === 401) {
      handleUnauthorized()
    }
    return Promise.reject(new Error(result.message || 'Error'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else if (status === 403) {
      ElMessage.error('没有访问权限')
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  clearAuth()
  // 避免在登录页重复跳转
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

export default service

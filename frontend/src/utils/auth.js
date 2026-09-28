/**
 * 登录态本地存储工具（不引入额外状态库，保持脚手架轻量）
 * Token 与登录用户信息持久化在 localStorage，刷新页面不丢失。
 */

const TOKEN_KEY = 'explore_token'
const USER_KEY = 'explore_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAuth(token, userInfo) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

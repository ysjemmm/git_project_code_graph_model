// 认证相关工具函数

import type { UserInfo } from '../api/invoke'

const USER_INFO_KEY = 'user_info'

/**
 * 获取本地缓存的用户信息
 */
export function getUserInfo(): UserInfo | null {
  const info = localStorage.getItem(USER_INFO_KEY)
  return info ? JSON.parse(info) : null
}

/**
 * 清除本地缓存
 */
export function clearUserInfo(): void {
  localStorage.removeItem(USER_INFO_KEY)
}

/**
 * 调用 invoke('forward', 'getUserInfo') 获取用户信息并缓存
 */
export async function fetchAndCacheUserInfo(): Promise<UserInfo | null> {
  try {
    const { getUserInfo: apiGetUserInfo } = await import('../api/invoke')
    const resp = await apiGetUserInfo()
    if (resp.ok && resp.data) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(resp.data))
      return resp.data
    }
    return null
  } catch (error) {
    console.warn('获取用户信息失败:', error)
    return null
  }
}

/**
 * 登出
 */
export function logout(): void {
  clearUserInfo()
  window.location.href = `${import.meta.env.VITE_CAS_LOGIN_URL}/logout`
}

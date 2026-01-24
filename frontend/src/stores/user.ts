import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, refreshToken } from '@/api/auth'
import { storage } from '@/utils/storage'
import type { AuthUserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(storage.getToken() || '')
  const refreshTokenStr = ref<string>(storage.getRefreshToken() || '')
  const userInfo = ref<AuthUserInfo | null>(storage.getUser() || null)

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(
    () => userInfo.value?.role === 'ADMIN' || userInfo.value?.role === 'SUPER_ADMIN'
  )

  function setTokens(data: { accessToken: string; refreshToken: string }) {
    token.value = data.accessToken
    refreshTokenStr.value = data.refreshToken
    storage.setToken(data.accessToken)
    storage.setRefreshToken(data.refreshToken)
  }

  function setUserInfo(user: AuthUserInfo) {
    userInfo.value = user
    storage.setUser(user)
  }

  async function login(username: string, password: string) {
    const data = await loginApi({ username, password })
    setTokens(data)
    setUserInfo(data.user)
    return data
  }

  async function logout() {
    try {
      if (refreshTokenStr.value) {
        await logoutApi({ refreshToken: refreshTokenStr.value })
      }
    } finally {
      token.value = ''
      refreshTokenStr.value = ''
      userInfo.value = null
      storage.clear()
    }
  }

  async function refreshAccessToken() {
    if (!refreshTokenStr.value) {
      throw new Error('No refresh token')
    }
    const data = await refreshToken({ refreshToken: refreshTokenStr.value })
    setTokens(data)
    setUserInfo(data.user)
    return data.accessToken
  }

  function initAuth() {
    const storedToken = storage.getToken()
    const storedRefreshToken = storage.getRefreshToken()
    const storedUser = storage.getUser()
    if (storedToken && storedUser) {
      token.value = storedToken
      refreshTokenStr.value = storedRefreshToken || ''
      userInfo.value = storedUser
    }
  }

  return {
    token,
    userInfo,
    isLogin,
    isAdmin,
    login,
    logout,
    refreshAccessToken,
    initAuth,
    setUserInfo
  }
})

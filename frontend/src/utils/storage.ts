import type { AuthUserInfo } from '@/types/user'

const TOKEN_KEY = 'ai_studio_token'
const REFRESH_TOKEN_KEY = 'ai_studio_refresh_token'
const USER_KEY = 'ai_studio_user'

export const storage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY)
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },

  removeRefreshToken(): void {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },

  getUser(): AuthUserInfo | null {
    try {
      const user = localStorage.getItem(USER_KEY)
      return user ? JSON.parse(user) as AuthUserInfo : null
    } catch {
      return null
    }
  },

  setUser(user: AuthUserInfo): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },

  removeUser(): void {
    localStorage.removeItem(USER_KEY)
  },

  clear(): void {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

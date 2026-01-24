import { post } from './request'
import type { AuthUserInfo } from '@/types/user'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: AuthUserInfo
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return post<LoginResponse>('/auth/register', data)
}

export function logout(data: RefreshTokenRequest) {
  return post('/auth/logout', data)
}

export function refreshToken(data: RefreshTokenRequest) {
  return post<LoginResponse>('/auth/refresh', data)
}

import { get, put, del, post } from './request'
import type { User, UserStatus } from '@/types/user'

export interface UpdateUserRequest {
  username?: string
  email?: string
  avatarUrl?: string
  role?: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  newPassword?: string
  currentPassword?: string
}

export interface UserProfile {
  id: number
  username: string
  email: string
  role: string
  bio: string
  language: string
  timezone: string
  createdAt: string
  updatedAt: string
}

export interface UpdateProfileRequest {
  email?: string
  bio?: string
  language?: string
  timezone?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export const userApi = {
  getAllUsers() {
    return get<User[]>('/users')
  },
  getUserById(id: number) {
    return get<User>(`/users/${id}`)
  },
  updateUser(id: number, data: UpdateUserRequest) {
    return put<void>(`/users/${id}`, data)
  },
  updateUserStatus(id: number, status: UserStatus) {
    return put<void>(`/users/${id}/status?status=${status}`)
  },
  deleteUser(id: number) {
    return del(`/users/${id}`)
  },
  getUserProfile() {
    return get<UserProfile>('/users/me')
  },
  updateProfile(data: UpdateProfileRequest) {
    return put<void>('/users/me', data)
  },
  changePassword(data: ChangePasswordRequest) {
    return post<void>('/users/me/password', data)
  }
}

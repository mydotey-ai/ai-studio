import { get, put, del } from './request'
import type { User, UserStatus } from '@/types/user'

export interface UpdateUserRequest {
  username?: string
  email?: string
  avatarUrl?: string
  role?: 'USER' | 'ADMIN' | 'SUPER_ADMIN'
  newPassword?: string
  currentPassword?: string
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
  }
}

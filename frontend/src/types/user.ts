export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
  SUPER_ADMIN = 'SUPER_ADMIN'
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  LOCKED = 'LOCKED'
}

export interface User {
  id: number
  username: string
  email: string
  role: UserRole
  status: UserStatus
  avatarUrl?: string
  orgId?: number
  createdAt: string
  updatedAt: string
}

// Simplified user info from auth responses (login, refresh token)
export interface AuthUserInfo {
  id: number
  username: string
  email: string
  role: string
}

export interface Organization {
  id: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}

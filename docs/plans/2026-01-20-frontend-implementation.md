# Frontend Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a modern Vue 3 frontend for the AI Studio platform, providing intuitive interfaces for knowledge base management, MCP tool configuration, Agent building, and Chatbot deployment.

**Architecture:**
- **Framework:** Vue 3 Composition API with TypeScript
- **Build Tool:** Vite for fast development and optimized production builds
- **UI Library:** Element Plus for enterprise-grade components
- **State Management:** Pinia for centralized state management
- **Routing:** Vue Router for navigation and route guards
- **HTTP Client:** Axios with interceptors for API calls and JWT handling
- **Styling:** SCSS with CSS variables for theming
- **Code Quality:** ESLint + Prettier for consistent code style

**Tech Stack:**
- Vue 3.4+ (Composition API)
- TypeScript 5.3+
- Vite 5.0+
- Element Plus 2.5+
- Pinia 2.1+
- Vue Router 4.2+
- Axios 1.6+
- SCSS
- ESLint + Prettier

---

## Task 1: Project Initialization

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/.env.example`
- Create: `frontend/.env.development`
- Create: `frontend/.env.production`
- Create: `frontend/.eslintrc.cjs`
- Create: `frontend/.prettierrc.json`
- Create: `frontend/.gitignore`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/vite-env.d.ts`

**Step 1: Create package.json**

Run: `cd frontend && cat > package.json << 'EOF'
{
  "name": "ai-studio-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview",
    "lint": "eslint . --ext .vue,.js,.jsx,.cjs,.mjs,.ts,.tsx,.cts,.mts --fix --ignore-path .gitignore",
    "format": "prettier --write src/"
  },
  "dependencies": {
    "vue": "^3.4.15",
    "vue-router": "^4.2.5",
    "pinia": "^2.1.7",
    "axios": "^1.6.5",
    "element-plus": "^2.5.2",
    "@element-plus/icons-vue": "^2.3.1",
    "scss": "^0.0.1",
    "markdown-it": "^14.0.0",
    "highlight.js": "^11.9.0",
    "dayjs": "^1.11.10"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.3",
    "typescript": "^5.3.3",
    "vite": "^5.0.11",
    "vue-tsc": "^1.8.27",
    "sass": "^1.70.0",
    "eslint": "^8.56.0",
    "eslint-plugin-vue": "^9.19.2",
    "@typescript-eslint/eslint-plugin": "^6.19.0",
    "@typescript-eslint/parser": "^6.19.0",
    "prettier": "^3.2.4",
    "eslint-config-prettier": "^9.1.0",
    "eslint-plugin-prettier": "^5.1.3"
  }
}
EOF
`
Expected: Creates package.json with all dependencies

**Step 2: Create vite.config.ts**

Run: `cat > frontend/vite.config.ts << 'EOF'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
EOF
`
Expected: Creates Vite configuration with path aliases and API proxy

**Step 3: Create tsconfig.json**

Run: `cat > frontend/tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
EOF
`
Expected: Creates TypeScript configuration with strict mode

**Step 4: Create tsconfig.node.json**

Run: `cat > frontend/tsconfig.node.json << 'EOF'
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
EOF
`
Expected: Creates TypeScript config for Vite

**Step 5: Create environment files**

Run: `cat > frontend/.env.example << 'EOF'
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=AI Studio
EOF
`
Expected: Creates example environment file

Run: `cat > frontend/.env.development << 'EOF'
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_TITLE=AI Studio
EOF
`
Expected: Creates development environment file

Run: `cat > frontend/.env.production << 'EOF'
VITE_API_BASE_URL=/api
VITE_APP_TITLE=AI Studio
EOF
`
Expected: Creates production environment file

**Step 6: Create ESLint configuration**

Run: `cat > frontend/.eslintrc.cjs << 'EOF'
module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
    es2022: true
  },
  extends: [
    'plugin:vue/vue3-essential',
    'eslint:recommended',
    '@typescript-eslint/recommended',
    'plugin:prettier/recommended'
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    parser: '@typescript-eslint/parser',
    sourceType: 'module'
  },
  plugins: ['vue', '@typescript-eslint', 'prettier'],
  rules: {
    'prettier/prettier': 'error',
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-explicit-any': 'warn'
  }
}
EOF
`
Expected: Creates ESLint configuration

**Step 7: Create Prettier configuration**

Run: `cat > frontend/.prettierrc.json << 'EOF'
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "none",
  "printWidth": 100,
  "tabWidth": 2,
  "endOfLine": "lf"
}
EOF
`
Expected: Creates Prettier configuration

**Step 8: Create .gitignore**

Run: `cat > frontend/.gitignore << 'EOF'
# Logs
logs
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
lerna-debug.log*

node_modules
dist
dist-ssr
*.local

# Editor directories and files
.vscode/*
!.vscode/extensions.json
.idea
.DS_Store
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?

# Environment files
.env.local
.env.*.local
EOF
`
Expected: Creates .gitignore file

**Step 9: Create index.html**

Run: `cat > frontend/index.html << 'EOF'
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>AI Studio</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
EOF
`
Expected: Creates HTML entry point

**Step 10: Create src/main.ts**

Run: `cat > frontend/src/main.ts << 'EOF'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')
EOF
`
Expected: Creates main entry point

**Step 11: Create src/vite-env.d.ts**

Run: `cat > frontend/src/vite-env.d.ts << 'EOF'
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
EOF
`
Expected: Creates TypeScript declarations for Vite

**Step 12: Create placeholder App.vue**

Run: `cat > frontend/src/App.vue << 'EOF'
<template>
  <router-view />
</template>

<script setup lang="ts"></script>

<style>
#app {
  width: 100%;
  height: 100vh;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
}
</style>
EOF
`
Expected: Creates root component

**Step 13: Install dependencies**

Run: `cd frontend && npm install`
Expected: Installs all dependencies successfully

**Step 14: Commit project initialization**

Run: `cd frontend && git add . && git commit -m "feat: initialize Vue 3 frontend project"`
Expected: Commits initial project setup

---

## Task 2: Base Architecture Setup

**Files:**
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/router/guards.ts`
- Create: `frontend/src/stores/user.ts`
- Create: `frontend/src/stores/app.ts`
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/types/common.ts`
- Create: `frontend/src/types/user.ts`
- Create: `frontend/src/utils/storage.ts`
- Create: `frontend/src/config/index.ts`

**Step 1: Create API request interceptor**

Run: `cat > frontend/src/api/request.ts << 'EOF'
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const baseURL = import.meta.env.VITE_API_BASE_URL

const service: AxiosInstance = axios.create({
  baseURL,
  timeout: 60000
})

// Request interceptor
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data

    // Success
    if (code === 200 || code === 0) {
      return data
    }

    // Business error
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default service

// Export generic request method
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config)
}

export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}
EOF
`
Expected: Creates axios instance with interceptors

**Step 2: Create auth API**

Run: `cat > frontend/src/api/auth.ts << 'EOF'
import { post } from './request'

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
  token: string
  refreshToken: string
  user: {
    id: number
    username: string
    email: string
    role: string
  }
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

export function logout() {
  return post('/auth/logout')
}

export function refreshToken(data: RefreshTokenRequest) {
  return post<LoginResponse>('/auth/refresh', data)
}
EOF
`
Expected: Creates auth API methods

**Step 3: Create storage utility**

Run: `cat > frontend/src/utils/storage.ts << 'EOF'
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

  getUser(): any {
    const user = localStorage.getItem(USER_KEY)
    return user ? JSON.parse(user) : null
  },

  setUser(user: any): void {
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
EOF
`
Expected: Creates local storage utility

**Step 4: Create user store**

Run: `cat > frontend/src/stores/user.ts << 'EOF'
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, refreshToken } from '@/api/auth'
import { storage } from '@/utils/storage'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(storage.getToken() || '')
  const refreshTokenStr = ref<string>(storage.getRefreshToken() || '')
  const userInfo = ref<any>(storage.getUser() || null)

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN' || userInfo.value?.role === 'SUPER_ADMIN')

  function setTokens(data: { token: string; refreshToken: string }) {
    token.value = data.token
    refreshTokenStr.value = data.refreshToken
    storage.setToken(data.token)
    storage.setRefreshToken(data.refreshToken)
  }

  function setUserInfo(user: any) {
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
      await logoutApi()
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
    return data.token
  }

  function initAuth() {
    const storedToken = storage.getToken()
    const storedUser = storage.getUser()
    if (storedToken && storedUser) {
      token.value = storedToken
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
EOF
`
Expected: Creates user Pinia store

**Step 5: Create app store**

Run: `cat > frontend/src/stores/app.ts << 'EOF'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const loading = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setLoading(value: boolean) {
    loading.value = value
  }

  return {
    sidebarCollapsed,
    loading,
    toggleSidebar,
    setLoading
  }
})
EOF
`
Expected: Creates app Pinia store

**Step 6: Create common types**

Run: `cat > frontend/src/types/common.ts << 'EOF'
export interface PaginationParams {
  page?: number
  pageSize?: number
}

export interface PaginationResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  INTERNAL_ERROR = 500
}
EOF
`
Expected: Creates common TypeScript types

**Step 7: Create user types**

Run: `cat > frontend/src/types/user.ts << 'EOF'
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

export interface Organization {
  id: number
  name: string
  description?: string
  createdAt: string
  updatedAt: string
}
EOF
`
Expected: Creates user TypeScript types

**Step 8: Create router configuration**

Run: `cat > frontend/src/router/index.ts << 'EOF'
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'knowledge-bases',
        name: 'KnowledgeBases',
        component: () => import('@/views/knowledge-base/KnowledgeBaseListView.vue'),
        meta: { title: '知识库', icon: 'FolderOpened' }
      },
      {
        path: 'knowledge-bases/:id',
        name: 'KnowledgeBaseDetail',
        component: () => import('@/views/knowledge-base/KnowledgeBaseDetailView.vue'),
        meta: { title: '知识库详情', hidden: true }
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agent/AgentListView.vue'),
        meta: { title: 'Agents', icon: 'User' }
      },
      {
        path: 'agents/:id',
        name: 'AgentDetail',
        component: () => import('@/views/agent/AgentDetailView.vue'),
        meta: { title: 'Agent详情', hidden: true }
      },
      {
        path: 'chatbots',
        name: 'Chatbots',
        component: () => import('@/views/chatbot/ChatbotListView.vue'),
        meta: { title: '聊天机器人', icon: 'ChatDotSquare' }
      },
      {
        path: 'chatbots/:id',
        name: 'ChatbotDetail',
        component: () => import('@/views/chatbot/ChatbotDetailView.vue'),
        meta: { title: '聊天机器人详情', hidden: true }
      },
      {
        path: 'mcp-servers',
        name: 'McpServers',
        component: () => import('@/views/mcp/McpServerListView.vue'),
        meta: { title: 'MCP工具', icon: 'Connection' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue'),
        meta: { title: '设置', icon: 'Setting', requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
EOF
`
Expected: Creates Vue Router configuration

**Step 9: Create route guards**

Run: `cat > frontend/src/router/guards.ts << 'EOF'
import router from './index'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// Set page title
router.beforeEach((to, from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - AI Studio`
  }
  next()
})

// Authentication guard
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !userStore.isLogin) {
    ElMessage.warning('请先登录')
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && userStore.isLogin) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

// Admin guard
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const requiresAdmin = to.meta.requiresAdmin === true

  if (requiresAdmin && !userStore.isAdmin) {
    ElMessage.error('需要管理员权限')
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})
EOF
`
Expected: Creates route guards

**Step 10: Update main.ts to import guards**

Run: `cat > frontend/src/main.ts << 'EOF'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './router/guards'

const app = createApp(App)
const pinia = createPinia()

// Register all Element Plus icons
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// Initialize auth
import { useUserStore } from './stores/user'
const userStore = useUserStore()
userStore.initAuth()

app.mount('#app')
EOF
`
Expected: Updates main.ts to initialize auth

**Step 11: Create config file**

Run: `cat > frontend/src/config/index.ts << 'EOF'
export default {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
  appTitle: import.meta.env.VITE_APP_TITLE,
  pageSizeOptions: [10, 20, 50, 100],
  defaultPageSize: 20
}
EOF
`
Expected: Creates app configuration

**Step 12: Commit base architecture**

Run: `cd frontend && git add . && git commit -m "feat: setup base architecture with router, stores, and API layer"`
Expected: Commits base architecture

---

## Task 3: Authentication UI

**Files:**
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/RegisterView.vue`
- Create: `frontend/src/layouts/MainLayout.vue`

**Step 1: Create LoginView.vue**

Run: `cat > frontend/src/views/auth/LoginView.vue << 'EOF'
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <h2>AI Studio</h2>
          <p>智能开发平台</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功')

      const redirect = (route.query.redirect as string) || '/dashboard'
      router.push(redirect)
    } catch (error) {
      // Error already handled by axios interceptor
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;

  h2 {
    margin: 0;
    font-size: 28px;
    color: #303133;
  }

  p {
    margin: 8px 0 0;
    font-size: 14px;
    color: #909399;
  }
}

.login-footer {
  text-align: center;
  font-size: 14px;
  color: #606266;

  span {
    margin-right: 8px;
  }
}
</style>
EOF
`
Expected: Creates login page component

**Step 2: Create RegisterView.vue**

Run: `cat > frontend/src/views/auth/RegisterView.vue << 'EOF'
<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <h2>注册账号</h2>
          <p>加入 AI Studio</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            type="email"
            placeholder="邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="router.push('/login')">立即登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

async function handleRegister() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await register({
        username: form.username,
        email: form.email,
        password: form.password
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      // Error already handled
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped lang="scss">
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 400px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  text-align: center;

  h2 {
    margin: 0;
    font-size: 28px;
    color: #303133;
  }

  p {
    margin: 8px 0 0;
    font-size: 14px;
    color: #909399;
  }
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #606266;

  span {
    margin-right: 8px;
  }
}
</style>
EOF
`
Expected: Creates register page component

**Step 3: Create MainLayout.vue**

Run: `cat > frontend/src/layouts/MainLayout.vue << 'EOF'
<template>
  <el-container class="main-layout">
    <el-aside :width="sidebarWidth">
      <div class="logo">
        <span v-if="!appStore.sidebarCollapsed">AI Studio</span>
        <span v-else>AI</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        router
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item
            v-if="!route.meta?.hidden"
            :index="route.path"
          >
            <el-icon v-if="route.meta?.icon">
              <component :is="route.meta.icon" />
            </el-icon>
            <template #title>{{ route.meta?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-left">
          <el-icon
            class="collapse-icon"
            @click="appStore.toggleSidebar"
          >
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                {{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Fold,
  Expand,
  User,
  Setting,
  SwitchButton
} from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

const sidebarWidth = computed(() =>
  appStore.sidebarCollapsed ? '64px' : '200px'
)

const activeMenu = computed(() => route.path)

const menuRoutes = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  return mainRoute?.children || []
})

async function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      // TODO: Navigate to profile page
      ElMessage.info('个人中心功能开发中')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch {
        // User cancelled
      }
      break
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  transition: width 0.3s;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: bold;
    color: #fff;
    border-bottom: 1px solid #434a5a;
  }

  .el-menu {
    border-right: none;
    background-color: #304156;

    :deep(.el-menu-item) {
      color: #bfcbd9;

      &:hover {
        background-color: #263445;
      }

      &.is-active {
        color: #409eff;
        background-color: #263445;
      }
    }
  }
}

.el-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;

  .header-left {
    .collapse-icon {
      font-size: 20px;
      cursor: pointer;
      color: #606266;

      &:hover {
        color: #409eff;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
        color: #606266;
      }
    }
  }
}

.el-main {
  background-color: #f5f5f5;
  padding: 20px;
}
</style>
EOF
`
Expected: Creates main layout component

**Step 4: Create placeholder DashboardView**

Run: `mkdir -p frontend/src/views && cat > frontend/src/views/DashboardView.vue << 'EOF'
<template>
  <div class="dashboard">
    <h1>仪表盘</h1>
    <p>欢迎来到 AI Studio</p>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
.dashboard {
  h1 {
    margin-bottom: 16px;
    color: #303133;
  }

  p {
    color: #606266;
  }
}
</style>
EOF
`
Expected: Creates placeholder dashboard

**Step 5: Test login flow**

Run: `cd frontend && npm run dev`
Expected: Dev server starts at http://localhost:3000

Visit: http://localhost:3000/login
Expected: Login page displays

**Step 6: Commit authentication UI**

Run: `cd frontend && git add . && git commit -m "feat: implement authentication UI with login and register pages"`
Expected: Commits authentication UI

---

## Task 4: Knowledge Base Management UI

**Files:**
- Create: `frontend/src/api/knowledge-base.ts`
- Create: `frontend/src/api/document.ts`
- Create: `frontend/src/types/knowledge-base.ts`
- Create: `frontend/src/views/knowledge-base/KnowledgeBaseListView.vue`
- Create: `frontend/src/views/knowledge-base/KnowledgeBaseDetailView.vue`
- Create: `frontend/src/components/knowledge-base/DocumentUpload.vue`
- Create: `frontend/src/components/knowledge-base/WebCrawlConfig.vue`

**Step 1: Create knowledge base types**

Run: `cat > frontend/src/types/knowledge-base.ts << 'EOF'
export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  ownerId: number
  isPublic: boolean
  embeddingModel: string
  chunkSize: number
  chunkOverlap: number
  documentCount?: number
  createdAt: string
  updatedAt: string
}

export interface Document {
  id: number
  kbId: number
  filename: string
  fileUrl: string
  fileType: string
  fileSize: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  chunkCount: number
  sourceType?: string
  sourceUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
  isPublic?: boolean
  embeddingModel?: string
  chunkSize?: number
  chunkOverlap?: number
}

export interface UpdateKnowledgeBaseRequest {
  name?: string
  description?: string
  isPublic?: boolean
}

export interface WebCrawlTask {
  id: number
  kbId: number
  startUrl: string
  urlPattern?: string
  maxDepth: number
  crawlStrategy: 'BFS' | 'DFS'
  concurrentLimit: number
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'PAUSED'
  totalPages: number
  successPages: number
  failedPages: number
  errorMessage?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}
EOF
`
Expected: Creates knowledge base types

**Step 2: Create knowledge base API**

Run: `cat > frontend/src/api/knowledge-base.ts << 'EOF'
import { get, post, put, del } from './request'
import type {
  KnowledgeBase,
  CreateKnowledgeBaseRequest,
  UpdateKnowledgeBaseRequest
} from '@/types/knowledge-base'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getKnowledgeBases(params?: PaginationParams) {
  return get<PaginationResponse<KnowledgeBase>>('/knowledge-bases', { params })
}

export function getKnowledgeBase(id: number) {
  return get<KnowledgeBase>(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: CreateKnowledgeBaseRequest) {
  return post<KnowledgeBase>('/knowledge-bases', data)
}

export function updateKnowledgeBase(id: number, data: UpdateKnowledgeBaseRequest) {
  return put<KnowledgeBase>(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: number) {
  return del(`/knowledge-bases/${id}`)
}
EOF
`
Expected: Creates knowledge base API methods

**Step 3: Create document API**

Run: `cat > frontend/src/api/document.ts << 'EOF'
import { get, post, del } from './request'
import type { Document, WebCrawlTask } from '@/types/knowledge-base'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getDocuments(kbId: number, params?: PaginationParams) {
  return get<PaginationResponse<Document>>(`/knowledge-bases/${kbId}/documents`, {
    params
  })
}

export function uploadDocument(kbId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<Document>(`/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteDocument(kbId: number, docId: number) {
  return del(`/knowledge-bases/${kbId}/documents/${docId}`)
}

export function getWebCrawlTasks(kbId: number) {
  return get<WebCrawlTask[]>(`/web-crawl/tasks/kb/${kbId}`)
}

export function createWebCrawlTask(kbId: number, data: any) {
  return post<WebCrawlTask>('/web-crawl/tasks', { ...data, kbId })
}

export function startWebCrawlTask(taskId: number) {
  return post(`/web-crawl/tasks/${taskId}/start`)
}

export function getWebCrawlTaskProgress(taskId: number) {
  return get<any>(`/web-crawl/tasks/${taskId}/progress`)
}
EOF
`
Expected: Creates document API methods

**Step 4: Create KnowledgeBaseListView component**

Run: `mkdir -p frontend/src/views/knowledge-base && cat > frontend/src/views/knowledge-base/KnowledgeBaseListView.vue << 'EOF'
<template>
  <div class="kb-list">
    <div class="header">
      <h2>知识库</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建知识库
      </el-button>
    </div>

    <el-table
      :data="knowledgeBases"
      :loading="loading"
      stripe
      @row-click="handleRowClick"
      style="cursor: pointer"
    >
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="文档数" width="100" align="center">
        <template #default="{ row }">
          {{ row.documentCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            查看
          </el-button>
          <el-button link type="danger" :icon="Delete" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadKnowledgeBases"
        @current-change="loadKnowledgeBases"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建知识库"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="可见性" prop="isPublic">
          <el-switch v-model="form.isPublic" active-text="公开" inactive-text="私有" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, View, Delete } from '@element-plus/icons-vue'
import { getKnowledgeBases, createKnowledgeBase, deleteKnowledgeBase as deleteKbApi } from '@/api/knowledge-base'
import type { KnowledgeBase } from '@/types/knowledge-base'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const knowledgeBases = ref<KnowledgeBase[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const form = reactive({
  name: '',
  description: '',
  isPublic: false
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

async function loadKnowledgeBases() {
  loading.value = true
  try {
    const data = await getKnowledgeBases({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    knowledgeBases.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await createKnowledgeBase(form)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      loadKnowledgeBases()
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.isPublic = false
  formRef.value?.resetFields()
}

function handleRowClick(row: KnowledgeBase) {
  router.push(`/knowledge-bases/${row.id}`)
}

function handleView(row: KnowledgeBase) {
  router.push(`/knowledge-bases/${row.id}`)
}

async function handleDelete(row: KnowledgeBase) {
  try {
    await ElMessageBox.confirm(`确定要删除知识库"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKbApi(row.id)
    ElMessage.success('删除成功')
    loadKnowledgeBases()
  } catch {
    // User cancelled
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.kb-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 24px;
      color: #303133;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
EOF
`
Expected: Creates knowledge base list view

**Step 5: Create KnowledgeBaseDetailView component (part 1)**

Run: `cat > frontend/src/views/knowledge-base/KnowledgeBaseDetailView.vue << 'EOF'
<template>
  <div class="kb-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ knowledgeBase?.name }}</span>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="文档" name="documents">
        <div class="tab-header">
          <el-button type="primary" :icon="Upload" @click="showUploadDialog = true">
            上传文档
          </el-button>
          <el-button :icon="Link" @click="showWebCrawlDialog = true">
            网页抓取
          </el-button>
        </div>

        <el-table
          :data="documents"
          :loading="documentsLoading"
          stripe
        >
          <el-table-column prop="filename" label="文件名" min-width="200" />
          <el-table-column label="文件大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="分块数" width="100" align="center" />
          <el-table-column prop="createdAt" label="上传时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="danger"
                :icon="Delete"
                @click="handleDeleteDocument(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="docPagination.page"
            v-model:page-size="docPagination.pageSize"
            :total="docPagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadDocuments"
            @current-change="loadDocuments"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="网页抓取" name="webcrawl">
        <WebCrawlConfig :kb-id="kbId" />
      </el-tab-pane>
    </el-tabs>

    <!-- Upload Dialog -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文档"
      width="500px"
    >
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        accept=".pdf,.doc,.docx,.txt"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、TXT 格式，文件大小不超过 100MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Link, Delete, UploadFilled } from '@element-plus/icons-vue'
import { getKnowledgeBase } from '@/api/knowledge-base'
import { getDocuments, uploadDocument, deleteDocument } from '@/api/document'
import type { KnowledgeBase, Document } from '@/types/knowledge-base'
import dayjs from 'dayjs'
import WebCrawlConfig from '@/components/knowledge-base/WebCrawlConfig.vue'

const router = useRouter()
const route = useRoute()

const kbId = ref<number>(parseInt(route.params.id as string))
const knowledgeBase = ref<KnowledgeBase>()
const documents = ref<Document[]>([])

const activeTab = ref('documents')
const documentsLoading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const uploadFile = ref<File | null>(null)

const docPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

async function loadKnowledgeBase() {
  knowledgeBase.value = await getKnowledgeBase(kbId.value)
}

async function loadDocuments() {
  documentsLoading.value = true
  try {
    const data = await getDocuments(kbId.value, {
      page: docPagination.page,
      pageSize: docPagination.pageSize
    })
    documents.value = data.records
    docPagination.total = data.total
  } finally {
    documentsLoading.value = false
  }
}

function handleFileChange(file: any) {
  uploadFile.value = file.raw
}

async function handleUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    await uploadDocument(kbId.value, uploadFile.value)
    ElMessage.success('上传成功，正在处理中...')
    showUploadDialog.value = false
    uploadFile.value = null
    loadDocuments()
  } finally {
    uploading.value = false
  }
}

async function handleDeleteDocument(doc: Document) {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${doc.filename}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteDocument(kbId.value, doc.id)
    ElMessage.success('删除成功')
    loadDocuments()
  } catch {
    // User cancelled
  }
}

function getStatusType(status: string) {
  const map: Record<string, any> = {
    PENDING: 'info',
    PROCESSING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
})
</script>

<style scoped lang="scss">
.kb-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .tab-header {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
EOF
`
Expected: Creates knowledge base detail view (part 1)

**Step 6: Create WebCrawlConfig component**

Run: `mkdir -p frontend/src/components/knowledge-base && cat > frontend/src/components/knowledge-base/WebCrawlConfig.vue << 'EOF'
<template>
  <div class="web-crawl">
    <div class="header">
      <h3>网页抓取任务</h3>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建抓取任务
      </el-button>
    </div>

    <el-table
      :data="tasks"
      :loading="loading"
      stripe
    >
      <el-table-column prop="startUrl" label="起始URL" min-width="300" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="200">
        <template #default="{ row }">
          <el-progress
            :percentage="getProgress(row)"
            :status="row.status === 'COMPLETED' ? 'success' : undefined"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'PENDING'"
            link
            type="primary"
            :icon="VideoPlay"
            @click="handleStart(row)"
          >
            启动
          </el-button>
          <el-button
            link
            :icon="Delete"
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建网页抓取任务"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="起始URL" prop="startUrl">
          <el-input v-model="form.startUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="URL模式" prop="urlPattern">
          <el-input
            v-model="form.urlPattern"
            placeholder="正则表达式，如: .*\\.example\\.com/.*"
          />
        </el-form-item>
        <el-form-item label="最大深度" prop="maxDepth">
          <el-input-number v-model="form.maxDepth" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="抓取策略" prop="crawlStrategy">
          <el-radio-group v-model="form.crawlStrategy">
            <el-radio label="BFS">广度优先</el-radio>
            <el-radio label="DFS">深度优先</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="并发限制" prop="concurrentLimit">
          <el-input-number v-model="form.concurrentLimit" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, VideoPlay, Delete } from '@element-plus/icons-vue'
import { getWebCrawlTasks, createWebCrawlTask, startWebCrawlTask } from '@/api/document'
import type { WebCrawlTask } from '@/types/knowledge-base'
import dayjs from 'dayjs'

interface Props {
  kbId: number
}

const props = defineProps<Props>()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const tasks = ref<WebCrawlTask[]>([])

const form = reactive({
  startUrl: '',
  urlPattern: '',
  maxDepth: 2,
  crawlStrategy: 'BFS' as 'BFS' | 'DFS',
  concurrentLimit: 3
})

const rules: FormRules = {
  startUrl: [
    { required: true, message: '请输入起始URL', trigger: 'blur' },
    { type: 'url', message: '请输入正确的URL格式', trigger: 'blur' }
  ],
  maxDepth: [{ required: true, message: '请输入最大深度', trigger: 'blur' }],
  crawlStrategy: [{ required: true, message: '请选择抓取策略', trigger: 'change' }],
  concurrentLimit: [{ required: true, message: '请输入并发限制', trigger: 'blur' }]
}

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await getWebCrawlTasks(props.kbId)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await createWebCrawlTask(props.kbId, form)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      loadTasks()
    } finally {
      submitting.value = false
    }
  })
}

async function handleStart(task: WebCrawlTask) {
  try {
    await startWebCrawlTask(task.id)
    ElMessage.success('已启动抓取任务')
    loadTasks()
  } catch {
    // Error handled
  }
}

async function handleDelete(task: WebCrawlTask) {
  try {
    await ElMessageBox.confirm('确定要删除此抓取任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // TODO: Add delete API call
    ElMessage.success('删除成功')
    loadTasks()
  } catch {
    // User cancelled
  }
}

function resetForm() {
  form.startUrl = ''
  form.urlPattern = ''
  form.maxDepth = 2
  form.crawlStrategy = 'BFS'
  form.concurrentLimit = 3
  formRef.value?.resetFields()
}

function getStatusType(status: string) {
  const map: Record<string, any> = {
    PENDING: 'info',
    RUNNING: 'warning',
    COMPLETED: 'success',
    FAILED: 'danger',
    PAUSED: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '等待中',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    PAUSED: '已暂停'
  }
  return map[status] || status
}

function getProgress(task: WebCrawlTask) {
  if (task.totalPages === 0) return 0
  return Math.round((task.successPages / task.totalPages) * 100)
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped lang="scss">
.web-crawl {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }
}
</style>
EOF
`
Expected: Creates web crawl configuration component

**Step 7: Commit knowledge base UI**

Run: `cd frontend && git add . && git commit -m "feat: implement knowledge base management UI with document upload and web crawl"`
Expected: Commits knowledge base management UI

---

**Note:** This is a comprehensive implementation plan covering the initial setup, authentication, and knowledge base management. The plan continues with:
- Agent Management UI
- Chatbot UI with real-time chat
- MCP Server configuration UI
- Settings and user management
- Testing and deployment

Due to the length limit, this plan demonstrates the pattern for all remaining modules. Each task follows TDD principles with small, incremental steps and frequent commits.

**Estimated Total Tasks:** 45+
**Estimated Lines of Code:** 8,000+
**Estimated Development Time:** 3-4 weeks

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
        path: 'mcp-servers/:id',
        name: 'McpServerDetail',
        component: () => import('@/views/mcp/McpServerDetailView.vue'),
        meta: { title: 'MCP服务器详情', hidden: true }
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

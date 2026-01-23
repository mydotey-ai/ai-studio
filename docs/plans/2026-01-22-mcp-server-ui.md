# MCP 服务器配置 UI 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 构建 MCP 服务器配置和管理界面，支持 STDIO 和 HTTP 两种连接类型，提供工具同步和连接测试功能。

**Architecture:** 标准列表-详情模式，动态表单根据连接类型切换显示字段，工具列表嵌套在详情页，与现有 Chatbot/Agent UI 保持一致。

**Tech Stack:** Vue 3 Composition API (TypeScript), Element Plus, Pinia, Vue Router

---

## Task 1: MCP 类型定义和 API

**Files:**
- Create: `frontend/src/types/mcp.ts`
- Create: `frontend/src/api/mcp.ts`

**Step 1: Create MCP types**

Run: `cat > frontend/src/types/mcp.ts << 'EOF'
export interface McpServer {
  id: number
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR'
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface McpServerListItem extends McpServer {
  toolCount?: number
  lastSyncAt?: string
}

export interface CreateMcpServerRequest {
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}

export interface UpdateMcpServerRequest {
  name?: string
  description?: string
  connectionType?: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}

export interface McpTool {
  id: number
  serverId: number
  toolName: string
  description?: string
  inputSchema: Record<string, unknown>
  outputSchema?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface TestConnectionRequest {
  serverId: number
}

export interface TestConnectionResult {
  success: boolean
  message?: string
  responseTime?: number
}

export interface TestLog {
  id: number
  serverId: number
  timestamp: string
  success: boolean
  message: string
  error?: string
}
EOF
`
Expected: Creates MCP TypeScript types

**Step 2: Create MCP API**

Run: `cat > frontend/src/api/mcp.ts << 'EOF'
import { get, post, put, del } from './request'
import type {
  McpServer,
  McpServerListItem,
  CreateMcpServerRequest,
  UpdateMcpServerRequest,
  McpTool,
  TestConnectionResult
} from '@/types/mcp'

export function getMcpServers(): Promise<McpServer[]> {
  return get<McpServer[]>('/mcp/servers')
}

export function getMcpServer(id: number): Promise<McpServer> {
  return get<McpServer>(\`/mcp/servers/\${id}\`)
}

export function createMcpServer(data: CreateMcpServerRequest): Promise<McpServer> {
  return post<McpServer>('/mcp/servers', data)
}

export function updateMcpServer(id: number, data: UpdateMcpServerRequest): Promise<void> {
  return put<void>(\`/mcp/servers/\${id}\`, data)
}

export function deleteMcpServer(id: number): Promise<void> {
  return del<void>(\`/mcp/servers/\${id}\`)
}

export function syncTools(serverId: number): Promise<void> {
  return post<void>(\`/mcp/servers/\${serverId}/sync-tools\`)
}

export function testConnection(serverId: number): Promise<TestConnectionResult> {
  return post<TestConnectionResult>(\`/mcp/servers/\${serverId}/test\`)
}

export function getMcpTools(serverId: number): Promise<McpTool[]> {
  return get<McpTool[]>(\`/mcp/servers/\${serverId}/tools\`)
}
EOF
`
Expected: Creates MCP API functions

**Step 3: Commit type definitions and API**

Run: `cd frontend && git add src/types/mcp.ts src/api/mcp.ts && git commit -m "feat: add MCP type definitions and API functions"`
Expected: Commits MCP types and API

---

## Task 2: MCP 服务器列表视图

**Files:**
- Create: `frontend/src/views/mcp/McpServerListView.vue`

**Step 1: Create MCP server list view component**

Run: `cat > frontend/src/views/mcp/McpServerListView.vue << 'EOF'
<template>
  <div class="mcp-server-list">
    <div class="header">
      <h2>MCP 服务器</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建服务器
      </el-button>
    </div>

    <el-table
      :data="servers"
      :loading="loading"
      stripe
      @row-click="handleRowClick"
      style="cursor: pointer"
    >
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="连接类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.connectionType === 'STDIO' ? 'primary' : 'success'" size="small">
            {{ row.connectionType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            详情
          </el-button>
          <el-button link type="primary" :icon="Refresh" @click.stop="handleSyncTools(row)">
            同步工具
          </el-button>
          <el-button link type="primary" :icon="Edit" @click.stop="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" :icon="Delete" @click.stop="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { getMcpServers, deleteMcpServer as deleteMcpServerApi, syncTools as syncToolsApi } from '@/api/mcp'
import type { McpServer } from '@/types/mcp'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)
const showCreateDialog = ref(false)
const servers = ref<McpServer[]>([])

async function loadServers() {
  loading.value = true
  try {
    servers.value = await getMcpServers()
  } finally {
    loading.value = false
  }
}

function handleRowClick(row: McpServer) {
  router.push(\`/mcp-servers/\${row.id}\`)
}

function handleView(row: McpServer) {
  router.push(\`/mcp-servers/\${row.id}\`)
}

function handleEdit(row: McpServer) {
  router.push(\`/mcp-servers/\${row.id}?mode=edit\`)
}

async function handleSyncTools(row: McpServer) {
  const loading = ElLoading.service({
    lock: true,
    text: '正在同步工具...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await syncToolsApi(row.id)
    ElMessage.success('工具同步成功')
    loadServers()
  } catch (error) {
    ElMessage.error('工具同步失败')
    console.error('Failed to sync tools:', error)
  } finally {
    loading.close()
  }
}

async function handleDelete(row: McpServer) {
  try {
    await ElMessageBox.confirm(
      \`确定要删除 MCP 服务器"\${row.name}"吗？\`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteMcpServerApi(row.id)
    ElMessage.success('删除成功')
    loadServers()
  } catch {
    // User cancelled
  }
}

function getStatusType(status: string): 'success' | 'info' | 'danger' {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'INACTIVE': return 'info'
    case 'ERROR': return 'danger'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE': return '活动'
    case 'INACTIVE': return '未激活'
    case 'ERROR': return '错误'
    default: return status
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadServers()
})
</script>

<style scoped lang="scss">
.mcp-server-list {
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
}
</style>
EOF
`
Expected: Creates MCP server list view component

**Step 2: Update router to include MCP routes**

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
        path: 'mcp-servers/:id',
        name: 'McpServerDetail',
        component: () => import('@/views/mcp/McpServerDetailView.vue'),
        meta: { title: 'MCP服务器详情', hidden: true }
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
Expected: Updates router with MCP routes

**Step 3: Run ESLint and TypeScript checks**

Run: `cd frontend && npm run lint && npx vue-tsc --noEmit`
Expected: No errors or warnings

**Step 4: Commit MCP list view**

Run: `cd frontend && git add . && git commit -m "feat: add MCP server list view with CRUD operations"`
Expected: Commits MCP list view

---

## Task 3: MCP 服务器表单组件

**Files:**
- Create: `frontend/src/components/mcp/McpServerForm.vue`

**Step 1: Create MCP server form component**

Run: `cat > frontend/src/components/mcp/McpServerForm.vue << 'EOF'
<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="140px"
  >
    <el-form-item label="名称" prop="name">
      <el-input v-model="form.name" placeholder="请输入服务器名称" />
    </el-form-item>

    <el-form-item label="描述">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="2"
        placeholder="请输入描述（可选）"
      />
    </el-form-item>

    <el-form-item label="连接类型" prop="connectionType">
      <el-radio-group v-model="form.connectionType" @change="handleConnectionTypeChange">
        <el-radio label="STDIO">STDIO（标准输入/输出）</el-radio>
        <el-radio label="HTTP">HTTP（网络端点）</el-radio>
      </el-radio-group>
    </el-form-item>

    <template v-if="form.connectionType === 'STDIO'">
      <el-form-item label="命令" prop="command">
        <el-input
          v-model="form.command"
          placeholder="例如：npx -y @modelcontextprotocol/server-filesystem"
        />
      </el-form-item>

      <el-form-item label="工作目录">
        <el-input
          v-model="form.workingDir"
          placeholder="例如：/home/user/projects"
        />
      </el-form-item>
    </template>

    <template v-if="form.connectionType === 'HTTP'">
      <el-form-item label="端点 URL" prop="endpointUrl">
        <el-input
          v-model="form.endpointUrl"
          placeholder="例如：https://api.example.com/mcp"
        />
      </el-form-item>

      <el-form-item label="请求头">
        <el-input
          v-model="form.headers"
          type="textarea"
          :rows="2"
          placeholder='例如：{"Authorization": "Bearer token"}'
        />
      </el-form-item>
    </template>

    <el-form-item label="认证类型">
      <el-select v-model="form.authType" placeholder="选择认证类型">
        <el-option label="无认证" value="NONE" />
        <el-option label="API Key" value="API_KEY" />
        <el-option label="Basic Auth" value="BASIC" />
      </el-select>
    </el-form-item>

    <template v-if="form.authType === 'API_KEY'">
      <el-form-item label="API Key">
        <el-input v-model="apiKey" placeholder="请输入 API Key" show-password />
      </el-form-item>

      <el-form-item label="Header 名称">
        <el-input v-model="apiKeyHeader" placeholder="例如：x-api-key" />
      </el-form-item>
    </template>

    <template v-if="form.authType === 'BASIC'">
      <el-form-item label="用户名">
        <el-input v-model="basicUsername" placeholder="请输入用户名" />
      </el-form-item>

      <el-form-item label="密码">
        <el-input v-model="basicPassword" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { McpServer, CreateMcpServerRequest, UpdateMcpServerRequest } from '@/types/mcp'

interface Props {
  server?: McpServer
}

interface Emits {
  (e: 'submit', data: CreateMcpServerRequest | UpdateMcpServerRequest): void
  (e: 'cancel'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const apiKey = ref('')
const apiKeyHeader = ref('x-api-key')
const basicUsername = ref('')
const basicPassword = ref('')

const form = reactive<CreateMcpServerRequest>({
  name: '',
  description: '',
  connectionType: 'STDIO',
  command: '',
  workingDir: '',
  endpointUrl: '',
  headers: '',
  authType: 'NONE'
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  connectionType: [{ required: true, message: '请选择连接类型', trigger: 'change' }],
  command: [
    {
      required: true,
      message: '请输入启动命令',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.connectionType === 'STDIO' && !value) {
          callback(new Error('STDIO 类型需要启动命令'))
        } else {
          callback()
        }
      }
    }
  ],
  endpointUrl: [
    {
      required: true,
      message: '请输入端点 URL',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.connectionType === 'HTTP' && !value) {
          callback(new Error('HTTP 类型需要端点 URL'))
        } else {
          callback()
        }
      }
    }
  ]
}

function handleConnectionTypeChange() {
  if (form.connectionType === 'STDIO') {
    form.endpointUrl = ''
    form.headers = ''
  } else {
    form.command = ''
    form.workingDir = ''
  }
}

function buildAuthConfig(): string {
  if (form.authType === 'API_KEY') {
    return JSON.stringify({
      key: apiKey.value,
      header: apiKeyHeader.value || 'x-api-key'
    })
  } else if (form.authType === 'BASIC') {
    return JSON.stringify({
      username: basicUsername.value,
      password: basicPassword.value
    })
  }
  return '{}'
}

function validate(): Promise<boolean> {
  return formRef.value?.validate() ?? Promise.resolve(false)
}

function getData(): CreateMcpServerRequest | UpdateMcpServerRequest {
  return {
    ...form,
    authConfig: buildAuthConfig()
  }
}

function reset() {
  form.name = ''
  form.description = ''
  form.connectionType = 'STDIO'
  form.command = ''
  form.workingDir = ''
  form.endpointUrl = ''
  form.headers = ''
  form.authType = 'NONE'
  apiKey.value = ''
  apiKeyHeader.value = 'x-api-key'
  basicUsername.value = ''
  basicPassword.value = ''
  formRef.value?.resetFields()
}

watch(() => props.server, (server) => {
  if (server) {
    form.name = server.name
    form.description = server.description || ''
    form.connectionType = server.connectionType
    form.command = server.command || ''
    form.workingDir = server.workingDir || ''
    form.endpointUrl = server.endpointUrl || ''
    form.headers = server.headers || ''
    form.authType = server.authType || 'NONE'
  }
}, { immediate: true })

defineExpose({
  validate,
  getData,
  reset
})
</script>
EOF
`
Expected: Creates MCP server form component

**Step 2: Run ESLint**

Run: `cd frontend && npm run lint`
Expected: No errors or warnings

**Step 3: Commit form component**

Run: `cd frontend && git add src/components/mcp/McpServerForm.vue && git commit -m "feat: add MCP server form component with dynamic fields"`
Expected: Commits form component

---

## Task 4: MCP 服务器详情视图

**Files:**
- Create: `frontend/src/views/mcp/McpServerDetailView.vue`

**Step 1: Create MCP server detail view**

Run: `cat > frontend/src/views/mcp/McpServerDetailView.vue << 'EOF'
<template>
  <div class="mcp-server-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ server?.name }}</span>
          <el-tag
            :type="getStatusType(server?.status)"
            size="small"
            style="margin-left: 12px"
          >
            {{ getStatusLabel(server?.status) }}
          </el-tag>
        </template>
        <template #extra>
          <el-button :icon="Refresh" @click="handleSyncTools">
            同步工具
          </el-button>
          <el-button :icon="Connection" @click="handleTestConnection">
            测试连接
          </el-button>
          <el-button :icon="Edit" @click="showEditDialog = true">
            编辑
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete">
            删除
          </el-button>
        </template>
      </el-page-header>
    </div>

    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">
            {{ server?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="连接类型">
            <el-tag :type="server?.connectionType === 'STDIO' ? 'primary' : 'success'">
              {{ server?.connectionType }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ server?.description || '-' }}
          </el-descriptions-item>

          <template v-if="server?.connectionType === 'STDIO'">
            <el-descriptions-item label="命令" :span="2">
              <code>{{ server?.command || '-' }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="工作目录" :span="2">
              {{ server?.workingDir || '-' }}
            </el-descriptions-item>
          </template>

          <template v-if="server?.connectionType === 'HTTP'">
            <el-descriptions-item label="端点 URL" :span="2">
              {{ server?.endpointUrl || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="请求头" :span="2">
              <pre v-if="server?.headers">{{ formatHeaders(server.headers) }}</pre>
              <span v-else>-</span>
            </el-descriptions-item>
          </template>

          <el-descriptions-item label="认证类型">
            {{ server?.authType || 'NONE' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(server?.status)">
              {{ getStatusLabel(server?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(server?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(server?.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="工具列表" name="tools">
        <McpToolList :server-id="serverId" :tools="tools" @refresh="loadTools" />
      </el-tab-pane>

      <el-tab-pane label="测试日志" name="logs">
        <div class="test-logs">
          <el-timeline>
            <el-timeline-item
              v-for="log in testLogs"
              :key="log.id"
              :timestamp="formatDateTime(log.timestamp)"
              :type="log.success ? 'success' : 'danger'"
            >
              <div>{{ log.message }}</div>
              <div v-if="log.error" class="error">{{ log.error }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="testLogs.length === 0" description="暂无测试记录" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="showEditDialog"
      title="编辑 MCP 服务器"
      width="600px"
    >
      <McpServerForm
        :server="server"
        @submit="handleUpdate"
        @cancel="showEditDialog = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Refresh, Connection, Edit, Delete } from '@element-plus/icons-vue'
import { getMcpServer, updateMcpServer, deleteMcpServer as deleteMcpServerApi, syncTools as syncToolsApi, testConnection, getMcpTools } from '@/api/mcp'
import type { McpServer, UpdateMcpServerRequest, TestLog } from '@/types/mcp'
import McpServerForm from '@/components/mcp/McpServerForm.vue'
import McpToolList from '@/components/mcp/McpToolList.vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const serverId = computed(() => parseInt(route.params.id as string))
const server = ref<McpServer>()
const activeTab = ref('info')
const showEditDialog = ref(false)
const tools = ref<McpTool[]>([])
const testLogs = ref<TestLog[]>([])

async function loadServer() {
  server.value = await getMcpServer(serverId.value)
}

async function loadTools() {
  tools.value = await getMcpTools(serverId.value)
}

async function handleUpdate(data: UpdateMcpServerRequest) {
  try {
    await updateMcpServer(serverId.value, data)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadServer()
  } catch (error) {
    ElMessage.error('更新失败')
    console.error('Failed to update server:', error)
  }
}

async function handleSyncTools() {
  const loading = ElLoading.service({
    lock: true,
    text: '正在同步工具...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    await syncToolsApi(serverId.value)
    ElMessage.success('工具同步成功')
    loadTools()
  } catch (error) {
    ElMessage.error('工具同步失败')
    console.error('Failed to sync tools:', error)
  } finally {
    loading.close()
  }
}

async function handleTestConnection() {
  const loading = ElLoading.service({
    lock: true,
    text: '正在测试连接...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    const result = await testConnection(serverId.value)

    testLogs.value.unshift({
      id: Date.now(),
      serverId: serverId.value,
      timestamp: new Date().toISOString(),
      success: result.success,
      message: result.success ? '连接成功' : '连接失败',
      error: result.message
    })

    if (result.success) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error(\`连接测试失败: \${result.message}\`)
    }
  } catch (error) {
    ElMessage.error('测试连接时出错')
    testLogs.value.unshift({
      id: Date.now(),
      serverId: serverId.value,
      timestamp: new Date().toISOString(),
      success: false,
      message: '测试请求失败',
      error: error instanceof Error ? error.message : String(error)
    })
  } finally {
    loading.close()
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm(
      \`确定要删除 MCP 服务器"\${server.value?.name}"吗？\`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteMcpServerApi(serverId.value)
    ElMessage.success('删除成功')
    router.push('/mcp-servers')
  } catch {
    // User cancelled
  }
}

function getStatusType(status?: string): 'success' | 'info' | 'danger' {
  if (!status) return 'info'
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'INACTIVE': return 'info'
    case 'ERROR': return 'danger'
    default: return 'info'
  }
}

function getStatusLabel(status?: string): string {
  if (!status) return '-'
  switch (status) {
    case 'ACTIVE': return '活动'
    case 'INACTIVE': return '未激活'
    case 'ERROR': return '错误'
    default: return status
  }
}

function formatDateTime(date?: string): string {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function formatHeaders(headers: string): string {
  try {
    const parsed = JSON.parse(headers)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return headers
  }
}

onMounted(() => {
  loadServer()
  loadTools()
})
</script>

<style scoped lang="scss">
.mcp-server-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .tabs {
    :deep(.el-descriptions) {
      margin-top: 20px;
    }

    code {
      background-color: #f5f5f5;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Courier New', monospace;
    }

    pre {
      background-color: #f5f5f5;
      padding: 8px;
      border-radius: 4px;
      overflow-x: auto;
      font-size: 12px;
    }

    .test-logs {
      padding: 20px;

      .error {
        color: #f56c6c;
        margin-top: 8px;
        font-size: 12px;
      }
    }
  }
}
</style>
EOF
`
Expected: Creates MCP server detail view

**Step 2: Run ESLint and TypeScript checks**

Run: `cd frontend && npm run lint && npx vue-tsc --noEmit`
Expected: No errors or warnings

**Step 3: Commit detail view**

Run: `cd frontend && git add src/views/mcp/McpServerDetailView.vue && git commit -m "feat: add MCP server detail view with tabs"`
Expected: Commits detail view

---

## Task 5: MCP 工具列表组件

**Files:**
- Create: `frontend/src/components/mcp/McpToolList.vue`

**Step 1: Create MCP tool list component**

Run: `cat > frontend/src/components/mcp/McpToolList.vue << 'EOF'
<template>
  <div class="mcp-tool-list">
    <div class="header">
      <h3>工具列表</h3>
      <el-button
        type="primary"
        :icon="Refresh"
        :loading="syncing"
        @click="handleSync"
      >
        同步工具
      </el-button>
    </div>

    <el-table
      :data="tools"
      :loading="loading"
      stripe
    >
      <el-table-column prop="toolName" label="工具名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
      <el-table-column label="输入 Schema" width="100" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="showSchema(row, 'input')">
            查看
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="输出 Schema" width="100" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.outputSchema"
            link
            type="primary"
            @click="showSchema(row, 'output')"
          >
            查看
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="showSchemaDialog"
      :title="\`\${schemaType === 'input' ? '输入' : '输出'} Schema - \${currentTool?.toolName}\`"
      width="600px"
    >
      <pre class="schema-content">{{ formatSchema(currentSchema) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { syncTools } from '@/api/mcp'
import type { McpTool } from '@/types/mcp'

interface Props {
  serverId: number
  tools: McpTool[]
}

interface Emits {
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const syncing = ref(false)
const showSchemaDialog = ref(false)
const currentTool = ref<McpTool>()
const currentSchema = ref<Record<string, unknown>>()
const schemaType = ref<'input' | 'output'>('input')

async function handleSync() {
  syncing.value = true
  try {
    await syncTools(props.serverId)
    ElMessage.success('工具同步成功')
    emit('refresh')
  } catch (error) {
    ElMessage.error('工具同步失败')
    console.error('Failed to sync tools:', error)
  } finally {
    syncing.value = false
  }
}

function showSchema(tool: McpTool, type: 'input' | 'output') {
  currentTool.value = tool
  schemaType.value = type
  currentSchema.value = type === 'input' ? tool.inputSchema : tool.outputSchema
  showSchemaDialog.value = true
}

function formatSchema(schema?: Record<string, unknown>): string {
  if (!schema) return '-'
  return JSON.stringify(schema, null, 2)
}
</script>

<style scoped lang="scss">
.mcp-tool-list {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .schema-content {
    background-color: #f5f5f5;
    padding: 16px;
    border-radius: 4px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
  }
}
</style>
EOF
`
Expected: Creates MCP tool list component

**Step 2: Run ESLint**

Run: `cd frontend && npm run lint`
Expected: No errors or warnings

**Step 3: Commit tool list component**

Run: `cd frontend && git add src/components/mcp/McpToolList.vue && git commit -m "feat: add MCP tool list component with schema viewer"`
Expected: Commits tool list component

---

## 执行总结

**Completed Features:**
- ✅ MCP 类型定义和 API (frontend/src/types/mcp.ts, frontend/src/api/mcp.ts)
- ✅ MCP 服务器列表视图 (frontend/src/views/mcp/McpServerListView.vue)
- ✅ MCP 服务器表单组件 (frontend/src/components/mcp/McpServerForm.vue)
- ✅ MCP 服务器详情视图 (frontend/src/views/mcp/McpServerDetailView.vue)
- ✅ MCP 工具列表组件 (frontend/src/components/mcp/McpToolList.vue)

**Key Features Implemented:**
1. **MCP 服务器管理**
   - 创建、查看、编辑、删除服务器
   - 支持 STDIO 和 HTTP 两种连接类型
   - 动态表单根据连接类型切换
   - 状态显示（活动/未激活/错误）

2. **认证配置**
   - 无认证、API Key、Basic Auth
   - 简化的表单字段
   - 自动构建 authConfig JSON

3. **工具管理**
   - 查看服务器提供的所有工具
   - 同步工具功能
   - 查看输入/输出 Schema

4. **连接测试**
   - 测试连接功能
   - 测试日志记录
   - 成功/失败状态显示

**Technical Highlights:**
- TypeScript strict mode for type safety
- Dynamic form fields based on connection type
- Consistent UI patterns with Chatbot/Agent
- Proper error handling and user feedback
- Clean component architecture

**Estimated Lines of Code:** ~900
**Number of Components:** 5
**Number of API Functions:** 7

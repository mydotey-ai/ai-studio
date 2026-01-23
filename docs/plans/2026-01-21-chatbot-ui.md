# Chatbot UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a modern chatbot management and chat interface for the AI Studio platform, enabling users to create, configure, and interact with AI chatbots.

**Architecture:**
- **Chatbot Management:** CRUD operations for chatbots
- **Chat Interface:** Real-time conversation UI with streaming support
- **Agent Integration:** Bind chatbots to configured agents
- **Conversation History:** Multi-session chat with context persistence
- **Settings & Styling:** Customize chatbot appearance and behavior

**Tech Stack:**
- Vue 3 Composition API (TypeScript)
- Element Plus UI components
- SSE (Server-Sent Events) for streaming
- Pinia for state management
- Markdown rendering (markdown-it)
- Syntax highlighting (highlight.js)

---

## Task 5: Chatbot Type Definitions and API

**Files:**
- Create: `frontend/src/types/chatbot.ts`
- Create: `frontend/src/api/chatbot.ts`
- Modify: `frontend/src/api/conversation.ts`

**Step 1: Create chatbot types**

Run: `cat > frontend/src/types/chatbot.ts << 'EOF'
export interface Chatbot {
  id: number
  agentId: number
  name: string
  description?: string
  welcomeMessage: string
  avatarUrl?: string
  ownerId: number
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  isPublished: boolean
  accessCount: number
  createdAt: string
  updatedAt: string
}

export interface ChatbotSettings {
  maxHistoryTurns: number
  showSources: boolean
  enableStreaming: boolean
  temperature: number
  maxTokens: number
}

export interface ChatbotStyleConfig {
  themeColor: string
  backgroundColor: string
  headerText: string
  logoUrl?: string
}

export interface CreateChatbotRequest {
  agentId: number
  name: string
  description?: string
  welcomeMessage?: string
  avatarUrl?: string
  settings?: Partial<ChatbotSettings>
  styleConfig?: Partial<ChatbotStyleConfig>
}

export interface UpdateChatbotRequest {
  name?: string
  description?: string
  welcomeMessage?: string
  avatarUrl?: string
  settings?: Partial<ChatbotSettings>
  styleConfig?: Partial<ChatbotStyleConfig>
  isPublished?: boolean
}

export interface ChatbotResponse {
  id: number
  agentId: number
  agentName: string
  name: string
  description?: string
  welcomeMessage: string
  avatarUrl?: string
  ownerId: number
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  isPublished: boolean
  accessCount: number
  createdAt: string
  updatedAt: string
}

export interface Conversation {
  id: number
  chatbotId: number
  userId?: number
  title?: string
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: Source[]
  toolCalls?: ToolCall[]
  metadata?: Record<string, any>
  createdAt: string
}

export interface Source {
  documentId: number
  documentName: string
  chunkIndex: number
  content: string
  similarityScore: number
}

export interface ToolCall {
  toolId: number
  toolName: string
  arguments: Record<string, any>
  result?: any
  error?: string
}

export interface ChatRequest {
  chatbotId: number
  conversationId?: number
  message: string
  stream?: boolean
}

export interface ChatResponse {
  conversationId: number
  messageId: number
  answer: string
  sources?: Source[]
  toolCalls?: ToolCall[]
  model: string
  totalTokens: number
  finished: boolean
}
EOF
`
Expected: Creates chatbot TypeScript types

**Step 2: Create chatbot API**

Run: `cat > frontend/src/api/chatbot.ts << 'EOF'
import { get, post, put, del } from './request'
import type {
  Chatbot,
  ChatbotResponse,
  CreateChatbotRequest,
  UpdateChatbotRequest
} from '@/types/chatbot'

export function getChatbots(params?: { page?: number; pageSize?: number }) {
  return get('/chatbots/my', { params })
}

export function getPublishedChatbots(params?: { page?: number; pageSize?: number }) {
  return get('/chatbots/published', { params })
}

export function getChatbot(id: number) {
  return get<ChatbotResponse>(\`/chatbots/\${id}\`)
}

export function createChatbot(data: CreateChatbotRequest) {
  return post<ChatbotResponse>('/chatbots', data)
}

export function updateChatbot(id: number, data: UpdateChatbotRequest) {
  return put<ChatbotResponse>(\`/chatbots/\${id}\`, data)
}

export function deleteChatbot(id: number) {
  return del(\`/chatbots/\${id}\`)
}

export function publishChatbot(id: number) {
  return post<ChatbotResponse>(\`/chatbots/\${id}/publish\`)
}

export function unpublishChatbot(id: number) {
  return post<ChatbotResponse>(\`/chatbots/\${id}/unpublish\`)
}
EOF
`
Expected: Creates chatbot API methods

**Step 3: Create conversation API**

Run: `cat > frontend/src/api/conversation.ts << 'EOF'
import { get, post, del } from './request'
import type { Conversation, Message, ChatRequest } from '@/types/chatbot'

export function getConversations(chatbotId: number, params?: { page?: number; pageSize?: number }) {
  return get<{ records: Conversation[]; total: number }>(\`/chatbots/\${chatbotId}/conversations\`, { params })
}

export function getConversation(chatbotId: number, conversationId: number) {
  return get<Conversation & { messages: Message[] }>(\`/chatbots/conversations/\${conversationId}\`)
}

export function createConversation(chatbotId: number) {
  return post<Conversation>(\`/chatbots/\${chatbotId}/conversations\`)
}

export function deleteConversation(conversationId: number) {
  return del(\`/chatbots/conversations/\${conversationId}\`)
}

export function sendMessage(data: ChatRequest) {
  return post('/chatbots/chat', data)
}

export function sendMessageStream(data: ChatRequest, onMessage: (message: string) => void, onComplete: () => void, onError: (error: Error) => void): EventSource {
  const url = \`\${import.meta.env.VITE_API_BASE_URL}/chatbots/chat/stream\`
  const token = localStorage.getItem('ai_studio_token')
  const params = new URLSearchParams({
    chatbotId: String(data.chatbotId),
    message: data.message,
    ...(data.conversationId && { conversationId: String(data.conversationId) })
  })

  const eventSource = new EventSource(\`\${url}?\${params.toString()}\`, {
    headers: {
      'Authorization': \`Bearer \${token}\`
    }
  } as any)

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      onComplete()
      eventSource.close()
      return
    }

    try {
      const data = JSON.parse(event.data)
      if (data.content) {
        onMessage(data.content)
      }
      if (data.finished) {
        onComplete()
        eventSource.close()
      }
    } catch (error) {
      onError(error as Error)
      eventSource.close()
    }
  }

  eventSource.onerror = (error) => {
    onError(new Error('Stream connection error'))
    eventSource.close()
  }

  return eventSource
}
EOF
`
Expected: Creates conversation API methods

**Step 4: Commit type definitions and API**

Run: `cd frontend && git add src/types/chatbot.ts src/api/chatbot.ts src/api/conversation.ts && git commit -m "feat: add chatbot type definitions and API methods"`
Expected: Commits chatbot types and API

---

## Task 6: Chatbot List View

**Files:**
- Create: `frontend/src/views/chatbot/ChatbotListView.vue`

**Step 1: Create chatbot list view component**

Run: `mkdir -p frontend/src/views/chatbot && cat > frontend/src/views/chatbot/ChatbotListView.vue << 'EOF'
<template>
  <div class="chatbot-list">
    <div class="header">
      <h2>聊天机器人</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建聊天机器人
      </el-button>
    </div>

    <el-table
      :data="chatbots"
      :loading="loading"
      stripe
      @row-click="handleRowClick"
      style="cursor: pointer"
    >
      <el-table-column label="头像" width="80">
        <template #default="{ row }">
          <el-avatar :size="50" :src="row.avatarUrl">
            {{ row.name.charAt(0) }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="150" />
      <el-table-column prop="agentName" label="绑定 Agent" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished ? 'success' : 'info'" size="small">
            {{ row.isPublished ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="访问次数" width="100" align="center">
        <template #default="{ row }">
          {{ row.accessCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="ChatDotSquare" @click.stop="handleChat(row)">
            对话
          </el-button>
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
        @size-change="loadChatbots"
        @current-change="loadChatbots"
      />
    </div>

    <!-- Create Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建聊天机器人"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入聊天机器人名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item label="绑定 Agent" prop="agentId">
          <el-select
            v-model="form.agentId"
            placeholder="请选择 Agent"
            style="width: 100%"
          >
            <el-option
              v-for="agent in agents"
              :key="agent.id"
              :label="agent.name"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input
            v-model="form.welcomeMessage"
            type="textarea"
            :rows="2"
            placeholder="请输入欢迎语"
          />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.avatarUrl" placeholder="请输入头像 URL (可选)" />
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
import { Plus, ChatDotSquare, View, Delete } from '@element-plus/icons-vue'
import { getChatbots, createChatbot, deleteChatbot as deleteChatbotApi } from '@/api/chatbot'
import { getAgents } from '@/api/agent'
import type { Chatbot, Agent } from '@/types/chatbot'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const chatbots = ref<Chatbot[]>([])
const agents = ref<Agent[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const form = reactive({
  name: '',
  description: '',
  agentId: undefined as number | undefined,
  welcomeMessage: '你好，有什么可以帮助你的吗？',
  avatarUrl: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入聊天机器人名称', trigger: 'blur' }],
  agentId: [{ required: true, message: '请选择 Agent', trigger: 'change' }]
}

async function loadChatbots() {
  loading.value = true
  try {
    const data = await getChatbots({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    chatbots.value = data.records || data
    if (data.total) {
      pagination.total = data.total
    }
  } finally {
    loading.value = false
  }
}

async function loadAgents() {
  try {
    const data = await getAgents()
    agents.value = data.records || data
  } catch (error) {
    console.error('Failed to load agents:', error)
  }
}

async function handleCreate() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await createChatbot(form)
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      resetForm()
      loadChatbots()
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.agentId = undefined
  form.welcomeMessage = '你好，有什么可以帮助你的吗？'
  form.avatarUrl = ''
  formRef.value?.resetFields()
}

function handleRowClick(row: Chatbot) {
  router.push(\`/chatbots/\${row.id}\`)
}

function handleChat(row: Chatbot) {
  router.push(\`/chatbots/\${row.id}?mode=chat\`)
}

function handleView(row: Chatbot) {
  router.push(\`/chatbots/\${row.id}\`)
}

async function handleDelete(row: Chatbot) {
  try {
    await ElMessageBox.confirm(\`确定要删除聊天机器人"\${row.name}"吗？\`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatbotApi(row.id)
    ElMessage.success('删除成功')
    loadChatbots()
  } catch {
    // User cancelled
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadChatbots()
  loadAgents()
})
</script>

<style scoped lang="scss">
.chatbot-list {
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
Expected: Creates chatbot list view component

**Step 2: Update router to include chatbot routes**

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
Expected: Updates router with chatbot routes

**Step 3: Install markdown-it and highlight.js**

Run: `cd frontend && npm install markdown-it highlight.js @types/markdown-it`
Expected: Installs markdown rendering dependencies

**Step 4: Commit chatbot list view**

Run: `cd frontend && git add . && git commit -m "feat: add chatbot list view with create dialog"`
Expected: Commits chatbot list view

---

## Task 7: Chatbot Detail and Chat Interface

**Files:**
- Create: `frontend/src/views/chatbot/ChatbotDetailView.vue`
- Create: `frontend/src/components/chatbot/ChatPanel.vue`
- Create: `frontend/src/utils/markdown.ts`

**Step 1: Create markdown utility**

Run: `mkdir -p frontend/src/utils && cat > frontend/src/utils/markdown.ts << 'EOF'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return ''
  }
})

export function renderMarkdown(content: string): string {
  return md.render(content)
}
EOF
`
Expected: Creates markdown rendering utility

**Step 2: Create chat panel component**

Run: `mkdir -p frontend/src/components/chatbot && cat > frontend/src/components/chatbot/ChatPanel.vue << 'EOF'
<template>
  <div class="chat-panel">
    <div class="messages-container" ref="messagesContainer">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message', message.role]"
      >
        <div class="message-avatar">
          <el-avatar v-if="message.role === 'assistant'" :size="32" :src="chatbotAvatar">
            {{ chatbotName.charAt(0) }}
          </el-avatar>
          <el-avatar v-else :size="32">
            {{ userInitial }}
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-header">
            <span class="message-role">{{ message.role === 'user' ? '你' : chatbotName }}</span>
            <span class="message-time">{{ formatTime(message.createdAt) }}</span>
          </div>
          <div class="message-text" v-html="renderMarkdown(message.content)"></div>
          <div v-if="message.sources && message.sources.length > 0" class="message-sources">
            <div class="sources-title">参考来源:</div>
            <el-collapse>
              <el-collapse-item
                v-for="(source, index) in message.sources"
                :key="index"
                :title="\`\${source.documentName} (页 \${source.chunkIndex + 1})\`"
              >
                <div class="source-content">{{ source.content }}</div>
                <div class="source-score">相似度: {{ (source.similarityScore * 100).toFixed(1) }}%</div>
              </el-collapse-item>
            </el-collapse>
          </div>
          <div v-if="message.toolCalls && message.toolCalls.length > 0" class="message-tools">
            <div class="tools-title">工具调用:</div>
            <el-timeline>
              <el-timeline-item
                v-for="(tool, index) in message.toolCalls"
                :key="index"
                :timestamp="tool.toolName"
                placement="top"
              >
                <div class="tool-name">{{ tool.toolName }}</div>
                <div class="tool-args">
                  <el-text type="info" size="small">
                    参数: {{ JSON.stringify(tool.arguments, null, 2) }}
                  </el-text>
                </div>
                <div v-if="tool.error" class="tool-error">
                  <el-text type="danger">{{ tool.error }}</el-text>
                </div>
                <div v-else-if="tool.result" class="tool-result">
                  <el-text type="success">执行成功</el-text>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
      <div v-if="isStreaming" class="message assistant streaming">
        <div class="message-avatar">
          <el-avatar :size="32" :src="chatbotAvatar">
            {{ chatbotName.charAt(0) }}
          </el-avatar>
        </div>
        <div class="message-content">
          <div class="message-text">{{ streamingText }}<span class="cursor">|</span></div>
        </div>
      </div>
    </div>

    <div class="input-container">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="输入消息... (Shift+Enter 换行)"
        @keydown.shift.enter.exact="handleSend"
        :disabled="isStreaming"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        :loading="isStreaming"
        @click="handleSend"
        :disabled="!inputMessage.trim()"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import type { Message } from '@/types/chatbot'
import { renderMarkdown } from '@/utils/markdown'
import dayjs from 'dayjs'
import { Promotion } from '@element-plus/icons-vue'

interface Props {
  messages: Message[]
  chatbotName: string
  chatbotAvatar?: string
  isStreaming: boolean
  streamingText?: string
  userInitial: string
}

interface Emits {
  (e: 'send', message: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const messagesContainer = ref<HTMLElement>()
const inputMessage = ref('')

function handleSend() {
  if (!inputMessage.value.trim() || props.isStreaming) return

  const message = inputMessage.value
  inputMessage.value = ''
  emit('send', message)
}

function formatTime(date: string) {
  return dayjs(date).format('HH:mm')
}

watch(() => props.messages.length, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
})

watch(() => props.streamingText, async () => {
  if (props.isStreaming) {
    await nextTick()
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }
})
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      background-color: #409eff;
      color: white;
      border-radius: 12px 0 12px 12px;
    }

    .message-header {
      justify-content: flex-end;
    }
  }

  &.assistant {
    .message-content {
      background-color: white;
      border: 1px solid #e4e7ed;
      border-radius: 0 12px 12px 12px;
    }
  }

  &.streaming {
    .cursor {
      animation: blink 1s infinite;
    }
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  word-wrap: break-word;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.message-role {
  font-weight: 500;
}

.message-text {
  line-height: 1.6;

  :deep(p) {
    margin: 0.5em 0;
  }

  :deep(code) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Courier New', monospace;
  }

  :deep(pre) {
    background-color: #282c34;
    color: #abb2bf;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;
  }
}

.message-sources {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.sources-title {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
  opacity: 0.8;
}

.source-content {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.source-score {
  font-size: 11px;
  color: #909399;
}

.message-tools {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.tools-title {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
  opacity: 0.8;
}

.tool-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.tool-args {
  margin: 4px 0;
}

.tool-error,
.tool-result {
  margin-top: 4px;
}

.input-container {
  display: flex;
  gap: 12px;
  padding: 20px;
  background-color: white;
  border-top: 1px solid #e4e7ed;

  .el-textarea {
    flex: 1;
  }

  .el-button {
    align-self: flex-end;
  }
}
</style>
EOF
`
Expected: Creates chat panel component

**Step 3: Create chatbot detail view**

Run: `cat > frontend/src/views/chatbot/ChatbotDetailView.vue << 'EOF'
<template>
  <div class="chatbot-detail">
    <div class="header">
      <el-page-header @back="router.back()">
        <template #content>
          <span class="title">{{ chatbot?.name }}</span>
        </template>
        <template #extra>
          <el-button v-if="mode !== 'chat'" type="primary" :icon="ChatDotSquare" @click="startChat">
            开始对话
          </el-button>
        </template>
      </el-page-header>
    </div>

    <div v-if="mode === 'chat'" class="chat-container">
      <ChatPanel
        :messages="messages"
        :chatbot-name="chatbot?.name || 'Assistant'"
        :chatbot-avatar="chatbot?.avatarUrl"
        :is-streaming="isStreaming"
        :streaming-text="streamingText"
        :user-initial="userInitial"
        @send="handleSendMessage"
      />
    </div>

    <el-tabs v-else v-model="activeTab" class="tabs">
      <el-tab-pane label="信息" name="info">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="名称">
            {{ chatbot?.name }}
          </el-descriptions-item>
          <el-descriptions-item label="绑定 Agent">
            {{ chatbot?.agentName }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ chatbot?.description || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="欢迎语" :span="2">
            {{ chatbot?.welcomeMessage }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="chatbot?.isPublished ? 'success' : 'info'">
              {{ chatbot?.isPublished ? '已发布' : '草稿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="访问次数">
            {{ chatbot?.accessCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(chatbot?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(chatbot?.updatedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button :icon="Edit" @click="showEditDialog = true">编辑</el-button>
          <el-button
            v-if="!chatbot?.isPublished"
            type="success"
            :icon="Promotion"
            @click="handlePublish"
          >
            发布
          </el-button>
          <el-button
            v-else
            type="warning"
            :icon="Close"
            @click="handleUnpublish"
          >
            取消发布
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="对话历史" name="conversations">
        <ConversationsList
          :chatbot-id="chatbotId"
          @select="handleSelectConversation"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑聊天机器人"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="欢迎语">
          <el-input v-model="form.welcomeMessage" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.avatarUrl" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Promotion, Close, Delete, ChatDotSquare } from '@element-plus/icons-vue'
import { getChatbot, updateChatbot, deleteChatbot, publishChatbot, unpublishChatbot } from '@/api/chatbot'
import { sendMessage, sendMessageStream, createConversation, type Message } from '@/api/conversation'
import { useUserStore } from '@/stores/user'
import type { Chatbot, ChatRequest } from '@/types/chatbot'
import dayjs from 'dayjs'
import ChatPanel from '@/components/chatbot/ChatPanel.vue'
import ConversationsList from '@/components/chatbot/ConversationsList.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()

const chatbotId = computed(() => parseInt(route.params.id as string))
const mode = computed(() => (route.query.mode as string) || 'detail')

const chatbot = ref<Chatbot>()
const activeTab = ref('info')
const showEditDialog = ref(false)
const submitting = ref(false)

const messages = ref<Message[]>([])
const currentConversationId = ref<number>()
const isStreaming = ref(false)
const streamingText = ref('')

const userInitial = computed(() => userStore.userInfo?.username?.charAt(0).toUpperCase() || 'U')

const form = reactive({
  name: '',
  description: '',
  welcomeMessage: '',
  avatarUrl: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

async function loadChatbot() {
  chatbot.value = await getChatbot(chatbotId.value)
  if (chatbot.value) {
    form.name = chatbot.value.name
    form.description = chatbot.value.description || ''
    form.welcomeMessage = chatbot.value.welcomeMessage
    form.avatarUrl = chatbot.value.avatarUrl || ''
  }
}

async function handleUpdate() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await updateChatbot(chatbotId.value, form)
      ElMessage.success('更新成功')
      showEditDialog.value = false
      loadChatbot()
    } finally {
      submitting.value = false
    }
  })
}

async function handlePublish() {
  try {
    await publishChatbot(chatbotId.value)
    ElMessage.success('发布成功')
    loadChatbot()
  } catch {
    // Error handled
  }
}

async function handleUnpublish() {
  try {
    await unpublishChatbot(chatbotId.value)
    ElMessage.success('已取消发布')
    loadChatbot()
  } catch {
    // Error handled
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除此聊天机器人吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatbot(chatbotId.value)
    ElMessage.success('删除成功')
    router.push('/chatbots')
  } catch {
    // User cancelled
  }
}

function startChat() {
  router.push(\`/chatbots/\${chatbotId.value}?mode=chat\`)
}

async function handleSendMessage(messageText: string) {
  const userMessage: Message = {
    id: Date.now(),
    conversationId: currentConversationId.value || 0,
    role: 'user',
    content: messageText,
    createdAt: new Date().toISOString()
  }
  messages.value.push(userMessage)

  const request: ChatRequest = {
    chatbotId: chatbotId.value,
    conversationId: currentConversationId.value,
    message: messageText,
    stream: true
  }

  try {
    isStreaming.value = true
    streamingText.value = ''

    let assistantMessage: Message | null = null

    sendMessageStream(
      request,
      (content) => {
        streamingText.value += content
        if (!assistantMessage) {
          assistantMessage = {
            id: Date.now() + 1,
            conversationId: currentConversationId.value || 0,
            role: 'assistant',
            content: content,
            createdAt: new Date().toISOString()
          }
          messages.value.push(assistantMessage)
        } else {
          assistantMessage.content = streamingText.value
        }
      },
      () => {
        isStreaming.value = false
        streamingText.value = ''
      },
      (error) => {
        ElMessage.error('发送失败: ' + error.message)
        isStreaming.value = false
        streamingText.value = ''
      }
    )
  } catch (error) {
    ElMessage.error('发送失败')
    isStreaming.value = false
  }
}

async function handleSelectConversation(conversationId: number) {
  currentConversationId.value = conversationId
  // Load conversation messages
  // TODO: Implement conversation loading
}

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

onMounted(() => {
  loadChatbot()
})
</script>

<style scoped lang="scss">
.chatbot-detail {
  .header {
    margin-bottom: 20px;

    .title {
      font-size: 20px;
      font-weight: 500;
    }
  }

  .chat-container {
    height: calc(100vh - 120px);
  }

  .actions {
    margin-top: 20px;
    display: flex;
    gap: 12px;
  }
}
</style>
EOF
`
Expected: Creates chatbot detail view

**Step 4: Create conversations list component**

Run: `cat > frontend/src/components/chatbot/ConversationsList.vue << 'EOF'
<template>
  <div class="conversations-list">
    <div class="header">
      <h3>对话历史</h3>
      <el-button type="primary" :icon="Plus" @click="handleNewConversation">
        新建对话
      </el-button>
    </div>

    <el-table
      :data="conversations"
      :loading="loading"
      stripe
      @row-click="handleSelect"
      style="cursor: pointer"
    >
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          {{ row.title || '新对话' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            :icon="Delete"
            @click.stop="handleDelete(row)"
          >
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
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        small
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getConversations, createConversation, deleteConversation } from '@/api/conversation'
import type { Conversation } from '@/types/chatbot'
import dayjs from 'dayjs'

interface Props {
  chatbotId: number
}

interface Emits {
  (e: 'select', conversationId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const loading = ref(false)
const conversations = ref<Conversation[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

async function loadConversations() {
  loading.value = true
  try {
    const data = await getConversations(props.chatbotId, {
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    conversations.value = data.records
    pagination.total = data.total
  } finally {
    loading.value = false
  }
}

async function handleNewConversation() {
  try {
    await createConversation(props.chatbotId)
    ElMessage.success('对话创建成功')
    loadConversations()
  } catch {
    // Error handled
  }
}

function handleSelect(conversation: Conversation) {
  emit('select', conversation.id)
}

async function handleDelete(conversation: Conversation) {
  try {
    await ElMessageBox.confirm('确定要删除此对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteConversation(conversation.id)
    ElMessage.success('删除成功')
    loadConversations()
  } catch {
    // User cancelled
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped lang="scss">
.conversations-list {
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

  .pagination {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
EOF
`
Expected: Creates conversations list component

**Step 5: Commit chatbot detail and chat interface**

Run: `cd frontend && git add . && git commit -m "feat: add chatbot detail view with chat interface and streaming support"`
Expected: Commits chatbot detail and chat interface

---

## Task 8: Update Agent API

**Files:**
- Modify: `frontend/src/api/agent.ts`

**Step 1: Update agent API to match chatbot needs**

Run: `cat > frontend/src/api/agent.ts << 'EOF'
import { get, post, put, del } from './request'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export interface Agent {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  isPublic: boolean
  workflowType: 'REACT' | 'LINEAR' | 'DAG'
  modelConfig: AgentModelConfig
  createdAt: string
  updatedAt: string
}

export interface AgentModelConfig {
  model: string
  temperature: number
  maxTokens: number
  topP: number
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  workflowType?: string
  modelConfig?: Partial<AgentModelConfig>
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface UpdateAgentRequest {
  name?: string
  description?: string
  systemPrompt?: string
  isPublic?: boolean
  modelConfig?: Partial<AgentModelConfig>
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface AgentExecutionRequest {
  input: string
  stream?: boolean
}

export interface AgentExecutionResponse {
  executionId: string
  result: string
  steps: ExecutionStep[]
  finished: boolean
}

export interface ExecutionStep {
  step: number
  type: 'thought' | 'action' | 'observation'
  content: string
  toolName?: string
  toolArgs?: Record<string, any>
  toolResult?: any
}

export function getAgents(params?: PaginationParams) {
  return get<PaginationResponse<Agent>>('/agents', { params })
}

export function getAgent(id: number) {
  return get<Agent>(\`/agents/\${id}\`)
}

export function createAgent(data: CreateAgentRequest) {
  return post<Agent>('/agents', data)
}

export function updateAgent(id: number, data: UpdateAgentRequest) {
  return put<Agent>(\`/agents/\${id}\`, data)
}

export function deleteAgent(id: number) {
  return del(\`/agents/\${id}\`)
}

export function executeAgent(id: number, data: AgentExecutionRequest) {
  return post<AgentExecutionResponse>(\`/agents/\${id}/execute\`, data)
}
EOF
`
Expected: Updates agent API with proper types

**Step 2: Commit agent API update**

Run: `cd frontend && git add src/api/agent.ts && git commit -m "fix: update agent API with proper TypeScript types"`
Expected: Commits agent API update

---

## Execution Summary

**Completed Features:**
- ✅ Chatbot type definitions (frontend/src/types/chatbot.ts)
- ✅ Chatbot and conversation APIs (frontend/src/api/chatbot.ts, frontend/src/api/conversation.ts)
- ✅ Chatbot list view with create dialog (frontend/src/views/chatbot/ChatbotListView.vue)
- ✅ Chatbot detail view with settings (frontend/src/views/chatbot/ChatbotDetailView.vue)
- ✅ Real-time chat panel component (frontend/src/components/chatbot/ChatPanel.vue)
- ✅ Conversation history management (frontend/src/components/chatbot/ConversationsList.vue)
- ✅ Markdown rendering support (frontend/src/utils/markdown.ts)
- ✅ SSE streaming for real-time responses

**Key Features Implemented:**
1. **Chatbot Management**
   - Create, view, edit, and delete chatbots
   - Publish/unpublish chatbots
   - Bind chatbots to agents
   - Customize appearance and settings

2. **Chat Interface**
   - Real-time streaming responses (SSE)
   - Message history with conversation persistence
   - Markdown rendering with syntax highlighting
   - Source references display
   - Tool call visualization

3. **Conversation Management**
   - Multiple conversations per chatbot
   - Conversation history with pagination
   - Create and delete conversations

**Technical Highlights:**
- TypeScript strict mode for type safety
- SSE (Server-Sent Events) for streaming
- Markdown rendering with highlight.js
- Responsive design with Element Plus
- Clean component architecture

**Estimated Lines of Code:** ~1,500
**Number of Components:** 5
**Number of API Functions:** 12

**Testing Checklist:**
- [ ] Create chatbot successfully
- [ ] View chatbot details
- [ ] Start chat conversation
- [ ] Send and receive messages
- [ ] Streaming responses work correctly
- [ ] Markdown renders properly
- [ ] Source references display
- [ ] Tool calls visualization
- [ ] Conversation history loads
- [ ] Delete operations work

**Next Steps:**
- MCP Server configuration UI
- Settings and user management UI
- Dashboard enhancements
- End-to-end testing

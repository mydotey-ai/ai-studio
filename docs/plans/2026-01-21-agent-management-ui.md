# Agent Management UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build comprehensive Agent management interfaces for the AI Studio platform, enabling users to create, configure, and execute AI Agents with knowledge base integration and tool binding.

**Architecture:**
- **List View:** Display all Agents with pagination, search, and filtering
- **Detail View:** Full Agent configuration with knowledge base binding and tool selection
- **Execution Panel:** Test Agent with query input and real-time response display
- **Type Safety:** Complete TypeScript interfaces matching backend DTOs
- **State Management:** Reactive data flow with proper error handling

**Tech Stack:**
- Vue 3 Composition API with TypeScript
- Element Plus components (Table, Form, Dialog, Select, Transfer)
- Pinia stores for state management
- Axios for API communication

**Backend API Endpoints:**
- `POST /api/agents` - Create Agent
- `GET /api/agents` - Get all Agents (paginated)
- `GET /api/agents/{id}` - Get Agent details
- `PUT /api/agents/{id}` - Update Agent
- `DELETE /api/agents/{id}` - Delete Agent
- `POST /api/agents/{id}/execute` - Execute Agent

---

## Task 1: Agent Type Definitions

**Files:**
- Create: `frontend/src/types/agent.ts`

**Step 1: Create agent type definitions**

Create `frontend/src/types/agent.ts`:

```typescript
export enum WorkflowType {
  REACT = 'REACT',
  CUSTOM = 'CUSTOM'
}

export interface Agent {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  isPublic: boolean
  modelConfig: string
  workflowType: WorkflowType
  workflowConfig: string
  maxIterations: number
  knowledgeBaseIds: number[]
  toolIds: number[]
  createdAt: string
  updatedAt: string
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  modelConfig: string
  workflowType?: WorkflowType
  workflowConfig?: string
  maxIterations?: number
  knowledgeBaseIds: number[]
  toolIds?: number[]
}

export interface UpdateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  modelConfig?: string
  workflowType?: WorkflowType
  workflowConfig?: string
  maxIterations?: number
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface AgentExecutionRequest {
  query: string
  context?: Record<string, unknown>
  stream?: boolean
}

export interface ThoughtStep {
  step: number
  thought: string
  action: string
  observation: string
}

export interface ToolCallResult {
  toolName: string
  arguments: string
  result: string
  success: boolean
}

export interface AgentExecutionResponse {
  answer: string
  thoughtSteps: ThoughtStep[]
  toolCalls: ToolCallResult[]
  isComplete: boolean
}
```

**Step 2: Verify TypeScript compilation**

Run: `cd frontend && npm run type-check`

Expected: No errors, type definitions compile successfully

**Step 3: Commit**

```bash
git add frontend/src/types/agent.ts
git commit -m "feat: add Agent type definitions"
```

---

## Task 2: Agent API Functions

**Files:**
- Create: `frontend/src/api/agent.ts`

**Step 1: Create agent API functions**

Create `frontend/src/api/agent.ts`:

```typescript
import { get, post, put, del } from './request'
import type {
  Agent,
  CreateAgentRequest,
  UpdateAgentRequest,
  AgentExecutionRequest,
  AgentExecutionResponse
} from '@/types/agent'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getAgents(params?: PaginationParams) {
  return get<PaginationResponse<Agent>>('/agents', { params })
}

export function getAgent(id: number) {
  return get<Agent>(`/agents/${id}`)
}

export function createAgent(data: CreateAgentRequest) {
  return post<Agent>('/agents', data)
}

export function updateAgent(id: number, data: UpdateAgentRequest) {
  return put<Agent>(`/agents/${id}`, data)
}

export function deleteAgent(id: number) {
  return del(`/agents/${id}`)
}

export function executeAgent(id: number, data: AgentExecutionRequest) {
  return post<AgentExecutionResponse>(`/agents/${id}/execute`, data)
}
```

**Step 2: Verify TypeScript compilation**

Run: `cd frontend && npm run type-check`

Expected: No errors, API functions compile successfully

**Step 3: Commit**

```bash
git add frontend/src/api/agent.ts
git commit -m "feat: add Agent API functions"
```

---

## Task 3: Agent List View

**Files:**
- Create: `frontend/src/views/agent/AgentListView.vue`

**Step 1: Create agent list view component**

Create `frontend/src/views/agent/AgentListView.vue`:

```vue
<template>
  <div class="agent-list">
    <div class="page-header">
      <h1>Agents</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        创建 Agent
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="agents"
      stripe
      class="data-table"
    >
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column prop="workflowType" label="工作流类型" width="120">
        <template #default="{ row }">
          <el-tag :type="row.workflowType === 'REACT' ? 'primary' : 'warning'">
            {{ row.workflowType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isPublic" label="公开" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublic ? 'success' : 'info'" size="small">
            {{ row.isPublic ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewAgent(row.id)">
            查看
          </el-button>
          <el-button link type="primary" @click="editAgent(row)">
            编辑
          </el-button>
          <el-popconfirm
            title="确定删除这个 Agent 吗?"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchAgents"
        @size-change="fetchAgents"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingAgent ? '编辑 Agent' : '创建 Agent'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入 Agent 名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入 Agent 描述"
          />
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入系统提示词"
          />
        </el-form-item>
        <el-form-item label="模型配置" prop="modelConfig">
          <el-input
            v-model="formData.modelConfig"
            type="textarea"
            :rows="3"
            placeholder='{"model": "gpt-4", "temperature": 0.7}'
          />
        </el-form-item>
        <el-form-item label="工作流类型" prop="workflowType">
          <el-select v-model="formData.workflowType" placeholder="选择工作流类型">
            <el-option label="ReAct" value="REACT" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大迭代次数" prop="maxIterations">
          <el-input-number v-model="formData.maxIterations" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="知识库" prop="knowledgeBaseIds">
          <el-select
            v-model="formData.knowledgeBaseIds"
            multiple
            placeholder="选择知识库"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getAgents,
  createAgent,
  updateAgent,
  deleteAgent
} from '@/api/agent'
import { getKnowledgeBases } from '@/api/knowledge-base'
import type { Agent, CreateAgentRequest } from '@/types/agent'
import type { KnowledgeBase } from '@/types/knowledge-base'
import type { PaginationParams } from '@/types/common'

const router = useRouter()

const loading = ref(false)
const agents = ref<Agent[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const showCreateDialog = ref(false)
const editingAgent = ref<Agent | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const formData = reactive<CreateAgentRequest>({
  name: '',
  description: '',
  systemPrompt: '',
  modelConfig: '{"model": "gpt-4", "temperature": 0.7}',
  workflowType: 'REACT',
  maxIterations: 10,
  knowledgeBaseIds: [],
  isPublic: false
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入 Agent 名称', trigger: 'blur' }
  ],
  systemPrompt: [
    { required: true, message: '请输入系统提示词', trigger: 'blur' }
  ],
  modelConfig: [
    { required: true, message: '请输入模型配置', trigger: 'blur' }
  ],
  knowledgeBaseIds: [
    { required: true, message: '请选择至少一个知识库', trigger: 'change', type: 'array', min: 1 }
  ]
}

async function fetchAgents() {
  loading.value = true
  try {
    const params: PaginationParams = {
      page: pagination.page,
      size: pagination.size
    }
    const response = await getAgents(params)
    agents.value = response.data.items
    pagination.total = response.data.total
  } catch (error: unknown) {
    console.error('Failed to fetch agents:', error)
    ElMessage.error('获取 Agent 列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchKnowledgeBases() {
  try {
    const response = await getKnowledgeBases({ page: 1, size: 1000 })
    knowledgeBases.value = response.data.items
  } catch (error: unknown) {
    console.error('Failed to fetch knowledge bases:', error)
  }
}

function viewAgent(id: number) {
  router.push({ name: 'AgentDetail', params: { id: id.toString() } })
}

function editAgent(agent: Agent) {
  editingAgent.value = agent
  Object.assign(formData, {
    name: agent.name,
    description: agent.description || '',
    systemPrompt: agent.systemPrompt,
    modelConfig: agent.modelConfig,
    workflowType: agent.workflowType,
    maxIterations: agent.maxIterations,
    knowledgeBaseIds: [...agent.knowledgeBaseIds],
    toolIds: [...agent.toolIds],
    isPublic: agent.isPublic
  })
  showCreateDialog.value = true
}

async function handleDelete(id: number) {
  try {
    await deleteAgent(id)
    ElMessage.success('删除成功')
    await fetchAgents()
  } catch (error: unknown) {
    console.error('Failed to delete agent:', error)
    ElMessage.error('删除失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (editingAgent.value) {
      await updateAgent(editingAgent.value.id, formData)
      ElMessage.success('更新成功')
    } else {
      await createAgent(formData)
      ElMessage.success('创建成功')
    }
    showCreateDialog.value = false
    await fetchAgents()
  } catch (error: unknown) {
    console.error('Failed to save agent:', error)
    ElMessage.error(editingAgent.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  editingAgent.value = null
  Object.assign(formData, {
    name: '',
    description: '',
    systemPrompt: '',
    modelConfig: '{"model": "gpt-4", "temperature": 0.7}',
    workflowType: 'REACT',
    maxIterations: 10,
    knowledgeBaseIds: [],
    isPublic: false
  })
  formRef.value?.clearValidate()
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(async () => {
  await Promise.all([fetchAgents(), fetchKnowledgeBases()])
})
</script>

<style scoped>
.agent-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.data-table {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
```

**Step 2: Verify TypeScript compilation**

Run: `cd frontend && npm run type-check`

Expected: No type errors

**Step 3: Commit**

```bash
git add frontend/src/views/agent/AgentListView.vue
git commit -m "feat: add Agent list view with create/edit dialogs"
```

---

## Task 4: Agent Detail View

**Files:**
- Create: `frontend/src/views/agent/AgentDetailView.vue`

**Step 1: Create agent detail view component**

Create `frontend/src/views/agent/AgentDetailView.vue`:

```vue
<template>
  <div class="agent-detail" v-loading="loading">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <div class="header-actions">
        <el-button type="primary" @click="showEditDialog = true">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-popconfirm
          title="确定删除这个 Agent 吗?"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="handleDelete"
        >
          <template #reference>
            <el-button type="danger">删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <el-card v-if="agent" class="agent-card">
      <template #header>
        <div class="card-header">
          <h2>{{ agent.name }}</h2>
          <el-tag :type="agent.isPublic ? 'success' : 'info'">
            {{ agent.isPublic ? '公开' : '私有' }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="描述" :span="2">
          {{ agent.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="工作流类型">
          <el-tag :type="agent.workflowType === 'REACT' ? 'primary' : 'warning'">
            {{ agent.workflowType }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最大迭代次数">
          {{ agent.maxIterations }}
        </el-descriptions-item>
        <el-descriptions-item label="模型配置" :span="2">
          <pre class="json-config">{{ formatJson(agent.modelConfig) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="系统提示词" :span="2">
          <div class="prompt-text">{{ agent.systemPrompt }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="知识库" :span="2">
          <el-tag
            v-for="kbId in agent.knowledgeBaseIds"
            :key="kbId"
            style="margin-right: 8px"
          >
            {{ getKnowledgeBaseName(kbId) }}
          </el-tag>
          <span v-if="agent.knowledgeBaseIds.length === 0">未绑定</span>
        </el-descriptions-item>
        <el-descriptions-item label="工具" :span="2">
          <el-tag
            v-for="toolId in agent.toolIds"
            :key="toolId"
            type="warning"
            style="margin-right: 8px"
          >
            {{ getToolName(toolId) }}
          </el-tag>
          <span v-if="agent.toolIds.length === 0">未绑定</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(agent.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(agent.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Test Execution Panel -->
    <el-card class="test-panel">
      <template #header>
        <h3>测试 Agent</h3>
      </template>

      <el-form @submit.prevent="executeTest">
        <el-form-item label="测试查询">
          <el-input
            v-model="testQuery"
            type="textarea"
            :rows="3"
            placeholder="输入测试查询..."
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="executing"
            :disabled="!testQuery.trim()"
            @click="executeTest"
          >
            执行测试
          </el-button>
          <el-button @click="clearResult" :disabled="!executionResult">
            清空结果
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="executionResult" class="execution-result">
        <el-divider>执行结果</el-divider>

        <div class="result-section">
          <h4>最终答案</h4>
          <div class="answer-box">{{ executionResult.answer }}</div>
        </div>

        <div v-if="executionResult.thoughtSteps.length > 0" class="result-section">
          <h4>思考步骤</h4>
          <el-timeline>
            <el-timeline-item
              v-for="step in executionResult.thoughtSteps"
              :key="step.step"
              :timestamp="`步骤 ${step.step}`"
            >
              <div><strong>思考:</strong> {{ step.thought }}</div>
              <div><strong>行动:</strong> {{ step.action }}</div>
              <div v-if="step.observation"><strong>观察:</strong> {{ step.observation }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-if="executionResult.toolCalls.length > 0" class="result-section">
          <h4>工具调用</h4>
          <div
            v-for="(call, index) in executionResult.toolCalls"
            :key="index"
            class="tool-call-item"
          >
            <div><strong>工具:</strong> {{ call.toolName }}</div>
            <div><strong>参数:</strong> <pre>{{ call.arguments }}</pre></div>
            <div><strong>结果:</strong> <pre>{{ call.result }}</pre></div>
            <el-tag :type="call.success ? 'success' : 'danger'" size="small">
              {{ call.success ? '成功' : '失败' }}
            </el-tag>
          </div>
        </div>

        <div class="result-section">
          <el-tag :type="executionResult.isComplete ? 'success' : 'warning'">
            {{ executionResult.isComplete ? '执行完成' : '执行中' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑 Agent"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入 Agent 名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入 Agent 描述"
          />
        </el-form-item>
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="formData.systemPrompt"
            type="textarea"
            :rows="5"
            placeholder="请输入系统提示词"
          />
        </el-form-item>
        <el-form-item label="模型配置" prop="modelConfig">
          <el-input
            v-model="formData.modelConfig"
            type="textarea"
            :rows="3"
            placeholder='{"model": "gpt-4", "temperature": 0.7}'
          />
        </el-form-item>
        <el-form-item label="工作流类型" prop="workflowType">
          <el-select v-model="formData.workflowType" placeholder="选择工作流类型">
            <el-option label="ReAct" value="REACT" />
            <el-option label="自定义" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大迭代次数" prop="maxIterations">
          <el-input-number v-model="formData.maxIterations" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="知识库" prop="knowledgeBaseIds">
          <el-select
            v-model="formData.knowledgeBaseIds"
            multiple
            placeholder="选择知识库"
            style="width: 100%"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Edit } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getAgent, updateAgent, deleteAgent, executeAgent } from '@/api/agent'
import { getKnowledgeBases } from '@/api/knowledge-base'
import type { Agent, UpdateAgentRequest, AgentExecutionResponse } from '@/types/agent'
import type { KnowledgeBase } from '@/types/knowledge-base'

const route = useRoute()
const router = useRouter()

const agentId = ref<number>(parseInt(route.params.id as string))
const agent = ref<Agent | null>(null)
const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(false)
const showEditDialog = ref(false)
const submitting = ref(false)
const executing = ref(false)
const formRef = ref<FormInstance>()

const testQuery = ref('')
const executionResult = ref<AgentExecutionResponse | null>(null)

const formData = reactive<UpdateAgentRequest>({
  name: '',
  description: '',
  systemPrompt: '',
  modelConfig: '',
  isPublic: false
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入 Agent 名称', trigger: 'blur' }
  ],
  systemPrompt: [
    { required: true, message: '请输入系统提示词', trigger: 'blur' }
  ]
}

async function fetchAgent() {
  loading.value = true
  try {
    const response = await getAgent(agentId.value)
    agent.value = response.data
  } catch (error: unknown) {
    console.error('Failed to fetch agent:', error)
    ElMessage.error('获取 Agent 详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchKnowledgeBases() {
  try {
    const response = await getKnowledgeBases({ page: 1, size: 1000 })
    knowledgeBases.value = response.data.items
  } catch (error: unknown) {
    console.error('Failed to fetch knowledge bases:', error)
  }
}

function goBack() {
  router.push({ name: 'Agents' })
}

async function handleDelete() {
  if (!agent.value) return
  try {
    await deleteAgent(agent.value.id)
    ElMessage.success('删除成功')
    goBack()
  } catch (error: unknown) {
    console.error('Failed to delete agent:', error)
    ElMessage.error('删除失败')
  }
}

async function handleSubmit() {
  if (!formRef.value || !agent.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await updateAgent(agent.value.id, formData)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    await fetchAgent()
  } catch (error: unknown) {
    console.error('Failed to update agent:', error)
    ElMessage.error('更新失败')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  if (agent.value) {
    Object.assign(formData, {
      name: agent.value.name,
      description: agent.value.description,
      systemPrompt: agent.value.systemPrompt,
      modelConfig: agent.value.modelConfig,
      isPublic: agent.value.isPublic
    })
  }
}

async function executeTest() {
  if (!agent.value || !testQuery.value.trim()) return

  executing.value = true
  try {
    const response = await executeAgent(agent.value.id, {
      query: testQuery.value,
      stream: false
    })
    executionResult.value = response.data
    ElMessage.success('执行完成')
  } catch (error: unknown) {
    console.error('Failed to execute agent:', error)
    ElMessage.error('执行失败')
  } finally {
    executing.value = false
  }
}

function clearResult() {
  executionResult.value = null
  testQuery.value = ''
}

function getKnowledgeBaseName(id: number) {
  const kb = knowledgeBases.value.find(k => k.id === id)
  return kb?.name || `ID: ${id}`
}

function getToolName(id: number) {
  return `Tool ${id}`
}

function formatJson(json: string) {
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

function formatDateTime(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(async () => {
  await Promise.all([fetchAgent(), fetchKnowledgeBases()])
  if (agent.value) {
    Object.assign(formData, {
      name: agent.value.name,
      description: agent.value.description,
      systemPrompt: agent.value.systemPrompt,
      modelConfig: agent.value.modelConfig,
      isPublic: agent.value.isPublic
    })
  }
})
</script>

<style scoped>
.agent-detail {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.agent-card {
  margin-bottom: 20px;
}

.json-config {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.prompt-text {
  white-space: pre-wrap;
  line-height: 1.6;
}

.test-panel {
  margin-top: 20px;
}

.execution-result {
  margin-top: 20px;
}

.result-section {
  margin-bottom: 30px;
}

.result-section h4 {
  margin-bottom: 15px;
  font-weight: 600;
}

.answer-box {
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  padding: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.tool-call-item {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 10px;
}

.tool-call-item pre {
  background: white;
  padding: 8px;
  border-radius: 4px;
  margin: 5px 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
```

**Step 2: Verify TypeScript compilation**

Run: `cd frontend && npm run type-check`

Expected: No type errors

**Step 3: Commit**

```bash
git add frontend/src/views/agent/AgentDetailView.vue
git commit -m "feat: add Agent detail view with test execution panel"
```

---

## Task 5: Update MainLayout Navigation

**Files:**
- Modify: `frontend/src/layouts/MainLayout.vue`

**Step 1: Verify Agent routes in navigation**

Check that MainLayout.vue includes the Agents navigation item. The route should already exist in router/index.ts (lines 42-51).

Run: `grep -n "Agents" frontend/src/layouts/MainLayout.vue`

Expected: Navigation item for Agents should be present

**Step 2: Test navigation**

Run: `cd frontend && npm run dev`

Expected: Development server starts successfully, navigate to http://localhost:5173 and verify "Agents" link appears in sidebar

**Step 3: Commit if needed**

```bash
git add frontend/src/layouts/MainLayout.vue
git commit -m "chore: ensure Agents navigation is present"
```

---

## Task 6: Integration Testing

**Files:**
- No new files (testing existing implementation)

**Step 1: Build frontend**

Run: `cd frontend && npm run build`

Expected: Successful build with no errors

**Step 2: Run type check**

Run: `cd frontend && npm run type-check`

Expected: No TypeScript errors

**Step 3: Run linter**

Run: `cd frontend && npm run lint`

Expected: No ESLint errors

**Step 4: Verify all tasks complete**

Check that all components are implemented:
- ✅ Agent type definitions
- ✅ Agent API functions
- ✅ Agent list view with create/edit
- ✅ Agent detail view with test execution
- ✅ Navigation integration

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: complete Agent Management UI implementation"
```

---

## Summary

This plan implements a complete Agent Management UI with the following features:

1. **Type-safe API Layer** - Full TypeScript interfaces matching backend DTOs
2. **List View** - Paginated table with search, create, edit, delete operations
3. **Detail View** - Complete Agent configuration display with:
   - Agent metadata and configuration
   - Knowledge base binding
   - Tool binding
   - Test execution panel with real-time response display
4. **Test Execution** - Execute Agent queries and visualize:
   - Final answer
   - Thought steps (ReAct workflow)
   - Tool calls with results
   - Execution status

**Code Quality Practices:**
- Zero `any` types - complete type safety
- Proper error handling with user feedback
- Loading states for better UX
- Form validation
- Responsive design
- Clean code structure following Vue 3 best practices

**Estimated Implementation Time:** ~2-3 hours (4-5 tasks × 25-35 minutes each)

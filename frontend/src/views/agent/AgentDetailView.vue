<template>
  <div v-loading="loading" class="agent-detail">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
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
        <el-descriptions-item label="模型配置" :span="2">
          <el-card class="model-config-card" shadow="never">
            <template #header>
              <span>LLM API 配置信息</span>
            </template>
            <div class="config-grid">
              <div class="config-item">
                <label>模型名称:</label>
                <span>{{ getModelConfigValue('model') || '-' }}</span>
              </div>
              <div class="config-item">
                <label>温度:</label>
                <span>{{ getModelConfigValue('temperature') || '-' }}</span>
              </div>
              <div class="config-item">
                <label>最大Token数:</label>
                <span>{{ getModelConfigValue('maxTokens') || '-' }}</span>
              </div>
              <div class="config-item">
                <label>Top-P:</label>
                <span>{{ getModelConfigValue('topP') || '-' }}</span>
              </div>
              <div class="config-item" v-if="getModelConfigValue('endpoint')">
                <label>API端点:</label>
                <span>{{ getModelConfigValue('endpoint') }}</span>
              </div>
              <div class="config-item" v-if="getModelConfigValue('maskedApiKey')">
                <label>API密钥:</label>
                <span>{{ getModelConfigValue('maskedApiKey') }}</span>
              </div>
            </div>
          </el-card>
        </el-descriptions-item>
        <el-descriptions-item label="关联模型配置" :span="2" v-if="agent?.llmModelConfig">
          <el-card class="linked-config-card" shadow="never">
            <template #header>
              <span>关联的模型配置详情</span>
            </template>
            <div class="linked-config-info">
              <div class="config-detail">
                <label>配置名称:</label>
                <span>{{ agent.llmModelConfig.name }}</span>
              </div>
              <div class="config-detail">
                <label>模型类型:</label>
                <span>{{ agent.llmModelConfig.type }}</span>
              </div>
              <div class="config-detail">
                <label>API端点:</label>
                <span>{{ agent.llmModelConfig.endpoint }}</span>
              </div>
              <div class="config-detail">
                <label>API密钥:</label>
                <span>{{ agent.llmModelConfig.maskedApiKey }}</span>
              </div>
              <div class="config-detail">
                <label>模型:</label>
                <span>{{ agent.llmModelConfig.model }}</span>
              </div>
              <div class="config-detail" v-if="agent.llmModelConfig.temperature">
                <label>温度:</label>
                <span>{{ agent.llmModelConfig.temperature }}</span>
              </div>
              <div class="config-detail" v-if="agent.llmModelConfig.maxTokens">
                <label>最大Token数:</label>
                <span>{{ agent.llmModelConfig.maxTokens }}</span>
              </div>
            </div>
          </el-card>
        </el-descriptions-item>
        <el-descriptions-item label="系统提示词" :span="2">
          <div class="prompt-text">{{ agent.systemPrompt }}</div>
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
          <el-input v-model="testQuery" type="textarea" :rows="3" placeholder="输入测试查询..." />
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
          <el-button :disabled="!executionResult" @click="clearResult"> 清空结果 </el-button>
        </el-form-item>
      </el-form>

      <div v-if="executionResult" class="execution-result">
        <el-divider>执行结果</el-divider>

        <div class="result-section">
          <h4>执行结果</h4>
          <div class="answer-box">{{ executionResult.result }}</div>
        </div>

        <div v-if="executionResult.steps && executionResult.steps.length > 0" class="result-section">
          <h4>执行步骤</h4>
          <el-timeline>
            <el-timeline-item
              v-for="step in executionResult.steps"
              :key="step.step"
              :timestamp="`步骤 ${step.step}`"
            >
              <el-tag size="small" style="margin-bottom: 8px">{{ step.type }}</el-tag>
              <div>{{ step.content }}</div>
              <div v-if="step.toolName"><strong>工具:</strong> {{ step.toolName }}</div>
              <div v-if="step.toolArgs">
                <strong>参数:</strong>
                <pre>{{ formatJson(step.toolArgs) }}</pre>
              </div>
              <div v-if="step.toolResult">
                <strong>结果:</strong>
                <pre>{{ formatJson(step.toolResult) }}</pre>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="result-section">
          <el-tag :type="executionResult.finished ? 'success' : 'warning'">
            {{ executionResult.finished ? '执行完成' : '执行中' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑 Agent" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
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
        <el-form-item label="是否公开" prop="isPublic">
          <el-switch v-model="formData.isPublic" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit"> 确定 </el-button>
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
import {
  getAgent,
  updateAgent,
  deleteAgent,
  executeAgent,
  type Agent,
  type UpdateAgentRequest,
  type AgentExecutionResponse,
  type AgentModelConfig
} from '@/api/agent'
import { getKnowledgeBases } from '@/api/knowledge-base'
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
  isPublic: false
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }]
}

async function fetchAgent() {
  loading.value = true
  try {
    agent.value = await getAgent(agentId.value)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取 Agent 详情失败'
    console.error('Failed to fetch agent:', error)
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

async function fetchKnowledgeBases() {
  try {
    const data = await getKnowledgeBases({ page: 1, pageSize: 1000 })
    knowledgeBases.value = data.records
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取知识库列表失败'
    console.error('Failed to fetch knowledge bases:', error)
    ElMessage.error(errorMessage)
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
    const errorMessage = error instanceof Error ? error.message : '删除失败'
    console.error('Failed to delete agent:', error)
    ElMessage.error(errorMessage)
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
    const errorMessage = error instanceof Error ? error.message : '更新失败'
    console.error('Failed to update agent:', error)
    ElMessage.error(errorMessage)
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

    // 确保响应格式正确，即使没有steps也提供默认值
    executionResult.value = {
      ...response,
      steps: response.steps || []
    }
    ElMessage.success('执行完成')
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '执行失败'
    console.error('Failed to execute agent:', error)
    ElMessage.error(errorMessage)
  } finally {
    executing.value = false
  }
}

function clearResult() {
  executionResult.value = null
  testQuery.value = ''
}

function formatJson(json: unknown) {
  if (typeof json === 'string') {
    try {
      return JSON.stringify(JSON.parse(json), null, 2)
    } catch {
      return json
    }
  }
  return JSON.stringify(json, null, 2)
}

function formatDateTime(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

function getModelConfigValue(key: keyof AgentModelConfig) {
  // 优先使用关联的模型配置信息
  if (agent.value?.llmModelConfig) {
    return agent.value.llmModelConfig[key as keyof typeof agent.value.llmModelConfig]
  }
  // 回退到agent自身存储的模型配置
  if (!agent.value?.modelConfig) return undefined

  let modelConfig: AgentModelConfig | null = null

  if (typeof agent.value.modelConfig === 'string') {
    try {
      modelConfig = JSON.parse(agent.value.modelConfig) as AgentModelConfig
    } catch {
      return undefined
    }
  } else {
    modelConfig = agent.value.modelConfig as AgentModelConfig
  }

  return modelConfig?.[key]
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

.model-config-card {
  width: 100%;
}

.linked-config-card {
  width: 100%;
  margin-top: 10px;
}

.model-config-card .el-card__header {
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  padding: 12px 20px;
  font-weight: 600;
}

.linked-config-card .el-card__header {
  background-color: #e8f4ff;
  border-bottom: 1px solid #b3d8ff;
  padding: 12px 20px;
  font-weight: 600;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.config-item, .config-detail {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.config-item label, .config-detail label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
  font-size: 14px;
}

.config-item span, .config-detail span {
  color: #303133;
  word-break: break-all;
  padding: 2px 0;
  font-family: monospace;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 13px;
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

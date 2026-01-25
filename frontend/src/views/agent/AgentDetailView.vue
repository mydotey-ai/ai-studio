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

    <!-- 基本信息卡片 -->
    <el-card v-if="agent" class="agent-card">
      <template #header>
        <div class="card-header">
          <div class="agent-title">
            <h2>{{ agent.name }}</h2>
            <el-tag :type="agent.isPublic ? 'success' : 'info'">
              {{ agent.isPublic ? '公开' : '私有' }}
            </el-tag>
          </div>
          <div class="agent-meta">
            <el-tag :type="getWorkflowTypeColor(agent.workflowType)" effect="dark">
              {{ agent.workflowType }}
            </el-tag>
          </div>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="24">
          <div class="section">
            <h4>描述</h4>
            <p class="agent-description">{{ agent.description || '-' }}</p>
          </div>
        </el-col>
      </el-row>

      <!-- 模型配置信息 -->
      <el-row :gutter="20" v-if="getModelConfig()">
        <el-col :span="24">
          <div class="section">
            <h4>模型配置</h4>
            <div class="config-container">
              <div class="config-item-simple">
                <label>模型名称:</label>
                <span>{{ getModelConfig().model || '-' }}</span>
              </div>
              <div class="config-item-simple">
                <label>温度:</label>
                <span>{{ getModelConfig().temperature ?? '-' }}</span>
              </div>
              <div class="config-item-simple">
                <label>最大Token数:</label>
                <span>{{ getModelConfig().maxTokens || '-' }}</span>
              </div>
              <div class="config-item-simple" v-if="getModelConfig().topP !== undefined">
                <label>Top-P:</label>
                <span>{{ getModelConfig().topP }}</span>
              </div>
              <div class="config-item-simple" v-if="getModelConfig().endpoint">
                <label>API端点:</label>
                <span>{{ getModelConfig().endpoint }}</span>
              </div>
              <div class="config-item-simple" v-if="getModelConfig().maskedApiKey">
                <label>API密钥:</label>
                <span>{{ getModelConfig().maskedApiKey }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 系统提示词 -->
      <el-row :gutter="20">
        <el-col :span="24">
          <div class="section">
            <h4>系统提示词</h4>
            <div class="system-prompt">{{ agent.systemPrompt }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 时间信息 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="section">
            <h4>创建时间</h4>
            <p>{{ formatDateTime(agent.createdAt) }}</p>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="section">
            <h4>更新时间</h4>
            <p>{{ formatDateTime(agent.updatedAt) }}</p>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 测试面板 -->
    <el-card class="test-panel">
      <template #header>
        <h3>测试 Agent</h3>
      </template>

      <el-form @submit.prevent="executeTest" :model="formState">
        <el-form-item>
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
          <el-button :disabled="!executionResult" @click="clearResult">清空结果</el-button>
        </el-form-item>
      </el-form>

      <div v-if="executionResult" class="execution-result">
        <el-divider content-position="left">执行结果</el-divider>

        <div class="result-content">
          <div class="answer-section">
            <h4>最终回答</h4>
            <div class="final-answer">{{ executionResult.result }}</div>
          </div>

          <div v-if="executionResult.steps && executionResult.steps.length > 0" class="steps-section">
            <h4>执行步骤</h4>
            <el-collapse accordion>
              <el-collapse-item
                v-for="(step, index) in executionResult.steps"
                :key="index"
                :title="`步骤 ${step.step || index + 1}: ${getStepTypeLabel(step.type)}`"
                :name="index"
              >
                <div class="step-details">
                  <div class="step-content">{{ step.content }}</div>

                  <div v-if="step.toolName" class="step-tool">
                    <strong>工具:</strong>
                    <el-tag type="info" size="small">{{ step.toolName }}</el-tag>
                  </div>

                  <div v-if="step.toolArgs" class="step-params">
                    <strong>参数:</strong>
                    <pre class="json-display">{{ formatJson(step.toolArgs) }}</pre>
                  </div>

                  <div v-if="step.toolResult" class="step-result">
                    <strong>结果:</strong>
                    <pre class="json-display result">{{ formatJson(step.toolResult) }}</pre>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <div class="status-section">
            <el-tag :type="executionResult.finished ? 'success' : 'warning'">
              {{ executionResult.finished ? '执行完成' : '执行中' }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑 Agent"
      width="60%"
      :fullscreen="dialogFullscreen"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入 Agent 名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="3"
                placeholder="请输入 Agent 描述"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="系统提示词" prop="systemPrompt">
              <el-input
                v-model="formData.systemPrompt"
                type="textarea"
                :rows="6"
                placeholder="请输入系统提示词"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否公开" prop="isPublic">
              <el-switch v-model="formData.isPublic" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
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
const dialogFullscreen = ref(false)

const testQuery = ref('')
const executionResult = ref<AgentExecutionResponse | null>(null)

const formData = reactive<UpdateAgentRequest>({
  name: '',
  description: '',
  systemPrompt: '',
  isPublic: false
})

const formState = reactive({
  testQuery: ''
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

function getWorkflowTypeColor(workflowType: string) {
  switch (workflowType) {
    case 'REACT': return 'primary'
    case 'LINEAR': return 'warning'
    case 'DAG': return 'success'
    default: return 'info'
  }
}

function getStepTypeLabel(type: string) {
  switch (type) {
    case 'thought': return '思考'
    case 'action': return '行动'
    case 'observation': return '观察'
    default: return type
  }
}

function getModelConfig(): AgentModelConfig {
  // 优先使用关联的模型配置信息
  if (agent.value?.llmModelConfig) {
    const { id, name, type, ...config } = agent.value.llmModelConfig
    return config as AgentModelConfig
  }
  // 回退到agent自身存储的模型配置
  if (!agent.value?.modelConfig) return { model: '-', temperature: 0, maxTokens: 0, topP: 0 }

  let modelConfig: AgentModelConfig | null = null

  if (typeof agent.value.modelConfig === 'string') {
    try {
      modelConfig = JSON.parse(agent.value.modelConfig) as AgentModelConfig
    } catch {
      return { model: '-', temperature: 0, maxTokens: 0, topP: 0 }
    }
  } else {
    modelConfig = agent.value.modelConfig as AgentModelConfig
  }

  return modelConfig || { model: '-', temperature: 0, maxTokens: 0, topP: 0 }
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
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}

.agent-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.agent-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-card {
  margin-bottom: 20px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.section {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafcff;
  border-radius: 6px;
  border-left: 4px solid #409eff;
}

.section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.agent-description {
  margin: 0;
  line-height: 1.6;
  color: #606266;
  font-size: 14px;
}

.config-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 8px;
}

.config-item-simple {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.config-item-simple label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
  font-size: 14px;
}

.config-item-simple span {
  color: #303133;
  word-break: break-all;
  padding: 6px 10px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
  display: inline-block;
  max-width: 100%;
}

.system-prompt {
  white-space: pre-wrap;
  line-height: 1.6;
  padding: 12px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  color: #303133;
}

.test-panel {
  margin-top: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.execution-result {
  margin-top: 20px;
  background: #f8f9fc;
  padding: 16px;
  border-radius: 6px;
}

.result-content {
  padding: 16px 0;
}

.answer-section h4,
.steps-section h4 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.final-answer {
  background: white;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  padding: 15px;
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(179, 216, 255, 0.3);
}

.step-details {
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-top: 8px;
}

.step-content {
  margin-bottom: 12px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 1.5;
}

.step-tool, .step-params, .step-result {
  margin-bottom: 12px;
}

.step-tool strong, .step-params strong, .step-result strong {
  display: inline-block;
  width: 80px;
  color: #606266;
  margin-right: 8px;
}

.json-display {
  background: #f8f9fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px;
  margin: 8px 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  font-family: monospace;
  max-height: 200px;
  overflow-y: auto;
}

.json-display.result {
  background: #f0f9ff;
  border-color: #b3d8ff;
}

.status-section {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  text-align: center;
}

.el-collapse-item__header {
  font-weight: 500;
  color: #409eff;
}

@media (max-width: 768px) {
  .agent-detail {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .config-container {
    grid-template-columns: 1fr;
  }

  .agent-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .section {
    padding: 12px;
  }

  .step-tool strong, .step-params strong, .step-result strong {
    width: 70px;
  }
}
</style>

<template>
  <router-view v-slot="{ Component: RouteComponent, route }">
    <div v-if="route.name === 'Agents'" class="agent-list">
    <div class="header">
      <h2>Agents</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
        创建 Agent
      </el-button>
    </div>

    <el-table
      :data="agents"
      :loading="loading"
      stripe
      style="cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column label="工作流类型" width="120" align="center">
        <template #default="{ row }">
          <el-tag size="small">
            {{ row.workflowType === 'REACT' ? 'ReAct' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="知识库" width="120" align="center">
        <template #default="{ row }">
          {{ row.knowledgeBaseIds?.length || 0 }}
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
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click.stop="handleView(row)">
            查看
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

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadAgents"
        @current-change="loadAgents"
      />
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="showCreateDialog"
      :title="isEdit ? '编辑 Agent' : '创建 Agent'"
      width="700px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入 Agent 名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="请输入系统提示词，定义 Agent 的角色和行为"
          />
        </el-form-item>

        <el-form-item label="LLM 模型" prop="modelConfigId">
          <el-select
            v-model="form.modelConfigId"
            placeholder="请选择 LLM 模型"
            style="width: 100%"
            clearable
            @change="handleModelConfigChange"
          >
            <el-option
              v-for="model in llmModels"
              :key="model.id"
              :label="model.name"
              :value="model.id"
            >
              <div style="display: flex; justify-content: space-between">
                <span>{{ model.name }}</span>
                <el-tag v-if="model.isDefault" size="small" type="success">默认</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="知识库" prop="knowledgeBaseIds">
          <el-select
            v-model="form.knowledgeBaseIds"
            multiple
            placeholder="请选择知识库"
            style="width: 100%"
          >
            <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="可见性" prop="isPublic">
          <el-switch v-model="form.isPublic" active="公开" inactive="私有" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
    </div>

    <component v-else :is="RouteComponent" />
  </router-view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, View, Edit, Delete } from '@element-plus/icons-vue'
import {
  getAgents,
  createAgent,
  updateAgent,
  deleteAgent as deleteAgentApi,
  type Agent,
  type AgentModelConfig,
  type CreateAgentRequest,
  type UpdateAgentRequest
} from '@/api/agent'
import { getKnowledgeBases } from '@/api/knowledge-base'
import { modelConfigApi, type ModelConfig, ModelConfigType } from '@/api/modelConfig'
import type { KnowledgeBase } from '@/types/knowledge-base'
import dayjs from 'dayjs'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)
const submitting = ref(false)
const showCreateDialog = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const agents = ref<Agent[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const llmModels = ref<ModelConfig[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const form = reactive<CreateAgentRequest & { modelConfigId?: number }>({
  name: '',
  description: '',
  systemPrompt: '',
  modelConfigId: undefined,
  modelConfig: {
    model: 'gpt-4',
    temperature: 0.7,
    maxTokens: 2000,
    topP: 0.9
  },
  workflowType: 'REACT',
  knowledgeBaseIds: [],
  toolIds: []
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入 Agent 名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度为 2-50 位', trigger: 'blur' }
  ],
  systemPrompt: [
    { required: true, message: '请输入系统提示词', trigger: 'blur' },
    { min: 10, message: '系统提示词至少 10 个字符', trigger: 'blur' }
  ]
}

async function loadAgents() {
  loading.value = true
  try {
    const data = await getAgents({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    agents.value = data.records
    pagination.total = data.total
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载 Agent 列表失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}

async function loadKnowledgeBases() {
  try {
    const data = await getKnowledgeBases({ page: 1, pageSize: 1000 })
    knowledgeBases.value = data.records
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载知识库列表失败'
    ElMessage.error(errorMessage)
  }
}

async function loadLlmModels() {
  try {
    const response = await modelConfigApi.getList(ModelConfigType.LLM);
    const models = response as ModelConfig[];
    llmModels.value = models

    // Auto-select default model if available
    if (models.length > 0 && !form.modelConfigId) {
      const defaultModel = models.find((m: any) => m.isDefault)
      if (defaultModel) {
        form.modelConfigId = defaultModel.id
        handleModelConfigChange(defaultModel.id)
      } else {
        form.modelConfigId = models[0].id
        handleModelConfigChange(models[0].id)
      }
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载 LLM 模型列表失败'
    ElMessage.error(errorMessage)
  }
}

function handleModelConfigChange(modelConfigId: number | undefined) {
  if (!modelConfigId) {
    form.modelConfig = {
      model: 'gpt-4',
      temperature: 0.7,
      maxTokens: 2000,
      topP: 0.9
    }
    return
  }

  const selectedModel = llmModels.value.find(m => m.id === modelConfigId)
  if (selectedModel) {
    form.modelConfig = {
      model: selectedModel.model,
      temperature: selectedModel.temperature || 0.7,
      maxTokens: selectedModel.maxTokens || 2000,
      topP: 0.9
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async valid => {
    if (!valid) return

    submitting.value = true
    try {
      const modelConfigJson = JSON.stringify(form.modelConfig)
      if (isEdit.value && editingId.value) {
        const updateData: UpdateAgentRequest = {
          name: form.name,
          description: form.description,
          systemPrompt: form.systemPrompt,
          modelConfig: modelConfigJson,
          knowledgeBaseIds: form.knowledgeBaseIds,
          isPublic: form.isPublic
        }
        await updateAgent(editingId.value, updateData)
        ElMessage.success('更新成功')
      } else {
        const createData: CreateAgentRequest = {
          ...form,
          modelConfig: modelConfigJson
        }
        await createAgent(createData)
        ElMessage.success('创建成功')
      }

      showCreateDialog.value = false
      resetForm()
      loadAgents()
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '操作失败，请稍后重试'
      ElMessage.error(errorMessage)
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.description = ''
  form.systemPrompt = ''
  form.modelConfigId = undefined
  form.modelConfig = {
    model: 'gpt-4',
    temperature: 0.7,
    maxTokens: 2000,
    topP: 0.9
  }
  form.knowledgeBaseIds = []
  form.toolIds = []
  form.isPublic = false

  // Auto-select default model if available
  if (llmModels.value.length > 0) {
    const defaultModel = llmModels.value.find(m => m.isDefault)
    if (defaultModel) {
      form.modelConfigId = defaultModel.id
      handleModelConfigChange(defaultModel.id)
    } else {
      form.modelConfigId = llmModels.value[0].id
      handleModelConfigChange(llmModels.value[0].id)
    }
  }

  formRef.value?.resetFields()
}

function handleRowClick(row: Agent) {
  router.push(`/agents/${row.id}`)
}

function handleView(row: Agent) {
  router.push(`/agents/${row.id}`)
}

function handleEdit(row: Agent) {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  form.systemPrompt = row.systemPrompt

  // Parse modelConfig from JSON string if it's a string
  if (typeof row.modelConfig === 'string') {
    form.modelConfig = JSON.parse(row.modelConfig) as AgentModelConfig
  } else {
    form.modelConfig = row.modelConfig
  }

  form.knowledgeBaseIds = []
  form.toolIds = []
  form.isPublic = row.isPublic

  // Try to find matching model config id based on model name
  const modelConfigObj = typeof row.modelConfig === 'string'
    ? JSON.parse(row.modelConfig) as AgentModelConfig
    : row.modelConfig
  const matchingModel = llmModels.value.find(m => m.model === modelConfigObj.model)
  form.modelConfigId = matchingModel ? matchingModel.id : undefined

  showCreateDialog.value = true
}

async function handleDelete(row: Agent) {
  try {
    await ElMessageBox.confirm(`确定要删除 Agent"${row.name}"吗？此操作不可恢复。`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAgentApi(row.id)
    ElMessage.success('删除成功')
    loadAgents()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      const errorMessage = error instanceof Error ? error.message : '删除失败，请稍后重试'
      ElMessage.error(errorMessage)
    }
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadAgents()
  loadKnowledgeBases()
  loadLlmModels()
})
</script>

<style scoped lang="scss">
.agent-list {
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

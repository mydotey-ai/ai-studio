<template>
  <div class="model-config-container">
    <el-page-header @back="$router.go(-1)" title="返回">
      <template #content>
        <span class="text-large font-600">模型配置管理</span>
      </template>
    </el-page-header>

    <el-card class="config-card" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <el-tabs v-model="activeType" @tab-change="handleTypeChange">
            <el-tab-pane label="向量模型" :name="ModelConfigType.EMBEDDING" />
            <el-tab-pane label="大语言模型" :name="ModelConfigType.LLM" />
          </el-tabs>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增配置
          </el-button>
        </div>
      </template>

      <el-table :data="configs" v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="endpoint" label="端点" />
        <el-table-column prop="model" label="模型" />
        <el-table-column prop="dimension" label="维度" v-if="activeType === ModelConfigType.EMBEDDING" />
        <el-table-column prop="maskedApiKey" label="API Key" />
        <el-table-column label="默认" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault" type="success" size="small">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              type="success"
              v-if="!row.isDefault"
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模型配置' : '新增模型配置'"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" disabled style="width: 100%">
            <el-option
              v-for="(label, value) in ModelConfigTypeLabels"
              :key="value"
              :label="label"
              :value="value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="端点" prop="endpoint">
          <el-input v-model="form.endpoint" placeholder="https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
">
          <el-input v-model="form.apiKey" placeholder="请输入API Key" show-password />
        </el-form-item>
        <el-form-item label="模型" prop="model">
          <el-input v-model="form.model" placeholder="gpt-3.5-turbo" />
        </el-form-item>
        <el-form-item
          label="维度"
          prop="dimension"
          v-if="form.type === ModelConfigType.EMBEDDING"
        >
          <el-input-number v-model="form.dimension" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item
          label="Temperature"
          prop="temperature"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
        <el-form-item
          label="Max Tokens"
          prop="maxTokens"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-input-number v-model="form.maxTokens" :min="1" :max="100000" />
        </el-form-item>
        <el-form-item label="超时(ms)" prop="timeout">
          <el-input-number v-model="form.timeout" :min="1000" :max="300000" :step="1000" />
        </el-form-item>
        <el-form-item
          label="流式输出"
          prop="enableStreaming"
          v-if="form.type === ModelConfigType.LLM"
        >
          <el-switch v-model="form.enableStreaming" />
        </el-form-item>
        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { modelConfigApi, type ModelConfig, type ModelConfigRequest } from '@/api/modelConfig'
import { ModelConfigType, ModelConfigTypeLabels } from '@/enums/modelConfigType'

const activeType = ref(ModelConfigType.EMBEDDING)
const configs = ref<ModelConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<Partial<ModelConfigRequest>>({
  name: '',
  type: ModelConfigType.EMBEDDING,
  endpoint: 'https://api.openai.com/v1',
  apiKey: '',
  model: '',
  dimension: undefined,
  temperature: 0.3,
  maxTokens: 1000,
  timeout: 30000,
  enableStreaming: true,
  isDefault: false,
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  endpoint: [{ required: true, message: '请输入端点', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入API Key', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型', trigger: 'blur' }],
  dimension: [
    {
      required: true,
      message: '请输入维度',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        if (form.type === ModelConfigType.EMBEDDING && !value) {
          callback(new Error('请输入维度'))
        } else {
          callback()
        }
      }
    }
  ]
}

const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await modelConfigApi.getList(activeType.value)
    configs.value = res.data
  } catch (error) {
    ElMessage.error('加载配置失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  loadConfigs()
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, {
    name: '',
    type: activeType.value,
    endpoint: 'https://api.openai.com/v1',
    apiKey: '',
    model: activeType.value === ModelConfigType.EMBEDDING ? 'text-embedding-ada-002' : 'gpt-3.5-turbo',
    dimension: activeType.value === ModelConfigType.EMBEDDING ? 1536 : undefined,
    temperature: 0.3,
    maxTokens: 1000,
    timeout: 30000,
    enableStreaming: true,
    isDefault: false,
    description: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: ModelConfig) => {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    name: row.name,
    type: row.type,
    endpoint: row.endpoint,
    apiKey: '',
    model: row.model,
    dimension: row.dimension,
    temperature: row.temperature,
    maxTokens: row.maxTokens,
    timeout: row.timeout,
    enableStreaming: row.enableStreaming,
    isDefault: row.isDefault,
    description: row.description
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await modelConfigApi.update(currentId.value, form as ModelConfigRequest)
      ElMessage.success('更新成功')
    } else {
      await modelConfigApi.create(form as ModelConfigRequest)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadConfigs()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (row: ModelConfig) => {
  try {
    await modelConfigApi.setDefault(row.id)
    ElMessage.success('设置成功')
    loadConfigs()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const handleTest = async (row: ModelConfig) => {
  try {
    const res = await modelConfigApi.test(row.id)
    if (res.data) {
      ElMessage.success('配置测试成功')
    } else {
      ElMessage.error('配置测试失败')
    }
  } catch (error) {
    ElMessage.error('配置测试失败')
  }
}

const handleDelete = async (row: ModelConfig) => {
  try {
    await ElMessageBox.confirm(`确定要删除配置"${row.name}"吗？`, '确认删除', {
      type: 'warning'
    })
    await modelConfigApi.delete(row.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString()
}

const currentId = ref(0)

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.model-config-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-card {
  margin-top: 20px;
}
</style>

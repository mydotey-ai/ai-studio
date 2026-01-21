<template>
  <el-form ref="formRef" :model="formData" :rules="formRules" label-width="140px">
    <el-form-item label="名称" prop="name">
      <el-input v-model="formData.name" placeholder="请输入服务器名称" />
    </el-form-item>

    <el-form-item label="描述" prop="description">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="2"
        placeholder="请输入服务器描述（可选）"
      />
    </el-form-item>

    <el-form-item label="连接类型" prop="connectionType">
      <el-radio-group v-model="formData.connectionType" @change="handleConnectionTypeChange">
        <el-radio value="STDIO">STDIO（标准输入/输出）</el-radio>
        <el-radio value="HTTP">HTTP（网络端点）</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- STDIO 连接配置 -->
    <template v-if="formData.connectionType === 'STDIO'">
      <el-form-item label="命令" prop="command">
        <el-input
          v-model="formData.command"
          placeholder="例如: npx -y @modelcontextprotocol/server-filesystem"
        />
      </el-form-item>

      <el-form-item label="工作目录" prop="workingDir">
        <el-input v-model="formData.workingDir" placeholder="例如：/home/user/projects" />
      </el-form-item>
    </template>

    <!-- HTTP 连接配置 -->
    <template v-if="formData.connectionType === 'HTTP'">
      <el-form-item label="端点 URL" prop="endpointUrl">
        <el-input v-model="formData.endpointUrl" placeholder="例如: https://api.example.com/mcp" />
      </el-form-item>

      <el-form-item label="请求头">
        <el-input
          v-model="formData.headersString"
          type="textarea"
          :rows="2"
          placeholder='例如: {"Custom-Header": "value"}'
        />
      </el-form-item>
    </template>

    <el-form-item label="认证类型" prop="authType">
      <el-select
        v-model="formData.authType"
        placeholder="请选择认证类型"
        @change="handleAuthTypeChange"
      >
        <el-option label="无认证" value="NONE" />
        <el-option label="API Key" value="API_KEY" />
        <el-option label="Basic Auth" value="BASIC" />
      </el-select>
    </el-form-item>

    <!-- API Key 认证配置 -->
    <template v-if="formData.authType === 'API_KEY'">
      <el-form-item label="API Key" prop="apiKey">
        <el-input
          v-model="formData.apiKey"
          type="password"
          placeholder="请输入 API Key"
          show-password
        />
      </el-form-item>

      <el-form-item label="Header 名称" prop="apiKeyHeader">
        <el-input v-model="formData.apiKeyHeader" placeholder="默认: x-api-key" />
      </el-form-item>
    </template>

    <!-- Basic Auth 认证配置 -->
    <template v-if="formData.authType === 'BASIC'">
      <el-form-item label="用户名" prop="basicUsername">
        <el-input v-model="formData.basicUsername" placeholder="请输入用户名" />
      </el-form-item>

      <el-form-item label="密码" prop="basicPassword">
        <el-input
          v-model="formData.basicPassword"
          type="password"
          placeholder="请输入密码"
          show-password
        />
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

const props = defineProps<Props>()

const emit = defineEmits<{
  submit: [data: CreateMcpServerRequest | UpdateMcpServerRequest]
}>()

const formRef = ref<FormInstance>()

interface FormData {
  name: string
  description: string
  connectionType: 'STDIO' | 'HTTP' | ''
  command: string
  workingDir: string
  endpointUrl: string
  headersString: string
  authType: 'NONE' | 'API_KEY' | 'BASIC'
  apiKey: string
  apiKeyHeader: string
  basicUsername: string
  basicPassword: string
}

const formData = reactive<FormData>({
  name: '',
  description: '',
  connectionType: '',
  command: '',
  workingDir: '',
  endpointUrl: '',
  headersString: '',
  authType: 'NONE',
  apiKey: '',
  apiKeyHeader: '',
  basicUsername: '',
  basicPassword: ''
})

// 验证命令（仅 STDIO 时必填）
const validateCommand = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (formData.connectionType === 'STDIO' && !value) {
    callback(new Error('请输入命令'))
  } else {
    callback()
  }
}

// 验证端点 URL（仅 HTTP 时必填）
const validateEndpointUrl = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (formData.connectionType === 'HTTP' && !value) {
    callback(new Error('请输入端点 URL'))
  } else {
    callback()
  }
}

const formRules: FormRules<FormData> = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  connectionType: [{ required: true, message: '请选择连接类型', trigger: 'change' }],
  command: [{ validator: validateCommand, trigger: 'blur' }],
  endpointUrl: [{ validator: validateEndpointUrl, trigger: 'blur' }]
}

// 处理连接类型变更
const handleConnectionTypeChange = () => {
  // 切换连接类型时清空相关字段
  if (formData.connectionType === 'STDIO') {
    formData.endpointUrl = ''
    formData.headersString = ''
  } else if (formData.connectionType === 'HTTP') {
    formData.command = ''
    formData.workingDir = ''
  }

  // 触发验证
  formRef.value?.validateField('command')
  formRef.value?.validateField('endpointUrl')
}

// 处理认证类型变更
const handleAuthTypeChange = () => {
  // 切换认证类型时清空相关字段
  if (formData.authType !== 'API_KEY') {
    formData.apiKey = ''
    formData.apiKeyHeader = ''
  }
  if (formData.authType !== 'BASIC') {
    formData.basicUsername = ''
    formData.basicPassword = ''
  }
}

// 构建认证配置
const buildAuthConfig = (): string => {
  if (formData.authType === 'API_KEY') {
    return JSON.stringify({
      key: formData.apiKey,
      header: formData.apiKeyHeader || 'x-api-key'
    })
  } else if (formData.authType === 'BASIC') {
    return JSON.stringify({
      username: formData.basicUsername,
      password: formData.basicPassword
    })
  }
  return '{}'
}

// 解析认证配置（保留用于编辑模式，虽然当前 API 不返回此字段）
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const parseAuthConfig = (authConfig: string) => {
  try {
    const config = JSON.parse(authConfig)
    if (config.key) {
      formData.authType = 'API_KEY'
      formData.apiKey = config.key
      formData.apiKeyHeader = config.header || 'x-api-key'
    } else if (config.username) {
      formData.authType = 'BASIC'
      formData.basicUsername = config.username
      formData.basicPassword = config.password
    } else {
      formData.authType = 'NONE'
    }
  } catch {
    formData.authType = 'NONE'
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate()
  if (!valid) return

  const data: CreateMcpServerRequest | UpdateMcpServerRequest = {
    name: formData.name,
    description: formData.description || undefined,
    connectionType: formData.connectionType as 'STDIO' | 'HTTP',
    authConfig: buildAuthConfig()
  }

  // STDIO 配置
  if (formData.connectionType === 'STDIO') {
    ;(data as CreateMcpServerRequest).command = formData.command
    if (formData.workingDir) {
      ;(data as CreateMcpServerRequest).workingDir = formData.workingDir
    }
  }

  // HTTP 配置
  if (formData.connectionType === 'HTTP') {
    ;(data as CreateMcpServerRequest).endpointUrl = formData.endpointUrl
    if (formData.headersString) {
      ;(data as CreateMcpServerRequest).headers = formData.headersString
    }
  }

  emit('submit', data)
}

// 重置表单
const reset = () => {
  formRef.value?.resetFields()
  formData.name = ''
  formData.description = ''
  formData.connectionType = ''
  formData.command = ''
  formData.workingDir = ''
  formData.endpointUrl = ''
  formData.headersString = ''
  formData.authType = 'NONE'
  formData.apiKey = ''
  formData.apiKeyHeader = ''
  formData.basicUsername = ''
  formData.basicPassword = ''
}

// 获取表单数据
const getData = () => {
  return formData
}

// 验证表单
const validate = async (): Promise<boolean> => {
  if (!formRef.value) return false
  return await formRef.value.validate()
}

// 监听 server prop 变化
watch(
  () => props.server,
  newServer => {
    if (newServer) {
      formData.name = newServer.name
      formData.description = newServer.description || ''
      formData.connectionType = newServer.connectionType

      // 解析连接配置
      if (newServer.connectionType === 'STDIO') {
        formData.command = newServer.command || ''
        formData.workingDir = newServer.workingDir || ''
      } else if (newServer.connectionType === 'HTTP') {
        formData.endpointUrl = newServer.endpointUrl || ''
        if (newServer.headers) {
          formData.headersString = newServer.headers
        }
      }
    } else {
      reset()
    }
  },
  { immediate: true }
)

// 暴露方法
defineExpose({
  validate,
  getData,
  reset,
  handleSubmit,
  parseAuthConfig
})
</script>

<style scoped>
.el-form {
  max-width: 800px;
}
</style>

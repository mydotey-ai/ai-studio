import { get, post, put, del } from './request'

export enum ModelConfigType {
  EMBEDDING = 'embedding',
  LLM = 'llm'
}

export const ModelConfigTypeLabels = {
  [ModelConfigType.EMBEDDING]: '向量模型',
  [ModelConfigType.LLM]: '大语言模型'
}

export interface ModelConfig {
  id: number
  orgId: number
  type: ModelConfigType
  name: string
  endpoint: string
  maskedApiKey: string
  model: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout: number
  enableStreaming?: boolean
  isDefault: boolean
  status: string
  description?: string
  createdBy?: number
  createdAt: string
  updatedAt: string
}

export interface CreateModelConfigRequest {
  name: string
  type: ModelConfigType
  endpoint: string
  apiKey: string
  model: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout?: number
  enableStreaming?: boolean
  isDefault?: boolean
  description?: string
}

export interface UpdateModelConfigRequest {
  name?: string
  type?: ModelConfigType
  endpoint?: string
  apiKey?: string
  model?: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout?: number
  enableStreaming?: boolean
  isDefault?: boolean
  description?: string
}

export const modelConfigApi = {
  // 获取模型配置列表
  getList: (type?: ModelConfigType) => {
    return get('/model-configs', {
      params: { type }
    }) as Promise<ModelConfig[]>
  },

  // 获取模型配置详情
  getById: (id: number) => {
    return get(`/model-configs/${id}`) as Promise<ModelConfig>
  },

  // 创建模型配置
  create: (data: CreateModelConfigRequest) => {
    return post('/model-configs', data) as Promise<ModelConfig>
  },

  // 更新模型配置
  update: (id: number, data: UpdateModelConfigRequest) => {
    return put(`/model-configs/${id}`, data) as Promise<ModelConfig>
  },

  // 删除模型配置
  delete: (id: number) => {
    return del(`/model-configs/${id}`)
  },

  // 设置默认配置
  setDefault: (id: number) => {
    return put(`/model-configs/${id}/set-default`)
  },

  // 测试配置
  test: (id: number) => {
    return post(`/model-configs/${id}/test`) as Promise<{ data: boolean }>
  }
}
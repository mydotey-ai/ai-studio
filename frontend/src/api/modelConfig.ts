import { get, post, put, del } from './request'

export enum enum ModelConfigType {
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
    return get<ModelConfig[]>('/api/model-configs', {
      params: { type }
    })
  },

  // 获取模型配置详情
  getById: (id: number) => {
    return get<ModelConfig>(`/api/model-configs/${id}`)
  },

  // 创建模型配置
  create: (data: CreateModelConfigRequest) => {
    return post<ModelConfig>('/api/model-configs', data)
  },

  // 更新模型配置
  update: (id: number, data: UpdateModelConfigRequest) => {
    return put<ModelConfig>(`/api/model-configs/${id}`, data)
  },

  // 删除模型配置
  delete: (id: number) => {
    return del(`/api/model-configs/${id}`)
  },

  // 设置默认配置
  setDefault: (id: number) => {
    return put(`/api/model-configs/${id}/set-default`)
  },

  // 测试配置
  test: (id: number) => {
    return post(`/api/model-configs/${id}/test`)
  }
}

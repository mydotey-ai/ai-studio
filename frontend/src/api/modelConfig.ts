import { get, post, put, del } from './request'

/**
 * 模型配置类型
 */
export enum ModelConfigType {
  EMBEDDING = 'embedding',
  LLM = 'llm'
}

/**
 * 模型配置
 */
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
  timeout?: number
  enableStreaming?: boolean
  isDefault?: boolean
  status: string
  description?: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

/**
 * 创建模型配置请求
 */
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

/**
 * 更新模型配置请求
 */
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

/**
 * 模型配置 DTO（用于默认配置返回）
 */
export interface ModelConfigDto {
  id: number
  orgId: number
  type: ModelConfigType
  name: string
  endpoint: string
  apiKey: string
  model: string
  dimension?: number
  temperature?: number
  maxTokens?: number
  timeout?: number
  enableStreaming?: boolean
  isDefault?: boolean
  status: string
  description?: string
}

/**
 * 获取组织的模型配置列表
 * @param type - 模型配置类型（可选）
 */
export function getModelConfigs(type?: ModelConfigType) {
  return get<ModelConfig[]>('/model-configs', {
    params: type ? { type } : undefined
  })
}

/**
 * 获取模型配置详情
 * @param id - 模型配置ID
 */
export function getModelConfig(id: number) {
  return get<ModelConfig>(`/model-configs/${id}`)
}

/**
 * 创建模型配置
 * @param data - 创建模型配置请求
 */
export function createModelConfig(data: CreateModelConfigRequest) {
  return post<ModelConfig>('/model-configs', data)
}

/**
 * 更新模型配置
 * @param id - 模型配置ID
 * @param data - 更新模型配置请求
 */
export function updateModelConfig(id: number, data: UpdateModelConfigRequest) {
  return put<ModelConfig>(`/model-configs/${id}`, data)
}

/**
 * 删除模型配置
 * @param id - 模型配置ID
 */
export function deleteModelConfig(id: number) {
  return del(`/model-configs/${id}`)
}

/**
 * 获取默认模型配置
 * @param type - 模型配置类型
 */
export function getDefaultModelConfig(type: ModelConfigType) {
  return get<ModelConfigDto>('/model-configs/default', {
    params: { type }
  })
}

/**
 * 设置默认模型配置
 * @param id - 模型配置ID
 */
export function setDefaultModelConfig(id: number) {
  return put(`/model-configs/${id}/default`)
}

/**
 * 测试模型配置是否有效
 * @param id - 模型配置ID
 */
export function testModelConfig(id: number) {
  return post<boolean>(`/model-configs/${id}/test`)
}

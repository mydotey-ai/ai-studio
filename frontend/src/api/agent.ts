import { get, post, put, del } from './request'
import type { PaginationParams, PaginationResponse } from '@/types/common'
import type { ModelConfig } from './modelConfig'

export interface Agent {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  isPublic: boolean
  workflowType: 'REACT' | 'LINEAR' | 'DAG'
  modelConfig: AgentModelConfig | string
  llmModelConfigId?: number
  llmModelConfig?: ModelConfig
  createdAt: string
  updatedAt: string
}

export interface AgentModelConfig {
  model: string
  temperature: number
  maxTokens: number
  topP: number
  endpoint?: string
  maskedApiKey?: string
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  workflowType?: string
  modelConfig?: Partial<AgentModelConfig> | string
  llmModelConfigId?: number
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface UpdateAgentRequest {
  name?: string
  description?: string
  systemPrompt?: string
  isPublic?: boolean
  modelConfig?: Partial<AgentModelConfig> | string
  llmModelConfigId?: number
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface AgentExecutionRequest {
  query: string
  stream?: boolean
}

export interface AgentExecutionResponse {
  answer: string
  thoughtSteps: ThoughtStep[]
  toolCalls: ToolCallResult[]
  isComplete: boolean
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

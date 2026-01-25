import { get, post, put, del } from './request'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export interface Agent {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  isPublic: boolean
  workflowType: 'REACT' | 'LINEAR' | 'DAG'
  modelConfig: AgentModelConfig | string
  createdAt: string
  updatedAt: string
}

export interface AgentModelConfig {
  model: string
  temperature: number
  maxTokens: number
  topP: number
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  workflowType?: string
  modelConfig?: Partial<AgentModelConfig> | string
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface UpdateAgentRequest {
  name?: string
  description?: string
  systemPrompt?: string
  isPublic?: boolean
  modelConfig?: Partial<AgentModelConfig> | string
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface AgentExecutionRequest {
  input: string
  stream?: boolean
}

export interface AgentExecutionResponse {
  executionId: string
  result: string
  steps: ExecutionStep[]
  finished: boolean
}

export interface ExecutionStep {
  step: number
  type: 'thought' | 'action' | 'observation'
  content: string
  toolName?: string
  toolArgs?: Record<string, any>
  toolResult?: any
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

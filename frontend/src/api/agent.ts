import { get, post, put, del } from './request'
import type {
  Agent,
  CreateAgentRequest,
  UpdateAgentRequest,
  AgentExecutionRequest,
  AgentExecutionResponse
} from '@/types/agent'
import type { PaginationParams, PaginationResponse } from '@/types/common'

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

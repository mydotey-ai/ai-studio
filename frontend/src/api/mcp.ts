import { get, post, put, del } from './request'
import type {
  McpServer,
  McpServerListItem,
  CreateMcpServerRequest,
  UpdateMcpServerRequest,
  McpTool,
  TestConnectionResult
} from '@/types/mcp'

export type {
  McpServer,
  McpServerListItem,
  CreateMcpServerRequest,
  UpdateMcpServerRequest,
  McpTool,
  TestConnectionResult
}

export function getMcpServers(): Promise<McpServerListItem[]> {
  return get<McpServerListItem[]>('/mcp/servers')
}

export function getMcpServer(id: number): Promise<McpServer> {
  return get<McpServer>(`/mcp/servers/${id}`)
}

export function createMcpServer(data: CreateMcpServerRequest): Promise<McpServer> {
  return post<McpServer>('/mcp/servers', data)
}

export function updateMcpServer(id: number, data: UpdateMcpServerRequest): Promise<void> {
  return put<void>(`/mcp/servers/${id}`, data)
}

export function deleteMcpServer(id: number): Promise<void> {
  return del<void>(`/mcp/servers/${id}`)
}

export function syncTools(serverId: number): Promise<void> {
  return post<void>(`/mcp/servers/${serverId}/sync-tools`)
}

export function testConnection(serverId: number): Promise<TestConnectionResult> {
  return post<TestConnectionResult>(`/mcp/servers/${serverId}/test`)
}

export function getMcpTools(serverId: number): Promise<McpTool[]> {
  return get<McpTool[]>(`/mcp/servers/${serverId}/tools`)
}

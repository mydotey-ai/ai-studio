export interface McpServer {
  id: number
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR'
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface McpServerListItem extends McpServer {
  toolCount?: number
  lastSyncAt?: string
}

export interface CreateMcpServerRequest {
  name: string
  description?: string
  connectionType: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}

export interface UpdateMcpServerRequest {
  name?: string
  description?: string
  connectionType?: 'STDIO' | 'HTTP'
  command?: string
  workingDir?: string
  endpointUrl?: string
  headers?: string
  authType?: 'NONE' | 'API_KEY' | 'BASIC'
  authConfig?: string
}

export interface McpTool {
  id: number
  serverId: number
  toolName: string
  description?: string
  inputSchema: Record<string, unknown>
  outputSchema?: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface TestConnectionRequest {
  serverId: number
}

export interface TestConnectionResult {
  success: boolean
  message?: string
  responseTime?: number
}

export interface TestLog {
  id: number
  serverId: number
  timestamp: string
  success: boolean
  message: string
  error?: string
}

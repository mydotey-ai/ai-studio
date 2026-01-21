export enum WorkflowType {
  REACT = 'REACT',
  CUSTOM = 'CUSTOM'
}

export interface Agent {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  orgId: number
  isPublic: boolean
  modelConfig: string
  workflowType: WorkflowType
  workflowConfig: string
  maxIterations: number
  knowledgeBaseIds: number[]
  toolIds: number[]
  createdAt: string
  updatedAt: string
}

export interface CreateAgentRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  modelConfig: string
  workflowType?: WorkflowType
  workflowConfig?: string
  maxIterations?: number
  knowledgeBaseIds: number[]
  toolIds?: number[]
}

export interface UpdateAgentRequest {
  name?: string
  description?: string
  systemPrompt?: string
  isPublic?: boolean
  modelConfig?: string
  workflowType?: WorkflowType
  workflowConfig?: string
  maxIterations?: number
  knowledgeBaseIds?: number[]
  toolIds?: number[]
}

export interface AgentExecutionRequest {
  query: string
  context?: Record<string, unknown>
  stream?: boolean
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

export interface AgentExecutionResponse {
  answer: string
  thoughtSteps: ThoughtStep[]
  toolCalls: ToolCallResult[]
  isComplete: boolean
}

/**
 * 数据导出请求
 */
export interface DataExportRequest {
  scope: ExportScope
  knowledgeBaseIds?: number[]
  agentIds?: number[]
  chatbotIds?: number[]
  includeDocumentContent?: boolean
  includeConversations?: boolean
}

/**
 * 导出范围枚举
 */
export enum ExportScope {
  ALL = 'ALL',
  KNOWLEDGE_BASES = 'KNOWLEDGE_BASES',
  AGENTS = 'AGENTS',
  CHATBOTS = 'CHATBOTS',
  MCP_SERVERS = 'MCP_SERVERS'
}

/**
 * 导出范围标签
 */
export const ExportScopeLabels: Record<ExportScope, string> = {
  [ExportScope.ALL]: '全部数据',
  [ExportScope.KNOWLEDGE_BASES]: '知识库',
  [ExportScope.AGENTS]: 'Agent',
  [ExportScope.CHATBOTS]: '聊天机器人',
  [ExportScope.MCP_SERVERS]: 'MCP 服务器'
}

/**
 * 数据导出响应
 */
export interface DataExportResponse {
  taskId: number
  status: ExportStatus
  message?: string
  fileSize?: number
  downloadUrl?: string
  stats?: DataStats
}

/**
 * 导出状态枚举
 */
export enum ExportStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * 数据统计
 */
export interface DataStats {
  knowledgeBases: number
  documents: number
  agents: number
  chatbots: number
  conversations: number
  mcpServers: number
}

/**
 * 数据导入请求
 */
export interface DataImportRequest {
  strategy: ImportStrategy
  validateOnly: boolean
}

/**
 * 导入策略枚举
 */
export enum ImportStrategy {
  SKIP_EXISTING = 'SKIP_EXISTING',
  OVERWRITE = 'OVERWRITE',
  RENAME_CONFLICT = 'RENAME_CONFLICT'
}

/**
 * 导入策略标签
 */
export const ImportStrategyLabels: Record<ImportStrategy, string> = {
  [ImportStrategy.SKIP_EXISTING]: '跳过已存在',
  [ImportStrategy.OVERWRITE]: '覆盖已存在',
  [ImportStrategy.RENAME_CONFLICT]: '重命名冲突'
}

/**
 * 数据导入响应
 */
export interface DataImportResponse {
  taskId: number
  status: ImportStatus
  message?: string
  stats?: ImportStats
}

/**
 * 导入状态枚举
 */
export enum ImportStatus {
  VALIDATING = 'VALIDATING',
  VALIDATED = 'VALIDATED',
  IMPORTING = 'IMPORTING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * 导入统计
 */
export interface ImportStats {
  knowledgeBasesCreated: number
  knowledgeBasesSkipped: number
  agentsCreated: number
  agentsSkipped: number
  chatbotsCreated: number
  chatbotsSkipped: number
}

/**
 * 导出任务
 */
export interface ExportTask {
  id: number
  userId: number
  organizationId: number
  scope: string
  status: string
  fileSize: number
  errorMessage?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}

/**
 * 导入任务
 */
export interface ImportTask {
  id: number
  userId: number
  organizationId: number
  strategy: string
  status: string
  stats?: string
  errorMessage?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}

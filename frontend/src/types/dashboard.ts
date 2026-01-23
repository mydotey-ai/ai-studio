/**
 * 数据可视化仪表盘类型定义
 */

// 统计数据相关类型
export interface KnowledgeBaseStats {
  totalCount: number
  activeCount: number
  archivedCount: number
  weeklyGrowthRate: number
}

export interface AgentStats {
  totalCount: number
  reactCount: number
  workflowCount: number
  monthlyNewCount: number
}

export interface ChatbotStats {
  totalCount: number
  publishedCount: number
  draftCount: number
  totalConversations: number
}

export interface DocumentStats {
  totalCount: number
  processingCount: number
  completedCount: number
  totalSizeBytes: number
}

export interface UserStats {
  totalCount: number
  activeCount: number
  adminCount: number
  regularCount: number
  weeklyNewCount: number
}

export interface StorageStats {
  totalSizeBytes: number
  fileCount: number
  localCount: number
  ossCount: number
  s3Count: number
}

export interface DashboardStatistics {
  knowledgeBases: KnowledgeBaseStats
  agents: AgentStats
  chatbots: ChatbotStats
  documents: DocumentStats
  users: UserStats
  storage: StorageStats
}

// 趋势数据
export interface TrendData {
  date: string // YYYY-MM-DD
  apiCalls: number
  activeUsers: number
}

// 活动记录
export interface Activity {
  id: number
  action: string
  resourceType: string
  username: string
  createdAt: string
}

// 健康状态
export interface HealthStatus {
  apiSuccessRate: number
  avgResponseTime: number // 毫秒
  errorRate: number
}

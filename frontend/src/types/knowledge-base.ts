export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  ownerId: number
  isPublic: boolean
  embeddingModel?: string
  embeddingModelId?: number
  llmModelId?: number
  chunkSize: number
  chunkOverlap: number
  documentCount?: number
  createdAt: string
  updatedAt: string
}

export interface Document {
  id: number
  kbId: number
  filename: string
  fileUrl: string
  fileType: string
  fileSize: number
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  chunkCount: number
  sourceType?: string
  sourceUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBaseRequest {
  name: string
  description?: string
  isPublic?: boolean
  embeddingModelId?: number
  llmModelId?: number
  embeddingModel?: string
  chunkSize?: number
  chunkOverlap?: number
}

export interface UpdateKnowledgeBaseRequest {
  name?: string
  description?: string
  isPublic?: boolean
}

export interface WebCrawlTask {
  id: number
  kbId: number
  startUrl: string
  urlPattern?: string
  maxDepth: number
  crawlStrategy: 'BFS' | 'DFS'
  concurrentLimit: number
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'PAUSED'
  totalPages: number
  successPages: number
  failedPages: number
  errorMessage?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}

export interface CreateWebCrawlTaskRequest {
  kbId: number
  startUrl: string
  urlPattern?: string
  maxDepth: number
  crawlStrategy: 'BFS' | 'DFS'
  concurrentLimit: number
}

export interface WebCrawlProgressResponse {
  id: number
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'PAUSED'
  totalPages: number
  successPages: number
  failedPages: number
  currentUrl?: string
  errorMessage?: string
}

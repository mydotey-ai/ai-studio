export interface Chatbot {
  id: number
  agentId: number
  name: string
  description?: string
  welcomeMessage: string
  avatarUrl?: string
  ownerId: number
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  isPublished: boolean
  accessCount: number
  createdAt: string
  updatedAt: string
}

export interface ChatbotSettings {
  maxHistoryTurns: number
  showSources: boolean
  enableStreaming: boolean
  temperature: number
  maxTokens: number
}

export interface ChatbotStyleConfig {
  themeColor: string
  backgroundColor: string
  headerText: string
  logoUrl?: string
}

export interface CreateChatbotRequest {
  agentId: number
  name: string
  description?: string
  welcomeMessage?: string
  avatarUrl?: string
  settings?: Partial<ChatbotSettings>
  styleConfig?: Partial<ChatbotStyleConfig>
}

export interface UpdateChatbotRequest {
  name?: string
  description?: string
  welcomeMessage?: string
  avatarUrl?: string
  settings?: Partial<ChatbotSettings>
  styleConfig?: Partial<ChatbotStyleConfig>
  isPublished?: boolean
}

export interface ChatbotResponse {
  id: number
  agentId: number
  agentName: string
  name: string
  description?: string
  welcomeMessage: string
  avatarUrl?: string
  ownerId: number
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  isPublished: boolean
  accessCount: number
  createdAt: string
  updatedAt: string
}

export interface Conversation {
  id: number
  chatbotId: number
  userId?: number
  title?: string
  createdAt: string
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: Source[]
  toolCalls?: ToolCall[]
  metadata?: Record<string, any>
  createdAt: string
}

export interface Source {
  documentId: number
  documentName: string
  chunkIndex: number
  content: string
  similarityScore: number
}

export interface ToolCall {
  toolId: number
  toolName: string
  arguments: Record<string, any>
  result?: any
  error?: string
}

export interface ChatRequest {
  chatbotId: number
  conversationId?: number
  message: string
  stream?: boolean
}

export interface ChatResponse {
  conversationId: number
  messageId: number
  answer: string
  sources?: Source[]
  toolCalls?: ToolCall[]
  isComplete?: boolean
}

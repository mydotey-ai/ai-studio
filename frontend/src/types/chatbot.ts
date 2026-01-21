export interface ChatbotSettings {
  temperature?: number
  maxTokens?: number
  topP?: number
  topK?: number
  enableMemory?: boolean
  enableKnowledgeBase?: boolean
  knowledgeBaseIds?: number[]
  enableTools?: boolean
  toolIds?: number[]
  welcomeMessage?: string
  suggestedQuestions?: string[]
}

export interface ChatbotStyleConfig {
  primaryColor?: string
  backgroundColor?: string
  fontFamily?: string
  position?: 'bottom-right' | 'bottom-left' | 'custom'
  customPosition?: { x: number; y: number }
  headerTitle?: string
  headerSubtitle?: string
  avatarUrl?: string
  showBranding?: boolean
}

export interface Chatbot {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  orgId: number
  isPublic: boolean
  isPublished: boolean
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  modelConfig: string
  createdAt: string
  updatedAt: string
}

export interface CreateChatbotRequest {
  name: string
  description?: string
  systemPrompt: string
  isPublic?: boolean
  settings?: ChatbotSettings
  styleConfig?: ChatbotStyleConfig
  modelConfig: string
}

export interface UpdateChatbotRequest {
  name?: string
  description?: string
  systemPrompt?: string
  isPublic?: boolean
  settings?: ChatbotSettings
  styleConfig?: ChatbotStyleConfig
  modelConfig?: string
}

export interface ChatbotListItem extends Chatbot {
  agentName: string
  accessCount: number
}

export interface ChatbotResponse {
  id: number
  name: string
  description?: string
  systemPrompt: string
  ownerId: number
  orgId: number
  isPublic: boolean
  isPublished: boolean
  settings: ChatbotSettings
  styleConfig: ChatbotStyleConfig
  modelConfig: string
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
  messageCount?: number
}

export interface Source {
  type: 'knowledge_base' | 'tool' | 'url'
  id?: string
  name: string
  content?: string
  url?: string
  relevance?: number
}

export interface ToolCall {
  id: string
  name: string
  arguments: string
  result?: string
  status: 'pending' | 'success' | 'error'
  error?: string
}

export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'system'
  content: string
  sources?: Source[]
  toolCalls?: ToolCall[]
  createdAt: string
  metadata?: Record<string, unknown>
}

export interface ChatRequest {
  chatbotId: number
  conversationId?: number
  message: string
  stream?: boolean
  context?: Record<string, unknown>
}

export interface ChatResponse {
  conversationId: number
  messageId: number
  message: Message
  isComplete: boolean
}

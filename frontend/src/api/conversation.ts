import { get, post, del } from './request'
import type { Conversation, Message, ChatRequest, ChatResponse } from '@/types/chatbot'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getConversations(chatbotId: number, params?: PaginationParams) {
  return get<PaginationResponse<Conversation>>(`/chatbots/${chatbotId}/conversations`, { params })
}

export function getConversation(chatbotId: number, conversationId: number) {
  return get<{ conversation: Conversation; messages: Message[] }>(
    `/chatbots/${chatbotId}/conversations/${conversationId}`
  )
}

export function createConversation(chatbotId: number) {
  return post<Conversation>(`/chatbots/${chatbotId}/conversations`)
}

export function deleteConversation(conversationId: number) {
  return del(`/conversations/${conversationId}`)
}

export function sendMessage(data: ChatRequest) {
  return post<ChatResponse>('/chat', data)
}

export interface MessageCallback {
  (data: ChatResponse): void
}

export interface CompleteCallback {
  (): void
}

export interface ErrorCallback {
  (error: Error): void
}

export function sendMessageStream(
  data: ChatRequest,
  onMessage: MessageCallback,
  onComplete?: CompleteCallback,
  onError?: ErrorCallback
): EventSource {
  const token = localStorage.getItem('ai_studio_token')
  const baseURL = import.meta.env.VITE_API_BASE_URL

  // Build query parameters for SSE endpoint
  const params = new URLSearchParams()
  params.append('chatbotId', data.chatbotId.toString())
  if (data.conversationId) {
    params.append('conversationId', data.conversationId.toString())
  }
  params.append('message', data.message)
  params.append('stream', 'true')

  // Add context if provided
  if (data.context) {
    params.append('context', JSON.stringify(data.context))
  }

  // Add token to URL for authentication (EventSource doesn't support custom headers)
  if (token) {
    params.append('token', token)
  }

  const url = `${baseURL}/chat/stream?${params.toString()}`

  // Create EventSource
  const eventSource = new EventSource(url, {
    withCredentials: true
  })

  // Register event listeners
  eventSource.addEventListener('message', event => {
    try {
      const data = JSON.parse(event.data) as ChatResponse
      onMessage(data)
    } catch (err) {
      if (onError) {
        onError(err as Error)
      }
    }
  })

  eventSource.addEventListener('error', () => {
    eventSource.close()
    if (onError) {
      onError(new Error('Stream connection error'))
    }
  })

  eventSource.addEventListener('complete', () => {
    eventSource.close()
    if (onComplete) {
      onComplete()
    }
  })

  return eventSource
}

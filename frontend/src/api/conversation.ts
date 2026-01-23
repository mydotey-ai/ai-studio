import { get, post, del } from './request'
import { storage } from '@/utils/storage'
import type { Conversation, Message, ChatRequest } from '@/types/chatbot'

export function getConversations(chatbotId: number, params?: { page?: number; pageSize?: number }) {
  return get<{ records: Conversation[]; total: number }>(`/chatbots/${chatbotId}/conversations`, {
    params
  })
}

export function getConversation(conversationId: number) {
  return get<Conversation & { messages: Message[] }>(`/chatbots/conversations/${conversationId}`)
}

export function createConversation(chatbotId: number) {
  return post<Conversation>(`/chatbots/${chatbotId}/conversations`)
}

export function deleteConversation(conversationId: number) {
  return del(`/chatbots/conversations/${conversationId}`)
}

export function sendMessage(data: ChatRequest) {
  return post('/chatbots/chat', data)
}

export function sendMessageStream(
  data: ChatRequest,
  onMessage: (message: string) => void,
  onComplete: () => void,
  onError: (error: Error) => void
): EventSource {
  const url = `${import.meta.env.VITE_API_BASE_URL}/chatbots/chat/stream`
  let token: string | null = null
  try {
    token = storage.getToken()
  } catch (error) {
    console.warn('Failed to get token from storage:', error)
  }

  const params = new URLSearchParams({
    chatbotId: String(data.chatbotId),
    message: data.message,
    ...(data.conversationId && { conversationId: String(data.conversationId) }),
    ...(token && { token })
  })

  const eventSource = new EventSource(`${url}?${params.toString()}`)

  eventSource.onmessage = event => {
    if (event.data === '[DONE]') {
      onComplete()
      eventSource.close()
      return
    }

    try {
      const data = event.data
      if (data) {
        onMessage(data)
      }
    } catch (error) {
      onError(error as Error)
      eventSource.close()
    }
  }

  eventSource.onerror = () => {
    onError(new Error('Stream connection error'))
    eventSource.close()
  }

  return eventSource
}

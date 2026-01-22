import { get, post, del } from './request'
import type { Conversation, Message, ChatRequest } from '@/types/chatbot'

export function getConversations(chatbotId: number, params?: { page?: number; pageSize?: number }) {
  return get<{ records: Conversation[]; total: number }>(`/chatbots/${chatbotId}/conversations`, { params })
}

export function getConversation(chatbotId: number, conversationId: number) {
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

export function sendMessageStream(data: ChatRequest, onMessage: (message: string) => void, onComplete: () => void, onError: (error: Error) => void): EventSource {
  const url = `${import.meta.env.VITE_API_BASE_URL}/chatbots/chat/stream`
  const token = localStorage.getItem('ai_studio_token')
  const params = new URLSearchParams({
    chatbotId: String(data.chatbotId),
    message: data.message,
    ...(data.conversationId && { conversationId: String(data.conversationId) })
  })

  const eventSource = new EventSource(`${url}?${params.toString()}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  } as any)

  eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
      onComplete()
      eventSource.close()
      return
    }

    try {
      const data = JSON.parse(event.data)
      if (data.content) {
        onMessage(data.content)
      }
      if (data.finished) {
        onComplete()
        eventSource.close()
      }
    } catch (error) {
      onError(error as Error)
      eventSource.close()
    }
  }

  eventSource.onerror = (error) => {
    onError(new Error('Stream connection error'))
    eventSource.close()
  }

  return eventSource
}

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

interface StreamController {
  abort: () => void
}

export function sendMessageStream(
  data: ChatRequest,
  onMessage: (message: string) => void,
  onComplete: () => void,
  onError: (error: Error) => void
): StreamController {
  const url = `${import.meta.env.VITE_API_BASE_URL}/chatbots/chat/stream`
  let token: string | null = null
  try {
    token = storage.getToken()
  } catch (error) {
    console.warn('Failed to get token from storage:', error)
  }

  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const controller = new AbortController()

  fetch(url, {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
    signal: controller.signal
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const body = response.body
      if (!body) {
        throw new Error('Response body is not readable')
      }

      const reader = body.getReader()
      const decoder = new TextDecoder()

      function read() {
        reader
          .read()
          .then(({ done, value }) => {
            if (done) {
              onComplete()
              return
            }

            const chunk = decoder.decode(value, { stream: true })
            const lines = chunk.split('\n')

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6)
                if (data === '[DONE]') {
                  onComplete()
                  return
                }
                if (data && data.trim()) {
                  try {
                    // 解析 SSE 数据格式
                    const unescapedData = data.replace(/\\n/g, '\n').replace(/\\"/g, '"')
                    onMessage(unescapedData)
                  } catch (error) {
                    console.error('Failed to parse SSE data:', error)
                  }
                }
              }
            }

            read()
          })
          .catch(error => {
            if (error.name !== 'AbortError') {
              onError(error)
            }
          })
      }

      read()
    })
    .catch(error => {
      if (error.name !== 'AbortError') {
        onError(error)
      }
    })

  return {
    abort: () => controller.abort()
  }
}

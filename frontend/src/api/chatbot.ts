import { get, post, put, del } from './request'
import type { ChatbotResponse, CreateChatbotRequest, UpdateChatbotRequest } from '@/types/chatbot'

export function getChatbots(params?: {
  page?: number
  pageSize?: number
}, config?: any): Promise<ChatbotResponse[]> {
  return get('/chatbots/my', { ...config, params })
}

export function getPublishedChatbots(params?: {
  page?: number
  pageSize?: number
}, config?: any): Promise<ChatbotResponse[]> {
  return get('/chatbots/published', { ...config, params })
}

export function getChatbot(id: number) {
  return get<ChatbotResponse>(`/chatbots/${id}`)
}

export function createChatbot(data: CreateChatbotRequest) {
  return post<ChatbotResponse>('/chatbots', data)
}

export function updateChatbot(id: number, data: UpdateChatbotRequest) {
  return put<ChatbotResponse>(`/chatbots/${id}`, data)
}

export function deleteChatbot(id: number) {
  return del(`/chatbots/${id}`)
}

export function publishChatbot(id: number) {
  return post<ChatbotResponse>(`/chatbots/${id}/publish`)
}

export function unpublishChatbot(id: number) {
  return post<ChatbotResponse>(`/chatbots/${id}/unpublish`)
}

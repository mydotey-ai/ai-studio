import { get, post, put, del } from './request'
import type {
  Chatbot,
  ChatbotListItem,
  CreateChatbotRequest,
  UpdateChatbotRequest
} from '@/types/chatbot'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getChatbots(params?: PaginationParams) {
  return get<PaginationResponse<ChatbotListItem>>('/chatbots', { params })
}

export function getPublishedChatbots(params?: PaginationParams) {
  return get<PaginationResponse<ChatbotListItem>>('/chatbots/published', { params })
}

export function getChatbot(id: number) {
  return get<Chatbot>(`/chatbots/${id}`)
}

export function createChatbot(data: CreateChatbotRequest) {
  return post<Chatbot>('/chatbots', data)
}

export function updateChatbot(id: number, data: UpdateChatbotRequest) {
  return put<Chatbot>(`/chatbots/${id}`, data)
}

export function deleteChatbot(id: number) {
  return del(`/chatbots/${id}`)
}

export function publishChatbot(id: number) {
  return post(`/chatbots/${id}/publish`)
}

export function unpublishChatbot(id: number) {
  return post(`/chatbots/${id}/unpublish`)
}

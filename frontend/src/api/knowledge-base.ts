import { get, post, put, del } from './request'
import type {
  KnowledgeBase,
  CreateKnowledgeBaseRequest,
  UpdateKnowledgeBaseRequest
} from '@/types/knowledge-base'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getKnowledgeBases(params?: PaginationParams, skipCache: boolean = false) {
  return get<PaginationResponse<KnowledgeBase>>('/knowledge-bases', {
    params,
    headers: skipCache ? { 'X-Skip-Cache': 'true' } : undefined
  })
}

export function getKnowledgeBase(id: number) {
  return get<KnowledgeBase>(`/knowledge-bases/${id}`)
}

export function createKnowledgeBase(data: CreateKnowledgeBaseRequest) {
  return post<KnowledgeBase>('/knowledge-bases', data)
}

export function updateKnowledgeBase(id: number, data: UpdateKnowledgeBaseRequest) {
  return put<KnowledgeBase>(`/knowledge-bases/${id}`, data)
}

export function deleteKnowledgeBase(id: number) {
  return del(`/knowledge-bases/${id}`)
}

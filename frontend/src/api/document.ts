import { get, post, del } from './request'
import type { Document, WebCrawlTask, CreateWebCrawlTaskRequest, WebCrawlProgressResponse } from '@/types/knowledge-base'
import type { PaginationParams, PaginationResponse } from '@/types/common'

export function getDocuments(kbId: number, params?: PaginationParams) {
  return get<PaginationResponse<Document>>(`/knowledge-bases/${kbId}/documents`, {
    params
  })
}

export function uploadDocument(kbId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<Document>(`/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteDocument(kbId: number, docId: number) {
  return del(`/knowledge-bases/${kbId}/documents/${docId}`)
}

export function getWebCrawlTasks(kbId: number) {
  return get<WebCrawlTask[]>(`/web-crawl/tasks/kb/${kbId}`)
}

export function createWebCrawlTask(data: CreateWebCrawlTaskRequest) {
  return post<WebCrawlTask>('/web-crawl/tasks', data)
}

export function startWebCrawlTask(taskId: number) {
  return post(`/web-crawl/tasks/${taskId}/start`)
}

export function getWebCrawlTaskProgress(taskId: number) {
  return get<WebCrawlProgressResponse>(`/web-crawl/tasks/${taskId}/progress`)
}

export function deleteWebCrawlTask(taskId: number) {
  return del(`/web-crawl/tasks/${taskId}`)
}

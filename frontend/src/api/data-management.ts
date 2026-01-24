import request from './request'
import type {
  DataExportRequest,
  DataExportResponse,
  DataImportResponse,
  ExportTask,
  ImportTask
} from '@/types/data-management'

/**
 * 创建导出任务（异步）
 */
export function createExportTask(data: DataExportRequest) {
  return request.post<DataExportResponse>('/data-management/export', data)
}

/**
 * 同步导出数据（直接返回 JSON）
 */
export function exportDataSync(data: DataExportRequest) {
  const token = localStorage.getItem('token')
  return fetch('/api/data-management/export/sync', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
}

/**
 * 获取导出任务状态
 */
export function getExportStatus(taskId: number) {
  return request.get<DataExportResponse>(`/data-management/export/${taskId}/status`)
}

/**
 * 获取导出任务列表
 */
export function getExportTasks() {
  return request.get<ExportTask[]>('/data-management/export/tasks')
}

/**
 * 创建导入任务
 */
export function createImportTask(file: File, strategy: string, validateOnly: boolean) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('strategy', strategy)
  formData.append('validateOnly', String(validateOnly))

  return request.post<DataImportResponse>('/data-management/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取导入任务状态
 */
export function getImportStatus(taskId: number) {
  return request.get<DataImportResponse>(`/data-management/import/${taskId}/status`)
}

/**
 * 获取导入任务列表
 */
export function getImportTasks() {
  return request.get<ImportTask[]>('/data-management/import/tasks')
}

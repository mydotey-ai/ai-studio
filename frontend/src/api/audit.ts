import { get } from './request'
import type { AuditLogQuery, AuditLogListResponse } from '@/types/audit'

export const auditApi = {
  queryAuditLogs(query: AuditLogQuery) {
    return get<AuditLogListResponse>('/audit-logs', { params: query })
  }
}

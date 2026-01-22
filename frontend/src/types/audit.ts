/**
 * Audit log entry representing a system action
 */
export interface AuditLog {
  /** Unique identifier for the audit log entry */
  id: number
  /** ID of the user who performed the action */
  userId: number
  /** Username of the user who performed the action */
  username: string
  /** The action performed (e.g., CREATE, UPDATE, DELETE, LOGIN, LOGOUT) */
  action: string
  /** Type of resource affected (e.g., USER, AGENT, CHATBOT, KNOWLEDGE_BASE) */
  resourceType: string
  /** Optional ID of the specific resource affected */
  resourceId?: number
  /** Additional details about the action (JSON string or text) */
  details?: string
  /** IP address from which the action was performed */
  ipAddress?: string
  /** User agent string of the client */
  userAgent?: string
  /** Timestamp when the action was performed (ISO 8601 format) */
  createdAt: string
}

/**
 * Query parameters for filtering audit logs
 */
export interface AuditLogQuery {
  /** Filter by user ID */
  userId?: number
  /** Filter by action type */
  action?: string
  /** Filter by resource type */
  resourceType?: string
  /** Filter by resource ID */
  resourceId?: number
  /** Filter by start date (ISO 8601 format) */
  startDate?: string
  /** Filter by end date (ISO 8601 format) */
  endDate?: string
  /** Page number (default: 1) */
  page?: number
  /** Number of items per page (default: 10) */
  pageSize?: number
}

/**
 * Paginated response for audit log list
 */
export interface AuditLogListResponse {
  /** List of audit log records */
  records: AuditLog[]
  /** Total number of records */
  total: number
  /** Current page number */
  current: number
  /** Number of items per page */
  size: number
}

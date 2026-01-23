/**
 * File metadata representation
 */
export interface FileMetadata {
  /** Unique identifier for the file metadata */
  id: number
  /** Stored file name */
  fileName: string
  /** Original uploaded file name */
  originalFileName: string
  /** Full file path in storage */
  filePath: string
  /** File size in bytes */
  fileSize: number
  /** Content type (MIME type) */
  contentType: string
  /** Storage type (LOCAL, OSS, S3) */
  storageType: string
  /** ID of the user who uploaded the file */
  uploadedBy: number
  /** Related entity type (e.g., "KNOWLEDGE_BASE", "CONVERSATION") */
  relatedEntityType?: string
  /** Related entity ID */
  relatedEntityId?: number
  /** Creation timestamp (ISO 8601 format) */
  createdAt: string
}

/**
 * File upload response
 */
export interface FileUploadResponse {
  /** File metadata ID */
  id: number
  /** Stored file name */
  fileName: string
  /** Original uploaded file name */
  originalFileName: string
  /** Full file path in storage */
  filePath: string
  /** File size in bytes */
  fileSize: number
  /** Content type (MIME type) */
  contentType: string
  /** Storage type (LOCAL, OSS, S3) */
  storageType: string
  /** URL to access the file (presigned or direct) */
  fileUrl: string
  /** ID of the user who uploaded the file */
  uploadedBy: number
  /** Upload timestamp (ISO 8601 format) */
  createdAt: string
}

/**
 * File upload request parameters
 */
export interface FileUploadRequest {
  /** File to upload */
  file: File
  /** Related entity type (optional) */
  relatedEntityType?: string
  /** Related entity ID (optional) */
  relatedEntityId?: number
}

/**
 * File list query parameters
 */
export interface FileListQuery {
  /** Page number (1-indexed) */
  page?: number
  /** Page size */
  pageSize?: number
  /** Search keyword (filename) */
  search?: string
  /** Filter by content type */
  contentType?: string
  /** Filter by storage type */
  storageType?: string
}

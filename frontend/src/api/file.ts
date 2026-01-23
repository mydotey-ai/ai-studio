import { get, post, del } from './request'
import type { FileMetadata, FileUploadResponse } from '@/types/file'

export const fileApi = {
  /**
   * Upload a file
   * @param file File to upload
   * @param relatedEntityType Optional related entity type
   * @param relatedEntityId Optional related entity ID
   * @returns Upload response with file metadata and URL
   */
  uploadFile(file: File, relatedEntityType?: string, relatedEntityId?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (relatedEntityType) {
      formData.append('relatedEntityType', relatedEntityType)
    }
    if (relatedEntityId) {
      formData.append('relatedEntityId', relatedEntityId.toString())
    }

    return post<FileUploadResponse>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * Get file metadata by ID
   * @param id File metadata ID
   * @returns File metadata
   */
  getFileMetadata(id: number) {
    return get<FileMetadata>(`/files/${id}`)
  },

  /**
   * Get current user's files
   * @returns List of file metadata
   */
  getMyFiles() {
    return get<FileMetadata[]>('/files/my')
  },

  /**
   * Get files related to an entity
   * @param entityType Entity type (e.g., "KNOWLEDGE_BASE")
   * @param entityId Entity ID
   * @returns List of file metadata
   */
  getRelatedFiles(entityType: string, entityId: number) {
    return get<FileMetadata[]>(`/files/related/${entityType}/${entityId}`)
  },

  /**
   * Get file download URL
   * @param id File metadata ID
   * @param expirationSeconds URL expiration time in seconds (default: 3600)
   * @returns Signed URL for file access
   */
  getFileUrl(id: number, expirationSeconds: number = 3600) {
    return get<string>(`/files/${id}/url`, {
      params: { expirationSeconds }
    })
  },

  /**
   * Get direct download link for browser
   * @param id File metadata ID
   * @returns Direct download URL
   */
  getDownloadUrl(id: number) {
    return `${import.meta.env.VITE_API_BASE_URL}/files/download/${id}`
  },

  /**
   * Delete a file
   * @param id File metadata ID
   */
  deleteFile(id: number) {
    return del(`/files/${id}`)
  }
}

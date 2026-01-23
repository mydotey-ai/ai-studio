import dayjs from 'dayjs'

/**
 * Format file size to human readable string
 * @param bytes File size in bytes
 * @returns Formatted string (e.g., "1.5 MB")
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

/**
 * Format date to readable string
 * @param dateString ISO 8601 date string
 * @returns Formatted date string (e.g., "2026-01-24 12:34")
 */
export function formatDate(dateString: string): string {
  return dayjs(dateString).format('YYYY-MM-DD HH:mm')
}

/**
 * Get file icon based on content type
 * @param contentType MIME type
 * @returns Element Plus icon name
 */
export function getFileIcon(contentType: string): string {
  if (contentType.startsWith('image/')) return 'Picture'
  if (contentType === 'application/pdf') return 'Document'
  if (contentType.includes('word')) return 'Document'
  if (contentType.includes('excel') || contentType.includes('spreadsheet')) return 'Tickets'
  if (contentType.includes('powerpoint') || contentType.includes('presentation')) return 'Notebook'
  if (contentType.startsWith('text/')) return 'Document'
  if (contentType.includes('zip') || contentType.includes('rar')) return 'Folder'
  if (contentType.startsWith('video/')) return 'VideoCamera'
  if (contentType.startsWith('audio/')) return 'Headset'
  return 'Document'
}

/**
 * Check if file can be previewed
 * @param contentType MIME type
 * @returns true if file can be previewed
 */
export function canPreview(contentType: string): boolean {
  return (
    contentType.startsWith('image/') ||
    contentType === 'application/pdf' ||
    contentType.startsWith('text/')
  )
}

/**
 * Validate file size
 * @param file File to validate
 * @param maxSize Maximum size in bytes
 * @returns true if file size is valid
 */
export function validateFileSize(file: File, maxSize: number): boolean {
  return file.size <= maxSize
}

/**
 * Validate file type
 * @param file File to validate
 * @param allowedTypes Array of allowed MIME types
 * @returns true if file type is allowed
 */
export function validateFileType(file: File, allowedTypes: string[]): boolean {
  return allowedTypes.includes(file.type)
}

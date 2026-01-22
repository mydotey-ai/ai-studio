/**
 * Storage type enumeration
 */
export enum StorageType {
  /** Local filesystem storage */
  LOCAL = 'LOCAL',
  /** Alibaba Cloud Object Storage Service */
  OSS = 'OSS',
  /** Amazon S3 compatible storage */
  S3 = 'S3'
}

/**
 * Storage configuration representing a storage provider
 */
export interface StorageConfig {
  /** Unique identifier for the storage configuration */
  id: number
  /** Type of storage (LOCAL, OSS, S3) - using string to match backend */
  storageType: string
  /** Description of the storage configuration */
  description?: string
  /** Storage endpoint URL (for OSS/S3) */
  endpoint?: string
  /** Bucket name (for OSS/S3) */
  bucketName?: string
  /** Storage region (for OSS/S3) */
  region?: string
  /** Access key for authentication (for OSS/S3) */
  accessKey?: string
  /** Secret key for authentication (for OSS/S3) */
  secretKey?: string
  /** Whether this is the default storage configuration */
  isDefault: boolean
  /** User ID of the creator */
  createdBy: number
  /** Timestamp when the configuration was created (ISO 8601 format) */
  createdAt: string
  /** Timestamp when the configuration was last updated (ISO 8601 format) */
  updatedAt: string
}

/**
 * Request payload for creating a new storage configuration
 */
export interface CreateStorageConfigRequest {
  /** Type of storage (LOCAL, OSS, S3) - using string to match backend */
  storageType: string
  /** Description of the storage configuration */
  description?: string
  /** Storage endpoint URL (for OSS/S3) */
  endpoint?: string
  /** Bucket name (for OSS/S3) */
  bucketName?: string
  /** Storage region (for OSS/S3) */
  region?: string
  /** Access key for authentication (for OSS/S3) */
  accessKey?: string
  /** Secret key for authentication (for OSS/S3) */
  secretKey?: string
  /** Whether to set this as the default storage */
  isDefault?: boolean
}

/**
 * Request payload for updating an existing storage configuration
 */
export interface UpdateStorageConfigRequest {
  /** Description of the storage configuration */
  description?: string
  /** Storage endpoint URL (for OSS/S3) */
  endpoint?: string
  /** Bucket name (for OSS/S3) */
  bucketName?: string
  /** Storage region (for OSS/S3) */
  region?: string
  /** Access key for authentication (for OSS/S3) */
  accessKey?: string
  /** Secret key for authentication (for OSS/S3) */
  secretKey?: string
}

package com.mydotey.ai.studio.service.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * File metadata returned by storage operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    /**
     * Stored file name (may include UUID or timestamp)
     */
    private String fileName;

    /**
     * Original uploaded file name
     */
    private String originalFileName;

    /**
     * Unique file key for retrieval
     */
    private String fileKey;

    /**
     * Full file path in storage
     */
    private String filePath;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Content type (MIME type)
     */
    private String contentType;

    /**
     * Bucket/container name
     */
    private String bucketName;

    /**
     * Storage type (LOCAL, S3, MINIO, OSS, etc.)
     */
    private String storageType;
}

package com.mydotey.ai.studio.dto.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for file upload operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * File metadata ID
     */
    private Long id;

    /**
     * Stored file name
     */
    private String fileName;

    /**
     * Original uploaded file name
     */
    private String originalFileName;

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
     * Storage type (LOCAL, S3, MINIO, OSS, etc.)
     */
    private String storageType;

    /**
     * URL to access the file (presigned or direct)
     */
    private String fileUrl;

    /**
     * ID of the user who uploaded the file
     */
    private Long uploadedBy;

    /**
     * Upload timestamp
     */
    private Instant createdAt;
}

package com.mydotey.ai.studio.service.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * File Storage Service Interface
 * Defines the contract for all storage implementations (Local, S3, MinIO, OSS, etc.)
 */
public interface FileStorageService {

    /**
     * Upload a file using MultipartFile
     *
     * @param file the file to upload
     * @param path the target path (e.g., "documents", "avatars", etc.)
     * @return FileInfo containing metadata about the uploaded file
     */
    FileInfo uploadFile(MultipartFile file, String path);

    /**
     * Upload a file using InputStream
     *
     * @param inputStream the file input stream
     * @param fileName the name of the file
     * @param contentType the content type (MIME type)
     * @param path the target path (e.g., "documents", "avatars", etc.)
     * @return FileInfo containing metadata about the uploaded file
     */
    FileInfo uploadFile(InputStream inputStream, String fileName, String contentType, String path);

    /**
     * Download a file by its key
     *
     * @param fileKey the unique file key
     * @return InputStream of the file content
     */
    InputStream downloadFile(String fileKey);

    /**
     * Delete a file by its key
     *
     * @param fileKey the unique file key
     */
    void deleteFile(String fileKey);

    /**
     * Generate a presigned URL for temporary access
     *
     * @param fileKey the unique file key
     * @param expirationInSeconds URL expiration time in seconds
     * @return presigned URL string
     */
    String getPresignedUrl(String fileKey, long expirationInSeconds);

    /**
     * Check if a file exists
     *
     * @param fileKey the unique file key
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String fileKey);

    /**
     * Get the storage type identifier
     *
     * @return storage type (e.g., "LOCAL", "S3", "MINIO", "OSS")
     */
    String getStorageType();
}

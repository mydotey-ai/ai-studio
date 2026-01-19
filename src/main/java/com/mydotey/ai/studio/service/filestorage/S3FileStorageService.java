package com.mydotey.ai.studio.service.filestorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public S3FileStorageService(String endpoint, String accessKeyId, String secretKey, String region, String bucketName) {
        this.region = region;
        this.bucketName = bucketName;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(() ->
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            accessKeyId,
                            secretKey
                    )
                )
                .build();

        // 检查 bucket 是否存在
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Created S3 bucket: {}", bucketName);
        }
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, String path) {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        String objectKey = buildObjectKey(path, uniqueFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            log.info("File uploaded to S3: bucket={}, key={}", bucketName, objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileKey(objectKey)
                .filePath(objectKey)
                .fileSize(file.getSize())
                .contentType(contentType)
                .bucketName(bucketName)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public FileInfo uploadFile(InputStream inputStream, String fileName, String contentType, String path) {
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = buildObjectKey(path, uniqueFileName);

        try {
            byte[] bytes = inputStream.readAllBytes();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));

            log.info("File uploaded to S3: bucket={}, key={}", bucketName, objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(fileName)
                .fileKey(objectKey)
                .filePath(objectKey)
                .fileSize((long) 0) // Size not available without additional read
                .contentType(contentType)
                .bucketName(bucketName)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public InputStream downloadFile(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(request);
    }

    @Override
    public void deleteFile(String fileKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        s3Client.deleteObject(request);
        log.info("File deleted from S3: bucket={}, key={}", bucketName, fileKey);
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationInSeconds) {
        // For S3, presigned URLs require more complex setup with S3Presigner
        // For now, return a simple URL format
        return String.format("s3://%s/%s", bucketName, fileKey);
    }

    @Override
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "S3";
    }

    private String buildObjectKey(String path, String fileName) {
        String normalizedPath = path.replaceFirst("^/", "");
        return normalizedPath.isEmpty() ? fileName : normalizedPath + "/" + fileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }

    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}

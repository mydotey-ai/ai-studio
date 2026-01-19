package com.mydotey.ai.studio.service.filestorage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssFileStorageService implements FileStorageService {

    private final OSS ossClient;
    private final String bucketName;
    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;

    public OssFileStorageService(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 检查 bucket 是否存在，不存在则创建
        if (!ossClient.doesBucketExist(bucketName)) {
            ossClient.createBucket(bucketName);
            log.info("Created OSS bucket: {}", bucketName);
        }
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, String path) {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 构建 OSS 对象键
        String objectKey = buildObjectKey(path, uniqueFileName);

        try {
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);

            // 上传文件
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, file.getInputStream(), metadata);
            ossClient.putObject(request);

            log.info("File uploaded to OSS: bucket={}, key={}", bucketName, objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to OSS", e);
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
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);

            // 读取所有字节以获取文件大小
            byte[] bytes = inputStream.readAllBytes();
            metadata.setContentLength(bytes.length);

            PutObjectRequest request = new PutObjectRequest(
                    bucketName,
                    objectKey,
                    new ByteArrayInputStream(bytes),
                    metadata
            );
            ossClient.putObject(request);

            log.info("File uploaded to OSS: bucket={}, key={}", bucketName, objectKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to OSS", e);
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
        return ossClient.getObject(bucketName, fileKey).getObjectContent();
    }

    @Override
    public void deleteFile(String fileKey) {
        ossClient.deleteObject(bucketName, fileKey);
        log.info("File deleted from OSS: bucket={}, key={}", bucketName, fileKey);
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationInSeconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileKey);
        if (expirationInSeconds > 0) {
            Date expiration = new Date(System.currentTimeMillis() + expirationInSeconds * 1000);
            request.setExpiration(expiration);
        }
        URL url = ossClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public boolean fileExists(String fileKey) {
        return ossClient.doesObjectExist(bucketName, fileKey);
    }

    @Override
    public String getStorageType() {
        return "OSS";
    }

    private String buildObjectKey(String path, String fileName) {
        // 移除开头的斜杠并构建对象键
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

    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}

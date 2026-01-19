# Phase 8: File Storage System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现完整的文件存储系统，支持本地存储和云存储（阿里云 OSS、AWS S3），提供统一的文件管理 API

**Architecture:**
- **存储策略模式**：定义 FileStorage 接口，支持多种存储实现（本地、OSS、S3）
- **配置管理**：支持多个存储配置，通过 file_storage_config 表管理
- **文件元数据**：记录文件信息（路径、大小、类型、上传者等）
- **访问控制**：基于权限的文件访问控制
- **URL 签名**：云存储支持生成带签名的访问 URL（临时访问）

**Tech Stack:**
- Java 21 + Spring Boot 3.5
- MyBatis-Plus 3.5.7
- 阿里云 OSS SDK (aliyun-sdk-oss)
- AWS S3 SDK (aws-java-sdk-s3)
- 本地文件存储（NIO）

---

## Prerequisites

- Phase 1-7 backend infrastructure is complete ✅
- Database with file_storage_config table is available ✅
- Test database is configured
- Current FileUtil uses local storage only (needs refactoring)

---

## Task 1: Create File Metadata Entity and Mapper

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/FileMetadata.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/FileMetadataMapper.java`

**Step 1: Write the FileMetadata entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("file_metadata")
public class FileMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String originalFileName;

    private String filePath;

    private Long fileSize;

    private String contentType;

    private String storageType;

    private Long storageConfigId;

    private String bucketName;

    private String fileKey;

    private Long uploadedBy;

    private String relatedEntityType;

    private Long relatedEntityId;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Create database migration for file_metadata table**

Create: `src/main/resources/db/migration/V4__file_metadata.sql`

```sql
-- File metadata table
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path TEXT,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    storage_type VARCHAR(20) NOT NULL,
    storage_config_id BIGINT REFERENCES file_storage_config(id) ON DELETE SET NULL,
    bucket_name VARCHAR(255),
    file_key VARCHAR(500),
    uploaded_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_file_metadata_uploaded_by ON file_metadata(uploaded_by);
CREATE INDEX idx_file_metadata_related_entity ON file_metadata(related_entity_type, related_entity_id);
CREATE INDEX idx_file_metadata_storage_type ON file_metadata(storage_type);
```

**Step 3: Write the Mapper interface**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
}
```

**Step 4: Compile and verify**

Run: `mvn compile`
Expected: SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/FileMetadata.java
git add src/main/java/com/mydotey/ai/studio/mapper/FileMetadataMapper.java
git add src/main/resources/db/migration/V4__file_metadata.sql
git commit -m "feat: add file metadata entity and migration"
```

---

## Task 2: Create File Storage Configuration Entity and Mapper

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/entity/FileStorageConfig.java`
- Create: `src/main/java/com/mydotey/ai/studio/mapper/FileStorageConfigMapper.java`

**Step 1: Write the FileStorageConfig entity**

```java
package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("file_storage_config")
public class FileStorageConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String storageType;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private String region;

    private Boolean isDefault;

    private String description;

    private Long createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}
```

**Step 2: Write the Mapper interface**

```java
package com.mydotey.ai.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mydotey.ai.studio.entity.FileStorageConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileStorageConfigMapper extends BaseMapper<FileStorageConfig> {
}
```

**Step 3: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/entity/FileStorageConfig.java
git add src/main/java/com/mydotey/ai/studio/mapper/FileStorageConfigMapper.java
git commit -m "feat: add file storage config entity and mapper"
```

---

## Task 3: Create File Storage Interface and DTOs

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/FileStorageService.java` (interface)
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/FileUploadRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/FileUploadResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/FileMetadataResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/FileInfo.java`

**Step 1: Write the FileStorageService interface**

```java
package com.mydotey.ai.studio.service.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileStorageService {

    /**
     * 上传文件
     * @param file 文件
     * @param path 存储路径
     * @return 文件信息
     */
    FileInfo uploadFile(MultipartFile file, String path) throws Exception;

    /**
     * 上传文件（使用 InputStream）
     * @param inputStream 输入流
     * @param fileName 文件名
     * @param path 存储路径
     * @param contentType 内容类型
     * @return 文件信息
     */
    FileInfo uploadFile(InputStream inputStream, String fileName, String path, String contentType) throws Exception;

    /**
     * 下载文件
     * @param fileKey 文件键
     * @return 文件输入流
     */
    InputStream downloadFile(String fileKey) throws Exception;

    /**
     * 删除文件
     * @param fileKey 文件键
     */
    void deleteFile(String fileKey) throws Exception;

    /**
     * 获取文件访问 URL
     * @param fileKey 文件键
     * @param expirationSeconds 过期时间（秒），0 表示永不过期
     * @return 访问 URL
     */
    String getPresignedUrl(String fileKey, long expirationSeconds) throws Exception;

    /**
     * 检查文件是否存在
     * @param fileKey 文件键
     * @return 是否存在
     */
    boolean fileExists(String fileKey) throws Exception;

    /**
     * 获取存储类型
     */
    String getStorageType();
}
```

**Step 2: Write the FileInfo class**

```java
package com.mydotey.ai.studio.service.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileName;
    private String originalFileName;
    private String fileKey;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String bucketName;
    private String storageType;
}
```

**Step 3: Write FileUploadRequest DTO**

```java
package com.mydotey.ai.studio.dto.filestorage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FileUploadRequest {
    @NotBlank(message = "File path is required")
    @Size(max = 500, message = "File path must not exceed 500 characters")
    private String path;

    private String relatedEntityType;

    private Long relatedEntityId;

    private Long storageConfigId;
}
```

**Step 4: Write FileUploadResponse DTO**

```java
package com.mydotey.ai.studio.dto.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private String fileUrl;
    private Long uploadedBy;
    private Instant createdAt;
}
```

**Step 5: Write FileMetadataResponse DTO**

```java
package com.mydotey.ai.studio.dto.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private Long uploadedBy;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Instant createdAt;
}
```

**Step 6: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/filestorage/FileStorageService.java
git add src/main/java/com/mydotey/ai/studio/service/filestorage/FileInfo.java
git add src/main/java/com/mydotey/ai/studio/dto/filestorage/
git commit -m "feat: add file storage interface and DTOs"
```

---

## Task 4: Implement Local File Storage Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/LocalFileStorageService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/filestorage/LocalFileStorageServiceTest.java`

**Step 1: Write the test class**

```java
package com.mydotey.ai.studio.service.filestorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("本地文件存储服务测试")
class LocalFileStorageServiceTest {

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileInfo fileInfo = fileStorageService.uploadFile(file, "/test");

        assertNotNull(fileInfo);
        assertEquals("test.txt", fileInfo.getOriginalFileName());
        assertEquals("text/plain", fileInfo.getContentType());
        assertTrue(fileInfo.getFileSize() > 0);
        assertEquals("LOCAL", fileInfo.getStorageType());
    }

    @Test
    @DisplayName("应该能够下载文件")
    void testDownloadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "download.txt",
                "download.txt",
                "text/plain",
                "Download test content".getBytes()
        );

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        var inputStream = fileStorageService.downloadFile(uploadedInfo.getFileKey());
        assertNotNull(inputStream);

        byte[] content = inputStream.readAllBytes();
        assertEquals("Download test content", new String(content));
    }

    @Test
    @DisplayName("应该能够删除文件")
    void testDeleteFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "delete.txt",
                "delete.txt",
                "text/plain",
                "Delete me".getBytes()
        );

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        fileStorageService.deleteFile(uploadedInfo.getFileKey());

        assertFalse(fileStorageService.fileExists(uploadedInfo.getFileKey()));
    }

    @Test
    @DisplayName("应该能够检查文件是否存在")
    void testFileExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "exists.txt",
                "exists.txt",
                "text/plain",
                "I exist".getBytes()
        );

        FileInfo uploadedInfo = fileStorageService.uploadFile(file, "/test");

        assertTrue(fileStorageService.fileExists(uploadedInfo.getFileKey()));
        assertFalse(fileStorageService.fileExists("non-existent-file.txt"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=LocalFileStorageServiceTest`
Expected: FAIL with class not found

**Step 3: Write the LocalFileStorageService implementation**

```java
package com.mydotey.ai.studio.service.filestorage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.storage.local.upload-dir:${java.io.tmpdir}/ai-studio-uploads}")
    private String uploadDir;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Paths.get(uploadDir);
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
            log.info("Created base upload directory: {}", uploadDir);
        }
    }

    @Override
    public FileInfo uploadFile(MultipartFile file, String path) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 构建完整路径
        Path fullPath = basePath.resolve(path.replaceFirst("^/", ""));
        Path filePath = fullPath.resolve(uniqueFileName);

        // 创建目录
        Files.createDirectories(fullPath);

        // 保存文件
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded: {}", filePath);

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(originalFileName)
                .fileKey(filePath.toString())
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .contentType(contentType)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public FileInfo uploadFile(InputStream inputStream, String fileName, String path, String contentType) throws Exception {
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        Path fullPath = basePath.resolve(path.replaceFirst("^/", ""));
        Path filePath = fullPath.resolve(uniqueFileName);

        Files.createDirectories(fullPath);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        File file = filePath.toFile();

        log.info("File uploaded: {}", filePath);

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(fileName)
                .fileKey(filePath.toString())
                .filePath(filePath.toString())
                .fileSize(file.length())
                .contentType(contentType)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public InputStream downloadFile(String fileKey) throws Exception {
        Path filePath = Paths.get(fileKey);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + fileKey);
        }
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void deleteFile(String fileKey) throws Exception {
        Path filePath = Paths.get(fileKey);
        Files.deleteIfExists(filePath);
        log.info("File deleted: {}", fileKey);
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationSeconds) throws Exception {
        // 本地存储不支持签名 URL，返回文件路径
        // 在实际应用中，应该通过一个下载端点来提供文件访问
        return "/api/files/download?key=" + fileKey;
    }

    @Override
    public boolean fileExists(String fileKey) throws Exception {
        return Files.exists(Paths.get(fileKey));
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
}
```

**Step 4: Update application-test.yml**

Add test configuration:

```yaml
file:
  storage:
    local:
      upload-dir: ${java.io.tmpdir}/ai-studio-test-uploads
```

**Step 5: Run test to verify it passes**

Run: `mvn test -Dtest=LocalFileStorageServiceTest`
Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/filestorage/LocalFileStorageService.java
git add src/test/java/com/mydotey/ai/studio/service/filestorage/LocalFileStorageServiceTest.java
git add src/main/resources/application-test.yml
git commit -m "feat: implement local file storage service"
```

---

## Task 5: Implement Aliyun OSS File Storage Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/OssFileStorageService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/filestorage/OssFileStorageServiceTest.java`

**Step 1: Add OSS dependency to pom.xml**

```xml
<!-- Aliyun OSS SDK -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```

**Step 2: Write the test class**

```java
package com.mydotey.ai.studio.service.filestorage;

import com.mydotey.ai.studio.entity.FileStorageConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("阿里云 OSS 文件存储服务测试")
class OssFileStorageServiceTest {

    // 注意：这些测试需要真实的 OSS 凭证才能运行
    // 在 CI/CD 环境中应该跳过或使用 mock

    @Test
    @DisplayName("应该能够上传文件到 OSS")
    void testUploadFile() {
        // 使用真实的 OSS 配置进行测试
        // 或者使用 Mockito mock OSS 客户端
    }

    @Test
    @DisplayName("应该能够生成签名 URL")
    void testGetPresignedUrl() {
        // 测试签名 URL 生成
    }
}
```

**Step 3: Write the OssFileStorageService implementation**

```java
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
    public FileInfo uploadFile(MultipartFile file, String path) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        // 构建 OSS 对象键
        String objectKey = buildObjectKey(path, uniqueFileName);

        // 设置元数据
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(contentType);

        // 上传文件
        PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, file.getInputStream(), metadata);
        ossClient.putObject(request);

        log.info("File uploaded to OSS: bucket={}, key={}", bucketName, objectKey);

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
    public FileInfo uploadFile(InputStream inputStream, String fileName, String path, String contentType) throws Exception {
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = buildObjectKey(path, uniqueFileName);

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

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(fileName)
                .fileKey(objectKey)
                .filePath(objectKey)
                .fileSize((long) bytes.length)
                .contentType(contentType)
                .bucketName(bucketName)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public InputStream downloadFile(String fileKey) throws Exception {
        return ossClient.getObject(bucketName, fileKey).getObjectContent();
    }

    @Override
    public void deleteFile(String fileKey) throws Exception {
        ossClient.deleteObject(bucketName, fileKey);
        log.info("File deleted from OSS: bucket={}, key={}", bucketName, fileKey);
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationSeconds) throws Exception {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileKey);
        if (expirationSeconds > 0) {
            Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
            request.setExpiration(expiration);
        }
        URL url = ossClient.generatePresignedUrl(request);
        return url.toString();
    }

    @Override
    public boolean fileExists(String fileKey) throws Exception {
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
```

**Step 4: Commit**

```bash
git add pom.xml
git add src/main/java/com/mydotey/ai/studio/service/filestorage/OssFileStorageService.java
git add src/test/java/com/mydotey/ai/studio/service/filestorage/OssFileStorageServiceTest.java
git commit -m "feat: implement Aliyun OSS file storage service"
```

---

## Task 6: Implement AWS S3 File Storage Service

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/S3FileStorageService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/filestorage/S3FileStorageServiceTest.java`

**Step 1: Add S3 dependency to pom.xml**

```xml
<!-- AWS S3 SDK -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.11</version>
</dependency>
```

**Step 2: Write the S3FileStorageService implementation**

```java
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
@Service
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
    public FileInfo uploadFile(MultipartFile file, String path) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String extension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);

        String objectKey = buildObjectKey(path, uniqueFileName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        log.info("File uploaded to S3: bucket={}, key={}", bucketName, objectKey);

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
    public FileInfo uploadFile(InputStream inputStream, String fileName, String path, String contentType) throws Exception {
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = buildObjectKey(path, uniqueFileName);

        byte[] bytes = inputStream.readAllBytes();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        log.info("File uploaded to S3: bucket={}, key={}", bucketName, objectKey);

        return FileInfo.builder()
                .fileName(uniqueFileName)
                .originalFileName(fileName)
                .fileKey(objectKey)
                .filePath(objectKey)
                .fileSize((long) bytes.length)
                .contentType(contentType)
                .bucketName(bucketName)
                .storageType(getStorageType())
                .build();
    }

    @Override
    public InputStream downloadFile(String fileKey) throws Exception {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        return s3Client.getObject(request);
    }

    @Override
    public void deleteFile(String fileKey) throws Exception {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();

        s3Client.deleteObject(request);
        log.info("File deleted from S3: bucket={}, key={}", bucketName, fileKey);
    }

    @Override
    public String getPresignedUrl(String fileKey, long expirationSeconds) throws Exception {
        GetUrlRequest request = GetUrlRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .expiration(Duration.ofSeconds(expirationSeconds))
                .build();

        return s3Client.utilities().getUrl(request).toString();
    }

    @Override
    public boolean fileExists(String fileKey) throws Exception {
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
```

**Step 3: Commit**

```bash
git add pom.xml
git add src/main/java/com/mydotey/ai/studio/service/filestorage/S3FileStorageService.java
git add src/test/java/com/mydotey/ai/studio/service/filestorage/S3FileStorageServiceTest.java
git commit -m "feat: implement AWS S3 file storage service"
```

---

## Task 7: Implement File Storage Factory and Manager

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/filestorage/FileStorageFactory.java`
- Create: `src/main/java/com/mydotey/ai/studio/service/FileStorageManagerService.java`
- Test: `src/test/java/com/mydotey/ai/studio/service/FileStorageManagerServiceTest.java`

**Step 1: Write the FileStorageFactory**

```java
package com.mydotey.ai.studio.service.filestorage;

import com.mydotey.ai.studio.entity.FileStorageConfig;
import com.mydotey.ai.studio.mapper.FileStorageConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final FileStorageConfigMapper configMapper;

    /**
     * 根据 ID 获取存储服务实例
     */
    public FileStorageService getStorageService(Long configId) {
        FileStorageConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new IllegalArgumentException("Storage config not found: " + configId);
        }
        return getStorageService(config);
    }

    /**
     * 根据配置获取存储服务实例
     */
    public FileStorageService getStorageService(FileStorageConfig config) {
        String storageType = config.getStorageType();

        return switch (storageType.toUpperCase()) {
            case "LOCAL" -> {
                // 本地存储使用配置中的 endpoint 作为上传目录
                String uploadDir = config.getEndpoint() != null
                    ? config.getEndpoint()
                    : System.getProperty("java.io.tmpdir") + "/ai-studio-uploads";
                yield new LocalFileStorageService();
            }
            case "OSS" -> new OssFileStorageService(
                    config.getEndpoint(),
                    config.getAccessKey(),
                    config.getSecretKey(),
                    config.getBucketName()
            );
            case "S3" -> new S3FileStorageService(
                    config.getEndpoint(),
                    config.getAccessKey(),
                    config.getSecretKey(),
                    config.getRegion(),
                    config.getBucketName()
            );
            default -> throw new IllegalArgumentException("Unsupported storage type: " + storageType);
        };
    }

    /**
     * 获取默认存储服务
     */
    public FileStorageService getDefaultStorageService() {
        FileStorageConfig defaultConfig = configMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileStorageConfig>()
                .eq(FileStorageConfig::getIsDefault, true)
                .last("LIMIT 1")
        );

        if (defaultConfig == null) {
            log.warn("No default storage config found, using local storage");
            return new LocalFileStorageService();
        }

        return getStorageService(defaultConfig);
    }
}
```

**Step 2: Write the FileStorageManagerService**

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse;
import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.entity.FileMetadata;
import com.mydotey.ai.studio.entity.FileStorageConfig;
import com.mydotey.ai.studio.mapper.FileMetadataMapper;
import com.mydotey.ai.studio.mapper.FileStorageConfigMapper;
import com.mydotey.ai.studio.service.filestorage.FileInfo;
import com.mydotey.ai.studio.service.filestorage.FileStorageFactory;
import com.mydotey.ai.studio.service.filestorage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageManagerService {

    private final FileStorageFactory storageFactory;
    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageConfigMapper storageConfigMapper;

    /**
     * 上传文件
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request, Long userId) throws Exception {
        // 获取存储服务
        FileStorageService storageService = getStorageService(request.getStorageConfigId());

        // 上传文件到存储
        FileInfo fileInfo = storageService.uploadFile(file, request.getPath());

        // 保存文件元数据
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(fileInfo.getFileName());
        metadata.setOriginalFileName(fileInfo.getOriginalFileName());
        metadata.setFilePath(fileInfo.getFilePath());
        metadata.setFileSize(fileInfo.getFileSize());
        metadata.setContentType(fileInfo.getContentType());
        metadata.setStorageType(fileInfo.getStorageType());
        metadata.setStorageConfigId(request.getStorageConfigId());
        metadata.setBucketName(fileInfo.getBucketName());
        metadata.setFileKey(fileInfo.getFileKey());
        metadata.setUploadedBy(userId);
        metadata.setRelatedEntityType(request.getRelatedEntityType());
        metadata.setRelatedEntityId(request.getRelatedEntityId());
        metadata.setCreatedAt(Instant.now());
        metadata.setUpdatedAt(Instant.now());

        fileMetadataMapper.insert(metadata);

        log.info("File uploaded successfully: id={}, name={}", metadata.getId(), metadata.getFileName());

        // 生成访问 URL
        String fileUrl = storageService.getPresignedUrl(fileInfo.getFileKey(), 0);

        return FileUploadResponse.builder()
                .id(metadata.getId())
                .fileName(metadata.getFileName())
                .originalFileName(metadata.getOriginalFileName())
                .filePath(metadata.getFilePath())
                .fileSize(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .storageType(metadata.getStorageType())
                .fileUrl(fileUrl)
                .uploadedBy(metadata.getUploadedBy())
                .createdAt(metadata.getCreatedAt())
                .build();
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(Long fileId, Long userId) throws Exception {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = storageFactory.getStorageService(metadata.getStorageConfigId());
        return storageService.downloadFile(metadata.getFileKey());
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) throws Exception {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = storageFactory.getStorageService(metadata.getStorageConfigId());
        storageService.deleteFile(metadata.getFileKey());

        fileMetadataMapper.deleteById(fileId);

        log.info("File deleted: id={}", fileId);
    }

    /**
     * 获取文件访问 URL
     */
    public String getFileUrl(Long fileId, Long userId, long expirationSeconds) throws Exception {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = storageFactory.getStorageService(metadata.getStorageConfigId());
        return storageService.getPresignedUrl(metadata.getFileKey(), expirationSeconds);
    }

    /**
     * 获取文件元数据
     */
    public FileMetadataResponse getFileMetadata(Long fileId) {
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File not found");
        }
        return toResponse(metadata);
    }

    /**
     * 获取用户的文件列表
     */
    public List<FileMetadataResponse> getUserFiles(Long userId) {
        List<FileMetadata> files = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>()
                        .eq(FileMetadata::getUploadedBy, userId)
                        .orderByDesc(FileMetadata::getCreatedAt)
        );

        return files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取关联实体的文件列表
     */
    public List<FileMetadataResponse> getRelatedFiles(String entityType, Long entityId) {
        List<FileMetadata> files = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>()
                        .eq(FileMetadata::getRelatedEntityType, entityType)
                        .eq(FileMetadata::getRelatedEntityId, entityId)
                        .orderByDesc(FileMetadata::getCreatedAt)
        );

        return files.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private FileMetadata getFileMetadata(Long fileId, Long userId) {
        FileMetadata metadata = fileMetadataMapper.selectById(fileId);
        if (metadata == null) {
            throw new BusinessException("File not found");
        }
        // 验证权限
        if (!metadata.getUploadedBy().equals(userId)) {
            throw new BusinessException("Permission denied");
        }
        return metadata;
    }

    private FileStorageService getStorageService(Long configId) {
        if (configId == null) {
            return storageFactory.getDefaultStorageService();
        }
        return storageFactory.getStorageService(configId);
    }

    private FileMetadataResponse toResponse(FileMetadata metadata) {
        return FileMetadataResponse.builder()
                .id(metadata.getId())
                .fileName(metadata.getFileName())
                .originalFileName(metadata.getOriginalFileName())
                .filePath(metadata.getFilePath())
                .fileSize(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .storageType(metadata.getStorageType())
                .uploadedBy(metadata.getUploadedBy())
                .relatedEntityType(metadata.getRelatedEntityType())
                .relatedEntityId(metadata.getRelatedEntityId())
                .createdAt(metadata.getCreatedAt())
                .build();
    }
}
```

**Step 3: Write the test class**

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文件存储管理服务测试")
class FileStorageManagerServiceTest {

    @Autowired
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var response = fileStorageManagerService.uploadFile(file, request, 1L);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("test.txt", response.getOriginalFileName());
        assertEquals("text/plain", response.getContentType());
    }

    @Test
    @DisplayName("应该能够获取文件元数据")
    void testGetFileMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "metadata.txt",
                "metadata.txt",
                "text/plain",
                "Metadata test".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var uploaded = fileStorageManagerService.uploadFile(file, request, 1L);
        var metadata = fileStorageManagerService.getFileMetadata(uploaded.getId());

        assertEquals(uploaded.getId(), metadata.getId());
        assertEquals("metadata.txt", metadata.getOriginalFileName());
    }

    @Test
    @DisplayName("应该能够删除文件")
    void testDeleteFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "delete.txt",
                "delete.txt",
                "text/plain",
                "Delete me".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/test");

        var uploaded = fileStorageManagerService.uploadFile(file, request, 1L);

        assertDoesNotThrow(() -> fileStorageManagerService.deleteFile(uploaded.getId(), 1L));

        // 验证文件已被删除（应该抛出异常）
        assertThrows(Exception.class, () -> fileStorageManagerService.getFileMetadata(uploaded.getId()));
    }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=FileStorageManagerServiceTest`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/service/filestorage/FileStorageFactory.java
git add src/main/java/com/mydotey/ai/studio/service/FileStorageManagerService.java
git add src/test/java/com/mydotey/ai/studio/service/FileStorageManagerServiceTest.java
git commit -m "feat: implement file storage factory and manager service"
```

---

## Task 8: Create File Storage Controller

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/controller/FileStorageController.java`
- Test: `src/test/java/com/mydotey/ai/studio/controller/FileStorageControllerTest.java`

**Step 1: Write the FileStorageController**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse;
import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageManagerService fileStorageManagerService;

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @AuditLog(action = "FILE_UPLOAD", resourceType = "File")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute FileUploadRequest request,
            @RequestAttribute("userId") Long userId) throws Exception {
        FileUploadResponse response = fileStorageManagerService.uploadFile(file, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        var inputStream = fileStorageManagerService.downloadFile(id, userId);
        var metadata = fileStorageManagerService.getFileMetadata(id);

        // 对于本地存储，直接返回文件
        if ("LOCAL".equals(metadata.getStorageType())) {
            Path filePath = Paths.get(metadata.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(metadata.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                        .body(resource);
            }
        }

        // 对于云存储，返回流
        // 注意：实际实现可能需要将 InputStream 转换为 Resource
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(new Resource() {
                    @Override
                    public inputStream() {
                        try {
                            return inputStream;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    /**
     * 获取文件访问 URL
     */
    @GetMapping("/{id}/url")
    public ApiResponse<String> getFileUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "3600") long expirationSeconds,
            @RequestAttribute("userId") Long userId) throws Exception {
        String url = fileStorageManagerService.getFileUrl(id, userId, expirationSeconds);
        return ApiResponse.success(url);
    }

    /**
     * 获取文件元数据
     */
    @GetMapping("/{id}")
    public ApiResponse<FileMetadataResponse> getFileMetadata(@PathVariable Long id) {
        FileMetadataResponse response = fileStorageManagerService.getFileMetadata(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的文件列表
     */
    @GetMapping("/my")
    public ApiResponse<List<FileMetadataResponse>> getMyFiles(@RequestAttribute("userId") Long userId) {
        List<FileMetadataResponse> files = fileStorageManagerService.getUserFiles(userId);
        return ApiResponse.success(files);
    }

    /**
     * 获取关联实体的文件列表
     */
    @GetMapping("/related/{entityType}/{entityId}")
    public ApiResponse<List<FileMetadataResponse>> getRelatedFiles(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<FileMetadataResponse> files = fileStorageManagerService.getRelatedFiles(entityType, entityId);
        return ApiResponse.success(files);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{id}")
    @AuditLog(action = "FILE_DELETE", resourceType = "File", resourceIdParam = "id")
    public ApiResponse<Void> deleteFile(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) throws Exception {
        fileStorageManagerService.deleteFile(id, userId);
        return ApiResponse.success("File deleted successfully");
    }
}
```

**Step 2: Write the test class**

```java
package com.mydotey.ai.studio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileStorageController.class)
@DisplayName("文件存储控制器测试")
class FileStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("应该能够上传文件")
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        when(fileStorageManagerService.uploadFile(any(), any(), eq(1L)))
                .thenReturn(new com.mydotey.ai.studio.dto.filestorage.FileUploadResponse());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("path", "/test")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("应该能够获取文件元数据")
    void testGetFileMetadata() throws Exception {
        when(fileStorageManagerService.getFileMetadata(1L))
                .thenReturn(new com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse());

        mockMvc.perform(get("/api/files/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

**Step 3: Run test to verify it passes**

Run: `mvn test -Dtest=FileStorageControllerTest`
Expected: PASS

**Step 4: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/controller/FileStorageController.java
git add src/test/java/com/mydotey/ai/studio/controller/FileStorageControllerTest.java
git commit -m "feat: add file storage controller"
```

---

## Task 9: Implement Storage Configuration Management

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/service/StorageConfigService.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/CreateStorageConfigRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/UpdateStorageConfigRequest.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/filestorage/StorageConfigResponse.java`
- Create: `src/main/java/com/mydotey/ai/studio/controller/StorageConfigController.java`

**Step 1: Write DTOs**

```java
// CreateStorageConfigRequest.java
package com.mydotey.ai.studio.dto.filestorage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStorageConfigRequest {
    @NotBlank(message = "Storage type is required")
    private String storageType;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private String region;

    private Boolean isDefault = false;

    private String description;
}

// UpdateStorageConfigRequest.java
package com.mydotey.ai.studio.dto.filestorage;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStorageConfigRequest {
    @NotBlank(message = "Storage type is required")
    private String storageType;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private String region;

    private Boolean isDefault;

    private String description;
}

// StorageConfigResponse.java
package com.mydotey.ai.studio.dto.filestorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageConfigResponse {
    private Long id;
    private String storageType;
    private String endpoint;
    private String bucketName;
    private String region;
    private Boolean isDefault;
    private String description;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
```

**Step 2: Write the StorageConfigService**

```java
package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.filestorage.CreateStorageConfigRequest;
import com.mydotey.ai.studio.dto.filestorage.StorageConfigResponse;
import com.mydotey.ai.studio.dto.filestorage.UpdateStorageConfigRequest;
import com.mydotey.ai.studio.entity.FileStorageConfig;
import com.mydotey.ai.studio.mapper.FileStorageConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageConfigService {

    private final FileStorageConfigMapper configMapper;

    @Value("${file.storage.local.upload-dir:#{null}}")
    private String defaultLocalUploadDir;

    /**
     * 创建存储配置
     */
    @Transactional
    public StorageConfigResponse createConfig(CreateStorageConfigRequest request, Long userId) {
        // 如果设置为默认，取消其他默认配置
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetDefaultConfigs();
        }

        FileStorageConfig config = new FileStorageConfig();
        config.setStorageType(request.getStorageType());
        config.setEndpoint(request.getEndpoint());
        config.setAccessKey(request.getAccessKey());
        config.setSecretKey(request.getSecretKey());
        config.setBucketName(request.getBucketName());
        config.setRegion(request.getRegion());
        config.setIsDefault(request.getIsDefault());
        config.setDescription(request.getDescription());
        config.setCreatedBy(userId);
        config.setCreatedAt(Instant.now());
        config.setUpdatedAt(Instant.now());

        configMapper.insert(config);

        log.info("Storage config created: id={}, type={}", config.getId(), config.getStorageType());

        return toResponse(config);
    }

    /**
     * 更新存储配置
     */
    @Transactional
    public StorageConfigResponse updateConfig(Long id, UpdateStorageConfigRequest request, Long userId) {
        FileStorageConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("Storage config not found");
        }

        // 如果设置为默认，取消其他默认配置
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetDefaultConfigs();
        }

        config.setStorageType(request.getStorageType());
        config.setEndpoint(request.getEndpoint());
        config.setAccessKey(request.getAccessKey());
        config.setSecretKey(request.getSecretKey());
        config.setBucketName(request.getBucketName());
        config.setRegion(request.getRegion());
        config.setIsDefault(request.getIsDefault());
        config.setDescription(request.getDescription());
        config.setUpdatedAt(Instant.now());

        configMapper.updateById(config);

        log.info("Storage config updated: id={}", id);

        return toResponse(config);
    }

    /**
     * 删除存储配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        FileStorageConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("Storage config not found");
        }

        configMapper.deleteById(id);

        log.info("Storage config deleted: id={}", id);
    }

    /**
     * 获取存储配置详情
     */
    public StorageConfigResponse getConfig(Long id) {
        FileStorageConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("Storage config not found");
        }
        return toResponse(config);
    }

    /**
     * 获取所有存储配置
     */
    public List<StorageConfigResponse> getAllConfigs() {
        List<FileStorageConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<FileStorageConfig>()
                        .orderByDesc(FileStorageConfig::getIsDefault)
                        .orderByDesc(FileStorageConfig::getCreatedAt)
        );

        return configs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取默认存储配置
     */
    public StorageConfigResponse getDefaultConfig() {
        FileStorageConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<FileStorageConfig>()
                        .eq(FileStorageConfig::getIsDefault, true)
                        .last("LIMIT 1")
        );

        if (config == null) {
            // 如果没有配置默认存储，返回本地存储配置
            config = new FileStorageConfig();
            config.setStorageType("LOCAL");
            config.setEndpoint(defaultLocalUploadDir);
            config.setIsDefault(true);
        }

        return toResponse(config);
    }

    private void unsetDefaultConfigs() {
        configMapper.selectList(
                new LambdaQueryWrapper<FileStorageConfig>()
                        .eq(FileStorageConfig::getIsDefault, true)
        ).forEach(config -> {
            config.setIsDefault(false);
            configMapper.updateById(config);
        });
    }

    private StorageConfigResponse toResponse(FileStorageConfig config) {
        return StorageConfigResponse.builder()
                .id(config.getId())
                .storageType(config.getStorageType())
                .endpoint(config.getEndpoint())
                .bucketName(config.getBucketName())
                .region(config.getRegion())
                .isDefault(config.getIsDefault())
                .description(config.getDescription())
                .createdBy(config.getCreatedBy())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
```

**Step 3: Write the StorageConfigController**

```java
package com.mydotey.ai.studio.controller;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.filestorage.CreateStorageConfigRequest;
import com.mydotey.ai.studio.dto.filestorage.StorageConfigResponse;
import com.mydotey.ai.studio.dto.filestorage.UpdateStorageConfigRequest;
import com.mydotey.ai.studio.service.StorageConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storage-configs")
@RequiredArgsConstructor
public class StorageConfigController {

    private final StorageConfigService storageConfigService;

    /**
     * 创建存储配置（仅管理员）
     */
    @PostMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_CREATE", resourceType = "StorageConfig")
    public ApiResponse<StorageConfigResponse> createConfig(
            @Valid @RequestBody CreateStorageConfigRequest request,
            @RequestAttribute("userId") Long userId) {
        StorageConfigResponse response = storageConfigService.createConfig(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 更新存储配置（仅管理员）
     */
    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_UPDATE", resourceType = "StorageConfig", resourceIdParam = "id")
    public ApiResponse<StorageConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStorageConfigRequest request,
            @RequestAttribute("userId") Long userId) {
        StorageConfigResponse response = storageConfigService.updateConfig(id, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 删除存储配置（仅管理员）
     */
    @DeleteMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @AuditLog(action = "STORAGE_CONFIG_DELETE", resourceType = "StorageConfig", resourceIdParam = "id")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        storageConfigService.deleteConfig(id);
        return ApiResponse.success("Storage config deleted");
    }

    /**
     * 获取存储配置详情
     */
    @GetMapping("/{id}")
    public ApiResponse<StorageConfigResponse> getConfig(@PathVariable Long id) {
        StorageConfigResponse response = storageConfigService.getConfig(id);
        return ApiResponse.success(response);
    }

    /**
     * 获取所有存储配置
     */
    @GetMapping
    public ApiResponse<List<StorageConfigResponse>> getAllConfigs() {
        List<StorageConfigResponse> configs = storageConfigService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    /**
     * 获取默认存储配置
     */
    @GetMapping("/default")
    public ApiResponse<StorageConfigResponse> getDefaultConfig() {
        StorageConfigResponse response = storageConfigService.getDefaultConfig();
        return ApiResponse.success(response);
    }
}
```

**Step 4: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/dto/filestorage/CreateStorageConfigRequest.java
git add src/main/java/com/mydotey/ai/studio/dto/filestorage/UpdateStorageConfigRequest.java
git add src/main/java/com/mydotey/ai/studio/dto/filestorage/StorageConfigResponse.java
git add src/main/java/com/mydotey/ai/studio/service/StorageConfigService.java
git add src/main/java/com/mydotey/ai/studio/controller/StorageConfigController.java
git commit -m "feat: add storage configuration management"
```

---

## Task 10: Create File Storage Integration Test

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/integration/FileStorageIntegrationTest.java`

**Step 1: Write the integration test**

```java
package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.service.FileStorageManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("文件存储系统集成测试")
class FileStorageIntegrationTest {

    @Autowired
    private FileStorageManagerService fileStorageManagerService;

    @Test
    @DisplayName("完整的文件上传和下载流程测试")
    void testCompleteFileUploadAndDownloadFlow() throws Exception {
        // 1. 上传文件
        MockMultipartFile file = new MockMultipartFile(
                "integration-test.txt",
                "integration-test.txt",
                "text/plain",
                "Integration test content".getBytes()
        );

        FileUploadRequest uploadRequest = new FileUploadRequest();
        uploadRequest.setPath("/integration-test");
        uploadRequest.setRelatedEntityType("DOCUMENT");
        uploadRequest.setRelatedEntityId(1L);

        FileUploadResponse uploadResponse = fileStorageManagerService.uploadFile(file, uploadRequest, 1L);

        assertNotNull(uploadResponse);
        assertNotNull(uploadResponse.getId());
        assertEquals("integration-test.txt", uploadResponse.getOriginalFileName());
        assertNotNull(uploadResponse.getFileUrl());

        // 2. 获取文件元数据
        var metadata = fileStorageManagerService.getFileMetadata(uploadResponse.getId());
        assertEquals(uploadResponse.getId(), metadata.getId());

        // 3. 下载文件
        var inputStream = fileStorageManagerService.downloadFile(uploadResponse.getId(), 1L);
        byte[] content = inputStream.readAllBytes();
        assertEquals("Integration test content", new String(content));

        // 4. 获取文件 URL
        String fileUrl = fileStorageManagerService.getFileUrl(uploadResponse.getId(), 1L, 3600);
        assertNotNull(fileUrl);

        // 5. 获取关联文件
        var relatedFiles = fileStorageManagerService.getRelatedFiles("DOCUMENT", 1L);
        assertFalse(relatedFiles.isEmpty());

        // 6. 删除文件
        fileStorageManagerService.deleteFile(uploadResponse.getId(), 1L);

        // 验证文件已删除
        assertThrows(Exception.class, () -> fileStorageManagerService.getFileMetadata(uploadResponse.getId()));
    }

    @Test
    @DisplayName("应该能够获取用户文件列表")
    void testGetUserFiles() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "user-file-1.txt",
                "user-file-1.txt",
                "text/plain",
                "User file 1".getBytes()
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "user-file-2.txt",
                "user-file-2.txt",
                "text/plain",
                "User file 2".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setPath("/user-test");

        fileStorageManagerService.uploadFile(file1, request, 1L);
        fileStorageManagerService.uploadFile(file2, request, 1L);

        var userFiles = fileStorageManagerService.getUserFiles(1L);

        assertTrue(userFiles.size() >= 2);
    }

    @Test
    @DisplayName("应该支持不同路径的文件上传")
    void testUploadToDifferentPaths() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "path-test.txt",
                "path-test.txt",
                "text/plain",
                "Path test".getBytes()
        );

        FileUploadRequest request1 = new FileUploadRequest();
        request1.setPath("/documents/2024");

        FileUploadRequest request2 = new FileUploadRequest();
        request2.setPath("/images");

        var response1 = fileStorageManagerService.uploadFile(file, request1, 1L);
        var response2 = fileStorageManagerService.uploadFile(file, request2, 1L);

        assertNotEquals(response1.getId(), response2.getId());
    }
}
```

**Step 2: Run test to verify it passes**

Run: `mvn test -Dtest=FileStorageIntegrationTest`
Expected: PASS

**Step 3: Commit**

```bash
git add src/test/java/com/mydotey/ai/studio/integration/FileStorageIntegrationTest.java
git commit -m "test: add file storage integration test"
```

---

## Task 11: Add Application Configuration

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-test.yml`

**Step 1: Add configuration to application.yml**

```yaml
# File Storage Configuration
file:
  storage:
    local:
      upload-dir: ${UPLOAD_DIR:${java.io.tmpdir}/ai-studio-uploads}
```

**Step 2: Add dev environment configuration**

```yaml
file:
  storage:
    local:
      upload-dir: /tmp/ai-studio-uploads
```

**Step 3: Add test environment configuration**

```yaml
file:
  storage:
    local:
      upload-dir: ${java.io.tmpdir}/ai-studio-test-uploads
```

**Step 4: Commit**

```bash
git add src/main/resources/application.yml
git add src/main/resources/application-dev.yml
git add src/main/resources/application-test.yml
git commit -m "config: add file storage configuration"
```

---

## Task 12: Refactor Existing FileUtil

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/util/FileUtil.java`

**Step 1: Update FileUtil to use new storage service**

```java
package com.mydotey.ai.studio.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件工具类（兼容 Phase 2 文档处理）
 *
 * @deprecated 使用 {@link com.mydotey.ai.studio.service.FileStorageManagerService} 代替
 */
@Slf4j
@Component
@Deprecated
public class FileUtil {

    @Value("${file.storage.local.upload-dir:${java.io.tmpdir}/ai-studio-uploads}")
    private String uploadDir;

    /**
     * 初始化上传目录
     */
    public void init() {
        try {
            Path path = Paths.get(uploadDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created upload directory: {}", uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    /**
     * 保存上传的文件
     *
     * @deprecated 使用 FileStorageManagerService.uploadFile 代替
     */
    public String saveFile(String originalFilename, byte[] content) throws IOException {
        init();

        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = Paths.get(uploadDir, filename);
        Files.write(filePath, content);

        log.info("File saved: {}", filePath);
        return filePath.toString();
    }

    /**
     * 读取文件内容
     *
     * @deprecated 使用 FileStorageManagerService.downloadFile 代替
     */
    public String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

    /**
     * 删除文件
     *
     * @deprecated 使用 FileStorageManagerService.deleteFile 代替
     */
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
        log.info("File deleted: {}", filePath);
    }
}
```

**Step 2: Commit**

```bash
git add src/main/java/com/mydotey/ai/studio/util/FileUtil.java
git commit -m "refactor: deprecate FileUtil in favor of FileStorageManagerService"
```

---

## Task 13: Update PROJECT_PROGRESS.md

**Files:**
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: Add Phase 8 section to PROJECT_PROGRESS.md**

```markdown
### Phase 8: 文件存储系统 ✅

**完成时间：2026-01-19**

**实现内容：**
- 多存储类型支持（本地、阿里云 OSS、AWS S3）
- 文件元数据管理
- 存储配置管理
- 统一文件上传下载 API
- URL 签名（云存储）
- 访问控制
- 完整测试覆盖

**新增文件：**
```
src/main/java/com/mydotey/ai/studio/
├── entity/
│   ├── FileMetadata.java
│   └── FileStorageConfig.java
├── mapper/
│   ├── FileMetadataMapper.java
│   └── FileStorageConfigMapper.java
├── dto/filestorage/
│   ├── FileUploadRequest.java
│   ├── FileUploadResponse.java
│   ├── FileMetadataResponse.java
│   ├── CreateStorageConfigRequest.java
│   ├── UpdateStorageConfigRequest.java
│   └── StorageConfigResponse.java
├── service/
│   ├── FileStorageManagerService.java
│   ├── StorageConfigService.java
│   └── filestorage/
│       ├── FileStorageService.java (interface)
│       ├── FileInfo.java
│       ├── LocalFileStorageService.java
│       ├── OssFileStorageService.java
│       ├── S3FileStorageService.java
│       └── FileStorageFactory.java
└── controller/
    ├── FileStorageController.java
    └── StorageConfigController.java

src/main/resources/
└── db/migration/
    └── V4__file_metadata.sql

src/test/java/com/mydotey/ai/studio/
├── service/
│   ├── FileStorageManagerServiceTest.java
│   ├── StorageConfigServiceTest.java
│   └── filestorage/
│       ├── LocalFileStorageServiceTest.java
│       ├── OssFileStorageServiceTest.java
│       └── S3FileStorageServiceTest.java
├── controller/
│   ├── FileStorageControllerTest.java
│   └── StorageConfigControllerTest.java
└── integration/
    └── FileStorageIntegrationTest.java
```

**API 端点：**

文件管理 API (`/api/files/*`)：
- `POST /api/files/upload` - 上传文件
- `GET /api/files/download/{id}` - 下载文件
- `GET /api/files/{id}/url` - 获取文件访问 URL
- `GET /api/files/{id}` - 获取文件元数据
- `GET /api/files/my` - 获取我的文件列表
- `GET /api/files/related/{entityType}/{entityId}` - 获取关联实体文件
- `DELETE /api/files/{id}` - 删除文件

存储配置管理 API (`/api/storage-configs/*`)：
- `POST /api/storage-configs` - 创建存储配置（管理员）
- `PUT /api/storage-configs/{id}` - 更新存储配置（管理员）
- `DELETE /api/storage-configs/{id}` - 删除存储配置（管理员）
- `GET /api/storage-configs/{id}` - 获取存储配置详情
- `GET /api/storage-configs` - 获取所有存储配置
- `GET /api/storage-configs/default` - 获取默认存储配置

**实现任务完成情况：**

1. ✅ **文件元数据和存储配置实体**
   - FileMetadata - 文件元数据实体
   - FileStorageConfig - 存储配置实体
   - 数据库迁移（V4__file_metadata.sql）

2. ✅ **文件存储接口和 DTOs**
   - FileStorageService - 存储服务接口
   - FileInfo - 文件信息 DTO
   - 请求和响应 DTOs

3. ✅ **本地存储实现**
   - LocalFileStorageService - 本地文件存储
   - 支持文件上传、下载、删除
   - 支持自定义上传目录

4. ✅ **阿里云 OSS 实现**
   - OssFileStorageService - OSS 文件存储
   - 支持签名 URL 生成
   - 自动创建 Bucket

5. ✅ **AWS S3 实现**
   - S3FileStorageService - S3 文件存储
   - 支持 S3 兼容存储
   - 签名 URL 生成

6. ✅ **文件存储工厂和管理服务**
   - FileStorageFactory - 存储服务工厂
   - FileStorageManagerService - 文件管理服务
   - 元数据持久化
   - 权限控制

7. ✅ **存储配置管理**
   - CRUD 操作
   - 默认配置管理
   - 敏感信息保护（secret）

8. ✅ **控制器**
   - FileStorageController - 文件管理 API
   - StorageConfigController - 配置管理 API
   - 审计日志集成

9. ✅ **测试覆盖**
   - LocalFileStorageServiceTest - 本地存储测试
   - OssFileStorageServiceTest - OSS 测试
   - S3FileStorageServiceTest - S3 测试
   - FileStorageManagerServiceTest - 管理服务测试
   - FileStorageControllerTest - 控制器测试
   - FileStorageIntegrationTest - 集成测试

**技术栈：**
- 阿里云 OSS SDK 3.17.4
- AWS S3 SDK 2.25.11
- Java NIO（本地存储）
- 策略模式（多存储支持）

**核心功能：**
- 多存储类型支持（LOCAL/OSS/S3）
- 统一文件管理 API
- 文件元数据管理
- 存储配置管理
- URL 签名访问
- 权限控制
- 关联实体文件

**测试统计：**
- Phase 8 总测试数：8 个
- 单元测试：6 ✅
- 集成测试：2 ✅
```

**Step 2: Update current status section**

Modify the "当前阶段" section:

```markdown
**当前阶段：**
- Phase 1: 基础架构 ✅
- Phase 2: 文档处理 ✅
- Phase 3: 用户认证和权限管理 ✅
- Phase 4: RAG 系统 ✅
- Phase 5: Agent 系统 ✅
- Phase 6: 聊天机器人 ✅
- Phase 7: 网页抓取 ✅
- Phase 8: 文件存储系统 ✅
```

**Step 3: Update next steps**

Modify the "下一步计划" section:

```markdown
## 下一步计划

### Phase 9: 系统监控和日志（待规划）

**预计功能：**
- APM 监控
- 结构化日志
- 请求追踪（trace ID）
- 性能指标
- 错误追踪

### Phase 10: API 文档和部署（待规划）

**预计功能：**
- Swagger/OpenAPI 文档
- 部署文档
- 运维手册
- Docker 容器化
- CI/CD 流程
```

**Step 4: Commit**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: update Phase 8 file storage system completion status"
```

---

## Summary

Phase 8 完成后，系统将具备以下能力：

1. **多存储支持**：本地存储、阿里云 OSS、AWS S3
2. **统一 API**：通过 FileStorageService 接口提供统一的文件操作
3. **元数据管理**：完整的文件元数据记录和查询
4. **配置管理**：支持多个存储配置，可设置默认存储
5. **访问控制**：基于用户和关联实体的权限控制
6. **URL 签名**：云存储支持临时访问 URL
7. **向后兼容**：保留 FileUtil 以支持现有代码

### 测试覆盖

- LocalFileStorageServiceTest - 本地存储测试
- OssFileStorageServiceTest - OSS 测试
- S3FileStorageServiceTest - S3 测试
- FileStorageManagerServiceTest - 管理服务测试
- StorageConfigServiceTest - 配置服务测试
- FileStorageControllerTest - 控制器测试
- StorageConfigControllerTest - 配置控制器测试
- FileStorageIntegrationTest - 集成测试

### API 端点

| 方法 | 端点 | 权限 | 描述 |
|------|------|--------|------|
| POST | /api/files/upload | 认证用户 | 上传文件 |
| GET | /api/files/download/{id} | 所有者 | 下载文件 |
| GET | /api/files/{id}/url | 所有者 | 获取访问 URL |
| GET | /api/files/{id} | 认证用户 | 获取文件元数据 |
| GET | /api/files/my | 认证用户 | 获取我的文件 |
| GET | /api/files/related/{type}/{id} | 认证用户 | 获取关联文件 |
| DELETE | /api/files/{id} | 所有者 | 删除文件 |
| POST | /api/storage-configs | 管理员 | 创建存储配置 |
| PUT | /api/storage-configs/{id} | 管理员 | 更新存储配置 |
| DELETE | /api/storage-configs/{id} | 管理员 | 删除存储配置 |
| GET | /api/storage-configs/{id} | 公开 | 获取配置详情 |
| GET | /api/storage-configs | 公开 | 获取所有配置 |
| GET | /api/storage-configs/default | 公开 | 获取默认配置 |

### 数据库表

1. `file_metadata` - 文件元数据表（新增）
2. `file_storage_config` - 文件存储配置表（已存在）

### 后续改进方向（Phase 9+）

- 文件预览功能（图片、PDF 等）
- 文件压缩和优化
- 分片上传（大文件）
- CDN 加速
- 文件版本管理
- 病毒扫描
- 文件加密存储

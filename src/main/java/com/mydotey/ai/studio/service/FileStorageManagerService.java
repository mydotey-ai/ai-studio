package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.filestorage.FileMetadataResponse;
import com.mydotey.ai.studio.dto.filestorage.FileUploadRequest;
import com.mydotey.ai.studio.dto.filestorage.FileUploadResponse;
import com.mydotey.ai.studio.entity.FileMetadata;
import com.mydotey.ai.studio.mapper.FileMetadataMapper;
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

    /**
     * 上传文件
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, FileUploadRequest request, Long userId) {
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
    public InputStream downloadFile(Long fileId, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = getStorageService(metadata.getStorageConfigId());
        return storageService.downloadFile(metadata.getFileKey());
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = getStorageService(metadata.getStorageConfigId());
        storageService.deleteFile(metadata.getFileKey());

        fileMetadataMapper.deleteById(fileId);

        log.info("File deleted: id={}", fileId);
    }

    /**
     * 获取文件访问 URL
     */
    public String getFileUrl(Long fileId, Long userId, long expirationSeconds) {
        FileMetadata metadata = getFileMetadata(fileId, userId);

        FileStorageService storageService = getStorageService(metadata.getStorageConfigId());
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

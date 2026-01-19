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

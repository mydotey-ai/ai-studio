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
    private final LocalFileStorageService localFileStorageService;

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
            case "LOCAL" -> localFileStorageService;
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
            return localFileStorageService;
        }

        return getStorageService(defaultConfig);
    }
}

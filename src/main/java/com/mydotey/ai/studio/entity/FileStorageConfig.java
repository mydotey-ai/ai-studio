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

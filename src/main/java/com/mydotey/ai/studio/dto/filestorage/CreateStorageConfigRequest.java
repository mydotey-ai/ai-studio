package com.mydotey.ai.studio.dto.filestorage;

import jakarta.validation.constraints.NotBlank;
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

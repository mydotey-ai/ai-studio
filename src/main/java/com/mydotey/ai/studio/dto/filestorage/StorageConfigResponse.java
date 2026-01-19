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

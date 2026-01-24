package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import java.time.Instant;

@Data
public class ModelConfigDto {
    private Long id;
    private Long orgId;
    private ModelConfigType type;
    private String name;
    private String endpoint;
    private String apiKey; // 部分隐藏
    private String model;
    private Integer dimension;
    private Double temperature;
    private Integer maxTokens;
    private Integer timeout;
    private Boolean enableStreaming;
    private Boolean isDefault;
    private String status;
    private String description;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}

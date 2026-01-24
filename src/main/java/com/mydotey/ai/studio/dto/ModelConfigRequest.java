package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ModelConfigRequest {
    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "类型不能为空")
    private ModelConfigType type;

    @NotBlank(message = "端点不能为空")
    private String endpoint;

    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    @NotBlank(message = "模型不能为空")
    private String model;

    private Integer dimension; // 向量模型必填

    private Double temperature;

    private Integer maxTokens;

    private Integer timeout = 30000;

    private Boolean enableStreaming = true;

    private Boolean isDefault = false;

    private String description;
}

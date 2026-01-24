package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.enums.ModelConfigType;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("model_configs")
public class ModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private ModelConfigType type;

    private String name;

    private String endpoint;

    private String apiKey;

    private String model;

    private Integer dimension; // 向量模型专用

    private Double temperature; // LLM模型专用

    private Integer maxTokens; // LLM模型专用

    private Integer timeout;

    private Boolean enableStreaming; // LLM模型专用

    private Boolean isDefault;

    private String status; // active, inactive

    private String description;

    private Long createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}

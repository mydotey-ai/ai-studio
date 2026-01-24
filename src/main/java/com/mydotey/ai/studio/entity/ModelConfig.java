package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型配置实体
 * 用于管理 LLM 模型的配置信息
 */
@Data
@TableName("model_config")
public class ModelConfig {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 提供商（如：openai、anthropic、ollama 等）
     */
    private String provider;

    /**
     * 模型名称（如：gpt-4、claude-3、llama-2 等）
     */
    private String modelName;

    /**
     * API 基础 URL
     */
    private String apiUrl;

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    /**
     * 温度参数（0-1 之间）
     */
    private Double temperature;

    /**
     * 是否为默认配置
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建者 ID
     */
    private Long createdBy;

    /**
     * 更新者 ID
     */
    private Long updatedBy;
}

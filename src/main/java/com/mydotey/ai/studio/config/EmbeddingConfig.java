package com.mydotey.ai.studio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    /**
     * API 端点地址
     */
    private String endpoint = "https://api.openai.com/v1";

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * 使用的模型名称
     */
    private String model = "text-embedding-ada-002";

    /**
     * 向量维度
     */
    private int dimension = 1536;

    /**
     * 批量处理大小
     */
    private int batchSize = 100;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 30000;
}

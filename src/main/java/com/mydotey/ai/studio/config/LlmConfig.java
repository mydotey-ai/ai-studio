package com.mydotey.ai.studio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {

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
    private String model = "gpt-3.5-turbo";

    /**
     * 默认温度
     */
    private Double defaultTemperature = 0.3;

    /**
     * 默认最大生成长度
     */
    private Integer defaultMaxTokens = 1000;

    /**
     * 请求超时时间（毫秒）
     */
    private Integer timeout = 60000;

    /**
     * 是否启用流式响应
     */
    private Boolean enableStreaming = true;
}

package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    /**
     * 模型名称
     */
    private String model;

    /**
     * 消息列表
     */
    private String messages;

    /**
     * 温度参数（0-2）
     */
    private Double temperature;

    /**
     * 最大生成长度
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;
}

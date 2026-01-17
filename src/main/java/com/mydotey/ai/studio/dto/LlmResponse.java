package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    /**
     * 生成的文本
     */
    private String content;

    /**
     * 完成原因
     */
    private String finishReason;

    /**
     * 消耗的 prompt tokens
     */
    private Integer promptTokens;

    /**
     * 消耗的 completion tokens
     */
    private Integer completionTokens;

    /**
     * 总消耗 tokens
     */
    private Integer totalTokens;
}

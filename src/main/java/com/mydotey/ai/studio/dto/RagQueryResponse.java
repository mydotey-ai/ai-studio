package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryResponse {
    /**
     * 生成的回答
     */
    private String answer;

    /**
     * 相关文档来源
     */
    private List<SourceDocument> sources;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 消耗的 tokens
     */
    private Integer totalTokens;

    /**
     * 流式响应完成标志
     */
    private Boolean isComplete = true;
}

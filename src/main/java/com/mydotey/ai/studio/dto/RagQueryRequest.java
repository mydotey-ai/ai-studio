package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RagQueryRequest {
    /**
     * 用户问题
     */
    @NotBlank(message = "Question is required")
    private String question;

    /**
     * 知识库 ID 列表
     */
    @NotNull(message = "Knowledge base IDs are required")
    private List<Long> knowledgeBaseIds;

    /**
     * 返回的相关文档数量，默认 5
     */
    private Integer topK = 5;

    /**
     * 相似度阈值（0-1），默认 0.7
     */
    private Double scoreThreshold = 0.7;

    /**
     * 对话历史（用于多轮对话）
     */
    private List<Message> conversationHistory;

    /**
     * 是否返回引用来源，默认 true
     */
    private Boolean includeSources = true;

    /**
     * 温度参数，默认 0.3
     */
    private Double temperature = 0.3;

    /**
     * 最大生成长度，默认 1000
     */
    private Integer maxTokens = 1000;
}

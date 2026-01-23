package com.mydotey.ai.studio.dto.export;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据导出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExportRequest {

    /**
     * 导出范围
     */
    @NotNull(message = "导出范围不能为空")
    private ExportScope scope;

    /**
     * 知识库 ID 列表 (scope=KNOWLEDGE_BASES 时有效)
     */
    private List<Long> knowledgeBaseIds;

    /**
     * Agent ID 列表 (scope=AGENTS 时有效)
     */
    private List<Long> agentIds;

    /**
     * Chatbot ID 列表 (scope=CHATBOTS 时有效)
     */
    private List<Long> chatbotIds;

    /**
     * 是否包含对话历史 (scope=CHATBOTS 时有效)
     */
    @Builder.Default
    private boolean includeConversations = true;

    /**
     * 是否包含文档内容 (scope=KNOWLEDGE_BASES 时有效)
     */
    @Builder.Default
    private boolean includeDocumentContent = false;

    /**
     * 是否异步导出
     */
    @Builder.Default
    private boolean async = true;
}

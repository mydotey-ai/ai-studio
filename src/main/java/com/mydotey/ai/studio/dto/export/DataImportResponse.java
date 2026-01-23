package com.mydotey.ai.studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 数据导入响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataImportResponse {

    /**
     * 导入任务 ID
     */
    private Long taskId;

    /**
     * 状态
     */
    private ImportStatus status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 导入统计
     */
    private ImportStats stats;

    /**
     * 错误详情
     */
    private Map<String, String> errors;

    /**
     * 导入状态枚举
     */
    public enum ImportStatus {
        VALIDATING,
        VALIDATED,
        IMPORTING,
        COMPLETED,
        FAILED
    }

    /**
     * 导入统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportStats {
        private int knowledgeBasesCreated;
        private int knowledgeBasesSkipped;
        private int documentsCreated;
        private int documentsSkipped;
        private int agentsCreated;
        private int agentsSkipped;
        private int chatbotsCreated;
        private int chatbotsSkipped;
        private int conversationsCreated;
        private int messagesImported;
    }
}

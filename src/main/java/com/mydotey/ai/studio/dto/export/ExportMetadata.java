package com.mydotey.ai.studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 导出包元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportMetadata {
    /**
     * 导出版本 (用于兼容性检查)
     */
    private String version = "1.0.0";

    /**
     * 导出时间
     */
    private LocalDateTime exportedAt;

    /**
     * 导出用户 ID
     */
    private Long exportedBy;

    /**
     * 导出用户名
     */
    private String exportedByName;

    /**
     * 导出组织 ID
     */
    private Long organizationId;

    /**
     * 导出范围
     */
    private ExportScope scope;

    /**
     * 包含的表
     */
    private Set<String> tables;

    /**
     * 数据统计
     */
    private DataStats stats;

    /**
     * 数据统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataStats {
        private int knowledgeBases;
        private int documents;
        private int agents;
        private int chatbots;
        private int conversations;
        private int messages;
        private int mcpServers;
    }
}

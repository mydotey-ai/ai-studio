package com.mydotey.ai.studio.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据导出响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataExportResponse {

    /**
     * 导出任务 ID (异步导出时)
     */
    private Long taskId;

    /**
     * 下载 URL
     */
    private String downloadUrl;

    /**
     * 文件大小 (bytes)
     */
    private Long fileSize;

    /**
     * 状态
     */
    private ExportStatus status;

    /**
     * 状态消息
     */
    private String message;

    /**
     * 导出统计
     */
    private ExportMetadata.DataStats stats;

    /**
     * 导出状态枚举
     */
    public enum ExportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}

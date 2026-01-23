package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class DocumentStats {
    private Long totalCount;
    private Long processingCount;
    private Long completedCount;
    private Long totalSizeBytes; // 文件总大小
}

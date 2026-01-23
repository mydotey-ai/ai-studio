package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class StorageStats {
    private Long totalSizeBytes;
    private Long fileCount;
    private Long localCount;
    private Long ossCount;
    private Long s3Count;
}

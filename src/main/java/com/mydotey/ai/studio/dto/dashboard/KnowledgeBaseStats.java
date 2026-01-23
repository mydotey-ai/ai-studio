package com.mydotey.ai.studio.dto.dashboard;

import lombok.Data;

@Data
public class KnowledgeBaseStats {
    private Long totalCount;
    private Long activeCount;
    private Long archivedCount;
    private Double weeklyGrowthRate; // 百分比
}

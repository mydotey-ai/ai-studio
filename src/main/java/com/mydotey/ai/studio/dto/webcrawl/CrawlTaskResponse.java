package com.mydotey.ai.studio.dto.webcrawl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlTaskResponse {
    private Long id;
    private Long kbId;
    private String startUrl;
    private String urlPattern;
    private Integer maxDepth;
    private String crawlStrategy;
    private Integer concurrentLimit;
    private String status;
    private Integer totalPages;
    private Integer successPages;
    private Integer failedPages;
    private String errorMessage;
    private Long createdBy;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

package com.mydotey.ai.studio.dto.webcrawl;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class CreateCrawlTaskRequest {
    @NotBlank(message = "Start URL is required")
    private String startUrl;

    @NotNull(message = "Knowledge base ID is required")
    private Long kbId;

    private String urlPattern;

    @Min(value = 1, message = "Max depth must be at least 1")
    @Max(value = 10, message = "Max depth must not exceed 10")
    private Integer maxDepth = 2;

    private String crawlStrategy = "BFS"; // BFS or DFS

    @Min(value = 1, message = "Concurrent limit must be at least 1")
    @Max(value = 10, message = "Concurrent limit must not exceed 10")
    private Integer concurrentLimit = 3;
}

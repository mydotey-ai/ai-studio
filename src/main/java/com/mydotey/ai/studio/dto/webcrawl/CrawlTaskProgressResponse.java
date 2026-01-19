package com.mydotey.ai.studio.dto.webcrawl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlTaskProgressResponse {
    private Long taskId;
    private String status;
    private Integer totalPages;
    private Integer successPages;
    private Integer failedPages;
    private Integer pendingPages;
    private String errorMessage;
    private List<WebPageResponse> pages;
}

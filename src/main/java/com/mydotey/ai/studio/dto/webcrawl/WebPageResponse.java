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
public class WebPageResponse {
    private Long id;
    private Long crawlTaskId;
    private Long documentId;
    private String url;
    private String title;
    private String status;
    private String errorMessage;
    private Integer depth;
    private Instant createdAt;
    private Instant updatedAt;
}

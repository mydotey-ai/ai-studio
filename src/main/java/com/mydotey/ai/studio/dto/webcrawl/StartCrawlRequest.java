package com.mydotey.ai.studio.dto.webcrawl;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartCrawlRequest {
    @NotNull(message = "Task ID is required")
    private Long taskId;
}

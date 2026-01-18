package com.mydotey.ai.studio.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class McpToolResponse {
    private Long id;
    private Long serverId;
    private String toolName;
    private String description;
    private String inputSchema;
    private String outputSchema;
    private Instant createdAt;
    private Instant updatedAt;
}

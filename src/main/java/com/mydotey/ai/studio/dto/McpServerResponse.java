package com.mydotey.ai.studio.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class McpServerResponse {
    private Long id;
    private String name;
    private String description;
    private String connectionType;
    private String command;
    private String workingDir;
    private String endpointUrl;
    private String headers;
    private String authType;
    private String status;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}

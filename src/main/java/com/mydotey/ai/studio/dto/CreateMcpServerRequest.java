package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMcpServerRequest {
    @NotBlank(message = "Server name is required")
    private String name;

    private String description;

    @NotBlank(message = "Connection type is required")
    private String connectionType;

    private String command;

    private String workingDir;

    private String endpointUrl;

    private String headers;

    private String authType;

    private String authConfig;
}

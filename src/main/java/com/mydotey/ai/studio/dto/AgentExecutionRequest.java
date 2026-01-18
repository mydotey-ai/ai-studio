package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class AgentExecutionRequest {
    @NotBlank(message = "Query is required")
    private String query;

    private Map<String, Object> context;

    private Boolean stream = false;
}

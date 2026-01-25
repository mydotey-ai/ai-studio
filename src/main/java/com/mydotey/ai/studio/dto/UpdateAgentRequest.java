package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.dto.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAgentRequest {
    @NotBlank(message = "Agent name is required")
    private String name;

    private String description;

    @NotBlank(message = "System prompt is required")
    private String systemPrompt;

    private Boolean isPublic;

    private String modelConfig;

    private WorkflowType workflowType;

    private String workflowConfig;

    private Integer maxIterations;

    private Long llmModelConfigId;

    private List<Long> knowledgeBaseIds;

    private List<Long> toolIds;
}

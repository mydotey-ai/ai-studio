package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.dto.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateAgentRequest {
    @NotBlank(message = "Agent name is required")
    private String name;

    private String description;

    @NotBlank(message = "System prompt is required")
    private String systemPrompt;

    private Boolean isPublic = false;

    @NotBlank(message = "Model config is required")
    private String modelConfig;

    private WorkflowType workflowType = WorkflowType.REACT;

    private String workflowConfig = "{}";

    private Integer maxIterations = 10;

    private Long llmModelConfigId;

    @NotEmpty(message = "At least one knowledge base is required")
    private List<Long> knowledgeBaseIds;

    private List<Long> toolIds;
}

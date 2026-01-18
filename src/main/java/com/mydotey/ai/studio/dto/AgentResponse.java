package com.mydotey.ai.studio.dto;

import com.mydotey.ai.studio.dto.WorkflowType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AgentResponse {
    private Long id;
    private String name;
    private String description;
    private String systemPrompt;
    private Boolean isPublic;
    private String modelConfig;
    private WorkflowType workflowType;
    private String workflowConfig;
    private Integer maxIterations;
    private List<Long> knowledgeBaseIds;
    private List<Long> toolIds;
    private Instant createdAt;
    private Instant updatedAt;
}

package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Boolean isPublic = false;

    private Long embeddingModelId;

    private Long llmModelId;

    private String embeddingModel = "text-embedding-3-small";

    private Integer chunkSize = 500;

    private Integer chunkOverlap = 100;
}

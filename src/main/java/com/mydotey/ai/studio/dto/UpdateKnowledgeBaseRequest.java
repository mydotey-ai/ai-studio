package com.mydotey.ai.studio.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateKnowledgeBaseRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private Boolean isPublic;

    private String embeddingModel;

    private Integer chunkSize;

    private Integer chunkOverlap;
}

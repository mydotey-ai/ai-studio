package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Boolean isPublic;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer documentCount;
    private Instant createdAt;
    private Instant updatedAt;
}

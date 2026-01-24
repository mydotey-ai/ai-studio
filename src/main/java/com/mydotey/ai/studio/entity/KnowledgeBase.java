package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("knowledge_bases")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String name;

    private String description;

    private Long ownerId;

    private Boolean isPublic;

    private String embeddingModel;

    private Long embeddingModelId;

    private Long llmModelId;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private String metadata;

    private Instant createdAt;

    private Instant updatedAt;
}

package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("document_chunks")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer chunkIndex;

    private String content;

    private String embedding;

    private String metadata;

    private Instant createdAt;
}

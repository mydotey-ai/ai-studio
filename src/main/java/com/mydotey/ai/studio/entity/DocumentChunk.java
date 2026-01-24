package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
import com.mydotey.ai.studio.handler.VectorTypeHandler;
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

    @TableField(typeHandler = VectorTypeHandler.class)
    private float[] embedding;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    private Instant createdAt;

    /**
     * 相似度分数（仅用于查询结果）
     */
    @TableField(exist = false)
    private Double similarityScore;
}

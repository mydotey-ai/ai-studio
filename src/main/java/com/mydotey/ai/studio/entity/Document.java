package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("documents")
public class Document {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String filename;

    private String fileUrl;

    private String fileType;

    private Long fileSize;

    private String status;

    private String errorMessage;

    private Integer chunkCount;

    private String sourceType;

    private String sourceUrl;

    private String metadata;

    private Instant createdAt;

    private Instant updatedAt;
}

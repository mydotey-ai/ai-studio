package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("file_metadata")
public class FileMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String originalFileName;

    private String filePath;

    private Long fileSize;

    private String contentType;

    private String storageType;

    private Long storageConfigId;

    private String bucketName;

    private String fileKey;

    private Long uploadedBy;

    private String relatedEntityType;

    private Long relatedEntityId;

    private Instant createdAt;

    private Instant updatedAt;
}

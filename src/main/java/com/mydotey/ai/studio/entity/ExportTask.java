package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.dto.export.ExportScope;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出任务实体
 */
@Data
@TableName("export_tasks")
public class ExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long organizationId;

    private ExportScope scope;

    private String status;

    private Long fileId;

    private Long fileSize;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

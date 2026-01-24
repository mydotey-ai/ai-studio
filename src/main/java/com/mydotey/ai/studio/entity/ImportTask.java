package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.dto.export.DataImportRequest.ImportStrategy;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入任务实体
 */
@Data
@TableName("import_tasks")
public class ImportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long organizationId;

    private Long fileId;

    private String status;

    private ImportStrategy strategy;

    private String stats;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

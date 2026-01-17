package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("audit_logs")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;

    private String resourceType;

    private Long resourceId;

    private String details;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;
}

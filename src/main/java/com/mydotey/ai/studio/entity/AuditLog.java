package com.mydotey.ai.studio.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mydotey.ai.studio.handler.JsonbTypeHandler;
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

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String details;

    private String ipAddress;

    private String userAgent;

    private Instant createdAt;
}

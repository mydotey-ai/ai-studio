package com.mydotey.ai.studio.dto.audit;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String details;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}

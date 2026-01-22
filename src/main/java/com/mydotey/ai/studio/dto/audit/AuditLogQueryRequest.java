package com.mydotey.ai.studio.dto.audit;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditLogQueryRequest {
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private Instant startDate;
    private Instant endDate;
    private Integer page = 1;
    private Integer pageSize = 20;
}

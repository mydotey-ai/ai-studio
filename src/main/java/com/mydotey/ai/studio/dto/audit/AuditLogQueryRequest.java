package com.mydotey.ai.studio.dto.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

/**
 * 审计日志查询请求DTO
 *
 * <p>用于审计日志的多条件查询，支持按用户、操作类型、资源类型、资源ID和时间范围筛选</p>
 *
 * @param userId 用户ID（可选）
 * @param action 操作类型（可选）
 * @param resourceType 资源类型（可选）
 * @param resourceId 资源ID（可选）
 * @param startDate 开始日期（可选）
 * @param endDate 结束日期（可选）
 * @param page 页码，从1开始（默认1）
 * @param pageSize 每页大小（默认20，最大100）
 */
@Data
public class AuditLogQueryRequest {
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private Instant startDate;
    private Instant endDate;

    @Min(value = 1, message = "Page number must be at least 1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must not exceed 100")
    private Integer pageSize = 20;
}

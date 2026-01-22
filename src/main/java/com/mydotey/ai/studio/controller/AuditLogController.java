package com.mydotey.ai.studio.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审计日志控制器
 *
 * <p>提供审计日志的查询功能，仅限管理员和超级管理员访问</p>
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询相关接口")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 查询审计日志
     *
     * <p>分页查询审计日志，支持按用户、操作类型、资源类型、资源ID和时间范围筛选</p>
     *
     * @param query 查询条件，包含分页参数和筛选条件
     * @return 分页结果，包含审计日志列表
     */
    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "查询审计日志", description = "分页查询审计日志,支持多种筛选条件")
    public ApiResponse<IPage<AuditLogResponse>> queryAuditLogs(
            @Valid @ModelAttribute AuditLogQueryRequest query) {
        IPage<AuditLogResponse> result = auditLogService.queryAuditLogs(query);
        return ApiResponse.success(result);
    }
}

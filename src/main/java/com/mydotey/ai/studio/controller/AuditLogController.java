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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "审计日志", description = "审计日志查询相关接口")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    @Operation(summary = "查询审计日志", description = "分页查询审计日志,支持多种筛选条件")
    public ApiResponse<IPage<AuditLogResponse>> queryAuditLogs(
            @ModelAttribute AuditLogQueryRequest query) {
        IPage<AuditLogResponse> result = auditLogService.queryAuditLogs(query);
        return ApiResponse.success(result);
    }
}

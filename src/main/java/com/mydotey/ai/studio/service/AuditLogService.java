package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mydotey.ai.studio.common.exception.BusinessException;
import com.mydotey.ai.studio.dto.audit.AuditLogQueryRequest;
import com.mydotey.ai.studio.dto.audit.AuditLogResponse;
import com.mydotey.ai.studio.entity.AuditLog;
import com.mydotey.ai.studio.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public void logAudit(Long userId, String action, String resourceType, Long resourceId, Object details) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);

            if (details != null) {
                auditLog.setDetails(objectMapper.writeValueAsString(details));
            } else {
                auditLog.setDetails("{}");
            }

            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }

            auditLog.setCreatedAt(Instant.now());

            auditLogMapper.insert(auditLog);

        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 查询审计日志
     *
     * <p>支持多条件查询和分页，包含参数验证和异常处理</p>
     *
     * @param query 查询条件
     * @return 分页查询结果
     * @throws BusinessException 当日期范围或分页参数无效时抛出
     */
    public IPage<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest query) {
        try {
            // 验证日期范围
            if (query.getStartDate() != null && query.getEndDate() != null) {
                if (query.getStartDate().isAfter(query.getEndDate())) {
                    throw new BusinessException("Start date must be before or equal to end date");
                }
            }

            // 验证分页参数
            if (query.getPageSize() != null && query.getPageSize() > 100) {
                throw new BusinessException("Page size must not exceed 100");
            }

            Page<AuditLogResponse> page = new Page<>(query.getPage(), query.getPageSize());
            return auditLogMapper.queryAuditLogs(page, query);

        } catch (BusinessException e) {
            log.error("Validation error when querying audit logs: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error when querying audit logs", e);
            throw new BusinessException("Failed to query audit logs", e);
        }
    }
}

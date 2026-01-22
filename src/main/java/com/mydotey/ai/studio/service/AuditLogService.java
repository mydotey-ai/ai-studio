package com.mydotey.ai.studio.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public IPage<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest query) {
        Page<AuditLogResponse> page = new Page<>(query.getPage(), query.getPageSize());
        return auditLogMapper.queryAuditLogs(page, query);
    }
}

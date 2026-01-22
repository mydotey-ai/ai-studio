package com.mydotey.ai.studio.dto.audit;

import lombok.Data;

import java.time.Instant;

/**
 * 审计日志响应DTO
 *
 * <p>用于返回审计日志查询结果，包含完整的审计信息</p>
 *
 * @param id 审计日志ID
 * @param userId 用户ID
 * @param username 用户名
 * @param action 操作类型（如 CREATE、UPDATE、DELETE等）
 * @param resourceType 资源类型
 * @param resourceId 资源ID
 * @param details 操作详情（JSON格式）
 * @param ipAddress 操作IP地址
 * @param userAgent 用户代理信息
 * @param createdAt 创建时间
 */
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

package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.AuditLog;
import com.mydotey.ai.studio.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        Long userId = null;

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            Object userIdObj = request.getAttribute("userId");
            if (userIdObj != null) {
                userId = (Long) userIdObj;
            }
        }

        // 获取资源 ID
        Long resourceId = null;
        if (!auditLog.resourceIdParam().isEmpty()) {
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();

            for (int i = 0; i < paramNames.length; i++) {
                if (paramNames[i].equals(auditLog.resourceIdParam())) {
                    if (args[i] instanceof Long) {
                        resourceId = (Long) args[i];
                    }
                    break;
                }
            }
        }

        Object result = joinPoint.proceed();

        // 记录审计日志
        auditLogService.logAudit(userId, auditLog.action(),
                auditLog.resourceType(), resourceId, null);

        return result;
    }
}

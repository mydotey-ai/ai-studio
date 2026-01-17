package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.RequireRole;
import com.mydotey.ai.studio.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requireRole)")
    public void checkPermission(JoinPoint joinPoint, RequireRole requireRole) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new AuthException("No request context");
        }

        HttpServletRequest request = attributes.getRequest();
        Object userRoleObj = request.getAttribute("userRole");

        if (userRoleObj == null) {
            throw new AuthException("Authentication required");
        }

        String userRole = (String) userRoleObj;
        String[] requiredRoles = requireRole.value();

        boolean hasPermission = requireRole.requireAll()
                ? Arrays.stream(requiredRoles).allMatch(role -> role.equals(userRole))
                : Arrays.stream(requiredRoles).anyMatch(role -> role.equals(userRole));

        if (!hasPermission) {
            log.warn("Permission denied: user role={}, required roles={}", userRole, Arrays.toString(requiredRoles));
            throw new AuthException("Permission denied");
        }
    }
}

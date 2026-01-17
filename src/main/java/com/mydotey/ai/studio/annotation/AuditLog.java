package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action();

    String resourceType() default "";

    String resourceIdParam() default "";
}

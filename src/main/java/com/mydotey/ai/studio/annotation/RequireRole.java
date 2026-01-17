package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 需要的角色列表
     */
    String[] value();

    /**
     * 是否需要匹配所有角色（AND），默认匹配任一角色（OR）
     */
    boolean requireAll() default false;
}

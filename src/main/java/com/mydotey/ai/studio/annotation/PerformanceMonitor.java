package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

/**
 * 性能监控注解
 * 用于标记需要监控执行性能的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerformanceMonitor {

    /**
     * 方法名称，用于指标标识
     */
    String value();

    /**
     * 是否记录方法参数
     */
    boolean logParams() default false;

    /**
     * 是否记录返回结果
     */
    boolean logResult() default false;

    /**
     * 慢方法阈值（毫秒），超过此阈值会记录为慢方法
     */
    long slowThreshold() default 1000;
}

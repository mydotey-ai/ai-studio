package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 性能监控 AOP 切面
 * 使用 Micrometer Timer 记录方法执行时间
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceMonitorAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(performanceMonitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, PerformanceMonitor performanceMonitor) throws Throwable {
        String methodName = performanceMonitor.value();
        long slowThreshold = performanceMonitor.slowThreshold();

        // 记录方法参数
        if (performanceMonitor.logParams()) {
            log.info("Method [{}] called with params: {}", methodName, Arrays.toString(joinPoint.getArgs()));
        }

        long startTime = System.currentTimeMillis();
        String status = "success";
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "failure";
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 记录 Micrometer Timer 指标
            Timer.builder("method.execution.time")
                    .tag("method", methodName)
                    .tag("status", status)
                    .description("Method execution time in milliseconds")
                    .register(meterRegistry)
                    .record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

            // 记录返回结果
            if (performanceMonitor.logResult() && "success".equals(status)) {
                log.info("Method [{}] returned: {}", methodName, result);
            }

            // 检查是否为慢方法
            if (duration > slowThreshold) {
                log.warn("Slow method detected: [{}] took {}ms (threshold: {}ms)",
                        methodName, duration, slowThreshold);
            }

            log.debug("Method [{}] executed in {}ms with status: {}", methodName, duration, status);
        }
    }
}

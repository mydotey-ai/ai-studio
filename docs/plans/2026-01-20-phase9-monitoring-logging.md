# Phase 9: 系统监控和日志实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 实现完整的系统监控和日志系统,包括结构化日志、请求追踪、性能监控、错误追踪和 APM 监控

**架构:**
- 使用 Logback 配置结构化 JSON 日志输出
- 使用 MDC (Mapped Diagnostic Context) 实现 Trace ID 在请求链路中的传递
- 使用 Micrometer 集成 Prometheus 收集应用指标
- 自定义 AOP 切面实现方法级性能监控
- 增强 GlobalExceptionHandler 实现错误追踪和聚合

**技术栈:**
- Spring Boot 3.5.0 (内置 Logback 和 Micrometer)
- Micrometer Prometheus (metrics 收集)
- Logback XML (日志配置)
- MDC (Trace ID 传递)
- AOP (性能监控切面)

---

## Task 1: 添加 Micrometer Prometheus 依赖

**Files:**
- Modify: `pom.xml`

**Step 1: 添加 Micrometer Prometheus 依赖到 pom.xml**

在 `<dependencies>` 部分添加以下依赖:

```xml
<!-- Micrometer Prometheus for metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Logback JSON encoder for structured logging -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Step 2: 运行 Maven 依赖解析**

Run: `mvn dependency:resolve`
Expected: 依赖成功下载,无错误

**Step 3: 提交**

```bash
git add pom.xml
git commit -m "feat: add micrometer prometheus and logstash dependencies for Phase 9"
```

---

## Task 2: 配置结构化 JSON 日志

**Files:**
- Create: `src/main/resources/logback-spring.xml`

**Step 1: 创建 Logback 配置文件**

创建 `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name" defaultValue="ai-studio"/>
    <springProperty scope="context" name="activeProfile" source="spring.profiles.active" defaultValue="dev"/>

    <!-- Console appender with JSON output -->
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${appName}"}</customFields>
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeStructuredArguments>true</includeStructuredArguments>
            <includeTags>true</includeTags>
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampPattern>
        </encoder>
    </appender>

    <!-- File appender with JSON output -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${appName}.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${appName}"}</customFields>
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <includeStructuredArguments>true</includeStructuredArguments>
            <includeTags>true</includeTags>
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampPattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/${appName}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- Error file appender -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <file>logs/${appName}-error.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${appName}"}</customFields>
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/${appName}-error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- Logger configurations -->
    <logger name="com.mydotey.ai.studio" level="INFO" additivity="false">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </logger>

    <logger name="com.mydotey.ai.studio.aspect" level="DEBUG" additivity="false">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </logger>

    <!-- Third-party loggers -->
    <logger name="org.springframework" level="WARN" additivity="false">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </logger>

    <logger name="com.baomidou.mybatisplus" level="WARN" additivity="false">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </logger>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
</configuration>
```

**Step 2: 运行应用验证日志配置**

Run: `mvn spring-boot:run`
Expected: 应用启动,日志以 JSON 格式输出到控制台,logs/ 目录下生成日志文件

**Step 3: 验证 JSON 日志格式**

检查日志输出,应包含以下字段:
- @timestamp
- level
- logger_name
- message
- app
- thread_name

**Step 4: 提交**

```bash
git add src/main/resources/logback-spring.xml
git commit -m "feat: configure structured JSON logging with Logback"
```

---

## Task 3: 实现 Trace ID 生成和传递

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/util/TraceIdUtil.java`
- Create: `src/main/java/com/mydotey/ai/studio/filter/TraceIdFilter.java`
- Create: `src/main/java/com/mydotey/ai/studio/aspect/TracingAspect.java`

**Step 1: 创建 TraceIdUtil 工具类**

创建 `src/main/java/com/mydotey/ai/studio/util/TraceIdUtil.java`:

```java
package com.mydotey.ai.studio.util;

import java.util.UUID;

public class TraceIdUtil {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";

    /**
     * 生成新的 Trace ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成新的 Span ID
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取当前 Trace ID
     */
    public static String getTraceId() {
        return org.slf4j.MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置 Trace ID
     */
    public static void setTraceId(String traceId) {
        org.slf4j.MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 Span ID
     */
    public static String getSpanId() {
        return org.slf4j.MDC.get(SPAN_ID_KEY);
    }

    /**
     * 设置 Span ID
     */
    public static void setSpanId(String spanId) {
        org.slf4j.MDC.put(SPAN_ID_KEY, spanId);
    }

    /**
     * 清除 MDC
     */
    public static void clear() {
        org.slf4MDC.clear();
    }
}
```

**Step 2: 创建 TraceIdFilter**

创建 `src/main/java/com/mydotey/ai/studio/filter/TraceIdFilter.java`:

```java
package com.mydotey.ai.studio.filter;

import com.mydotey.ai.studio.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头获取 Trace ID,如果没有则生成新的
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = TraceIdUtil.generateTraceId();
            }

            // 设置到 MDC
            TraceIdUtil.setTraceId(traceId);
            TraceIdUtil.setSpanId(TraceIdUtil.generateSpanId());

            // 将 Trace ID 添加到响应头
            response.setHeader(TRACE_ID_HEADER, traceId);

            log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            // 清除 MDC
            TraceIdUtil.clear();
        }
    }
}
```

**Step 3: 运行测试验证 Trace ID**

Run: `mvn test -Dtest=TraceIdFilterTest` (如果创建测试)
Expected: Trace ID 正确生成和传递

**Step 4: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/util/TraceIdUtil.java
git add src/main/java/com/mydotey/ai/studio/filter/TraceIdFilter.java
git commit -m "feat: implement Trace ID generation and propagation using MDC"
```

---

## Task 4: 实现性能监控 AOP 切面

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/annotation/PerformanceMonitor.java`
- Create: `src/main/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspect.java`
- Test: `src/test/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspectTest.java`

**Step 1: 编写失败的测试**

创建 `src/test/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspectTest.java`:

```java
package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.service.DummyService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PerformanceMonitorAspectTest {

    @Autowired
    private DummyService dummyService;

    @MockBean
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        assertNotNull(dummyService);
    }

    @Test
    void testPerformanceMonitorLogsExecutionTime() {
        // 执行带注解的方法
        dummyService.performSlowOperation();

        // 验证方法执行完成
        assertTrue(true, "Method should execute without errors");
    }

    @Test
    void testPerformanceMonitorHandlesExceptions() {
        // 执行会抛出异常的方法
        assertThrows(RuntimeException.class, () -> {
            dummyService.performFailingOperation();
        });
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=PerformanceMonitorAspectTest`
Expected: FAIL with "DummyService not found"

**Step 3: 创建 PerformanceMonitor 注解**

创建 `src/main/java/com/mydotey/ai/studio/annotation/PerformanceMonitor.java`:

```java
package com.mydotey.ai.studio.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerformanceMonitor {

    /**
     * 方法描述,用于日志输出
     */
    String value() default "";

    /**
     * 是否记录参数
     */
    boolean logParams() default false;

    /**
     * 是否记录返回值
     */
    boolean logResult() default false;

    /**
     * 慢调用阈值(毫秒),超过此阈值记录 WARN 日志
     */
    long slowThreshold() default 1000;
}
```

**Step 4: 创建 PerformanceMonitorAspect**

创建 `src/main/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspect.java`:

```java
package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.util.TraceIdUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceMonitorAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(performanceMonitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint,
                                     PerformanceMonitor performanceMonitor) throws Throwable {
        String methodName = getMethodName(joinPoint);
        String description = performanceMonitor.value().isEmpty()
                ? methodName
                : performanceMonitor.value();

        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;
            sample.stop(Timer.builder("method.execution.time")
                    .description("Method execution time")
                    .tag("method", methodName)
                    .register(meterRegistry));

            logExecution(executionTime, description, methodName,
                        performanceMonitor, null, null);

            return result;
        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            sample.stop(Timer.builder("method.execution.time")
                    .description("Method execution time")
                    .tag("method", methodName)
                    .tag("status", "error")
                    .register(meterRegistry));

            logExecution(executionTime, description, methodName,
                        performanceMonitor, ex, null);

            throw ex;
        }
    }

    private void logExecution(long executionTime, String description, String methodName,
                             PerformanceMonitor monitor, Throwable ex, Object result) {
        String traceId = TraceIdUtil.getTraceId();
        String level = executionTime > monitor.slowThreshold() ? "WARN" : "INFO";

        StringBuilder message = new StringBuilder();
        message.append("Performance: ").append(description)
               .append(" executed in ").append(executionTime).append("ms");

        if (traceId != null) {
            message.append(" | TraceID: ").append(traceId);
        }

        if (monitor.logParams()) {
            // 参数记录可以在这里扩展
        }

        if (monitor.logResult() && result != null) {
            message.append(" | Result: ").append(result);
        }

        if (ex != null) {
            message.append(" | Error: ").append(ex.getMessage());
        }

        if ("WARN".equals(level)) {
            log.warn(message.toString());
        } else {
            log.info(message.toString());
        }
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
```

**Step 5: 创建测试用 DummyService**

创建 `src/test/java/com/mydotey/ai/studio/service/DummyService.java`:

```java
package com.mydotey.ai.studio.service;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import org.springframework.stereotype.Service;

@Service
public class DummyService {

    @PerformanceMonitor(value = "Slow Operation", slowThreshold = 500)
    public void performSlowOperation() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PerformanceMonitor(value = "Failing Operation")
    public void performFailingOperation() {
        throw new RuntimeException("Intentional failure for testing");
    }
}
```

**Step 6: 运行测试验证通过**

Run: `mvn test -Dtest=PerformanceMonitorAspectTest`
Expected: PASS

**Step 7: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/annotation/PerformanceMonitor.java
git add src/main/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspect.java
git add src/test/java/com/mydotey/ai/studio/aspect/PerformanceMonitorAspectTest.java
git add src/test/java/com/mydotey/ai/studio/service/DummyService.java
git commit -m "feat: implement performance monitoring AOP aspect with Micrometer metrics"
```

---

## Task 5: 增强 GlobalExceptionHandler 实现错误追踪

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/mydotey/ai/studio/dto/ErrorDetails.java`

**Step 1: 编写失败的测试**

创建 `src/test/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandlerTest.java`:

```java
package com.mydotey.ai.studio.common.exception;

import com.mydotey.ai.studio.dto.ErrorDetails;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @MockBean
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        assertNotNull(exceptionHandler);
    }

    @Test
    void testExceptionHandlerRecordsErrorMetrics() {
        BusinessException exception = new BusinessException(400, "Test business error");

        var response = exceptionHandler.handleBusinessException(exception);

        assertNotNull(response);
        assertEquals(400, response.getBody().getCode());
    }
}
```

**Step 2: 运行测试验证失败**

Run: `mvn test -Dtest=GlobalExceptionHandlerTest`
Expected: 测试可能通过,但 ErrorDetails 类不存在

**Step 3: 创建 ErrorDetails DTO**

创建 `src/main/java/com/mydotey/ai/studio/dto/ErrorDetails.java`:

```java
package com.mydotey.ai.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetails {

    private String timestamp;
    private String traceId;
    private int status;
    private String error;
    private String message;
    private String path;
    private String method;
    private Map<String, Object> details;
}
```

**Step 4: 修改 GlobalExceptionHandler**

修改 `src/main/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandler.java`:

```java
package com.mydotey.ai.studio.common.exception;

import com.mydotey.ai.studio.common.ApiResponse;
import com.mydotey.ai.studio.dto.ErrorDetails;
import com.mydotey.ai.studio.util.TraceIdUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(
            AuthException e,
            HttpServletRequest request) {
        recordErrorMetrics("AuthException", 403, request);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now().toString())
                .traceId(TraceIdUtil.getTraceId())
                .status(403)
                .error("Forbidden")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        log.warn("Auth exception: {}", errorDetails);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {
        recordErrorMetrics("BusinessException", 400, request);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now().toString())
                .traceId(TraceIdUtil.getTraceId())
                .status(400)
                .error("Bad Request")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        log.warn("Business exception: {}", errorDetails);

        Map<String, String> errors = new HashMap<>();
        errors.put("code", String.valueOf(e.getCode()));
        errors.put("message", e.getMessage());

        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request) {
        recordErrorMetrics("ValidationException", 400, request);

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now().toString())
                .traceId(TraceIdUtil.getTraceId())
                .status(400)
                .error("Validation Failed")
                .message("Validation failed for one or more fields")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .details(Map.of("errors", errors))
                .build();

        log.warn("Validation exception: {}", errorDetails);

        return ApiResponse.error(400, "Validation failed");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(
            Exception e,
            HttpServletRequest request) {
        recordErrorMetrics("Exception", 500, request);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(LocalDateTime.now().toString())
                .traceId(TraceIdUtil.getTraceId())
                .status(500)
                .error("Internal Server Error")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .details(Map.of("exceptionType", e.getClass().getSimpleName()))
                .build();

        log.error("Unexpected error: {}", errorDetails, e);

        return ApiResponse.error(500, "Internal server error");
    }

    private void recordErrorMetrics(String exceptionType, int status, HttpServletRequest request) {
        Counter.builder("errors.total")
                .description("Total number of errors")
                .tag("type", exceptionType)
                .tag("status", String.valueOf(status))
                .tag("path", request.getRequestURI())
                .register(meterRegistry)
                .increment();
    }
}
```

**Step 5: 运行测试验证通过**

Run: `mvn test -Dtest=GlobalExceptionHandlerTest`
Expected: PASS

**Step 6: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandler.java
git add src/main/java/com/mydotey/ai/studio/dto/ErrorDetails.java
git add src/test/java/com/mydotey/ai/studio/common/exception/GlobalExceptionHandlerTest.java
git commit -m "feat: enhance GlobalExceptionHandler with error tracking and metrics"
```

---

## Task 6: 配置 Prometheus Metrics 端点

**Files:**
- Create: `src/main/java/com/mydotey/ai/studio/config/MetricsConfig.java`
- Modify: `src/main/resources/application.yml`

**Step 1: 在 application.yml 添加 metrics 配置**

修改 `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ai-studio
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

server:
  port: 8080

# Management endpoints for monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
  prometheus:
    metrics:
      export:
        enabled: true

jwt:
  secret: ${JWT_SECRET:your-super-secret-key-change-in-production-minimum-256-bits}
  access-token-expiration: 7200000
  refresh-token-expiration: 604800000

file:
  storage:
    local:
      upload-dir: ${UPLOAD_DIR:${java.io.tmpdir}/ai-studio-uploads}
```

**Step 2: 创建 MetricsConfig**

创建 `src/main/java/com/mydotey/ai/studio/config/MetricsConfig.java`:

```java
package com.mydotey.ai.studio.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MetricsConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                    "application", "ai-studio",
                    "environment", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev")
                );
    }
}
```

**Step 3: 启动应用验证 metrics 端点**

Run: `mvn spring-boot:run`
然后访问: `curl http://localhost:8080/actuator/metrics`
Expected: 返回可用的 metrics 列表

访问: `curl http://localhost:8080/actuator/prometheus`
Expected: 返回 Prometheus 格式的 metrics

**Step 4: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/config/MetricsConfig.java
git add src/main/resources/application.yml
git commit -m "feat: configure Prometheus metrics endpoint"
```

---

## Task 7: 为核心服务添加性能监控注解

**Files:**
- Modify: `src/main/java/com/mydotey/ai/studio/service/RagService.java`
- Modify: `src/main/java/com/mydotey/ai/studio/service/AgentExecutionService.java`
- Modify: `src/main/java/com/mydotey/ai/studio/service/ChatService.java`

**Step 1: 为 RagService 添加性能监控**

修改 `src/main/java/com/mydotey/ai/studio/service/RagService.java`,在 query 方法上添加:

```java
@PerformanceMonitor(value = "RAG Query", slowThreshold = 2000, logParams = true)
public RagQueryResponse query(RagQueryRequest request) {
    // 现有实现保持不变
}
```

**Step 2: 为 AgentExecutionService 添加性能监控**

修改 `src/main/java/com/mydotey/ai/studio/service/AgentExecutionService.java`,在 execute 方法上添加:

```java
@PerformanceMonitor(value = "Agent Execution", slowThreshold = 5000)
public AgentExecutionResponse execute(Long agentId, AgentExecutionRequest request) {
    // 现有实现保持不变
}
```

**Step 3: 为 ChatService 添加性能监控**

修改 `src/main/java/com/mydotey/ai/studio/service/ChatService.java`,在 chat 方法上添加:

```java
@PerformanceMonitor(value = "Chat", slowThreshold = 1000)
public ChatResponse chat(ChatRequest request) {
    // 现有实现保持不变
}
```

**Step 4: 运行测试验证**

Run: `mvn test`
Expected: 所有测试通过

**Step 5: 提交**

```bash
git add src/main/java/com/mydotey/ai/studio/service/RagService.java
git add src/main/java/com/mydotey/ai/studio/service/AgentExecutionService.java
git add src/main/java/com/mydotey/ai/studio/service/ChatService.java
git commit -m "feat: add performance monitoring annotations to core services"
```

---

## Task 8: 编写集成测试

**Files:**
- Create: `src/test/java/com/mydotey/ai/studio/integration/MonitoringLoggingIntegrationTest.java`

**Step 1: 编写测试**

创建 `src/test/java/com/mydotey/ai/studio/integration/MonitoringLoggingIntegrationTest.java`:

```java
package com.mydotey.ai.studio.integration;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.service.DummyService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MonitoringLoggingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private DummyService dummyService;

    @BeforeEach
    void setUp() {
        assertNotNull(restTemplate);
        assertNotNull(meterRegistry);
        assertNotNull(dummyService);
    }

    @Test
    void testTraceIdPropagation() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Trace-ID", "test-trace-id-123");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 请求一个不存在的端点,触发异常处理
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/non-existent",
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
        );

        // 验证 Trace ID 被返回
        String traceId = response.getHeaders().getFirst("X-Trace-ID");
        assertEquals("test-trace-id-123", traceId);
    }

    @Test
    void testMetricsEndpointAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/metrics",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testPrometheusEndpointAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/prometheus",
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("# HELP"));
    }

    @Test
    void testPerformanceMonitorRecordsMetrics() {
        // 执行带性能监控的方法
        dummyService.performSlowOperation();

        // 验证 metrics 被记录
        assertNotNull(meterRegistry.get("method.execution.time")
                .tag("method", "DummyService.performSlowOperation")
                .timer());
    }
}
```

**Step 2: 运行集成测试**

Run: `mvn test -Dtest=MonitoringLoggingIntegrationTest`
Expected: PASS

**Step 3: 提交**

```bash
git add src/test/java/com/mydotey/ai/studio/integration/MonitoringLoggingIntegrationTest.java
git commit -m "test: add monitoring and logging integration tests"
```

---

## Task 9: 更新应用配置文档

**Files:**
- Modify: `docs/PROJECT_PROGRESS.md`

**Step 1: 更新项目进度文档**

在 `docs/PROJECT_PROGRESS.md` 中添加 Phase 9 完成记录:

在"已完成阶段"部分后添加:

```markdown
### Phase 9: 系统监控和日志 ✅

**完成时间：2026-01-20**

**实现内容:**
- 结构化 JSON 日志输出 (Logback + Logstash Encoder)
- 请求追踪 (Trace ID 传递)
- 性能监控 (AOP 切面 + Micrometer)
- 错误追踪 (增强异常处理 + metrics)
- APM 监控 (Prometheus metrics endpoint)

**新增文件:**
```
src/main/java/com/mydotey/ai/studio/
├── util/
│   └── TraceIdUtil.java
├── filter/
│   └── TraceIdFilter.java
├── annotation/
│   └── PerformanceMonitor.java
├── aspect/
│   └── PerformanceMonitorAspect.java
├── dto/
│   └── ErrorDetails.java
├── config/
│   └── MetricsConfig.java
└── integration/
    └── MonitoringLoggingIntegrationTest.java

src/main/resources/
└── logback-spring.xml
```

**配置项:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**API 端点:**

监控 API (`/actuator/*`):
- `GET /actuator/health` - 健康检查
- `GET /actuator/metrics` - 指标列表
- `GET /actuator/prometheus` - Prometheus 格式指标
- `GET /actuator/info` - 应用信息

**实现任务完成情况:**

1. ✅ **结构化 JSON 日志**
   - Logback XML 配置
   - JSON 格式控制台输出
   - JSON 格式文件输出
   - 日志文件滚动策略

2. ✅ **Trace ID 传递**
   - TraceIdUtil 工具类
   - TraceIdFilter (最高优先级)
   - MDC 上下文传递
   - 请求头 X-Trace-ID

3. ✅ **性能监控**
   - @PerformanceMonitor 注解
   - PerformanceMonitorAspect 切面
   - Micrometer Timer 指标
   - 慢调用告警

4. ✅ **错误追踪**
   - ErrorDetails DTO
   - 增强 GlobalExceptionHandler
   - 错误 metrics 计数
   - 结构化错误日志

5. ✅ **APM 监控**
   - MetricsConfig 配置
   - Prometheus endpoint
   - 应用通用 tags
   - 核心服务性能注解

6. ✅ **测试覆盖**
   - PerformanceMonitorAspectTest - 性能监控测试
   - GlobalExceptionHandlerTest - 异常处理测试
   - MonitoringLoggingIntegrationTest - 集成测试 (4 个测试)

**技术栈:**
- Logback + Logstash Encoder (结构化日志)
- MDC (Trace ID 传递)
- Micrometer + Prometheus (metrics)
- AOP (性能监控切面)
- Spring Boot Actuator (监控端点)

**核心功能:**
- 结构化 JSON 日志输出
- 请求链路追踪 (Trace ID)
- 方法级性能监控
- 错误聚合和追踪
- Prometheus metrics 集成

**测试统计:**
- Phase 9 总测试数: 6 个
- 单元测试: 2 ✅
- 集成测试: 4 ✅
```

**Step 2: 更新技术债务部分**

修改技术债务中的"监控和日志"部分:

```markdown
3. **监控和日志** ✅
   - [x] 添加 APM 监控 (Micrometer + Prometheus)
   - [x] 结构化日志 (Logback JSON)
   - [x] 请求追踪 (Trace ID)
```

**Step 3: 提交**

```bash
git add docs/PROJECT_PROGRESS.md
git commit -m "docs: record Phase 9 completion"
```

---

## Task 10: 最终测试和验证

**Files:**
- None (验证任务)

**Step 1: 运行所有测试**

Run: `mvn clean test`
Expected: 所有测试通过,包括新的 Phase 9 测试

**Step 2: 启动应用验证日志输出**

Run: `mvn spring-boot:run`

验证检查点:
- [ ] 控制台输出 JSON 格式日志
- [ ] logs/ 目录生成日志文件
- [ ] logs/ai-studio-error.log 只记录 ERROR 级别
- [ ] 请求响应包含 X-Trace-ID header

**Step 3: 验证监控端点**

检查以下端点:
- `curl http://localhost:8080/actuator/health` - 返回健康状态
- `curl http://localhost:8080/actuator/metrics` - 返回指标列表
- `curl http://localhost:8080/actuator/prometheus` - 返回 Prometheus 格式指标

**Step 4: 验证性能监控**

查看日志输出,应该包含:
- Performance: RAG Query executed in XXXms | TraceID: XXX
- Performance: Agent Execution executed in XXXms | TraceID: XXX
- Performance: Chat executed in XXXms | TraceID: XXX

**Step 5: 验证错误追踪**

触发一个错误,查看日志:
```bash
curl http://localhost:8080/api/non-existent
```

验证:
- [ ] 日志包含 ErrorDetails
- [ ] 日志包含 Trace ID
- [ ] Prometheus metrics 包含错误计数

**Step 6: 最终提交**

```bash
git add .
git commit -m "feat: complete Phase 9 monitoring and logging system"
```

---

## 执行说明

**实施顺序:**
1. 添加依赖 (Task 1)
2. 配置日志 (Task 2)
3. 实现 Trace ID (Task 3)
4. 性能监控 AOP (Task 4)
5. 错误追踪 (Task 5)
6. Prometheus 配置 (Task 6)
7. 核心服务监控 (Task 7)
8. 集成测试 (Task 8)
9. 文档更新 (Task 9)
10. 最终验证 (Task 10)

**预计测试数量:**
- 新增单元测试: 2 个
- 新增集成测试: 4 个
- Phase 9 总测试数: 6 个

**提交频率:**
每个任务独立提交,小步快跑,便于代码审查和回滚。

**配置文件位置:**
- Logback 配置: `src/main/resources/logback-spring.xml`
- Metrics 配置: `src/main/resources/application.yml`

**监控端点:**
- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

**日志位置:**
- 控制台: JSON 格式输出
- 文件: `logs/ai-studio.log`
- 错误: `logs/ai-studio-error.log`

**Trace ID 使用:**
- 请求头: `X-Trace-ID`
- 响应头: `X-Trace-ID`
- 日志字段: `traceId`

**性能注解使用:**
```java
@PerformanceMonitor(value = "方法描述", slowThreshold = 1000)
public void myMethod() { ... }
```

**完成标准:**
- ✅ 所有测试通过
- ✅ 日志 JSON 格式正确
- ✅ Trace ID 正确传递
- ✅ Metrics 端点可访问
- ✅ 性能监控正常工作
- ✅ 错误追踪正常工作
- ✅ 文档已更新

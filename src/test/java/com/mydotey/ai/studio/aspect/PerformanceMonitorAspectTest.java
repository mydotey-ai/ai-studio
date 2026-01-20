package com.mydotey.ai.studio.aspect;

import com.mydotey.ai.studio.annotation.PerformanceMonitor;
import com.mydotey.ai.studio.config.MeterTestConfig;
import com.mydotey.ai.studio.service.DummyService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(MeterTestConfig.class)
@DisplayName("性能监控 AOP 切面测试")
class PerformanceMonitorAspectTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @SpyBean
    private PerformanceMonitorAspect performanceMonitorAspect;

    @Autowired
    private DummyService dummyService;

    @BeforeEach
    void setUp() {
        // Clear any existing timers
        meterRegistry.clear();
    }

    @Test
    @DisplayName("应该记录方法执行时间和成功次数")
    void testShouldRecordMethodExecutionTimeAndSuccessCount() {
        // When
        String result = dummyService.fastMethod("test");

        // Then
        assertNotNull(result);
        assertEquals("test-success", result);

        // Verify timer was recorded
        Timer timer = meterRegistry.get("method.execution.time")
                .tag("method", "fastMethod")
                .tag("status", "success")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 0);
    }

    @Test
    @DisplayName("应该记录方法执行时间和失败次数")
    void testShouldRecordMethodExecutionTimeAndFailureCount() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dummyService.slowMethod("error");
        });

        // Verify timer was recorded for failure
        Timer timer = meterRegistry.get("method.execution.time")
                .tag("method", "slowMethod")
                .tag("status", "failure")
                .timer();

        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 0);
    }

    @Test
    @DisplayName("应该记录方法参数和返回值")
    void testShouldLogMethodParametersAndResult() {
        // When
        String result = dummyService.methodWithLogging("param1", 42);

        // Then
        assertNotNull(result);
        assertEquals("param1-42", result);

        // Verify timer with additional tags
        Timer timer = meterRegistry.get("method.execution.time")
                .tag("method", "methodWithLogging")
                .tag("status", "success")
                .timer();

        assertNotNull(timer);
    }

    @Test
    @DisplayName("应该记录慢方法并标记超过阈值")
    void testShouldRecordSlowMethodWithThreshold() throws InterruptedException {
        // When
        String result = dummyService.slowMethod("slow");

        // Then
        assertNotNull(result);

        // Verify slow method was recorded
        Timer timer = meterRegistry.get("method.execution.time")
                .tag("method", "slowMethod")
                .tag("status", "success")
                .timer();

        assertNotNull(timer);
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 100);
    }
}

package com.mydotey.ai.studio.filter;

import com.mydotey.ai.studio.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration test to verify Trace ID propagation through the application.
 * This test verifies that:
 * 1. Trace IDs can be set and retrieved from MDC
 * 2. Trace IDs are properly cleaned up
 * 3. Span IDs work correctly with Trace IDs
 *
 * Note: The JSON log output shows that traceId and spanId are automatically
 * included in logs via LogstashEncoder's includeMdc=true configuration.
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TraceIdIntegrationTest {

    @Test
    void testMdcContainsTraceId() {
        String testTraceId = "test-mdc-trace-id-12345";
        log.info("Testing MDC Trace ID propagation");

        try {
            TraceIdUtil.setTraceId(testTraceId);
            String mdcTraceId = MDC.get("traceId");

            assert mdcTraceId != null : "Trace ID should be present in MDC";
            assert mdcTraceId.equals(testTraceId) : "Trace ID in MDC should match the set value";

            // Log a message to verify Trace ID appears in JSON output
            log.info("This log should contain traceId: {}", testTraceId);
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void testMdcContainsSpanId() {
        String testSpanId = "test-span-id-67890";
        log.info("Testing MDC Span ID propagation");

        try {
            TraceIdUtil.setSpanId(testSpanId);
            String mdcSpanId = MDC.get("spanId");

            assert mdcSpanId != null : "Span ID should be present in MDC";
            assert mdcSpanId.equals(testSpanId) : "Span ID in MDC should match the set value";

            // Log a message to verify Span ID appears in JSON output
            log.info("This log should contain spanId: {}", testSpanId);
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void testTraceIdAndSpanIdTogether() {
        String testTraceId = "integrated-trace-id";
        String testSpanId = "integrated-span-id";
        log.info("Testing Trace ID and Span ID together");

        try {
            TraceIdUtil.setTraceId(testTraceId);
            TraceIdUtil.setSpanId(testSpanId);

            String mdcTraceId = MDC.get("traceId");
            String mdcSpanId = MDC.get("spanId");

            assert mdcTraceId != null && mdcTraceId.equals(testTraceId);
            assert mdcSpanId != null && mdcSpanId.equals(testSpanId);

            // Log a message to verify both appear in JSON output
            log.info("This log should contain traceId and spanId");
        } finally {
            TraceIdUtil.clear();
        }
    }

    @Test
    void testMdcClearRemovesAll() {
        log.info("Testing MDC clear functionality");

        TraceIdUtil.setTraceId("test-trace");
        TraceIdUtil.setSpanId("test-span");

        assert MDC.get("traceId") != null;
        assert MDC.get("spanId") != null;

        TraceIdUtil.clear();

        assert MDC.get("traceId") == null : "Trace ID should be null after clear";
        assert MDC.get("spanId") == null : "Span ID should be null after clear";

        log.info("MDC cleared successfully");
    }
}

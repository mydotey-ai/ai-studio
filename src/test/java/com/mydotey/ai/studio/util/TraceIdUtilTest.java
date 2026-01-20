package com.mydotey.ai.studio.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdUtilTest {

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear();
    }

    @Test
    void testGenerateTraceId() {
        String traceId = TraceIdUtil.generateTraceId();

        assertNotNull(traceId, "Generated Trace ID should not be null");
        assertEquals(32, traceId.length(), "Trace ID should be 32 characters (UUID without dashes)");
        assertFalse(traceId.contains("-"), "Trace ID should not contain dashes");

        // Generate another Trace ID and verify it's different
        String anotherTraceId = TraceIdUtil.generateTraceId();
        assertNotEquals(traceId, anotherTraceId, "Each generated Trace ID should be unique");
    }

    @Test
    void testGenerateSpanId() {
        String spanId = TraceIdUtil.generateSpanId();

        assertNotNull(spanId, "Generated Span ID should not be null");
        assertEquals(16, spanId.length(), "Span ID should be 16 characters");
        assertFalse(spanId.contains("-"), "Span ID should not contain dashes");

        // Generate another Span ID and verify it's different
        String anotherSpanId = TraceIdUtil.generateSpanId();
        assertNotEquals(spanId, anotherSpanId, "Each generated Span ID should be unique");
    }

    @Test
    void testSetAndGetTraceId() {
        String testTraceId = "test-trace-id-12345";

        TraceIdUtil.setTraceId(testTraceId);
        String retrievedTraceId = TraceIdUtil.getTraceId();

        assertEquals(testTraceId, retrievedTraceId, "Should retrieve the same Trace ID that was set");
    }

    @Test
    void testSetAndGetSpanId() {
        String testSpanId = "test-span-id-6789";

        TraceIdUtil.setSpanId(testSpanId);
        String retrievedSpanId = TraceIdUtil.getSpanId();

        assertEquals(testSpanId, retrievedSpanId, "Should retrieve the same Span ID that was set");
    }

    @Test
    void testGetTraceIdWhenNotSet() {
        String traceId = TraceIdUtil.getTraceId();
        assertNull(traceId, "Should return null when Trace ID is not set");
    }

    @Test
    void testGetSpanIdWhenNotSet() {
        String spanId = TraceIdUtil.getSpanId();
        assertNull(spanId, "Should return null when Span ID is not set");
    }

    @Test
    void testClear() {
        // Set both Trace ID and Span ID
        TraceIdUtil.setTraceId("test-trace-id");
        TraceIdUtil.setSpanId("test-span-id");

        // Verify they are set
        assertNotNull(TraceIdUtil.getTraceId());
        assertNotNull(TraceIdUtil.getSpanId());

        // Clear MDC
        TraceIdUtil.clear();

        // Verify they are cleared
        assertNull(TraceIdUtil.getTraceId(), "Trace ID should be null after clear");
        assertNull(TraceIdUtil.getSpanId(), "Span ID should be null after clear");
    }

    @Test
    void testMultipleTraceIds() {
        String traceId1 = TraceIdUtil.generateTraceId();
        String traceId2 = TraceIdUtil.generateTraceId();
        String traceId3 = TraceIdUtil.generateTraceId();

        // All should be different
        assertNotEquals(traceId1, traceId2);
        assertNotEquals(traceId2, traceId3);
        assertNotEquals(traceId1, traceId3);

        // All should be valid format
        assertEquals(32, traceId1.length());
        assertEquals(32, traceId2.length());
        assertEquals(32, traceId3.length());
    }

    @Test
    void testMultipleSpanIds() {
        String spanId1 = TraceIdUtil.generateSpanId();
        String spanId2 = TraceIdUtil.generateSpanId();
        String spanId3 = TraceIdUtil.generateSpanId();

        // All should be different
        assertNotEquals(spanId1, spanId2);
        assertNotEquals(spanId2, spanId3);
        assertNotEquals(spanId1, spanId3);

        // All should be valid format
        assertEquals(16, spanId1.length());
        assertEquals(16, spanId2.length());
        assertEquals(16, spanId3.length());
    }
}

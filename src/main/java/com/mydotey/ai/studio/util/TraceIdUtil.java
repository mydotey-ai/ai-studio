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
        org.slf4j.MDC.clear();
    }
}

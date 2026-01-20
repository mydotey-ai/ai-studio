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

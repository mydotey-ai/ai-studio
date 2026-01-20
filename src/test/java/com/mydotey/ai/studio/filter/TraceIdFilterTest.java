package com.mydotey.ai.studio.filter;

import com.mydotey.ai.studio.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    @InjectMocks
    private TraceIdFilter traceIdFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TraceIdUtil.clear(); // Ensure clean state
    }

    @AfterEach
    void tearDown() {
        TraceIdUtil.clear(); // Clean up after each test
    }

    @Test
    void testGenerateNewTraceIdWhenNotProvided() throws ServletException, IOException {
        // Given: request without X-Trace-ID header
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        // When: filter processes the request
        traceIdFilter.doFilterInternal(request, response, filterChain);

        // Then: verify response has X-Trace-ID header
        String responseTraceId = response.getHeader("X-Trace-ID");
        assertNotNull(responseTraceId, "Trace ID should be generated and added to response");
        assertEquals(32, responseTraceId.length(), "Trace ID should be 32 characters (UUID without dashes)");

        // And: verify filter chain was called
        verify(filterChain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void testUseExistingTraceIdWhenProvided() throws ServletException, IOException {
        // Given: request with X-Trace-ID header
        String existingTraceId = "existing-trace-id-12345";
        request.addHeader("X-Trace-ID", existingTraceId);
        request.setRequestURI("/api/test");
        request.setMethod("POST");

        // When: filter processes the request
        traceIdFilter.doFilterInternal(request, response, filterChain);

        // Then: verify response uses the same Trace ID
        String responseTraceId = response.getHeader("X-Trace-ID");
        assertEquals(existingTraceId, responseTraceId, "Should use existing Trace ID from request header");
    }

    @Test
    void testMdcClearedAfterRequest() throws ServletException, IOException {
        // Given: request without Trace ID
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        // When: filter processes the request
        traceIdFilter.doFilterInternal(request, response, filterChain);

        // Then: MDC should be cleared after request processing
        String traceIdAfter = TraceIdUtil.getTraceId();
        assertNull(traceIdAfter, "MDC should be cleared after request completes");
    }

    @Test
    void testTraceIdSetInMdcDuringRequest() throws ServletException, IOException {
        // Given: request with Trace ID
        String testTraceId = "test-trace-id-67890";
        request.addHeader("X-Trace-ID", testTraceId);
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        // When: filter processes the request (we need to capture during execution)
        doAnswer(invocation -> {
            // During filter chain execution, Trace ID should be in MDC
            String traceIdInMdc = TraceIdUtil.getTraceId();
            assertEquals(testTraceId, traceIdInMdc, "Trace ID should be set in MDC during request processing");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        traceIdFilter.doFilterInternal(request, response, filterChain);
    }

    @Test
    void testSpanIdSetInMdc() throws ServletException, IOException {
        // Given: request
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        // When: filter processes the request
        doAnswer(invocation -> {
            // During filter chain execution, Span ID should be in MDC
            String spanIdInMdc = TraceIdUtil.getSpanId();
            assertNotNull(spanIdInMdc, "Span ID should be set in MDC");
            assertEquals(16, spanIdInMdc.length(), "Span ID should be 16 characters");
            return null;
        }).when(filterChain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        traceIdFilter.doFilterInternal(request, response, filterChain);
    }

    @Test
    void testEmptyTraceIdHeaderGeneratesNew() throws ServletException, IOException {
        // Given: request with empty X-Trace-ID header
        request.addHeader("X-Trace-ID", "");
        request.setRequestURI("/api/test");
        request.setMethod("GET");

        // When: filter processes the request
        traceIdFilter.doFilterInternal(request, response, filterChain);

        // Then: new Trace ID should be generated
        String responseTraceId = response.getHeader("X-Trace-ID");
        assertNotNull(responseTraceId, "Should generate new Trace ID when header is empty");
        assertFalse(responseTraceId.isEmpty(), "Generated Trace ID should not be empty");
    }

    @Test
    void testFilterChainAlwaysExecuted() throws ServletException, IOException {
        // Given: request
        request.setRequestURI("/api/test");
        request.setMethod("DELETE");

        // When: filter processes the request
        traceIdFilter.doFilterInternal(request, response, filterChain);

        // Then: filter chain should always be executed
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

package com.mydotey.ai.studio.common.exception;

import com.mydotey.ai.studio.common.ApiResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler
 * Tests error handling, metrics recording, and structured logging
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MeterRegistry meterRegistry;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        exceptionHandler = new GlobalExceptionHandler(meterRegistry);
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
    }

    @Test
    void handleAuthException_ShouldReturnForbiddenStatusAndRecordMetric() {
        // Given
        AuthException exception = new AuthException("Authentication failed");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAuthException(exception, request);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getCode());
        assertEquals("Authentication failed", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());

        // Verify metric was recorded
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "AuthException")
                .tag("status", "403")
                .counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count(), 0.01);
    }

    @Test
    void handleBusinessException_ShouldReturnBadRequestAndRecordMetric() {
        // Given
        BusinessException exception = new BusinessException(400, "Invalid business logic");

        // When
        ApiResponse<Void> response = exceptionHandler.handleBusinessException(exception, request);

        // Then
        assertEquals(400, response.getCode());
        assertEquals("Invalid business logic", response.getMessage());
        assertNotNull(response.getTimestamp());

        // Verify metric was recorded
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "BusinessException")
                .tag("status", "400")
                .counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count(), 0.01);
    }

    @Test
    void handleBusinessException_WithCustomCode_ShouldUseCustomCode() {
        // Given
        BusinessException exception = new BusinessException(409, "Resource conflict");

        // When
        ApiResponse<Void> response = exceptionHandler.handleBusinessException(exception, request);

        // Then
        assertEquals(409, response.getCode());
        assertEquals("Resource conflict", response.getMessage());

        // Verify metric with custom status code
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "BusinessException")
                .tag("status", "409")
                .counter();
        assertNotNull(errorCounter);
    }

    @Test
    void handleValidationException_ShouldReturnValidationErrorsAndRecordMetric() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError1 = new FieldError("object", "email", "invalid", false, null, null, "Email is required");
        FieldError fieldError2 = new FieldError("object", "password", "invalid", false, null, null, "Password is too short");
        when(exception.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ApiResponse<?> response = exceptionHandler.handleValidationException(exception, request);

        // Then
        assertEquals(400, response.getCode());
        assertEquals("Validation failed", response.getMessage());
        assertNotNull(response.getData());

        // Verify metric was recorded
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "MethodArgumentNotValidException")
                .tag("status", "400")
                .counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count(), 0.01);
    }

    @Test
    void handleException_ShouldReturnInternalServerErrorAndRecordMetric() {
        // Given
        Exception exception = new RuntimeException("Unexpected system error");

        // When
        ApiResponse<Void> response = exceptionHandler.handleException(exception, request);

        // Then
        assertEquals(500, response.getCode());
        assertEquals("Internal server error", response.getMessage());
        assertNotNull(response.getTimestamp());

        // Verify metric was recorded
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "RuntimeException")
                .tag("status", "500")
                .counter();
        assertNotNull(errorCounter);
        assertEquals(1.0, errorCounter.count(), 0.01);
    }

    @Test
    void handleException_WithNullException_ShouldHandleGracefully() {
        // Given
        Exception exception = new NullPointerException();

        // When
        ApiResponse<Void> response = exceptionHandler.handleException(exception, request);

        // Then
        assertEquals(500, response.getCode());
        assertNotNull(response.getMessage());

        // Verify metric was recorded for NullPointerException
        Counter errorCounter = meterRegistry.find("errors.total")
                .tag("type", "NullPointerException")
                .tag("status", "500")
                .counter();
        assertNotNull(errorCounter);
    }

    @Test
    void multipleExceptionHandlers_ShouldRecordSeparateMetrics() {
        // When - Handle multiple different exceptions
        exceptionHandler.handleAuthException(new AuthException("Auth failed"), request);
        exceptionHandler.handleBusinessException(new BusinessException(400, "Business error"), request);
        exceptionHandler.handleException(new RuntimeException("System error"), request);

        // Then - Verify separate counters for each type
        Counter authCounter = meterRegistry.find("errors.total")
                .tag("type", "AuthException")
                .counter();
        assertEquals(1.0, authCounter.count(), 0.01);

        Counter businessCounter = meterRegistry.find("errors.total")
                .tag("type", "BusinessException")
                .counter();
        assertEquals(1.0, businessCounter.count(), 0.01);

        Counter runtimeCounter = meterRegistry.find("errors.total")
                .tag("type", "RuntimeException")
                .counter();
        assertEquals(1.0, runtimeCounter.count(), 0.01);
    }

    @Test
    void repeatedExceptions_ShouldIncrementMetricCounter() {
        // When - Handle same exception multiple times
        exceptionHandler.handleBusinessException(new BusinessException(400, "Error 1"), request);
        exceptionHandler.handleBusinessException(new BusinessException(400, "Error 2"), request);
        exceptionHandler.handleBusinessException(new BusinessException(400, "Error 3"), request);

        // Then - Verify counter incremented
        Counter counter = meterRegistry.find("errors.total")
                .tag("type", "BusinessException")
                .tag("status", "400")
                .counter();
        assertNotNull(counter);
        assertEquals(3.0, counter.count(), 0.01);
    }

    @Test
    void errorDetails_ShouldContainRequiredFields() {
        // Given
        ErrorDetails details = ErrorDetails.builder()
                .timestamp(java.time.Instant.now())
                .status(500)
                .error("Internal Server Error")
                .message("Something went wrong")
                .path("/api/test")
                .traceId("trace-123")
                .exceptionType("RuntimeException")
                .build();

        // Then
        assertNotNull(details.getTimestamp());
        assertEquals(500, details.getStatus());
        assertEquals("Internal Server Error", details.getError());
        assertEquals("Something went wrong", details.getMessage());
        assertEquals("/api/test", details.getPath());
        assertEquals("trace-123", details.getTraceId());
        assertEquals("RuntimeException", details.getExceptionType());
    }

    @Test
    void errorDetails_ConstructorWithoutOptionalFields_ShouldWork() {
        // Given
        ErrorDetails details = new ErrorDetails(
                java.time.Instant.now(),
                404,
                "Not Found",
                "Resource not found"
        );

        // Then
        assertNotNull(details.getTimestamp());
        assertEquals(404, details.getStatus());
        assertEquals("Not Found", details.getError());
        assertEquals("Resource not found", details.getMessage());
        assertNull(details.getPath());
        assertNull(details.getTraceId());
        assertNull(details.getExceptionType());
    }
}

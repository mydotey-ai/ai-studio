package com.mydotey.ai.studio.common.exception;

import com.mydotey.ai.studio.common.ApiResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler with metrics tracking and structured logging
 * Records metrics for all exceptions and logs with Trace ID for distributed tracing
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;
    private final Counter errorCounter;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.errorCounter = Counter.builder("errors.total")
                .description("Total number of errors")
                .register(meterRegistry);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        int status = HttpStatus.FORBIDDEN.value();

        recordErrorMetric("AuthException", status);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(java.time.Instant.now())
                .status(status)
                .error("Forbidden")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .exceptionType("AuthException")
                .build();

        log.error("Authentication failed: traceId={}, path={}, error={}",
                traceId, request.getRequestURI(), errorDetails);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(status, e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        int status = e.getCode();

        recordErrorMetric("BusinessException", status);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(java.time.Instant.now())
                .status(status)
                .error(status == 401 ? "Unauthorized" : "Bad Request")
                .message(e.getMessage())
                .path(request.getRequestURI())
                .traceId(traceId)
                .exceptionType("BusinessException")
                .build();

        log.warn("Business exception: traceId={}, path={}, error={}",
                traceId, request.getRequestURI(), errorDetails);

        return ResponseEntity.status(status)
                .body(ApiResponse.error(status, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        int status = HttpStatus.BAD_REQUEST.value();

        recordErrorMetric("MethodArgumentNotValidException", status);

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(java.time.Instant.now())
                .status(status)
                .error("Validation Failed")
                .message("Request validation failed")
                .path(request.getRequestURI())
                .traceId(traceId)
                .exceptionType("MethodArgumentNotValidException")
                .build();

        log.warn("Validation failed: traceId={}, path={}, error={}, fieldErrors={}",
                traceId, request.getRequestURI(), errorDetails, errors);

        return new ApiResponse<>(400, "Validation failed", errors, java.time.Instant.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = getTraceId(request);
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String exceptionType = e.getClass().getSimpleName();

        recordErrorMetric(exceptionType, status);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .timestamp(java.time.Instant.now())
                .status(status)
                .error("Internal Server Error")
                .message(e.getMessage() != null ? e.getMessage() : "Unexpected error occurred")
                .path(request.getRequestURI())
                .traceId(traceId)
                .exceptionType(exceptionType)
                .build();

        log.error("Unexpected error: traceId={}, path={}, error={}",
                traceId, request.getRequestURI(), errorDetails, e);

        return ApiResponse.error(500, "Internal server error");
    }

    /**
     * Records error metrics with exception type and status code tags
     */
    private void recordErrorMetric(String exceptionType, int status) {
        Counter.builder("errors.total")
                .tag("type", exceptionType)
                .tag("status", String.valueOf(status))
                .description("Total number of errors by type and status")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Extracts trace ID from request headers for distributed tracing
     * Supports common tracing headers: X-Trace-Id, X-Request-Id, X-Correlation-Id
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = request.getHeader("X-Request-Id");
        }
        if (traceId == null) {
            traceId = request.getHeader("X-Correlation-Id");
        }
        return traceId != null ? traceId : "N/A";
    }
}

package com.sism.exception;

import com.sism.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for REST controllers.
 * Converts exceptions to unified ErrorResponse format with comprehensive logging.
 * 
 * Validates: Requirements 3.1.1, 3.1.3, 3.1.5
 * Validates: P10 - 所有错误响应包含 requestId 用于追踪
 * 
 * Requirements:
 * - 15.4: Complete stack trace for errors
 * - 15.5: Request context in error logs
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle business exceptions (expected application errors).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse<Void>> handleBusinessException(BusinessException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Business exception: code={}, message={}, requestId={}, context=[{}]", 
                e.getCode(), e.getMessage(), requestId, requestContext);
        
        String errorCode = mapBusinessCodeToErrorCode(e.getCode());
        ErrorResponse<Void> response = ErrorResponse.of(errorCode, e.getMessage(), requestId);
        
        return ResponseEntity
                .status(getHttpStatus(e.getCode()))
                .body(response);
    }
    
    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Resource not found: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ErrorResponse<Void> response = ErrorResponse.of("BIZ_002", e.getMessage(), requestId);
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
    
    /**
     * Handle unauthorized exceptions.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Unauthorized access: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ErrorResponse<Void> response = ErrorResponse.of("AUTH_003", e.getMessage(), requestId);
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle conflict exceptions.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse<Void>> handleConflictException(ConflictException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Conflict: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ErrorResponse<Void> response = ErrorResponse.of("BIZ_001", e.getMessage(), requestId);
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    
    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, Object>>> handleValidationException(
            MethodArgumentNotValidException e) {
        String requestId = getOrGenerateRequestId();
        
        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> details = new HashMap<>();
        details.put("errors", fieldErrors);
        
        String requestContext = getRequestContext();
        log.warn("Validation failed: errors={}, requestId={}, context=[{}]", 
                fieldErrors, requestId, requestContext);
        
        ErrorResponse<Map<String, Object>> response = ErrorResponse.of(
                "VAL_001", 
                "Validation failed", 
                details, 
                requestId
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse<Void>> handleValidationException(ValidationException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Validation error: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ErrorResponse<Void> response = ErrorResponse.of("VAL_002", e.getMessage(), requestId);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Illegal argument: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ErrorResponse<Void> response = ErrorResponse.of("VAL_002", e.getMessage(), requestId);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle all other unexpected exceptions with full stack trace.
     * This is the catch-all handler for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<Void>> handleGenericException(Exception e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        // Log with full stack trace and request context
        log.error("Unexpected error occurred: message={}, type={}, requestId={}, context=[{}]", 
                e.getMessage(), 
                e.getClass().getName(), 
                requestId,
                requestContext, 
                e);  // This will include full stack trace
        
        ErrorResponse<Void> response = ErrorResponse.of("SYS_001", "Internal server error", requestId);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    /**
     * Get request ID from MDC or generate a new one.
     * The request ID should be set by RequestLoggingFilter or passed in X-Request-ID header.
     * 
     * @return Request ID
     */
    private String getOrGenerateRequestId() {
        // First try to get from MDC (set by RequestLoggingFilter)
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        
        // Try to get from request header
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String headerRequestId = request.getHeader("X-Request-ID");
                if (headerRequestId != null && !headerRequestId.isBlank()) {
                    return headerRequestId;
                }
            }
        } catch (Exception ignored) {
            // Ignore if request context is not available
        }
        
        // Generate a new request ID as fallback
        return UUID.randomUUID().toString();
    }
    
    /**
     * Build request context string for logging.
     * Includes request ID, user ID, method, URI, and client IP from MDC.
     */
    private String getRequestContext() {
        StringBuilder context = new StringBuilder();
        
        // Get values from MDC (set by RequestLoggingFilter)
        String requestId = MDC.get("requestId");
        String userId = MDC.get("userId");
        String clientIp = MDC.get("clientIp");
        String requestMethod = MDC.get("requestMethod");
        String requestUri = MDC.get("requestUri");
        
        context.append("requestId=").append(requestId != null ? requestId : "N/A");
        context.append(", userId=").append(userId != null ? userId : "N/A");
        context.append(", clientIp=").append(clientIp != null ? clientIp : "N/A");
        context.append(", method=").append(requestMethod != null ? requestMethod : "N/A");
        context.append(", uri=").append(requestUri != null ? requestUri : "N/A");
        
        // Try to get additional info from request if available
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && !userAgent.isBlank()) {
                    // Truncate user agent to avoid overly long logs
                    if (userAgent.length() > 100) {
                        userAgent = userAgent.substring(0, 100) + "...";
                    }
                    context.append(", userAgent=").append(userAgent);
                }
            }
        } catch (Exception ignored) {
            // Ignore if request context is not available
        }
        
        return context.toString();
    }
    
    /**
     * Map business error code to standard error code format.
     */
    private String mapBusinessCodeToErrorCode(int code) {
        return switch (code) {
            case 400 -> "VAL_001";
            case 401 -> "AUTH_003";
            case 403 -> "AUTH_004";
            case 404 -> "BIZ_002";
            case 409 -> "BIZ_001";
            case 422 -> "VAL_002";
            case 429 -> "RATE_001";
            case 500 -> "SYS_001";
            default -> "BIZ_003";
        };
    }
    
    /**
     * Map error code to HTTP status.
     */
    private HttpStatus getHttpStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

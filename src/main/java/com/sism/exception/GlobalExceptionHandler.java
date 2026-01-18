package com.sism.exception;

import com.sism.common.ApiResponse;
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

/**
 * Global exception handler for REST controllers.
 * Converts exceptions to unified ApiResponse format with comprehensive logging.
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
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        String requestContext = getRequestContext();
        log.warn("Business exception: code={}, message={}, context=[{}]", 
                e.getCode(), e.getMessage(), requestContext);
        return ResponseEntity
                .status(getHttpStatus(e.getCode()))
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    
    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        String requestContext = getRequestContext();
        log.warn("Resource not found: message={}, context=[{}]", e.getMessage(), requestContext);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, e.getMessage()));
    }
    
    /**
     * Handle unauthorized exceptions.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        String requestContext = getRequestContext();
        log.warn("Unauthorized access: message={}, context=[{}]", e.getMessage(), requestContext);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, e.getMessage()));
    }
    
    /**
     * Handle conflict exceptions.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException e) {
        String requestContext = getRequestContext();
        log.warn("Conflict: message={}, context=[{}]", e.getMessage(), requestContext);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, e.getMessage()));
    }
    
    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String requestContext = getRequestContext();
        log.warn("Validation failed: errors={}, context=[{}]", errors, requestContext);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Validation failed", errors));
    }
    
    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException e) {
        String requestContext = getRequestContext();
        log.warn("Validation error: message={}, context=[{}]", e.getMessage(), requestContext);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, e.getMessage()));
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        String requestContext = getRequestContext();
        log.warn("Illegal argument: message={}, context=[{}]", e.getMessage(), requestContext);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, e.getMessage()));
    }
    
    /**
     * Handle all other unexpected exceptions with full stack trace.
     * This is the catch-all handler for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        String requestContext = getRequestContext();
        
        // Log with full stack trace and request context
        log.error("Unexpected error occurred: message={}, type={}, context=[{}]", 
                e.getMessage(), 
                e.getClass().getName(), 
                requestContext, 
                e);  // This will include full stack trace
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error"));
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

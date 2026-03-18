package com.sism.exception;

import com.sism.common.ApiResponse;
import com.sism.shared.domain.exception.AuthenticationException;
import com.sism.shared.domain.exception.AuthorizationException;
import com.sism.shared.domain.exception.TechnicalException;
import com.sism.shared.infrastructure.nplusone.NPlusOneQueryException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for REST controllers.
 * Converts exceptions to unified ApiResponse format matching API documentation.
 * 
 * Response format:
 * - Success: {"code": 200, "message": "...", "data": {...}}
 * - Error: {"code": 1001, "message": "...", "errors": [...], "timestamp": "..."}
 * 
 * Validates: Requirements 3.1.1, 3.1.3, 3.1.5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    /**
     * Handle business exceptions (expected application errors).
     * Maps to API doc error codes: 1000-1999
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Business exception: code={}, message={}, requestId={}, context=[{}]", 
                e.getCode(), e.getMessage(), requestId, requestContext);
        
        int errorCode = mapBusinessCodeToApiErrorCode(e.getCode());
        ApiResponse<Void> response = ApiResponse.error(errorCode, e.getMessage());
        
        return ResponseEntity
                .status(getHttpStatus(e.getCode()))
                .body(response);
    }
    
    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Resource not found: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ApiResponse<Void> response = ApiResponse.error(1002, e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
    
    /**
     * Handle unauthorized exceptions.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Unauthorized access: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ApiResponse<Void> response = ApiResponse.error(2002, e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle conflict exceptions.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Conflict: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ApiResponse<Void> response = ApiResponse.error(1003, e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    
    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e) {
        String requestId = getOrGenerateRequestId();

        List<Map<String, String>> fieldErrors = new ArrayList<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", fieldName);
            fieldError.put("message", errorMessage);
            fieldErrors.add(fieldError);
        });

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String requestContext = getRequestContext();
        log.warn("Validation failed: errors={}, requestId={}, context=[{}]",
                fieldErrors, requestId, requestContext);

        ApiResponse<Object> response = ApiResponse.error(1001, "参数验证失败", fieldErrors, timestamp);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();
        
        log.warn("Validation error: message={}, requestId={}, context=[{}]", 
                e.getMessage(), requestId, requestContext);
        
        ApiResponse<Void> response = ApiResponse.error(1001, e.getMessage());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Illegal argument: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1001, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Illegal state: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1000, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle shared business exceptions from DDD layer.
     */
    @ExceptionHandler(com.sism.shared.domain.exception.BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleSharedBusinessException(com.sism.shared.domain.exception.BusinessException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Shared business exception: code={}, message={}, requestId={}, context=[{}]",
                e.getCode(), e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1000, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle shared resource not found exceptions from DDD layer.
     */
    @ExceptionHandler(com.sism.shared.domain.exception.ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSharedResourceNotFoundException(com.sism.shared.domain.exception.ResourceNotFoundException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Shared resource not found: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1002, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle authentication exceptions from DDD layer.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Authentication failed: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(2001, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    /**
     * Handle authorization exceptions from DDD layer.
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationException(AuthorizationException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Authorization failed: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(2003, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle technical exceptions from DDD layer.
     */
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ApiResponse<Void>> handleTechnicalException(TechnicalException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.error("Technical error: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext, e);

        ApiResponse<Void> response = ApiResponse.error(3001, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    /**
     * Handle workflow exceptions.
     */
    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflowException(WorkflowException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Workflow exception: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1000, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle N+1 query detection exceptions.
     */
    @ExceptionHandler(NPlusOneQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleNPlusOneQueryException(NPlusOneQueryException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("N+1 query detected: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1000, e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle endpoint not found (404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Resource endpoint not found: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1002, "接口不存在");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle method not supported (405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Method not supported: method={}, message={}, requestId={}, context=[{}]",
                e.getMethod(), e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(1001, "请求方法不支持");

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }

    /**
     * Handle access denied (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.warn("Access denied: message={}, requestId={}, context=[{}]",
                e.getMessage(), requestId, requestContext);

        ApiResponse<Void> response = ApiResponse.error(2003, "无权限访问");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle all other unexpected exceptions with full stack trace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        String requestId = getOrGenerateRequestId();
        String requestContext = getRequestContext();

        log.error("Unexpected error occurred: message={}, type={}, requestId={}, context=[{}]",
                e.getMessage(),
                e.getClass().getName(),
                requestId,
                requestContext,
                e);

        ApiResponse<Void> response = ApiResponse.error(1000, "系统错误");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    /**
     * Get request ID from MDC or generate a new one.
     */
    private String getOrGenerateRequestId() {
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        
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
        }
        
        return UUID.randomUUID().toString();
    }
    
    /**
     * Build request context string for logging.
     */
    private String getRequestContext() {
        StringBuilder context = new StringBuilder();
        
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
        
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && !userAgent.isBlank()) {
                    if (userAgent.length() > 100) {
                        userAgent = userAgent.substring(0, 100) + "...";
                    }
                    context.append(", userAgent=").append(userAgent);
                }
            }
        } catch (Exception ignored) {
        }
        
        return context.toString();
    }
    
    /**
     * Map business error code to API documentation error code (number format).
     * API doc uses: 1000-1999 for general, 2000-2999 for auth, etc.
     */
    private int mapBusinessCodeToApiErrorCode(int code) {
        return switch (code) {
            case 400 -> 1001;  // Parameter validation failed
            case 401 -> 2002; // Unauthorized
            case 403 -> 2003; // Forbidden
            case 404 -> 1002; // Data not found
            case 409 -> 1003; // Data already exists
            case 422 -> 1004; // Operation not allowed
            case 429 -> 1008; // Rate limit
            case 500 -> 1000; // System error
            default -> 1000;
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

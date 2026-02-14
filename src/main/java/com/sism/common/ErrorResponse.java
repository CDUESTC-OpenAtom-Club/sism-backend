package com.sism.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 统一错误响应格式
 * 
 * 定义前后端统一的错误响应格式，用于错误追踪和处理
 * 
 * Validates: Requirements 3.1.1, 3.1.5
 * Validates: P10 - 所有错误响应包含 requestId 用于追踪
 * 
 * @param <T> 详细错误信息的类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse<T> {
    
    /**
     * 错误码，格式为 "模块_编号"，如 "AUTH_001"
     */
    private String code;
    
    /**
     * 用户友好的错误消息
     */
    private String message;
    
    /**
     * 详细错误信息，仅在开发环境返回
     */
    private T details;
    
    /**
     * 请求 ID，用于日志关联和问题追踪
     */
    private String requestId;
    
    /**
     * ISO 8601 格式时间戳
     */
    private String timestamp;
    
    /**
     * 创建错误响应
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param requestId 请求 ID
     * @return 错误响应
     */
    public static ErrorResponse<Void> of(String code, String message, String requestId) {
        return ErrorResponse.<Void>builder()
                .code(code)
                .message(message)
                .requestId(requestId)
                .timestamp(Instant.now().toString())
                .build();
    }
    
    /**
     * 创建带详情的错误响应
     * 
     * @param code 错误码
     * @param message 错误消息
     * @param details 详细信息
     * @param requestId 请求 ID
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> of(String code, String message, T details, String requestId) {
        return ErrorResponse.<T>builder()
                .code(code)
                .message(message)
                .details(details)
                .requestId(requestId)
                .timestamp(Instant.now().toString())
                .build();
    }
    
    /**
     * 从 HTTP 状态码获取默认错误码
     * 
     * @param status HTTP 状态码
     * @return 错误码
     */
    public static String getCodeByStatus(int status) {
        return switch (status) {
            case 400 -> "VAL_001";
            case 401 -> "AUTH_003";
            case 403 -> "AUTH_004";
            case 404 -> "BIZ_002";
            case 409 -> "BIZ_001";
            case 422 -> "VAL_002";
            case 429 -> "RATE_001";
            case 500 -> "SYS_001";
            case 502, 503, 504 -> "SYS_003";
            default -> "SYS_001";
        };
    }
}

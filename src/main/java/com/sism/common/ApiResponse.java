package com.sism.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Unified API response wrapper
 * Provides consistent response format across all endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * Response code: 0 = success, non-zero = error code
     */
    private int code;
    
    /**
     * Success or error message
     */
    private String message;
    
    /**
     * Response payload
     */
    private T data;
    
    /**
     * Response timestamp (Unix epoch milliseconds)
     */
    private long timestamp;
    
    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "Success", data, Instant.now().toEpochMilli());
    }
    
    /**
     * Create a successful response with custom message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data, Instant.now().toEpochMilli());
    }
    
    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "Success", null, Instant.now().toEpochMilli());
    }
    
    /**
     * Create an error response with code and message
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now().toEpochMilli());
    }
    
    /**
     * Create an error response with code, message, and error data
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, Instant.now().toEpochMilli());
    }
}

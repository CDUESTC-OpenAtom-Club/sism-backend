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
     * Success flag (derived from code)
     */
    private boolean success;

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
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessage("Success");
        response.setData(data);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }

    /**
     * Create a successful response with custom message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }

    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setCode(0);
        response.setMessage("Success");
        response.setData(null);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }

    /**
     * Create an error response with code and message
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }

    /**
     * Create an error response with code, message, and error data
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(Instant.now().toEpochMilli());
        return response;
    }
}

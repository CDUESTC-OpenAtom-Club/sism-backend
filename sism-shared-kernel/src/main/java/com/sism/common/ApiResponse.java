package com.sism.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Unified API response wrapper
 * Provides consistent response format across all endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);
    
    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;

    /**
     * Success flag
     */
    private boolean success;

    /**
     * Response code: 200 = success, non-200 = error code
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
     * Response timestamp (ISO 8601 format)
     */
    private String timestamp;

    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = SUCCESS_CODE;
        response.message = "Success";
        response.data = data;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create a successful response with custom message and data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = SUCCESS_CODE;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = SUCCESS_CODE;
        response.message = "Success";
        response.data = null;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create a successful response with message only
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = SUCCESS_CODE;
        response.message = message;
        response.data = null;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create an error response with message only (uses default error code 500)
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = ERROR_CODE;
        response.message = message;
        response.data = null;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create an error response with code and message
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        response.data = null;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create an error response with code, message, and error data
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now().format(FORMATTER);
        return response;
    }

    /**
     * Create an error response with code, message, errors, and timestamp
     */
    public static <T> ApiResponse<T> error(int code, String message, T data, String timestamp) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        response.data = data;
        response.timestamp = timestamp;
        return response;
    }
}

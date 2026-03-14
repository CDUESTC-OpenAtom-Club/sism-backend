package com.sism.shared.domain.exception;

import java.time.LocalDateTime;

/**
 * 业务异常基类
 * 用于处理业务规则相关的异常
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final LocalDateTime timestamp;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.timestamp = LocalDateTime.now();
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
        this.timestamp = LocalDateTime.now();
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

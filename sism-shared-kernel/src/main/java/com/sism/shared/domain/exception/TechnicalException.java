package com.sism.shared.domain.exception;

/**
 * 技术异常基类
 * 用于处理系统级别、技术相关的异常
 */
public class TechnicalException extends RuntimeException {

    private final String code;

    public TechnicalException(String message) {
        super(message);
        this.code = "TECHNICAL_ERROR";
    }

    public TechnicalException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
        this.code = "TECHNICAL_ERROR";
    }

    public TechnicalException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

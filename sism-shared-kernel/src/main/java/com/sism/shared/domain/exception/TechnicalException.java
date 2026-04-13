package com.sism.shared.domain.exception;

/**
 * 技术异常基类
 * 用于处理系统级别、技术相关的异常
 */
public class TechnicalException extends BusinessException {

    public TechnicalException(String message) {
        super("TECHNICAL_ERROR", message);
    }

    public TechnicalException(String code, String message) {
        super(code, message);
    }

    public TechnicalException(String message, Throwable cause) {
        super("TECHNICAL_ERROR", message, cause);
    }

    public TechnicalException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}

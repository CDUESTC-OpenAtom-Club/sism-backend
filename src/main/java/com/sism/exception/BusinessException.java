package com.sism.exception;

import lombok.Getter;

/**
 * Base business exception class
 * All custom business exceptions should extend this class
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * Error code
     */
    private final int code;
    
    /**
     * Error message
     */
    private final String message;
    
    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.message = message;
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}

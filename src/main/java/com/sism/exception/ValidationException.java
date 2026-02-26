package com.sism.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Exception thrown when validation fails
 */
@Getter
public class ValidationException extends BusinessException {
    
    /**
     * Field-level validation errors
     */
    private final Map<String, String> fieldErrors;
    
    public ValidationException(Map<String, String> fieldErrors) {
        super(400, "Validation failed");
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message) {
        super(400, message);
        this.fieldErrors = null;
    }
}

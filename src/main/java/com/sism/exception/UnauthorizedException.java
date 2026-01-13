package com.sism.exception;

/**
 * Exception thrown when authentication fails or is required
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(401, message);
    }
    
    public UnauthorizedException() {
        super(401, "Unauthorized access");
    }
}

package com.sism.shared.infrastructure.nplusone;

/**
 * Exception thrown when N+1 query problem is detected
 */
public class NPlusOneQueryException extends RuntimeException {
    
    public NPlusOneQueryException(String message) {
        super(message);
    }
    
    public NPlusOneQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}

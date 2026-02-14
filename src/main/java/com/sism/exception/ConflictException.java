package com.sism.exception;

/**
 * Exception thrown when a data conflict occurs (e.g., optimistic locking failure)
 */
public class ConflictException extends BusinessException {
    
    public ConflictException(String message) {
        super(409, message);
    }
}

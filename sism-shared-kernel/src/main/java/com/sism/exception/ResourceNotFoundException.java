package com.sism.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resource, Long id) {
        super(404, resource + " not found with id: " + id);
    }
    
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}

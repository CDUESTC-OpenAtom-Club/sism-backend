package com.sism.enums;

/**
 * Alert status enumeration
 * Defines the handling status of alert events
 */
public enum AlertStatus {
    /**
     * Alert is open and pending handling
     */
    OPEN,
    
    /**
     * Alert is being handled
     */
    IN_PROGRESS,
    
    /**
     * Alert has been resolved
     */
    RESOLVED,
    
    /**
     * Alert has been closed
     */
    CLOSED
}

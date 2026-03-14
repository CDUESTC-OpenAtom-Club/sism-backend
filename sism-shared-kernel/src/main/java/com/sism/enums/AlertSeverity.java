package com.sism.enums;

/**
 * Alert severity enumeration
 * Defines the severity levels of alert events
 */
public enum AlertSeverity {
    /**
     * Informational alert - gap <= 10%
     */
    INFO,
    
    /**
     * Warning alert - gap 10-20%
     */
    WARNING,
    
    /**
     * Critical alert - gap > 20%
     */
    CRITICAL
}

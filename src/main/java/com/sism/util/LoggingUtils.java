package com.sism.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Utility class for structured logging with request context.
 * 
 * Provides helper methods to ensure consistent logging format
 * with request context information across the application.
 * 
 * Requirements:
 * - 15.4: Complete stack trace for errors
 * - 15.5: Request context in error logs
 */
public final class LoggingUtils {
    
    private LoggingUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Log an error with full context and stack trace.
     * 
     * @param logger The logger to use
     * @param message The error message
     * @param exception The exception to log
     */
    public static void logError(Logger logger, String message, Throwable exception) {
        String context = buildContextString();
        logger.error("{} [context={}]", message, context, exception);
    }
    
    /**
     * Log an error with additional details and stack trace.
     * 
     * @param logger The logger to use
     * @param message The error message
     * @param details Additional details map
     * @param exception The exception to log
     */
    public static void logError(Logger logger, String message, Map<String, Object> details, Throwable exception) {
        String context = buildContextString();
        String detailsStr = formatDetails(details);
        logger.error("{} [details={}, context={}]", message, detailsStr, context, exception);
    }
    
    /**
     * Log a warning with context.
     * 
     * @param logger The logger to use
     * @param message The warning message
     */
    public static void logWarning(Logger logger, String message) {
        String context = buildContextString();
        logger.warn("{} [context={}]", message, context);
    }
    
    /**
     * Log a warning with additional details.
     * 
     * @param logger The logger to use
     * @param message The warning message
     * @param details Additional details map
     */
    public static void logWarning(Logger logger, String message, Map<String, Object> details) {
        String context = buildContextString();
        String detailsStr = formatDetails(details);
        logger.warn("{} [details={}, context={}]", message, detailsStr, context);
    }
    
    /**
     * Log an info message with context (useful for audit logging).
     * 
     * @param logger The logger to use
     * @param message The info message
     * @param details Additional details map
     */
    public static void logInfo(Logger logger, String message, Map<String, Object> details) {
        String context = buildContextString();
        String detailsStr = formatDetails(details);
        logger.info("{} [details={}, context={}]", message, detailsStr, context);
    }
    
    /**
     * Build a context string from MDC values.
     * 
     * @return Formatted context string
     */
    public static String buildContextString() {
        StringJoiner joiner = new StringJoiner(", ");
        
        addMdcValue(joiner, "requestId");
        addMdcValue(joiner, "userId");
        addMdcValue(joiner, "clientIp");
        addMdcValue(joiner, "requestMethod");
        addMdcValue(joiner, "requestUri");
        
        return joiner.toString();
    }
    
    /**
     * Get the current request ID from MDC.
     * 
     * @return Request ID or "N/A" if not set
     */
    public static String getRequestId() {
        String requestId = MDC.get("requestId");
        return requestId != null ? requestId : "N/A";
    }
    
    /**
     * Get the current user ID from MDC.
     * 
     * @return User ID or "anonymous" if not set
     */
    public static String getUserId() {
        String userId = MDC.get("userId");
        return userId != null ? userId : "anonymous";
    }
    
    /**
     * Add MDC value to joiner if present.
     */
    private static void addMdcValue(StringJoiner joiner, String key) {
        String value = MDC.get(key);
        if (value != null && !value.isBlank()) {
            joiner.add(key + "=" + value);
        }
    }
    
    /**
     * Format details map as string.
     */
    private static String formatDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        details.forEach((key, value) -> {
            String valueStr = value != null ? value.toString() : "null";
            // Truncate long values
            if (valueStr.length() > 200) {
                valueStr = valueStr.substring(0, 200) + "...";
            }
            joiner.add(key + "=" + valueStr);
        });
        
        return joiner.toString();
    }
}

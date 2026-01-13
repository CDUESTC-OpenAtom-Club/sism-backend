package com.sism.config;

import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark service methods for automatic audit logging
 * 
 * Requirements: 7.1, 7.2, 7.3 - Automatic audit log recording
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableOperation {
    
    /**
     * The type of entity being audited
     */
    AuditEntityType entityType();
    
    /**
     * The action being performed
     */
    AuditAction action();
    
    /**
     * SpEL expression to extract entity ID from method parameters or return value
     * Default: "#result.id" for CREATE, "#p0" for UPDATE/DELETE (first parameter)
     */
    String entityIdExpression() default "";
    
    /**
     * SpEL expression to extract the entity data for before snapshot
     * Used for UPDATE and DELETE operations
     */
    String beforeDataExpression() default "";
    
    /**
     * SpEL expression to extract the entity data for after snapshot
     * Used for CREATE and UPDATE operations
     */
    String afterDataExpression() default "";
    
    /**
     * Whether to capture the return value as after data
     * Default: true for CREATE operations
     */
    boolean captureReturnValue() default true;
}

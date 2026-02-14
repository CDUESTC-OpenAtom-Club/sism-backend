package com.sism.config;

import com.sism.entity.SysUser;
import com.sism.entity.SysOrg;
import com.sism.enums.AuditAction;
import com.sism.enums.AuditEntityType;
import com.sism.repository.UserRepository;
import com.sism.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect for automatic audit logging
 * Intercepts service layer methods annotated with @AuditableOperation
 * and automatically records audit logs for CREATE, UPDATE, DELETE operations
 * 
 * Requirements: 7.1, 7.2, 7.3 - Automatic audit log recording
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Around advice for methods annotated with @AuditableOperation
     * Captures method parameters and return values for audit logging
     * 
     * @param joinPoint the join point
     * @param auditableOperation the annotation
     * @return the method result
     * @throws Throwable if the method throws an exception
     */
    @Around("@annotation(auditableOperation)")
    public Object auditOperation(ProceedingJoinPoint joinPoint, AuditableOperation auditableOperation) throws Throwable {
        AuditEntityType entityType = auditableOperation.entityType();
        AuditAction action = auditableOperation.action();
        
        // Get method signature and parameters
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        // Capture before data for UPDATE and DELETE operations
        Object beforeData = null;
        if (action == AuditAction.UPDATE || action == AuditAction.DELETE || action == AuditAction.ARCHIVE) {
            beforeData = captureBeforeData(joinPoint, auditableOperation, method, args);
        }
        
        // Execute the actual method
        Object result = joinPoint.proceed();
        
        try {
            // Extract entity ID
            Long entityId = extractEntityId(joinPoint, auditableOperation, method, args, result);
            
            // Capture after data for CREATE and UPDATE operations
            Object afterData = null;
            if (action == AuditAction.CREATE || action == AuditAction.UPDATE) {
                afterData = captureAfterData(joinPoint, auditableOperation, method, args, result);
            }
            
            // Get current user and organization
            SysUser actorUser = getCurrentUser();
            SysOrg actorOrg = actorUser != null ? actorUser.getOrg() : null;
            
            // Record audit log based on action type
            recordAuditLog(entityType, entityId, action, beforeData, afterData, actorUser, actorOrg);
            
        } catch (Exception e) {
            // Log error but don't fail the main operation
            log.warn("Failed to record audit log for {} {} on {}: {}", 
                    action, entityType, signature.getName(), e.getMessage());
        }
        
        return result;
    }

    /**
     * Capture before data using SpEL expression or default behavior
     */
    private Object captureBeforeData(ProceedingJoinPoint joinPoint, AuditableOperation annotation,
                                      Method method, Object[] args) {
        String expression = annotation.beforeDataExpression();
        if (expression != null && !expression.isEmpty()) {
            return evaluateExpression(expression, method, args, null);
        }
        // Default: return first parameter if it's an entity ID, try to load the entity
        return null; // Will be handled by the service layer
    }

    /**
     * Capture after data using SpEL expression or return value
     */
    private Object captureAfterData(ProceedingJoinPoint joinPoint, AuditableOperation annotation,
                                     Method method, Object[] args, Object result) {
        String expression = annotation.afterDataExpression();
        if (expression != null && !expression.isEmpty()) {
            return evaluateExpression(expression, method, args, result);
        }
        // Default: return the method result if captureReturnValue is true
        if (annotation.captureReturnValue() && result != null) {
            return result;
        }
        return null;
    }

    /**
     * Extract entity ID from method parameters or return value
     */
    private Long extractEntityId(ProceedingJoinPoint joinPoint, AuditableOperation annotation,
                                  Method method, Object[] args, Object result) {
        String expression = annotation.entityIdExpression();
        
        if (expression != null && !expression.isEmpty()) {
            Object value = evaluateExpression(expression, method, args, result);
            return convertToLong(value);
        }
        
        // Default behavior based on action type
        AuditAction action = annotation.action();
        if (action == AuditAction.CREATE) {
            // Try to get ID from result
            return extractIdFromObject(result);
        } else {
            // Try to get ID from first parameter
            if (args.length > 0) {
                return convertToLong(args[0]);
            }
        }
        
        return null;
    }

    /**
     * Evaluate SpEL expression
     */
    private Object evaluateExpression(String expressionString, Method method, Object[] args, Object result) {
        try {
            Expression expression = parser.parseExpression(expressionString);
            EvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, args, parameterNameDiscoverer);
            
            // Add result to context
            if (result != null) {
                ((MethodBasedEvaluationContext) context).setVariable("result", result);
            }
            
            return expression.getValue(context);
        } catch (Exception e) {
            log.debug("Failed to evaluate SpEL expression '{}': {}", expressionString, e.getMessage());
            return null;
        }
    }

    /**
     * Extract ID from an object using reflection
     */
    private Long extractIdFromObject(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            // Try common ID getter methods
            String[] idMethods = {"getId", "getIndicatorId", "getMilestoneId", "getReportId", 
                                  "getTaskId", "getLogId", "getOrgId", "getUserId"};
            
            for (String methodName : idMethods) {
                try {
                    Method getter = obj.getClass().getMethod(methodName);
                    Object value = getter.invoke(obj);
                    return convertToLong(value);
                } catch (NoSuchMethodException ignored) {
                    // Try next method
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract ID from object: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Convert object to Long
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get current authenticated user
     */
    private SysUser getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String username) {
                    return userRepository.findByUsername(username).orElse(null);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Record audit log based on action type
     */
    private void recordAuditLog(AuditEntityType entityType, Long entityId, AuditAction action,
                                 Object beforeData, Object afterData, SysUser actorUser, SysOrg actorOrg) {
        if (entityId == null) {
            log.warn("Cannot record audit log: entity ID is null");
            return;
        }
        
        switch (action) {
            case CREATE -> auditLogService.logCreate(entityType, entityId, afterData, actorUser, actorOrg, null);
            case UPDATE -> auditLogService.logUpdate(entityType, entityId, beforeData, afterData, actorUser, actorOrg, null);
            case DELETE -> auditLogService.logDelete(entityType, entityId, beforeData, actorUser, actorOrg, null);
            case ARCHIVE -> auditLogService.logArchive(entityType, entityId, beforeData, actorUser, actorOrg, null);
            case APPROVE -> auditLogService.logApprove(entityType, entityId, beforeData, afterData, actorUser, actorOrg, null);
            case RESTORE -> auditLogService.logRestore(entityType, entityId, afterData, actorUser, actorOrg, null);
        }
        
        log.debug("Audit log recorded: {} {} {}", action, entityType, entityId);
    }
}

package com.sism.exception;

/**
 * Base exception class for workflow-related errors
 * Extends BusinessException to integrate with existing error handling
 */
public class WorkflowException extends BusinessException {

    public WorkflowException(String message) {
        super(400, message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(400, message, cause);
    }

    /**
     * Exception for invalid workflow state transitions
     */
    public static class InvalidWorkflowStateException extends WorkflowException {
        public InvalidWorkflowStateException(String message) {
            super(message);
        }
        
        public InvalidWorkflowStateException(com.sism.enums.WorkflowStatus current, 
                                           com.sism.enums.WorkflowStatus attempted) {
            super(String.format("无效的工作流状态转换: %s -> %s", current, attempted));
        }
    }

    /**
     * Exception for unauthorized workflow actions
     */
    public static class UnauthorizedWorkflowActionException extends WorkflowException {
        public UnauthorizedWorkflowActionException(String message) {
            super(message);
        }
        
        public UnauthorizedWorkflowActionException(String action, Long departmentId) {
            super(String.format("部门 %d 无权限执行操作: %s", departmentId, action));
        }
    }

    /**
     * Exception for workflow validation failures
     */
    public static class WorkflowValidationException extends WorkflowException {
        public WorkflowValidationException(String message) {
            super(message);
        }
        
        public WorkflowValidationException(String field, String reason) {
            super(String.format("工作流验证失败 - %s: %s", field, reason));
        }
    }

    /**
     * Exception for concurrent workflow modifications
     */
    public static class ConcurrentWorkflowModificationException extends WorkflowException {
        public ConcurrentWorkflowModificationException(String message) {
            super(message);
        }
        
        public ConcurrentWorkflowModificationException(Long indicatorId) {
            super(String.format("指标 %d 正在被其他用户修改，请稍后重试", indicatorId));
        }
    }
}
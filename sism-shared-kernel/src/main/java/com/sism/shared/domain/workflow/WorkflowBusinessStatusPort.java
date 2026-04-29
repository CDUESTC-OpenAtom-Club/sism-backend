package com.sism.shared.domain.workflow;

/**
 * Shared business status sync contract for workflow terminal state changes.
 */
public interface WorkflowBusinessStatusPort {

    default void syncBusinessStatus(String entityType, Long entityId, String status) {
        syncBusinessStatus(entityType, entityId, status, null, null, null);
    }

    void syncBusinessStatus(
            String entityType,
            Long entityId,
            String status,
            Long operatorId,
            String comment,
            Long workflowInstanceId
    );
}

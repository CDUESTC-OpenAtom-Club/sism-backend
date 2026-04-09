package com.sism.execution.domain.repository;

public interface WorkflowAuditSyncGateway {

    void markApproved(Long auditInstanceId);

    void markRejected(Long auditInstanceId);
}

package com.sism.execution.domain.report;

public interface WorkflowAuditSyncGateway {

    void markApproved(Long auditInstanceId);

    void markRejected(Long auditInstanceId);
}

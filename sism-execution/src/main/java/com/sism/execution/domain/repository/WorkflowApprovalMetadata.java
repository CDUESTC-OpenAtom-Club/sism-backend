package com.sism.execution.domain.repository;

import java.time.LocalDateTime;

public record WorkflowApprovalMetadata(
        Long submittedBy,
        Long approvedBy,
        LocalDateTime approvedAt
) {

    public static WorkflowApprovalMetadata empty() {
        return new WorkflowApprovalMetadata(null, null, null);
    }
}

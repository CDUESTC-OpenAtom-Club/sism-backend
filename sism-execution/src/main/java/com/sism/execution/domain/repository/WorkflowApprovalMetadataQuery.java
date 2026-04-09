package com.sism.execution.domain.repository;

import java.util.Map;

public interface WorkflowApprovalMetadataQuery {

    Map<Long, WorkflowApprovalMetadata> findByAuditInstanceIds(Iterable<Long> auditInstanceIds);
}

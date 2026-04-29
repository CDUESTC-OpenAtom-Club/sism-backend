package com.sism.execution.domain.report;

import java.util.Map;

public interface WorkflowApprovalMetadataQuery {

    Map<Long, WorkflowApprovalMetadata> findByAuditInstanceIds(Iterable<Long> auditInstanceIds);
}

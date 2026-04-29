package com.sism.shared.domain.workflow;

import java.util.Optional;

/**
 * Shared business context lookup for workflow decisions.
 */
public interface WorkflowBusinessContextPort {

    record BusinessSummary(
            Long planId,
            String planName,
            Long sourceOrgId,
            String sourceOrgName,
            Long targetOrgId,
            String targetOrgName,
            String displayName
    ) {}

    Optional<Long> getPlanIdByEntity(String entityType, Long entityId);

    Optional<BusinessSummary> getBusinessSummary(String entityType, Long entityId);
}

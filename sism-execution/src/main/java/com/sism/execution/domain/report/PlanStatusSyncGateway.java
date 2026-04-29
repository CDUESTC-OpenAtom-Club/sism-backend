package com.sism.execution.domain.report;

public interface PlanStatusSyncGateway {

    void syncBackToDraft(Long planId);
}
